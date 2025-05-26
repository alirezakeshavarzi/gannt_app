package com.example.ganttapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

object PdfExporter {
    // نام درخواست مجوز (برای تشخیص در onRequestPermissionsResult)
    private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1001

    /**
     * این تابع را از داخل یک Activity صدا بزنید.
     * آرگومان context حتما باید Activity باشد (یا لااقل به Activity قابل Cast باشد).
     */
    fun createPdf(
        context: Context,
        fileName: String,
        tasks: List<Task>,
        startDate: java.time.LocalDate,
        viewMode: ViewMode
    ) {
        // اگر Context از نوع Activity نیست، نمی‌توانیم مجوز بخواهیم → به‌صورت خودکار ادامه می‌دهیم
        val activity = context as? Activity

        // تابع داخلی برای تولید و ذخیرهٔ PDF
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
                    // ذخیره با MediaStore در پوشهٔ عمومی Downloads
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        ) // "Download/"
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
                    // **نسخه‌های اندروید پایین‌تر از Q**
                    // اول مطمئن می‌شویم مجوز WRITE_EXTERNAL_STORAGE داریم
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        // اگر Activity موجود باشد، مجوز را درخواست می‌کنیم
                        activity?.let {
                            ActivityCompat.requestPermissions(
                                it,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                REQUEST_WRITE_EXTERNAL_STORAGE
                            )
                        }
                        // پس از درخواست مجوز، کار را در بازگشت (onRequestPermissionsResult) ادامه می‌دهیم
                        doc.close()
                        return
                    }

                    // حالا مسیر عمومیِ Downloads را می‌گیریم
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

                    Toast.makeText(
                        context,
                        "PDF saved to ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_LONG)
                    .show()
            } finally {
                doc.close()
            }
        }

        // اگر نسخه < Q است و هنوز مجوز نداده‌اند، باید صبر کنیم تا در onRequestPermissionsResult بیاید
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                // در صورتی که Activity در دسترس باشد، درخواست مجوز بده و پس از Granted ادامه بده
                activity?.let {
                    ActivityCompat.requestPermissions(
                        it,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_WRITE_EXTERNAL_STORAGE
                    )
                    return
                }
            }
        }

        // اگر همه‌چیز آماده است، مستقیماً PDF را بسازیم و ذخیره کنیم
        generateAndSavePdf()
    }

    /**
     * این تابع را باید درون Activity شما Override کنید تا پس از Grant شدن مجوز،
     * دوباره PDF ذخیره شود.
     */
    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        // پارامترهای بعدی دقیقاً همان چیزهایی هستند که برای createPdf نیاز داریم
        context: Context,
        fileName: String,
        tasks: List<Task>,
        startDate: java.time.LocalDate,
        viewMode: ViewMode
    ) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // مجوز صادر شده؛ حالا PDF را دوباره بساز
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
