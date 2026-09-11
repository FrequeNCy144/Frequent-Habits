package com.example.ui.components.habitrow

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.FocusTimerManager
import com.example.data.Habit
import com.example.ui.theme.PrimaryViolet
import java.util.Locale

@Composable
fun HabitRowTimerIndicator(
    habit: Habit,
    currentValue: Float,
    onAddQuantity: (Int, Float, Float) -> Unit
) {
    val activeTimerHabitId by FocusTimerManager.activeHabitId.collectAsState()
    val isTimerActiveForThisHabit = activeTimerHabitId == habit.id.toLong()
    val isGlobalTimerRunning by FocusTimerManager.isRunning.collectAsState()
    val globalTimerSeconds by FocusTimerManager.secondsRemaining.collectAsState()

    if (isTimerActiveForThisHabit && (isGlobalTimerRunning || globalTimerSeconds > 0)) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_habit_timer")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        val effectiveAlpha = if (isGlobalTimerRunning) pulseAlpha else 0.7f

        val timerMins = globalTimerSeconds / 60
        val timerSecs = globalTimerSeconds % 60
        val timerText = String.format(Locale.US, "%02d:%02d", timerMins, timerSecs)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryViolet.copy(alpha = 0.15f * effectiveAlpha))
                .border(1.dp, PrimaryViolet.copy(alpha = 0.75f * effectiveAlpha), RoundedCornerShape(12.dp))
                .clickable {
                    onAddQuantity(habit.id, currentValue, 0f)
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Running Timer",
                    tint = PrimaryViolet,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            scaleX = if (isGlobalTimerRunning) pulseScale else 1f
                            scaleY = if (isGlobalTimerRunning) pulseScale else 1f
                        }
                )
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryViolet
                )
            }
        }
    }
}
