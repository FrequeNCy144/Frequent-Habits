package com.example.ui.screens.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.data.PerfectDaysStats
import com.example.tr
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

                // In parallel, wait 350ms, then update displayedStreak & pop the scale
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
            }
        } else {
            energyAnim.snapTo(0f)
            streakScale.snapTo(1.0f)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.size(20.dp)
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

                if (isAllDone) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SuccessGreen.copy(alpha = 0.12f))
                            .border(0.8.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✨ " + tr(
                                language,
                                "Fantastisch! Alle Gewohnheiten für heute gemeistert",
                                "ფანტასტიკურია! დღევანდელი ყველა ჩვევა შესრულებულია",
                                "太棒了！今天的所有习惯均已完成",
                                "Fantastic! All habits crushed for today"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            // Energy Multi-Strand Bridge & Frame Circuit Wrap Overlay
            TodayProgressEnergyOverlay(
                energyVal = energyAnim.value,
                progressBarPos = progressBarPos,
                streakChipPos = streakChipPos,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
