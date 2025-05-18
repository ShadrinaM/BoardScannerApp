package com.example.bs

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraXManager(private val context: Context) {
    private lateinit var imageCapture: ImageCapture
    private var aspectRatio: Int = AspectRatio.RATIO_4_3 // По умолчанию 4:3

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Определяем оптимальное соотношение сторон
            val metrics = DisplayMetrics().also {
                (context as Activity).windowManager.defaultDisplay.getMetrics(it)
            }
            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            aspectRatio = screenAspectRatio

            val preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(previewView.display.rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Устанавливаем масштабирование превью
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

            } catch (exc: Exception) {
                Log.e("CameraXManager", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    suspend fun captureImage(): Uri? = withContext(Dispatchers.IO) {
        if (!::imageCapture.isInitialized) return@withContext null

        val photoFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        return@withContext suspendCoroutine<Uri?> { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        Log.d("CameraXManager", "Image saved to: ${savedUri.path}")
                        continuation.resume(savedUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraXManager", "Image capture failed", exception)
                        continuation.resume(null)
                    }
                }
            )
        }
    }
}

/*
class CameraXManager(private val context: Context) {
    private lateinit var imageCapture: ImageCapture

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraXManager", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun captureImage(): Uri? = withContext(Dispatchers.IO) {
        if (!::imageCapture.isInitialized) return@withContext null

        val photoFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        return@withContext suspendCoroutine<Uri?> { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        Log.d("CameraXManager", "Image saved to: ${savedUri.path}")
                        continuation.resume(savedUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraXManager", "Image capture failed", exception)
                        continuation.resume(null)
                    }
                }
            )
        }
    }
}*/