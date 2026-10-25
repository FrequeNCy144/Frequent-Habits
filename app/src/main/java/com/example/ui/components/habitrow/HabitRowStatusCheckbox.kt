package com.example.ui.components.habitrow
import com.frequent.habits.R

import androidx.compose.ui.res.painterResource

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.ui.components.FeedbackHelper
import com.example.ui.theme.FailedRed
import com.example.ui.theme.HabitOrange
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HabitRowStatusCheckbox(
    habit: Habit,
    isNumerical: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
    isPaused: Boolean,
    isWeeklyTargetReached: Boolean,
    habitColor: Color,
    checkboxScale: Float,
    extraPopScale: Float,
    popRotation: Float,
    popProgress: Float,
    animatedProgressRatio: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    var floatingDeltaText by remember { mutableStateOf<String?>(null) }
    val deltaAnim = remember { Animatable(0f) }

    val handleTapped = {
        coroutineScope.launch {
            bounceScale.snapTo(0.85f)
            bounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        if (isNumerical && !isCompleted && !isPaused && !isFailed) {
            FeedbackHelper.playCompletionFeedback(context, vibrationEnabled = true)
            val step = if (habit.clickIncrement > 0f) habit.clickIncrement else 1f
            val deltaStr = "+${if (step % 1f == 0f) step.toInt().toString() else step.toString()}"
            floatingDeltaText = deltaStr
            coroutineScope.launch {
                deltaAnim.snapTo(0f)
                deltaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                )
                floatingDeltaText = null
            }
        }
        onClick()
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                val totalScale = (checkboxScale + extraPopScale) * bounceScale.value
                scaleX = totalScale
                scaleY = totalScale
                rotationZ = popRotation
            }
            .drawBehind {
                if (popProgress > 0f && popProgress < 1f) {
                    // 1. Expanding glowing halo ring
                    val haloRadius = (size.width / 2f) + (36.dp.toPx() * popProgress)
                    val haloAlpha = (1f - popProgress) * 0.75f
                    drawCircle(
                        color = SuccessGreen.copy(alpha = haloAlpha),
                        radius = haloRadius,
                        style = Stroke(width = 2.5.dp.toPx() * (1f - popProgress))
                    )

                    // 2. Sparkling particle dots burst
                    val particleCount = 6
                    for (i in 0 until particleCount) {
                        val angle = i * (360f / particleCount) * (Math.PI / 180.0)
                        val dist = 32.dp.toPx() * popProgress
                        val px = (size.width / 2f) + (cos(angle) * dist).toFloat()
                        val py = (size.height / 2f) + (sin(angle) * dist).toFloat()
                        val pAlpha = (1f - popProgress) * 0.9f
                        val pRadius = 3.dp.toPx() * (1f - popProgress)
                        val pColor = if (i % 2 == 0) SuccessGreen else Color(0xFFFFD54F)

                        drawCircle(
                            color = pColor.copy(alpha = pAlpha),
                            radius = pRadius,
                            center = Offset(px, py)
                        )
                    }
                }

                // 3. Circular Progress Ring for numerical / quantity habits
                if (isNumerical && !isCompleted && !isPaused && !isFailed) {
                    val strokeWidthPx = 3.dp.toPx()
                    val inset = strokeWidthPx / 2f
                    val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                    // Base track ring
                    drawArc(
                        color = habitColor.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx)
                    )

                    // Active progress arc closing up to goal
                    if (animatedProgressRatio > 0f) {
                        drawArc(
                            color = habitColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgressRatio,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
            .background(
                when {
                    isPaused -> HabitOrange
                    isCompleted -> SuccessGreen
                    isFailed -> FailedRed
                    habit.frequency == "TIMES_WEEKLY" && isWeeklyTargetReached -> SuccessGreen.copy(alpha = 0.15f)
                    isNumerical -> habitColor.copy(alpha = 0.12f)
                    else -> Color.Transparent
                },
                CircleShape
            )
            .border(
                width = 2.dp,
                color = when {
                    isPaused -> HabitOrange
                    isCompleted -> SuccessGreen
                    isFailed -> FailedRed
                    habit.frequency == "TIMES_WEEKLY" && isWeeklyTargetReached -> SuccessGreen
                    isNumerical -> Color.Transparent
                    else -> TextSecondary.copy(alpha = 0.5f)
                },
                shape = CircleShape
            )
            .clickable(enabled = !isPaused, onClick = handleTapped),
        contentAlignment = Alignment.Center
    ) {
        when {
            isPaused -> {
                Icon(painter = painterResource(id = R.drawable.ic_pause), contentDescription = "Paused", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            isFailed -> {
                Icon(Icons.Default.Close, contentDescription = "Failed", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            isNumerical -> {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment",
                    tint = habitColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            isCompleted -> {
                Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            (habit.frequency == "TIMES_WEEKLY" || habit.frequency == "TIMES_MONTHLY") && isWeeklyTargetReached -> {
                Icon(Icons.Default.Check, contentDescription = "Target Reached", tint = SuccessGreen, modifier = Modifier.size(18.dp))
            }
        }

        if (floatingDeltaText != null && deltaAnim.value < 1f) {
            Text(
                text = floatingDeltaText!!,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = habitColor,
                modifier = Modifier
                    .offset(y = (-24).dp * deltaAnim.value)
                    .graphicsLayer {
                        alpha = (1f - deltaAnim.value).coerceIn(0f, 1f)
                        scaleX = 0.9f + (0.35f * (1f - deltaAnim.value))
                        scaleY = 0.9f + (0.35f * (1f - deltaAnim.value))
                    }
            )
        }
    }
}
