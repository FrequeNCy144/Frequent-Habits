package com.example.ui.screens.habitdetail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.InfoIconButton
import com.example.ui.components.getStrengthLabel
import com.example.ui.theme.*

@Composable
fun HabitDetailStrengthCard(
    habit: Habit,
    strength: Int,
    language: String,
    onInfoClick: (String, String) -> Unit
) {
    val habitColor = remember(habit.color) { HabitIconMapping.getColor(habit.color) }
    var hasAnimatedDetailStrength by rememberSaveable(habit.id) { mutableStateOf(false) }
    val animStrength = remember(habit.id) { Animatable(if (hasAnimatedDetailStrength) strength.toFloat() else 0f) }

    LaunchedEffect(strength, habit.id) {
        if (!hasAnimatedDetailStrength) {
            animStrength.snapTo(0f)
            animStrength.animateTo(
                targetValue = strength.toFloat(),
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
            hasAnimatedDetailStrength = true
        } else {
            animStrength.animateTo(
                targetValue = strength.toFloat(),
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }
    val animatedStrength = animStrength.value
    val sweepAngleVal = (animatedStrength / 100f) * 360f
    val strengthText = "${animatedStrength.toInt()}"
    val strengthLabel = remember(strength, language) { getStrengthLabel(strength, language) }
    val strengthColor = remember(strength) {
        when {
            strength < 35 -> HabitRed
            strength < 70 -> HabitYellow
            else -> SuccessGreen
        }
    }
    val startDateStr = remember(habit.startDate, language) {
        try {
            val date = java.util.Date(habit.startDate)
            val format = java.text.SimpleDateFormat(
                when (language) {
                    "de" -> "dd.MM.yyyy"
                    "ka" -> "dd.MM.yyyy"
                    "zh" -> "yyyy/MM/dd"
                    else -> "MMM d, yyyy"
                },
                when (language) {
                    "de" -> java.util.Locale.GERMAN
                    "ka" -> java.util.Locale("ka")
                    "zh" -> java.util.Locale.CHINESE
                    else -> java.util.Locale.ENGLISH
                }
            )
            format.format(date)
        } catch (e: Exception) { "" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = strengthColor.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                InfoIconButton(
                    title = tr(language, "Stärke-Wert", "სიძლიერის ქულა", "稳固度评分", "Strength Score"),
                    explanation = if (language == "de") "Die gewichtete Stärke dieser Gewohnheit (0-100) basierend auf den letzten 30 Tagen. Neuere Einträge werden etwas stärker gewichtet, sodass sich dein Score schneller erholen kann. Ausstehende Aufgaben für den heutigen Tag reduzieren den Score nicht, solange sie unmarkiert bleiben." else "The strength score of this habit (0-100) based on the last 30 days. Recent entries carry slightly more weight, allowing your score to recover faster. Pending tasks for today do not reduce your score as long as they remain unmarked.",
                    onClick = onInfoClick
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circular Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(84.dp)
                        .drawWithCache {
                            val strokeWidth = 8.dp.toPx()
                            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            onDrawBehind {
                                drawArc(
                                    color = ProgressTrack,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = stroke
                                )
                                drawArc(
                                    color = strengthColor,
                                    startAngle = -90f,
                                    sweepAngle = sweepAngleVal,
                                    useCenter = false,
                                    style = stroke
                                )
                            }
                        }
                ) {
                    Text(
                        text = strengthText,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        color = TextPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = tr(language, "GEWOHNHEITS-STÄRKE", "ჩვევის სიძლიერე", "习惯稳固度", "HABIT STRENGTH"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = strengthLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = strengthColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    )
                    if (startDateStr.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(language, "Aktiv seit: $startDateStr", "აქტიურია: $startDateStr-დან", "自 $startDateStr 起激活", "Active since: $startDateStr"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
