package com.example.ui

import com.example.data.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HabitDetailStatsEngine {
    fun calculateHabitDetailUiState(
        habitId: Int?,
        habits: List<Habit>,
        logs: List<HabitLog>,
        lang: String,
        monthOffset: Int
    ): HabitDetailUiState? {
        if (habitId == null) return null
        val habit = habits.find { it.id == habitId } ?: return null
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startDate = Instant.ofEpochMilli(validStartMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val startSdfStr = startDate.toString()
        val completedDateStr = habit.completedAt?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
        }
        val completedLocalDate = completedDateStr?.let {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }
        val useCompletedTime = habit.isFinishable && habit.isCompletedGoal && completedLocalDate != null
        val effectiveLogs = if (useCompletedTime) {
            logs.filter { it.date <= completedDateStr!! }
        } else {
            logs
        }
        val referenceToday = if (useCompletedTime) completedLocalDate!! else LocalDate.now()
        val today = referenceToday
        val diffMonths = ChronoUnit.MONTHS.between(
            startDate.withDayOfMonth(1),
            today.withDayOfMonth(1)
        )
        val minMonthOffset = -diffMonths.toInt()
        val actualOffset = monthOffset.coerceIn(minMonthOffset, 0)
        val canPrevMonth = actualOffset > minMonthOffset
        val canNextMonth = actualOffset < 0
        val (currentStreak, longestStreak) = HabitCalculationEngine.calculateStreak(habit, effectiveLogs, targetDateStr = if (useCompletedTime) completedDateStr else null)
        val strength = HabitCalculationEngine.calculateHabitStrength(habit, effectiveLogs, todayStr = if (useCompletedTime) completedDateStr else null)
        val completionRate = HabitCalculationEngine.calculateCompletionRate(habit, effectiveLogs, todayStr = if (useCompletedTime) completedDateStr else null)
        val thisWeekCount = HabitCalculationEngine.getCompletedLogsCount(habit, effectiveLogs, "WEEK", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val thisMonthCount = HabitCalculationEngine.getCompletedLogsCount(habit, effectiveLogs, "MONTH", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val thisYearCount = HabitCalculationEngine.getCompletedLogsCount(habit, effectiveLogs, "YEAR", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val totalCount = HabitCalculationEngine.getCompletedLogsCount(habit, effectiveLogs, "ALL", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val habitLogs = effectiveLogs.filter { it.habitId == habitId }
        val logsByDate = habitLogs.associateBy { it.date }
        val targetMonthDate = today.withDayOfMonth(1).plusMonths(actualOffset.toLong())
        val monthNameFormatter = DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            when (lang) {
                "de" -> Locale.GERMANY
                "zh" -> Locale.SIMPLIFIED_CHINESE
                else -> Locale.US
            }
        )
        val monthNameStr = targetMonthDate.format(monthNameFormatter)
        val currentMonthDaysCount = targetMonthDate.lengthOfMonth()
        val firstDayOfWeekOffset = (targetMonthDate.dayOfWeek.value - 1)
        val todayStr = today.toString()
        val daysList = mutableListOf<CalendarCellState?>()
        for (i in 0 until firstDayOfWeekOffset) {
            daysList.add(null)
        }
        for (i in 1..currentMonthDaysCount) {
            val dateVal = targetMonthDate.withDayOfMonth(i)
            val dateStr = dateVal.toString()
            val log = logsByDate[dateStr]
            val isCompleted = isLogCompleted(habit, log)
            val status = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)

            daysList.add(CalendarCellState(id = dateStr, dayNum = i.toString(), isCompleted = isCompleted, status = status))
        }
        val targetStats = calculateTargetPeriodStats(habit, effectiveLogs)
        val occurrences = IntArray(7)
        val completions = IntArray(7)
        var currentDay = startDate
        if (!currentDay.isAfter(today)) {
            while (!currentDay.isAfter(today)) {
                val dateStr = currentDay.toString()
                if (isHabitActiveOnDate(habit, dateStr)) {
                    val log = logsByDate[dateStr]
                    val dayOfWeekIndex = currentDay.dayOfWeek.value - 1
                    if (dayOfWeekIndex in 0..6) {
                        occurrences[dayOfWeekIndex]++
                        if (getLogStatus(habit, log, dateStr, startSdfStr, todayStr) == "SUCCESS") {
                            completions[dayOfWeekIndex]++
                        }
                    }
                }
                currentDay = currentDay.plusDays(1)
            }
        }
        val weekdayStats = List(7) { index ->
            val total = occurrences[index]
            val completed = completions[index]
            val pct = if (total > 0) (completed.toFloat() / total.toFloat() * 100).toInt() else 0
            Triple(index, completed, pct)
        }
        val mondayOfCurrentWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val numWeeks = 15
        val weekdayGridData = List(numWeeks) { weekOffset ->
            val weekStart = mondayOfCurrentWeek.minusWeeks((numWeeks - 1 - weekOffset).toLong())
            List(7) { dayOffset ->
                val cellDate = weekStart.plusDays(dayOffset.toLong())
                val dateStr = cellDate.toString()
                val log = logsByDate[dateStr]
                val status = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
                cellDate to status
            }
        }
        val weeksWithMonthLabels = Array(weekdayGridData.size) { "" }
        var lastMonth = -1
        var lastAddedIndex = -10
        weekdayGridData.forEachIndexed { index, weekDays ->
            val monday = weekDays.first().first
            val monthVal = monday.monthValue
            if (monthVal != lastMonth) {
                val formatter = DateTimeFormatter.ofPattern(
                    "MMM",
                    when (lang) {
                        "de" -> Locale.GERMANY
                        "zh" -> Locale.SIMPLIFIED_CHINESE
                        else -> Locale.US
                    }
                )
                val labelStr = monday.format(formatter)
                if (index - lastAddedIndex >= 3) {
                    weeksWithMonthLabels[index] = labelStr
                    lastAddedIndex = index
                } else if (lastAddedIndex == 0) {
                    weeksWithMonthLabels[0] = ""
                    weeksWithMonthLabels[index] = labelStr
                    lastAddedIndex = index
                }
                lastMonth = monthVal
            }
        }
        return HabitDetailUiState(
            habit = habit,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            strength = strength,
            thisWeekCount = thisWeekCount,
            thisMonthCount = thisMonthCount,
            thisYearCount = thisYearCount,
            totalCount = totalCount,
            calendarGridRows = daysList.chunked(7),
            monthName = monthNameStr,
            canPrevMonth = canPrevMonth,
            canNextMonth = canNextMonth,
            completionRate = completionRate,
            targetStats = targetStats,
            weekdayStats = weekdayStats,
            weekdayGridData = weekdayGridData,
            weeksWithMonthLabels = weeksWithMonthLabels.toList()
        )
    }
}