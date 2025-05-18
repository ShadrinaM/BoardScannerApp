package com.example.bs

import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.view.PreviewView
import org.opencv.android.NativeCameraView.TAG
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var startStopButton: Button
    private lateinit var intervalSpinner: Spinner
    private lateinit var cameraManager: CameraXManager
    private lateinit var cameraExecutor: ExecutorService
    private var isShooting = false  // Для отслеживания состояния съёмки
    private var permissionRequestAttempted = false // для отслеживания попытки запроса

    private lateinit var startShootingUseCase: StartShootingUseCase
    private lateinit var stopShootingUseCase: StopShootingUseCase
    private lateinit var generatePdfUseCase: GeneratePdfUseCase
    private lateinit var previewView: PreviewView

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация View элементов
        startStopButton = findViewById(R.id.startStopButton)
        intervalSpinner = findViewById(R.id.intervalSpinner)
        previewView = findViewById(R.id.previewView)

        // Проверка подключения OpenCV
        if (!checkOpenCVInitialization()) {
            finish()
            return
        }

        setupIntervalSpinner()
        setupCamera()
        checkCameraPermission()
        setupUseCases()
        setupViewModel()

        // Обработчик кнопки старт/стоп съёмки
        startStopButton.setOnClickListener {
            if (isShooting) {
                stopShooting()
                requestPdf()
            } else {
                startShooting()
            }
        }
    }

    // Проверка OpenCV
    private fun checkOpenCVInitialization(): Boolean {
        return if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
            true
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            false
        }
    }

    // Инициализация Спинера
    private fun setupIntervalSpinner() {
        val spinner: Spinner = findViewById(R.id.intervalSpinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.interval_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    // Инициализация CameraX
    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraXManager(this)
    }

    // Проверка наличия разрешения
    private fun checkCameraPermission() {
    when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> {
            // Разрешение уже есть
            startCamera()
        }

        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
            // Пользователь уже отклонял — показываем объяснение и запрашиваем снова
            showRationaleAndRequest()
        }

        else -> {
            if (!permissionRequestAttempted) {
                // Если первый запуск — запрашиваем разрешение
                permissionRequestAttempted = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            } else {
                // Если навсегда отказано — переход в настройки
                showSettingsDialog()
            }
        }
    }
}

    // Объяснение того, что разрешение необходимо, если пользователь впервый раз отказался
    private fun showRationaleAndRequest() {
        AlertDialog.Builder(this)
            .setTitle("Нужен доступ к камере")
            .setMessage("Приложению требуется камера, чтобы снимать и генерировать PDF.")
            .setPositiveButton("Разрешить") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Выход") { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    //  объяснение того, что разрешение необходимо, если пользователь отказался навсегда
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Разрешение на использование камеры навсегда запрещено")
            .setMessage("Чтобы продолжить, зайдите в настройки приложения и включите разрешение на камеру.")
            .setPositiveButton("Настройки") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
                // Можно также finishAffinity(), если хотите сразу закрыть
            }
            .setNegativeButton("Выход") { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    // переход в настройки если разрешение запрещено навсегда
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                // Показать rationale или перейти в настройки
                checkCameraPermission()
            }
        }
    }

    // Инициализация UseCases
    private fun setupUseCases() {
        startShootingUseCase = StartShootingUseCase(cameraManager, cameraExecutor)
        stopShootingUseCase = StopShootingUseCase(startShootingUseCase)
        generatePdfUseCase = GeneratePdfUseCase(PdfGenerator(this))
    }

    // Инициализация ViewModel
    private fun setupViewModel() {
        val factory = CameraViewModelFactory(startShootingUseCase, stopShootingUseCase, generatePdfUseCase)
        cameraViewModel = ViewModelProvider(this, factory).get(CameraViewModel::class.java)
    }

    // Запуск камеры и отображение превью
    private fun startCamera() {
        //Запуск камеры, отображение в превью в PreviewView
        cameraManager.startCamera(findViewById(R.id.previewView))
    }

    // Старт съёмки
    private fun startShooting() {

        // Уведомление о начале записи
        Toast.makeText(this, "Запись начата", Toast.LENGTH_SHORT).show()

        val selectedInterval = intervalSpinner.selectedItem.toString() // строка из Spinner
        val intervalSec = selectedInterval.split(" ")[0].toInt() // извлекает  число

        // Обновление кнопочки startStopButton
        startStopButton.text = "Stop"
        startStopButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        isShooting = true

        cameraViewModel.startShooting(intervalSec) // Запрос на запуск съёмки
    }

    // Остановка съёмки
    private fun stopShooting() {

        //Уведомление о завершении записи
        Toast.makeText(this, "Запись завершена", Toast.LENGTH_SHORT).show()

        // Обновление кнопочки startStopButton
        startStopButton.text = "Start"
        startStopButton.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        isShooting = false

        cameraViewModel.stopShooting() // Запрос на остановку съёмки
    }

    // Генерация PDF
    private fun requestPdf() {

        val progressContainer = findViewById<LinearLayout>(R.id.pdfProgressContainer)

        // Показать ProgressBar с текстом
        progressContainer.visibility = View.VISIBLE

        cameraViewModel.generatePDF(this) { pdfUri ->
            pdfUri?.let {
                // Скрыть ProgressBar с текстом
                progressContainer.visibility = View.GONE
                Toast.makeText(this, "PDF saved: ${getPathFromUri(it)}", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String {
        return uri.path ?: "unknown path"
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Закрытие Executor, когда активность уничтожается
    }
}