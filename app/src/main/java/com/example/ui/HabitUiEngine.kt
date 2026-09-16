package com.example.ui

import com.example.data.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HabitUiEngine {

    fun calculateMinWeekStartMillis(habits: List<Habit>, archHabits: List<Habit>, logs: List<HabitLog>): Long {
        val all = habits + archHabits
        var earliestMs = if (all.isNotEmpty()) {
            all.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else System.currentTimeMillis()
        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try { sdf.parse(log.date)?.time } catch (e: Exception) { null }
            }.minOrNull()
            if (earliestLogMs != null) earliestMs = minOf(earliestMs, earliestLogMs)
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

    fun calculateMinDateStr(habits: List<Habit>, archHabits: List<Habit>, logs: List<HabitLog>): String {
        val all = habits + archHabits
        var earliestMs = if (all.isNotEmpty()) {
            all.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else System.currentTimeMillis()
        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try { sdf.parse(log.date)?.time } catch (e: Exception) { null }
            }.minOrNull()
            if (earliestLogMs != null) earliestMs = minOf(earliestMs, earliestLogMs)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(earliestMs))
    }

    fun calculateOldestHabitDateMs(active: List<Habit>, arch: List<Habit>, logs: List<HabitLog>): Long {
        val habits = active + arch
        var earliestMs = if (habits.isNotEmpty()) {
            habits.map {
                if (it.startDate > 946684800000L) it.startDate else it.createdAt
            }.minOrNull() ?: System.currentTimeMillis()
        } else System.currentTimeMillis()
        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try { sdf.parse(log.date)?.time } catch (e: Exception) { null }
            }.minOrNull()
            if (earliestLogMs != null) earliestMs = minOf(earliestMs, earliestLogMs)
        }
        return earliestMs
    }

    fun calculateHeatmapCanPrevMonth(monthCal: Calendar, activeHabits: List<Habit>, archHabits: List<Habit>, logs: List<HabitLog>): Boolean {
        val oldestHabitDateMs = calculateOldestHabitDateMs(activeHabits, archHabits, logs)
        val oldestCal = Calendar.getInstance().apply { timeInMillis = oldestHabitDateMs }
        val currentCalMonth = monthCal.get(Calendar.MONTH) + monthCal.get(Calendar.YEAR) * 12
        val oldestCalMonth = oldestCal.get(Calendar.MONTH) + oldestCal.get(Calendar.YEAR) * 12
        return currentCalMonth > oldestCalMonth
    }

    fun calculateTodayProgress(habits: List<Habit>, logs: List<HabitLog>, date: String): Pair<Int, Int> {
        val activeHabits = habits.filter { habit ->
            val isCompletedInFuture = if (habit.isFinishable && habit.isCompletedGoal && habit.completedAt != null) {
                val compDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(habit.completedAt))
                date > compDateStr
            } else false
            !isCompletedInFuture && isHabitActiveOnDate(habit, date)
        }
        val logsMap = logs.filter { it.date == date }.associateBy { it.habitId }
        var completedCount = 0
        var nonPausedActiveCount = 0
        activeHabits.forEach { habit ->
            val log = logsMap[habit.id]
            val isPaused = log != null && log.isPaused
            if (!isPaused) {
                nonPausedActiveCount++
                if (isLogCompleted(habit, log)) completedCount++
            }
        }
        return completedCount to nonPausedActiveCount
    }

    fun calculateActiveHabitUiItems(habits: List<Habit>, logs: List<HabitLog>, date: String): List<HabitUiItem> {
        val logsForDateMap = logs.filter { it.date == date }.associateBy { it.habitId }
        val activeHabits = habits.filter { habit ->
            val isCompletedInFuture = if (habit.isFinishable && habit.isCompletedGoal && habit.completedAt != null) {
                val compDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(habit.completedAt))
                date > compDateStr
            } else false
            !isCompletedInFuture && (isHabitActiveOnDate(habit, date) || logsForDateMap.containsKey(habit.id))
        }
        val mappedItems = activeHabits.map { habit ->
            val log = logsForDateMap[habit.id]
            val currentValue = log?.value ?: 0f
            val hasLog = log != null
            val isPaused = log?.isPaused == true
            val isMinimalViable = log?.isMinimalViable == true
            val effectiveTargetValue = if (isMinimalViable && habit.type == "NUMBER" && habit.minimalViableValue != null) {
                habit.minimalViableValue
            } else habit.targetValue

            val status = when {
                isPaused -> "PAUSED"
                log == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
                log.value == -1f -> "FAILED"
                log.value == -2f -> "SUCCESS"
                log.value == 0f -> if (habit.isNegative) "SUCCESS" else "PENDING"
                else -> {
                    if (habit.type == "BINARY") {
                        if (habit.isNegative) "FAILED" else "SUCCESS"
                    } else {
                        if (habit.isNegative) {
                            if (log.value >= effectiveTargetValue) "FAILED" else "PENDING"
                        } else {
                            if (log.value >= effectiveTargetValue) "SUCCESS" else "PENDING"
                        }
                    }
                }
            }
            val isCompleted = status == "SUCCESS"
            val isFailed = status == "FAILED"
            var isWeeklyTargetReached = false
            var weeklyLoggedCount = 0
            var weeklyTargetCount = 0
            if (habit.frequency == "TIMES_WEEKLY") {
                weeklyTargetCount = habit.specificDays.toIntOrNull() ?: 3
                val curDate = try { LocalDate.parse(date) } catch (e: Exception) { LocalDate.now() }
                val startOf7Days = curDate.minusDays(6)
                val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
                val habitStartDate = try {
                    java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L)).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } catch (e: Exception) { curDate }
                if (habit.isNegative) {
                    val habitLogsMap = logs.filter { it.habitId == habit.id }.associateBy { it.date }
                    var cleanInWindow = 0
                    var pausedInWindow = 0
                    for (d in 0..6) {
                        val checkDate = startOf7Days.plusDays(d.toLong())
                        if (!checkDate.isBefore(habitStartDate) && !checkDate.isAfter(curDate)) {
                            val l = habitLogsMap[checkDate.toString()]
                            if (l?.isPaused == true) pausedInWindow++
                            else if (l == null || isLogCompleted(habit, l)) cleanInWindow++
                        }
                    }
                    weeklyLoggedCount = cleanInWindow
                    val adjustedTarget = (weeklyTargetCount - pausedInWindow).coerceAtLeast(1)
                    isWeeklyTargetReached = weeklyLoggedCount >= adjustedTarget
                } else {
                    val startOf7DaysStr = startOf7Days.toString()
                    val endOf7DaysStr = curDate.toString()
                    val habitLogsInWindow = logs.filter { l ->
                        l.habitId == habit.id && l.date >= startOf7DaysStr && l.date <= endOf7DaysStr
                    }
                    val pausedInWindow = habitLogsInWindow.count { it.isPaused }
                    weeklyLoggedCount = habitLogsInWindow.count { !it.isPaused && isLogCompleted(habit, it) }
                    val adjustedTarget = (weeklyTargetCount - pausedInWindow).coerceAtLeast(1)
                    isWeeklyTargetReached = weeklyLoggedCount >= adjustedTarget
                }
            }
            val (streakVal, _) = HabitCalculationEngine.calculateStreak(habit, logs, targetDateStr = date)
            val totalAchievedValue = if (habit.isFinishable) HabitCalculationEngine.calculateTotalAchievedValue(habit, logs) else 0f
            val isGoalTargetReached = habit.isFinishable && !habit.isCompletedGoal && HabitCalculationEngine.isGoalTargetReached(habit, logs)
            HabitUiItem(
                habit = habit,
                currentValue = currentValue,
                isCompleted = isCompleted,
                isFailed = isFailed,
                isPaused = isPaused,
                hasLog = hasLog,
                isWeeklyTargetReached = isWeeklyTargetReached,
                weeklyLoggedCount = weeklyLoggedCount,
                weeklyTargetCount = weeklyTargetCount,
                streak = streakVal,
                isMinimalViable = isMinimalViable,
                totalAchievedValue = totalAchievedValue,
                isGoalTargetReached = isGoalTargetReached
            )
        }
        val itemsById = mappedItems.associateBy { it.habit.id }
        val rootItems = mappedItems.filter { it.habit.stackedOnHabitId == null || !itemsById.containsKey(it.habit.stackedOnHabitId) }
        val childrenMap = mappedItems.filter { it.habit.stackedOnHabitId != null && itemsById.containsKey(it.habit.stackedOnHabitId) }
            .groupBy { it.habit.stackedOnHabitId!! }
        val orderedItems = mutableListOf<HabitUiItem>()
        fun addWithChildren(item: HabitUiItem) {
            orderedItems.add(item)
            childrenMap[item.habit.id]?.forEach { child -> addWithChildren(child) }
        }
        rootItems.forEach { root -> addWithChildren(root) }
        return orderedItems
    }

    fun calculateProfileStats(habits: List<Habit>, logs: List<HabitLog>, perfectDaysState: PerfectDaysStats): ProfileStats {
        val totalGlobalCompletions = habits.sumOf { habit ->
            val compDateStr = habit.completedAt?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
            }
            val effectiveLogs = if (habit.isFinishable && habit.isCompletedGoal && compDateStr != null) {
                logs.filter { it.date <= compDateStr }
            } else logs
            HabitCalculationEngine.getCompletedLogsCount(habit, effectiveLogs, "ALL", referenceTimeMs = if (habit.isFinishable && habit.isCompletedGoal) habit.completedAt else null)
        }
        val unlockedCompletions = listOf(10, 50, 200, 500).count { totalGlobalCompletions >= it }
        val perfectDaysStreak = perfectDaysState.perfectDaysStreak
        val unlockedPerfectDays = listOf(7, 30, 100).count { perfectDaysStreak >= it }
        val habitStreaks = habits.map { habit ->
            val compDateStr = habit.completedAt?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
            }
            val effectiveLogs = if (habit.isFinishable && habit.isCompletedGoal && compDateStr != null) {
                logs.filter { it.date <= compDateStr }
            } else logs
            val (_, longestStreak) = HabitCalculationEngine.calculateStreak(habit, effectiveLogs, targetDateStr = compDateStr)
            val completions = HabitCalculationEngine.getCompletedLogsCount(habit, effectiveLogs, "ALL", referenceTimeMs = if (habit.isFinishable && habit.isCompletedGoal) habit.completedAt else null)
            ProfileHabitStreak(habit, longestStreak, completions)
        }
        val unlockedHabitStreaks = habitStreaks.sumOf { streakInfo ->
            val longestStreak = streakInfo.longestStreak
            var count = 0
            if (longestStreak >= 7) count++
            if (longestStreak >= 14) count++
            if (longestStreak >= 30) count++
            if (longestStreak >= 100) count++
            count
        }
        val unlockedMasteredGoals = habits.count { it.isFinishable && it.isCompletedGoal }
        val finishableHabitsCount = habits.count { it.isFinishable }
        val totalUnlockedCount = unlockedCompletions + unlockedPerfectDays + unlockedHabitStreaks + unlockedMasteredGoals
        val totalPossibleCount = 4 + 3 + (habits.filter { !it.isCompletedGoal }.size * 4) + finishableHabitsCount
        return ProfileStats(
            totalGlobalCompletions = totalGlobalCompletions,
            unlockedCompletions = unlockedCompletions,
            perfectDaysStreak = perfectDaysStreak,
            unlockedPerfectDays = unlockedPerfectDays,
            habitStreaks = habitStreaks,
            unlockedHabitStreaks = unlockedHabitStreaks,
            totalUnlockedCount = totalUnlockedCount,
            totalPossibleCount = totalPossibleCount
        )
    }

    fun calculateCurrentWeekDays(startCal: Calendar, lang: String): List<Pair<String, Pair<String, String>>> {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNumFormatter = DateTimeFormatter.ofPattern("d", Locale.US)
        val loc = when (lang) {
            "de" -> Locale.GERMANY
            "ka" -> Locale.forLanguageTag("ka")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "fr" -> Locale.FRANCE
            else -> Locale.US
        }
        val dayNameFormatter = DateTimeFormatter.ofPattern("E", loc)
        val cal = startCal.clone() as Calendar
        val days = mutableListOf<Pair<String, Pair<String, String>>>()
        for (i in 0 until 7) {
            val dateStr = sdfDb.format(cal.time)
            val localDate = LocalDate.parse(dateStr)
            val dayNum = localDate.format(dayNumFormatter)
            val dayName = localDate.format(dayNameFormatter)
            days.add(dateStr to (dayNum to dayName))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    fun formatDisplayDate(dateStr: String, lang: String): String {
        return try {
            val localDate = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            val loc = when (lang) {
                "de" -> Locale.GERMANY
                "ka" -> Locale.forLanguageTag("ka")
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "fr" -> Locale.FRANCE
                else -> Locale.US
            }
            if (localDate == today) {
                when (lang) {
                    "de" -> "Heute"
                    "ka" -> "დღეს"
                    "zh" -> "今天"
                    "fr" -> "Aujourd'hui"
                    else -> "Today"
                }
            } else {
                when (lang) {
                    "de" -> localDate.format(DateTimeFormatter.ofPattern("d. MMMM", loc))
                    "ka" -> localDate.format(DateTimeFormatter.ofPattern("d MMMM", loc))
                    "zh" -> localDate.format(DateTimeFormatter.ofPattern("M月d日", loc))
                    "fr" -> localDate.format(DateTimeFormatter.ofPattern("d MMMM", loc))
                    else -> localDate.format(DateTimeFormatter.ofPattern("MMMM d", loc))
                }
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun calculateStatsDayNamesAndNumbers(lang: String): List<Pair<String, String>> {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNumFormatter = DateTimeFormatter.ofPattern("d", Locale.US)
        val loc = when (lang) {
            "de" -> Locale.GERMANY
            "ka" -> Locale.forLanguageTag("ka")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "fr" -> Locale.FRANCE
            else -> Locale.US
        }
        val dayNameFormatter = DateTimeFormatter.ofPattern("E", loc)
        val todayCal = Calendar.getInstance()
        return (0..6).map { i ->
            val c = todayCal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdfDb.format(c.time)
            val localDate = LocalDate.parse(dateStr)
            localDate.format(dayNumFormatter) to localDate.format(dayNameFormatter)
        }.reversed()
    }
}
