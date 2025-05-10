package com.example.ganttapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)

                // state های اصلی
                val tasks = remember { mutableStateListOf<Task>() }
                var showDialog by remember { mutableStateOf(false) }
                var startDate by remember { mutableStateOf(LocalDate.now()) }
                var viewMode by remember { mutableStateOf(ViewMode.WEEK) }

                // بارگذاری از DataStore
                LaunchedEffect(Unit) {
                    DataStoreManager.getTasksFlow(applicationContext)
                        .collectLatest { saved ->
                            tasks.clear()
                            tasks.addAll(saved)
                        }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text("نمایش:", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                            NavigationDrawerItem(
                                label = { Text("Day") },
                                selected = viewMode == ViewMode.DAY,
                                onClick = {
                                    viewMode = ViewMode.DAY
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                            )
                            NavigationDrawerItem(
                                label = { Text("3 Days") },
                                selected = viewMode == ViewMode.THREE_DAYS,
                                onClick = {
                                    viewMode = ViewMode.THREE_DAYS
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                            )
                            NavigationDrawerItem(
                                label = { Text("Week") },
                                selected = viewMode == ViewMode.WEEK,
                                onClick = {
                                    viewMode = ViewMode.WEEK
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.CalendarViewWeek, contentDescription = null) }
                            )
                            NavigationDrawerItem(
                                label = { Text("Month") },
                                selected = viewMode == ViewMode.MONTH,
                                onClick = {
                                    viewMode = ViewMode.MONTH
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Gantt Chart") },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } }
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Open menu"
                                        )
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { showDialog = true }) {
                                Icon(Icons.Default.Add, "Add Task")
                            }
                        }
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            WeekGanttChart(
                                tasks = tasks,
                                startDate = startDate,
                                viewMode = viewMode,
                                onPrev = {
                                    startDate = when (viewMode) {
                                        ViewMode.DAY        -> startDate.minusDays(1)
                                        ViewMode.THREE_DAYS -> startDate.minusDays(3)
                                        ViewMode.WEEK       -> startDate.minusWeeks(1)
                                        ViewMode.MONTH      -> startDate.minusMonths(1)
                                        ViewMode.YEAR       -> startDate.minusYears(1)
                                    }
                                },
                                onNext = {
                                    startDate = when (viewMode) {
                                        ViewMode.DAY        -> startDate.plusDays(1)
                                        ViewMode.THREE_DAYS -> startDate.plusDays(3)
                                        ViewMode.WEEK       -> startDate.plusWeeks(1)
                                        ViewMode.MONTH      -> startDate.plusMonths(1)
                                        ViewMode.YEAR       -> startDate.plusYears(1)
                                    }
                                },
                                onDateClick = { date ->
                                    // وقتی کاربر روی تاریخ زد، وارد نماى روزانه شو
                                    startDate = date
                                    viewMode = ViewMode.DAY
                                }
                            )

                            if (showDialog) {
                                AddTaskDialog(
                                    existingTasks = tasks,
                                    onAdd = { newTask ->
                                        tasks.add(newTask)
                                        scope.launch {
                                            DataStoreManager.saveTasks(applicationContext, tasks)
                                        }
                                        showDialog = false
                                    },
                                    onDismiss = { showDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
