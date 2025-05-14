package com.example.bs

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService

class StartShootingUseCase(
    private val cameraManager: CameraXManager,
    private val cameraExecutor: ExecutorService
) {
    private var isShooting = false
    private var intervalSec: Int = 2
    private var shootingJob: Job? = null

    fun execute(intervalSec: Int, onImageCaptured: (Uri?) -> Unit) {
        if (!isShooting) {
            this.intervalSec = intervalSec
            isShooting = true

            shootingJob = CoroutineScope(Dispatchers.IO).launch {
                while (isShooting) {
                    val uri = cameraManager.captureImage()
                    onImageCaptured(uri)
                    delay(intervalSec * 1000L)
                }
            }
        }
    }

    fun stop() {
        isShooting = false
        shootingJob?.cancel()
    }
}