package com.example.ui.screens.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.components.getStrengthLabel
import com.example.ui.dialogs.animatedGlowingBorder
import com.example.ui.theme.*

@Composable
fun StatsOverallCard(
    strength: Int,
    language: String,
    accentColor: Color = PrimaryViolet,
    onOverallClick: () -> Unit
) {
    var hasAnimatedTodayStrength by rememberSaveable { mutableStateOf(false) }
    val animStrength = remember { Animatable(if (hasAnimatedTodayStrength) strength.toFloat() else 0f) }

    LaunchedEffect(strength) {
        if (!hasAnimatedTodayStrength) {
            animStrength.snapTo(0f)
            animStrength.animateTo(
                targetValue = strength.toFloat(),
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
            hasAnimatedTodayStrength = true
        } else {
            animStrength.animateTo(
                targetValue = strength.toFloat(),
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }
    val animatedStrength = animStrength.value
    val ringColor = remember(strength) {
        when {
            strength < 35 -> HabitRed
            strength < 70 -> HabitYellow
            else -> SuccessGreen
        }
    }
    val strengthLabel = remember(strength, language) { getStrengthLabel(strength, language) }

    val overallInteractionSource = remember { MutableInteractionSource() }
    val overallIsPressed by overallInteractionSource.collectIsPressedAsState()
    val overallArrowOffsetX by animateDpAsState(
        targetValue = if (overallIsPressed) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "overall_arrow_offset"
    )
    val overallArrowScale by animateFloatAsState(
        targetValue = if (overallIsPressed) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "overall_arrow_scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animatedGlowingBorder(
                glowColor = accentColor,
                cornerRadius = 22.dp,
                borderWidth = 2.dp,
                baseColor = accentColor
            )
            .clickable(
                interactionSource = overallInteractionSource,
                indication = ripple(),
                onClick = onOverallClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Strength",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "GESAMT-STÄRKE", "საერთო სიძლიერე", "综合稳固度", "OVERALL STRENGTH"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = if (overallIsPressed) accentColor else TextSecondary,
                    modifier = Modifier
                        .offset(x = overallArrowOffsetX)
                        .scale(overallArrowScale)
                        .size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val sweepHalfVal = (animatedStrength / 100f) * 180f
                Canvas(
                    modifier = Modifier
                        .width(290.dp)
                        .height(150.dp)
                ) {
                    val strokeWidth = 18.dp.toPx()
                    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(size.width - strokeWidth, (size.height * 2) - strokeWidth)

                    drawArc(
                        color = ProgressTrack,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )

                    if (sweepHalfVal > 0f) {
                        drawArc(
                            color = ringColor,
                            startAngle = 180f,
                            sweepAngle = sweepHalfVal,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "${animatedStrength.toInt()}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    if (strengthLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strengthLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ringColor
                        )
                    }
                }
            }
        }
    }
}
