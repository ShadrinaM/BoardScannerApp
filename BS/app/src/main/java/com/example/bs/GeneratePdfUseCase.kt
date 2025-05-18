package com.example.bs

import android.content.Context
import android.net.Uri

class GeneratePdfUseCase(private val pdfGenerator: PdfGenerator) {
    fun execute(context: Context, images: List<Uri>): Uri? {
        return pdfGenerator.generatePdf(images, context)
    }
}