package com.example.ganttapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun MonthCalendar(
    referenceDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: (LocalDate) -> Unit = {}
) {
    val firstOfMonth = referenceDate.withDayOfMonth(1)
    val monthLength = referenceDate.lengthOfMonth()
    val firstDowValue = firstOfMonth.dayOfWeek.value % 7
    val totalCells = ((firstDowValue + monthLength + 6) / 7) * 7
    val dates = List<LocalDate?>(totalCells) { idx ->
        val di = idx - firstDowValue
        if (di in 0 until monthLength) firstOfMonth.plusDays(di.toLong()) else null
    }
    val dowLabels = DayOfWeek.values().toList().let {
        listOf(it[6]) + it.subList(0, 6)
    }.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, d ->
                    if (d > 100) onPrev()
                    if (d < -100) onNext()
                }
            }
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.Default.ArrowBack, null) }
            Text(
                "${referenceDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${referenceDate.year}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForward, null) }
        }
        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            dowLabels.forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(it, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        val today = LocalDate.now()
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val m = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .then(if (date != null) Modifier.clickable { onDateClick(date) } else Modifier)
                    Box(m, contentAlignment = Alignment.TopCenter) {
                        date?.let {
                            val isToday = it == today
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(
                                        if (isToday) Color(0xFF3F51B5) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isToday) Color.Transparent else Color.LightGray,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    it.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    color = if (isToday) Color.White else Color.Black,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
