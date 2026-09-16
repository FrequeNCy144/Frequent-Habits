package com.example.ui.components.dialogs.addvalue

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.theme.PrimaryViolet
import kotlin.math.atan2

fun formatDurationHMS(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

fun parseDurationHMS(input: String): Int? {
    val cleaned = input.trim()
    val parts = cleaned.split(":").map { it.trim() }
    return try {
        when (parts.size) {
            1 -> {
                val num = parts[0].toIntOrNull() ?: return null
                num * 60
            }
            2 -> {
                val m = parts[0].toIntOrNull() ?: return null
                val s = parts[1].toIntOrNull() ?: return null
                m * 60 + s
            }
            3 -> {
                val h = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                val s = parts[2].toIntOrNull() ?: return null
                h * 3600 + m * 60 + s
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CircularTimerGauge(
    secondsRemaining: Int,
    initialSeconds: Int,
    isTimerRunning: Boolean,
    hasActiveSession: Boolean = false,
    language: String = "de",
    onSecondsSelected: (Int) -> Unit
) {
    val totalTargetSeconds = if (initialSeconds > 0) initialSeconds else 900
    val sweepFraction = if (hasActiveSession || isTimerRunning) {
        if (totalTargetSeconds > 0) (secondsRemaining.toFloat() / totalTargetSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    } else {
        1.0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "timer_wabend_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isInputFocused by remember { mutableStateOf(false) }

    var textValue by remember {
        val formatted = formatDurationHMS(secondsRemaining)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    LaunchedEffect(secondsRemaining, isTimerRunning, hasActiveSession) {
        if (!isInputFocused) {
            val formatted = formatDurationHMS(secondsRemaining)
            textValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }
    }

    Box(
        modifier = Modifier
            .size(220.dp)
            .pointerInput(isTimerRunning, hasActiveSession) {
                if (!isTimerRunning && !hasActiveSession) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touch = change.position
                        val dx = touch.x - center.x
                        val dy = touch.y - center.y
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        angleDeg = (angleDeg + 90f + 360f) % 360f
                        val selectedMinutes = ((angleDeg / 360f) * 60f).toInt().coerceIn(1, 180)
                        val newSecs = selectedMinutes * 60
                        textValue = TextFieldValue(text = formatDurationHMS(newSecs))
                        onSecondsSelected(newSecs)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CircularTimerGaugeCanvas(
            sweepFraction = sweepFraction,
            isTimerRunning = isTimerRunning,
            hasActiveSession = hasActiveSession,
            glowAlpha = glowAlpha,
            glowScale = glowScale
        )

        // Center Time Display / Editable Field
        if (isTimerRunning || hasActiveSession) {
            val mins = (secondsRemaining / 60) % 60
            val secs = secondsRemaining % 60
            val hours = secondsRemaining / 3600
            val displayRunning = if (hours > 0) {
                String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, mins, secs)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayRunning,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                if (!isTimerRunning && hasActiveSession) {
                    Text(
                        text = tr(language, "Pausiert", "დაპაუზებული", "已暂停", "Paused").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = PrimaryViolet.copy(alpha = 0.9f)
                    )
                }
            }
        } else {
            val customTextSelectionColors = TextSelectionColors(
                handleColor = PrimaryViolet,
                backgroundColor = PrimaryViolet.copy(alpha = 0.3f)
            )
            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        val parsed = parseDurationHMS(newValue.text)
                        if (parsed != null && parsed > 0) {
                            onSecondsSelected(parsed.coerceIn(5, 86400))
                        }
                    },
                    modifier = Modifier
                        .width(170.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            isInputFocused = state.isFocused
                            if (!state.isFocused) {
                                val parsed = parseDurationHMS(textValue.text)
                                if (parsed != null && parsed > 0) {
                                    val clamped = parsed.coerceIn(5, 86400)
                                    onSecondsSelected(clamped)
                                    textValue = TextFieldValue(text = formatDurationHMS(clamped))
                                } else {
                                    textValue = TextFieldValue(text = formatDurationHMS(secondsRemaining))
                                }
                            }
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(PrimaryViolet),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    )
                )
            }
        }
    }
}
