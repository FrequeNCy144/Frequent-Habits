package com.example.data

import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object HabitCalculationEngine {

    private val epochDaysCache = ConcurrentHashMap<String, Int>()

    fun dateToEpochDaysFast(dateStr: String): Int {
        return epochDaysCache.getOrPut(dateStr) {
            try {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    LocalDate.of(year, month, day).toEpochDay().toInt()
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    fun millisToEpochDays(millis: Long): Int {
        return try {
            val instant = java.time.Instant.ofEpochMilli(millis)
            val zoneId = ZoneId.systemDefault()
            instant.atZone(zoneId).toLocalDate().toEpochDay().toInt()
        } catch (e: Exception) {
            (millis / 86400000L).toInt()
        }
    }
    fun calculateStreak(habit: Habit, logs: List<HabitLog>, targetDateStr: String? = null): Pair<Int, Int> {
        // Return (currentStreak, longestStreak)
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val targetMaxDateStr = targetDateStr ?: "9999-12-31"
        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr && it.date <= targetMaxDateStr }
        if (habitLogs.isEmpty() && !habit.isNegative) return 0 to 0

        val completedDates = mutableSetOf<String>()
        val loggedDates = mutableSetOf<String>()
        val pausedDates = mutableSetOf<String>()
        val failedDates = mutableSetOf<String>()
        habitLogs.forEach { log ->
            if (log.isPaused) {
                pausedDates.add(log.date)
            }
            if (log.value == -1f) {
                failedDates.add(log.date)
            }
            val isCompleted = isLogCompleted(habit, log)
            if (isCompleted && !log.isPaused) {
                completedDates.add(log.date)
            }
            if (!log.isPaused) {
                loggedDates.add(log.date)
            }
        }

        if (habit.frequency == "TIMES_WEEKLY") {
            val targetTimes = habit.specificDays.toIntOrNull() ?: 3
            val today = if (targetDateStr != null) {
                try { java.time.LocalDate.parse(targetDateStr) } catch (e: Exception) { java.time.LocalDate.now() }
            } else java.time.LocalDate.now()
            
            val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
            val habitStartDate = try {
                java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L))
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } catch (e: Exception) {
                java.time.LocalDate.now()
            }

            if (habitStartDate.isAfter(today)) return 0 to 0

            var longestStreak = 0
            var tempStreak = 0
            var currDate = habitStartDate
            val daysSuccessMap = mutableMapOf<java.time.LocalDate, Boolean>()

            while (!currDate.isAfter(today)) {
                val startOf7Days = currDate.minusDays(6)
                var completedInWindow = 0
                var pausedInWindow = 0
                for (d in 0..6) {
                    val dDate = startOf7Days.plusDays(d.toLong())
                    val dayStr = dDate.toString()
                    if (pausedDates.contains(dayStr)) {
                        pausedInWindow++
                    } else if (habit.isNegative) {
                        if (!dDate.isBefore(habitStartDate) && !dDate.isAfter(currDate) && !failedDates.contains(dayStr)) {
                            completedInWindow++
                        }
                    } else {
                        if (completedDates.contains(dayStr)) completedInWindow++
                    }
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

            // Calculate current streak backwards from today (or yesterday if today is not yet met)
            var currentStreak = 0
            val startFrom = if (daysSuccessMap[today] == true) today else today.minusDays(1)
            var checkDate = startFrom
            while (!checkDate.isBefore(habitStartDate) && daysSuccessMap[checkDate] == true) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }

            return currentStreak to longestStreak
        }

        if (habit.isNegative && loggedDates.isEmpty() && validStartMillis >= System.currentTimeMillis()) return 0 to 0

        val actualTodayEpoch = millisToEpochDays(System.currentTimeMillis())
        val targetEpoch = if (targetDateStr != null) dateToEpochDaysFast(targetDateStr) else actualTodayEpoch
        val todayEpoch = targetEpoch.coerceAtMost(actualTodayEpoch)
        val startEpoch = millisToEpochDays(validStartMillis)

        if (startEpoch > todayEpoch) return 0 to 0

        val completedEpochDays = completedDates.map { dateToEpochDaysFast(it) }.toSet()
        val loggedEpochDays = loggedDates.map { dateToEpochDaysFast(it) }.toSet()
        val pausedEpochDays = pausedDates.map { dateToEpochDaysFast(it) }.toSet()

        val getDayStr = { ep: Int ->
            val localDate = java.time.LocalDate.ofEpochDay(ep.toLong())
            String.format(Locale.US, "%04d-%02d-%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
        }

        var longest = 0
        var tempStreak = 0

        for (d in startEpoch..todayEpoch) {
            val dStr = getDayStr(d)
            if (!isHabitActiveOnDate(habit, dStr) || pausedEpochDays.contains(d)) {
                continue
            }

            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(d) || completedEpochDays.contains(d)
            } else {
                completedEpochDays.contains(d)
            }

            if (successful) {
                tempStreak++
                if (tempStreak > longest) longest = tempStreak
            } else {
                tempStreak = 0
            }
        }

        // Current streak backwards
        var currentStreak = 0
        var cursor = todayEpoch
        var continueChecking = true

        while (continueChecking && cursor >= startEpoch) {
            val dStr = getDayStr(cursor)
            if (!isHabitActiveOnDate(habit, dStr) || pausedEpochDays.contains(cursor)) {
                cursor--
                continue
            }

            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(cursor) || completedEpochDays.contains(cursor)
            } else {
                completedEpochDays.contains(cursor)
            }

            if (successful) {
                currentStreak++
                cursor--
            } else {
                if (cursor == todayEpoch) {
                    // Check yesterday (and skip inactive days)
                    var prev = cursor - 1
                    var prevStr = getDayStr(prev)
                    while (prev >= startEpoch && (!isHabitActiveOnDate(habit, prevStr) || pausedEpochDays.contains(prev))) {
                        prev--
                        if (prev >= startEpoch) {
                            prevStr = getDayStr(prev)
                        }
                    }
                    
                    val yesterdaySuccessful = if (prev >= startEpoch) {
                        if (habit.isNegative) {
                            !loggedEpochDays.contains(prev) || completedEpochDays.contains(prev)
                        } else {
                            completedEpochDays.contains(prev)
                        }
                    } else {
                        false
                    }

                    if (yesterdaySuccessful && prev >= startEpoch) {
                        cursor = prev
                    } else {
                        continueChecking = false
                    }
                } else {
                    continueChecking = false
                }
            }
        }

        return currentStreak to longest
    }

    fun calculateTotalStrength(habits: List<Habit>, logs: List<HabitLog>): Int {
        if (habits.isEmpty()) return 0
        // Calculate average strength score of all habits
        val strengths = habits.map { calculateHabitStrength(it, logs) }
        return strengths.average().toInt().coerceIn(0, 100)
    }

    fun calculateHabitStrength(habit: Habit, logs: List<HabitLog>, todayStr: String? = null): Int {
        return com.example.data.calculateHabitStrength(habit, logs, todayStr)
    }

    fun calculateCompletionRate(habit: Habit, logs: List<HabitLog>, todayStr: String? = null): Int {
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val limitDateStr = todayStr ?: "9999-12-31"
        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr && it.date <= limitDateStr }
        
        val completedEpochDays = habitLogs.filter { log ->
            isLogCompleted(habit, log)
        }.map { dateToEpochDaysFast(it.date) }.toSet()
        
        if (habit.frequency == "TIMES_WEEKLY") {
            val targetTimes = habit.specificDays.toIntOrNull() ?: 3
            val today = if (todayStr != null) {
                try { java.time.LocalDate.parse(todayStr) } catch (e: Exception) { java.time.LocalDate.now() }
            } else {
                java.time.LocalDate.now()
            }
            val habitStartDate = try {
                java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L))
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } catch (e: Exception) {
                today
            }

            if (habitStartDate.isAfter(today)) return 0

            var weekMonday = habitStartDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val currentWeekMonday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

            var totalExpectedCompletions = 0
            var safetyCount = 0
            while (!weekMonday.isAfter(currentWeekMonday) && safetyCount < 2000) {
                safetyCount++
                totalExpectedCompletions += targetTimes
                weekMonday = weekMonday.plusWeeks(1)
            }

            if (habit.isNegative) {
                var totalDays = 0
                var cleanDays = 0
                val failedDates = habitLogs.filter { it.value == -1f }.map { it.date }.toSet()
                val pausedDates = habitLogs.filter { it.isPaused }.map { it.date }.toSet()
                var cur = habitStartDate
                while (!cur.isAfter(today)) {
                    val dStr = cur.toString()
                    if (!pausedDates.contains(dStr)) {
                        totalDays++
                        if (!failedDates.contains(dStr)) {
                            cleanDays++
                        }
                    }
                    cur = cur.plusDays(1)
                }
                return if (totalDays > 0) ((cleanDays.toFloat() / totalDays.toFloat()) * 100).toInt().coerceIn(0, 100) else 100
            }

            if (totalExpectedCompletions == 0) return 0
            val completedCount = habitLogs.filter { log ->
                isLogCompleted(habit, log)
            }.size
            return (completedCount.toFloat() / totalExpectedCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
        }
        
        val loggedEpochDays = habitLogs.map { dateToEpochDaysFast(it.date) }.toSet()
        val pausedEpochDays = habitLogs.filter { it.isPaused }.map { dateToEpochDaysFast(it.date) }.toSet()

        val todayEpoch = if (todayStr != null) {
            try { java.time.LocalDate.parse(todayStr).toEpochDay().toInt() } catch (e: Exception) { millisToEpochDays(System.currentTimeMillis()) }
        } else {
            millisToEpochDays(System.currentTimeMillis())
        }
        val startEpoch = millisToEpochDays(validStartMillis)

        if (startEpoch > todayEpoch) return 0

        var completedDays = 0
        var totalDays = 0

        for (d in startEpoch..todayEpoch) {
            if (pausedEpochDays.contains(d)) {
                continue
            }
            totalDays++
            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(d) || completedEpochDays.contains(d)
            } else {
                completedEpochDays.contains(d)
            }
            if (successful) completedDays++
        }

        return if (totalDays > 0) {
            (completedDays.toFloat() / totalDays.toFloat() * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    fun getCompletedLogsCount(habit: Habit, logs: List<HabitLog>, period: String, referenceTimeMs: Long? = null): Int {
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val limitDateStr = referenceTimeMs?.let {
            startSdf.format(Date(it))
        } ?: "9999-12-31"
        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr && it.date <= limitDateStr }
        val cal = Calendar.getInstance()
        if (referenceTimeMs != null) {
            cal.timeInMillis = referenceTimeMs
        }
        val limit = when (period) {
            "WEEK" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "YEAR" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }

        if (!habit.isNegative) {
            return habitLogs.count { log ->
                val logTime = parseDateStringToMillis(log.date)
                (period == "ALL" || logTime >= limit) && isLogCompleted(habit, log)
            }
        } else {
            val startEpoch = millisToEpochDays(validStartMillis)
            val todayEpoch = if (referenceTimeMs != null) millisToEpochDays(referenceTimeMs) else millisToEpochDays(System.currentTimeMillis())
            val limitEpoch = if (limit > 0L) millisToEpochDays(limit) else startEpoch
            
            val searchStart = maxOf(startEpoch, limitEpoch)
            if (searchStart > todayEpoch) return 0

            val completedEpochDays = habitLogs.filter { log ->
                isLogCompleted(habit, log)
            }.map { dateToEpochDaysFast(it.date) }.toSet()

            val loggedEpochDays = habitLogs.map { dateToEpochDaysFast(it.date) }.toSet()

            var completedDays = 0
            val getDayStr = { ep: Int ->
                val localDate = java.time.LocalDate.ofEpochDay(ep.toLong())
                String.format(Locale.US, "%04d-%02d-%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
            }

            for (d in searchStart..todayEpoch) {
                val dStr = getDayStr(d)
                if (!isHabitActiveOnDate(habit, dStr)) {
                    continue
                }
                val successful = !loggedEpochDays.contains(d) || completedEpochDays.contains(d)
                if (successful) {
                    completedDays++
                }
            }
            return completedDays
        }
    }

    // Helper functions for dates
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun calculatePerfectDaysStats(habits: List<Habit>, logs: List<HabitLog>, targetDateStr: String? = null): PerfectDaysStats {
        if (habits.isEmpty()) return PerfectDaysStats(0, 0, 0, 0)

        val actualTodayEpoch = millisToEpochDays(System.currentTimeMillis())
        val targetEpoch = if (targetDateStr != null) dateToEpochDaysFast(targetDateStr) else actualTodayEpoch
        val todayEpoch = targetEpoch.coerceAtMost(actualTodayEpoch)
        
        // Oldest start epoch
        var oldestStartEpoch = todayEpoch
        habits.forEach { habit ->
            val validStartMillis = if (habit.startDate > 946684800000L) {
                habit.startDate
            } else if (habit.createdAt > 946684800000L) {
                habit.createdAt
            } else {
                System.currentTimeMillis()
            }
            val startEpoch = millisToEpochDays(validStartMillis)
            if (startEpoch < oldestStartEpoch) {
                oldestStartEpoch = startEpoch
            }
        }

        if (todayEpoch - oldestStartEpoch > 365) {
            oldestStartEpoch = todayEpoch - 365
        }

        if (oldestStartEpoch > todayEpoch) return PerfectDaysStats(0, 0, 0, 0)

        // Pre-group logs by date for fast lookup
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
            val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

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
                    val successful = if (log != null) {
                        when (log.value) {
                            -1f -> false
                            -2f -> true
                            else -> {
                                if (habit.type == "BINARY") {
                                    if (habit.isNegative) false else true
                                } else {
                                    if (habit.isNegative) log.value < habit.targetValue else log.value >= habit.targetValue
                                }
                            }
                        }
                    } else {
                        habit.isNegative
                    }

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

    fun calculateTotalAchievedValue(habit: Habit, logs: List<HabitLog>): Float {
        val habitLogs = logs.filter { it.habitId == habit.id }
        return if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
            habitLogs.filter { it.value > 0f && !it.isPaused }.sumOf { it.value.toDouble() }.toFloat()
        } else {
            habitLogs.count { isLogCompleted(habit, it) }.toFloat()
        }
    }

    private val dateToMillisCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun parseDateStringToMillis(dateStr: String): Long {
        return dateToMillisCache.getOrPut(dateStr) {
            try {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    val localDate = java.time.LocalDate.of(year, month, day)
                    localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    System.currentTimeMillis()
                }
            } catch (e: Exception) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (ex: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }

    // Achievement Unlocked Popup State
    private val achievementQueue = MutableStateFlow<List<com.example.data.UnlockedAchievementInfo>>(emptyList())
    private val _newlyUnlockedAchievement = MutableStateFlow<com.example.data.UnlockedAchievementInfo?>(null)
    val newlyUnlockedAchievement: StateFlow<com.example.data.UnlockedAchievementInfo?> = _newlyUnlockedAchievement.asStateFlow()

    private var knownUnlockedAchievementIds: MutableSet<String>? = null

}
