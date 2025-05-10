package com.example.ganttapp

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Task(
    val name: String,
    val start: String,    // فرمت "yyyy-MM-dd"
    val end: String,
    val color: String     // مثلا "#FFA726"
) {
    fun startDate(): LocalDate = LocalDate.parse(start, DateTimeFormatter.ISO_DATE)
    fun endDate(): LocalDate = LocalDate.parse(end, DateTimeFormatter.ISO_DATE)
}
