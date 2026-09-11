package com.example.ui.screens.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasteredGoalCard(
    habit: Habit,
    language: String,
    onDelete: (Habit) -> Unit,
    onViewStats: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = HabitIconMapping.getColor(habit.color)

    val dateRangeStr = remember(habit.createdAt, habit.completedAt, habit.startDate) {
        val sdf = SimpleDateFormat("dd. MMM yyyy", when (language) {
            "de" -> Locale.GERMAN
            else -> Locale.US
        })
        val startMs = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val endMs = habit.completedAt ?: habit.createdAt
        val startFormatted = sdf.format(Date(startMs))
        val endFormatted = sdf.format(Date(endMs))
        if (startFormatted == endFormatted) {
            endFormatted
        } else {
            "$startFormatted – $endFormatted"
        }
    }

    val (diffLabel, diffColor, diffIcon) = when (habit.difficulty) {
        "EASY" -> Triple(tr(language, "Leicht", "იოლი", "简单", "Easy"), androidx.compose.ui.graphics.Color(0xFF2ECC71), "🥉")
        "HARD" -> Triple(tr(language, "Schwer", "რთული", "困难", "Hard"), androidx.compose.ui.graphics.Color(0xFFFFB300), "🥇")
        "ULTRA" -> Triple(tr(language, "Extrem", "ულტრა", "极难", "Ultra"), androidx.compose.ui.graphics.Color(0xFFE040FB), "👑")
        else -> Triple(tr(language, "Mittel", "საშუალო", "中等", "Medium"), androidx.compose.ui.graphics.Color(0xFF00E5FF), "🥈")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onViewStats(habit) },
                onLongClick = { onDelete(habit) }
            )
            .testTag("mastered_goal_card_${habit.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Habit Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(habitColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconMapping.getIcon(habit.icon),
                        contentDescription = habit.name,
                        tint = habitColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title, Date and Difficulty
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Date Range
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = dateRangeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Difficulty Tag
                        if (habit.isFinishable) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = diffIcon, fontSize = 11.sp)
                                Text(
                                    text = diffLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = diffColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right arrow indicating navigation to stats
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = tr(language, "Statistik anzeigen", "სტატისტიკის ნახვა", "查看统计", "View statistics"),
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Optional reflection note
            if (habit.completionNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppBorder.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "💬", fontSize = 12.sp)
                        Text(
                            text = habit.completionNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
