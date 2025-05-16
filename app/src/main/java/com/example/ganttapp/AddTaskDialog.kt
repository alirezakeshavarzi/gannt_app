package com.example.ganttapp

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    existingTasks: List<Task>,
    onAdd: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plusDays(1)) }
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth()) {
                    // ورودی عنوان تسک با پشتیبانی RTL و کیبورد فارسی
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("عنوان تسک") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            textAlign = TextAlign.Start,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // انتخاب تاریخ شروع و پایان
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تاریخ شروع", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newDate = LocalDate.of(y, m + 1, d)
                                            startDate = newDate
                                            if (endDate.isBefore(newDate)) {
                                                endDate = newDate.plusDays(1)
                                            }
                                        },
                                        startDate.year,
                                        startDate.monthValue - 1,
                                        startDate.dayOfMonth
                                    ).apply { datePicker.minDate = 0 }
                                        .show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(startDate.format(formatter))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تاریخ پایان", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newDate = LocalDate.of(y, m + 1, d)
                                            endDate = newDate
                                            if (endDate.isBefore(startDate)) {
                                                startDate = newDate.minusDays(1)
                                            }
                                        },
                                        endDate.year,
                                        endDate.monthValue - 1,
                                        endDate.dayOfMonth
                                    ).apply {
                                        datePicker.minDate = startDate
                                            .atStartOfDay(java.time.ZoneId.systemDefault())
                                            .toInstant().toEpochMilli()
                                    }
                                        .show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(endDate.format(formatter))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("انتخاب رنگ:", style = MaterialTheme.typography.bodyMedium)
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
                                    .background(
                                        Color(android.graphics.Color.parseColor(col)),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = if (color == col) 2.dp else 1.dp,
                                        color = if (color == col) Color.Black else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { color = col }
                            )
                        }
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
            }) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}
