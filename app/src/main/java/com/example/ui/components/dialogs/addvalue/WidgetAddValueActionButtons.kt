package com.example.ui.components.dialogs.addvalue

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FocusTimerManager
import com.example.data.Habit
import com.example.tr
import com.example.ui.*
import com.example.ui.logNumericalHabit
import com.example.ui.theme.PrimaryViolet

@Composable
fun WidgetAddValueActionButtons(
    context: Context,
    habit: Habit,
    selectedDate: String,
    manualInputValue: String,
    timerDurationSeconds: Int,
    isCurrentHabitInTimer: Boolean,
    viewModel: HabitsViewModel,
    language: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row with Reset & Failed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Reset Button
            OutlinedButton(
                onClick = {
                    if (isCurrentHabitInTimer) {
                        FocusTimerManager.stopTimer(context, logProgress = false)
                    }
                    viewModel.logNumericalHabit(habit.id, selectedDate, 0f)
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1B1C28)
                ),
                border = BorderStroke(1.dp, Color(0xFF4A3830))
            ) {
                Text(
                    text = tr(language, "Reset", "გადატვირთვა", "重置", "Reset"),
                    color = Color(0xFFFFA726),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Failed Button
            OutlinedButton(
                onClick = {
                    if (isCurrentHabitInTimer) {
                        FocusTimerManager.stopTimer(context, logProgress = false)
                    }
                    viewModel.logNumericalHabit(habit.id, selectedDate, -1f)
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1B1C28)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f))
            ) {
                Text(
                    text = tr(language, "Failed", "ჩაიშალა", "失败", "Failed"),
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Full-width Save Button
        Button(
            onClick = {
                val fValue = manualInputValue.replace(',', '.').toFloatOrNull() ?: (timerDurationSeconds / 60f)
                if (isCurrentHabitInTimer) {
                    FocusTimerManager.stopTimer(context, logProgress = false)
                }
                if (fValue <= 0f) {
                    viewModel.logNumericalHabit(habit.id, selectedDate, 0f)
                } else {
                    viewModel.logNumericalHabit(habit.id, selectedDate, fValue)
                }
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("widget_save_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
        ) {
            Text(
                text = tr(language, "Save", "შენახვა", "保存", "Save"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
        }
    }
}
