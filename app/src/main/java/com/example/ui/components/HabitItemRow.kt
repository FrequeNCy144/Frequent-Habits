package com.example.ui.components

import androidx.compose.ui.layout.onGloballyPositioned
import com.example.FocusTimerManager
import com.example.tr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.Habit
import com.example.ui.HabitIconMapping
import com.example.ui.components.habitrow.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitItemRow(
    modifier: Modifier = Modifier,
    habit: Habit,
    currentValue: Float,
    isCompleted: Boolean,
    isFailed: Boolean,
    isPaused: Boolean,
    hasLog: Boolean,
    isMinimalViable: Boolean = false,
    onToggle: (Int, Boolean) -> Unit,
    onAddQuantity: (Int, Float, Float) -> Unit,
    onLongClick: (Habit) -> Unit,
    language: String,
    vibrationEnabled: Boolean = true,
    selectedDate: String = "",
    isWeeklyTargetReached: Boolean = false,
    weeklyLoggedCount: Int = 0,
    weeklyTargetCount: Int = 0,
    streak: Int = 0,
    isReorderMode: Boolean = false,
    anchorName: String? = null,
    anchorCompleted: Boolean = false,
    anchorJustCompleted: Boolean = false,
    onMoveUp: ((Int) -> Unit)? = null,
    onMoveDown: ((Int) -> Unit)? = null,
    listState: androidx.compose.foundation.lazy.LazyListState? = null,
    isBeingDragged: Boolean = false,
    dragTranslationY: Float = 0f,
    onDragStart: (() -> Unit)? = null,
    onDragDelta: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    totalAchievedValue: Float = 0f,
    isGoalTargetReached: Boolean = false,
    onCompleteGoal: ((Habit) -> Unit)? = null
) {
    val context = LocalContext.current
    val habitColor = HabitIconMapping.getColor(habit.color)

    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isPaused -> PausedBg
            isCompleted -> SuccessBg
            isFailed -> FailedBg
            else -> AppCard
        },
        animationSpec = tween(120),
        label = "bgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isPaused -> HabitOrange
            isCompleted -> SuccessGreen
            isFailed -> FailedRed
            else -> AppBorder
        },
        animationSpec = tween(120),
        label = "borderColor"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (isPaused || isCompleted || isFailed) 1.05f else 1.0f,
        animationSpec = tween(100),
        label = "checkboxScale"
    )

    val isNumerical = habit.type == "NUMBER" || habit.type == "NUMERICAL" || (habit.targetValue > 1f && habit.type != "BINARY")
    val targetVal = if (habit.targetValue > 0f) habit.targetValue else 1f
    val progressRatio = if (isNumerical) (currentValue / targetVal).coerceIn(0f, 1f) else if (isCompleted) 1f else 0f
    val animatedProgressRatio by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "progressRatio"
    )

    var lastDate by remember(habit.id) { mutableStateOf(selectedDate) }
    var wasCompleted by remember(habit.id, selectedDate) { mutableStateOf(isCompleted) }
    var isDateChange by remember(selectedDate) { mutableStateOf(true) }
    val completionAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val cardShadowElevation by animateDpAsState(
        targetValue = if (isBeingDragged) 12.dp else 0.dp,
        label = "shadowElevation"
    )
    val dragZIndex = if (isBeingDragged) 1000f else 0f

    val triggerUserFeedback = {
        FeedbackHelper.playCompletionFeedback(context, vibrationEnabled)
        coroutineScope.launch {
            completionAnim.snapTo(0f)
            completionAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    val handleUserToggle = {
        if (!isNumerical && !isCompleted && !isFailed) {
            triggerUserFeedback()
        }
        onToggle(habit.id, hasLog)
    }

    val handleUserAddQuantity = {
        val step = if (habit.clickIncrement > 0f) habit.clickIncrement else 1f
        if (currentValue < targetVal && currentValue + step >= targetVal) {
            triggerUserFeedback()
        }
        onAddQuantity(habit.id, currentValue, step)
    }

    LaunchedEffect(isCompleted, selectedDate) {
        lastDate = selectedDate
        wasCompleted = isCompleted
        isDateChange = false
    }

    val popProgress = completionAnim.value
    val cardScale = 1f + if (popProgress > 0f && popProgress < 1f) {
        0.025f * kotlin.math.sin(popProgress * Math.PI.toFloat())
    } else 0f

    val extraPopScale = if (popProgress > 0f && popProgress < 1f) {
        0.35f * kotlin.math.sin(popProgress * Math.PI.toFloat())
    } else 0f
    val popRotation = if (popProgress > 0f && popProgress < 1f) {
        (1f - popProgress) * -25f
    } else 0f

    val density = androidx.compose.ui.platform.LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Habit Stacking: energy flow traveling along the line from anchor habit down to this habit
    // Only animate traveling beam if anchor was actively completed during this session (anchorJustCompleted).
    // If it was already completed (e.g. on app launch or tab change), show it immediately at 1f.
    val stackLineFlowAnim = remember(habit.id) {
        Animatable(if (anchorCompleted && !anchorJustCompleted) 1f else 0f)
    }

    LaunchedEffect(anchorCompleted, anchorJustCompleted) {
        if (!anchorCompleted) {
            stackLineFlowAnim.snapTo(0f)
        } else if (anchorJustCompleted) {
            stackLineFlowAnim.snapTo(0f)
            stackLineFlowAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        } else {
            stackLineFlowAnim.snapTo(1f)
        }
    }
    val stackLineFlowProgress = stackLineFlowAnim.value

    // Follower Habit effect transforms only when the energy beam has fully reached the icon
    val stackTargetEffectAnim = remember(habit.id) {
        Animatable(if (anchorCompleted && !anchorJustCompleted) 1f else 0f)
    }

    LaunchedEffect(anchorCompleted, anchorJustCompleted, stackLineFlowProgress >= 0.92f) {
        if (!anchorCompleted) {
            stackTargetEffectAnim.snapTo(0f)
        } else if (anchorJustCompleted) {
            if (stackLineFlowProgress >= 0.92f) {
                stackTargetEffectAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                )
            }
        } else {
            stackTargetEffectAnim.snapTo(1f)
        }
    }
    val stackTargetEffectProgress = stackTargetEffectAnim.value
    val isStackAuraActive = stackTargetEffectProgress > 0.02f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(dragZIndex)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                translationY = if (isBeingDragged) dragTranslationY else 0f
                shadowElevation = cardShadowElevation.toPx()
            }
            .drawBehind {
                // 1. Draw animated background
                drawRoundRect(
                    color = animatedBgColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )

                // 1a. Visual Connection Line for Stacked Habits (centered directly on symbols, bridging from symbol to symbol)
                if (anchorName != null) {
                    val lineX = 40.dp.toPx() // Precisely centered on the 48.dp icon (16.dp padding + 24.dp)
                    val startY = -25.dp.toPx() // Bottom edge of the previous habit's symbol
                    val endY = (size.height / 2f) - 24.dp.toPx() // Top edge of the current habit's symbol
                    val totalDistance = (endY - startY).coerceAtLeast(1f)
                    
                    // Base track (subtle dashed line)
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.22f),
                        start = androidx.compose.ui.geometry.Offset(lineX, startY),
                        end = androidx.compose.ui.geometry.Offset(lineX, endY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // Flowing energy beam moving smoothly down the line
                    if (stackLineFlowProgress > 0f) {
                        val currentTipY = startY + totalDistance * stackLineFlowProgress

                        // Glowing ambient halo behind the active line segment
                        drawLine(
                            color = PrimaryViolet.copy(alpha = 0.35f * stackLineFlowProgress),
                            start = androidx.compose.ui.geometry.Offset(lineX, startY),
                            end = androidx.compose.ui.geometry.Offset(lineX, currentTipY),
                            strokeWidth = 6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
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
                            start = androidx.compose.ui.geometry.Offset(lineX, startY),
                            end = androidx.compose.ui.geometry.Offset(lineX, currentTipY),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        // Travelling particle / glowing spark head at the tip of the beam
                        if (stackLineFlowProgress < 0.99f) {
                            drawCircle(
                                color = PrimaryViolet.copy(alpha = 0.6f),
                                radius = 6.5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(lineX, currentTipY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(lineX, currentTipY)
                            )
                        } else {
                            // Reached the icon: pulsing connection spark
                            drawCircle(
                                color = PrimaryViolet.copy(alpha = 0.45f * (pulseAlpha + 0.3f)),
                                radius = 5.5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(lineX, endY)
                            )
                        }
                    }
                }

                // 1b. Glowing ambient aura in accent color when anchor habit energy arrives
                if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) {
                    drawRoundRect(
                        color = PrimaryViolet.copy(alpha = 0.22f * (pulseAlpha + 0.3f) * stackTargetEffectProgress),
                        size = androidx.compose.ui.geometry.Size(
                            width = size.width + (6.dp.toPx() * stackTargetEffectProgress),
                            height = size.height + (6.dp.toPx() * stackTargetEffectProgress)
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx() * stackTargetEffectProgress, -3.dp.toPx() * stackTargetEffectProgress),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx())
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
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                }

                // 2. Draw animated or gradient border
                val brush = when {
                    isPaused -> {
                        Brush.linearGradient(
                            colors = listOf(
                                HabitOrange,
                                HabitOrange.copy(alpha = 0.4f),
                                HabitOrange
                            )
                        )
                    }
                    isCompleted -> {
                        Brush.linearGradient(
                            colors = listOf(
                                SuccessGreen,
                                SuccessGreen.copy(alpha = 0.4f),
                                SuccessGreen
                            )
                        )
                    }
                    isFailed -> {
                        Brush.linearGradient(
                            colors = listOf(
                                FailedRed,
                                FailedRed.copy(alpha = 0.4f),
                                FailedRed
                            )
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
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
            .let { m ->
                if (isReorderMode) {
                    m
                } else {
                    m.combinedClickable(
                        onClick = { handleUserToggle() },
                        onLongClick = { onLongClick(habit) }
                    )
                }
            }
            .testTag("habit_card_${habit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            val popEnergy = if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) {
                                0.12f * kotlin.math.sin(stackTargetEffectProgress * Math.PI.toFloat())
                            } else 0f
                            scaleX = checkboxScale * (1f + popEnergy)
                            scaleY = checkboxScale * (1f + popEnergy)
                        }
                        .background(
                            if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) {
                                PrimaryViolet.copy(alpha = 0.22f * stackTargetEffectProgress + 0.15f * (1f - stackTargetEffectProgress))
                            } else {
                                habitColor.copy(alpha = 0.15f)
                            },
                            CircleShape
                        )
                        .border(
                            if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) 1.5.dp else 1.dp,
                            if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) PrimaryViolet else habitColor.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconMapping.getIcon(habit.icon),
                        contentDescription = habit.name,
                        tint = if (isStackAuraActive && !isCompleted && !isFailed && !isPaused) PrimaryViolet else habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitleText = if (isPaused) {
                        tr(language, "Skipped", "გამოტოვებული", "已跳过", "Skipped")
                    } else if (habit.frequency == "TIMES_WEEKLY") {
                        "$weeklyLoggedCount/$weeklyTargetCount " + (tr(language, "in 7 Tagen", "7 დღეში", "7 天内", "in 7 days"))
                    } else if (isNumerical) {
                        val displayValue = if (isFailed) 0f else currentValue
                        val formattedVal = if (displayValue % 1f == 0f) displayValue.toInt().toString() else displayValue.toString()
                        val targetVal = if (isMinimalViable && habit.minimalViableValue != null) habit.minimalViableValue else habit.targetValue
                        val formattedTarget = if (targetVal % 1f == 0f) targetVal.toInt().toString() else targetVal.toString()
                        val unitStr = if (habit.unit.isNotBlank()) " ${habit.unit}" else ""
                        val suffix = if (isMinimalViable) " (Min)" else ""
                        "$formattedVal / $formattedTarget$unitStr$suffix"
                    } else {
                        if (isCompleted) {
                            val suffix = if (isMinimalViable) " (Min)" else ""
                            tr(language, "Erledigt", "დასრულებული", "已完成", "Completed") + suffix
                        } else if (isFailed) {
                            tr(language, "Fehlgeschlagen", "ვერ მოხერხდა", "未达成", "Failed")
                        } else if (anchorCompleted && isStackAuraActive) {
                            tr(language, "Perfektes Momentum!", "Perfect Momentum!")
                        } else {
                            if (isMinimalViable) {
                                if (habit.minimalViableText.isNotBlank()) {
                                    "⚡ " + habit.minimalViableText
                                } else {
                                    "⚡ Min"
                                }
                            } else ""
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Streak always first on second line
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Habit Streak",
                                tint = HabitStreakFlame,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "$streak",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }

                        if (subtitleText.isNotBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = subtitleText,
                                style = if (isNumerical && !isPaused) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                                fontWeight = if ((isNumerical || isMinimalViable) && !isPaused) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isPaused -> HabitOrange
                                    isCompleted -> SuccessGreen
                                    isFailed -> FailedRed
                                    habit.frequency == "TIMES_WEEKLY" && isWeeklyTargetReached -> SuccessGreen
                                    anchorCompleted && isStackAuraActive -> HabitOrange
                                    isMinimalViable -> PrimaryViolet
                                    else -> if (isNumerical) habitColor else TextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (habit.isFinishable && habit.totalTargetValue != null && habit.totalTargetValue > 0f) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val totalTarget = habit.totalTargetValue
                            val fraction = (totalAchievedValue / totalTarget).coerceIn(0f, 1f)
                            val pct = (fraction * 100).toInt()
                            val formattedCurrent = if (totalAchievedValue % 1f == 0f) totalAchievedValue.toInt().toString() else totalAchievedValue.toString()
                            val formattedTarget = if (totalTarget % 1f == 0f) totalTarget.toInt().toString() else totalTarget.toString()
                            val unitStr = if (habit.unit.isNotBlank()) " ${habit.unit}" else if (!isNumerical) " " + tr(language, "Tage", "დღე", "天", "days") else ""

                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = HabitYellow,
                                trackColor = ProgressTrack
                            )
                            Text(
                                text = "$formattedCurrent/$formattedTarget$unitStr ($pct%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = HabitYellow.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isReorderMode) {
                    com.example.ui.components.habitrow.HabitRowReorderControls(
                        habitId = habit.id,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown
                    )
                } else {
                    HabitRowTimerIndicator(
                        habit = habit,
                        currentValue = currentValue,
                        onAddQuantity = onAddQuantity
                    )

                    // Status Checkbox (zum schnellen Abhaken/Zurücksetzen bzw. um 1 Erhöhen für Zahlenbasiert)
                    HabitRowStatusCheckbox(
                        habit = habit,
                        isNumerical = isNumerical,
                        isCompleted = isCompleted,
                        isFailed = isFailed,
                        isPaused = isPaused,
                        isWeeklyTargetReached = isWeeklyTargetReached,
                        habitColor = habitColor,
                        checkboxScale = checkboxScale,
                        extraPopScale = extraPopScale,
                        popRotation = popRotation,
                        popProgress = popProgress,
                        animatedProgressRatio = animatedProgressRatio,
                        onClick = {
                            if (isNumerical) {
                                handleUserAddQuantity()
                            } else {
                                handleUserToggle()
                            }
                        }
                    )
            }
        }
    }
}

}
