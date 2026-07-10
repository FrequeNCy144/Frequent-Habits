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
    val completionRate: Int = 0
)

@Immutable
data class PerfectDaysStats(
    val totalPerfectDays: Int = 0,
    val perfectDaysStreak: Int = 0,
    val totalCompletedHabits: Int = 0,
    val totalCompletionRate: Int = 0
)

fun isHabitActiveOnDate(habit: Habit, dateStr: String): Boolean {
    val dateMs = try {
        val baseTime = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time ?: 0L
        baseTime + 86399000L // cover full day
    } catch (e: Exception) {
        0L
    }
    val validStart = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    if (validStart > dateMs) {
        return false
    }

    val calendar = Calendar.getInstance(Locale.GERMANY).apply {
        time = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    return when (habit.frequency) {
        "DAILY", "TIMES_WEEKLY" -> true
        "SPECIFIC" -> {
            val isoDayNum = when (dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
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
    if (log != null && log.value > 0f) {
        return "FAILED"
    }
    return "PENDING"
}


