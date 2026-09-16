package com.example.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SuccessGreen

@Composable
fun TodayProgressEnergyOverlay(
    energyVal: Float,
    progressBarPos: Pair<Offset, Size>?,
    streakChipPos: Pair<Offset, Size>?,
    modifier: Modifier = Modifier
) {
    if (energyVal <= 0f || energyVal >= 1f) return

    Canvas(modifier = modifier) {
        val pBar = progressBarPos
        val sChip = streakChipPos
        if (pBar != null && sChip != null) {
            val startX = pBar.first.x + pBar.second.width
            val pTopY = pBar.first.y + 1.dp.toPx()
            val pBottomY = pBar.first.y + pBar.second.height - 1.dp.toPx()
            val pHeight = pBottomY - pTopY

            val targetX = sChip.first.x
            val sRightX = sChip.first.x + sChip.second.width
            val sTopY = sChip.first.y + 1.dp.toPx()
            val sBottomY = sChip.first.y + sChip.second.height - 1.dp.toPx()
            val sHeight = sBottomY - sTopY

            val dx = targetX - startX

            if (dx > 0f) {
                val electricBlue = Color(0xFF00E5FF)

                // Overall fade out towards the very end
                val globalAlpha = if (energyVal <= 0.85f) 1f else (1f - (energyVal - 0.85f) / 0.15f).coerceIn(0f, 1f)

                // Phase 1: Travel across gap from Progress Bar to Streak Chip Barrier (energyVal: 0.0 -> 0.45)
                val gapProgress = (energyVal / 0.45f).coerceIn(0f, 1f)
                val currentX = startX + dx * gapProgress

                // Lines morph quickly from Green at start to Electric Blue on the way
                if (gapProgress > 0f && energyVal <= 0.65f) {
                    val strandAlpha = if (energyVal <= 0.45f) 1f else (1f - (energyVal - 0.45f) / 0.20f).coerceIn(0f, 1f)
                    val strandCount = 4
                    for (i in 0 until strandCount) {
                        val ratio = (i + 0.5f) / strandCount
                        val startY = pTopY + pHeight * ratio
                        val endY = sTopY + sHeight * ratio

                        val strandPath = Path().apply {
                            moveTo(startX, startY)
                            val midX = (startX + currentX) / 2f
                            val waveOffset = if (i % 2 == 0) 2.5.dp.toPx() else -2.5.dp.toPx()
                            val midY = (startY + endY) / 2f + waveOffset * gapProgress
                            quadraticTo(midX, midY, currentX, endY)
                        }

                        drawPath(
                            path = strandPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    SuccessGreen.copy(alpha = 0.8f * strandAlpha * globalAlpha),
                                    electricBlue.copy(alpha = 0.95f * strandAlpha * globalAlpha),
                                    Color.White.copy(alpha = 0.9f * strandAlpha * globalAlpha)
                                ),
                                startX = startX,
                                endX = currentX.coerceAtLeast(startX + 1f)
                            ),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f * strandAlpha * globalAlpha),
                            radius = 3.dp.toPx(),
                            center = Offset(currentX, endY)
                        )
                    }
                }

                // Phase 2: Collision with left barrier & wrap around top/bottom rounded corners to the back center (energyVal: 0.35 -> 0.85)
                if (energyVal >= 0.35f) {
                    val wrapProgress = ((energyVal - 0.35f) / 0.50f).coerceIn(0f, 1f)
                    val cornerRadius = 14.dp.toPx()
                    val sLeft = targetX
                    val sRight = sRightX
                    val sTop = sTopY
                    val sBottom = sBottomY
                    val sCenterY = sTopY + sHeight / 2f

                    val fullTopPath = Path().apply {
                        moveTo(sLeft, sCenterY)
                        lineTo(sLeft, sTop + cornerRadius)
                        quadraticTo(sLeft, sTop, sLeft + cornerRadius, sTop)
                        lineTo(sRight - cornerRadius, sTop)
                        quadraticTo(sRight, sTop, sRight, sTop + cornerRadius)
                        lineTo(sRight, sCenterY)
                    }

                    val fullBottomPath = Path().apply {
                        moveTo(sLeft, sCenterY)
                        lineTo(sLeft, sBottom - cornerRadius)
                        quadraticTo(sLeft, sBottom, sLeft + cornerRadius, sBottom)
                        lineTo(sRight - cornerRadius, sBottom)
                        quadraticTo(sRight, sBottom, sRight, sBottom - cornerRadius)
                        lineTo(sRight, sCenterY)
                    }

                    val topMeasure = PathMeasure().apply { setPath(fullTopPath, false) }
                    val bottomMeasure = PathMeasure().apply { setPath(fullBottomPath, false) }

                    val partialTopPath = Path()
                    topMeasure.getSegment(0f, topMeasure.length * wrapProgress, partialTopPath, true)

                    val partialBottomPath = Path()
                    bottomMeasure.getSegment(0f, bottomMeasure.length * wrapProgress, partialBottomPath, true)

                    drawPath(
                        path = partialTopPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                electricBlue.copy(alpha = 0.9f * globalAlpha),
                                Color.White.copy(alpha = 1.0f * globalAlpha)
                            ),
                            startX = sLeft,
                            endX = sRight
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = partialBottomPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                electricBlue.copy(alpha = 0.9f * globalAlpha),
                                Color.White.copy(alpha = 1.0f * globalAlpha)
                            ),
                            startX = sLeft,
                            endX = sRight
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Leading sparks at top & bottom moving tips
                    if (wrapProgress > 0f && wrapProgress < 1f) {
                        val topTip = topMeasure.getPosition(topMeasure.length * wrapProgress)
                        val bottomTip = bottomMeasure.getPosition(bottomMeasure.length * wrapProgress)

                        drawCircle(
                            color = Color.White.copy(alpha = 0.95f * globalAlpha),
                            radius = 4.dp.toPx(),
                            center = topTip
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.95f * globalAlpha),
                            radius = 4.dp.toPx(),
                            center = bottomTip
                        )
                    }

                    // Phase 3: Meeting at back (right center) burst pulse when strands merge
                    if (wrapProgress >= 0.85f) {
                        val burstPhase = ((wrapProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)
                        drawCircle(
                            color = electricBlue.copy(alpha = (1f - burstPhase) * globalAlpha),
                            radius = 4.dp.toPx() + burstPhase * 18.dp.toPx(),
                            center = Offset(sRight, sCenterY),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - burstPhase) * globalAlpha),
                            radius = 3.dp.toPx() + burstPhase * 8.dp.toPx(),
                            center = Offset(sRight, sCenterY)
                        )
                    }
                }
            }
        }
    }
}
