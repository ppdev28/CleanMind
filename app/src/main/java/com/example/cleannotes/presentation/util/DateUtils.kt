package com.example.cleannotes.presentation.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun getDayNumber(timestamp: Long): String = format(timestamp, "dd")
    fun getMonthName(timestamp: Long): String = format(timestamp, "MMM").uppercase(Locale.getDefault())
    fun getTime(timestamp: Long): String = format(timestamp, "HH:mm")
    fun dayKey(timestamp: Long): String = format(timestamp, "yyyy-MM-dd")
    fun fullDate(timestamp: Long): String = format(timestamp, "EEEE, d MMMM")
    fun shortDate(timestamp: Long): String = format(timestamp, "d MMM yyyy")
    fun isToday(timestamp: Long): Boolean = dayKey(timestamp) == dayKey(System.currentTimeMillis())
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return dayKey(timestamp) == dayKey(yesterday.timeInMillis)
    }
    private fun format(timestamp: Long, pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
