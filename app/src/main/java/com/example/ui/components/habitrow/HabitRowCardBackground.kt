package com.example.ui.components.habitrow

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

fun Modifier.habitRowCardBackground(
    animatedBgColor: Color,
    animatedBorderColor: Color,
    anchorName: String?,
    stackLineFlowProgress: Float,
    isStackAuraActive: Boolean,
    stackTargetEffectProgress: Float,
    pulseAlpha: Float,
    popProgress: Float,
    isPaused: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean
): Modifier = this.drawBehind {
    // 1. Draw animated background
    drawRoundRect(
        color = animatedBgColor,
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    )

    // 1a. Visual Connection Line for Stacked Habits
    if (anchorName != null) {
        val lineX = 40.dp.toPx()
        val startY = -25.dp.toPx()
        val endY = (size.height / 2f) - 24.dp.toPx()
        val totalDistance = (endY - startY).coerceAtLeast(1f)

        // Base track (subtle dashed line)
        drawLine(
            color = TextSecondary.copy(alpha = 0.22f),
            start = Offset(lineX, startY),
            end = Offset(lineX, endY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )

        // Flowing energy beam moving smoothly down the line
        if (stackLineFlowProgress > 0f) {
            val currentTipY = startY + totalDistance * stackLineFlowProgress

            // Glowing ambient halo behind the active line segment
            drawLine(
                color = PrimaryViolet.copy(alpha = 0.35f * stackLineFlowProgress),
                start = Offset(lineX, startY),
                end = Offset(lineX, currentTipY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Energized glowing core line
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = 0.85f),
                        PrimaryViolet,
                        Color(0xFFE0C3FC)
                    ),
                    startY = startY,
                    endY = currentTipY.coerceAtLeast(startY + 1f)
                ),
                start = Offset(lineX, startY),
                end = Offset(lineX, currentTipY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Travelling particle / glowing spark head at the tip of the beam
            if (stackLineFlowProgress < 0.99f) {
                drawCircle(
                    color = PrimaryViolet.copy(alpha = 0.6f),
                    radius = 6.5.dp.toPx(),
                    center = Offset(lineX, currentTipY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(lineX, currentTipY)
                )
            } else {
                // Reached the icon: pulsing connection spark
                drawCircle(
                    color = PrimaryViolet.copy(alpha = 0.45f * (pulseAlpha + 0.3f)),
                    radius = 5.5.dp.toPx(),
                    center = Offset(lineX, endY)
                )
            }
        }
    }

    // 1b. Glowing ambient aura in accent color when anchor habit energy arrives
    if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) {
        drawRoundRect(
            color = PrimaryViolet.copy(alpha = 0.22f * (pulseAlpha + 0.3f) * stackTargetEffectProgress),
            size = Size(
                width = size.width + (6.dp.toPx() * stackTargetEffectProgress),
                height = size.height + (6.dp.toPx() * stackTargetEffectProgress)
            ),
            topLeft = Offset(-3.dp.toPx() * stackTargetEffectProgress, -3.dp.toPx() * stackTargetEffectProgress),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )
    }

    // 1c. Glowing pulse & shine across the whole card on completion
    if (popProgress > 0f && popProgress < 1f) {
        val glowAlpha = 0.38f * kotlin.math.sin(popProgress * Math.PI.toFloat())
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    SuccessGreen.copy(alpha = glowAlpha),
                    Color(0xFFFFD54F).copy(alpha = glowAlpha * 0.6f),
                    SuccessGreen.copy(alpha = glowAlpha)
                )
            ),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )
    }

    // 2. Draw animated or gradient border
    val brush = when {
        isPaused -> {
            Brush.linearGradient(
                colors = listOf(HabitOrange, HabitOrange.copy(alpha = 0.4f), HabitOrange)
            )
        }
        isCompleted -> {
            Brush.linearGradient(
                colors = listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.4f), SuccessGreen)
            )
        }
        isFailed -> {
            Brush.linearGradient(
                colors = listOf(FailedRed, FailedRed.copy(alpha = 0.4f), FailedRed)
            )
        }
        else -> {
            if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) {
                Brush.linearGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = ((pulseAlpha + 0.3f) * stackTargetEffectProgress).coerceIn(0f, 1f)),
                        PrimaryViolet.copy(alpha = stackTargetEffectProgress.coerceIn(0.3f, 1f)),
                        PrimaryViolet.copy(alpha = ((pulseAlpha + 0.3f) * stackTargetEffectProgress).coerceIn(0f, 1f))
                    )
                )
            } else {
                SolidColor(animatedBorderColor)
            }
        }
    }

    val strokeWidth = if (popProgress > 0f && popProgress < 1f) {
        (1.dp.toPx() + 2.dp.toPx() * kotlin.math.sin(popProgress * Math.PI.toFloat()))
    } else if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) {
        1.5.dp.toPx() + (0.5.dp.toPx() * stackTargetEffectProgress)
    } else {
        1.dp.toPx()
    }
    drawRoundRect(
        brush = brush,
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
}
