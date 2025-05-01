package com.example.bs

import android.net.Uri
import java.util.concurrent.ExecutorService

class StartShootingUseCase(
    private val cameraManager: CameraXManager,
    private val cameraExecutor: ExecutorService
) {
    private var isShooting = false
    private var intervalSec: Int = 2
    private var shootingJob: Runnable? = null

    fun execute(intervalSec: Int, onImageCaptured: (Uri?) -> Unit) {
        if (!isShooting) {
            this.intervalSec = intervalSec
            isShooting = true

            shootingJob = Runnable {
                while (isShooting) {
                    val uri = cameraManager.captureImage() // теперь должен возвращать Uri
                    onImageCaptured(uri)
                    Thread.sleep(intervalSec * 1000L)
                }
            }

            cameraExecutor.submit(shootingJob)
        }
    }

    fun stop() {
        isShooting = false
    }
}
