package com.example.cleannotes.presentation

import com.example.cleannotes.presentation.util.DateUtils
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {
    @Test fun sameDayAndMonthInDifferentYearsHaveDifferentGroups() {
        val date = Calendar.getInstance().apply { set(2025, Calendar.MARCH, 20, 12, 0, 0) }
        val first = DateUtils.dayKey(date.timeInMillis)
        date.set(Calendar.YEAR, 2026)
        assertNotEquals(first, DateUtils.dayKey(date.timeInMillis))
    }

    @Test fun todayIsCheckedAgainstTheCalendar() {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        assertTrue(DateUtils.isToday(System.currentTimeMillis()))
        assertFalse(DateUtils.isToday(yesterday))
        assertTrue(DateUtils.isYesterday(yesterday))
    }
}
