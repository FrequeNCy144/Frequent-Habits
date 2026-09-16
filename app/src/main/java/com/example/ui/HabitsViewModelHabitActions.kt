package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.HabitWidgetProvider
import com.example.data.AchievementEvaluator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

fun HabitsViewModel.dismissGoalDialog(habitId: Int) {
    val newSet = dismissedGoalIds.value + habitId
    _dismissedGoalIds.value = newSet
    sharedPrefs.edit().putStringSet("dismissed_goal_dialog_ids", newSet.map { it.toString() }.toSet()).apply()
}

fun HabitsViewModel.unDismissGoalDialog(habitId: Int) {
    val newSet = dismissedGoalIds.value - habitId
    _dismissedGoalIds.value = newSet
    sharedPrefs.edit().putStringSet("dismissed_goal_dialog_ids", newSet.map { it.toString() }.toSet()).apply()
}

fun HabitsViewModel.dismissSmartInsight(dateStr: String) {
    _smartInsightDismissedDate.value = dateStr
    sharedPrefs.edit().putString("smart_insight_dismissed_date", dateStr).apply()
}

fun HabitsViewModel.updateUserName(name: String) {
    val trimmed = name.trim()
    _userName.value = trimmed
    sharedPrefs.edit().putString("user_name", trimmed).apply()
}

fun HabitsViewModel.updateProfileImageUri(uri: String) {
    val avatarFile = File(getApplication<android.app.Application>().filesDir, "profile_avatar.jpg")
    if (uri.isEmpty()) {
        if (avatarFile.exists()) {
            try { avatarFile.delete() } catch (e: Exception) { e.printStackTrace() }
        }
        _profileImageUri.value = ""
        sharedPrefs.edit().putString("profile_image_uri", "").apply()
    } else {
        _profileImageUri.value = uri
        sharedPrefs.edit().putString("profile_image_uri", uri).apply()
    }
}

fun HabitsViewModel.setPendingWidgetHabitId(id: Int) {
    _pendingWidgetHabitId.value = id
}

fun HabitsViewModel.clearPendingWidgetHabitId() {
    _pendingWidgetHabitId.value = null
}

fun HabitsViewModel.recordHabitCompleted(habitId: Int) {
    _justCompletedHabitEvent.value = Pair(habitId, System.currentTimeMillis())
}

fun HabitsViewModel.completeFinishableGoal(habitId: Int, reflectionNote: String = "", claimReward: Boolean = false) {
    viewModelScope.launch {
        val habit = allHabits.value.find { it.id == habitId } ?: return@launch
        val updated = habit.copy(
            isCompletedGoal = true,
            completedAt = System.currentTimeMillis(),
            completionNote = reflectionNote.trim()
        )
        repository.updateHabit(updated)
        val rewards = repository.getAllMilestoneRewardsRaw().filter { it.habitId == habitId }
        rewards.forEach { reward ->
            val updatedReward = reward.copy(
                unlockedAt = if (reward.unlockedAt > 0L) reward.unlockedAt else System.currentTimeMillis(),
                isRedeemed = if (claimReward) true else reward.isRedeemed
            )
            repository.updateMilestoneReward(updatedReward)
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.restartFinishableGoal(habitId: Int, newTotalTarget: Float? = null) {
    viewModelScope.launch {
        unDismissGoalDialog(habitId)
        val habit = allHabits.value.find { it.id == habitId } ?: return@launch
        val updated = habit.copy(
            isCompletedGoal = false,
            completedAt = null,
            totalTargetValue = newTotalTarget ?: habit.totalTargetValue
        )
        repository.updateHabit(updated)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.initAchievementObserver() {
    viewModelScope.launch {
        combine(profileStats, language, allMilestoneRewards) { stats, lang, rewards ->
            Triple(stats, lang, rewards)
        }.collect { (stats, lang, rewards) ->
            rewards.forEach { reward ->
                if (reward.unlockedAt == 0L) {
                    val habitStat = stats.habitStreaks.find { it.habit.id == reward.habitId }
                    val isReached = when (reward.conditionType) {
                        "STREAK" -> (habitStat?.longestStreak ?: 0) >= reward.conditionValue
                        "COMPLETIONS" -> (habitStat?.totalCompletions ?: 0) >= reward.conditionValue
                        "TROPHY_COUPLED" -> {
                            val streak = habitStat?.longestStreak ?: 0
                            when (reward.trophyId) {
                                "WOOD" -> streak >= 7
                                "BRONZE" -> streak >= 14
                                "SILVER" -> streak >= 30
                                "GOLD" -> streak >= 100
                                "COMP_10" -> stats.totalGlobalCompletions >= 10
                                "COMP_50" -> stats.totalGlobalCompletions >= 50
                                "COMP_200" -> stats.totalGlobalCompletions >= 200
                                "COMP_500" -> stats.totalGlobalCompletions >= 500
                                "PERF_7" -> stats.perfectDaysStreak >= 7
                                "PERF_30" -> stats.perfectDaysStreak >= 30
                                "PERF_100" -> stats.perfectDaysStreak >= 100
                                else -> false
                            }
                        }
                        else -> false
                    }
                    if (isReached) {
                        viewModelScope.launch {
                            repository.updateMilestoneReward(reward.copy(unlockedAt = System.currentTimeMillis()))
                        }
                    }
                }
            }
            val currentUnlocked = AchievementEvaluator.calculateUnlockedAchievementsList(stats, lang, rewards)
            val currentIds = currentUnlocked.map { it.id }.toSet()
            if (knownUnlockedAchievementIds == null) {
                val savedIds = sharedPrefs.getStringSet("known_unlocked_achievement_ids", null)
                if (savedIds != null) {
                    knownUnlockedAchievementIds = savedIds.toMutableSet()
                } else {
                    knownUnlockedAchievementIds = currentIds.toMutableSet()
                    sharedPrefs.edit().putStringSet("known_unlocked_achievement_ids", currentIds).apply()
                }
            }
            val newlyUnlocked = currentUnlocked.filter { it.id !in knownUnlockedAchievementIds!! }
            if (newlyUnlocked.isNotEmpty()) {
                newlyUnlocked.forEach { ach ->
                    knownUnlockedAchievementIds!!.add(ach.id)
                }
                sharedPrefs.edit().putStringSet("known_unlocked_achievement_ids", knownUnlockedAchievementIds).apply()
                val updatedQueue = achievementQueue.value + newlyUnlocked
                achievementQueue.value = updatedQueue
                if (_newlyUnlockedAchievement.value == null) {
                    _newlyUnlockedAchievement.value = if (updatedQueue.isNotEmpty()) updatedQueue[0] else null
                }
            }
        }
    }
}

fun HabitsViewModel.dismissUnlockedAchievement() {
    val currentQueue = achievementQueue.value
    if (currentQueue.isNotEmpty()) {
        val nextQueue = currentQueue.drop(1)
        achievementQueue.value = nextQueue
        _newlyUnlockedAchievement.value = if (nextQueue.isNotEmpty()) nextQueue[0] else null
    } else {
        _newlyUnlockedAchievement.value = null
    }
}

fun HabitsViewModel.redeemMilestoneReward(rewardId: Int) {
    toggleRedeemMilestoneReward(rewardId, true)
}

fun HabitsViewModel.toggleRedeemMilestoneReward(rewardId: Int, isRedeemed: Boolean) {
    viewModelScope.launch {
        try {
            val rewards = repository.getAllMilestoneRewardsRaw()
            val reward = rewards.find { it.id == rewardId }
            if (reward != null) {
                repository.updateMilestoneReward(reward.copy(isRedeemed = isRedeemed))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
