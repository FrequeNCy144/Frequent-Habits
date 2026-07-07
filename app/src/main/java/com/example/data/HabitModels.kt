package com.example.data
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

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
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
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
    val timestamp: Long = System.currentTimeMillis()
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

