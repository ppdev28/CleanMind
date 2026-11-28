package com.example.cleannotes.presentation

import com.example.cleannotes.presentation.util.DateUtils
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {
	@Test
	fun cachedFormatsFollowLocaleAndTimezoneAndAreSafeAcrossThreads() {
		val beforeLocale = java.util.Locale.getDefault()
		val beforeZone = java.util.TimeZone.getDefault()
		val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
		try {
			java.util.Locale.setDefault(java.util.Locale.US)
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
			val tasks = (1..100).map { pool.submit<String> { DateUtils.dayKey(0) } }
			assertTrue(tasks.all { it.get() == "1970-01-01" })
			assertEquals("1970-01-01", DateUtils.dayKey(0))
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
			assertEquals("1969-12-31", DateUtils.dayKey(0))
			val english = DateUtils.shortDate(0)
			java.util.Locale.setDefault(java.util.Locale.FRANCE)
			assertNotEquals(english, DateUtils.shortDate(0))
		} finally {
			pool.shutdownNow(); java.util.Locale.setDefault(beforeLocale); java.util.TimeZone.setDefault(
				beforeZone
			)
		}
	}

	@Test
	fun sameDayAndMonthInDifferentYearsHaveDifferentGroups() {
		val date = Calendar.getInstance().apply { set(2025, Calendar.MARCH, 20, 12, 0, 0) }
		val first = DateUtils.dayKey(date.timeInMillis)
		date.set(Calendar.YEAR, 2026)
		assertNotEquals(first, DateUtils.dayKey(date.timeInMillis))
	}

	@Test
	fun todayIsCheckedAgainstTheCalendar() {
		val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
		assertTrue(DateUtils.isToday(System.currentTimeMillis()))
		assertFalse(DateUtils.isToday(yesterday))
		assertTrue(DateUtils.isYesterday(yesterday))
	}
}
