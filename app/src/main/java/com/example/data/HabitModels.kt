package com.example.data
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "habits"
)
@Immutable
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Allgemein",
    val icon: String = "sparkle",
    val color: String = "purple",
    val isNegative: Boolean = false, // true = "Abgewöhnen", false = "Aufbauen"
    val type: String = "BINARY", // "BINARY" (Ja/Nein) or "NUMBER" (Zahlenbasiert)
    val unit: String = "", // e.g. "Liter", "Std", "km"
    val targetValue: Float = 1.0f,
    val frequency: String = "DAILY", // "DAILY", etc.
    val specificDays: String = "", // e.g. "1,3,5" for Monday, Wednesday, Friday
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["date"])]
)
@Immutable
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String, // format: "yyyy-MM-dd"
    val value: Float = 1.0f, // 1.0f for binary complete, or logged amount for numbers
    val timestamp: Long = System.currentTimeMillis(),
    val isPaused: Boolean = false
)

@Entity(tableName = "daily_notes")
@Immutable
data class DailyNote(
    @PrimaryKey val date: String, // format: "yyyy-MM-dd"
    val content: String
)

@Immutable
data class StableHabitLogs(val list: List<HabitLog>)

@Immutable
data class StableHabitList(val list: List<Habit>)

@Immutable
data class StableHabitLogsMap(val map: Map<String, HabitLog>)

@Immutable
data class CalendarCellState(
    val id: String,
    val dayNum: String,
    val isCompleted: Boolean,
    val status: String = "PENDING" // "SUCCESS", "FAILED", "PENDING"
)

@Immutable
data class HabitStatModel(
    val habit: Habit,
    val strength: Int,
    val past7DaysStatuses: List<String>
)

@Immutable
data class TargetPeriodStats(
    val actualValue: Float,
    val targetValue: Float,
    val isNumerical: Boolean
)

@Immutable
data class HabitTargetStats(
    val today: TargetPeriodStats,
    val week: TargetPeriodStats,
    val month: TargetPeriodStats,
    val quarter: TargetPeriodStats,
    val year: TargetPeriodStats
)

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
    
    // Calculate targets
    val todayTargetCount = if (isHabitActiveOnDate(habit, todayStr)) 1f else 0f
    val weekTargetCount = getTargetCount(startOfWeek, endOfWeek)
    val monthTargetCount = getTargetCount(startOfMonth, endOfMonth)
    val quarterTargetCount = getTargetCount(startOfQuarter, endOfQuarter)
    val yearTargetCount = getTargetCount(startOfYear, endOfYear)
    
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
    
    val weekActual = getActualStatsInPeriod(startOfWeek, endOfWeek)
    val monthActual = getActualStatsInPeriod(startOfMonth, endOfMonth)
    val quarterActual = getActualStatsInPeriod(startOfQuarter, endOfQuarter)
    val yearActual = getActualStatsInPeriod(startOfYear, endOfYear)
    
    return HabitTargetStats(
        today = TargetPeriodStats(todayActual, todayTarget, isNumerical),
        week = TargetPeriodStats(weekActual, weekTarget, isNumerical),
        month = TargetPeriodStats(monthActual, monthTarget, isNumerical),
        quarter = TargetPeriodStats(quarterActual, quarterTarget, isNumerical),
        year = TargetPeriodStats(yearActual, yearTarget, isNumerical)
    )
}

@Immutable
data class HabitDetailUiState(
    val habit: Habit,
    val currentStreak: Int,
    val longestStreak: Int,
    val strength: Int,
    val thisWeekCount: Int,
    val thisMonthCount: Int,
    val thisYearCount: Int,
    val totalCount: Int,
    val calendarGridRows: List<List<CalendarCellState?>>,
    val monthName: String,
    val canPrevMonth: Boolean = true,
    val canNextMonth: Boolean = true,
    val completionRate: Int = 0,
    val targetStats: HabitTargetStats? = null
)

@Immutable
data class PerfectDaysStats(
    val totalPerfectDays: Int = 0,
    val perfectDaysStreak: Int = 0,
    val totalCompletedHabits: Int = 0,
    val totalCompletionRate: Int = 0
)

fun isHabitActiveOnDate(habit: Habit, dateStr: String): Boolean {
    val date = try {
        java.time.LocalDate.parse(dateStr)
    } catch (e: Exception) {
        return false
    }
    
    val validStart = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    val startLocalDate = java.time.Instant.ofEpochMilli(validStart).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    
    if (startLocalDate.isAfter(date)) {
        return false
    }

    return when (habit.frequency) {
        "DAILY", "TIMES_WEEKLY" -> true
        "SPECIFIC" -> {
            val isoDayNum = date.dayOfWeek.value // Monday = 1, ..., Sunday = 7
            val specList = habit.specificDays.split(",").mapNotNull { it.trim().toIntOrNull() }
            specList.contains(isoDayNum)
        }
        else -> true
    }
}

fun isLogCompleted(habit: Habit, log: HabitLog?): Boolean {
    if (log == null) {
        return habit.isNegative
    }
    if (log.isPaused) {
        return false
    }
    return when (log.value) {
        -2f -> true
        -1f -> false
        else -> {
            if (habit.type == "BINARY") {
                !habit.isNegative
            } else {
                if (habit.isNegative) {
                    log.value < habit.targetValue
                } else {
                    log.value >= habit.targetValue
                }
            }
        }
    }
}

fun getLogStatus(habit: Habit, log: HabitLog?, dateStr: String, startSdfStr: String, todayStr: String): String {
    if (dateStr < startSdfStr || dateStr > todayStr || !isHabitActiveOnDate(habit, dateStr)) {
        return "INACTIVE"
    }
    if (log != null && log.isPaused) {
        return "PAUSED"
    }
    val isCompleted = isLogCompleted(habit, log)
    if (isCompleted) {
        return "SUCCESS"
    }
    if (habit.isNegative) {
        return "FAILED"
    }
    if (log != null && log.value == -1f) {
        return "FAILED"
    }
    return "PENDING"
}

@Immutable
data class ProfileHabitStreak(
    val habit: Habit,
    val longestStreak: Int
)

@Immutable
data class ProfileStats(
    val totalGlobalCompletions: Int = 0,
    val unlockedCompletions: Int = 0,
    val unlockedPerfectDays: Int = 0,
    val habitStreaks: List<ProfileHabitStreak> = emptyList(),
    val unlockedHabitStreaks: Int = 0,
    val totalUnlockedCount: Int = 0,
    val totalPossibleCount: Int = 0
)

@Immutable
data class OverallCalendarData(
    val statusMap: Map<String, String> = emptyMap(),
    val progressMap: Map<String, Pair<Int, Int>> = emptyMap()
)

@Immutable
data class HabitUiItem(
    val habit: Habit,
    val currentValue: Float,
    val isCompleted: Boolean,
    val isFailed: Boolean,
    val isPaused: Boolean,
    val hasLog: Boolean
)

@Immutable
data class CalendarGridCellData(
    val day: Int,
    val dateStr: String,
    val combinedStatus: String,
    val isToday: Boolean,
    val isFuture: Boolean,
    val total: Int,
    val completed: Int,
    val isOutOfRange: Boolean = false
)




