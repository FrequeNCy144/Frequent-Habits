package com.example.ui.components.dialogs.addvalue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.theme.AppBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.ProgressTrack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WidgetAddValueManualSection(
    habit: Habit,
    language: String,
    manualInputValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val locUnit = com.example.getLocalizedUnit(habit.unit, language)
        Text(
            text = tr(
                language,
                "Manuell eintragen ($locUnit):",
                "ხელით შეყვანა ($locUnit):",
                "手动记录 ($locUnit):",
                "Manual log ($locUnit):"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input box
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(46.dp)
                    .background(ProgressTrack, RoundedCornerShape(12.dp))
                    .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                var isManualFocused by remember { mutableStateOf(false) }
                BasicTextField(
                    value = manualInputValue,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                            onValueChange(input)
                        }
                    },
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
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(PrimaryViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_value_input")
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && !isManualFocused) {
                                isManualFocused = true
                                onValueChange("")
                            } else if (!focusState.isFocused) {
                                isManualFocused = false
                            }
                        }
                )
            }

            // Presets
            val presets = if (habit.clickIncrement > 0f && habit.clickIncrement != 1f) {
                val inc = habit.clickIncrement.toInt()
                listOf(1, inc, inc * 2, inc * 5)
            } else {
                listOf(1, 5, 10, 15)
            }
            presets.forEach { preset ->
                Surface(
                    onClick = {
                        onValueChange(preset.toString())
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = ProgressTrack,
                    border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
