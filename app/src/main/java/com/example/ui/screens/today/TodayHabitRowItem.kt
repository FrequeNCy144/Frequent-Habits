package com.example.ui.screens.today

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.data.HabitUiItem
import com.example.ui.components.HabitItemRow

@Composable
fun TodayHabitRowItem(
    uiItem: HabitUiItem,
    displayedHabits: List<HabitUiItem>,
    justCompletedEvent: Pair<Int, Long>?,
    isNested: Boolean,
    isReorderMode: Boolean,
    isBeingDragged: Boolean,
    dragTranslationY: Float,
    listState: LazyListState,
    selectedDate: String,
    language: String,
    vibrationEnabled: Boolean,
    onToggle: (habitId: Int, hasLog: Boolean) -> Unit,
    onManualAddValue: (habitId: Int) -> Unit,
    onAddQuantity: (habitId: Int, currentVal: Float, diff: Float) -> Unit,
    onLongClick: (Habit) -> Unit,
    onCompleteGoal: (Habit) -> Unit,
    onDragStart: (() -> Unit)? = null,
    onDragDelta: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentHabit = uiItem.habit
    val isNumerical = currentHabit.type == "NUMBER" || currentHabit.type == "NUMERICAL" || (currentHabit.targetValue > 1f && currentHabit.type != "BINARY")

    val onToggleClick = remember(isNumerical, currentHabit.id, onManualAddValue, onToggle) {
        { habitId: Int, hasLog: Boolean ->
            if (isNumerical) {
                onManualAddValue(habitId)
            } else {
                onToggle(habitId, hasLog)
            }
        }
    }

    val anchorHabit = currentHabit.stackedOnHabitId?.let { anchorId ->
        displayedHabits.find { it.habit.id == anchorId }
    }
    val anchorHabitModel = anchorHabit?.habit
    val anchorName = anchorHabitModel?.name
    val anchorCompleted = anchorHabit?.isCompleted == true
    val isAnchorJustCompleted = anchorCompleted &&
        currentHabit.stackedOnHabitId != null &&
        justCompletedEvent != null &&
        justCompletedEvent.first == currentHabit.stackedOnHabitId &&
        (System.currentTimeMillis() - justCompletedEvent.second) < 4000L

    val itemContent = @Composable {
        HabitItemRow(
            modifier = Modifier,
            habit = currentHabit,
            currentValue = uiItem.currentValue,
            isCompleted = uiItem.isCompleted,
            isFailed = uiItem.isFailed,
            isPaused = uiItem.isPaused,
            hasLog = uiItem.hasLog,
            isMinimalViable = uiItem.isMinimalViable,
            onToggle = onToggleClick,
            onAddQuantity = onAddQuantity,
            onLongClick = onLongClick,
            language = language,
            vibrationEnabled = vibrationEnabled,
            selectedDate = selectedDate,
            isWeeklyTargetReached = uiItem.isWeeklyTargetReached,
            weeklyLoggedCount = uiItem.weeklyLoggedCount,
            weeklyTargetCount = uiItem.weeklyTargetCount,
            streak = uiItem.streak,
            isReorderMode = isReorderMode,
            anchorName = anchorName,
            anchorCompleted = anchorCompleted,
            anchorJustCompleted = isAnchorJustCompleted,
            onMoveUp = if (onMoveUp != null) { { onMoveUp() } } else null,
            onMoveDown = if (onMoveDown != null) { { onMoveDown() } } else null,
            listState = listState,
            isBeingDragged = isBeingDragged,
            dragTranslationY = dragTranslationY,
            onDragStart = onDragStart,
            onDragDelta = onDragDelta,
            onDragEnd = onDragEnd,
            totalAchievedValue = uiItem.totalAchievedValue,
            isGoalTargetReached = uiItem.isGoalTargetReached,
            onCompleteGoal = onCompleteGoal,
            isMonthlyTargetReached = uiItem.isMonthlyTargetReached,
            monthlyLoggedCount = uiItem.monthlyLoggedCount,
            monthlyTargetCount = uiItem.monthlyTargetCount,
            isWeeklyNumeric = uiItem.isWeeklyNumeric,
            weeklySum = uiItem.weeklySum,
            isMonthlyNumeric = uiItem.isMonthlyNumeric,
            monthlySum = uiItem.monthlySum
        )
    }

    if (isNested) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 24.dp)
        ) {
            itemContent()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            itemContent()
        }
    }
}
