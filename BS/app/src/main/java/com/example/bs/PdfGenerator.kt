package com.example.bs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class PdfGenerator(private val context: Context){

    /*// Просто ПДФ
    fun generatePdf(images: List<Uri>, context: Context): Uri? {
        if (images.isEmpty()) {
            Log.e("PdfGenerator", "No images to generate PDF")
            return null
        }

        val bordScanDir = getOrCreateBordScanDir()
        val pdfFile = File(bordScanDir, "lecture_${getFormattedDateTime()}.pdf")

        return try {
            val document = Document()
            val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            for (imageUri in images) {
                val bitmap = try {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { originalBitmap ->
                            // Поворачиваем изображение на 90 градусов вправо
                            val matrix = Matrix()
                            matrix.postRotate(90f)
                            val rotatedBitmap = Bitmap.createBitmap(
                                originalBitmap,
                                0, 0,
                                originalBitmap.width,
                                originalBitmap.height,
                                matrix,
                                true
                            )
                            // Масштабируем после поворота
                            val maxWidth = 1000
                            val scale = maxWidth.toFloat() / rotatedBitmap.width
                            val height = (rotatedBitmap.height * scale).toInt()
                            Bitmap.createScaledBitmap(rotatedBitmap, maxWidth, height, true)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PdfGenerator", "Error loading image: $imageUri", e)
                    continue
                }

                bitmap?.let {
                    try {
                        document.setPageSize(com.itextpdf.text.Rectangle(it.width.toFloat(), it.height.toFloat()))
                        document.newPage()

                        val stream = ByteArrayOutputStream()
                        it.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        val image = Image.getInstance(stream.toByteArray())
                        image.setAbsolutePosition(0f, 0f)
                        document.add(image)
                    } catch (e: Exception) {
                        Log.e("PdfGenerator", "Error adding image to PDF", e)
                    }
                }
            }

            document.close()
            writer.close()

            Log.d("PdfGenerator", "PDF successfully generated at: ${pdfFile.absolutePath}")
            cleanPictures()
            Uri.fromFile(pdfFile)
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error generating PDF", e)
            null
        }
    }*/

    fun generatePdf(images: List<Uri>, context: Context): Uri? {
        if (images.isEmpty()) {
            Log.e("PdfGenerator", "No images to generate PDF")
            return null
        }

        val bordScanDir = getOrCreateBordScanDir()
        val pdfFile = File(bordScanDir, "lecture_${getFormattedDateTime()}.pdf")

        return try {
            val document = Document()
            val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            for (imageUri in images) {
                val bitmap = try {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { originalBitmap ->
//                            // Выравнивание доски перед обработкой
//                            val alignedBitmap = alignBoard(originalBitmap)
//
//                            // Поворачиваем изображение на 90 градусов вправо
//                            val matrix = Matrix()
//                            matrix.postRotate(90f)
//                            val rotatedBitmap = Bitmap.createBitmap(
//                                alignedBitmap,  // Используем выровненное изображение
//                                0, 0,
//                                alignedBitmap.width,
//                                alignedBitmap.height,
//                                matrix,
//                                true
//                            )
//                            // Масштабируем после поворота
//                            val maxWidth = 1000
//                            val scale = maxWidth.toFloat() / rotatedBitmap.width
//                            val height = (rotatedBitmap.height * scale).toInt()
//                            Bitmap.createScaledBitmap(rotatedBitmap, maxWidth, height, true)

                            val alignedBitmap = alignBoard(originalBitmap)

                            val maxWidth = 1000
                            val scale = maxWidth.toFloat() / alignedBitmap.width
                            val height = (alignedBitmap.height * scale).toInt()
                            Bitmap.createScaledBitmap(alignedBitmap, maxWidth, height, true)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PdfGenerator", "Error loading image: $imageUri", e)
                    continue
                }

                bitmap?.let {
                    try {
                        document.setPageSize(com.itextpdf.text.Rectangle(it.width.toFloat(), it.height.toFloat()))
                        document.newPage()

                        val stream = ByteArrayOutputStream()
                        it.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        val image = Image.getInstance(stream.toByteArray())
                        image.setAbsolutePosition(0f, 0f)
                        document.add(image)
                    } catch (e: Exception) {
                        Log.e("PdfGenerator", "Error adding image to PDF", e)
                    }
                }
            }

            document.close()
            writer.close()

            Log.d("PdfGenerator", "PDF successfully generated at: ${pdfFile.absolutePath}")
            cleanPictures()
            Uri.fromFile(pdfFile)
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error generating PDF", e)
            null
        }
    }

    // Получение текущей даты в нужном формате
    private fun getFormattedDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            .format(Date())
    }

    // Создание или получение директории BordScan
    private fun getOrCreateBordScanDir(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val bordScanDir = File(downloadsDir, "BordScan")
        if (!bordScanDir.exists()) {
            bordScanDir.mkdirs()
        }
        return bordScanDir
    }

    // Очищение директории
    private fun cleanPictures() {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        picturesDir?.listFiles()?.forEach {
            try {
                if (it.delete()) {
                    Log.d("PdfGenerator", "Deleted file: ${it.name}")
                }
            } catch (e: Exception) {
                Log.e("PdfGenerator", "Error deleting file: ${it.name}", e)
            }
        }
    }




    fun alignBoard(srcBitmap: Bitmap): Bitmap {
        // Конвертация Bitmap в Mat (BGR)
        val srcMat = Mat()
        Utils.bitmapToMat(srcBitmap, srcMat)
        val origMat = srcMat.clone()  // копия для применения warpPerspective

        // Преобразование в серый и размытие для подавления шума
        val gray = Mat()
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // Решаем, светлая или тёмная доска (по средней яркости)
        val meanVal = Core.mean(gray).`val`[0]
        val thresh = Mat()
        if (meanVal > 127) {
            // Светлая доска: применяем адаптивный порог для выделения границ доски и надписей
            Imgproc.adaptiveThreshold(gray, thresh, 255.0,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15.0)
        } else {
            // Тёмная доска: инвертированный порог (чтобы доска стала белой на чёрном фоне)
            Imgproc.adaptiveThreshold(gray, thresh, 255.0,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 15.0)
        }

        // Поиск контуров
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        Log.d("BoardAlign", "Contours found: ${contours.size}")

        // Выбираем максимальный по площади контур-четырёхугольник
        var boardContour: MatOfPoint2f? = null
        var maxArea = 0.0
        for (contour in contours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val area = Imgproc.contourArea(contour2f)
            if (area < 1000) continue  // фильтр очень маленьких областей
            // Аппроксимация контура
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * Imgproc.arcLength(contour2f, true), true)
            // Проверяем, что контур четырёхвершинный и с максимальной площадью
/*            if (approx.total() == 4L && area > maxArea) {
                maxArea = area
                boardContour = approx
            }*/
            // Вместо area > maxArea
            if (approx.total() == 4L && area > maxArea && area > srcMat.width() * srcMat.height() * 0.2) {
                maxArea = area
                boardContour = approx
            }

        }

        if (boardContour == null) {
            Log.d("BoardAlign", "Board contour not found, returning original")
            return srcBitmap  // доска не обнаружена
        }

        // Сортировка углов: разделяем по y (верхние, нижние) и по x внутри них
        val pts = boardContour.toArray().sortedBy { it.y }
        val topPts = pts.take(2).sortedBy { it.x }
        val bottomPts = pts.takeLast(2).sortedBy { it.x }
        val tl = topPts[0];  val tr = topPts[1]
        val bl = bottomPts[0];  val br = bottomPts[1]
        Log.d("BoardAlign", "Corners: TL=$tl, TR=$tr, BR=$br, BL=$bl")

        // Вычисление размеров доски по расстоянию между точками (реальные пропорции)
        fun dist(p1: Point, p2: Point) = Math.hypot(p1.x - p2.x, p1.y - p2.y)
        val widthA = dist(br, bl)
        val widthB = dist(tr, tl)
        val maxWidth = Math.max(widthA, widthB).toInt()
        val heightA = dist(tr, br)
        val heightB = dist(tl, bl)
        val maxHeight = Math.max(heightA, heightB).toInt()
        Log.d("BoardAlign", "Transformed size: width=$maxWidth, height=$maxHeight")

        // Исходные и целевые точки для преобразования
        val srcPoints = MatOfPoint2f(tl, tr, br, bl)
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point((maxWidth - 1).toDouble(), 0.0),
            Point((maxWidth - 1).toDouble(), (maxHeight - 1).toDouble()),
            Point(0.0, (maxHeight - 1).toDouble())
        )

        // Перспективное преобразование и обрезка
        val M = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        Imgproc.warpPerspective(origMat, warped, M, Size(maxWidth.toDouble(), maxHeight.toDouble()))

        // Конвертация Mat обратно в Bitmap
        val resultBitmap = Bitmap.createBitmap(warped.cols(), warped.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warped, resultBitmap)
        Log.d("BoardAlign", "Board aligned and cropped, returning result")
        return resultBitmap
    }



}




//class PdfGenerator {
//    fun generatePdf(images: List<Uri>, context: Context): Uri? {
//        val bordScanDir = getOrCreateBordScanDir()
//        val pdfFile = File(bordScanDir, "timelapse_photos.pdf")
//        val document = Document()
//
//        return try {
//            val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
//            document.open()
//
//            for (imageUri in images) {
//                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                    val source = ImageDecoder.createSource(context.contentResolver, imageUri)
//                    ImageDecoder.decodeBitmap(source)
//                } else {
//                    @Suppress("DEPRECATION")
//                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//                }
//
//                val width = bitmap.width.toFloat()
//                val height = bitmap.height.toFloat()
//
//                document.setPageSize(com.itextpdf.text.Rectangle(width, height))
//                document.newPage()
//
//                val stream = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//                val image = Image.getInstance(stream.toByteArray())
//                image.setAbsolutePosition(0f, 0f)
//                image.scaleToFit(width, height)
//
//                document.add(image)
//            }
//
//            document.close()
//            writer.close()
//
//            val message = "PDF сохранён в: ${pdfFile.absolutePath}"
//            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//
//            Log.d("PdfGenerator", message)
//
//            // Очистка папки после успешного создания PDF
//            cleanPictures()
//
//            Uri.fromFile(pdfFile)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//    private fun getOrCreateBordScanDir(): File {
//        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val bordScanDir = File(downloadsDir, "BordScan")
//        if (!bordScanDir.exists()) {
//            bordScanDir.mkdirs()
//        }
//        return bordScanDir
//    }
//
//    private fun cleanPictures() {
//        val picturesDir = File("/storage/emulated/0/Android/data/com.example.bs/files/Pictures")
//        if (picturesDir.exists() && picturesDir.isDirectory) {
//            picturesDir.listFiles()?.forEach {
//                if (it.delete()) {
//                    Log.d("PdfGenerator", "Удалён файл: ${it.name}")
//                } else {
//                    Log.e("PdfGenerator", "Не удалось удалить: ${it.name}")
//                }
//            }
//        } else {
//            Log.w("PdfGenerator", "Папка не найдена или не является директорией")
//        }
//    }
//}




//    // // применяет нужные алгоритмы но находит маленький прямоугольник
////    private fun processDocument(bitmap: Bitmap): Bitmap {
////        val safeBitmap = ensureBitmapConfig(bitmap)
////        val src = Mat()
////        Utils.bitmapToMat(safeBitmap, src)
////
////        val gray = Mat()
////        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
////        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
////        Imgproc.Canny(gray, gray, 75.0, 200.0)
////
////        val contours = mutableListOf<MatOfPoint>()
////        val hierarchy = Mat()
////        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
////
////        val docContour = contours
////            .map { MatOfPoint2f(*it.toArray()) }
////            .mapNotNull { contour ->
////                val peri = Imgproc.arcLength(contour, true)
////                val approx = MatOfPoint2f()
////                Imgproc.approxPolyDP(contour, approx, 0.02 * peri, true)
////                if (approx.total() == 4L) approx else null
////            }
////            .maxByOrNull { Imgproc.contourArea(it) }
////
////        if (docContour != null) {
////            val srcPoints = docContour.toArray().sortedWith(compareBy({ it.y }, { it.x }))
////            val ordered = MatOfPoint2f(
////                srcPoints[0], srcPoints[1], srcPoints[3], srcPoints[2]
////            ) // TL, TR, BR, BL
////
////            val widthA = Math.hypot((srcPoints[2].x - srcPoints[3].x), (srcPoints[2].y - srcPoints[3].y))
////            val widthB = Math.hypot((srcPoints[1].x - srcPoints[0].x), (srcPoints[1].y - srcPoints[0].y))
////            val maxWidth = Math.max(widthA, widthB).toInt()
////
////            val heightA = Math.hypot((srcPoints[1].x - srcPoints[2].x), (srcPoints[1].y - srcPoints[2].y))
////            val heightB = Math.hypot((srcPoints[0].x - srcPoints[3].x), (srcPoints[0].y - srcPoints[3].y))
////            val maxHeight = Math.max(heightA, heightB).toInt()
////
////            val dst = MatOfPoint2f(
////                Point(0.0, 0.0),
////                Point(maxWidth.toDouble(), 0.0),
////                Point(maxWidth.toDouble(), maxHeight.toDouble()),
////                Point(0.0, maxHeight.toDouble())
////            )
////
////            val transform = Imgproc.getPerspectiveTransform(ordered, dst)
////            val warped = Mat()
////            Imgproc.warpPerspective(src, warped, transform, Size(maxWidth.toDouble(), maxHeight.toDouble()))
////
////            val outputBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
////            Utils.matToBitmap(warped, outputBitmap)
////            return outputBitmap
////        }
////
////        Log.w("PdfGenerator", "Контур документа не найден, возвращаю оригинал")
////        return safeBitmap
////    }
//
//
// // с коментами тоже самое
//    private fun processDocument(bitmap: Bitmap): Bitmap {
//        // Приводим изображение к безопасному формату конфигурации (например, ARGB_8888)
//        val safeBitmap = ensureBitmapConfig(bitmap)
//
//        // Создаём пустую матрицу для хранения изображения
//        val src = Mat()
//
//        // Конвертируем Bitmap в Mat (OpenCV формат)
//        Utils.bitmapToMat(safeBitmap, src)
//
//        // Создаём матрицу для хранения градаций серого
//        val gray = Mat()
//
//        // Переводим изображение в оттенки серого
//        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
//
//        // Размытие изображения по Гауссу для снижения шума
//        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
//
//        // Поиск границ с помощью оператора Канни
//        Imgproc.Canny(gray, gray, 75.0, 200.0)
//
//        // Список для хранения найденных контуров
//        val contours = mutableListOf<MatOfPoint>()
//
//        // Матрица для иерархии контуров
//        val hierarchy = Mat()
//
//        // Поиск контуров на изображении
//        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
//
//        // Пытаемся найти контур, который может быть документом (четырёхугольник с наибольшей площадью)
//        val docContour = contours
//            .map { MatOfPoint2f(*it.toArray()) } // Преобразуем в MatOfPoint2f
//            .mapNotNull { contour ->
//                val peri = Imgproc.arcLength(contour, true) // Периметр
//                val approx = MatOfPoint2f()
//                Imgproc.approxPolyDP(contour, approx, 0.02 * peri, true) // Аппроксимация до многоугольника
//                if (approx.total() == 4L) approx else null // Оставляем только 4-угольники
//            }
//            .maxByOrNull { Imgproc.contourArea(it) } // Выбираем контур с максимальной площадью
//
//        // Если контур найден
//        if (docContour != null) {
//            // Получаем точки и сортируем по Y, затем по X (для упорядочивания углов)
//            val srcPoints = docContour.toArray().sortedWith(compareBy({ it.y }, { it.x }))
//
//            // Упорядочиваем углы: top-left, top-right, bottom-right, bottom-left
//            val ordered = MatOfPoint2f(
//                srcPoints[0], srcPoints[1], srcPoints[3], srcPoints[2]
//            )
//
//            // Вычисляем ширину документа по верхнему и нижнему краю
//            val widthA = Math.hypot((srcPoints[2].x - srcPoints[3].x), (srcPoints[2].y - srcPoints[3].y))
//            val widthB = Math.hypot((srcPoints[1].x - srcPoints[0].x), (srcPoints[1].y - srcPoints[0].y))
//            val maxWidth = Math.max(widthA, widthB).toInt()
//
//            // Вычисляем высоту документа по правой и левой сторонам
//            val heightA = Math.hypot((srcPoints[1].x - srcPoints[2].x), (srcPoints[1].y - srcPoints[2].y))
//            val heightB = Math.hypot((srcPoints[0].x - srcPoints[3].x), (srcPoints[0].y - srcPoints[3].y))
//            val maxHeight = Math.max(heightA, heightB).toInt()
//
//            // Целевые координаты точек для трансформации (прямоугольник)
//            val dst = MatOfPoint2f(
//                Point(0.0, 0.0),
//                Point(maxWidth.toDouble(), 0.0),
//                Point(maxWidth.toDouble(), maxHeight.toDouble()),
//                Point(0.0, maxHeight.toDouble())
//            )
//
//            // Получаем матрицу преобразования перспективы
//            val transform = Imgproc.getPerspectiveTransform(ordered, dst)
//
//            // Создаём пустую матрицу для результирующего изображения
//            val warped = Mat()
//
//            // Преобразуем изображение по перспективе
//            Imgproc.warpPerspective(src, warped, transform, Size(maxWidth.toDouble(), maxHeight.toDouble()))
//
//            // Создаём пустой Bitmap нужного размера и конфигурации
//            val outputBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
//
//            // Конвертируем Mat обратно в Bitmap
//            Utils.matToBitmap(warped, outputBitmap)
//
//            // Возвращаем результат
//            return outputBitmap
//        }
//
//        // Если не найден документ — возвращаем оригинал
//        Log.w("PdfGenerator", "Контур документа не найден, возвращаю оригинал")
//        return safeBitmap
//    }
//
////    fun processDocument(bitmap: Bitmap): Bitmap {
////        // Убеждаемся, что Bitmap в подходящей конфигурации
////        val safeBitmap = ensureBitmapConfig(bitmap)
////        // Конвертируем Bitmap в OpenCV Mat
////        val src = Mat()
////        Utils.bitmapToMat(safeBitmap, src)
////
////        // Переводим в оттенки серого
////        val gray = Mat()
////        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
////
////        // Размытие по Гауссу для уменьшения шума
////        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 1.4)
////
////        // Применение оператора Канни для выделения границ
////        val edges = Mat()
////        Imgproc.Canny(gray, edges, 50.0, 150.0)
////
////        // Конвертируем результат обратно в Bitmap
////        val outputBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
////        Imgproc.cvtColor(
////            edges,
////            edges,
////            Imgproc.COLOR_GRAY2RGBA
////        ) // чтобы корректно сконвертировать в ARGB_8888
////        Utils.matToBitmap(edges, outputBitmap)
////
////        return outputBitmap
////    }
//
//
////    fun processDocument(inputBitmap: Bitmap): Bitmap {
////        // 1. Преобразуем Bitmap в Mat
////        val src = Mat()
////        Utils.bitmapToMat(inputBitmap, src)
////
////        // 2. Преобразуем в оттенки серого
////        val gray = Mat()
////        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
////
////        // 3. Применяем размытие по Гауссу (5x5, σ = 1.4)
////        val blurred = Mat()
////        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 1.4)
////
////        // 4. Применяем Canny
////        val edges = Mat()
////        Imgproc.Canny(blurred, edges, 50.0, 150.0)
////
////        // 5. Преобразуем результат в ARGB Bitmap
////        val edgesColor = Mat()
////        Imgproc.cvtColor(edges, edgesColor, Imgproc.COLOR_GRAY2RGBA)
////
////        val resultBitmap = Bitmap.createBitmap(edgesColor.cols(), edgesColor.rows(), Bitmap.Config.ARGB_8888)
////        Utils.matToBitmap(edgesColor, resultBitmap)
////
////        return resultBitmap
////    }
//
//
//
//
//
//    private fun getOrCreateBordScanDir(): File {
//        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val bordScanDir = File(downloadsDir, "BordScan")
//        if (!bordScanDir.exists()) {
//            bordScanDir.mkdirs()
//        }
//        return bordScanDir
//    }
//
//    private fun cleanPictures() {
//        val picturesDir = File("/storage/emulated/0/Android/data/com.example.bs/files/Pictures")
//        if (picturesDir.exists() && picturesDir.isDirectory) {
//            picturesDir.listFiles()?.forEach {
//                if (it.delete()) {
//                    Log.d("PdfGenerator", "Удалён файл: ${it.name}")
//                } else {
//                    Log.e("PdfGenerator", "Не удалось удалить: ${it.name}")
//                }
//            }
//        } else {
//            Log.w("PdfGenerator", "Папка не найдена или не является директорией")
//        }
//    }
//}
