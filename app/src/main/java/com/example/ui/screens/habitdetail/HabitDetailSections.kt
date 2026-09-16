package com.example.ui.screens.habitdetail
import com.frequent.habits.R

import androidx.compose.ui.res.painterResource

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.tr
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun WeekdayFrequencySection(
    weekdayStats: List<Triple<Int, Int, Int>>,
    gridData: List<List<Pair<java.time.LocalDate, String>>>,
    weeksWithMonthLabels: List<String>,
    language: String,
    onInfoClick: (String, String) -> Unit,
    allDailyNotes: List<com.example.data.DailyNote> = emptyList()
) {
    val daysAbbr = if (language == "de") {
        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(PrimaryViolet.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bar_chart),
                        contentDescription = "Frequency",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "WOCHENTAGS-FREQUENZ", "კვირის სიხშირე", "星期频率统计", "WEEKDAY FREQUENCY"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Wochentags-Frequenz", "სამუშაო დღეების სიხშირე", "星期频率统计", "Weekday Frequency"),
                    explanation = if (language == "de") "Zeigt, an welchen Wochentagen du diese Gewohnheit historisch am häufigsten abgeschlossen hast. Je heller der Kreis, desto höher die Erfolgsquote." else "Shows on which weekdays you have historically completed this habit most often. The brighter the circle, the higher your completion rate.",
                    onClick = onInfoClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Horizontal circles for each of the 7 weekdays
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekdayStats.forEach { (index, completed, pct) ->
                    val dayName = daysAbbr[index]
                    val circleBgColor = when {
                        pct == 0 -> ProgressTrack
                        pct <= 25 -> PrimaryViolet.copy(alpha = 0.15f)
                        pct <= 50 -> PrimaryViolet.copy(alpha = 0.40f)
                        pct <= 75 -> PrimaryViolet.copy(alpha = 0.70f)
                        else -> PrimaryViolet
                    }
                    val textColor = if (pct > 50) Color.White else TextPrimary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(circleBgColor)
                                .border(1.dp, if (pct == 0) AppBorder else PrimaryViolet.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName.take(2),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Git-style Weekly Heatmap (Last 15 Weeks)
            Text(
                text = tr(language, "AKTIVITÄT (LETZTE 15 WOCHEN)", "აქტივობა (ბოლო 15 კვირა)", "近期活动（最近 15 周）", "ACTIVITY (LAST 15 WEEKS)"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // 7 weekday rows with background line and evenly spaced circles
                for (dayIndex in 0 until 7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Subtle background line for track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(AppBorder.copy(alpha = 0.4f))
                            )

                            // Row of circles for each week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                gridData.forEach { weekDays ->
                                    val cell = weekDays[dayIndex]
                                    val dateStr = cell.first.toString()
                                    val status = cell.second
                                    val hasNote = remember(allDailyNotes, dateStr) {
                                        allDailyNotes.any { it.date == dateStr && it.content.isNotBlank() }
                                    }
                                    
                                    val isSuccess = status == "SUCCESS"
                                    val size = if (isSuccess) 14.dp else 4.dp
                                    val color = when (status) {
                                        "SUCCESS" -> PrimaryViolet
                                        "FAILED" -> ProgressTrack
                                        else -> ProgressTrack.copy(alpha = 0.3f)
                                    }

                                    Box(
                                        modifier = Modifier.size(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(size)
                                                .background(color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Weekday label on the right side
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = daysAbbr[dayIndex].take(2),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Month labels row aligned beautifully with the week columns above
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        gridData.forEachIndexed { index, _ ->
                            val label = weeksWithMonthLabels[index]
                            Box(
                                modifier = Modifier.width(14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (label.isNotEmpty()) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(40.dp)) // Matches 12.dp spacer + 28.dp label
                }
            }
        }
    }
}

@Composable
fun HabitTargetSection(
    state: HabitDetailUiState?,
    language: String,
    onInfoClick: (String, String) -> Unit = { _, _ -> }
) {
    val stats = state?.targetStats ?: return
    val habit = state.habit

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(PrimaryViolet.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_target),
                        contentDescription = "Target",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "ZIELERFÜLLUNG (ZIEL)", "მიზნობრივი შესრულება", "目标达成表现", "TARGET PERFORMANCE"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Zielerfüllung", "მიზნობრივი შესრულება", "目标达成表现", "Target Performance"),
                    explanation = if (language == "de") "Zeigt deinen Fortschritt im Verhältnis zum festgelegten Zielwert für heute, diese Woche, diesen Monat und dieses Jahr." else "Shows your progress relative to your set target value for today, this week, this month, and this year.",
                    onClick = { t, e -> onInfoClick(t, e) }
                )
            }

            // Today, Week, Month, Year rows (omit Quarter)
            val periodLabels = if (language == "de") {
                listOf("Heute", "Woche", "Monat", "Jahr")
            } else {
                listOf("Today", "Week", "Month", "Year")
            }

            val periodStatsList = listOf(
                stats.today,
                stats.week,
                stats.month,
                stats.year
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                periodLabels.zip(periodStatsList).forEach { (label, periodStat) ->
                    TargetProgressRow(
                        label = label,
                        actual = periodStat.actualValue,
                        target = periodStat.targetValue,
                        isNumerical = periodStat.isNumerical,
                        unit = habit.unit,
                        language = language
                    )
                }
            }
        }
    }
}

@Composable
fun TargetProgressRow(
    label: String,
    actual: Float,
    target: Float,
    isNumerical: Boolean,
    unit: String,
    language: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val progressFraction = remember(actual, target) {
            if (target > 0f) (actual / target).coerceIn(0f, 1f) else 0f
        }
        var hasAnimatedTargetRow by rememberSaveable(actual, target) { mutableStateOf(false) }
        val animFraction = remember { androidx.compose.animation.core.Animatable(if (hasAnimatedTargetRow) progressFraction else 0f) }

        LaunchedEffect(progressFraction) {
            if (!hasAnimatedTargetRow) {
                animFraction.snapTo(0f)
                animFraction.animateTo(
                    targetValue = progressFraction,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
                hasAnimatedTargetRow = true
            } else {
                animFraction.animateTo(
                    targetValue = progressFraction,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        }
        val animatedProgressFraction = animFraction.value
        val hasOverachieved = remember(actual, target) {
            target > 0f && actual > target
        }

        val formattedActual = remember(actual) {
            if (actual % 1f == 0f) actual.toInt().toString() else String.format(Locale.US, "%.1f", actual)
        }
        val formattedTarget = remember(target) {
            if (target % 1f == 0f) target.toInt().toString() else String.format(Locale.US, "%.1f", target)
        }
        val unitSuffix = remember(isNumerical, unit) {
            if (isNumerical && unit.isNotEmpty()) " $unit" else ""
        }

        // Row 1: Period label and Value text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period label (e.g. Heute, Woche, etc.)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            // Value text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$formattedActual$unitSuffix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (target > 0f) {
                    val formattedDiff = remember(actual, target) {
                        val diff = actual - target
                        if (diff % 1f == 0f) diff.toInt().toString() else String.format(Locale.US, "%.1f", diff)
                    }
                    val targetLabel = remember(language, formattedTarget, unitSuffix) {
                        tr(language, " / Ziel: $formattedTarget$unitSuffix", "/ სამიზნე: $formattedTarget $unitSuffix", " / 目标：$formattedTarget$unitSuffix", " / Target: $formattedTarget$unitSuffix")
                    }

                    Text(
                        text = buildAnnotatedString {
                            append(targetLabel)
                            if (hasOverachieved) {
                                withStyle(style = SpanStyle(color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)) {
                                    append(" (+$formattedDiff$unitSuffix)")
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Text(
                        text = tr(language, " (Kein Ziel)", "(სამიზნე არ არის)", "（无目标）", " (No Target)"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(AppBorder)
        ) {
            // Progress bar (colored portion)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (animatedProgressFraction > 0f) animatedProgressFraction else 0.001f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (hasOverachieved) {
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryViolet, SuccessGreen)
                            )
                        } else {
                            SolidColor(PrimaryViolet)
                        }
                    )
            )
        }
    }
}

