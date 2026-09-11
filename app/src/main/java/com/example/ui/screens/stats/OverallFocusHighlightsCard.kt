package com.example.ui.screens.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.Habit
import com.example.data.HabitStatModel
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.InfoIconButton
import com.example.ui.dialogs.animatedGlowingBorder
import com.example.ui.theme.*

@Composable
fun OverallFocusHighlightsCard(
    habitsWithStats: List<HabitStatModel>,
    language: String,
    onExplanationClick: (String, String) -> Unit
) {
    val bottomHabits = remember(habitsWithStats) {
        val lowStrength = habitsWithStats.filter { it.strength < 60 }.sortedBy { it.strength }
        if (lowStrength.isNotEmpty()) {
            lowStrength.take(2)
        } else if (habitsWithStats.size > 2) {
            habitsWithStats.filter { it.strength < 100 }.sortedBy { it.strength }.take(2)
        } else {
            emptyList()
        }
    }
    val topHabits = remember(habitsWithStats, bottomHabits) {
        val bottomIds = bottomHabits.map { it.habit.id }.toSet()
        habitsWithStats
            .filter { it.habit.id !in bottomIds }
            .sortedByDescending { it.strength }
            .take(2)
    }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // "Läuft super" card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(SuccessGreen.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Great performance",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(language, "Läuft super", "მშვენივრად აკეთებს", "表现出色", "Doing great"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                InfoIconButton(
                                    title = tr(language, "Läuft super", "მშვენივრად აკეთებს", "表现出色", "Doing great"),
                                    explanation = if (language == "de") "Deine 2 Gewohnheiten mit der höchsten historischen Erfolgsquote (Stärke)." else if (language == "ka") "თქვენი 2 ჩვევა უმაღლესი ისტორიული დასრულების სიძლიერით." else "Your 2 habits with the highest historical completion strength.",
                                    onClick = { t, e -> onExplanationClick(t, e) }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (topHabits.isEmpty()) {
                                Text(
                                    text = tr(language, "Keine Daten", "მონაცემები არ არის", "暂无数据", "No data"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    topHabits.forEach { stat ->
                                        val habitColor = remember(stat.habit.color) { HabitIconMapping.getColor(stat.habit.color) }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(habitColor.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, habitColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = HabitIconMapping.getIcon(stat.habit.icon),
                                                    contentDescription = null,
                                                    tint = habitColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stat.habit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${stat.strength}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "Braucht Fokus" card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(ErrorRed.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Needs focus",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(language, "Braucht Fokus", "ფოკუსირება სჭირდება", "需要关注", "Needs focus"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                InfoIconButton(
                                    title = tr(language, "Braucht Fokus", "ფოკუსირება სჭირდება", "需要关注", "Needs focus"),
                                    explanation = if (language == "de") "Deine Gewohnheiten unter 100%, die noch Aufmerksamkeit benötigen." else if (language == "ka") "თქვენი ჩვევები 100%-ზე ნაკლებია, რომლებსაც ყურადღება სჭირდებათ." else "Your habits below 100% completion that need attention.",
                                    onClick = { t, e -> onExplanationClick(t, e) }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (bottomHabits.isEmpty()) {
                                Text(
                                    text = tr(language, "Alles im grünen Bereich! 🎉", "ყველა გზაზეა! 🎉", "一切尽在掌握！🎉", "All on track! 🎉"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    bottomHabits.forEach { stat ->
                                        val habitColor = remember(stat.habit.color) { HabitIconMapping.getColor(stat.habit.color) }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(habitColor.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, habitColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = HabitIconMapping.getIcon(stat.habit.icon),
                                                    contentDescription = null,
                                                    tint = habitColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stat.habit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${stat.strength}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ErrorRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
}
