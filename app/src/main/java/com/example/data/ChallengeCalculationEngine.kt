package com.example.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ChallengeStats(
    val cleanDays: Int,
    val failedDays: Int,
    val totalActiveDays: Int,
    val currentRatePercent: Int,
    val currentStreak: Int,
    val targetRatePercent: Int,
    val targetDays: Int,
    val isGoalReached: Boolean
)

object ChallengeCalculationEngine {

    fun calculateTotalAchieved(habit: Habit, logs: List<HabitLog>): Float {
        val stats = getChallengeStats(habit, logs)
        return when (habit.challengeMode) {
            "STREAK" -> stats.currentStreak.toFloat()
            "SUCCESS_RATE" -> stats.cleanDays.toFloat()
            else -> {
                if (habit.isNegative) stats.cleanDays.toFloat()
                else HabitCalculationEngine.calculateTotalAchievedValue(habit, logs)
            }
        }
    }

    fun isGoalTargetReached(habit: Habit, logs: List<HabitLog>): Boolean {
        if (!habit.isFinishable) return false
        val totalTarget = habit.totalTargetValue ?: return false
        if (totalTarget <= 0f) return false

        val stats = getChallengeStats(habit, logs)
        return when (habit.challengeMode) {
            "STREAK" -> stats.currentStreak >= totalTarget.toInt()
            "SUCCESS_RATE" -> {
                stats.totalActiveDays >= totalTarget.toInt() && stats.currentRatePercent >= habit.challengeSuccessRate
            }
            else -> {
                if (habit.isNegative) {
                    stats.cleanDays >= totalTarget.toInt()
                } else {
                    val achieved = HabitCalculationEngine.calculateTotalAchievedValue(habit, logs)
                    achieved >= totalTarget
                }
            }
        }
    }

    fun getChallengeStats(habit: Habit, logs: List<HabitLog>): ChallengeStats {
        val habitLogs = logs.filter { it.habitId == habit.id }
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val habitStartDate = try {
            Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L))
                .atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (e: Exception) {
            LocalDate.now()
        }
        val today = LocalDate.now()
        val target = habit.totalTargetValue?.toInt() ?: 0

        if (habitStartDate.isAfter(today)) {
            return ChallengeStats(
                cleanDays = 0,
                failedDays = 0,
                totalActiveDays = 0,
                currentRatePercent = 100,
                currentStreak = 0,
                targetRatePercent = habit.challengeSuccessRate,
                targetDays = target,
                isGoalReached = false
            )
        }

        val logByDate = habitLogs.associateBy { it.date }

        var successfulDays = 0
        var failedDays = 0
        var totalActiveDays = 0
        var cur = habitStartDate

        while (!cur.isAfter(today)) {
            val dStr = cur.toString()
            val log = logByDate[dStr]
            val isPaused = log?.isPaused == true
            if (isHabitActiveOnDate(habit, dStr) && !isPaused) {
                totalActiveDays++
                val isSuccessful = isDaySuccessful(habit, log)
                if (isSuccessful) {
                    successfulDays++
                } else {
                    failedDays++
                }
            }
            cur = cur.plusDays(1)
        }

        val rate = if (totalActiveDays > 0) {
            ((successfulDays.toFloat() / totalActiveDays.toFloat()) * 100f).toInt()
        } else {
            100
        }

        val (currentStreak, _) = HabitCalculationEngine.calculateStreak(habit, logs)

        val reached = when (habit.challengeMode) {
            "STREAK" -> target > 0 && currentStreak >= target
            "SUCCESS_RATE" -> target > 0 && totalActiveDays >= target && rate >= habit.challengeSuccessRate
            else -> {
                if (habit.isNegative) target > 0 && successfulDays >= target
                else {
                    val achieved = HabitCalculationEngine.calculateTotalAchievedValue(habit, logs)
                    target > 0 && achieved >= (habit.totalTargetValue ?: 0f)
                }
            }
        }

        return ChallengeStats(
            cleanDays = successfulDays,
            failedDays = failedDays,
            totalActiveDays = totalActiveDays,
            currentRatePercent = rate,
            currentStreak = currentStreak,
            targetRatePercent = habit.challengeSuccessRate,
            targetDays = target,
            isGoalReached = reached
        )
    }

    fun isDaySuccessful(habit: Habit, log: HabitLog?): Boolean {
        if (habit.isNegative) {
            return isNegativeDayClean(habit, log)
        }
        if (log == null) return false
        if (log.isPaused) return false
        return when (log.value) {
            -1f -> false
            -2f -> true
            else -> {
                if (habit.type == "BINARY") {
                    log.value > 0f
                } else {
                    log.value >= habit.targetValue
                }
            }
        }
    }

    fun isNegativeDayClean(habit: Habit, log: HabitLog?): Boolean {
        if (log == null) return true // Unlogged day for negative habit means clean/abstinent
        if (log.isPaused) return false
        return when (log.value) {
            -1f -> false
            -2f -> true
            else -> {
                if (habit.type == "BINARY") {
                    log.value <= 0f
                } else {
                    log.value <= habit.targetValue
                }
            }
        }
    }
}
