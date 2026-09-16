package com.example.data

import java.util.Calendar
import androidx.compose.ui.graphics.Color
import com.example.tr
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun parseHabitColor(colorName: String): Color {
    return when (colorName.lowercase()) {
        "blue" -> HabitBlue
        "purple", "violet" -> HabitPurple
        "cyan" -> HabitCyan
        "green" -> HabitGreen
        "yellow" -> HabitYellow
        "orange" -> HabitOrange
        "red" -> HabitRed
        "pink" -> HabitPink
        "slate" -> HabitSlate
        "teal" -> HabitTeal
        "rose" -> HabitRose
        "indigo" -> HabitIndigo
        else -> try {
            if (colorName.startsWith("#")) Color(android.graphics.Color.parseColor(colorName))
            else PrimaryViolet
        } catch (e: Exception) {
            PrimaryViolet
        }
    }
}

data class MonthlyReviewData(
    val year: Int,
    val month: Int,
    val monthName: String,
    val totalCompletions: Int,
    val totalCheckIns: Int = 0,
    val prevMonthCompletions: Int,
    val growthPercentage: Int,
    val startScore: Int,
    val endScore: Int,
    val scoreDelta: Int,
    val mvpHabit: Habit?,
    val mvpCompletions: Int,
    val mvpStrength: Int,
    val focusHabit: Habit?,
    val focusCompletions: Int,
    val focusExpected: Int,
    val bestDayOfWeekName: String,
    val bestDayOfWeekRate: Int
)

fun calculateHabitStrengthOnDate(habit: Habit, logs: List<HabitLog>, targetDateStr: String): Int {
    val todayStr = java.time.LocalDate.now().toString()
    if (targetDateStr >= todayStr) {
        return com.example.data.calculateHabitStrength(habit, logs)
    }

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val targetDate = try { sdf.parse(targetDateStr) } catch (e: Exception) { java.util.Date() }
    val targetMillis = targetDate?.time ?: System.currentTimeMillis()
    val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    val startSdfStr = sdf.format(java.util.Date(validStartMillis))
    
    val filteredLogs = logs.filter { it.date <= targetDateStr && it.date >= startSdfStr }
    val habitLogs = filteredLogs.filter { it.habitId == habit.id }
    
    val completedEpochDays = habitLogs.filter { log ->
        com.example.data.isLogCompleted(habit, log)
    }.map { 
        try {
            java.time.LocalDate.parse(it.date).toEpochDay().toInt()
        } catch (e: Exception) {
            0
        }
    }.toSet()
    
    if (habit.frequency == "TIMES_WEEKLY") {
        val targetTimes = habit.specificDays.toIntOrNull() ?: 3
        val activeDays = (targetMillis - validStartMillis) / (24 * 3600 * 1000) + 1
        val activeWeeks = (activeDays / 7.0).coerceAtMost(4.0).coerceAtLeast(1.0)
        val expectedCompletions = (activeWeeks * targetTimes).toInt().coerceAtLeast(1)
        
        val limitMillis = targetMillis - 28L * 24 * 3600 * 1000
        val limitSdfStr = sdf.format(java.util.Date(limitMillis.coerceAtLeast(validStartMillis)))
        val completedInLast4Weeks = habitLogs.filter { log ->
            com.example.data.isLogCompleted(habit, log) && log.date >= limitSdfStr
        }.size
        
        return (completedInLast4Weeks.toFloat() / expectedCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
    }
    
    val loggedEpochDays = habitLogs.map { 
        try {
            java.time.LocalDate.parse(it.date).toEpochDay().toInt()
        } catch (e: Exception) {
            0
        }
    }.toSet()
    
    val todayEpoch = try {
        java.time.LocalDate.parse(targetDateStr).toEpochDay().toInt()
    } catch (e: Exception) {
        0
    }
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
    
    val realTodayEpoch = java.time.LocalDate.now().toEpochDay().toInt()
    
    for (i in 0 until maxDays) {
        val currentEpoch = todayEpoch - i
        val dayWeight = 20 + (totalDaysToCheck - i)
        
        val successful = if (habit.isNegative) {
            !loggedEpochDays.contains(currentEpoch) || completedEpochDays.contains(currentEpoch)
        } else {
            completedEpochDays.contains(currentEpoch)
        }
        
        // Skip penalizing if it's the actual today, it's a positive habit, and hasn't been logged yet
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

fun isLogCompleted(log: HabitLog, habit: Habit?): Boolean {
    if (habit == null) return false
    return com.example.data.isLogCompleted(habit, log)
}



fun calculateStreakFast(habit: Habit, logs: List<HabitLog>): Pair<Int, Int> {
    val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    val startSdfStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(validStartMillis))
    val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr }
    if (habitLogs.isEmpty()) return 0 to 0

    val completedDates = habitLogs.filter { log ->
        com.example.data.isLogCompleted(habit, log) && !log.isPaused
    }.map { it.date }.toSet()

    if (habit.frequency == "TIMES_WEEKLY") {
        val targetTimes = habit.specificDays.toIntOrNull() ?: 3
        val today = java.time.LocalDate.now()
        val habitStartDate = try {
            java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L))
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        } catch (e: Exception) {
            today
        }

        if (habitStartDate.isAfter(today)) return 0 to 0

        val pausedDates = habitLogs.filter { it.isPaused }.map { it.date }.toSet()
        var longestStreak = 0
        var tempStreak = 0
        var currDate = habitStartDate
        val daysSuccessMap = mutableMapOf<java.time.LocalDate, Boolean>()

        while (!currDate.isAfter(today)) {
            val startOf7Days = currDate.minusDays(6)
            var completedInWindow = 0
            var pausedInWindow = 0
            for (d in 0..6) {
                val dayStr = startOf7Days.plusDays(d.toLong()).toString()
                if (completedDates.contains(dayStr)) completedInWindow++
                if (pausedDates.contains(dayStr)) pausedInWindow++
            }
            val adjustedTarget = (targetTimes - pausedInWindow).coerceAtLeast(1)
            val isSuccess = completedInWindow >= adjustedTarget
            daysSuccessMap[currDate] = isSuccess

            if (isSuccess) {
                tempStreak++
                if (tempStreak > longestStreak) longestStreak = tempStreak
            } else {
                if (currDate != today) {
                    tempStreak = 0
                }
            }
            currDate = currDate.plusDays(1)
        }

        var currentStreak = 0
        val startFrom = if (daysSuccessMap[today] == true) today else today.minusDays(1)
        var checkDate = startFrom
        while (!checkDate.isBefore(habitStartDate) && daysSuccessMap[checkDate] == true) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        return currentStreak to longestStreak
    }

    val sortedDates = completedDates.sorted()
    if (sortedDates.isEmpty()) return 0 to 0

    var longest = 0
    var current = 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val todayStr = sdf.format(java.util.Date())
    
    val epochDays = sortedDates.map { 
        try {
            java.time.LocalDate.parse(it).toEpochDay().toInt()
        } catch (e: Exception) {
            0
        }
    }.toSet()
    
    if (epochDays.isEmpty()) return 0 to 0
    
    val minEpoch = epochDays.minOrNull() ?: 0
    val maxEpoch = epochDays.maxOrNull() ?: 0
    
    var tempCurrent = 0
    for (epoch in minEpoch..maxEpoch) {
        if (epochDays.contains(epoch)) {
            tempCurrent++
            if (tempCurrent > longest) {
                longest = tempCurrent
            }
        } else {
            tempCurrent = 0
        }
    }
    
    val todayEpoch = try {
        java.time.LocalDate.parse(todayStr).toEpochDay().toInt()
    } catch (e: Exception) {
        0
    }
    
    var currentStreak = 0
    var checkEpoch = todayEpoch
    if (!epochDays.contains(todayEpoch) && epochDays.contains(todayEpoch - 1)) {
        checkEpoch = todayEpoch - 1
    }
    
    while (epochDays.contains(checkEpoch)) {
        currentStreak++
        checkEpoch--
    }

    return currentStreak to longest
}

data class YearlyReviewData(
    val year: Int,
    val totalCompletions: Int,
    val totalCheckIns: Int = 0,
    val activeDaysCount: Int,
    val activeDaysPercentage: Int,
    val topHabit: Habit?,
    val topHabitCompletions: Int,
    val growthHabit: Habit?,
    val growthHabitCompletions: Int,
    val bestMonthName: String,
    val bestMonthCompletions: Int,
    val bestDayOfWeekName: String,
    val bestDayOfWeekCompletions: Int,
    val perfectDaysCount: Int,
    val longestStreak: Int = 0,
    val longestStreakHabit: Habit? = null,
    val isStreakActive: Boolean = false,
    val startScore: Int = 0,
    val endScore: Int = 0,
    val scoreDelta: Int = 0
)



fun getMonthNameLocalized(month: Int, language: String): String {
    val GermanMonths = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )
    val EnglishMonths = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val idx = (month - 1).coerceIn(0, 11)
    return if (language == "de") GermanMonths[idx] else EnglishMonths[idx]
}

fun getDayOfWeekNameLocalized(dayOfWeek: Int, language: String): String {
    val GermanDays = mapOf(
        Calendar.MONDAY to "Montag",
        Calendar.TUESDAY to "Dienstag",
        Calendar.WEDNESDAY to "Mittwoch",
        Calendar.THURSDAY to "Donnerstag",
        Calendar.FRIDAY to "Freitag",
        Calendar.SATURDAY to "Samstag",
        Calendar.SUNDAY to "Sonntag"
    )
    val GeorgianDays = mapOf(
        Calendar.MONDAY to "ორშაბათი",
        Calendar.TUESDAY to "სამშაბათი",
        Calendar.WEDNESDAY to "ოთხშაბათი",
        Calendar.THURSDAY to "ხუთშაბათი",
        Calendar.FRIDAY to "პარასკევი",
        Calendar.SATURDAY to "შაბათი",
        Calendar.SUNDAY to "კვირა"
    )
    val ChineseDays = mapOf(
        Calendar.MONDAY to "周一",
        Calendar.TUESDAY to "周二",
        Calendar.WEDNESDAY to "周三",
        Calendar.THURSDAY to "周四",
        Calendar.FRIDAY to "周五",
        Calendar.SATURDAY to "周六",
        Calendar.SUNDAY to "周日"
    )
    val EnglishDays = mapOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday",
        Calendar.SUNDAY to "Sunday"
    )
    return when (language) {
        "de" -> GermanDays[dayOfWeek] ?: "Montag"
        "ka" -> GeorgianDays[dayOfWeek] ?: "ორშაბათი"
        "zh" -> ChineseDays[dayOfWeek] ?: "周一"
        else -> EnglishDays[dayOfWeek] ?: "Monday"
    }
}


typealias MonthlyReview = MonthlyReviewData
typealias YearlyReview = YearlyReviewData
