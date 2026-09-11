package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.logNumericalHabit
import com.example.ui.theme.*

@Composable
fun WidgetManualInputSection(
    habit: Habit,
    habitColor: Color,
    currentValue: Float,
    selectedDate: String,
    language: String,
    viewModel: HabitsViewModel,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    onDismiss: () -> Unit
) {
    var inputVal by remember(habit.id, currentValue) {
        val target = habit.targetValue
        val displayVal = if (target <= 0f) 0f else target
        val formatted = if (displayVal <= 0f) "" else {
            if (displayVal % 1f == 0f) displayVal.toInt().toString() else displayVal.toString()
        }
        mutableStateOf(formatted)
    }

    val suggestions = remember(habit) {
        val target = habit.targetValue
        when {
            habit.unit.lowercase() in listOf("l", "liter", "liters") -> listOf(0.5f, 1.0f, 1.5f, 2.0f)
            habit.unit.lowercase() in listOf("ml") -> listOf(250f, 500f, 750f, 1000f)
            habit.unit.lowercase() in listOf("min", "minutes", "minuten") -> listOf(1f, 15f, 30f, 60f)
            habit.unit.lowercase() in listOf("h", "stunden", "hours") -> listOf(1f, 2f, 3f, 4f)
            else -> {
                if (target > 1f) {
                    val half = target / 2f
                    val halfSuggested = if (half % 1f == 0f) half else (Math.round(half * 10f) / 10f).toFloat()
                    listOfNotNull(
                        1f,
                        if (halfSuggested > 1f && halfSuggested != target) halfSuggested else null,
                        target,
                        target * 1.5f
                    ).distinct()
                } else {
                    listOf(1f, 2f, 3f, 5f)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBg, RoundedCornerShape(16.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = tr(language, "Manueller Eintrag", "ხელით შეყვანა", "手动输入", "Manual Entry"),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputVal,
                onValueChange = { inputVal = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("widget_value_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = habitColor,
                    unfocusedBorderColor = AppBorder,
                    focusedContainerColor = AppCard,
                    unfocusedContainerColor = AppCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    Text(
                        text = habit.unit,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )

            // Quick Log Button
            Button(
                onClick = {
                    val fValue = inputVal.toFloatOrNull() ?: 0f
                    if (fValue <= 0f) {
                        viewModel.logNumericalHabit(habit.id, selectedDate, -1f)
                    } else {
                        viewModel.logNumericalHabit(habit.id, selectedDate, fValue)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = habitColor),
                modifier = Modifier.testTag("widget_save_button")
            ) {
                Text(
                    text = tr(language, "Speichern", "შენახვა", "保存", "Save"),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Quick Value Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestions.forEach { suggestion ->
                val text = if (suggestion % 1f == 0f) "+${suggestion.toInt()}" else "+$suggestion"
                Surface(
                    onClick = {
                        val currentVal = inputVal.toFloatOrNull() ?: currentValue
                        val newVal = (currentVal + suggestion)
                        inputVal = if (newVal % 1f == 0f) newVal.toInt().toString() else (Math.round(newVal * 10f) / 10f).toString()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = AppCard,
                    border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        // Reset & Mark Failed actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    viewModel.logNumericalHabit(habit.id, selectedDate, 0f)
                    onDismiss()
                }
            ) {
                Text(
                    text = tr(language, "Auf 0 zurücksetzen", "0-ზე გადაყენება", "重置为0", "Reset to 0"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            TextButton(
                onClick = {
                    viewModel.logNumericalHabit(habit.id, selectedDate, -1f)
                    onDismiss()
                }
            ) {
                Text(
                    text = tr(language, "Als nicht geschafft markieren", "შეუსრულებლად მონიშვნა", "标记为未完成", "Mark as failed"),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed
                )
            }
        }
    }
}
