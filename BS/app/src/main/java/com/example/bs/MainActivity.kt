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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startStopButton = findViewById(R.id.startStopButton)
        intervalSpinner = findViewById(R.id.intervalSpinner)

        // Проверка подключения OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // Инициализация Spinner
        val spinner: Spinner = findViewById(R.id.intervalSpinner)
        // Создание адаптера для Spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.interval_array, // Используем массив интервалов, который мы добавили в strings.xml
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Устанавливаем адаптер в Spinner
        spinner.adapter = adapter

        // Инициализация камеры и обработчик
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraXManager(this)

//        // Проверяем разрешение на использование камеры
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//            // Разрешение есть, запускаем камеру
//            startCamera()
//        } else {
//            // Если разрешения нет, запрашиваем его у пользователя
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
//        }


        checkCameraPermission()

        // Инициализация зависимостей
        startShootingUseCase = StartShootingUseCase(cameraManager, cameraExecutor)
        stopShootingUseCase = StopShootingUseCase(startShootingUseCase)

        val pdfGenerator = PdfGenerator(this) // в Activity
        generatePdfUseCase = GeneratePdfUseCase(pdfGenerator)


        // Создаём фабрику для ViewModel
        val factory = CameraViewModelFactory(startShootingUseCase, stopShootingUseCase, generatePdfUseCase)

        // Получаем ViewModel с фабрикой
        cameraViewModel = ViewModelProvider(this, factory).get(CameraViewModel::class.java)

        // Запуск камеры
        startCamera()

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
                // Либо первый запуск, либо навсегда отказано
                if (!permissionRequestAttempted) {
                    // Первый запуск — запрашиваем
                    permissionRequestAttempted = true
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Навсегда отказано — ведём в настройки
                    showSettingsDialog()
                }
            }
        }
    }

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


    // Запуск камеры и отображение превью
    private fun startCamera() {
        //Запуск камеры, отображение в превью в PreviewView
        cameraManager.startCamera(findViewById(R.id.previewView))
    }

    // Старт съёмки
    private fun startShooting() {

        // Уведомление о начале записи
        Toast.makeText(this, "Запись начата", Toast.LENGTH_SHORT).show()

        val selectedInterval = intervalSpinner.selectedItem.toString() // Получаем строку из Spinner, например, "5 сек"
        val intervalSec = selectedInterval.split(" ")[0].toInt() // Извлекаем первую часть строки и преобразуем в число


        cameraViewModel.startShooting(intervalSec) // Запрос на запуск съёмки

        // Обновление кнопочки startStopButton
        startStopButton.text = "Stop"
        startStopButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        isShooting = true
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

    private fun requestPdf() {
        cameraViewModel.generatePDF(this) { pdfUri ->
            pdfUri?.let {
                Toast.makeText(this, "PDF saved: ${getPathFromUri(it)}", Toast.LENGTH_LONG).show()
//                // Можно открыть PDF
//                openPdf(it)
            } ?: run {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String {
        return uri.path ?: "unknown path"
    }

    private fun openPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Закрытие Executor, когда активность уничтожается
    }
}



//class MainActivity : AppCompatActivity() {
//    private lateinit var cameraViewModel: CameraViewModel
//    private lateinit var startStopButton: Button
//    private lateinit var intervalSpinner: Spinner
//    private lateinit var cameraManager: CameraXManager
//    private lateinit var cameraExecutor: ExecutorService
//    private var isShooting = false  // Для отслеживания состояния съёмки
//    private var permissionRequestAttempted = false // для отслеживания попытки запроса
//
//    private lateinit var startShootingUseCase: StartShootingUseCase
//    private lateinit var stopShootingUseCase: StopShootingUseCase
//    private lateinit var generatePdfUseCase: GeneratePdfUseCase
//
//    companion object {
//        const val CAMERA_PERMISSION_REQUEST_CODE = 101
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        startStopButton = findViewById(R.id.startStopButton)
//        intervalSpinner = findViewById(R.id.intervalSpinner)
//
//        // Проверка подключения OpenCV
//        if (OpenCVLoader.initLocal()) {
//            Log.i(TAG, "OpenCV loaded successfully");
//        } else {
//            Log.e(TAG, "OpenCV initialization failed!");
//            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
//            return;
//        }
//
//        // Инициализация Spinner
//        val spinner: Spinner = findViewById(R.id.intervalSpinner)
//        // Создание адаптера для Spinner
//        val adapter = ArrayAdapter.createFromResource(
//            this,
//            R.array.interval_array, // Используем массив интервалов, который мы добавили в strings.xml
//            android.R.layout.simple_spinner_item
//        )
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        // Устанавливаем адаптер в Spinner
//        spinner.adapter = adapter
//
//        // Инициализация камеры и обработчик
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        cameraManager = CameraXManager(this)
//
//        // проверка наличия разрешение, запрос на разрешения, запус камеры при согласии
//        checkCameraPermission()
//
//        // Инициализация зависимостей
//        startShootingUseCase = StartShootingUseCase(cameraManager, cameraExecutor)
//        stopShootingUseCase = StopShootingUseCase(startShootingUseCase)
//
//        val pdfGenerator = PdfGenerator()
//        generatePdfUseCase = GeneratePdfUseCase(pdfGenerator)
//
//        // Создаём фабрику для ViewModel
//        val factory = CameraViewModelFactory(startShootingUseCase, stopShootingUseCase, generatePdfUseCase)
//
//        // Получаем ViewModel с фабрикой
//        cameraViewModel = ViewModelProvider(this, factory).get(CameraViewModel::class.java)
//
//        // Обработчик кнопки старт/стоп съёмки
//        startStopButton.setOnClickListener {
//            if (isShooting) {
//                stopShooting()
//                requestPdf()
//            } else {
//                startShooting()
//            }
//        }
//    }
//
//    private fun checkCameraPermission() {
//        when {
//            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                    == PackageManager.PERMISSION_GRANTED -> {
//                // Разрешение уже есть
//                startCamera()
//            }
//
//            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
//                // Пользователь уже отклонял — показываем объяснение и запрашиваем снова
//                showRationaleAndRequest()
//            }
//
//            else -> {
//                // Либо первый запуск, либо навсегда отказано
//                if (!permissionRequestAttempted) {
//                    // Первый запуск — запрашиваем
//                    permissionRequestAttempted = true
//                    ActivityCompat.requestPermissions(
//                        this,
//                        arrayOf(Manifest.permission.CAMERA),
//                        CAMERA_PERMISSION_REQUEST_CODE
//                    )
//                } else {
//                    // Навсегда отказано — ведём в настройки
//                    showSettingsDialog()
//                }
//            }
//        }
//    }
//
//    private fun showRationaleAndRequest() {
//        AlertDialog.Builder(this)
//            .setTitle("Нужен доступ к камере")
//            .setMessage("Приложению требуется камера, чтобы снимать и генерировать PDF.")
//            .setPositiveButton("Разрешить") { _, _ ->
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.CAMERA),
//                    CAMERA_PERMISSION_REQUEST_CODE
//                )
//            }
//            .setNegativeButton("Выход") { _, _ -> finishAffinity() }
//            .setCancelable(false)
//            .show()
//    }
//
//    private fun showSettingsDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Разрешение на использование камеры навсегда запрещено")
//            .setMessage("Чтобы продолжить, зайдите в настройки приложения и включите разрешение на камеру.")
//            .setPositiveButton("Настройки") { _, _ ->
//                val intent = Intent(
//                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                    Uri.fromParts("package", packageName, null)
//                )
//                startActivity(intent)
//                // Можно также finishAffinity(), если хотите сразу закрыть
//            }
//            .setNegativeButton("Выход") { _, _ -> finishAffinity() }
//            .setCancelable(false)
//            .show()
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
//            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
//                startCamera()
//            } else {
//                // Показать rationale или перейти в настройки
//                checkCameraPermission()
//            }
//        }
//    }
//
//    // Запуск камеры и отображение превью
//    private fun startCamera() {
//        //Запуск камеры, отображение в превью в PreviewView
//        cameraManager.startCamera(findViewById(R.id.previewView))
//    }
//
//    // Старт съёмки
//    private fun startShooting() {
//
//        // Уведомление о начале записи
//        Toast.makeText(this, "Запись начата", Toast.LENGTH_SHORT).show()
//
//        val selectedInterval = intervalSpinner.selectedItem.toString() // Получаем строку из Spinner, например, "5 сек"
//        val intervalSec = selectedInterval.split(" ")[0].toInt() // Извлекаем первую часть строки и преобразуем в число
//
//
//        cameraViewModel.startShooting(intervalSec) // Запрос на запуск съёмки
//
//        // Обновление кнопочки startStopButton
//        startStopButton.text = "Stop"
//        startStopButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
//        isShooting = true
//    }
//
//    // Остановка съёмки
//    private fun stopShooting() {
//
//        //Уведомление о завершении записи
//        Toast.makeText(this, "Запись завершена", Toast.LENGTH_SHORT).show()
//
//        // Обновление кнопочки startStopButton
//        startStopButton.text = "Start"
//        startStopButton.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
//        isShooting = false
//
//        cameraViewModel.stopShooting() // Запрос на остановку съёмки
//    }
//
//    // Остановка съёмки
//    private fun requestPdf() {
//
//        val pdfUri = cameraViewModel.generatePDF(this) // Запрос на генерацию PDF
//        pdfUri?.let {
//            // Здесь убраны строки с Toast
//            Log.d("MainActivity", "PDF сохранён: ${it.path}")
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown() // Закрытие Executor, когда активность уничтожается
//    }
//}


