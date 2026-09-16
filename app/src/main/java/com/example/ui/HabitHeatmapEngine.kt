package com.example.ui

import com.example.data.CalendarGridCellData
import com.example.data.Habit
import com.example.data.OverallCalendarData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HabitHeatmapEngine {

    fun calculateMonthGridData(
        monthCal: Calendar,
        overallCal: OverallCalendarData,
        habits: List<Habit>,
        minDate: String
    ): List<List<CalendarGridCellData?>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

        val temp = monthCal.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val year = temp.get(Calendar.YEAR)
        val month = temp.get(Calendar.MONTH)

        temp.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        temp.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        val weeksList = mutableListOf<List<CalendarGridCellData?>>()
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
                    val progress = overallCal.progressMap[dateStr] ?: (0 to 0)
                    val combinedStatus = overallCal.statusMap[dateStr] ?: "INACTIVE"
                    val isToday = dateStr == today
                    val isFuture = dateStr > today
                    val isOutOfRange = false
                    val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)

                    weekDays.add(
                        CalendarGridCellData(
                            day = dayOfMonth,
                            dateStr = dateStr,
                            combinedStatus = combinedStatus,
                            isToday = isToday,
                            isFuture = isFuture,
                            total = progress.second,
                            completed = progress.first,
                            isOutOfRange = isOutOfRange
                        )
                    )
                } else {
                    weekDays.add(null)
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (hasDaysInMonth) {
                weeksList.add(weekDays)
            }
        }
        return weeksList
    }

    fun calculateYearGridData(
        overallCal: OverallCalendarData,
        habits: List<Habit>,
        minDate: String
    ): List<List<CalendarGridCellData>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

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

        val weeksList = mutableListOf<List<CalendarGridCellData>>()
        val cursor = startCal.clone() as Calendar
        for (w in 0 until 24) {
            val weekDays = mutableListOf<CalendarGridCellData>()
            for (d in 0 until 7) {
                val dateStr = sdf.format(cursor.time)
                val progress = overallCal.progressMap[dateStr] ?: (0 to 0)
                val combinedStatus = overallCal.statusMap[dateStr] ?: "INACTIVE"
                val isToday = dateStr == today
                val isFuture = dateStr > today
                val isOutOfRange = false
                val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)

                weekDays.add(
                    CalendarGridCellData(
                        day = dayOfMonth,
                        dateStr = dateStr,
                        combinedStatus = combinedStatus,
                        isToday = isToday,
                        isFuture = isFuture,
                        total = progress.second,
                        completed = progress.first,
                        isOutOfRange = isOutOfRange
                    )
                )
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            weeksList.add(weekDays)
        }
        return weeksList
    }

    fun calculateYearMonthLabels(
        yearGrid: List<List<CalendarGridCellData>>,
        lang: String
    ): List<Pair<Int, String>> {
        val labels = mutableListOf<Pair<Int, String>>()
        val sdfMonth = SimpleDateFormat(
            "MMM",
            when (lang) {
                "de" -> Locale.GERMANY
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "fr" -> Locale.FRANCE
                else -> Locale.US
            }
        )
        var lastAddedIndex = -10
        var lastMonthStr = ""

        yearGrid.forEachIndexed { index, weekDays ->
            val mondayDate = weekDays.first().dateStr
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(mondayDate)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val monthStr = sdfMonth.format(cal.time)
                if (monthStr != lastMonthStr) {
                    if (index - lastAddedIndex >= 3) {
                        labels.add(index to monthStr)
                        lastAddedIndex = index
                    }
                    lastMonthStr = monthStr
                }
            }
        }
        return labels
    }
}
