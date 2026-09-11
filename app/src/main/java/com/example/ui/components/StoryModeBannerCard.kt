package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.isLogCompleted
import com.example.ui.dialogs.ExplanationDialog
import com.example.ui.theme.*
import com.example.tr

@Composable
fun StoryModeBannerCard(
    gameMode: String,
    unlockedSlots: Int,
    currentHabitCount: Int,
    allLogs: List<HabitLog>,
    allHabits: List<Habit>,
    language: String,
    onOpenSettings: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onUnlockNextSlot: () -> Unit
) {
    if (gameMode == "FREE") return

    var showInfoDialog by remember { mutableStateOf(false) }

    // STORY MODE CARD
    val activeHabits = remember(allHabits) { allHabits.filter { !it.isArchived } }
    val uniqueLoggedDays = remember(allLogs) {
        allLogs.map { it.date }.distinct().size
    }

    val currentCycleDays = uniqueLoggedDays.coerceAtMost(30)

    val overallScore = remember(allLogs, activeHabits) {
        if (allLogs.isEmpty() || activeHabits.isEmpty()) 100f
        else {
            var completedCount = 0
            var totalCount = 0
            for (log in allLogs) {
                val matchingHabit = activeHabits.find { it.id == log.habitId } ?: continue
                totalCount++
                if (isLogCompleted(matchingHabit, log)) {
                    completedCount++
                }
            }
            if (totalCount > 0) (completedCount.toFloat() / totalCount) * 100f else 100f
        }
    }

    val isSlotUnlockable = currentCycleDays >= 30 && overallScore >= 85.0f

    if (showInfoDialog) {
        ExplanationDialog(
            title = tr(language, "Story-Modus", "Story Mode"),
            explanation = tr(
                language,
                "🎯 Was ist der Story-Modus?\n" +
                "Der Story-Modus schützt dich vor Überforderung: Du startest mit begrenzten Gewohnheits-Slots und erweiterst deine Kapazität schrittweise, während du echte Beständigkeit aufbaust.\n\n" +
                "🔓 Neuen Slot freischalten:\n" +
                "Absolviere 30 aktive Tracking-Tage mit einer durchschnittlichen Erfolgsquote von mindestens 85%. Sobald du dieses Ziel meisterst, schaltest du dauerhaft einen weiteren Slot (+1) frei.\n\n" +
                "🎮 Freies Spiel:\n" +
                "Du möchtest ohne Slot-Begrenzung sofort beliebig viele Gewohnheiten erstellen? In den Einstellungen unter 'Erscheinungsbild' kannst du jederzeit nahtlos in das Freie Spiel wechseln.",
                "🎯 What is Story Mode?\n" +
                "Story Mode prevents habit burnout: You begin with limited slots and expand step-by-step as you build lasting consistency.\n\n" +
                "🔓 Unlocking New Slots:\n" +
                "Complete 30 active tracking days with a success rate of at least 85%. Once achieved, you permanently unlock another habit slot (+1).\n\n" +
                "🎮 Free Play:\n" +
                "Want to track habits without slot limits? You can switch anytime in Settings under 'Appearance' to Free Play."
            ),
            onDismiss = { showInfoDialog = false }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        border = BorderStroke(1.dp, if (isSlotUnlockable) SuccessGreen else PrimaryViolet.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isSlotUnlockable) SuccessGreen.copy(alpha = 0.15f) else PrimaryViolet.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSlotUnlockable) Icons.Default.EmojiEvents else Icons.Default.VideogameAsset,
                            contentDescription = "Story Mode",
                            tint = if (isSlotUnlockable) SuccessGreen else PrimaryViolet,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = tr(language, "Story-Modus", "Story Mode"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { showInfoDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Story Mode Info",
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    color = if (currentHabitCount >= unlockedSlots && !isSlotUnlockable) AppBorder else PrimaryViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${tr(language, "Slots", "Slots")}: $currentHabitCount / $unlockedSlots",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (currentHabitCount >= unlockedSlots && !isSlotUnlockable) TextSecondary else PrimaryViolet,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isSlotUnlockable) {
                Text(
                    text = tr(
                        language,
                        "🎉 30 Tage mit ≥ 85% Erfolgsquote gemeistert! Schalte jetzt deinen nächsten Slot frei.",
                        "🎉 30 days with ≥ 85% success rate completed! Unlock your next slot now."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onUnlockNextSlot()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(language, "Nächsten Slot freischalten (+1 Slot)", "Unlock Next Slot (+1 Slot)"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tr(language, "Fortschritt", "Progress")}: $currentCycleDays / 30 ${tr(language, "Tage", "days")}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                    Text(
                        text = "Score: ${overallScore.toInt()}% (${tr(language, "Ziel", "Target")}: ≥85%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (overallScore >= 85f) SuccessGreen else HabitOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(ProgressTrack, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (currentCycleDays / 30.0f).coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(
                                if (overallScore >= 85f) PrimaryViolet else HabitOrange,
                                CircleShape
                            )
                    )
                }

                if (currentHabitCount < unlockedSlots) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = tr(
                                language,
                                "Freier Slot verfügbar – neue Gewohnheit kann gestartet werden!",
                                "Free slot available – ready to start a new habit!"
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = SuccessGreen
                        )
                    }
                }
            }
        }
    }
}
