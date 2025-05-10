package com.example.ganttapp
import com.example.ganttapp.ViewMode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
    onDateClick: (LocalDate) -> Unit
) {
    val days = when (viewMode) {
        ViewMode.DAY        -> (0..0).map { startDate.plusDays(it.toLong()) }
        ViewMode.THREE_DAYS -> (0..2).map { startDate.plusDays(it.toLong()) }
        ViewMode.WEEK       -> (0..6).map { startDate.plusDays(it.toLong()) }
        ViewMode.MONTH      -> {
            val first = startDate.withDayOfMonth(1)
            (0 until startDate.lengthOfMonth()).map { first.plusDays(it.toLong()) }
        }

        ViewMode.YEAR -> TODO()
    }
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, drag ->
                    if (drag > 100) onPrev()
                    if (drag < -100) onNext()
                }
            }
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(80.dp))
            days.forEach { day ->
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onDateClick(day) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (viewMode) {
                            ViewMode.WEEK -> day.dayOfWeek.name.first().toString()
                            else           -> ""
                        },
                        fontSize = 12.sp, color = Color.Gray
                    )
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(
                                color = if (day == today) Color(0xFF3F51B5) else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.dayOfMonth.toString(),
                            color = if (day == today) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Divider(Modifier.padding(vertical = 4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            tasks.forEach { task ->
                val s = LocalDate.parse(task.start, formatter)
                val e = LocalDate.parse(task.end, formatter)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(80.dp)
                            .padding(4.dp)
                            .background(Color.LightGray, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(task.name, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                    }
                    days.forEach { day ->
                        val inRange = day in s..e
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp)
                                .background(
                                    if (inRange) Color(android.graphics.Color.parseColor(task.color))
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(0.5.dp, Color.LightGray)
                        )
                    }
                }
            }
        }
    }
}
