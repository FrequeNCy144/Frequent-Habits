package com.example.ui.screens.profile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.tr
import com.example.ui.dialogs.EditProfileDialog
import com.example.ui.dialogs.ExplanationDialog
import com.example.ui.share.ProfileShareDialog
import com.example.ui.theme.FailedRed
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileDialogsOverlay(
    showEditNameDialog: Boolean,
    userName: String,
    profileImageUri: String?,
    language: String,
    onSaveName: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onDismissEditNameDialog: () -> Unit,
    activeExplanation: Pair<String, String>?,
    onDismissExplanation: () -> Unit,
    showProfileShareDialog: Boolean,
    onDismissProfileShareDialog: () -> Unit,
    habits: List<Habit>,
    allLogs: List<HabitLog>,
    habitToDelete: Habit?,
    onConfirmDeleteHabit: (Habit) -> Unit,
    onDismissDeleteHabit: () -> Unit
) {
    if (showEditNameDialog) {
        EditProfileDialog(
            userName = userName,
            profileImageUri = profileImageUri ?: "",
            language = language,
            onSaveName = onSaveName,
            onChangePhoto = onChangePhoto,
            onDismiss = onDismissEditNameDialog
        )
    }

    if (activeExplanation != null) {
        ExplanationDialog(
            title = activeExplanation.first,
            explanation = activeExplanation.second,
            onDismiss = onDismissExplanation
        )
    }

    if (showProfileShareDialog) {
        ProfileShareDialog(
            userName = userName,
            profileImageUri = profileImageUri ?: "",
            habits = habits,
            logs = allLogs,
            language = language,
            onDismiss = onDismissProfileShareDialog
        )
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = onDismissDeleteHabit,
            title = {
                Text(
                    text = tr(language, "Ziel löschen?", "მიზნის წაშლა?", "删除目标？", "Delete Goal?"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        language,
                        "Möchtest du „${habit.name}“ wirklich unwiderruflich aus deinen gemeisterten Zielen löschen?",
                        "ნამდვილად გსურთ წაშალოთ „${habit.name}“ მიღწეული მიზნებიდან?",
                        "确定要从已掌握的目标中永久删除“${habit.name}”吗？",
                        "Do you really want to permanently delete \"${habit.name}\" from your mastered goals?"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteHabit(habit) }
                ) {
                    Text(
                        text = tr(language, "Löschen", "წაშლა", "删除", "Delete"),
                        color = FailedRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteHabit) {
                    Text(
                        text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
