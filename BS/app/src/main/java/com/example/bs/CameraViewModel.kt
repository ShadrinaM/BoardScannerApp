package com.example.bs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                uri?.let {
                    viewModelScope.launch {
                        imageUris.add(it)
                    }
                }
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

    fun generatePDF(context: Context, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfUri = generatePdfUseCase.execute(context, imageUris)
            withContext(Dispatchers.Main) {
                onComplete(pdfUri)
            }
        }
    }
}

