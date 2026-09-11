package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.getLogStatus
import com.example.data.isHabitActiveOnDate
import com.example.data.isLogCompleted
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun CalendarDayItem(
    dayStr: String,
    dayNum: String,
    dayName: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    accentColor: Color = PrimaryViolet
) {
    val dayScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(120),
        label = "dayScale"
    )

    val weekdayColor = when {
        !isEnabled -> TextSecondary.copy(alpha = 0.3f)
        isSelected -> Color.White
        isToday -> accentColor
        isFuture -> TextSecondary.copy(alpha = 0.4f)
        else -> TextSecondary
    }

    val dateColor = when {
        !isEnabled -> TextSecondary.copy(alpha = 0.3f)
        isSelected -> Color.White
        isToday -> accentColor
        isFuture -> TextSecondary.copy(alpha = 0.4f)
        else -> TextPrimary
    }

    // Selected day gets the full pill background
    val bgModifier = if (isSelected) {
        Modifier
            .background(color = accentColor, shape = RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp)
    } else {
        Modifier
            .padding(vertical = 10.dp, horizontal = 4.dp)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = dayScale
                scaleY = dayScale
            }
            .clickable(enabled = isEnabled) { onSelect(dayStr) }
            .then(bgModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Weekday Name (e.g. MO)
        Text(
            text = dayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = weekdayColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        // Date Number
        Text(
            text = dayNum,
            style = MaterialTheme.typography.titleMedium,
            color = dateColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

fun getHabitProgressForDate(dateStr: String, habits: List<Habit>, logs: List<HabitLog>): Pair<Int, Int> {
    val activeHabits = habits.filter { isHabitActiveOnDate(it, dateStr) }
    if (activeHabits.isEmpty()) return 0 to 0

    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
    var completedCount = 0
    var nonPausedActiveCount = 0
    activeHabits.forEach { habit ->
        val log = logsMap[habit.id]
        val isPaused = log != null && log.isPaused
        if (!isPaused) {
            nonPausedActiveCount++
            val isCompleted = isLogCompleted(habit, log)
            if (isCompleted) {
                completedCount++
            }
        }
    }
    return completedCount to nonPausedActiveCount
}

fun getDayCombinedStatus(dateStr: String, habits: List<Habit>, logs: List<HabitLog>): String {
    val todayStr = LocalDate.now().toString()
    if (dateStr > todayStr) return "INACTIVE"

    val activeHabits = habits.filter { isHabitActiveOnDate(it, dateStr) }
    if (activeHabits.isEmpty()) return "INACTIVE"

    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
    
    var anyPending = false
    var anyFailed = false
    var anySuccess = false
    var anyPaused = false

    activeHabits.forEach { habit ->
        val log = logsMap[habit.id]
        val isWeeklyTargetReached = if (habit.frequency == "TIMES_WEEKLY") {
            val weeklyTargetCount = habit.specificDays.toIntOrNull() ?: 3
            val curDate = try { LocalDate.parse(dateStr) } catch (e: Exception) { LocalDate.now() }
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
                        if (l?.isPaused == true) {
                            pausedInWindow++
                        } else if (l == null || isLogCompleted(habit, l)) {
                            cleanInWindow++
                        }
                    }
                }
                val adjustedTarget = (weeklyTargetCount - pausedInWindow).coerceAtLeast(1)
                cleanInWindow >= adjustedTarget
            } else {
                val startStr = startOf7Days.toString()
                val endStr = curDate.toString()
                val habitLogsInWindow = logs.filter { l ->
                    l.habitId == habit.id && l.date >= startStr && l.date <= endStr
                }
                val pausedInWindow = habitLogsInWindow.count { it.isPaused }
                val weeklyLoggedCount = habitLogsInWindow.count { !it.isPaused && isLogCompleted(habit, it) }
                val adjustedTarget = (weeklyTargetCount - pausedInWindow).coerceAtLeast(1)
                weeklyLoggedCount >= adjustedTarget
            }
        } else false

        val hStatus = getLogStatus(habit, log, dateStr, "1970-01-01", todayStr, isWeeklyTargetReached)

        if (hStatus == "PENDING") anyPending = true
        if (hStatus == "FAILED") anyFailed = true
        if (hStatus == "SUCCESS") anySuccess = true
        if (hStatus == "PAUSED") anyPaused = true
    }

    return when {
        anyPending -> "PENDING"
        anyFailed -> "FAILED"
        anySuccess -> "SUCCESS"
        anyPaused -> "PAUSED"
        else -> "SUCCESS"
    }
}
