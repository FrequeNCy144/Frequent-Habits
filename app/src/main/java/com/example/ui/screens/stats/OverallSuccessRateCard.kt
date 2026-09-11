package com.example.ui.screens.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.InfoIconButton
import com.example.ui.components.TimeframeSelectorPills
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class SuccessRateStats(
    val doneCount: Int = 0,
    val missedCount: Int = 0,
    val skippedCount: Int = 0,
    val pendingCount: Int = 0,
    val totalCount: Int = 0
)

fun calculateHabitSuccessStatsForPeriod(
    habit: Habit,
    logs: List<HabitLog>,
    periodIndex: Int // 0 = Week, 1 = Month, 2 = Year, 3 = Total
): SuccessRateStats {
    val today = java.time.LocalDate.now()
    val todayStr = today.toString()

    val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    val rawHabitStartDate = java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L)).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val habitStartDate = if (rawHabitStartDate.isBefore(today.minusYears(5))) today.minusYears(5) else rawHabitStartDate
    val startSdfStr = habitStartDate.toString()

    val startDate = when (periodIndex) {
        0 -> { // This Week
            val dayOfWeek = today.dayOfWeek.value
            val weekStart = today.minusDays((dayOfWeek - 1).toLong())
            if (weekStart.isBefore(habitStartDate)) habitStartDate else weekStart
        }
        1 -> { // This Month
            val monthStart = today.withDayOfMonth(1)
            if (monthStart.isBefore(habitStartDate)) habitStartDate else monthStart
        }
        2 -> { // This Year
            val yearStart = today.withDayOfYear(1)
            if (yearStart.isBefore(habitStartDate)) habitStartDate else yearStart
        }
        else -> habitStartDate
    }

    if (startDate.isAfter(today)) {
        return SuccessRateStats()
    }

    val habitLogs = logs.filter { it.habitId == habit.id }
    val logsByDate = habitLogs.associateBy { it.date }

    var done = 0
    var missed = 0
    var skipped = 0
    var pending = 0

    var current = startDate
    while (!current.isAfter(today)) {
        val dateStr = current.toString()
        if (isHabitActiveOnDate(habit, dateStr)) {
            val log = logsByDate[dateStr]
            val status = if (habit.frequency == "TIMES_WEEKLY") {
                if (dateStr < startSdfStr || dateStr > todayStr) "INACTIVE"
                else if (log != null && log.isPaused) "PAUSED"
                else if (log != null && log.value == -1f) "FAILED"
                else if (com.example.data.isLogCompleted(habit, log)) "SUCCESS"
                else if (dateStr == todayStr) "PENDING"
                else "INACTIVE"
            } else {
                getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
            }
            when (status) {
                "SUCCESS" -> done++
                "FAILED" -> missed++
                "PAUSED" -> skipped++
                "PENDING" -> pending++
            }
        }
        current = current.plusDays(1)
    }

    val total = done + missed + skipped + pending
    return SuccessRateStats(done, missed, skipped, pending, total)
}

fun calculateOverallSuccessStats(
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    periodIndex: Int = 0 // 0 = Week, 1 = Month, 2 = Year, 3 = Total
): SuccessRateStats {
    val activeHabits = allHabits.filter { !it.isArchived }
    if (activeHabits.isEmpty()) return SuccessRateStats()

    var totalDone = 0
    var totalMissed = 0
    var totalSkipped = 0
    var totalPending = 0

    activeHabits.forEach { habit ->
        val habitStats = calculateHabitSuccessStatsForPeriod(habit, allLogs, periodIndex)
        totalDone += habitStats.doneCount
        totalMissed += habitStats.missedCount
        totalSkipped += habitStats.skippedCount
        totalPending += habitStats.pendingCount
    }

    val total = totalDone + totalMissed + totalSkipped + totalPending
    return SuccessRateStats(totalDone, totalMissed, totalSkipped, totalPending, total)
}

@Composable
fun SuccessRateCard(
    title: String,
    doneCount: Int,
    missedCount: Int,
    skippedCount: Int,
    pendingCount: Int,
    language: String,
    infoTitle: String,
    infoExplanation: String,
    onInfoClick: (String, String) -> Unit,
    showTimeframeSelector: Boolean = false,
    selectedTimeframeIndex: Int = 0,
    accentColor: Color = PrimaryViolet,
    onTimeframeSelected: (Int) -> Unit = {}
) {
    val totalCount = doneCount + missedCount + skippedCount + pendingCount
    val donePct = if (totalCount > 0) ((doneCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt() else 0
    val missedPct = if (totalCount > 0) ((missedCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt() else 0
    val skippedPct = if (totalCount > 0) ((skippedCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt() else 0
    val pendingPct = if (totalCount > 0) ((pendingCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt() else 0

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(doneCount, missedCount, skippedCount, pendingCount, selectedTimeframeIndex) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }
    val animatedFactor = animProgress.value

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success Rate",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    InfoIconButton(
                        title = infoTitle,
                        explanation = infoExplanation,
                        onClick = onInfoClick
                    )
                }
            }

            // Timeframe Selector Row (if enabled)
            if (showTimeframeSelector) {
                Spacer(modifier = Modifier.height(14.dp))
                TimeframeSelectorPills(
                    selectedTimeframeIndex = selectedTimeframeIndex,
                    onTimeframeSelected = onTimeframeSelected,
                    language = language
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Ring Canvas + Center Content
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringTrackColor = ProgressTrack
                val doneColor = SuccessGreen
                val missedColor = HabitRed
                val skippedColor = Color(0xFFF97316)
                val pendingColor = Color(0xFFFACC15)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    if (totalCount == 0) {
                        drawArc(
                            color = ringTrackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    } else {
                        val activeValues = listOf(
                            doneCount to doneColor,
                            missedCount to missedColor,
                            skippedCount to skippedColor,
                            pendingCount to pendingColor
                        ).filter { it.first > 0 }

                        val activeCount = activeValues.size
                        val gapAngle = if (activeCount > 1) 6f else 0f
                        val totalGapAngle = activeCount * gapAngle
                        val availableAngle = (360f - totalGapAngle) * animatedFactor

                        var currentStartAngle = -90f

                        activeValues.forEach { (count, color) ->
                            val sweep = (count.toFloat() / totalCount.toFloat()) * availableAngle
                            if (sweep > 0f) {
                                drawArc(
                                    color = color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = stroke
                                )
                                currentStartAngle += sweep + gapAngle
                            }
                        }
                    }
                }

                // Center Information
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(SuccessGreen.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$donePct%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tr(language, "$doneCount von $totalCount", "$doneCount $totalCount -დან", "$doneCount / $totalCount", "$doneCount of $totalCount"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 Summary Metrics Cards at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Done
                MetricPillBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconTint = SuccessGreen,
                    bgColor = SuccessGreen.copy(alpha = 0.12f),
                    value = "$donePct%",
                    count = doneCount,
                    label = tr(language, "Erreicht", "შესრულებულია", "完成", "Done")
                )

                // 2. Missed
                MetricPillBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Close,
                    iconTint = HabitRed,
                    bgColor = HabitRed.copy(alpha = 0.12f),
                    value = "$missedPct%",
                    count = missedCount,
                    label = tr(language, "Gescheitert", "გაუშვა", "未达成", "Missed")
                )

                // 3. Skipped
                MetricPillBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FastForward,
                    iconTint = Color(0xFFF97316),
                    bgColor = Color(0xFFF97316).copy(alpha = 0.12f),
                    value = "$skippedPct%",
                    count = skippedCount,
                    label = tr(language, "Skipped", "გამოტოვებული", "已跳过", "Skipped")
                )

                // 4. Pending
                MetricPillBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    iconTint = Color(0xFFFACC15),
                    bgColor = Color(0xFFFACC15).copy(alpha = 0.12f),
                    value = "$pendingPct%",
                    count = pendingCount,
                    label = tr(language, "Ausstehend", "მომლოდინე", "待完成", "Pending")
                )
            }
        }
    }
}

@Composable
fun MetricPillBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    value: String,
    count: Int,
    label: String
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AchievementCounterCol(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}
