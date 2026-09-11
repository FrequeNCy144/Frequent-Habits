package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.HabitWidgetProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun HabitsViewModel.addHabit(
    name: String,
    category: String,
    icon: String,
    color: String,
    isNegative: Boolean,
    type: String,
    unit: String,
    targetValue: Float,
    frequency: String,
    startDate: Long,
    specificDays: String = "",
    reminderEnabled: Boolean = false,
    reminderHour: Int = 18,
    reminderMinute: Int = 0,
    customReminders: String = "",
    description: String = "",
    clickIncrement: Float = 1.0f,
    milestoneRewards: List<com.example.data.MilestoneReward> = emptyList(),
    minimalViableValue: Float? = null,
    minimalViableText: String = "",
    why: String = "",
    stackedOnHabitId: Int? = null,
    isFinishable: Boolean = false,
    totalTargetValue: Float? = null,
    difficulty: String = "MEDIUM"
) {
    viewModelScope.launch {
        try {
            val habit = Habit(
                name = name,
                category = category,
                icon = icon,
                color = color,
                isNegative = isNegative,
                type = type,
                unit = unit,
                targetValue = targetValue,
                frequency = frequency,
                startDate = startDate,
                specificDays = specificDays,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                customReminders = customReminders,
                description = description,
                clickIncrement = clickIncrement,
                minimalViableValue = minimalViableValue,
                minimalViableText = minimalViableText,
                why = why,
                stackedOnHabitId = stackedOnHabitId,
                isFinishable = isFinishable,
                totalTargetValue = totalTargetValue,
                difficulty = difficulty
            )
            val insertedId = repository.insertHabit(habit).toInt()
            val finalHabit = habit.copy(id = insertedId)
            
            milestoneRewards.forEach { reward ->
                repository.insertMilestoneReward(reward.copy(habitId = insertedId))
            }
            
            try {
                com.example.NotificationHelper.scheduleAllHabitReminders(
                    getApplication(),
                    finalHabit
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                HabitWidgetProvider.triggerUpdate(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun HabitsViewModel.updateHabit(
    habit: Habit,
    milestoneRewards: List<com.example.data.MilestoneReward>? = null
) {
    viewModelScope.launch {
        try {
            val oldHabit = repository.getHabitByIdSuspend(habit.id)
            if (oldHabit != null) {
                com.example.NotificationHelper.cancelAllHabitReminders(getApplication(), oldHabit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            repository.updateHabit(habit)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (milestoneRewards != null) {
            try {
                val existing = repository.getAllMilestoneRewardsRaw().filter { it.habitId == habit.id }
                repository.deleteMilestoneRewardsForHabit(habit.id)
                milestoneRewards.forEach { reward ->
                    val matchingExisting = existing.find {
                        it.rewardText == reward.rewardText &&
                        it.conditionType == reward.conditionType &&
                        it.conditionValue == reward.conditionValue &&
                        it.trophyId == reward.trophyId
                    }
                    val finalReward = reward.copy(
                        id = 0,
                        habitId = habit.id,
                        isRedeemed = matchingExisting?.isRedeemed ?: reward.isRedeemed,
                        unlockedAt = matchingExisting?.unlockedAt ?: reward.unlockedAt
                    )
                    repository.insertMilestoneReward(finalReward)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            com.example.NotificationHelper.scheduleAllHabitReminders(
                getApplication(),
                habit
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            HabitWidgetProvider.triggerUpdate(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun HabitsViewModel.deleteHabit(habit: Habit) {
    viewModelScope.launch {
        repository.deleteHabit(habit)
        
        // Clean up notes with no active habits
        try {
            val remainingHabits = repository.allHabits.first()
            val allNotes = repository.allDailyNotes.first()
            allNotes.forEach { note ->
                val hasActiveHabit = remainingHabits.any { isHabitActiveOnDate(it, note.date) }
                if (!hasActiveHabit) {
                    repository.saveDailyNote(note.date, "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Adjust current selected date and week start if they became out-of-bounds
        adjustDateAndWeekIfOutOfRange()

        com.example.NotificationHelper.cancelAllHabitReminders(
            getApplication(),
            habit
        )
        if (selectedHabitIdForDetail.value == habit.id) {
            selectHabitForDetail(null)
        }
        // Trigger widget update
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.archiveHabit(habit: Habit) {
    viewModelScope.launch {
        val updated = habit.copy(isArchived = true)
        repository.updateHabit(updated)
        com.example.NotificationHelper.cancelAllHabitReminders(
            getApplication(),
            habit
        )
        // Trigger widget update
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.unarchiveHabit(habit: Habit) {
    viewModelScope.launch {
        val updated = habit.copy(isArchived = false)
        repository.updateHabit(updated)
        com.example.NotificationHelper.scheduleAllHabitReminders(
            getApplication(),
            updated
        )
        // Trigger widget update
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.moveHabitUp(habitId: Int) {
    viewModelScope.launch {
        val list = allHabits.value
        val index = list.indexOfFirst { it.id == habitId }
        if (index > 0) {
            val updatedHabits = list.mapIndexed { idx, h ->
                h.copy(sortOrder = idx)
            }.toMutableList()
            
            val temp = updatedHabits[index]
            updatedHabits[index] = updatedHabits[index - 1].copy(sortOrder = index)
            updatedHabits[index - 1] = temp.copy(sortOrder = index - 1)
            
            updatedHabits.forEach { h ->
                repository.updateHabit(h)
            }
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }
}

fun HabitsViewModel.moveHabitDown(habitId: Int) {
    viewModelScope.launch {
        val list = allHabits.value
        val index = list.indexOfFirst { it.id == habitId }
        if (index != -1 && index < list.size - 1) {
            val updatedHabits = list.mapIndexed { idx, h ->
                h.copy(sortOrder = idx)
            }.toMutableList()
            
            val temp = updatedHabits[index]
            updatedHabits[index] = updatedHabits[index + 1].copy(sortOrder = index)
            updatedHabits[index + 1] = temp.copy(sortOrder = index + 1)
            
            updatedHabits.forEach { h ->
                repository.updateHabit(h)
            }
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }
}

fun HabitsViewModel.saveNewHabitOrder(orderedHabitIds: List<Int>) {
    viewModelScope.launch {
        val allCurrentHabits = allHabits.value
        if (allCurrentHabits.isEmpty() || orderedHabitIds.isEmpty()) return@launch

        val orderedSet = orderedHabitIds.toSet()
        val habitsInNewOrder = orderedHabitIds.mapNotNull { id -> allCurrentHabits.find { it.id == id } }

        val newFullList = if (orderedHabitIds.size == allCurrentHabits.size) {
            habitsInNewOrder
        } else {
            val result = allCurrentHabits.toMutableList()
            val indicesInAll = allCurrentHabits.mapIndexedNotNull { idx, h -> if (h.id in orderedSet) idx else null }
            if (indicesInAll.size == habitsInNewOrder.size) {
                indicesInAll.forEachIndexed { slotIdx, targetIdx ->
                    result[targetIdx] = habitsInNewOrder[slotIdx]
                }
                result
            } else {
                val remaining = allCurrentHabits.filter { it.id !in orderedSet }
                habitsInNewOrder + remaining
            }
        }

        val orders = newFullList.mapIndexed { index, habit -> habit.id to index }
        repository.updateHabitSortOrders(orders)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.revertHabitOrders(savedOrders: Map<Int, Int>) {
    viewModelScope.launch {
        val orders = savedOrders.map { (id, order) -> id to order }
        repository.updateHabitSortOrders(orders)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

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
                val achieved = HabitCalculationEngine.calculateTotalAchievedValue(habit, currentLogs)
                if (!habit.isCompletedGoal && achieved >= totalTarget) {
                    completeFinishableGoal(habitId)
                } else if (habit.isCompletedGoal && achieved < totalTarget) {
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
