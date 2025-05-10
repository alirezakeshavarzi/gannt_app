package com.example.ganttapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun YearGanttChart(
    tasks: List<Task>,
    referenceStartYear: Int,
    yearsToShow: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onYearClick: (Int) -> Unit
) {
    val years = (referenceStartYear until referenceStartYear + yearsToShow).toList()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("←", modifier = Modifier.clickable { onPrev() })
            Text("Years ${referenceStartYear} - ${referenceStartYear + yearsToShow - 1}", style = MaterialTheme.typography.titleLarge)
            Text("→", modifier = Modifier.clickable { onNext() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            years.forEach { year ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .clickable { onYearClick(year) }
                ) {
                    Text(text = year.toString(), modifier = Modifier.padding(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        tasks.forEach { task ->
            val taskStartYear = task.startDate().year
            val taskEndYear = task.endDate().year

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(vertical = 4.dp)) {

                val totalWidth = size.width
                val yearWidth = totalWidth / yearsToShow

                val startIndex = taskStartYear - referenceStartYear
                val endIndex = taskEndYear - referenceStartYear

                if (startIndex < yearsToShow && endIndex >= 0) {
                    val left = (startIndex.coerceAtLeast(0)) * yearWidth
                    val right = ((endIndex + 1).coerceAtMost(yearsToShow)) * yearWidth

                    drawRect(
                        color = Color(android.graphics.Color.parseColor(task.color)),
                        topLeft = Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(right - left, size.height)
                    )
                }
            }

            Text(text = task.name)
        }
    }
}
