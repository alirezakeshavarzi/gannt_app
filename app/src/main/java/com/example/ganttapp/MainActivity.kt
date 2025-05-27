package com.example.ganttapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.ganttapp.DataStoreManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("CoroutineCreationDuringComposition")
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {

                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                val tasks = remember { mutableStateListOf<Task>() }

                // حالت‌های اصلی
                var showAddDialog by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var editingTaskIndex by remember { mutableStateOf(-1) }
                var startDate by remember { mutableStateOf(LocalDate.now()) }
                var viewMode by remember { mutableStateOf(ViewMode.WEEK) }

                var showSearchDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }

                // حالت جدید برای دیالوگ «درباره ما»
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
                                    IconButton(onClick = {
                                        startDate = LocalDate.now()
                                        viewMode = ViewMode.WEEK
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Today")
                                    }
                                    // Search
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

                            // دیالوگ ویرایش
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
                                        // ست کردن جهت نوشتار به راست-به-چپ
                                        CompositionLocalProvider(
                                            LocalLayoutDirection provides LayoutDirection.Rtl
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(4.dp)
                                            ) {
                                                // بخش «درباره ما»
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

                                                // بخش «استاد راهنما»
                                                Text(
                                                    text = "استاد راهنما",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "دکتر احمد علی‌یاری"
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // بخش «تیم توسعه»
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

                                                // بخش «سپاس‌گزاری»

                                                Spacer(modifier = Modifier.height(2.dp))
                                                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "از همه‌ی عزیزانی که در ارزیابی، تست و بهبود این اپلیکیشن " +
                                                            "ما را همراهی کردند، صمیمانه سپاس‌گزاریم."
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // بخش «مشخصات نسخه»
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
