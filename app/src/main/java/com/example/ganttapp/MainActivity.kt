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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                val tasks = remember { mutableStateListOf<Task>() }

                val activity = LocalContext.current as Activity

                var showAddDialog by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var editingTaskIndex by remember { mutableStateOf(-1) }
                var startDate by remember { mutableStateOf(LocalDate.now()) }
                var viewMode by remember { mutableStateOf(ViewMode.WEEK) }

                var showSearchDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }

                var showSaveDialog by remember { mutableStateOf(false) }
                var saveFileName by remember { mutableStateOf("gantt_report.pdf") }

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
                            Spacer(modifier = Modifier.height(24.dp))

                            // Title at top of drawer
                            Text(
                                text = "Gantt Chart",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 16.dp)
                            )

                            // List of view modes with custom icons
                            Column {
                                NavigationDrawerItem(
                                    label = { Text("Day") },
                                    selected = viewMode == ViewMode.DAY,
                                    onClick = {
                                        viewMode = ViewMode.DAY
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.ViewDay,
                                            contentDescription = "Day View"
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("3 Days") },
                                    selected = viewMode == ViewMode.THREE_DAYS,
                                    onClick = {
                                        viewMode = ViewMode.THREE_DAYS
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.ViewAgenda,
                                            contentDescription = "Three Days View"
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Week") },
                                    selected = viewMode == ViewMode.WEEK,
                                    onClick = {
                                        viewMode = ViewMode.WEEK
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.ViewWeek,
                                            contentDescription = "Week View"
                                        )
                                    },
                                    modifier = if (viewMode == ViewMode.WEEK) {
                                        Modifier
                                            .padding(horizontal = 8.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    } else {
                                        Modifier.padding(horizontal = 8.dp)
                                    }
                                )

                                NavigationDrawerItem(
                                    label = { Text("Month") },
                                    selected = viewMode == ViewMode.MONTH,
                                    onClick = {
                                        viewMode = ViewMode.MONTH
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarViewMonth,
                                            contentDescription = "Month View"
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Year") },
                                    selected = viewMode == ViewMode.YEAR,
                                    onClick = {
                                        viewMode = ViewMode.YEAR
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Year View"
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // About section replaced with the custom "About" design
                            NavigationDrawerItem(
                                label = { Text("About us") },
                                selected = false,
                                onClick = {
                                    showAboutDialog = true
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CropSquare,
                                        contentDescription = "About"
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
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

                            if (showAboutDialog) {
                                // You can keep or update the existing About dialog content here.
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
                                                Text(text = "• علیرضا کشاورزی")
                                                Text(text = "• مانی مهانفر")
                                                Text(text = "• نیما قدیمی")

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
                                                Text(text = "نسخه‌ی فعلی: 1.1")
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

    private fun saveReportAsPdf(activity: Activity, tasks: List<Task>, inputFileName: String) {
        var fileName = inputFileName.trim()
        if (!fileName.lowercase(Locale.getDefault()).endsWith(".pdf")) {
            fileName += ".pdf"
        }

        val window = activity.window
        val view = window.decorView

        view.post {
            val width = view.width
            val height = view.height
            if (width == 0 || height == 0) {
                Toast.makeText(this, "امکان تولید PDF در حال حاضر وجود ندارد.", Toast.LENGTH_SHORT).show()
                return@post
            }

            val chartBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val location = IntArray(2)
                view.getLocationInWindow(location)
                val rect = Rect(location[0], location[1], location[0] + width, location[1] + height)

                PixelCopy.request(window, rect, chartBitmap, { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        createPdfDocument(activity, tasks, chartBitmap, fileName)
                    } else {
                        Toast.makeText(this, "خطا در گرفتن نمودار ($copyResult)", Toast.LENGTH_LONG).show()
                    }
                }, Handler(Looper.getMainLooper()))
            } else {
                val canvas = Canvas(chartBitmap)
                view.draw(canvas)
                createPdfDocument(activity, tasks, chartBitmap, fileName)
            }
        }
    }

    private fun createPdfDocument(
        activity: Activity,
        tasks: List<Task>,
        chartBitmap: Bitmap,
        fileName: String
    ) {
        val today = LocalDate.now()
        val formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        val persianDate = convertLatinDigitsToPersian(formattedDate)

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

        val headerHeight = 180
        val rowHeight = 60
        val tableHeaderHeight = rowHeight
        val tableHeight = tableHeaderHeight + tasks.size * rowHeight
        val dividerHeight = 60
        val ganttLabelHeight = 40
        val bottomMargin = 40
        val pdfWidth = chartBitmap.width
        val pdfHeight = headerHeight + tableHeight + dividerHeight + ganttLabelHeight + chartBitmap.height + bottomMargin

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val centerX = (pdfWidth / 2).toFloat()

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

        var currentY = startTableY + rowHeight
        smallPaint.textSize = 24f
        tasks.forEachIndexed { index, task ->
            val rowNumPersian = convertLatinDigitsToPersian((index + 1).toString())

            val startPersian = convertLatinDigitsToPersian(
                task.startDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            )
            val endPersian = convertLatinDigitsToPersian(
                task.endDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            )

            val days = ChronoUnit.DAYS.between(task.startDate(), task.endDate()).toInt()
            val durationPersian = "${convertLatinDigitsToPersian(days.toString())} روز"

            canvas.drawText(rowNumPersian, col1X, currentY, smallPaint)
            canvas.drawText(task.name, col2X, currentY, smallPaint)
            canvas.drawText(startPersian, col3X, currentY, smallPaint)
            canvas.drawText(endPersian, col4X, currentY, smallPaint)
            canvas.drawText(durationPersian, col5X, currentY, smallPaint)

            currentY += rowHeight
        }

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        canvas.drawText(
            "───────────────────────────────────────────────",
            centerX,
            currentY + 40f,
            paint
        )
        currentY += dividerHeight

        canvas.drawText("Gantt Chart", centerX, currentY.toFloat(), paint)
        currentY += ganttLabelHeight

        canvas.drawBitmap(chartBitmap, 0f, currentY.toFloat(), null)

        document.finishPage(page)

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
