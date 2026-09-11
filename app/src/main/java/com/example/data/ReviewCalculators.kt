package com.example.data

import com.example.tr
import java.util.Calendar

fun calculateMonthlyReviewData(
    year: Int,
    month: Int,
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    language: String
): MonthlyReviewData {
    val monthPrefix = String.format(java.util.Locale.US, "%04d-%02d-", year, month)
    
    val monthLogs = allLogs.filter { it.date.startsWith(monthPrefix) }
    val validMonthLogs = monthLogs.filter { log ->
        isLogCompleted(log, allHabits.find { it.id == log.habitId })
    }
    val totalCompletions = validMonthLogs.size
    val totalCheckIns = monthLogs.count { it.value != 0f && !it.isPaused }
    
    val prevYear = if (month == 1) year - 1 else year
    val prevMonth = if (month == 1) 12 else month - 1
    val prevMonthPrefix = String.format(java.util.Locale.US, "%04d-%02d-", prevYear, prevMonth)
    
    val validPrevLogs = allLogs.filter { log ->
        log.date.startsWith(prevMonthPrefix) && isLogCompleted(log, allHabits.find { it.id == log.habitId })
    }
    val prevMonthCompletions = validPrevLogs.size
    
    val growthPercentage = if (prevMonthCompletions > 0) {
        (((totalCompletions - prevMonthCompletions).toFloat() / prevMonthCompletions) * 100).toInt()
    } else {
        0
    }
    
    val calendar = java.util.Calendar.getInstance()
    // Last day of previous month
    calendar.set(java.util.Calendar.YEAR, prevYear)
    calendar.set(java.util.Calendar.MONTH, prevMonth - 1)
    val prevMaxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val prevMonthLastDay = String.format(java.util.Locale.US, "%04d-%02d-%02d", prevYear, prevMonth, prevMaxDay)
    
    // Last day of this month
    calendar.set(java.util.Calendar.YEAR, year)
    calendar.set(java.util.Calendar.MONTH, month - 1)
    val thisMaxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val thisMonthLastDay = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, thisMaxDay)
    
    val activeHabits = allHabits.filter { !it.isArchived }
    
    val habitsAtStart = activeHabits.filter { h ->
        val startStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(if (h.startDate > 946684800000L) h.startDate else h.createdAt))
        startStr <= prevMonthLastDay
    }
    val startScore = if (habitsAtStart.isNotEmpty()) {
        habitsAtStart.map { calculateHabitStrengthOnDate(it, allLogs, prevMonthLastDay) }.average().toInt()
    } else 0
    
    val habitsAtEnd = activeHabits.filter { h ->
        val startStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(if (h.startDate > 946684800000L) h.startDate else h.createdAt))
        startStr <= thisMonthLastDay
    }
    val endScore = if (habitsAtEnd.isNotEmpty()) {
        habitsAtEnd.map { calculateHabitStrengthOnDate(it, allLogs, thisMonthLastDay) }.average().toInt()
    } else 0
    
    val scoreDelta = endScore - startScore
    
    // MVP Habit: most completions in this month
    val mvpHabit = activeHabits.maxByOrNull { h ->
        allLogs.count { it.habitId == h.id && it.date.startsWith(monthPrefix) && isLogCompleted(it, h) }
    }
    val mvpCompletions = if (mvpHabit != null) {
        allLogs.count { it.habitId == mvpHabit.id && it.date.startsWith(monthPrefix) && isLogCompleted(it, mvpHabit) }
    } else 0
    val mvpStrength = if (mvpHabit != null) {
        calculateHabitStrengthOnDate(mvpHabit, allLogs, thisMonthLastDay)
    } else 0
    
    // Focus Habit: lowest completion rate in this month
    val focusHabit = activeHabits.filter { h ->
        val validStartStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(if (h.startDate > 946684800000L) h.startDate else h.createdAt))
        validStartStr <= thisMonthLastDay
    }.minByOrNull { h ->
        val comps = allLogs.count { it.habitId == h.id && it.date.startsWith(monthPrefix) && isLogCompleted(it, h) }
        val expected = if (h.frequency == "TIMES_WEEKLY") (h.specificDays.toIntOrNull() ?: 3) * 4 else 28
        comps.toFloat() / expected.coerceAtLeast(1).toFloat()
    }
    
    val focusCompletions = if (focusHabit != null) {
        allLogs.count { it.habitId == focusHabit.id && it.date.startsWith(monthPrefix) && isLogCompleted(it, focusHabit) }
    } else 0
    
    val focusExpected = if (focusHabit != null) {
        if (focusHabit.frequency == "TIMES_WEEKLY") (focusHabit.specificDays.toIntOrNull() ?: 3) * 4 else 28
    } else 0
    
    // Power day of week
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val logsByDayOfWeek = validMonthLogs.groupBy { log ->
        try {
            val d = sdf.parse(log.date)
            val cal = java.util.Calendar.getInstance()
            if (d != null) cal.time = d
            cal.get(java.util.Calendar.DAY_OF_WEEK)
        } catch (e: Exception) {
            java.util.Calendar.MONDAY
        }
    }
    val bestDayEntry = logsByDayOfWeek.maxByOrNull { it.value.size }
    val bestDayOfWeekNum = bestDayEntry?.key ?: java.util.Calendar.MONDAY
    val bestDayOfWeekName = getDayOfWeekNameLocalized(bestDayOfWeekNum, language)
    val bestDayOfWeekRate = bestDayEntry?.value?.size ?: 0
    
    return MonthlyReviewData(
        year = year,
        month = month,
        monthName = getMonthNameLocalized(month, language),
        totalCompletions = totalCompletions,
        totalCheckIns = totalCheckIns,
        prevMonthCompletions = prevMonthCompletions,
        growthPercentage = growthPercentage,
        startScore = startScore,
        endScore = endScore,
        scoreDelta = scoreDelta,
        mvpHabit = mvpHabit,
        mvpCompletions = mvpCompletions,
        mvpStrength = mvpStrength,
        focusHabit = focusHabit,
        focusCompletions = focusCompletions,
        focusExpected = focusExpected,
        bestDayOfWeekName = bestDayOfWeekName,
        bestDayOfWeekRate = bestDayOfWeekRate
    )
}

fun calculateYearlyReviewData(
    year: Int,
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    language: String
): YearlyReviewData {
    val yearPrefix = "$year-"
    
    val yearLogs = allLogs.filter { it.date.startsWith(yearPrefix) }
    val validYearLogs = yearLogs.filter { log -> 
        isLogCompleted(log, allHabits.find { it.id == log.habitId })
    }
    val totalCompletions = validYearLogs.size
    val totalCheckIns = yearLogs.count { it.value != 0f && !it.isPaused }

    val activeDates = validYearLogs.map { it.date }.distinct()
    val totalActiveDays = activeDates.size
    
    val activeHabits = allHabits.filter { !it.isArchived }
    
    // Calculate Score from Dec 31 of prev year to Dec 31 of this year
    val prevYearLastDay = "${year - 1}-12-31"
    val thisYearLastDay = "$year-12-31"
    
    val habitsAtStart = activeHabits.filter { h ->
        val startStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(if (h.startDate > 946684800000L) h.startDate else h.createdAt))
        startStr <= prevYearLastDay
    }
    val startScore = if (habitsAtStart.isNotEmpty()) {
        habitsAtStart.map { calculateHabitStrengthOnDate(it, allLogs, prevYearLastDay) }.average().toInt()
    } else 0
    
    val habitsAtEnd = activeHabits.filter { h ->
        val startStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(if (h.startDate > 946684800000L) h.startDate else h.createdAt))
        startStr <= thisYearLastDay
    }
    val endScore = if (habitsAtEnd.isNotEmpty()) {
        habitsAtEnd.map { calculateHabitStrengthOnDate(it, allLogs, thisYearLastDay) }.average().toInt()
    } else 0
    
    val scoreDelta = endScore - startScore

    val mvpHabit = activeHabits.maxByOrNull { h ->
        allLogs.count { it.habitId == h.id && it.date.startsWith(yearPrefix) && isLogCompleted(it, h) }
    }
    val mvpCompletions = if (mvpHabit != null) {
        allLogs.count { it.habitId == mvpHabit.id && it.date.startsWith(yearPrefix) && isLogCompleted(it, mvpHabit) }
    } else 0

    val growthHabit = activeHabits.maxByOrNull { h ->
        val startH = calculateHabitStrengthOnDate(h, allLogs, prevYearLastDay)
        val endH = calculateHabitStrengthOnDate(h, allLogs, thisYearLastDay)
        endH - startH
    }
    val growthHabitCompletions = if (growthHabit != null) {
        allLogs.count { it.habitId == growthHabit.id && it.date.startsWith(yearPrefix) && isLogCompleted(it, growthHabit) }
    } else 0

    val activeDaysPercentage = if (totalActiveDays > 0) {
        (totalActiveDays * 100) / 365
    } else 0

    val completionsByMonth = validYearLogs.groupBy { 
        if (it.date.length >= 7) it.date.substring(5, 7).toIntOrNull() ?: 1 else 1 
    }
    val bestMonthEntry = completionsByMonth.maxByOrNull { it.value.size }
    val bestMonthNum = bestMonthEntry?.key ?: 1
    val bestMonthName = getMonthNameLocalized(bestMonthNum, language)
    val bestMonthCompletions = bestMonthEntry?.value?.size ?: 0

    val sdfDay = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val calDay = Calendar.getInstance()
    val completionsByDayOfWeek = validYearLogs.groupBy { log ->
        try {
            val date = sdfDay.parse(log.date)
            if (date != null) {
                calDay.time = date
                calDay.get(Calendar.DAY_OF_WEEK)
            } else Calendar.MONDAY
        } catch (e: Exception) {
            Calendar.MONDAY
        }
    }
    val bestDayOfWeekEntry = completionsByDayOfWeek.maxByOrNull { it.value.size }
    val bestDayOfWeekNum = bestDayOfWeekEntry?.key ?: Calendar.MONDAY
    val bestDayOfWeekName = when (bestDayOfWeekNum) {
        Calendar.SUNDAY -> com.example.tr(language, "Sonntag", "კვირა", "周日", "Sunday")
        Calendar.MONDAY -> com.example.tr(language, "Montag", "ორშაბათი", "周一", "Monday")
        Calendar.TUESDAY -> com.example.tr(language, "Dienstag", "სამშაბათი", "周二", "Tuesday")
        Calendar.WEDNESDAY -> com.example.tr(language, "Mittwoch", "ოთხშაბათი", "周三", "Wednesday")
        Calendar.THURSDAY -> com.example.tr(language, "Donnerstag", "ხუთშაბათი", "周四", "Thursday")
        Calendar.FRIDAY -> com.example.tr(language, "Freitag", "პარასკევი", "周五", "Friday")
        Calendar.SATURDAY -> com.example.tr(language, "Samstag", "შაბათი", "周六", "Saturday")
        else -> com.example.tr(language, "Montag", "ორშაბათი", "周一", "Monday")
    }
    val bestDayOfWeekCompletions = bestDayOfWeekEntry?.value?.size ?: 0

    var perfectDaysCount = 0
    val daysInYearWithLogs = validYearLogs.groupBy { it.date }
    for ((_, logsOnDay) in daysInYearWithLogs) {
        if (activeHabits.isNotEmpty() && logsOnDay.size >= activeHabits.size) {
            perfectDaysCount++
        }
    }

    val streakPairs = activeHabits.map { h ->
        h to calculateStreakFast(h, allLogs)
    }
    val longestStreakPair = streakPairs.maxByOrNull { it.second.second }
    val longestStreak = longestStreakPair?.second?.second ?: 0
    val longestStreakHabit = longestStreakPair?.first
    val isStreakActive = (longestStreakPair?.second?.first ?: 0) > 0

    return YearlyReviewData(
        year = year,
        totalCompletions = totalCompletions,
        totalCheckIns = totalCheckIns,
        activeDaysCount = totalActiveDays,
        activeDaysPercentage = activeDaysPercentage,
        topHabit = mvpHabit,
        topHabitCompletions = mvpCompletions,
        growthHabit = growthHabit,
        growthHabitCompletions = growthHabitCompletions,
        bestMonthName = bestMonthName,
        bestMonthCompletions = bestMonthCompletions,
        bestDayOfWeekName = bestDayOfWeekName,
        bestDayOfWeekCompletions = bestDayOfWeekCompletions,
        perfectDaysCount = perfectDaysCount,
        longestStreak = longestStreak,
        longestStreakHabit = longestStreakHabit,
        isStreakActive = isStreakActive,
        startScore = startScore,
        endScore = endScore,
        scoreDelta = scoreDelta
    )
}
