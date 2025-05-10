package com.example.ganttapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val tasks = remember { mutableStateListOf<Task>() }
                var showDialog by remember { mutableStateOf(false) }
                var startDate by remember { mutableStateOf(LocalDate.now()) }
                var viewMode by remember { mutableStateOf(ViewMode.WEEK) }

                LaunchedEffect(Unit) {
                    DataStoreManager.getTasksFlow(applicationContext)
                        .collectLatest {
                            tasks.clear()
                            tasks.addAll(it)
                        }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            NavigationDrawerItem(
                                label = { Text("Day") },
                                selected = viewMode == ViewMode.DAY,
                                onClick = {
                                    viewMode = ViewMode.DAY
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.CalendarToday, null) }
                            )
                            NavigationDrawerItem(
                                label = { Text("3 Days") },
                                selected = viewMode == ViewMode.THREE_DAYS,
                                onClick = {
                                    viewMode = ViewMode.THREE_DAYS
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.DateRange, null) }
                            )
                            NavigationDrawerItem(
                                label = { Text("Week") },
                                selected = viewMode == ViewMode.WEEK,
                                onClick = {
                                    viewMode = ViewMode.WEEK
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.CalendarViewWeek, null) }
                            )
                            NavigationDrawerItem(
                                label = { Text("Month") },
                                selected = viewMode == ViewMode.MONTH,
                                onClick = {
                                    viewMode = ViewMode.MONTH
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.CalendarMonth, null) }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Gantt Chart") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        startDate = LocalDate.now()
                                        viewMode = ViewMode.WEEK
                                    }) {
                                        Icon(Icons.Default.CalendarToday, "Today")
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
                            when (viewMode) {
                                ViewMode.MONTH -> MonthCalendar(
                                    referenceDate = startDate,
                                    onPrev = { startDate = startDate.minusMonths(1) },
                                    onNext = { startDate = startDate.plusMonths(1) },
                                    onDateClick = { date ->
                                        startDate = date
                                        viewMode = ViewMode.DAY
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
                                    }
                                )
                            }

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
