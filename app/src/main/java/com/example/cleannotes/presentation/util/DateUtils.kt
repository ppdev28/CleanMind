package com.example.cleannotes.presentation.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
	fun getTime(timestamp: Long): String = format(timestamp, "HH:mm")

	fun dayKey(timestamp: Long): String = format(timestamp, "yyyy-MM-dd")

	fun shortDate(timestamp: Long): String = format(timestamp, "d MMM yyyy")

	fun isToday(timestamp: Long): Boolean = dayKey(timestamp) == dayKey(System.currentTimeMillis())

	fun isYesterday(timestamp: Long): Boolean {
		val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
		return dayKey(timestamp) == dayKey(yesterday.timeInMillis)
	}

	private data class Formats(
		val locale: Locale,
		val zone: TimeZone,
		val values: MutableMap<String, SimpleDateFormat> = mutableMapOf()
	)

	private val formats = ThreadLocal<Formats>()

	private fun format(timestamp: Long, pattern: String): String {
		val locale = Locale.getDefault()
		val zone = TimeZone.getDefault()
		val cache = formats.get()?.takeIf { it.locale == locale && it.zone == zone }
			?: Formats(locale, zone).also(formats::set)
		val formatter =
			cache.values.getOrPut(pattern) { SimpleDateFormat(pattern, locale).apply { timeZone = zone } }
		return formatter.format(Date(timestamp))
	}
}
