package com.example.bs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CameraViewModelFactory(
    private val startShootingUseCase: StartShootingUseCase,
    private val stopShootingUseCase: StopShootingUseCase,
    private val generatePdfUseCase: GeneratePdfUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(
                startShootingUseCase,
                stopShootingUseCase,
                generatePdfUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
