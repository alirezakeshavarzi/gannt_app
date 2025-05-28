package com.example.ganttapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

object PdfExporter {
    private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1001

    fun createPdf(
        context: Context,
        fileName: String,
        tasks: List<Task>,
        startDate: java.time.LocalDate,
        viewMode: ViewMode
    ) {
        val activity = context as? Activity

        fun generateAndSavePdf() {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint().apply { textSize = 12f }
            var y = 40f


            canvas.drawText("Gantt Chart", 40f, y, paint)
            y += 20f
            canvas.drawText("Start: ${startDate.format(DateTimeFormatter.ISO_DATE)}", 40f, y, paint)
            y += 20f
            canvas.drawText("Mode: ${viewMode.name}", 40f, y, paint)
            y += 30f

            tasks.forEach { task ->
                if (y > pageInfo.pageHeight - 40) return@forEach
                canvas.drawText("• ${task.name}: ${task.start} → ${task.end}", 40f, y, paint)
                y += 20f
            }
            doc.finishPage(page)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: throw Exception("Could not create MediaStore URI")
                    resolver.openOutputStream(uri)?.use { out -> doc.writeTo(out) }
                        ?: throw Exception("Could not open output stream")
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    Toast.makeText(
                        context,
                        "PDF saved to Downloads/$fileName",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {

                        activity?.let {
                            ActivityCompat.requestPermissions(
                                it,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                REQUEST_WRITE_EXTERNAL_STORAGE
                            )
                        }
                        doc.close()
                        return
                    }


                    val publicDownloads =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (publicDownloads == null || !publicDownloads.canWrite()) {
                        throw Exception("External storage not available or not writable")
                    }
                    if (!publicDownloads.exists()) {
                        publicDownloads.mkdirs()
                    }
                    val file = File(publicDownloads, fileName)
                    FileOutputStream(file).use { out -> doc.writeTo(out) }


                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("application/pdf"),
                        null
                    )

                    Toast.makeText(
                        context,
                        "PDF saved to ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Error saving PDF: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                doc.close()
            }
        }


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                activity?.let {
                    ActivityCompat.requestPermissions(
                        it,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_WRITE_EXTERNAL_STORAGE
                    )
                }
                return
            }
        }


        generateAndSavePdf()
    }


    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        context: Context,
        fileName: String,
        tasks: List<Task>,
        startDate: java.time.LocalDate,
        viewMode: ViewMode
    ) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPdf(context, fileName, tasks, startDate, viewMode)
            } else {
                Toast.makeText(
                    context,
                    "Permission denied: cannot save PDF on this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
