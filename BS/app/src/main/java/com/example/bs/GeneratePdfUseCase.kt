package com.example.bs

import android.content.Context
import android.net.Uri

//class GeneratePdfUseCase(private val pdfRepository: PdfRepository) {
//    fun execute(context: Context, images: List<Uri>): Uri? {
//        return pdfRepository.generatePdf(context, images)
//    }
//}




class GeneratePdfUseCase(private val pdfGenerator: PdfGenerator) {
    fun execute(context: Context, images: List<Uri>): Uri? {
        return pdfGenerator.generatePdf(images, context)
    }
}