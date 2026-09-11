package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.tr
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit




@Composable
fun HabitVolumeProgressionCard(
    habit: Habit,
    logs: List<HabitLog>,
    language: String,
    accentColor: Color = PrimaryViolet,
    onInfoClick: (String, String) -> Unit
) {
    var selectedTimeframeIndex by rememberSaveable(habit.id) { mutableIntStateOf(0) }

    val volumePoints = remember(habit, logs, selectedTimeframeIndex) {
        calculateHabitVolumePoints(habit, logs, selectedTimeframeIndex)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Volume",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "ABSCHLUSS-VOLUMEN", "დასრულების მოცულობა", "完成量统计", "COMPLETION VOLUME"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Abschluss-Volumen", "დასრულების მოცულობა", "完成量统计", "Completion Volume"),
                    explanation = if (language == "de") "Zeigt die absoluten erfolgreichen Gewohnheitsabschlüsse pro Periode." else "Shows absolute completed habit goals per selected period.",
                    onClick = onInfoClick
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

            Spacer(modifier = Modifier.height(14.dp))

            TimeframeSelectorPills(
                selectedTimeframeIndex = selectedTimeframeIndex,
                onTimeframeSelected = { selectedTimeframeIndex = it },
                language = language,
                customLabels = if (language == "de") {
                    listOf("Wöchentlich", "Monatlich", "Jährlich")
                } else {
                    listOf("Weekly", "Monthly", "Yearly")
                },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            CompletionsBarChart(points = volumePoints, habitColor = accentColor)
        }
    }
}


@Composable
fun CompletionsBarChart(
    points: List<TrendPoint>,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val maxCount = remember(points) { (points.maxOfOrNull { it.completions } ?: 1).coerceAtLeast(1) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { pt ->
                val ratio = pt.completions.toFloat() / maxCount.toFloat()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (pt.completions > 0) {
                        Text(
                            text = "${pt.completions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (pt.completions > 0) habitColor else Color.White.copy(alpha = 0.06f)
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
fun OverallVolumeProgressionCard(
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    language: String,
    accentColor: Color = PrimaryViolet,
    onInfoClick: (String, String) -> Unit
) {
    var selectedTimeframeIndex by rememberSaveable { mutableIntStateOf(0) }

    val volumePoints = remember(allHabits, allLogs, selectedTimeframeIndex) {
        calculateOverallVolumePoints(allHabits, allLogs, selectedTimeframeIndex)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Volume",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "GESAMT-VOLUMEN", "მთლიანი მოცულობის პროგრესი", "累计完成量走势", "TOTAL VOLUME PROGRESSION"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Gesamt-Volumen", "მთლიანი მოცულობის პროგრესი", "累计完成量走势", "Total Volume Progression"),
                    explanation = if (language == "de") "Zeigt die absolute Anzahl aller erfolgreich absolvierten Abschlüsse im ausgewählten Zeitraum." else "Shows total completed habit goals recorded across all habits per selected period.",
                    onClick = onInfoClick
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

            Spacer(modifier = Modifier.height(14.dp))

            TimeframeSelectorPills(
                selectedTimeframeIndex = selectedTimeframeIndex,
                onTimeframeSelected = { selectedTimeframeIndex = it },
                language = language,
                customLabels = if (language == "de") {
                    listOf("Wöchentlich", "Monatlich", "Jährlich")
                } else {
                    listOf("Weekly", "Monthly", "Yearly")
                },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            CompletionsBarChart(
                points = volumePoints,
                habitColor = accentColor
            )
        }
    }
}