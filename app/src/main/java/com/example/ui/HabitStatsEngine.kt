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

object HabitStatsEngine {

    fun calculateOverallCalendar(habits: List<Habit>, logs: List<HabitLog>): OverallCalendarData {
        val statusMap = mutableMapOf<String, String>()
        val progressMap = mutableMapOf<String, Pair<Int, Int>>()
        if (habits.isNotEmpty()) {
            val oldestHabitDateMs = habits.map {
                if (it.startDate > 946684800000L) {
                    it.startDate
                } else if (it.createdAt > 946684800000L) {
                    it.createdAt
                } else {
                    System.currentTimeMillis()
                }
            }.minOrNull() ?: System.currentTimeMillis()
            val startCal = Calendar.getInstance().apply { timeInMillis = oldestHabitDateMs }
            val todayCal = Calendar.getInstance()
            val maxCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }

            val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdfDb.format(todayCal.time)

            val cal = startCal.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            while (cal.timeInMillis <= maxCal.timeInMillis) {
                val dateStr = sdfDb.format(cal.time)

                val activeHabits = habits.filter { habit ->
                    val isCompletedInFuture = if (habit.isFinishable && habit.isCompletedGoal && habit.completedAt != null) {
                        val compDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(habit.completedAt))
                        dateStr > compDateStr
                    } else {
                        false
                    }
                    !isCompletedInFuture && isHabitActiveOnDate(habit, dateStr)
                }
                if (activeHabits.isNotEmpty()) {
                    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
                    var completedCount = 0
                    var nonPausedActiveCount = 0

                    var anyPending = false
                    var anyFailed = false
                    var anySuccess = false
                    var anyPaused = false

                    activeHabits.forEach { habit ->
                        val log = logsMap[habit.id]
                        val isPaused = log != null && log.isPaused
                        val isWeeklyTargetReached = if (habit.frequency == "TIMES_WEEKLY") {
                            val weeklyTargetCount = habit.specificDays.toIntOrNull() ?: 3
                            val curDate = try { LocalDate.parse(dateStr) } catch (e: Exception) { LocalDate.now() }
                            val startOf7Days = curDate.minusDays(6).toString()
                            val endOf7Days = curDate.toString()
                            val weeklyLoggedCount = logs.filter { l ->
                                l.habitId == habit.id && l.date >= startOf7Days && l.date <= endOf7Days && isLogCompleted(habit, l)
                            }.size
                            weeklyLoggedCount >= weeklyTargetCount
                        } else false
                        if (!isPaused) {
                            nonPausedActiveCount++
                            val isCompleted = isLogCompleted(habit, log)
                            if (isCompleted) {
                                completedCount++
                            }
                        }

                        val hStatus = getLogStatus(habit, log, dateStr, "1970-01-01", todayStr, isWeeklyTargetReached)
                        if (hStatus == "PENDING") anyPending = true
                        if (hStatus == "FAILED") anyFailed = true
                        if (hStatus == "SUCCESS") anySuccess = true
                        if (hStatus == "PAUSED") anyPaused = true
                    }

                    progressMap[dateStr] = completedCount to nonPausedActiveCount

                    val combinedStatus = if (dateStr > todayStr) "INACTIVE" else {
                        when {
                            anyPending -> "PENDING"
                            anyFailed -> "FAILED"
                            anySuccess -> "SUCCESS"
                            anyPaused -> "PAUSED"
                            else -> "SUCCESS"
                        }
                    }
                    statusMap[dateStr] = combinedStatus
                } else {
                    progressMap[dateStr] = 0 to 0
                    statusMap[dateStr] = "INACTIVE"
                }

                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return OverallCalendarData(statusMap, progressMap)
    }

    fun calculateStatsScreenData(
        habits: List<Habit>,
        logs: List<HabitLog>,
        monthCal: Calendar,
        lang: String,
        folders: List<HabitFolder> = emptyList()
    ): List<HabitStatModel> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance()
        val daysList = (0..6).map { i ->
            val c = todayCal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            sdf.format(c.time)
        }.reversed()

        val folderMap = folders.associateBy { it.id }
        val activeHabits = habits.filter { !(it.isFinishable && it.isCompletedGoal) }
        val sortedActiveHabits = if (folders.isNotEmpty()) {
            activeHabits.sortedWith(
                compareBy<Habit> { habit ->
                    val folder = habit.folderId?.let { folderMap[it] }
                    folder?.sortOrder ?: (10000 + habit.sortOrder)
                }.thenBy { habit ->
                    habit.sortOrder
                }.thenByDescending { habit ->
                    habit.id
                }
            )
        } else {
            activeHabits
        }

        return sortedActiveHabits.map { habit ->
            val habitLogs = logs.filter { it.habitId == habit.id }
            val logsByDate = habitLogs.associateBy { it.date }
            val strength = calculateHabitStrength(habit, logs)

            val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
            val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startSdfStr = startSdf.format(Date(validStartMillis))
            val todayStr = startSdf.format(Date())
            val past7DaysStatuses = daysList.map { dateStr ->
                val log = logsByDate[dateStr]
                getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
            }

            val temp = monthCal.clone() as Calendar
            temp.set(Calendar.DAY_OF_MONTH, 1)
            val year = temp.get(Calendar.YEAR)
            val month = temp.get(Calendar.MONTH)

            temp.firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            temp.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

            val monthGridList = mutableListOf<List<CalendarGridCellData?>>()
            val cursor = temp.clone() as Calendar

            for (w in 0 until 6) {
                if (w >= 4 && cursor.get(Calendar.MONTH) != month) {
                    break
                }

                val weekDays = mutableListOf<CalendarGridCellData?>()
                var hasDaysInMonth = false
                for (d in 0 until 7) {
                    val cellYear = cursor.get(Calendar.YEAR)
                    val cellMonth = cursor.get(Calendar.MONTH)
                    val dateStr = sdf.format(cursor.time)

                    if (cellYear == year && cellMonth == month) {
                        hasDaysInMonth = true
                        val isActive = isHabitActiveOnDate(habit, dateStr)
                        var progressTotal = 0
                        var progressCompleted = 0
                        var combinedStatus = "INACTIVE"

                        if (isActive) {
                            val log = logsByDate[dateStr]
                            val isPaused = log != null && log.isPaused
                            if (!isPaused) {
                                progressTotal = 1
                                if (isLogCompleted(habit, log)) {
                                    progressCompleted = 1
                                }
                            }
                            combinedStatus = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
                        }

                        val isToday = dateStr == todayStr
                        val isFuture = dateStr > todayStr
                        val isOutOfRange = false
                        val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)

                        weekDays.add(
                            CalendarGridCellData(
                                day = dayOfMonth,
                                dateStr = dateStr,
                                combinedStatus = combinedStatus,
                                isToday = isToday,
                                isFuture = isFuture,
                                total = progressTotal,
                                completed = progressCompleted,
                                isOutOfRange = isOutOfRange
                            )
                        )
                    } else {
                        weekDays.add(null)
                    }
                    cursor.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (hasDaysInMonth) {
                    monthGridList.add(weekDays)
                }
            }

            val currentCal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            }

            val startCal = (currentCal.clone() as Calendar).apply {
                add(Calendar.WEEK_OF_YEAR, -23)
            }

            val yearWeeksList = mutableListOf<List<CalendarGridCellData>>()
            val yearCursor = startCal.clone() as Calendar
            for (w in 0 until 24) {
                val weekDays = mutableListOf<CalendarGridCellData>()
                for (d in 0 until 7) {
                    val dateStr = sdf.format(yearCursor.time)

                    val isActive = isHabitActiveOnDate(habit, dateStr)
                    var progressTotal = 0
                    var progressCompleted = 0
                    var combinedStatus = "INACTIVE"

                    if (isActive) {
                        val log = logsByDate[dateStr]
                        val isPaused = log != null && log.isPaused
                        if (!isPaused) {
                            progressTotal = 1
                            if (isLogCompleted(habit, log)) {
                                progressCompleted = 1
                            }
                        }
                        combinedStatus = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
                    }

                    val isToday = dateStr == todayStr
                    val isFuture = dateStr > todayStr
                    val isOutOfRange = false
                    val dayOfMonth = yearCursor.get(Calendar.DAY_OF_MONTH)

                    weekDays.add(
                        CalendarGridCellData(
                            day = dayOfMonth,
                            dateStr = dateStr,
                            combinedStatus = combinedStatus,
                            isToday = isToday,
                            isFuture = isFuture,
                            total = progressTotal,
                            completed = progressCompleted,
                            isOutOfRange = isOutOfRange
                        )
                    )
                    yearCursor.add(Calendar.DAY_OF_YEAR, 1)
                }
                yearWeeksList.add(weekDays)
            }

            val yearMonthLabels = mutableListOf<Pair<Int, String>>()
            val sdfMonth = SimpleDateFormat(
                "MMM",
                when (lang) {
                    "de" -> Locale.GERMANY
                    "zh" -> Locale.SIMPLIFIED_CHINESE
                    else -> Locale.US
                }
            )
            var lastAddedIndex = -10
            var lastMonthStr = ""

            yearWeeksList.forEachIndexed { index, weekDays ->
                val mondayDate = weekDays.first().dateStr
                val date = sdf.parse(mondayDate)
                if (date != null) {
                    val calForLabel = Calendar.getInstance().apply { time = date }
                    val monthStr = sdfMonth.format(calForLabel.time)
                    if (monthStr != lastMonthStr) {
                        if (index - lastAddedIndex >= 3) {
                            yearMonthLabels.add(index to monthStr)
                            lastAddedIndex = index
                        }
                        lastMonthStr = monthStr
                    }
                }
            }

            HabitStatModel(
                habit = habit,
                strength = strength,
                past7DaysStatuses = past7DaysStatuses,
                past7DaysDates = daysList,
                monthGridData = monthGridList,
                yearGridData = yearWeeksList,
                yearMonthLabels = yearMonthLabels
            )
        }
    }

}