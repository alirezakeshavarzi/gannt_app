package com.example.ganttapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun YearCalendar(
    tasks: List<Task>,
    referenceDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onYearClick: (LocalDate) -> Unit = {}
) {
    val startYear = referenceDate.year
    // لیست ۱۰ سال متوالی
    val years = (startYear until startYear + 10).toList()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val todayYear = LocalDate.now().year

    Column(Modifier
        .fillMaxSize()
        .padding(8.dp)
    ) {
        // — Header با کنترل ۱۰ سال
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.ArrowBack, contentDescription = "ده سال قبل")
            }
            Text(
                text = "$startYear – ${startYear + 9}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ArrowForward, contentDescription = "ده سال بعد")
            }
        }

        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // — هدر سال‌ها (اسکرول افقی)
        Row {
            Spacer(Modifier.width(80.dp))
            Row(
                Modifier
                    .horizontalScroll(hScroll)
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                years.forEach { y ->
                    val isCurrent = y == todayYear
                    Box(
                        Modifier
                            .width(80.dp)
                            .fillMaxHeight()
                            .background(
                                if (isCurrent) Color(0xFFE3F2FD) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable { onYearClick(LocalDate.of(y, 1, 1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = y.toString(),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Divider(Modifier.padding(vertical = 4.dp))

        // — گانت‌چارت بر مبنای سال
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
        ) {
            Column(
                Modifier
                    .horizontalScroll(hScroll)
                    .fillMaxWidth()
            ) {
                tasks.forEach { task ->
                    val s = LocalDate.parse(task.start, formatter)
                    val e = LocalDate.parse(task.end, formatter)

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ستون نام تسک
                        Box(
                            Modifier
                                .width(80.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(task.name, fontSize = 14.sp)
                        }
                        // ستون‌های سال
                        years.forEach { y ->
                            val yearStart = LocalDate.of(y, 1, 1)
                            val yearEnd = LocalDate.of(y, 12, 31)
                            val inRange = !(e < yearStart || s > yearEnd)
                            Box(
                                Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .padding(1.dp)
                                    .background(
                                        if (inRange)
                                            Color(android.graphics.Color.parseColor(task.color))
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
}
