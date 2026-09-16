package com.example.ui.components.dialogs.addvalue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularTimerGaugeCanvas(
    sweepFraction: Float,
    isTimerRunning: Boolean,
    hasActiveSession: Boolean,
    glowAlpha: Float,
    glowScale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = (size.minDimension / 2f) - 12.dp.toPx()
        val innerRadius = outerRadius - 14.dp.toPx()

        // 1. Inner dark background circle
        drawCircle(
            color = Color(0xFF13141E),
            radius = innerRadius,
            center = center
        )

        // 2. Atmospheric glow
        if (isTimerRunning || hasActiveSession) {
            val currentAlpha = if (isTimerRunning) glowAlpha else 0.14f
            val currentScale = if (isTimerRunning) glowScale else 0.55f
            val currentGlowRadius = innerRadius * currentScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = currentAlpha * 0.85f),
                        PrimaryViolet.copy(alpha = currentAlpha * 0.40f),
                        PrimaryViolet.copy(alpha = currentAlpha * 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentGlowRadius
                ),
                radius = currentGlowRadius,
                center = center
            )
        }

        // 3. 60 clock dial tick marks
        for (i in 0 until 60) {
            val tickAngleDeg = (i * 6f) - 90f
            val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
            val isMajor = i % 5 == 0
            val tickLength = if (isMajor) 7.dp.toPx() else 4.dp.toPx()
            val tickColor = if (isMajor) PrimaryViolet.copy(alpha = 0.8f) else Color(0xFF424458)
            val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

            val startR = innerRadius - 4.dp.toPx()
            val endR = startR - tickLength

            val startX = (center.x + startR * cos(tickAngleRad)).toFloat()
            val startY = (center.y + startR * sin(tickAngleRad)).toFloat()
            val endX = (center.x + endR * cos(tickAngleRad)).toFloat()
            val endY = (center.y + endR * sin(tickAngleRad)).toFloat()

            drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // 4. Background ring track
        drawCircle(
            color = Color(0xFF232438),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 9.dp.toPx())
        )

        // 5. Active purple arc
        val sweepAngle = sweepFraction * 360f
        if (sweepAngle > 0f) {
            drawArc(
                color = PrimaryViolet,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            )

            // 6. Indicator knob
            val knobAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
            val knobX = (center.x + outerRadius * cos(knobAngleRad)).toFloat()
            val knobY = (center.y + outerRadius * sin(knobAngleRad)).toFloat()

            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(knobX, knobY)
            )
            drawCircle(
                color = Color(0xFF13141E),
                radius = 2.5.dp.toPx(),
                center = Offset(knobX, knobY)
            )
        }
    }
}
