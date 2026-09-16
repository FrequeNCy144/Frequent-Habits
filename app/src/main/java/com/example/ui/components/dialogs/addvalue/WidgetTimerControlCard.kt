package com.example.ui.components.dialogs.addvalue

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.FocusTimerManager
import com.example.data.Habit
import com.example.ui.*
import com.example.ui.logNumericalHabit
import com.example.ui.theme.AppBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.ProgressTrack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.frequent.habits.R
import java.io.File

@Composable
fun WidgetTimerControlCard(
    context: Context,
    habit: Habit,
    selectedDate: String,
    currentValue: Float,
    selectedAudioFile: File?,
    isCurrentHabitInTimer: Boolean,
    globalIsRunning: Boolean,
    globalSecondsRemaining: Int,
    initialTimerSeconds: Int,
    timerSecondsRemaining: Int,
    timerDurationSeconds: Int,
    defaultMinutes: Int,
    manualInputValue: String,
    viewModel: HabitsViewModel,
    onResetTimerDuration: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ProgressTrack,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Reset / Restart Button
                IconButton(
                    onClick = {
                        if (isCurrentHabitInTimer) {
                            FocusTimerManager.stopTimer(context, logProgress = false)
                        }
                        val mins = manualInputValue.toIntOrNull() ?: defaultMinutes
                        onResetTimerDuration(mins * 60)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(ProgressTrack, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Play / Pause Button
                IconButton(
                    onClick = {
                        if (isCurrentHabitInTimer) {
                            if (globalIsRunning) {
                                FocusTimerManager.pauseTimer(context)
                            } else {
                                if (globalSecondsRemaining <= 0) {
                                    FocusTimerManager.startTimer(
                                        context = context,
                                        habitId = habit.id.toLong(),
                                        habitName = habit.name,
                                        habitColor = habit.color,
                                        durationMinutes = timerDurationSeconds / 60f,
                                        selectedDate = selectedDate,
                                        currentValue = currentValue,
                                        audioFile = selectedAudioFile,
                                        totalSecondsOverride = timerDurationSeconds
                                    )
                                } else {
                                    FocusTimerManager.resumeTimer(context)
                                }
                            }
                        } else {
                            FocusTimerManager.startTimer(
                                context = context,
                                habitId = habit.id.toLong(),
                                habitName = habit.name,
                                habitColor = habit.color,
                                durationMinutes = timerDurationSeconds / 60f,
                                selectedDate = selectedDate,
                                currentValue = currentValue,
                                audioFile = selectedAudioFile,
                                totalSecondsOverride = timerDurationSeconds
                            )
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(PrimaryViolet, CircleShape)
                ) {
                    if (isCurrentHabitInTimer && globalIsRunning) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pause),
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Done / Complete Button
                IconButton(
                    onClick = {
                        val loggedMins = if (isCurrentHabitInTimer && initialTimerSeconds > timerSecondsRemaining) {
                            ((initialTimerSeconds - timerSecondsRemaining + 59) / 60).coerceAtLeast(1)
                        } else {
                            (timerDurationSeconds / 60).coerceAtLeast(1)
                        }
                        if (isCurrentHabitInTimer) {
                            FocusTimerManager.stopTimer(context, logProgress = false)
                        }
                        viewModel.logNumericalHabit(habit.id, selectedDate, currentValue + loggedMins)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(ProgressTrack, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete Session",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
