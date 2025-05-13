package com.example.ganttapp

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onUpdate: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var name by remember { mutableStateOf(task.name) }
    var startDate by remember { mutableStateOf(LocalDate.parse(task.start)) }
    var endDate by remember { mutableStateOf(LocalDate.parse(task.end)) }
    var color by remember { mutableStateOf(task.color) }
    val context = LocalContext.current
    val palette = listOf(
        "#F44336","#E91E63","#9C27B0","#3F51B5",
        "#2196F3","#03A9F4","#009688","#4CAF50",
        "#FFEB3B","#FF9800","#795548","#9E9E9E"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش تسک") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                startDate = LocalDate.of(y, m + 1, d)
                                if (endDate.isBefore(startDate)) endDate = startDate
                            },
                            startDate.year,
                            startDate.monthValue - 1,
                            startDate.dayOfMonth
                        ).apply { datePicker.minDate = 0 }.show()
                    }) { Text(startDate.format(formatter)) }
                    Spacer(Modifier.width(8.dp))
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
                        ).apply { datePicker.minDate = 0 }.show()
                    }) { Text(endDate.format(formatter)) }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palette.forEach { col ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(Color(android.graphics.Color.parseColor(col)), RoundedCornerShape(4.dp))
                                .border(if (color == col) 2.dp else 0.dp, Color.Black)
                                .clickable { color = col }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(Task(name, startDate.format(formatter), endDate.format(formatter), color))
                onDismiss()
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
    )
}