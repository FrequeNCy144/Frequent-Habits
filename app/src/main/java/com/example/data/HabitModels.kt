package com.example.data
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
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
    val customReminders: String = "",
    val isArchived: Boolean = false,
    val description: String = "",
    val clickIncrement: Float = 1.0f,
    val minimalViableValue: Float? = null,
    val minimalViableText: String = "",
    val why: String = "",
    val stackedOnHabitId: Int? = null,
    val isFinishable: Boolean = false,
    val totalTargetValue: Float? = null,
    val isCompletedGoal: Boolean = false,
    val completedAt: Long? = null,
    val completionNote: String = "",
    val difficulty: String = "MEDIUM" // "EASY", "MEDIUM", "HARD", "ULTRA"
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
    val isPaused: Boolean = false,
    val isMinimalViable: Boolean = false
)

@Entity(tableName = "daily_notes")
@Immutable
data class DailyNote(
    @PrimaryKey val date: String, // format: "yyyy-MM-dd"
    val content: String
)

@Entity(tableName = "time_capsule_notes")
@Immutable
data class TimeCapsuleNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "MONTHLY" or "YEARLY"
    val targetPeriod: String, // format: "yyyy-MM" or "yyyy"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "milestone_rewards",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MilestoneReward(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) val habitId: Int,
    val rewardText: String,
    val description: String = "",
    val isRedeemed: Boolean = false,
    val unlockedAt: Long = 0L,
    val conditionType: String = "", // "STREAK", "COMPLETIONS", "TROPHY_COUPLED"
    val conditionValue: Int = 0, // e.g., 30 for 30 completions
    val trophyId: String = "" // if conditionType == "TROPHY_COUPLED", e.g., "STREAK_BRONZE"
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
    val past7DaysStatuses: List<String>,
    val past7DaysDates: List<String> = emptyList(),
    val monthGridData: List<List<CalendarGridCellData?>>,
    val yearGridData: List<List<CalendarGridCellData>>,
    val yearMonthLabels: List<Pair<Int, String>>
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
    val targetStats: HabitTargetStats? = null,
    val weekdayStats: List<Triple<Int, Int, Int>> = emptyList(),
    val weekdayGridData: List<List<Pair<java.time.LocalDate, String>>> = emptyList(),
    val weeksWithMonthLabels: List<String> = emptyList()
)

@Immutable
data class PerfectDaysStats(
    val totalPerfectDays: Int = 0,
    val perfectDaysStreak: Int = 0,
    val currentStreak: Int = 0,
    val totalCompletedHabits: Int = 0,
    val totalCompletionRate: Int = 0
)

fun isHabitActiveOnDate(habit: Habit, dateStr: String): Boolean {
    if (habit.isArchived) return false
    if (habit.isCompletedGoal) {
        val compDateStr = habit.completedAt?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it))
        } ?: java.time.LocalDate.now().toString()
        if (dateStr > compDateStr) return false
    }
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
    val targetVal = if (log.isMinimalViable && habit.type == "NUMBER" && habit.minimalViableValue != null) {
        habit.minimalViableValue
    } else {
        habit.targetValue
    }
    return when (log.value) {
        -2f -> true
        -1f -> false
        else -> {
            if (habit.type == "BINARY") {
                if (log.value > 0f) !habit.isNegative else habit.isNegative
            } else {
                if (habit.isNegative) {
                    log.value < targetVal
                } else {
                    log.value >= targetVal
                }
            }
        }
    }
}

fun getLogStatus(habit: Habit, log: HabitLog?, dateStr: String, startSdfStr: String, todayStr: String, isWeeklyTargetReached: Boolean = false): String {
    if (dateStr < startSdfStr || dateStr > todayStr || !isHabitActiveOnDate(habit, dateStr)) {
        return "INACTIVE"
    }
    if (log != null && log.isPaused) {
        return "PAUSED"
    }
    if (log != null && log.value == -1f) {
        return "FAILED"
    }
    if (isLogCompleted(habit, log)) {
        return "SUCCESS"
    }
    if (habit.isNegative) {
        return "FAILED"
    }
    return "PENDING"
}

@Immutable
data class ProfileHabitStreak(
    val habit: Habit,
    val longestStreak: Int,
    val totalCompletions: Int = 0
)

@Immutable
data class ProfileStats(
    val totalGlobalCompletions: Int = 0,
    val unlockedCompletions: Int = 0,
    val perfectDaysStreak: Int = 0,
    val unlockedPerfectDays: Int = 0,
    val habitStreaks: List<ProfileHabitStreak> = emptyList(),
    val unlockedHabitStreaks: Int = 0,
    val totalUnlockedCount: Int = 0,
    val totalPossibleCount: Int = 0
)

data class UnlockedAchievementInfo(
    val id: String,
    val type: String, // "STREAK", "COMPLETIONS", "PERFECT_DAYS", "CUSTOM_MILESTONE"
    val tier: String, // "WOOD", "BRONZE", "SILVER", "GOLD", "COMP_10", "PERF_7", etc.
    val title: String,
    val description: String,
    val rewardText: String? = null,
    val rewardDescription: String? = null,
    val habitName: String? = null,
    val habitColor: String? = null,
    val habitIcon: String? = null,
    val milestoneRewardId: Int? = null
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
    val hasLog: Boolean,
    val isWeeklyTargetReached: Boolean = false,
    val weeklyLoggedCount: Int = 0,
    val weeklyTargetCount: Int = 0,
    val streak: Int = 0,
    val isMinimalViable: Boolean = false,
    val totalAchievedValue: Float = 0f,
    val isGoalTargetReached: Boolean = false
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







