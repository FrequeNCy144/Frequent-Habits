package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.theme.*

@Composable
fun MasterGoalDialog(
    habit: Habit,
    totalAchievedValue: Float,
    rewardTitle: String? = null,
    language: String,
    onDismiss: () -> Unit,
    onConfirmMastered: (reflectionNote: String, claimReward: Boolean) -> Unit
) {
    val habitColor = HabitIconMapping.getColor(habit.color)
    val isNumerical = habit.type == "NUMBER" || habit.type == "NUMERICAL" || (habit.targetValue > 1f && habit.type != "BINARY")
    val formattedAchieved = if (totalAchievedValue % 1f == 0f) totalAchievedValue.toInt().toString() else totalAchievedValue.toString()
    val formattedTarget = if ((habit.totalTargetValue ?: 0f) % 1f == 0f) (habit.totalTargetValue?.toInt() ?: 0).toString() else (habit.totalTargetValue ?: 0f).toString()
    val unitStr = if (habit.unit.isNotBlank()) " ${habit.unit}" else if (!isNumerical) " " + tr(language, "Tage", "დღე", "天", "days") else ""

    Dialog(
        onDismissRequest = {
            if (rewardTitle.isNullOrBlank()) {
                onConfirmMastered("", false)
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = AppCard),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.5.dp, HabitYellow.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Golden Trophy Icon Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(HabitYellow.copy(alpha = 0.15f))
                        .border(2.dp, HabitYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = HabitYellow,
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(language, "Ziel gemeistert! 🏆", "მიზანი მიღწეულია! 🏆", "目标达成！🏆", "Goal Mastered! 🏆"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = habitColor,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tr(
                            language,
                            "Glückwunsch! Du hast $formattedAchieved von $formattedTarget$unitStr erreicht und dieses Ziel erfolgreich abgeschlossen.",
                            "გილოცავთ! თქვენ მიაღწიეთ $formattedAchieved / $formattedTarget$unitStr და წარმატებით დაასრულეთ ეს მიზანი.",
                            "祝贺！你已完成 $formattedAchieved / $formattedTarget$unitStr，成功达成目标！",
                            "Congratulations! You reached $formattedAchieved of $formattedTarget$unitStr and mastered this goal."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                // Reward Banner (if custom reward/milestone is configured)
                if (!rewardTitle.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryViolet.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryViolet.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🎁", fontSize = 26.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(language, "Deine Belohnung", "თქვენი ჯილდო", "你的奖励", "Your Reward"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryViolet,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rewardTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons with equal weight and unbiased styling
                if (!rewardTitle.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onConfirmMastered("", false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppBorder.copy(alpha = 0.55f),
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(
                                text = tr(language, "Klasse! 🎉", "გასაოცარია! 🎉", "太棒了！🎉", "Awesome! 🎉"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { onConfirmMastered("", true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HabitYellow,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = tr(language, "Einlösen 🎁", "გამოისყიდე 🎁", "兑换奖励 🎁", "Claim 🎁"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onConfirmMastered("", false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HabitYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = tr(language, "Abschließen 🏆", "დასრულება 🏆", "完成 🏆", "Complete 🏆"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
