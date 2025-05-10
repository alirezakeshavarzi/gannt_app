package com.example.ganttapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun MonthCalendar(
    tasks: List<Task>,
    referenceDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMonthClick: (LocalDate) -> Unit = {}
) {
    val year = referenceDate.year
    val months = Month.values().toList()
    val scrollState = rememberScrollState()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val todayMonth = LocalDate.now().monthValue

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
        // Header: کنترل تغییر سال
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.ArrowBack, contentDescription = "سال قبل")
            }
            Text(
                text = year.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ArrowForward, contentDescription = "سال بعد")
            }
        }

        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // دو ردیف ۶ ماه برای خوانایی بهتر
        val firstHalf = months.take(6)
        val secondHalf = months.drop(6)

        listOf(firstHalf, secondHalf).forEach { half ->
            Row(Modifier.fillMaxWidth()) {
                half.forEachIndexed { idx, month ->
                    val monthIndex = if (half === firstHalf) idx + 1 else idx + 7
                    val isCurrent = monthIndex == todayMonth
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                if (isCurrent) Color(0xFFE3F2FD)
                                else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable {
                                onMonthClick(LocalDate.of(year, monthIndex, 1))
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Divider(Modifier.padding(vertical = 4.dp))

        // گانت‌چارت: هر ردیف یک تسک، هر ستون یک ماه
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            tasks.forEach { task ->
                val start = LocalDate.parse(task.start, formatter)
                val end = LocalDate.parse(task.end, formatter)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // نام تسک
                    Box(
                        Modifier
                            .width(80.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(task.name, fontSize = 14.sp)
                    }
                    // ستون‌های ماه
                    months.forEachIndexed { idx, month ->
                        val monthIndex = idx + 1
                        val monthStart = LocalDate.of(year, monthIndex, 1)
                        val monthEnd = monthStart.withDayOfMonth(month.length(year % 4 == 0))
                        val inRange = !(end < monthStart || start > monthEnd)

                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp)
                                .background(
                                    if (inRange) Color(android.graphics.Color.parseColor(task.color))
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(0.5.dp, Color.LightGray)
                        )
                    }
                }
            }
        }
    }
}
