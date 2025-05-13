package com.example.ganttapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeekGanttChart(
    tasks: List<Task>,
    startDate: LocalDate,
    viewMode: ViewMode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onTaskClick: (Int) -> Unit
) {
    val days = when (viewMode) {
        ViewMode.DAY        -> listOf(startDate)
        ViewMode.THREE_DAYS -> (0..2).map { startDate.plusDays(it.toLong()) }
        ViewMode.WEEK       -> (0..6).map { startDate.plusDays(it.toLong()) }
        else                -> emptyList()
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectHorizontalDragGestures { _, drag -> if (drag > 100) onPrev() else if (drag < -100) onNext() } }
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(80.dp))
            days.forEach { day ->
                Text(day.dayOfMonth.toString(), fontSize = 12.sp, modifier = Modifier.weight(1f).clickable { onDateClick(day) })
            }
        }
        Divider(Modifier.padding(vertical = 4.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            tasks.forEachIndexed { index, task ->
                val s = LocalDate.parse(task.start, formatter)
                val e = LocalDate.parse(task.end, formatter)
                Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(80.dp)
                            .padding(4.dp)
                            .background(Color.LightGray, RoundedCornerShape(20.dp))
                            .clickable { onTaskClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(task.name, fontSize = 12.sp)
                    }
                    days.forEach { day ->
                        val inRange = day in s..e
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp)
                                .background(if (inRange) Color(android.graphics.Color.parseColor(task.color)) else Color.Transparent, RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.LightGray)
                                .clickable { onTaskClick(index) }
                        )
                    }
                }
            }
        }
    }
}