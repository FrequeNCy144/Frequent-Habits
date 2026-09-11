package com.example.ui.screens.createhabit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MilestoneReward
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.components.InfoIconButton
import com.example.ui.dialogs.STANDARD_TROPHIES
import com.example.ui.theme.*

@Composable
fun HabitMilestoneRewardsCard(
    language: String,
    milestoneRewards: List<MilestoneReward>,
    onAddRewardClick: () -> Unit,
    onRemoveReward: (MilestoneReward) -> Unit,
    onShowInfoDialog: () -> Unit
) {
            // 6. CARD: Meilenstein-Belohnungen
            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = HabitOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr(language, "Meilenstein-Belohnungen", "Milestone ჯილდოები", "里程碑奖励", "Milestone Rewards"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        InfoIconButton(
                            title = tr(language, "Meilenstein-Belohnungen", "Milestone ჯილდოები", "里程碑奖励", "Milestone Rewards"),
                            explanation = if (language == "de") "Setze dir persönliche Belohnungen für das Erreichen von Meilensteinen (z. B. 30 Tage Serie). Sobald du das Ziel erreichst, wird die Belohnung freigeschaltet!" else "Set personal rewards for reaching milestones (e.g. 30-day streak). Once you reach the target, the reward unlocks!",
                            onClick = { _, _ -> onShowInfoDialog() }
                        )
                    }

                    if (milestoneRewards.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            milestoneRewards.forEachIndexed { index, reward ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ProgressTrack),
                                    border = BorderStroke(1.dp, AppBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = reward.rewardText,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val condText = when (reward.conditionType) {
                                                "STREAK" -> tr(language, "${reward.conditionValue} Tage Serie", "${reward.conditionValue} დღის სტრიქონი", "${reward.conditionValue} 天连续", "${reward.conditionValue} Day Streak")
                                                "COMPLETIONS" -> tr(language, "${reward.conditionValue} Erledigungen", "${reward.conditionValue} დასრულებები", "${reward.conditionValue} 次完成", "${reward.conditionValue} Completions")
                                                "TROPHY_COUPLED" -> {
                                                    val trophy = STANDARD_TROPHIES.find { it.id == reward.trophyId }
                                                    if (trophy != null) {
                                                        tr(language, "Trophäe: ${trophy.titleDe}", "ტროფი: ${trophy.titleKa}", "奖杯：${trophy.titleZh}", "Trophy: ${trophy.titleEn}")
                                                    } else {
                                                        reward.trophyId ?: ""
                                                    }
                                                }
                                                else -> ""
                                            }
                                            if (condText.isNotEmpty()) {
                                                Text(
                                                    text = condText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onRemoveReward(reward) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Reward",
                                                tint = FailedRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onAddRewardClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProgressTrack,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Reward Icon",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(language, "Belohnung hinzufügen", "დაამატე ჯილდო", "添加奖励", "Add Reward"),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
}
