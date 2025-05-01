// обеспечивает доступ списку сделанных фото, отправляет их на генерацию в PdfGenerator

package com.example.bs

import android.content.Context
import android.net.Uri

class PdfRepositoryImpl(
    private val pdfGenerator: PdfGenerator
) : PdfRepository {
    override fun generatePdf(context: Context, images: List<Uri>): Uri? {
        return pdfGenerator.generatePdf(images, context)
    }
}
