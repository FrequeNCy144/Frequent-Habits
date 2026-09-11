package com.example.ui.dialogs

import com.example.ui.screens.profile.AchievementBadge

import com.example.tr
import com.example.ui.components.*

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.HabitsViewModel
import com.example.ui.components.AppSegmentedButton
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsOverviewSheet(
    viewModel: HabitsViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allRewards by viewModel.allMilestoneRewards.collectAsStateWithLifecycle(initialValue = emptyList())
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle(initialValue = emptyList())
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle(initialValue = ProfileStats())
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val activeRewards = allRewards.filter { it.unlockedAt == 0L }
    val achievedRewards = allRewards.filter { it.unlockedAt > 0L }.sortedByDescending { it.unlockedAt }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openWebUrl(rawUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(rawUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun extractUrl(text: String): String? {
        if (text.isBlank()) return null
        val pattern = java.util.regex.Pattern.compile("(https?://[\\w-]+(\\.[\\w-]+)+[/#?]?.*|www\\.[\\w-]+(\\.[\\w-]+)+[/#?]?.*)", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            var url = matcher.group(0) ?: ""
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            return url
        }
        return null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { StandardSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = tr(language, "Meilenstein-Belohnungen", "ეტაპობრივი ჯილდოები", "里程碑奖励", "Milestone Rewards"),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            val unredeemedCount = achievedRewards.count { !it.isRedeemed }

            TimeframeSelectorPills(
                selectedTimeframeIndex = selectedTabIndex,
                onTimeframeSelected = { selectedTabIndex = it },
                language = language,
                customLabels = listOf(
                    tr(language, "Aktiv (${activeRewards.size})", "აქტიური (${activeRewards.size})", "进行中 (${activeRewards.size})", "Active (${activeRewards.size})"),
                    tr(language, "Freigeschaltet (${achievedRewards.size})", "მიღწეული (${achievedRewards.size})", "已解锁 (${achievedRewards.size})", "Unlocked (${achievedRewards.size})")
                ),
                badges = listOf(0, unredeemedCount)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    if (selectedTabIndex == 0) {
                        if (activeRewards.isEmpty()) {
                            item {
                                Text(
                                    text = tr(language, "Keine aktiven Belohnungen festgelegt. Du kannst beim Bearbeiten oder Erstellen einer Gewohnheit Meilensteine und Belohnungen hinzufügen.", "არ არის განსაზღვრული აქტიური ჯილდოები.", "尚未设定进行中的奖励。你可以在创建或编辑习惯时添加里程碑与专属奖励。", "No active rewards set. You can add milestone rewards when creating or editing a habit."),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            items(activeRewards) { reward ->
                                val habitStat = profileStats.habitStreaks.find { it.habit.id == reward.habitId }
                                val trophy = if (reward.conditionType == "TROPHY_COUPLED") STANDARD_TROPHIES.find { it.id == reward.trophyId } else null
                                
                                val (currentValue, targetValue) = when (reward.conditionType) {
                                    "STREAK" -> (habitStat?.longestStreak ?: 0) to reward.conditionValue.coerceAtLeast(1)
                                    "COMPLETIONS" -> (habitStat?.totalCompletions ?: 0) to reward.conditionValue.coerceAtLeast(1)
                                    "TROPHY_COUPLED" -> {
                                        val streak = habitStat?.longestStreak ?: 0
                                        when (reward.trophyId) {
                                            "WOOD" -> streak to 7
                                            "BRONZE" -> streak to 14
                                            "SILVER" -> streak to 30
                                            "GOLD" -> streak to 100
                                            "DIAMOND" -> streak to 365
                                            "COMP_10" -> profileStats.totalGlobalCompletions to 10
                                            "COMP_50" -> profileStats.totalGlobalCompletions to 50
                                            "COMP_200" -> profileStats.totalGlobalCompletions to 200
                                            "COMP_500" -> profileStats.totalGlobalCompletions to 500
                                            "COMP_1000" -> profileStats.totalGlobalCompletions to 1000
                                            "PERF_7" -> profileStats.perfectDaysStreak to 7
                                            "PERF_30" -> profileStats.perfectDaysStreak to 30
                                            "PERF_365" -> profileStats.perfectDaysStreak to 365
                                            "PERF_1000" -> profileStats.perfectDaysStreak to 1000
                                            else -> 0 to 1
                                        }
                                    }
                                    else -> 0 to 1
                                }
                                val progress = (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
                                val extractedUrl = extractUrl("${reward.rewardText} ${reward.description}")

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AppCard),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(PrimaryViolet.copy(alpha = 0.15f), CircleShape)
                                                    .border(1.dp, PrimaryViolet.copy(alpha = 0.5f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (trophy != null) {
                                                    AchievementBadge(
                                                        type = trophy.type,
                                                        tier = trophy.tier,
                                                        isUnlocked = false,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                } else {
                                                    Text(text = "🎁", fontSize = 24.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = reward.rewardText,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (reward.conditionType == "TROPHY_COUPLED") {
                                                            if (trophy != null) trophy.getLocalizedTitle(language) else (reward.trophyId ?: "")
                                                        } else {
                                                            "🏆 $targetValue " + tr(language, "Tage", "დღე", "天", "Days")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = PrimaryViolet,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                val habit = allHabits.find { it.id == reward.habitId }
                                                if (habit != null) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = habit.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }

                                        if (reward.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = reward.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }

                                        if (extractedUrl != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedButton(
                                                onClick = { openWebUrl(extractedUrl) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryViolet),
                                                border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (language == "de") "Link öffnen 🔗" else "Open Link 🔗",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = tr(language, "Fortschritt", "პროგრესი", "进度", "Progress"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "$currentValue / $targetValue",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = PrimaryViolet,
                                            trackColor = AppBorder
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (achievedRewards.isEmpty()) {
                            item {
                                Text(
                                    text = tr(language, "Noch keine Belohnungen freigeschaltet.", "ჯერ არ არის მიღებული ჯილდო.", "尚未解锁任何奖励。", "No rewards unlocked yet."),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            items(achievedRewards) { reward ->
                                val trophy = if (reward.conditionType == "TROPHY_COUPLED") STANDARD_TROPHIES.find { it.id == reward.trophyId } else null
                                val extractedUrl = extractUrl("${reward.rewardText} ${reward.description}")
                                val isRedeemed = reward.isRedeemed
                                val dateStr = remember(reward.unlockedAt) {
                                    if (reward.unlockedAt > 0L) {
                                        val sdf = SimpleDateFormat("d. MMM yyyy", Locale.getDefault())
                                        sdf.format(Date(reward.unlockedAt))
                                    } else ""
                                }
                                val habit = allHabits.find { it.id == reward.habitId }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isRedeemed) AppCard.copy(alpha = 0.65f) else AppCard),
                                    border = BorderStroke(1.dp, if (isRedeemed) AppBorder else PrimaryViolet.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        if (isRedeemed) TextSecondary.copy(alpha = 0.1f) else PrimaryViolet.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isRedeemed) AppBorder else PrimaryViolet.copy(alpha = 0.5f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (trophy != null) {
                                                    AchievementBadge(
                                                        type = trophy.type,
                                                        tier = trophy.tier,
                                                        isUnlocked = true,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                } else {
                                                    Text(text = "🎁", fontSize = 24.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = reward.rewardText,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = if (isRedeemed) TextSecondary else TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        textDecoration = if (isRedeemed) TextDecoration.LineThrough else null,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (reward.conditionType == "TROPHY_COUPLED") {
                                                            if (trophy != null) trophy.getLocalizedTitle(language) else (reward.trophyId ?: "")
                                                        } else {
                                                            "🏆 ${reward.conditionValue} " + tr(language, "Tage", "დღე", "天", "Days")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isRedeemed) TextSecondary else PrimaryViolet,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                val habitName = habit?.name
                                                Text(
                                                    text = if (!habitName.isNullOrBlank() && dateStr.isNotBlank()) "$habitName • $dateStr" else if (!habitName.isNullOrBlank()) habitName else dateStr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        if (reward.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = reward.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                textDecoration = if (isRedeemed) TextDecoration.LineThrough else null
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (extractedUrl != null) {
                                                OutlinedButton(
                                                    onClick = { openWebUrl(extractedUrl) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryViolet),
                                                    border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.6f)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (language == "de") "Link öffnen 🔗" else "Open Link 🔗",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }

                                            if (!isRedeemed) {
                                                Button(
                                                    onClick = { viewModel.toggleRedeemMilestoneReward(reward.id, true) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = tr(language, "Als eingelöst markieren ✓", "გამოისყიდე", "标记为已兑换", "Mark as Redeemed ✓"),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = tr(language, "Eingelöst ✓", "გამოისყიდა ✓", "已兑换 ✓", "Redeemed ✓"),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF10B981),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    TextButton(
                                                        onClick = { viewModel.toggleRedeemMilestoneReward(reward.id, false) },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (language == "de") "Rückgängig" else "Undo",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
