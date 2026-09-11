package com.example.ui.screens.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import com.example.data.PerfectDaysStats
import com.example.ui.dialogs.animatedGlowingBorder
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayProgressSummaryCard(
    selectedDate: String,
    todayDateString: String,
    todayProgressTuple: Pair<Int, Int>,
    perfectDaysStats: PerfectDaysStats,
    progressGlowColor: Color,
    language: String,
    onClick: () -> Unit
) {
    val isPastDay = remember(selectedDate, todayDateString) {
        selectedDate < todayDateString
    }
    val (completed, total) = todayProgressTuple
    val fraction = remember(todayProgressTuple) {
        if (total > 0) completed.toFloat() / total else 0f
    }
    val progressText = remember(completed, total) { "$completed/$total" }
    var hasAnimatedTodayProgress by rememberSaveable { mutableStateOf(false) }
    val animFraction = remember { Animatable(if (hasAnimatedTodayProgress) fraction else 0f) }

    LaunchedEffect(fraction) {
        if (!hasAnimatedTodayProgress) {
            hasAnimatedTodayProgress = true
            animFraction.animateTo(
                targetValue = fraction,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        } else {
            animFraction.animateTo(
                targetValue = fraction,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    val animatedFraction = animFraction.value
    val streakScale = remember { Animatable(1f) }
    val energyAnim = remember { Animatable(0f) }
    var displayedStreak by remember { mutableStateOf(perfectDaysStats.currentStreak) }
    var hasAnimatedStreak by rememberSaveable(selectedDate) { mutableStateOf(fraction >= 1f) }

    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var progressBarPos by remember { mutableStateOf<Pair<Offset, Size>?>(null) }
    var streakChipPos by remember { mutableStateOf<Pair<Offset, Size>?>(null) }

    LaunchedEffect(fraction, selectedDate, perfectDaysStats.currentStreak) {
        if (fraction >= 1.0f && total > 0) {
            if (!hasAnimatedStreak) {
                // Keep pre-incremented value prior to animation finish
                displayedStreak = (perfectDaysStats.currentStreak - 1).coerceAtLeast(0)
            } else {
                displayedStreak = perfectDaysStats.currentStreak
            }
        } else {
            // Fraction < 1.0f (user has unchecked habit, etc.)
            displayedStreak = perfectDaysStats.currentStreak
            hasAnimatedStreak = false
        }
    }

    LaunchedEffect(animatedFraction) {
        if (animatedFraction >= 1.0f && fraction >= 1.0f && total > 0) {
            if (!hasAnimatedStreak) {
                energyAnim.snapTo(0f)
                
                // Animate energy bridge flow
                val energyJob = launch {
                    energyAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing)
                    )
                }

                // In parallel, wait 350ms (when energy reaches left wall of the streak chip),
                // then update displayedStreak to increment and pop the scale with spring physics
                val scaleJob = launch {
                    kotlinx.coroutines.delay(350)
                    displayedStreak = perfectDaysStats.currentStreak
                    hasAnimatedStreak = true
                    streakScale.snapTo(1.0f)
                    streakScale.animateTo(
                        targetValue = 1.22f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    streakScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            } else {
                energyAnim.snapTo(0f)
                streakScale.snapTo(1.0f)
            }
        } else {
            // Cancel and reset animation cleanly if progress falls below 100%
            if (fraction < 1.0f) {
                energyAnim.snapTo(0f)
                streakScale.snapTo(1.0f)
            }
        }
    }

    val isAllDone = fraction >= 1.0f && total > 0
    val cardGlowTargetColor = if (isAllDone) SuccessGreen else progressGlowColor
    val currentCardGlowColor by animateColorAsState(targetValue = cardGlowTargetColor, animationSpec = tween(600))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .animatedGlowingBorder(currentCardGlowColor, 20.dp, 2.dp, currentCardGlowColor)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onClick() }
            )
            .testTag("today_progress_card"),
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { parentCoordinates = it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .onGloballyPositioned { coords ->
                            val pCoords = parentCoordinates
                            if (pCoords != null && pCoords.isAttached && coords.isAttached) {
                                val relPos = pCoords.localPositionOf(coords, Offset.Zero)
                                progressBarPos = relPos to coords.size.toSize()
                            }
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppCard)
                        .border(1.dp, AppBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(18.dp))
                                .background(SuccessGreen)
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$progressText (${(fraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Perfect Days Streak Counter Chip
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val pCoords = parentCoordinates
                            if (pCoords != null && pCoords.isAttached && coords.isAttached) {
                                val relPos = pCoords.localPositionOf(coords, Offset.Zero)
                                streakChipPos = relPos to coords.size.toSize()
                            }
                        }
                        .border(
                            if (displayedStreak >= 1) 1.5.dp else 1.dp,
                            if (displayedStreak >= 1) PerfectStreakFlame else AppBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppCard)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Perfect Days Streak",
                            tint = if (displayedStreak > 0) PerfectStreakFlame else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(20.dp)
                        )
                        Text(
                            text = "${displayedStreak}d",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.scale(streakScale.value)
                        )
                    }
                }
            }

            // Energy Multi-Strand Bridge & Frame Circuit Wrap Overlay when 100% is hit
            val energyVal = energyAnim.value
            if (energyVal > 0f && energyVal < 1f) {
                Canvas(modifier = Modifier.matchParentSize()) {
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
        }
    }
}
