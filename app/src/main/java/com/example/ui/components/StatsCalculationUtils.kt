package com.example.ui.components

import com.example.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields





fun getWeekOfYear(date: java.time.LocalDate): Int {
    val cal = java.util.Calendar.getInstance()
    cal.set(date.year, date.monthValue - 1, date.dayOfMonth)
    return cal.get(java.util.Calendar.WEEK_OF_YEAR)
}

fun getHabitStartLocalDate(habit: Habit): java.time.LocalDate {
    val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    return try {
        java.time.Instant.ofEpochMilli(validStartMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    } catch (e: Exception) {
        java.time.LocalDate.of(2024, 1, 1)
    }
}

fun calculateHabitAverageScoreInRange(
    habit: Habit,
    logs: List<HabitLog>,
    startDate: java.time.LocalDate,
    endDate: java.time.LocalDate
): Float {
    if (startDate.isAfter(endDate)) return 0f
    val today = java.time.LocalDate.now()
    val actualEnd = if (endDate.isAfter(today)) today else endDate
    
    val habitStartDate = getHabitStartLocalDate(habit)
    val effectiveStart = if (startDate.isBefore(habitStartDate)) habitStartDate else startDate

    if (effectiveStart.isAfter(actualEnd)) return 0f

    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, actualEnd) + 1
    if (totalDays <= 0) return 0f

    val step = if (totalDays > 180) 5L else 1L
    var sum = 0f
    var count = 0
    var curr = effectiveStart
    while (!curr.isAfter(actualEnd)) {
        sum += calculateHabitStrengthOnDate(habit, logs, curr.toString()).toFloat()
        count++
        curr = curr.plusDays(step)
    }
    return if (count > 0) (sum / count).coerceIn(0f, 100f) else 0f
}

fun calculateOverallAverageScoreInRange(
    activeHabits: List<Habit>,
    allLogs: List<HabitLog>,
    startDate: java.time.LocalDate,
    endDate: java.time.LocalDate
): Float {
    if (activeHabits.isEmpty() || startDate.isAfter(endDate)) return 0f
    val today = java.time.LocalDate.now()
    val actualEnd = if (endDate.isAfter(today)) today else endDate
    if (startDate.isAfter(actualEnd)) return 0f

    val habitsWithStart = activeHabits.map { habit ->
        habit to getHabitStartLocalDate(habit)
    }

    val earliestHabitStart = habitsWithStart.minOfOrNull { it.second } ?: startDate
    val effectiveStart = if (startDate.isBefore(earliestHabitStart)) earliestHabitStart else startDate

    if (effectiveStart.isAfter(actualEnd)) return 0f

    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, actualEnd) + 1
    if (totalDays <= 0) return 0f

    val step = if (totalDays > 180) 7L else 1L
    var sum = 0f
    var count = 0
    var curr = effectiveStart
    while (!curr.isAfter(actualEnd)) {
        val currDate = curr
        val habitsOnCurr = habitsWithStart.filter { !it.second.isAfter(currDate) }.map { it.first }
        if (habitsOnCurr.isNotEmpty()) {
            val dateStr = currDate.toString()
            val dailyAvg = habitsOnCurr.map { calculateHabitStrengthOnDate(it, allLogs, dateStr) }.average().toFloat()
            sum += dailyAvg
            count++
        }
        curr = curr.plusDays(step)
    }
    return if (count > 0) (sum / count).coerceIn(0f, 100f) else 0f
}

fun calculateHabitTrendPoints(
    habit: Habit,
    logs: List<HabitLog>,
    timeframeIndex: Int
): List<TrendPoint> {
    val today = java.time.LocalDate.now()
    val points = mutableListOf<TrendPoint>()

    when (timeframeIndex) {
        0 -> { // Weekly
            for (i in 5 downTo 0) {
                val weekStart = today.minusWeeks(i.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weekEnd = weekStart.plusDays(6)
                val score = calculateHabitAverageScoreInRange(habit, logs, weekStart, weekEnd)
                val weekNum = getWeekOfYear(weekStart)
                val label = "KW$weekNum"
                points.add(TrendPoint(label, score, 0))
            }
        }
        1 -> { // Monthly
            for (i in 5 downTo 0) {
                val monthDate = today.minusMonths(i.toLong())
                val startOfMonth = monthDate.withDayOfMonth(1)
                val endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
                val score = calculateHabitAverageScoreInRange(habit, logs, startOfMonth, endOfMonth)
                val monthName = monthDate.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                points.add(TrendPoint(monthName, score, 0))
            }
        }
        2 -> { // Yearly
            for (i in 3 downTo 0) {
                val yearDate = today.minusYears(i.toLong())
                val startOfYear = yearDate.withDayOfYear(1)
                val endOfYear = yearDate.withDayOfYear(yearDate.lengthOfYear())
                val score = calculateHabitAverageScoreInRange(habit, logs, startOfYear, endOfYear)
                val yearLabel = yearDate.year.toString()
                points.add(TrendPoint(yearLabel, score, 0))
            }
        }
    }
    return points
}

fun calculateOverallTrendPoints(
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    timeframeIndex: Int
): List<TrendPoint> {
    val today = java.time.LocalDate.now()
    val points = mutableListOf<TrendPoint>()
    val activeHabits = allHabits.filter { !it.isArchived }

    if (activeHabits.isEmpty()) {
        val labels = when (timeframeIndex) {
            0 -> (5 downTo 0).map { i ->
                val weekStart = today.minusWeeks(i.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                "KW${getWeekOfYear(weekStart)}"
            }
            1 -> (5 downTo 0).map { i ->
                today.minusMonths(i.toLong()).month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
            }
            else -> (3 downTo 0).map { i ->
                today.minusYears(i.toLong()).year.toString()
            }
        }
        return labels.map { TrendPoint(it, 0f, 0) }
    }

    when (timeframeIndex) {
        0 -> { // Weekly
            for (i in 5 downTo 0) {
                val weekStart = today.minusWeeks(i.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weekEnd = weekStart.plusDays(6)
                val score = calculateOverallAverageScoreInRange(activeHabits, allLogs, weekStart, weekEnd)
                val weekNum = getWeekOfYear(weekStart)
                val label = "KW$weekNum"
                points.add(TrendPoint(label, score, 0))
            }
        }
        1 -> { // Monthly
            for (i in 5 downTo 0) {
                val monthDate = today.minusMonths(i.toLong())
                val startOfMonth = monthDate.withDayOfMonth(1)
                val endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
                val score = calculateOverallAverageScoreInRange(activeHabits, allLogs, startOfMonth, endOfMonth)
                val monthName = monthDate.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                points.add(TrendPoint(monthName, score, 0))
            }
        }
        2 -> { // Yearly
            for (i in 3 downTo 0) {
                val yearDate = today.minusYears(i.toLong())
                val startOfYear = yearDate.withDayOfYear(1)
                val endOfYear = yearDate.withDayOfYear(yearDate.lengthOfYear())
                val score = calculateOverallAverageScoreInRange(activeHabits, allLogs, startOfYear, endOfYear)
                val yearLabel = yearDate.year.toString()
                points.add(TrendPoint(yearLabel, score, 0))
            }
        }
    }
    return points
}

fun calculateOverallVolumePoints(
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    timeframeIndex: Int
): List<TrendPoint> {
    val today = java.time.LocalDate.now()
    val points = mutableListOf<TrendPoint>()
    val activeHabits = allHabits.filter { !it.isArchived }

    if (activeHabits.isEmpty()) {
        val labels = when (timeframeIndex) {
            0 -> (5 downTo 0).map { i ->
                val weekStart = today.minusWeeks(i.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                "KW${getWeekOfYear(weekStart)}"
            }
            1 -> (5 downTo 0).map { i ->
                today.minusMonths(i.toLong()).month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
            }
            else -> (3 downTo 0).map { i ->
                today.minusYears(i.toLong()).year.toString()
            }
        }
        return labels.map { TrendPoint(it, 0f, 0) }
    }

    when (timeframeIndex) {
        0 -> { // Weekly (Last 6 weeks)
            for (i in 5 downTo 0) {
                val weekStart = today.minusWeeks(i.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weekEnd = weekStart.plusDays(6)
                
                var totalCompletions = 0
                activeHabits.forEach { habit ->
                    val habitLogs = allLogs.filter { it.habitId == habit.id }
                    habitLogs.forEach { log ->
                        val logDate = try { java.time.LocalDate.parse(log.date) } catch (e: Exception) { null }
                        if (logDate != null && !logDate.isBefore(weekStart) && !logDate.isAfter(weekEnd)) {
                            if (isLogCompleted(habit, log)) {
                                totalCompletions++
                            }
                        }
                    }
                }
                
                val weekNum = getWeekOfYear(weekStart)
                val label = "KW$weekNum"
                points.add(TrendPoint(label, 0f, totalCompletions))
            }
        }
        1 -> { // Monthly (Last 6 months)
            for (i in 5 downTo 0) {
                val monthDate = today.minusMonths(i.toLong())
                val startOfMonth = monthDate.withDayOfMonth(1)
                val endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
                
                var totalCompletions = 0
                activeHabits.forEach { habit ->
                    val habitLogs = allLogs.filter { it.habitId == habit.id }
                    habitLogs.forEach { log ->
                        val logDate = try { java.time.LocalDate.parse(log.date) } catch (e: Exception) { null }
                        if (logDate != null && !logDate.isBefore(startOfMonth) && !logDate.isAfter(endOfMonth)) {
                            if (isLogCompleted(habit, log)) {
                                totalCompletions++
                            }
                        }
                    }
                }
                
                val monthName = monthDate.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                points.add(TrendPoint(monthName, 0f, totalCompletions))
            }
        }
        2 -> { // Yearly (Last 4 years)
            for (i in 3 downTo 0) {
                val yearDate = today.minusYears(i.toLong())
                val startOfYear = yearDate.withDayOfYear(1)
                val endOfYear = yearDate.withDayOfYear(yearDate.lengthOfYear())
                
                var totalCompletions = 0
                activeHabits.forEach { habit ->
                    val habitLogs = allLogs.filter { it.habitId == habit.id }
                    habitLogs.forEach { log ->
                        val logDate = try { java.time.LocalDate.parse(log.date) } catch (e: Exception) { null }
                        if (logDate != null && !logDate.isBefore(startOfYear) && !logDate.isAfter(endOfYear)) {
                            if (isLogCompleted(habit, log)) {
                                totalCompletions++
                            }
                        }
                    }
                }
                
                val yearLabel = yearDate.year.toString()
                points.add(TrendPoint(yearLabel, 0f, totalCompletions))
            }
        }
    }
    return points
}

fun calculateHabitVolumePoints(
    habit: Habit,
    logs: List<HabitLog>,
    timeframeIndex: Int
): List<TrendPoint> {
    val today = java.time.LocalDate.now()
    val points = mutableListOf<TrendPoint>()
    val habitLogs = logs.filter { it.habitId == habit.id }

    when (timeframeIndex) {
        0 -> { // Weekly
            for (i in 5 downTo 0) {
                val weekStart = today.minusWeeks(i.toLong()).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weekEnd = weekStart.plusDays(6)
                
                var totalCompletions = 0
                habitLogs.forEach { log ->
                    val logDate = try { java.time.LocalDate.parse(log.date) } catch (e: Exception) { null }
                    if (logDate != null && !logDate.isBefore(weekStart) && !logDate.isAfter(weekEnd)) {
                        if (isLogCompleted(habit, log)) {
                            totalCompletions++
                        }
                    }
                }
                
                val weekNum = getWeekOfYear(weekStart)
                val label = "KW$weekNum"
                points.add(TrendPoint(label, 0f, totalCompletions))
            }
        }
        1 -> { // Monthly
            for (i in 5 downTo 0) {
                val monthDate = today.minusMonths(i.toLong())
                val startOfMonth = monthDate.withDayOfMonth(1)
                val endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
                
                var totalCompletions = 0
                habitLogs.forEach { log ->
                    val logDate = try { java.time.LocalDate.parse(log.date) } catch (e: Exception) { null }
                    if (logDate != null && !logDate.isBefore(startOfMonth) && !logDate.isAfter(endOfMonth)) {
                        if (isLogCompleted(habit, log)) {
                            totalCompletions++
                        }
                    }
                }
                
                val monthName = monthDate.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                points.add(TrendPoint(monthName, 0f, totalCompletions))
            }
        }
        2 -> { // Yearly
            for (i in 3 downTo 0) {
                val yearDate = today.minusYears(i.toLong())
                val startOfYear = yearDate.withDayOfYear(1)
                val endOfYear = yearDate.withDayOfYear(yearDate.lengthOfYear())
                
                var totalCompletions = 0
                habitLogs.forEach { log ->
                    val logDate = try { java.time.LocalDate.parse(log.date) } catch (e: Exception) { null }
                    if (logDate != null && !logDate.isBefore(startOfYear) && !logDate.isAfter(endOfYear)) {
                        if (isLogCompleted(habit, log)) {
                            totalCompletions++
                        }
                    }
                }
                
                val yearLabel = yearDate.year.toString()
                points.add(TrendPoint(yearLabel, 0f, totalCompletions))
            }
        }
    }
    return points
}