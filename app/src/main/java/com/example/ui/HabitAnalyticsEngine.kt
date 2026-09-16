package com.example.ui

import com.example.data.Habit
import com.example.data.HabitLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object HabitAnalyticsEngine {

    fun calculateAnalyticsUiState(
        habitId: Int?,
        habits: List<Habit>,
        logs: List<HabitLog>,
        lang: String,
        filter: String,
        verlaufOffset: Int
    ): HabitAnalyticsUiState? {
        if (habitId == null) return null
        val habit = habits.find { it.id == habitId } ?: return null
        val habitLogs = logs.filter { it.habitId == habitId }

        val today = LocalDate.now()
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startDate = Instant.ofEpochMilli(validStartMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        val minVerlaufOffset = when (filter) {
            "WEEK" -> {
                val todayMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                val startMonday = startDate.minusDays((startDate.dayOfWeek.value - 1).toLong())
                val diffDays = ChronoUnit.DAYS.between(startMonday, todayMonday)
                val diffWeeks = diffDays / 7
                -diffWeeks.toInt()
            }
            "MONTH" -> {
                val diffMonths = ChronoUnit.MONTHS.between(
                    startDate.withDayOfMonth(1),
                    today.withDayOfMonth(1)
                )
                -diffMonths.toInt()
            }
            "YEAR" -> {
                val diffYears = ChronoUnit.YEARS.between(
                    startDate.withDayOfYear(1),
                    today.withDayOfYear(1)
                )
                -diffYears.toInt()
            }
            else -> 0
        }.coerceAtMost(0)

        val coercedOffset = verlaufOffset.coerceAtLeast(minVerlaufOffset).coerceAtMost(0)

        val verlaufTitle = when (filter) {
            "WEEK" -> {
                val targetMonday = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(coercedOffset.toLong())
                val targetSunday = targetMonday.plusDays(6)
                val formatter = DateTimeFormatter.ofPattern(
                    "d. MMM",
                    when (lang) {
                        "de" -> Locale.GERMANY
                        "zh" -> Locale.SIMPLIFIED_CHINESE
                        "fr" -> Locale.FRANCE
                        else -> Locale.US
                    }
                )
                "${targetMonday.format(formatter)} - ${targetSunday.format(formatter)} ${targetSunday.year}"
            }
            "MONTH" -> {
                val targetMonthDate = today.withDayOfMonth(1).plusMonths(coercedOffset.toLong())
                val formatter = DateTimeFormatter.ofPattern(
                    "MMMM yyyy",
                    when (lang) {
                        "de" -> Locale.GERMANY
                        "zh" -> Locale.SIMPLIFIED_CHINESE
                        "fr" -> Locale.FRANCE
                        else -> Locale.US
                    }
                )
                targetMonthDate.format(formatter)
            }
            else -> {
                val targetYearDate = today.plusYears(coercedOffset.toLong())
                "${targetYearDate.year}"
            }
        }

        val canPrevVerlauf = coercedOffset > minVerlaufOffset
        val canNextVerlauf = coercedOffset < 0

        val barData = mutableListOf<Pair<String, Float>>()
        val todayStr = today.toString()
        val startSdfStr = startDate.toString()

        when (filter) {
            "WEEK" -> {
                val targetMonday = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(coercedOffset.toLong())
                val labels = if (lang == "de") listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                else if (lang == "ka") listOf("ორშ", "სამ", "ოთხ", "ხუთ", "პარ", "შაბ", "კვი")
                else if (lang == "zh") listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

                for (i in 0 until 7) {
                    val dDate = targetMonday.plusDays(i.toLong())
                    val dStr = dDate.toString()
                    val isOutOfRange = dStr < startSdfStr || dStr > todayStr
                    val ratio = if (isOutOfRange) {
                        0f
                    } else {
                        val logForDate = habitLogs.find { it.date == dStr }
                        if (habit.type == "BINARY") {
                            val isSuccess = if (logForDate == null) {
                                habit.isNegative
                            } else {
                                when (logForDate.value) {
                                    -2f -> true
                                    -1f -> false
                                    else -> !habit.isNegative
                                }
                            }
                            if (isSuccess) 1f else 0f
                        } else {
                            if (logForDate == null) {
                                if (habit.isNegative) 1f else 0f
                            } else {
                                if (habit.isNegative) {
                                    if (logForDate.value >= habit.targetValue) 0f else 1f
                                } else {
                                    (logForDate.value / habit.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                }
                            }
                        }
                    }
                    barData.add(labels[i] to ratio)
                }
            }
            "MONTH" -> {
                val targetMonthDate = today.withDayOfMonth(1).plusMonths(coercedOffset.toLong())
                val daysCount = targetMonthDate.lengthOfMonth()

                for (i in 1..daysCount) {
                    val dDate = targetMonthDate.withDayOfMonth(i)
                    val dStr = dDate.toString()
                    val isOutOfRange = dStr < startSdfStr || dStr > todayStr
                    val ratio = if (isOutOfRange) {
                        0f
                    } else {
                        val logForDate = habitLogs.find { it.date == dStr }
                        if (habit.type == "BINARY") {
                            val isSuccess = if (logForDate == null) {
                                habit.isNegative
                            } else {
                                when (logForDate.value) {
                                    -2f -> true
                                    -1f -> false
                                    else -> !habit.isNegative
                                }
                            }
                            if (isSuccess) 1f else 0f
                        } else {
                            if (logForDate == null) {
                                if (habit.isNegative) 1f else 0f
                            } else {
                                if (habit.isNegative) {
                                    if (logForDate.value >= habit.targetValue) 0f else 1f
                                } else {
                                    (logForDate.value / habit.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                }
                            }
                        }
                    }
                    barData.add(i.toString() to ratio)
                }
            }
            else -> {
                val targetYear = today.year + coercedOffset
                val months = if (lang == "de")
                    listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
                else if (lang == "ka")
                    listOf("იან", "თებ", "მარ", "აპრ", "მაი", "ივნ", "ივლ", "აგვ", "სექ", "ოქტ", "ნოე", "დეკ")
                else if (lang == "zh")
                    listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                else
                    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                for (m in 1..12) {
                    val targetMonthDate = LocalDate.of(targetYear, m, 1)
                    val maxDays = targetMonthDate.lengthOfMonth()
                    var completedDays = 0
                    var validDaysInMonth = 0

                    for (d in 1..maxDays) {
                        val dDate = LocalDate.of(targetYear, m, d)
                        val dStr = dDate.toString()
                        if (dStr >= startSdfStr && dStr <= todayStr) {
                            validDaysInMonth++
                            val logForDate = habitLogs.find { it.date == dStr }
                            val isSuccess = if (habit.type == "BINARY") {
                                if (logForDate == null) {
                                    habit.isNegative
                                } else {
                                    when (logForDate.value) {
                                        -2f -> true
                                        -1f -> false
                                        else -> !habit.isNegative
                                    }
                                }
                            } else {
                                if (logForDate == null) {
                                    habit.isNegative
                                } else {
                                    if (habit.isNegative) {
                                        logForDate.value < habit.targetValue
                                    } else {
                                        logForDate.value >= habit.targetValue
                                    }
                                }
                            }
                            if (isSuccess) completedDays++
                        }
                    }
                    val ratio = if (validDaysInMonth > 0) completedDays.toFloat() / validDaysInMonth.toFloat() else 0f
                    barData.add(months[m - 1] to ratio)
                }
            }
        }

        return HabitAnalyticsUiState(
            minVerlaufOffset = minVerlaufOffset,
            verlaufTitle = verlaufTitle,
            canPrevVerlauf = canPrevVerlauf,
            canNextVerlauf = canNextVerlauf,
            barData = barData
        )
    }
}
