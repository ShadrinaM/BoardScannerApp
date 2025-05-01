package com.example.bs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel

class CameraViewModel(
    private val startShootingUseCase: StartShootingUseCase,
    private val stopShootingUseCase: StopShootingUseCase,
    private val generatePdfUseCase: GeneratePdfUseCase
) : ViewModel() {

    private val imageUris = mutableListOf<Uri>()
    private var isShooting = false

    fun startShooting(intervalSec: Int) {
        if (!isShooting) {
            startShootingUseCase.execute(intervalSec) { uri ->
                uri?.let { imageUris.add(it) }
            }
            isShooting = true
        }
    }

    fun stopShooting() {
        if (isShooting) {
            stopShootingUseCase.execute()
            isShooting = false
        }
    }

    fun generatePDF(context: Context): Uri? {
        return generatePdfUseCase.execute(context, imageUris)
    }
}
