package com.example.ganttapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    tasks: List<Task>,
    referenceDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMonthClick: (LocalDate) -> Unit,
    onTaskClick: (Int) -> Unit
) {
    val year = referenceDate.year
    val months = Month.values().toList()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

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
            IconButton(onClick = onPrev) { Icon(Icons.Default.ArrowBack, contentDescription = "Prev year") }
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForward, contentDescription = "Next year") }
        }

        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))


        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(80.dp))
            Row(
                Modifier
                    .horizontalScroll(hScroll)
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                months.forEachIndexed { idx, month ->
                    Box(
                        Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                            .border(0.5.dp, Color.LightGray)
                            .clickable { onMonthClick(LocalDate.of(year, idx + 1, 1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.bodySmall
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
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
            ) {
                tasks.forEachIndexed { index, task ->
                    val start = LocalDate.parse(task.start, formatter)
                    val end = LocalDate.parse(task.end, formatter)

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { onTaskClick(index) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Label background updated: white fill and grey border
                        Box(
                            Modifier
                                .width(80.dp)
                                .padding(4.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(task.name, style = MaterialTheme.typography.bodyMedium)
                        }


                        months.forEachIndexed { idx, _ ->
                            val monthStart = LocalDate.of(year, idx + 1, 1)
                            val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
                            val inRange = !(end < monthStart || start > monthEnd)
                            Box(
                                Modifier
                                    .width(60.dp)
                                    .fillMaxHeight()
                                    .padding(1.dp)
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
}
