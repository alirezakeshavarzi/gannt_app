package com.example.ganttapp

import android.app.DatePickerDialog // For showing native date picker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow // Arranges color swatches
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class) // Enables FlowRow
@Composable
fun EditTaskDialog(
    task: Task,
    onUpdate: (Task) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    // Formatter for date display
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    // State holders for editable fields
    var name by remember { mutableStateOf(task.name) }
    var startDate by remember { mutableStateOf(LocalDate.parse(task.start)) }
    var endDate by remember { mutableStateOf(LocalDate.parse(task.end)) }
    var color by remember { mutableStateOf(task.color) }
    val context = LocalContext.current
    // Color palette swatches
    val palette = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#3F51B5",
        "#2196F3", "#03A9F4", "#009688", "#4CAF50",
        "#FFEB3B", "#FF9800", "#795548", "#9E9E9E"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش تسک") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Editable title field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    // Start date button opens DatePickerDialog
                    Button(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                // Update state and enforce endDate >= startDate
                                startDate = LocalDate.of(y, m + 1, d)
                                if (endDate.isBefore(startDate)) endDate = startDate
                            },
                            startDate.year,
                            startDate.monthValue - 1,
                            startDate.dayOfMonth
                        ).apply { datePicker.minDate = 0 } // Prevent past dates
                            .show()
                    }) {
                        Text(startDate.format(formatter))
                    }
                    Spacer(Modifier.width(8.dp))
                    // End date button with similar logic
                    Button(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                endDate = LocalDate.of(y, m + 1, d)
                                if (endDate.isBefore(startDate)) startDate = endDate
                            },
                            endDate.year,
                            endDate.monthValue - 1,
                            endDate.dayOfMonth
                        ).apply { datePicker.minDate = 0 }
                            .show()
                    }) {
                        Text(endDate.format(formatter))
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Color palette selection row
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
                // Call update callback and dismiss
                onUpdate(Task(name, startDate.format(formatter), endDate.format(formatter), color))
                onDismiss()
            }) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                // Delete button triggers onDelete then dismiss
                TextButton(onClick = {
                    onDelete()
                    onDismiss()
                }) { Text("حذف", color = Color.Red) }
                Spacer(Modifier.width(8.dp))
                // Cancel button
                TextButton(onClick = onDismiss) { Text("لغو") }
            }
        }
    )
}