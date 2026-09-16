package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.HabitWidgetProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun HabitsViewModel.toggleBinaryHabit(habitId: Int, date: String, isCurrentlyCompleted: Boolean) {
    viewModelScope.launch {
        val habit = allHabits.value.find { it.id == habitId } ?: return@launch
        val existingLogs = repository.getLogsForHabitOnDate(habitId, date)
        val log = existingLogs.firstOrNull()

        val currentStatus = when {
            log == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
            log.isPaused -> "PAUSED"
            log.value == -1f -> "FAILED"
            log.value == -2f -> "SUCCESS"
            log.value == 0f -> if (habit.isNegative) "SUCCESS" else "PENDING"
            else -> {
                if (habit.type == "BINARY") {
                    if (habit.isNegative) "FAILED" else "SUCCESS"
                } else {
                    val effectiveTarget = if (log.isMinimalViable && habit.minimalViableValue != null) habit.minimalViableValue else habit.targetValue
                    if (habit.isNegative) {
                        if (log.value >= effectiveTarget) "FAILED" else "PENDING"
                    } else {
                        if (log.value >= effectiveTarget) "SUCCESS" else "PENDING"
                    }
                }
            }
        }

        val nextStatus = if (habit.isNegative) {
            if (currentStatus == "SUCCESS") "FAILED" else "SUCCESS"
        } else {
            when (currentStatus) {
                "PENDING" -> "SUCCESS"
                "SUCCESS" -> "FAILED"
                else -> "PENDING"
            }
        }

        if (nextStatus == "SUCCESS") {
            recordHabitCompleted(habitId)
        }

        if (habit.isNegative) {
            if (nextStatus == "SUCCESS") {
                repository.unlogHabit(habitId, date)
            } else {
                repository.logHabit(habitId, date, -1f)
            }
        } else {
            if (nextStatus == "PENDING") {
                repository.unlogHabit(habitId, date)
            } else {
                val effectiveTarget = if (log?.isMinimalViable == true && habit.minimalViableValue != null) habit.minimalViableValue else habit.targetValue
                val nextValue = if (nextStatus == "SUCCESS") {
                    if (habit.type == "BINARY") -2f else effectiveTarget
                } else {
                    -1f
                }
                repository.logHabit(habitId, date, nextValue)
                checkAutoFinishableGoal(habitId)
            }
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.logNumericalHabit(habitId: Int, date: String, value: Float) {
    viewModelScope.launch {
        val habit = allHabits.value.find { it.id == habitId }
        val target = habit?.targetValue ?: 1f
        if (habit != null && !habit.isNegative && value >= target) {
            recordHabitCompleted(habitId)
        }
        if (value == 0f) {
            repository.unlogHabit(habitId, date)
        } else {
            repository.logHabit(habitId, date, value)
            checkAutoFinishableGoal(habitId)
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.checkAutoFinishableGoal(habitId: Int) {
    viewModelScope.launch {
        val habit = allHabits.value.find { it.id == habitId } ?: return@launch
        if (habit.isFinishable) {
            val totalTarget = habit.totalTargetValue ?: 0f
            if (totalTarget > 0f) {
                val currentLogs = allLogs.value
                val isReached = HabitCalculationEngine.isGoalTargetReached(habit, currentLogs)
                if (!habit.isCompletedGoal && isReached) {
                    completeFinishableGoal(habitId)
                } else if (habit.isCompletedGoal && !isReached) {
                    val updated = habit.copy(
                        isCompletedGoal = false,
                        completedAt = null
                    )
                    repository.updateHabit(updated)
                    HabitWidgetProvider.triggerUpdate(getApplication())
                }
            }
        }
    }
}

fun HabitsViewModel.togglePauseHabit(habitId: Int) {
    viewModelScope.launch {
        val date = selectedDate.value
        repository.togglePauseHabit(habitId, date)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.toggleMinimalViableHabit(habitId: Int) {
    viewModelScope.launch {
        val date = selectedDate.value
        repository.toggleMinimalViableHabit(habitId, date)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.toggleMinimalViableAllHabitsForSelectedDate() {
    viewModelScope.launch {
        val date = selectedDate.value
        val activeHabits = activeHabitUiItemsForSelectedDate.value
        val eligibleHabits = activeHabits.filter {
            (it.habit.minimalViableValue != null && it.habit.minimalViableValue > 0f) || it.habit.minimalViableText.isNotBlank()
        }
        if (eligibleHabits.isEmpty()) return@launch
        
        val allMinimalViable = eligibleHabits.all { it.isMinimalViable }
        val targetMVState = !allMinimalViable
        
        eligibleHabits.forEach { item ->
            repository.setMinimalViableStateForHabit(item.habit.id, date, targetMVState)
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.togglePauseAllHabitsForSelectedDate() {
    viewModelScope.launch {
        val date = selectedDate.value
        val activeHabits = activeHabitUiItemsForSelectedDate.value
        if (activeHabits.isEmpty()) return@launch
        
        val allPaused = activeHabits.all { it.isPaused }
        val targetPauseState = !allPaused
        
        activeHabits.forEach { item ->
            repository.setPauseStateForHabit(item.habit.id, date, targetPauseState)
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.toggleFolderCompletion(folderId: Int) {
    viewModelScope.launch {
        val date = selectedDate.value
        val activeHabitsInFolder = activeHabitUiItemsForSelectedDate.value.filter { it.habit.folderId == folderId && !it.isPaused }
        if (activeHabitsInFolder.isEmpty()) return@launch

        val allCompleted = activeHabitsInFolder.all { it.isCompleted }
        val allFailed = activeHabitsInFolder.all { it.isFailed }

        if (allCompleted) {
            // Stage 2: Mark all as failed
            activeHabitsInFolder.forEach { item ->
                repository.logHabit(item.habit.id, date, -1f)
            }
        } else if (allFailed) {
            // Stage 3: Reset all to pending/clean
            activeHabitsInFolder.forEach { item ->
                repository.unlogHabit(item.habit.id, date)
            }
        } else {
            // Stage 1: Mark all as completed
            activeHabitsInFolder.forEach { item ->
                val habit = item.habit
                if (habit.isNegative) {
                    repository.unlogHabit(habit.id, date)
                } else {
                    val effectiveTarget = if (item.isMinimalViable && habit.minimalViableValue != null) habit.minimalViableValue else habit.targetValue
                    val valueToLog = if (habit.type == "BINARY") -2f else effectiveTarget
                    repository.logHabit(habit.id, date, valueToLog)
                    recordHabitCompleted(habit.id)
                    checkAutoFinishableGoal(habit.id)
                }
            }
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.markFolderAllCompleted(folderId: Int) {
    viewModelScope.launch {
        val date = selectedDate.value
        val activeHabitsInFolder = activeHabitUiItemsForSelectedDate.value.filter { it.habit.folderId == folderId && !it.isPaused }
        if (activeHabitsInFolder.isEmpty()) return@launch

        activeHabitsInFolder.forEach { item ->
            val habit = item.habit
            if (habit.isNegative) {
                repository.unlogHabit(habit.id, date)
            } else {
                val effectiveTarget = if (item.isMinimalViable && habit.minimalViableValue != null) habit.minimalViableValue else habit.targetValue
                val valueToLog = if (habit.type == "BINARY") -2f else effectiveTarget
                repository.logHabit(habit.id, date, valueToLog)
                recordHabitCompleted(habit.id)
                checkAutoFinishableGoal(habit.id)
            }
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.markFolderAllFailed(folderId: Int) {
    viewModelScope.launch {
        val date = selectedDate.value
        val activeHabitsInFolder = activeHabitUiItemsForSelectedDate.value.filter { it.habit.folderId == folderId && !it.isPaused }
        if (activeHabitsInFolder.isEmpty()) return@launch

        activeHabitsInFolder.forEach { item ->
            val habit = item.habit
            repository.logHabit(habit.id, date, -1f)
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.resetFolderLogs(folderId: Int) {
    viewModelScope.launch {
        val date = selectedDate.value
        val activeHabitsInFolder = activeHabitUiItemsForSelectedDate.value.filter { it.habit.folderId == folderId }
        if (activeHabitsInFolder.isEmpty()) return@launch

        activeHabitsInFolder.forEach { item ->
            repository.unlogHabit(item.habit.id, date)
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}