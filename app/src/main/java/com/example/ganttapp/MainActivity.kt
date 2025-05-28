package com.example.ganttapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ganttapp.DataStoreManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@SuppressLint("CoroutineCreationDuringComposition", "UnusedMaterialScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {

    // Launcher for WRITE_EXTERNAL_STORAGE permission (needed on API < 29)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "مجوز ذخیره‌سازی داده نشد. برای ذخیره‌ی PDF باید مجوز دهید.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // اگر SDK < 29 است، مجوز WRITE_EXTERNAL_STORAGE را درخواست کن
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                val tasks = remember { mutableStateListOf<Task>() }

                // Context را یک بار بگیر و به Activity تبدیل کن
                val activity = LocalContext.current as Activity

                // حالت‌های اصلی
                var showAddDialog by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var editingTaskIndex by remember { mutableStateOf(-1) }
                var startDate by remember { mutableStateOf(LocalDate.now()) }
                var viewMode by remember { mutableStateOf(ViewMode.WEEK) }

                var showSearchDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }

                // حالت برای ذخیره PDF
                var showSaveDialog by remember { mutableStateOf(false) }
                var saveFileName by remember { mutableStateOf("gantt_report.pdf") }

                // حالت برای «درباره ما»
                var showAboutDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    DataStoreManager.getTasksFlow(applicationContext).collectLatest {
                        tasks.clear()
                        tasks.addAll(it)
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(16.dp))

                            // لیست ViewModeها
                            ViewMode.values().forEach { mode ->
                                NavigationDrawerItem(
                                    label = { Text(mode.name.replace("_", " ")) },
                                    selected = mode == viewMode,
                                    onClick = {
                                        viewMode = mode
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = mode.name) }
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // آیتم «درباره ما»
                            NavigationDrawerItem(
                                label = { Text("درباره ما") },
                                selected = false,
                                onClick = {
                                    showAboutDialog = true
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Info, contentDescription = "About Us") }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Gantt Chart")
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = startDate.format(monthFormatter),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                actions = {
                                    // آیکون دانلود PDF
                                    IconButton(onClick = { showSaveDialog = true }) {
                                        Icon(Icons.Default.FileDownload, contentDescription = "Save PDF")
                                    }
                                    IconButton(onClick = {
                                        startDate = LocalDate.now()
                                        viewMode = ViewMode.WEEK
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Today")
                                    }
                                    IconButton(onClick = { showSearchDialog = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search tasks")
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add task")
                            }
                        }
                    ) { paddingValues ->
                        Box(Modifier.padding(paddingValues)) {
                            when (viewMode) {
                                ViewMode.MONTH -> MonthCalendar(
                                    tasks = tasks,
                                    referenceDate = startDate,
                                    onPrev = { startDate = startDate.minusYears(1) },
                                    onNext = { startDate = startDate.plusYears(1) },
                                    onMonthClick = { startDate = it },
                                    onTaskClick = { idx ->
                                        editingTaskIndex = idx
                                        showEditDialog = true
                                    }
                                )
                                ViewMode.YEAR -> YearCalendar(
                                    tasks = tasks,
                                    referenceDate = startDate,
                                    onPrev = { startDate = startDate.minusYears(10) },
                                    onNext = { startDate = startDate.plusYears(10) },
                                    onYearClick = {
                                        startDate = it
                                        viewMode = ViewMode.MONTH
                                    },
                                    onTaskClick = { idx ->
                                        editingTaskIndex = idx
                                        showEditDialog = true
                                    }
                                )
                                else -> WeekGanttChart(
                                    tasks = tasks,
                                    startDate = startDate,
                                    viewMode = viewMode,
                                    onPrev = {
                                        startDate = when (viewMode) {
                                            ViewMode.DAY        -> startDate.minusDays(1)
                                            ViewMode.THREE_DAYS -> startDate.minusDays(3)
                                            ViewMode.WEEK       -> startDate.minusWeeks(1)
                                            else                -> startDate
                                        }
                                    },
                                    onNext = {
                                        startDate = when (viewMode) {
                                            ViewMode.DAY        -> startDate.plusDays(1)
                                            ViewMode.THREE_DAYS -> startDate.plusDays(3)
                                            ViewMode.WEEK       -> startDate.plusWeeks(1)
                                            else                -> startDate
                                        }
                                    },
                                    onDateClick = { date ->
                                        startDate = date
                                        viewMode = ViewMode.DAY
                                    },
                                    onTaskClick = { idx ->
                                        editingTaskIndex = idx
                                        showEditDialog = true
                                    }
                                )
                            }

                            // دیالوگ اضافه کردن تسک
                            if (showAddDialog) {
                                AddTaskDialog(
                                    existingTasks = tasks,
                                    onAdd = { newTask ->
                                        tasks.add(newTask)
                                        scope.launch { DataStoreManager.saveTasks(applicationContext, tasks) }
                                        showAddDialog = false
                                    },
                                    onDismiss = { showAddDialog = false }
                                )
                            }

                            // دیالوگ ویرایش تسک
                            if (showEditDialog && editingTaskIndex >= 0) {
                                EditTaskDialog(
                                    task = tasks[editingTaskIndex],
                                    onUpdate = { updated ->
                                        tasks[editingTaskIndex] = updated
                                        scope.launch { DataStoreManager.saveTasks(applicationContext, tasks) }
                                        showEditDialog = false
                                    },
                                    onDelete = {
                                        tasks.removeAt(editingTaskIndex)
                                        scope.launch { DataStoreManager.saveTasks(applicationContext, tasks) }
                                        showEditDialog = false
                                    },
                                    onDismiss = { showEditDialog = false }
                                )
                            }

                            // دیالوگ جست‌وجو
                            if (showSearchDialog) {
                                SearchTaskDialog(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    tasks = tasks,
                                    onSelect = { idx ->
                                        editingTaskIndex = idx
                                        showEditDialog = true
                                        showSearchDialog = false
                                        searchQuery = ""
                                    },
                                    onDismiss = {
                                        showSearchDialog = false
                                        searchQuery = ""
                                    }
                                )
                            }

                            // دیالوگ ذخیره PDF
                            if (showSaveDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSaveDialog = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showSaveDialog = false
                                            saveReportAsPdf(activity, tasks, saveFileName)
                                        }) {
                                            Text("ذخیره")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showSaveDialog = false
                                            saveFileName = "gantt_report.pdf"
                                        }) {
                                            Text("انصراف")
                                        }
                                    },
                                    title = { Text("ذخیره گزارش PDF") },
                                    text = {
                                        Column {
                                            OutlinedTextField(
                                                value = saveFileName,
                                                onValueChange = { saveFileName = it },
                                                label = { Text("نام فایل") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "پسوند فایل باید .pdf باشد.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                )
                            }

                            // دیالوگ «درباره ما»
                            if (showAboutDialog) {
                                AlertDialog(
                                    onDismissRequest = { showAboutDialog = false },
                                    confirmButton = {
                                        TextButton(onClick = { showAboutDialog = false }) {
                                            Text("باشه")
                                        }
                                    },
                                    text = {
                                        CompositionLocalProvider(
                                            LocalLayoutDirection provides LayoutDirection.Rtl
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = "درباره ما",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "این اپلیکیشن به‌عنوان پروژه‌ی درسی درس «اصول طراحی نرم‌افزار» " +
                                                            "در دانشگاه زنجان، دانشکده‌ی علوم کامپیوتر و ریاضی " +
                                                            "طراحی و توسعه یافته است. هدف ما ارائه‌ی یک ابزار ساده و کارآمد " +
                                                            "برای برنامه‌ریزی و مدیریت وظایف به صورت نمودار گانت است تا " +
                                                            "دانشجویان و متخصصان بتوانند به راحتی مراحل و زمان‌بندی پروژه‌های " +
                                                            "خود را ترسیم و پیگیری کنند."
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = "تیم توسعه",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(text = "• نیما قدیمی")
                                                Text(text = "• مانی مهانفر")
                                                Text(text = "• علیرضا کشاورزی")

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = "استاد راهنما",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "دکتر احمد علیاری"
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "از همه‌ی عزیزانی که در ارزیابی، تست و بهبود این اپلیکیشن " +
                                                            "ما را همراهی کردند، صمیمانه سپاس‌گزاریم."
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = "مشخصات نسخه",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(text = "نسخه‌ی فعلی: 1.0.0")
                                                Text(text = "تاریخ انتشار: خرداد ۱۴۰۴")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * تابعی برای تولید PDF که شامل:
     * 1. سربرگ گزارش با تاریخ تولید (به فارسی)
     * 2. جدول وظایف (ردیف، نام کار، تاریخ شروع، تاریخ پایان، مدت زمان)
     * 3. متن "Gantt Chart"
     * 4. نمودار گانت (گرفته‌شده با PixelCopy یا رسم مستقیم در API<26)
     *
     * مسیر ذخیره: /sdcard/Android/data/com.example.ganttapp/files/Documents/<fileName>
     */
    private fun saveReportAsPdf(activity: Activity, tasks: List<Task>, inputFileName: String) {
        // اطمینان از پسوند .pdf
        var fileName = inputFileName.trim()
        if (!fileName.lowercase(Locale.getDefault()).endsWith(".pdf")) {
            fileName += ".pdf"
        }

        val window = activity.window
        val view = window.decorView

        // صبر کن تا View اندازه‌اش مشخص شود
        view.post {
            val width = view.width
            val height = view.height
            if (width == 0 || height == 0) {
                Toast.makeText(this, "امکان تولید PDF در حال حاضر وجود ندارد.", Toast.LENGTH_SHORT).show()
                return@post
            }

            // Bitmap خالی برای کپی محتوا
            val chartBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // برای API 26 به بالا از PixelCopy استفاده می‌کنیم
                val location = IntArray(2)
                view.getLocationInWindow(location)
                val rect = Rect(location[0], location[1], location[0] + width, location[1] + height)

                PixelCopy.request(window, rect, chartBitmap, { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        // پس از گرفتن نمودار، PDF را تولید کن
                        createPdfDocument(activity, tasks, chartBitmap, fileName)
                    } else {
                        Toast.makeText(this, "خطا در گرفتن نمودار ($copyResult)", Toast.LENGTH_LONG).show()
                    }
                }, Handler(Looper.getMainLooper()))
            } else {
                // برای API <26، مستقیماً View را روی Canvas می‌کشیم
                val canvas = Canvas(chartBitmap)
                view.draw(canvas)
                createPdfDocument(activity, tasks, chartBitmap, fileName)
            }
        }
    }

    /**
     * ایجاد یک PdfDocument بر اساس:
     * - لیست تسک‌ها
     * - Bitmap نمودار
     * - نام فایل
     *
     * سپس فایل را در:
     * /sdcard/Android/data/com.example.ganttapp/files/Documents/
     * ذخیره می‌کند.
     */
    private fun createPdfDocument(
        activity: Activity,
        tasks: List<Task>,
        chartBitmap: Bitmap,
        fileName: String
    ) {
        // تبدیل تاریخ تولید گزارش به فرمت فارسی (Jalali فرض شده نیست؛ فقط جایگزینی ارقام)
        val today = LocalDate.now()
        val formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        val persianDate = convertLatinDigitsToPersian(formattedDate)

        // تنظیمات متن و جدول
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        val smallPaint = Paint().apply {
            isAntiAlias = true
            textSize = 24f
            textAlign = Paint.Align.LEFT
        }

        // محاسبه ارتفاع‌های مورد نیاز
        val headerHeight = 180          // فضای مورد نیاز برای سربرگ (متون و خط تیره)
        val rowHeight = 60              // ارتفاع هر سطر جدول
        val tableHeaderHeight = rowHeight
        val tableHeight = tableHeaderHeight + tasks.size * rowHeight
        val dividerHeight = 60          // فاصله قبل از متن "Gantt Chart"
        val ganttLabelHeight = 40       // ارتفاع متن "Gantt Chart"
        val bottomMargin = 40           // فاصله نهایی
        val pdfWidth = chartBitmap.width
        val pdfHeight = headerHeight + tableHeight + dividerHeight + ganttLabelHeight + chartBitmap.height + bottomMargin

        // ساخت PdfDocument
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val centerX = (pdfWidth / 2).toFloat()

        // رسم سربرگ
        canvas.drawText(
            "───────────────────────────────────────────────",
            centerX,
            50f,
            paint
        )
        canvas.drawText(
            "گزارش نمودار گانت",
            centerX,
            100f,
            paint
        )
        canvas.drawText(
            "تاریخ تولید گزارش: $persianDate",
            centerX,
            150f,
            paint
        )
        canvas.drawText(
            "───────────────────────────────────────────────",
            centerX,
            200f,
            paint
        )

        // رسم جدول (عنوان ستون‌ها)
        val startTableY = headerHeight.toFloat()
        val col1X = 20f
        val col2X = 120f
        val col3X = 420f
        val col4X = 650f
        val col5X = 880f

        smallPaint.textSize = 28f
        canvas.drawText("ردیف", col1X, startTableY, smallPaint)
        canvas.drawText("نام کار", col2X, startTableY, smallPaint)
        canvas.drawText("تاریخ شروع", col3X, startTableY, smallPaint)
        canvas.drawText("تاریخ پایان", col4X, startTableY, smallPaint)
        canvas.drawText("مدت زمان", col5X, startTableY, smallPaint)

        // رسم سطرهای داده
        var currentY = startTableY + rowHeight
        smallPaint.textSize = 24f
        tasks.forEachIndexed { index, task ->
            // ردیف فارسی
            val rowNumPersian = convertLatinDigitsToPersian((index + 1).toString())
            // تبدیل تاریخ کار به yyyy/MM/dd و سپس جایگزینی ارقام
            val startPersian = convertLatinDigitsToPersian(task.startDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")))
            val endPersian = convertLatinDigitsToPersian(task.endDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")))
            // محاسبه فاصله روزها و تبدیل به فارسی
            val days = ChronoUnit.DAYS.between(task.startDate(), task.endDate()).toInt()
            val durationPersian = "${convertLatinDigitsToPersian(days.toString())} روز"

            canvas.drawText(rowNumPersian, col1X, currentY, smallPaint)
            canvas.drawText(task.name, col2X, currentY, smallPaint)
            canvas.drawText(startPersian, col3X, currentY, smallPaint)
            canvas.drawText(endPersian, col4X, currentY, smallPaint)
            canvas.drawText(durationPersian, col5X, currentY, smallPaint)

            currentY += rowHeight
        }

        // رسم خط جداکننده قبل از متن "Gantt Chart"
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        canvas.drawText(
            "───────────────────────────────────────────────",
            centerX,
            currentY + 40f,
            paint
        )
        currentY += dividerHeight

        // رسم متن "Gantt Chart"
        canvas.drawText("Gantt Chart", centerX, currentY.toFloat(), paint)
        currentY += ganttLabelHeight

        // رسم نمودار گانت (Bitmap)
        canvas.drawBitmap(chartBitmap, 0f, currentY.toFloat(), null)

        // پایان صفحه و ذخیره سند
        document.finishPage(page)

        // ذخیره در پوشه‌ی Documents داخل فضای برنامه
        val docsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (docsDir == null) {
            Toast.makeText(this, "امکان دسترسی به Documents برنامه وجود ندارد.", Toast.LENGTH_LONG).show()
            return
        }
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
        val outFile = File(docsDir, fileName)
        try {
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
            Toast.makeText(
                this,
                "PDF با موفقیت ذخیره شد:\n${outFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "خطا در ذخیره PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }

    /**
     * جایگزینی ارقام لاتین با ارقام فارسی برای نمایش در متن فارسی
     */
    private fun convertLatinDigitsToPersian(input: String): String {
        val map = mapOf(
            '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
            '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
        )
        val builder = StringBuilder()
        for (ch in input) {
            builder.append(map[ch] ?: ch)
        }
        return builder.toString()
    }
}

// بخش SearchTaskDialog بدون تغییر
@Composable
fun SearchTaskDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    tasks: List<Task>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val results = remember(query, tasks) {
        if (query.isBlank()) emptyList()
        else tasks.mapIndexed { index, task -> index to task }
            .filter { it.second.name.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        },
        title = { Text("جست‌وجو در تسک‌ها") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("عبارت جست‌وجو") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty() && query.isNotBlank()) {
                    Text("هیچ تسکی یافت نشد.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(results) { (idx, task) ->
                            Text(
                                text = task.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(idx) }
                                    .padding(8.dp)
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    )
}
