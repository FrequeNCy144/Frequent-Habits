package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.Habit
import com.example.ui.HabitIconMapping
import com.example.ui.components.habitrow.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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
    listState: LazyListState? = null,
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
            .habitRowCardBackground(
                animatedBgColor = animatedBgColor,
                animatedBorderColor = animatedBorderColor,
                anchorName = anchorName,
                stackLineFlowProgress = stackLineFlowProgress,
                isStackAuraActive = isStackAuraActive,
                stackTargetEffectProgress = stackTargetEffectProgress,
                pulseAlpha = pulseAlpha,
                popProgress = popProgress,
                isPaused = isPaused,
                isCompleted = isCompleted,
                isFailed = isFailed
            )
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
            HabitRowContent(
                habit = habit,
                currentValue = currentValue,
                isCompleted = isCompleted,
                isFailed = isFailed,
                isPaused = isPaused,
                isMinimalViable = isMinimalViable,
                isWeeklyTargetReached = isWeeklyTargetReached,
                weeklyLoggedCount = weeklyLoggedCount,
                weeklyTargetCount = weeklyTargetCount,
                streak = streak,
                isNumerical = isNumerical,
                habitColor = habitColor,
                checkboxScale = checkboxScale,
                isStackAuraActive = isStackAuraActive,
                stackTargetEffectProgress = stackTargetEffectProgress,
                anchorCompleted = anchorCompleted,
                totalAchievedValue = totalAchievedValue,
                language = language,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isReorderMode) {
                    HabitRowReorderControls(
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
