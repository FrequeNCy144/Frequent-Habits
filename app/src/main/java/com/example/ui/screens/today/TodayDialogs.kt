package com.example.ui.screens.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.data.HabitUiItem
import com.example.data.MilestoneReward
import com.example.ui.*
import com.example.ui.components.WidgetAddValueDialog
import com.example.ui.dialogs.DailyNoteDialog
import com.example.ui.dialogs.MasterGoalDialog

@Composable
fun TodayDialogs(
    viewModel: HabitsViewModel,
    language: String,
    selectedDate: String,
    currentNote: String,
    accentColor: Color,
    activeHabitUiItemsForSelectedDate: List<HabitUiItem>,
    allMilestoneRewards: List<MilestoneReward>,
    allArePaused: Boolean,
    showDailyNoteDialog: Boolean,
    onDismissDailyNoteDialog: () -> Unit,
    showFolderDialog: Boolean,
    editingFolder: HabitFolder?,
    onDismissFolderDialog: () -> Unit,
    onRequestCreateFolder: (() -> Unit)? = null,
    showDayOverviewSheet: Boolean,
    onDismissDayOverviewSheet: () -> Unit,
    onStartReorder: () -> Unit,
    longPressedHabit: Habit?,
    onDismissLongPressAction: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    showDeleteConfirmHabit: Habit?,
    onDismissDeleteConfirm: () -> Unit,
    showArchiveConfirmHabit: Habit?,
    onDismissArchiveConfirm: () -> Unit,
    manualAddValueHabitId: Int?,
    onDismissManualAddValue: () -> Unit,
    showSaskiaDialog: Boolean,
    onDismissSaskiaDialog: () -> Unit,
    goalToMaster: Pair<Habit, Float>?,
    onDismissGoalDialog: (Int?) -> Unit,
    onRequestArchiveHabit: (Habit) -> Unit,
    onRequestDeleteHabit: (Habit) -> Unit
) {
    val allFolders by viewModel.allFolders.collectAsState()
    if (showDayOverviewSheet) {
        val eligibleMvHabits = activeHabitUiItemsForSelectedDate.filter { 
            (it.habit.minimalViableValue != null && it.habit.minimalViableValue > 0f) || it.habit.minimalViableText.isNotBlank()
        }
        val hasMinimalViableHabits = eligibleMvHabits.isNotEmpty()
        val allAreMinimalViable = hasMinimalViableHabits && eligibleMvHabits.all { it.isMinimalViable }

        DayOverviewBottomSheet(
            language = language,
            selectedDate = selectedDate,
            allArePaused = allArePaused,
            hasMinimalViableHabits = hasMinimalViableHabits,
            allAreMinimalViable = allAreMinimalViable,
            onTogglePauseAll = {
                viewModel.togglePauseAllHabitsForSelectedDate()
                onDismissDayOverviewSheet()
            },
            onToggleMinimalViableDay = {
                viewModel.toggleMinimalViableAllHabitsForSelectedDate()
                onDismissDayOverviewSheet()
            },
            onStartReorder = {
                onDismissDayOverviewSheet()
                onStartReorder()
            },
            onDismiss = onDismissDayOverviewSheet
        )
    }

    if (longPressedHabit != null) {
        val habit = longPressedHabit
        val currentUiItem = activeHabitUiItemsForSelectedDate.find { it.habit.id == habit.id }
        val isPausedOnSelectedDate = currentUiItem?.isPaused == true
        val isMinimalViableOnSelectedDate = currentUiItem?.isMinimalViable == true

        HabitActionBottomSheet(
            habit = habit,
            language = language,
            isPausedOnSelectedDate = isPausedOnSelectedDate,
            isMinimalViableOnSelectedDate = isMinimalViableOnSelectedDate,
            onTogglePause = {
                viewModel.togglePauseHabit(habit.id)
                onDismissLongPressAction()
            },
            onToggleMinimalViable = {
                viewModel.toggleMinimalViableHabit(habit.id)
                onDismissLongPressAction()
            },
            onEditHabit = {
                onDismissLongPressAction()
                onEditHabit(habit)
            },
            onArchiveHabit = {
                onDismissLongPressAction()
                onRequestArchiveHabit(habit)
            },
            onDeleteHabit = {
                onDismissLongPressAction()
                onRequestDeleteHabit(habit)
            },
            onStartReorder = onStartReorder,
            allFolders = allFolders,
            onMoveToFolder = { newFolderId ->
                viewModel.updateHabit(habit.copy(folderId = newFolderId))
            },
            onCreateFolderAndMove = onRequestCreateFolder,
            onDismiss = onDismissLongPressAction
        )
    }

    showDeleteConfirmHabit?.let { habitToDelete ->
        TodayDeleteConfirmDialog(
            habit = habitToDelete,
            viewModel = viewModel,
            language = language,
            onDismiss = onDismissDeleteConfirm
        )
    }

    showArchiveConfirmHabit?.let { habitToArchive ->
        TodayArchiveConfirmDialog(
            habit = habitToArchive,
            viewModel = viewModel,
            language = language,
            onDismiss = onDismissArchiveConfirm
        )
    }

    if (manualAddValueHabitId != null) {
        WidgetAddValueDialog(
            habitId = manualAddValueHabitId,
            viewModel = viewModel,
            language = language,
            onDismiss = onDismissManualAddValue
        )
    }

    if (showSaskiaDialog) {
        TodaySaskiaUnlockDialog(
            language = language,
            onDismiss = onDismissSaskiaDialog
        )
    }

    if (showDailyNoteDialog) {
        DailyNoteDialog(
            currentNote = currentNote,
            language = language,
            onDismiss = onDismissDailyNoteDialog,
            onSave = { noteText ->
                viewModel.saveDailyNote(selectedDate, noteText)
                onDismissDailyNoteDialog()
            }
        )
    }

    goalToMaster?.let { (habit, totalAchieved) ->
        val matchingReward = allMilestoneRewards.find { it.habitId == habit.id }
        MasterGoalDialog(
            habit = habit,
            totalAchievedValue = totalAchieved,
            rewardTitle = matchingReward?.rewardText,
            language = language,
            onDismiss = {
                onDismissGoalDialog(goalToMaster.first.id)
            },
            onConfirmMastered = { note, claimReward ->
                val hId = habit.id
                viewModel.completeFinishableGoal(hId, note, claimReward = claimReward)
                onDismissGoalDialog(hId)
            }
        )
    }

    if (showFolderDialog) {
        TodayFolderDialog(
            folder = editingFolder,
            language = language,
            accentColor = accentColor,
            onDismiss = onDismissFolderDialog,
            onSave = { name, icon, color ->
                if (editingFolder == null) {
                    viewModel.addFolder(name, icon, color)
                } else {
                    viewModel.updateFolder(
                        editingFolder.copy(
                            name = name,
                            icon = icon,
                            color = color
                        )
                    )
                }
                onDismissFolderDialog()
            },
            onDelete = if (editingFolder != null) {
                {
                    viewModel.deleteFolder(editingFolder)
                    onDismissFolderDialog()
                }
            } else null
        )
    }
}
