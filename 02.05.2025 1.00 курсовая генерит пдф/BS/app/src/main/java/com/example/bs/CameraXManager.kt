package com.example.bs

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    fun captureImage(): Uri? {
        if (!::imageCapture.isInitialized) return null

        val photoFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        var savedUri: Uri? = null
        val latch = CountDownLatch(1)

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraXManager", "Image saved to: ${savedUri?.path}")
                    latch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraXManager", "Image capture failed", exception)
                    latch.countDown()
                }
            }
        )

        latch.await() // Ждём завершения в текущем потоке (используется внутри background executor)
        return savedUri
    }
}
