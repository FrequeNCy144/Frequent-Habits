package com.example.ui

import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun HabitsViewModel.navigateCalendarMonth(offsetDelta: Int) {
    _calendarMonthOffset.update { (it + offsetDelta).coerceAtMost(0) }
}

fun HabitsViewModel.selectDate(date: String) {
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    if (date > todayStr) return
    val minDate = getMinDateStr()
    if (date < minDate) return
    _selectedDate.value = date
}

fun HabitsViewModel.selectDateAndSyncWeek(dateStr: String) {
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    if (dateStr > todayStr) return
    val minDate = getMinDateStr()
    if (dateStr < minDate) return
    _selectedDate.value = dateStr
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    try {
        val date = sdfDb.parse(dateStr)
        if (date != null) {
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                time = date
                val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            _currentWeekStart.value = cal
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun HabitsViewModel.getMinWeekStartMillis(): Long {
    val habits = allHabits.value + archivedHabits.value
    var earliestMs = if (habits.isNotEmpty()) {
        habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
    } else {
        System.currentTimeMillis()
    }
    val logs = allLogs.value
    if (logs.isNotEmpty()) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val earliestLogMs = logs.mapNotNull { log ->
            try {
                sdf.parse(log.date)?.time
            } catch (e: Exception) {
                null
            }
        }.minOrNull()
        if (earliestLogMs != null) {
            earliestMs = minOf(earliestMs, earliestLogMs)
        }
    }
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = earliestMs
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun HabitsViewModel.getMinDateStr(): String {
    val habits = allHabits.value + archivedHabits.value
    var earliestMs = if (habits.isNotEmpty()) {
        habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
    } else {
        System.currentTimeMillis()
    }
    val logs = allLogs.value
    if (logs.isNotEmpty()) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val earliestLogMs = logs.mapNotNull { log ->
            try {
                sdf.parse(log.date)?.time
            } catch (e: Exception) {
                null
            }
        }.minOrNull()
        if (earliestLogMs != null) {
            earliestMs = minOf(earliestMs, earliestLogMs)
        }
    }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(earliestMs))
}

fun HabitsViewModel.adjustDateAndWeekIfOutOfRange() {
    val minDate = getMinDateStr()
    val currentSelected = _selectedDate.value
    if (currentSelected.compareTo(minDate) < 0) {
        _selectedDate.value = minDate
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val parsed = sdfDb.parse(minDate)
            if (parsed != null) {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    time = parsed
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                _currentWeekStart.value = cal
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun HabitsViewModel.selectHabitForDetail(habitId: Int?) {
    _calendarMonthOffset.value = 0
    _selectedHabitIdForDetail.value = habitId
}

fun HabitsViewModel.nextWeek() {
    updateWeekAndSelectedDate(1)
}

fun HabitsViewModel.prevWeek() {
    updateWeekAndSelectedDate(-1)
}

fun HabitsViewModel.updateWeekAndSelectedDate(weeksDelta: Int) {
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val currentSelected = _selectedDate.value
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
    }
    try {
        val date = sdfDb.parse(currentSelected)
        if (date != null) {
            cal.time = date
        }
    } catch (e: Exception) {}

    cal.add(Calendar.DAY_OF_YEAR, weeksDelta * 7)
    val targetDate = cal.time
    val targetDateStr = sdfDb.format(targetDate)

    val mondayCal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        time = targetDate
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val maxWeekStart = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val minWeekStart = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = getMinWeekStartMillis()
    }
    if (mondayCal.timeInMillis > maxWeekStart.timeInMillis || mondayCal.timeInMillis < minWeekStart.timeInMillis) {
        return
    }

    val todayCal = Calendar.getInstance()
    val todayStr = sdfDb.format(todayCal.time)
    val weekStartTest = mondayCal.clone() as Calendar
    var todayInWeek: String? = null
    for (i in 0 until 7) {
        val dStr = sdfDb.format(weekStartTest.time)
        if (dStr == todayStr) {
            todayInWeek = dStr
        }
        weekStartTest.add(Calendar.DAY_OF_YEAR, 1)
    }
    val dateToSelect = todayInWeek ?: targetDateStr
    _selectedDate.value = dateToSelect
    _currentWeekStart.value = mondayCal
}
