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
    difficulty: String = "MEDIUM",
    folderId: Int? = null,
    challengeMode: String = "ACCUMULATE",
    challengeSuccessRate: Int = 90
) {
    viewModelScope.launch {
        try {
            var finalFolderId = folderId
            if (stackedOnHabitId != null) {
                try {
                    val parent = repository.getHabitByIdSuspend(stackedOnHabitId)
                    if (parent != null) {
                        finalFolderId = parent.folderId
                    }
                } catch (e: Exception) {}
            }
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
                difficulty = difficulty,
                folderId = finalFolderId,
                challengeMode = challengeMode,
                challengeSuccessRate = challengeSuccessRate
            )
            val insertedId = repository.insertHabit(habit).toInt()
            val finalHabit = habit.copy(id = insertedId)
            
            try {
                cascadeFolderIdToStack(insertedId, finalFolderId)
            } catch (e: Exception) {}
            
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
        var finalHabit = habit
        try {
            if (habit.stackedOnHabitId != null) {
                val parent = repository.getHabitByIdSuspend(habit.stackedOnHabitId)
                if (parent != null && parent.folderId != habit.folderId) {
                    finalHabit = habit.copy(folderId = parent.folderId)
                }
            }
            repository.updateHabit(finalHabit)
            cascadeFolderIdToStack(finalHabit.id, finalHabit.folderId)
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

suspend fun HabitsViewModel.cascadeFolderIdToStack(targetHabitId: Int, folderId: Int?) {
    val currentHabits = allHabits.value
    if (currentHabits.isEmpty()) return

    val visitedIds = mutableSetOf<Int>()
    val stack = mutableListOf<Habit>()

    fun dfs(hId: Int) {
        if (hId in visitedIds) return
        visitedIds.add(hId)
        val h = currentHabits.find { it.id == hId } ?: return
        stack.add(h)

        // Find children (habits stacked on h)
        currentHabits.filter { it.stackedOnHabitId == h.id }.forEach { dfs(it.id) }
        // Find parent (h is stacked on another)
        h.stackedOnHabitId?.let { dfs(it) }
    }

    dfs(targetHabitId)

    for (stackHabit in stack) {
        if (stackHabit.folderId != folderId) {
            repository.updateHabit(stackHabit.copy(folderId = folderId))
        }
    }
}
