package com.example.ganttapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
                var showAddDialog by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var editingTaskIndex by remember { mutableStateOf(-1) }
                var startDate by remember { mutableStateOf(LocalDate.now()) }
                var viewMode by remember { mutableStateOf(ViewMode.WEEK) }


                var showSearchDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }


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
                            ViewMode.values().forEach { mode ->
                                NavigationDrawerItem(
                                    label = { Text(mode.name.replace("_"," ")) },
                                    selected = mode == viewMode,
                                    onClick = {
                                        viewMode = mode
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = mode.name) }
                                )
                            }
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
                        }
                    }
                }
            }
        }
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