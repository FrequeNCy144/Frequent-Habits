package com.example.ui.screens.createhabit

import androidx.compose.runtime.Composable
import com.example.data.MilestoneReward
import com.example.ui.dialogs.*
import java.util.*

@Composable
fun CreateHabitDialogsOverlay(
    showStartDatePicker: Boolean,
    startDateMillis: Long,
    onStartDateSelected: (Long) -> Unit,
    onDismissStartDatePicker: () -> Unit,
    showEndDatePicker: Boolean,
    endDateMillis: Long?,
    onEndDateSelected: (Long) -> Unit,
    onDismissEndDatePicker: () -> Unit,
    showAddRewardDialog: Boolean,
    onAddReward: (MilestoneReward) -> Unit,
    onDismissRewardDialog: () -> Unit,
    activeExplanation: Pair<String, String>?,
    onDismissExplanation: () -> Unit,
    showAddCustomDialog: Boolean,
    reminderToEditTime: String?,
    onConfirmReminder: (Int, Int) -> Unit,
    onDismissReminderDialog: () -> Unit,
    language: String
) {
    if (showStartDatePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        SimpleDatePickerDialog(
            initialYear = cal.get(Calendar.YEAR),
            initialMonth = cal.get(Calendar.MONTH),
            initialDay = cal.get(Calendar.DAY_OF_MONTH),
            onDateSelected = { year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }
                onStartDateSelected(selected.timeInMillis)
            },
            onDismiss = onDismissStartDatePicker
        )
    }

    if (showEndDatePicker) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = endDateMillis ?: (startDateMillis + 30L * 86400000L)
        }
        SimpleDatePickerDialog(
            initialYear = cal.get(Calendar.YEAR),
            initialMonth = cal.get(Calendar.MONTH),
            initialDay = cal.get(Calendar.DAY_OF_MONTH),
            onDateSelected = { year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }
                onEndDateSelected(selected.timeInMillis)
            },
            onDismiss = onDismissEndDatePicker
        )
    }

    if (showAddRewardDialog) {
        AddMilestoneRewardDialog(
            language = language,
            onDismiss = onDismissRewardDialog,
            onAdd = onAddReward
        )
    }

    if (activeExplanation != null) {
        ExplanationDialog(
            title = activeExplanation.first,
            explanation = activeExplanation.second,
            onDismiss = onDismissExplanation
        )
    }

    if (showAddCustomDialog) {
        AddCustomReminderDialog(
            language = language,
            initialTime = reminderToEditTime,
            onDismiss = onDismissReminderDialog,
            onConfirm = onConfirmReminder
        )
    }
}
