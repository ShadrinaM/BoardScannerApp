package com.example.bs

import android.content.Context
import android.net.Uri

class GeneratePdfUseCase(private val pdfRepository: PdfRepository) {
    fun execute(context: Context, images: List<Uri>): Uri? {
        return pdfRepository.generatePdf(context, images)
    }
}

