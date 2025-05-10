package com.example.ganttapp

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AddTaskDialog(
    existingTasks: List<Task>,
    onAdd: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var name by remember { mutableStateOf("") }
    // حداقل شروع: یک روز بعد آخرین تسک یا امروز
    val lastEnd = existingTasks
        .maxByOrNull { LocalDate.parse(it.end, formatter) }
        ?.let { LocalDate.parse(it.end, formatter).plusDays(1) }
        ?: LocalDate.now()
    var startDate by remember { mutableStateOf(lastEnd) }
    var endDate by remember { mutableStateOf(lastEnd) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعریف تسک جدید") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("عنوان تسک") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        val dlg = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                startDate = LocalDate.of(y, m + 1, d)
                                if (endDate.isBefore(startDate)) endDate = startDate
                            },
                            startDate.year,
                            startDate.monthValue - 1,
                            startDate.dayOfMonth
                        )
                        dlg.datePicker.minDate = lastEnd
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        dlg.show()
                    }) {
                        Text("شروع: ${startDate.format(formatter)}")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val dlg = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                endDate = LocalDate.of(y, m + 1, d)
                                if (endDate.isBefore(startDate)) startDate = endDate
                            },
                            endDate.year,
                            endDate.monthValue - 1,
                            endDate.dayOfMonth
                        )
                        dlg.datePicker.minDate = startDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        dlg.show()
                    }) {
                        Text("پایان: ${endDate.format(formatter)}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onAdd(Task(name, startDate.format(formatter), endDate.format(formatter), "#FFA726"))
                    onDismiss()
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
