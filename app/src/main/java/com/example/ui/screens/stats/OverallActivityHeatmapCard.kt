package com.example.ui.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarGridCellData
import com.example.data.DailyNote
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.components.InfoIconButton
import com.example.ui.dialogs.animatedGlowingBorder
import com.example.ui.theme.*
import java.time.LocalDate

@Composable
fun OverallActivityHeatmapCard(
    language: String,
    viewModel: HabitsViewModel,
    heatmapYearGridData: List<List<CalendarGridCellData>>,
    heatmapYearMonthLabels: List<Pair<Int, String>>,
    activeCell: CalendarGridCellData?,
    formattedDate: String,
    firstHabitDateStr: String?,
    allDailyNotes: List<DailyNote>,
    accentColor: Color = PrimaryViolet,
    onNavigateToToday: () -> Unit,
    onExplanationClick: (String, String) -> Unit
) {
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
                            .padding(16.dp)
                    ) {
                        // Header info row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(accentColor.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Calendar",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(language, "AKTIVITÄTS-HEATMAP", "აქტივობა სითბოს რუკა", "打卡热力图", "ACTIVITY HEATMAP"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                InfoIconButton(
                                    title = tr(language, "Aktivitäts-Heatmap", "აქტივობის სითბოს რუკა", "打卡热力图", "Activity Heatmap"),
                                    explanation = if (language == "de") "Zeigt deinen täglichen Gewohnheitsfortschritt für den ausgewählten Zeitraum, ähnlich wie das GitHub-Beitragssystem. Dunklere grüne Felder stehen für eine höhere Anzahl an abgeschlossenen Gewohnheiten an dem Tag." else if (language == "ka") "აჩვენებს თქვენი ყოველდღიური ჩვევების დასრულების პროგრესს არჩეული დროის განმავლობაში, სტილიზებული GitHub-ის წვლილის დაფის მსგავსად. მუქი მწვანე კვადრატები წარმოადგენს იმ დღეს დასრულებული ჩვევების უფრო მაღალ თანაფარდობას." else "Shows your daily habit completion progress for the selected timeframe, styled like GitHub's contribution board. Darker green squares represent a higher ratio of completed habits on that day.",
                                    onClick = { t, e -> onExplanationClick(t, e) }
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr(language, "Interaktiv", "ინტერაქტიული", "交互式", "Interactive"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // --- YEAR VIEW (Horizontal Scrollable 24-Weeks GitHub board, scaled up!) ---
                        val yearGridData = heatmapYearGridData
                        val yearMonthLabels = heatmapYearMonthLabels

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Top
                        ) {
                            // 1. Fixed Weekday Labels Column on the left
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 22.dp, end = 8.dp)
                            ) {
                                val weekdays = if (language == "de") {
                                    listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                                } else {
                                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                }
                                weekdays.forEach { dayLabel ->
                                    Box(
                                        modifier = Modifier.height(18.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = dayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 2. Horizontally scrollable Content containing Month labels and Week columns
                            val scrollState = rememberScrollState()
                            
                            LaunchedEffect(yearGridData) {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(scrollState)
                            ) {
                                // Month Labels Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    for (index in 0 until 24) {
                                        val label = yearMonthLabels.firstOrNull { it.first == index }
                                        Box(
                                            modifier = Modifier.width(18.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (label != null) {
                                                Text(
                                                    text = label.second,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Weeks Columns Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    yearGridData.forEach { weekDays ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            weekDays.forEach { cell ->
                                                val firstHabitDate = firstHabitDateStr
                                                val isBeforeFirstHabit = firstHabitDate != null && cell.dateStr < firstHabitDate
                                                val ratio = if (cell.total > 0) cell.completed.toFloat() / cell.total.toFloat() else -1f
                                                val bgColor = when {
                                                    cell.isOutOfRange || isBeforeFirstHabit -> Color.Transparent
                                                    cell.isFuture -> AppCard
                                                    ratio < 0f -> ProgressTrack
                                                    ratio == 0f -> ProgressTrack
                                                    ratio <= 0.25f -> accentColor.copy(alpha = 0.15f)
                                                    ratio <= 0.5f -> accentColor.copy(alpha = 0.40f)
                                                    ratio <= 0.75f -> accentColor.copy(alpha = 0.70f)
                                                    else -> accentColor
                                                }
                                                
                                                val isSelectedSquare = activeCell?.dateStr == cell.dateStr && !isBeforeFirstHabit
                                                val isTodayBorder = cell.isToday && !isBeforeFirstHabit
                                                val hasNote = remember(allDailyNotes, cell.dateStr) {
                                                    !isBeforeFirstHabit && allDailyNotes.any { it.date == cell.dateStr && it.content.isNotBlank() }
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .background(bgColor, RoundedCornerShape(4.dp))
                                                        .then(
                                                            if (isSelectedSquare) {
                                                                Modifier.border(2.dp, accentColor, RoundedCornerShape(4.dp))
                                                            } else if (isTodayBorder) {
                                                                Modifier.border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                            } else Modifier
                                                        )
                                                        .clickable(enabled = !cell.isOutOfRange && !isBeforeFirstHabit) {
                                                            viewModel.selectHeatmapCell(cell)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (hasNote && !cell.isOutOfRange) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(top = 2.dp, end = 2.dp)
                                                                .size(4.dp)
                                                                .background(accentColor, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Heatmap Horizontal Legend (GitHub-style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(language, "Weniger ", "ნაკლები", "少 ", "Less "),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            
                            listOf(
                                ProgressTrack,
                                accentColor.copy(alpha = 0.15f),
                                accentColor.copy(alpha = 0.40f),
                                accentColor.copy(alpha = 0.70f),
                                accentColor
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(12.dp)
                                        .background(color, RoundedCornerShape(3.dp))
                                )
                            }
                            
                            Text(
                                text = tr(language, " Mehr", "მეტი", " 更多", " More"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Tapped cell details container
                        val cell = activeCell
                        if (cell != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppCard, RoundedCornerShape(12.dp))
                                    .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val statusText = when {
                                        cell.isFuture -> {
                                            tr(language, "Zukünftiger Tag", "მომავლის დღე", "未来日期", "Future day")
                                        }
                                        cell.total == 0 -> {
                                            tr(language, "Keine Gewohnheiten an diesem Tag", "არანაირი აქტიური ჩვევა ამ დღეს", "这一天没有活跃习惯", "No habits active on this day")
                                        }
                                        else -> {
                                            val pct = (cell.completed.toFloat() / cell.total.toFloat() * 100).toInt()
                                            if (language == "de") "${cell.completed} von ${cell.total} Gewohnheiten abgeschlossen ($pct%)" else {
                                                if (language == "ka") "${cell.completed} / ${cell.total} ჩვევა შესრულებულია ($pct%)" else "${cell.completed} of ${cell.total} habits completed ($pct%)"
                                            }
                                        }
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (cell.total > 0 && cell.completed == cell.total) SuccessGreen else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (!cell.isFuture) {
                                    val cellInteractionSource = remember { MutableInteractionSource() }
                                    val cellIsPressed by cellInteractionSource.collectIsPressedAsState()
                                    val cellArrowOffsetX by animateDpAsState(
                                        targetValue = if (cellIsPressed) 6.dp else 0.dp,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "cell_arrow_offset"
                                    )

                                    TextButton(
                                        onClick = {
                                            viewModel.selectDateAndSyncWeek(cell.dateStr)
                                            onNavigateToToday()
                                        },
                                        interactionSource = cellInteractionSource,
                                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                                    ) {
                                        Text(
                                            text = tr(language, "Anzeigen", "ხედი", "查看", "View"),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Go to date",
                                            modifier = Modifier
                                                .offset(x = cellArrowOffsetX)
                                                .size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
}
