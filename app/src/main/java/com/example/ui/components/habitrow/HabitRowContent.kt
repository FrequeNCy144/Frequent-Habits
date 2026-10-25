package com.example.ui.components.habitrow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.tr
import com.example.getLocalizedUnit
import com.example.ui.HabitIconMapping
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun HabitRowContent(
    habit: Habit,
    currentValue: Float,
    isCompleted: Boolean,
    isFailed: Boolean,
    isPaused: Boolean,
    isMinimalViable: Boolean,
    isWeeklyTargetReached: Boolean,
    weeklyLoggedCount: Int,
    weeklyTargetCount: Int,
    streak: Int,
    isNumerical: Boolean,
    habitColor: Color,
    checkboxScale: Float,
    isStackAuraActive: Boolean,
    stackTargetEffectProgress: Float,
    anchorCompleted: Boolean,
    totalAchievedValue: Float,
    language: String,
    modifier: Modifier = Modifier,
    isMonthlyTargetReached: Boolean = false,
    monthlyLoggedCount: Int = 0,
    monthlyTargetCount: Int = 0,
    isWeeklyNumeric: Boolean = false,
    weeklySum: Float = 0f,
    isMonthlyNumeric: Boolean = false,
    monthlySum: Float = 0f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
                if (isWeeklyNumeric) {
                    val formattedSum = if (weeklySum % 1f == 0f) weeklySum.toInt().toString() else weeklySum.toString()
                    val targetVal = habit.targetValue
                    val formattedTarget = if (targetVal % 1f == 0f) targetVal.toInt().toString() else targetVal.toString()
                    val unitStr = if (habit.unit.isNotBlank()) " ${getLocalizedUnit(habit.unit, language)}" else ""
                    val limitPrefix = if (habit.isNegative) tr(language, "max. ", "მაქს. ", "最多 ", "max ") else ""
                    val todayPart = if (currentValue > 0f) {
                        val formattedToday = if (currentValue % 1f == 0f) currentValue.toInt().toString() else currentValue.toString()
                        " (" + tr(language, "Heute: +$formattedToday", "დღეს: +$formattedToday", "今日: +$formattedToday", "Today: +$formattedToday") + ")"
                    } else ""
                    "$formattedSum / $limitPrefix$formattedTarget$unitStr " + tr(language, "in 7 Tagen", "7 დღეში", "7 天内", "in 7 days") + todayPart
                } else {
                    val todayDone = if (currentValue > 0f) " (" + tr(language, "Heute erledigt", "დღეს შესრულდა", "今日已完成", "Done today") + ")" else ""
                    "$weeklyLoggedCount/$weeklyTargetCount " + tr(language, "in 7 Tagen", "7 დღეში", "7 天内", "in 7 days") + todayDone
                }
            } else if (habit.frequency == "TIMES_MONTHLY") {
                if (isMonthlyNumeric) {
                    val formattedSum = if (monthlySum % 1f == 0f) monthlySum.toInt().toString() else monthlySum.toString()
                    val targetVal = habit.targetValue
                    val formattedTarget = if (targetVal % 1f == 0f) targetVal.toInt().toString() else targetVal.toString()
                    val unitStr = if (habit.unit.isNotBlank()) " ${getLocalizedUnit(habit.unit, language)}" else ""
                    val limitPrefix = if (habit.isNegative) tr(language, "max. ", "მაქს. ", "最多 ", "max ") else ""
                    val todayPart = if (currentValue > 0f) {
                        val formattedToday = if (currentValue % 1f == 0f) currentValue.toInt().toString() else currentValue.toString()
                        " (" + tr(language, "Heute: +$formattedToday", "დღეს: +$formattedToday", "今日: +$formattedToday", "Today: +$formattedToday") + ")"
                    } else ""
                    "$formattedSum / $limitPrefix$formattedTarget$unitStr " + tr(language, "in 30 Tagen", "30 დღეში", "30 天内", "in 30 days") + todayPart
                } else {
                    val todayDone = if (currentValue > 0f) " (" + tr(language, "Heute erledigt", "დღეს შესრულდა", "今日已完成", "Done today") + ")" else ""
                    "$monthlyLoggedCount/$monthlyTargetCount " + tr(language, "in 30 Tagen", "30 დღეში", "30 天内", "in 30 days") + todayDone
                }
            } else if (isNumerical) {
                val displayValue = if (isFailed && !habit.isNegative) 0f else currentValue
                val formattedVal = if (displayValue % 1f == 0f) displayValue.toInt().toString() else displayValue.toString()
                val targetVal = if (isMinimalViable && habit.minimalViableValue != null) habit.minimalViableValue else habit.targetValue
                val formattedTarget = if (targetVal % 1f == 0f) targetVal.toInt().toString() else targetVal.toString()
                val unitStr = if (habit.unit.isNotBlank()) " ${getLocalizedUnit(habit.unit, language)}" else ""
                val suffix = if (isMinimalViable) " (Min)" else ""
                val limitPrefix = if (habit.isNegative) tr(language, "max. ", "მაქს. ", "最多 ", "max ") else ""
                "$formattedVal / $limitPrefix$formattedTarget$unitStr$suffix"
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fire),
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
                            (habit.frequency == "TIMES_WEEKLY" && isWeeklyTargetReached) || (habit.frequency == "TIMES_MONTHLY" && isMonthlyTargetReached) -> SuccessGreen
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
                    val unitStr = if (habit.unit.isNotBlank()) " ${getLocalizedUnit(habit.unit, language)}" else if (!isNumerical) " " + tr(language, "Tage", "დღე", "天", "days") else ""

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .width(52.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = HabitYellow,
                        trackColor = ProgressTrack
                    )
                    val labelText = if (habit.challengeMode == "STREAK") {
                        "🔥 $formattedCurrent/$formattedTarget$unitStr ($pct%)"
                    } else if (habit.challengeMode == "SUCCESS_RATE") {
                        "$formattedCurrent/$formattedTarget$unitStr (min. ${habit.challengeSuccessRate}%)"
                    } else {
                        "$formattedCurrent/$formattedTarget$unitStr ($pct%)"
                    }
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        color = HabitYellow.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
