package com.example.ui.components

import androidx.compose.runtime.*
import com.example.data.UnlockedAchievementInfo
import com.example.ui.HabitsViewModel
import com.example.ui.screens.profile.AchievementUnlockedOverlay
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.profile.FullScreensCelebrationConfetti

@Composable
fun MainRootOverlays(
    pendingWidgetHabitId: Int?,
    newlyUnlockedAchievement: UnlockedAchievementInfo?,
    hasOnboarded: Boolean,
    viewModel: HabitsViewModel,
    language: String,
    onSetShowCreateFirstHabitHint: (Boolean) -> Unit
) {
    if (pendingWidgetHabitId != null) {
        WidgetAddValueDialog(
            habitId = pendingWidgetHabitId,
            viewModel = viewModel,
            language = language,
            onDismiss = { viewModel.clearPendingWidgetHabitId() }
        )
    }

    if (newlyUnlockedAchievement != null) {
        AchievementUnlockedOverlay(
            achievement = newlyUnlockedAchievement,
            language = language,
            onDismiss = { viewModel.dismissUnlockedAchievement() },
            onClaimReward = { rewardId ->
                if (rewardId != null) {
                    viewModel.redeemMilestoneReward(rewardId)
                }
                viewModel.dismissUnlockedAchievement()
            }
        )
    }

    var showOnboardingCelebration by remember { mutableStateOf(false) }

    if (showOnboardingCelebration) {
        FullScreensCelebrationConfetti(
            onFinished = { showOnboardingCelebration = false }
        )
    }

    if (!hasOnboarded) {
        OnboardingScreen(
            viewModel = viewModel,
            language = language,
            onFinish = {
                viewModel.setOnboarded(true)
                showOnboardingCelebration = true
                onSetShowCreateFirstHabitHint(true)
            }
        )
    }
}
