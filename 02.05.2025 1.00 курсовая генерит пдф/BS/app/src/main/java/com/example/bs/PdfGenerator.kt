package com.example.bs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import android.widget.Toast

class PdfGenerator {
    fun generatePdf(images: List<Uri>, context: Context): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val bordScanDir = File(downloadsDir, "BordScan")
        if (!bordScanDir.exists()) {
            bordScanDir.mkdirs()
        }

        val pdfFile = File(bordScanDir, "timelapse_photos.pdf")
        val document = Document()

        return try {
            val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            for (imageUri in images) {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                }

                val width = bitmap.width.toFloat()
                val height = bitmap.height.toFloat()

                document.setPageSize(com.itextpdf.text.Rectangle(width, height))
                document.newPage()

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val image = Image.getInstance(stream.toByteArray())
                image.setAbsolutePosition(0f, 0f)
                image.scaleToFit(width, height)

                document.add(image)
            }

            document.close()
            writer.close()

            val message = "PDF сохранён в: ${pdfFile.absolutePath}"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            Log.d("PdfGenerator", message)
            Uri.fromFile(pdfFile)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}