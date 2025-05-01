package com.example.bs

import android.content.Context
import android.net.Uri

interface PdfRepository {
    fun generatePdf(context: Context, images: List<Uri>): Uri?
}