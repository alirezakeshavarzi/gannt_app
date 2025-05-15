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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearCalendar(
    tasks: List<Task>,
    referenceDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onYearClick: (LocalDate) -> Unit,
    onTaskClick: (Int) -> Unit
) {
    val startYear = referenceDate.year
    val years = (startYear until startYear + 10).toList()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val todayYear = LocalDate.now().year

    Column(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.Default.ArrowBack, contentDescription = "Prev") }
            Text(text = "$startYear – ${'$'}{startYear + 9}", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForward, contentDescription = "Next") }
        }
        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

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
                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable { onYearClick(LocalDate.of(y, 1, 1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = y.toString(),
                            style = if (isCurrent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Divider(Modifier.padding(vertical = 4.dp))

        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
                .pointerInput(Unit) { detectHorizontalDragGestures { _, drag -> if (drag > 100) onNext() else if (drag < -100) onPrev() } }
        ) {
            Column(
                Modifier
                    .horizontalScroll(hScroll)
                    .fillMaxWidth()
            ) {
                tasks.forEachIndexed { index, task ->
                    val s = LocalDate.parse(task.start, formatter)
                    val e = LocalDate.parse(task.end, formatter)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { onTaskClick(index) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .width(80.dp)
                                .padding(4.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(task.name, style = MaterialTheme.typography.bodyMedium)
                        }
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
                                        if (inRange) Color(android.graphics.Color.parseColor(task.color)) else Color.Transparent,
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