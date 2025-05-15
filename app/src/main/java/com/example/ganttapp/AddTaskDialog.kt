package com.example.ganttapp

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    existingTasks: List<Task>,
    onAdd: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    // Default dates: today and tomorrow
    val today = LocalDate.now()
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plusDays(1)) }
    // Default color: first in palette
    val palette = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#3F51B5",
        "#2196F3", "#03A9F4", "#009688", "#4CAF50",
        "#FFEB3B", "#FF9800", "#795548", "#9E9E9E"
    )
    var color by remember { mutableStateOf(palette.first()) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن تسک جدید") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Date selection labels and buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "تاریخ شروع", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newDate = LocalDate.of(y, m + 1, d)
                                    startDate = newDate
                                    if (endDate.isBefore(newDate)) endDate = newDate.plusDays(1)
                                },
                                startDate.year,
                                startDate.monthValue - 1,
                                startDate.dayOfMonth
                            ).apply { datePicker.minDate = 0 }
                                .show()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(startDate.format(formatter))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "تاریخ پایان", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newDate = LocalDate.of(y, m + 1, d)
                                    endDate = newDate
                                    if (endDate.isBefore(startDate)) startDate = newDate.minusDays(1)
                                },
                                endDate.year,
                                endDate.monthValue - 1,
                                endDate.dayOfMonth
                            ).apply { datePicker.minDate = 0 }
                                .show()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(endDate.format(formatter))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("انتخاب رنگ:")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palette.forEach { col ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor(col)),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = if (color == col) 2.dp else 0.dp,
                                    color = Color.Black
                                )
                                .clickable { color = col }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onAdd(
                        Task(
                            name = name,
                            start = startDate.format(formatter),
                            end = endDate.format(formatter),
                            color = color
                        )
                    )
                }
                onDismiss()
            }) { Text("افزودن") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        }
    )
}
