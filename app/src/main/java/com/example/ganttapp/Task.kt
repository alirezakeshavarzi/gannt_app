package com.example.ganttapp

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Task(
    val name: String,
    val start: String,
    val end: String,
    val color: String
) {
    fun startDate(): LocalDate = LocalDate.parse(start, DateTimeFormatter.ISO_DATE)
    fun endDate(): LocalDate = LocalDate.parse(end, DateTimeFormatter.ISO_DATE)
}
