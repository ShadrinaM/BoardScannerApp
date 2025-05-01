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
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var startStopButton: Button
    private lateinit var intervalSpinner: Spinner
    private lateinit var cameraManager: CameraXManager
    private lateinit var cameraExecutor: ExecutorService
    private var isShooting = false  // Для отслеживания состояния съёмки

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

        // Проверяем разрешение на использование камеры
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение есть, запускаем камеру
            startCamera()
        } else {
            // Если разрешения нет, запрашиваем его у пользователя
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }

        // Инициализация зависимостей
        startShootingUseCase = StartShootingUseCase(cameraManager, cameraExecutor)
        stopShootingUseCase = StopShootingUseCase(startShootingUseCase)

        val pdfGenerator = PdfGenerator()
        val pdfRepository = PdfRepositoryImpl(pdfGenerator)
        generatePdfUseCase = GeneratePdfUseCase(pdfRepository)


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

    // Остановка съёмки
    private fun requestPdf() {

        val pdfUri = cameraViewModel.generatePDF(this) // Запрос на генерацию PDF
        pdfUri?.let {
            // Здесь убраны строки с Toast
            Log.d("MainActivity", "PDF сохранён: ${it.path}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Закрытие Executor, когда активность уничтожается
    }
}
