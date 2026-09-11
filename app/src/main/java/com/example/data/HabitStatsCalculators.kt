package com.example.data

import java.util.Locale

fun calculateTargetPeriodStats(habit: Habit, logs: List<HabitLog>): HabitTargetStats {
    val today = java.time.LocalDate.now()
    val todayStr = today.toString()
    
    // Filter logs for this habit
    val habitLogs = logs.filter { it.habitId == habit.id }
    val logsByDate = habitLogs.associateBy { it.date }
    
    // Get start of week (Monday) and end of week (Sunday)
    val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val endOfWeek = startOfWeek.plusDays(6)
    
    // Get start of month and end of month
    val startOfMonth = today.withDayOfMonth(1)
    val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
    
    // Get start of quarter and end of quarter
    val currentMonthValue = today.monthValue
    val startMonthOfQuarter = ((currentMonthValue - 1) / 3) * 3 + 1
    val startOfQuarter = today.withMonth(startMonthOfQuarter).withDayOfMonth(1)
    val endOfQuarter = startOfQuarter.plusMonths(2).let { it.withDayOfMonth(it.lengthOfMonth()) }
    
    // Get start of year and end of year
    val startOfYear = today.withDayOfYear(1)
    val endOfYear = today.withMonth(12).withDayOfMonth(31)
    
    // Helper to calculate target days for DAILY or SPECIFIC
    fun getScheduledDaysCount(start: java.time.LocalDate, end: java.time.LocalDate): Int {
        var count = 0
        var current = start
        while (!current.isAfter(end)) {
            if (isHabitActiveOnDate(habit, current.toString())) {
                count++
            }
            current = current.plusDays(1)
        }
        return count
    }
    
    // Helper to count days in period
    fun getDaysInPeriod(start: java.time.LocalDate, end: java.time.LocalDate): Int {
        return (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1).toInt()
    }
    
    // Function to calculate Target Day/Value count based on frequency
    fun getTargetCount(start: java.time.LocalDate, end: java.time.LocalDate): Float {
        return if (habit.frequency == "TIMES_WEEKLY") {
            val timesWeekly = habit.specificDays.toIntOrNull() ?: 3
            val daysInPeriod = getDaysInPeriod(start, end)
            // Proportional target
            (timesWeekly * (daysInPeriod / 7f))
        } else {
            getScheduledDaysCount(start, end).toFloat()
        }
    }
    
    val effectiveEndOfWeek = if (endOfWeek.isAfter(today)) today else endOfWeek
    val effectiveEndOfMonth = if (endOfMonth.isAfter(today)) today else endOfMonth
    val effectiveEndOfQuarter = if (endOfQuarter.isAfter(today)) today else endOfQuarter
    val effectiveEndOfYear = if (endOfYear.isAfter(today)) today else endOfYear

    // Calculate targets for elapsed period so far
    val todayTargetCount = if (isHabitActiveOnDate(habit, todayStr)) 1f else 0f
    val weekTargetCount = getTargetCount(startOfWeek, effectiveEndOfWeek)
    val monthTargetCount = getTargetCount(startOfMonth, effectiveEndOfMonth)
    val quarterTargetCount = getTargetCount(startOfQuarter, effectiveEndOfQuarter)
    val yearTargetCount = getTargetCount(startOfYear, effectiveEndOfYear)
    
    // Helper to sum or count logs in a period
    fun getActualStatsInPeriod(start: java.time.LocalDate, end: java.time.LocalDate): Float {
        var sum = 0f
        var count = 0f
        var current = start
        while (!current.isAfter(end)) {
            val log = logsByDate[current.toString()]
            if (log != null) {
                if (habit.type == "NUMBER") {
                    if (log.value > 0f) {
                        sum += log.value
                    }
                } else {
                    if (isLogCompleted(habit, log)) {
                        count += 1f
                    }
                }
            } else {
                // For negative habits, a missing log counts as completed
                if (habit.type == "BINARY" && habit.isNegative) {
                    count += 1f
                }
            }
            current = current.plusDays(1)
        }
        return if (habit.type == "NUMBER") sum else count
    }
    
    val isNumerical = habit.type == "NUMBER"
    
    // Today actual
    val todayLog = logsByDate[todayStr]
    val todayActual = if (isNumerical) {
        todayLog?.value ?: 0f
    } else {
        if (todayLog != null && isLogCompleted(habit, todayLog)) 1f else if (todayLog == null && habit.isNegative) 1f else 0f
    }
    
    val todayTarget = if (isNumerical) todayTargetCount * habit.targetValue else todayTargetCount
    val weekTarget = if (isNumerical) weekTargetCount * habit.targetValue else weekTargetCount
    val monthTarget = if (isNumerical) monthTargetCount * habit.targetValue else monthTargetCount
    val quarterTarget = if (isNumerical) quarterTargetCount * habit.targetValue else quarterTargetCount
    val yearTarget = if (isNumerical) yearTargetCount * habit.targetValue else yearTargetCount
    
    val weekActual = getActualStatsInPeriod(startOfWeek, effectiveEndOfWeek)
    val monthActual = getActualStatsInPeriod(startOfMonth, effectiveEndOfMonth)
    val quarterActual = getActualStatsInPeriod(startOfQuarter, effectiveEndOfQuarter)
    val yearActual = getActualStatsInPeriod(startOfYear, effectiveEndOfYear)
    
    return HabitTargetStats(
        today = TargetPeriodStats(todayActual, todayTarget, isNumerical),
        week = TargetPeriodStats(weekActual, weekTarget, isNumerical),
        month = TargetPeriodStats(monthActual, monthTarget, isNumerical),
        quarter = TargetPeriodStats(quarterActual, quarterTarget, isNumerical),
        year = TargetPeriodStats(yearActual, yearTarget, isNumerical)
    )
}

object StreakCalculator {
    fun calculate(habits: List<Habit>, logs: List<HabitLog>, targetDateStr: String? = null): PerfectDaysStats {
        if (habits.isEmpty()) return PerfectDaysStats(0, 0, 0, 0)

        val actualTodayDate = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        val actualTodayEpoch = actualTodayDate.toEpochDay().toInt()

        val targetEpoch = if (targetDateStr != null) {
            try { java.time.LocalDate.parse(targetDateStr).toEpochDay().toInt() } catch (e: Exception) { actualTodayEpoch }
        } else actualTodayEpoch
        val todayEpoch = targetEpoch.coerceAtMost(actualTodayEpoch)
        
        var oldestStartEpoch = todayEpoch
        habits.forEach { habit ->
            val validStartMillis = if (habit.startDate > 946684800000L) {
                habit.startDate
            } else if (habit.createdAt > 946684800000L) {
                habit.createdAt
            } else {
                System.currentTimeMillis()
            }
            val startEpoch = java.time.Instant.ofEpochMilli(validStartMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay()
                .toInt()
            if (startEpoch < oldestStartEpoch) {
                oldestStartEpoch = startEpoch
            }
        }

        if (todayEpoch - oldestStartEpoch > 365) {
            oldestStartEpoch = todayEpoch - 365
        }

        if (oldestStartEpoch > todayEpoch) return PerfectDaysStats(0, 0, 0, 0)

        val logsByDateAndHabit = logs.groupBy { it.date }.mapValues { entry ->
            entry.value.associateBy { it.habitId }
        }

        var totalPerfectDays = 0
        var perfectDaysStreak = 0
        var currentPerfectStreak = 0

        var totalCompletedCompletions = 0
        var totalPossibleCompletions = 0

        for (epochDay in oldestStartEpoch..todayEpoch) {
            val date = java.time.LocalDate.ofEpochDay(epochDay.toLong())
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)

            val activeHabits = habits.filter { habit ->
                isHabitActiveOnDate(habit, dateStr)
            }

            if (activeHabits.isEmpty()) {
                continue
            }

            val dayLogs = logsByDateAndHabit[dateStr] ?: emptyMap()
            var allCompletedThisDay = true
            var checkedAnyOnDay = false

            activeHabits.forEach { habit ->
                if (habit.frequency == "TIMES_WEEKLY") {
                    return@forEach
                }
                val log = dayLogs[habit.id]
                val isPaused = log != null && log.isPaused
                if (!isPaused) {
                    checkedAnyOnDay = true
                    totalPossibleCompletions++
                    val successful = isLogCompleted(habit, log)

                    if (successful) {
                        totalCompletedCompletions++
                    } else {
                        allCompletedThisDay = false
                    }
                }
            }

            if (checkedAnyOnDay && allCompletedThisDay) {
                totalPerfectDays++
                currentPerfectStreak++
                if (currentPerfectStreak > perfectDaysStreak) {
                    perfectDaysStreak = currentPerfectStreak
                }
            } else if (checkedAnyOnDay) {
                if (epochDay < actualTodayEpoch) {
                    currentPerfectStreak = 0
                }
            }
        }

        val completionRate = if (totalPossibleCompletions > 0) {
            (totalCompletedCompletions.toFloat() / totalPossibleCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return PerfectDaysStats(
            totalPerfectDays = totalPerfectDays,
            perfectDaysStreak = perfectDaysStreak,
            currentStreak = currentPerfectStreak,
            totalCompletedHabits = totalCompletedCompletions,
            totalCompletionRate = completionRate
        )
    }
}

fun calculateHabitStrength(habit: Habit, logs: List<HabitLog>, todayStr: String? = null): Int {
    val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    val today = if (todayStr != null) {
        try { java.time.LocalDate.parse(todayStr) } catch (e: Exception) { java.time.LocalDate.now() }
    } else {
        java.time.LocalDate.now()
    }
    val startSdfStr = try {
        java.time.Instant.ofEpochMilli(validStartMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
    } catch (e: Exception) { "2024-01-01" }

    val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr }
    
    val completedEpochDays = habitLogs.filter { log ->
        isLogCompleted(habit, log)
    }.map { 
        try { java.time.LocalDate.parse(it.date).toEpochDay().toInt() } catch (e: Exception) { 0 }
    }.toSet()
    
    if (habit.frequency == "TIMES_WEEKLY") {
        val targetTimes = habit.specificDays.toIntOrNull() ?: 3
        val activeDays = ((System.currentTimeMillis() - validStartMillis) / (24 * 3600 * 1000) + 1).coerceAtLeast(1)

        if (habit.isNegative) {
            val limitMillis = System.currentTimeMillis() - 28L * 24 * 3600 * 1000
            val limitEpoch = try {
                java.time.Instant.ofEpochMilli(limitMillis.coerceAtLeast(validStartMillis)).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay().toInt()
            } catch (e: Exception) { 0 }
            val todayEpoch = today.toEpochDay().toInt()

            val failedEpochDays = habitLogs.filter { it.value == -1f }.map {
                try { java.time.LocalDate.parse(it.date).toEpochDay().toInt() } catch (e: Exception) { 0 }
            }.toSet()
            val pausedEpochDays = habitLogs.filter { it.isPaused }.map {
                try { java.time.LocalDate.parse(it.date).toEpochDay().toInt() } catch (e: Exception) { 0 }
            }.toSet()

            var totalDays = 0
            var cleanDays = 0
            for (ep in limitEpoch..todayEpoch) {
                if (!pausedEpochDays.contains(ep)) {
                    totalDays++
                    if (!failedEpochDays.contains(ep)) {
                        cleanDays++
                    }
                }
            }
            return if (totalDays > 0) ((cleanDays.toFloat() / totalDays.toFloat()) * 100).toInt().coerceIn(0, 100) else 100
        } else {
            val activeWeeks = (activeDays / 7.0).coerceAtMost(4.0).coerceAtLeast(1.0)
            val expectedCompletions = (activeWeeks * targetTimes).toInt().coerceAtLeast(1)
            
            val limitMillis = System.currentTimeMillis() - 28L * 24 * 3600 * 1000
            val limitSdfStr = try {
                java.time.Instant.ofEpochMilli(limitMillis.coerceAtLeast(validStartMillis)).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
            } catch (e: Exception) {
                startSdfStr
            }
            val completedInLast4Weeks = habitLogs.filter { log ->
                isLogCompleted(habit, log) && log.date >= limitSdfStr
            }.size
            
            return (completedInLast4Weeks.toFloat() / expectedCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
        }
    }
    
    val loggedEpochDays = habitLogs.map { 
        try { java.time.LocalDate.parse(it.date).toEpochDay().toInt() } catch (e: Exception) { 0 }
    }.toSet()

    val pausedEpochDays = habitLogs.filter { it.isPaused }.map {
        try { java.time.LocalDate.parse(it.date).toEpochDay().toInt() } catch (e: Exception) { 0 }
    }.toSet()

    val todayEpoch = today.toEpochDay().toInt()
    val startEpoch = try {
        java.time.Instant.ofEpochMilli(validStartMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay().toInt()
    } catch (e: Exception) {
        (validStartMillis / 86400000L).toInt()
    }

    var weightedCompleted = 0
    var totalPossibleWeight = 0
    val totalDaysToCheck = 30

    val maxDays = if (todayEpoch - startEpoch + 1 < totalDaysToCheck) {
        todayEpoch - startEpoch + 1
    } else {
        totalDaysToCheck
    }

    if (maxDays <= 0) return 0

    val realTodayEpoch = todayEpoch

    for (i in 0 until maxDays) {
        val currentEpoch = todayEpoch - i
        if (pausedEpochDays.contains(currentEpoch)) {
            continue
        }
        val dayWeight = 20 + (totalDaysToCheck - i)
        
        val successful = if (habit.isNegative) {
            !loggedEpochDays.contains(currentEpoch) || completedEpochDays.contains(currentEpoch)
        } else {
            completedEpochDays.contains(currentEpoch)
        }
        
        val isPendingRealToday = (currentEpoch == realTodayEpoch) && !habit.isNegative && !loggedEpochDays.contains(currentEpoch)
        if (isPendingRealToday) {
            continue
        }
        
        if (successful) {
            weightedCompleted += dayWeight
        }
        totalPossibleWeight += dayWeight
    }

    val percentage = if (totalPossibleWeight > 0) {
        (weightedCompleted.toFloat() / totalPossibleWeight.toFloat() * 100).toInt()
    } else {
        0
    }
    return percentage.coerceIn(0, 100)
}
