package com.example.ui.screens.profile

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LocalInfoCardsEnabled
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*

@Composable
fun CategoryHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    explanationTitle: String? = null,
    explanationText: String? = null,
    onInfoClick: ((String, String) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        if (explanationTitle != null && explanationText != null && onInfoClick != null) {
            InfoIconButton(
                title = explanationTitle,
                explanation = explanationText,
                onClick = onInfoClick
            )
        }
    }
}

@Composable
fun HabitStreakAchievementCard(
    habitName: String,
    habitColor: Color,
    habitIcon: String,
    longestStreak: Int,
    language: String
) {
    val context = LocalContext.current
    val targets = listOf(7, 14, 30, 100, 365)
    val tiers = listOf("WOOD", "BRONZE", "SILVER", "GOLD", "DIAMOND")
    val tierNamesDe = listOf("Holz-Serie", "Bronze-Serie", "Silber-Serie", "Gold-Serie", "Diamant-Serie")
    val tierNamesEn = listOf("Wood Streak", "Bronze Streak", "Silver Streak", "Gold Streak", "Diamond Streak")

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(habitColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconMapping.getIcon(habitIcon),
                        contentDescription = null,
                        tint = habitColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habitName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "Längste Serie: $longestStreak Tage", "უგრძელესი სერია: $longestStreak დღე", "最长连续：$longestStreak 天", "Longest Streak: $longestStreak Days"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val nextTargetIndex = when {
                longestStreak < 7 -> 0
                longestStreak < 14 -> 1
                longestStreak < 30 -> 2
                longestStreak < 100 -> 3
                longestStreak < 365 -> 4
                else -> -1
            }

            val (labelText, progressFraction, percent) = if (nextTargetIndex != -1) {
                val prevTarget = if (nextTargetIndex == 0) 0 else targets[nextTargetIndex - 1]
                val nextTarget = targets[nextTargetIndex]
                val targetName = tr(language,
                    listOf("Holz-Serie", "Bronze-Serie", "Silber-Serie", "Gold-Serie", "Diamant-Serie")[nextTargetIndex],
                    listOf("ხის სერია", "ბრინჯაოს სერია", "ვერცხლის სერია", "ოქროს სერია", "ბრილიანტის სერია")[nextTargetIndex],
                    listOf("木质连续", "青铜连续", "白银连续", "黄金连续", "钻石连续")[nextTargetIndex],
                    listOf("Wood Streak", "Bronze Streak", "Silver Streak", "Gold Streak", "Diamond Streak")[nextTargetIndex]
                )
                
                val range = nextTarget - prevTarget
                val earned = longestStreak - prevTarget
                val fraction = (earned.toFloat() / range.toFloat()).coerceIn(0f, 1f)
                val pct = (fraction * 100).toInt()
                
                val label = if (language == "de") "Weg zur $targetName (${longestStreak}/${nextTarget} Tage)" else if (language == "ka") "გზა $targetName -მდე ( ${longestStreak} / ${nextTarget} დღეები)" else "Path to $targetName (${longestStreak}/${nextTarget} Days)"
                Triple(label, fraction, pct)
            } else {
                val label = tr(language, "Diamant-Serie meisterhaft erreicht! 🎉", "ბრილიანტის სერია დაეუფლა! 🎉", "钻石连续完美达成！🎉", "Diamond Streak mastered! 🎉")
                Triple(label, 1f, 100)
            }

            var hasAnimatedStreakProgress by rememberSaveable(habitName) { mutableStateOf(false) }
            val animStreakFraction = remember { Animatable(if (hasAnimatedStreakProgress) progressFraction else 0f) }

            LaunchedEffect(progressFraction) {
                if (!hasAnimatedStreakProgress) {
                    animStreakFraction.snapTo(0f)
                    animStreakFraction.animateTo(
                        targetValue = progressFraction,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                    )
                    hasAnimatedStreakProgress = true
                } else {
                    animStreakFraction.animateTo(
                        targetValue = progressFraction,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            }

            val animatedStreakFraction = animStreakFraction.value

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${(animatedStreakFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { animatedStreakFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = habitColor,
                trackColor = ProgressTrack.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                targets.forEachIndexed { idx, target ->
                    val isUnlocked = longestStreak >= target
                    val tierStr = tiers[idx]
                    val badgeTitle = if (language == "de") tierNamesDe[idx] else tierNamesEn[idx]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (isUnlocked) {
                                    Toast.makeText(
                                        context,
                                        tr(language, "Freigeschaltet: $badgeTitle ($target Tage)!", "განბლოკილია: $badgeTitle ( $target დღე)!", "已解锁：$badgeTitle（达成 $target 天）！", "Unlocked: $badgeTitle ($target days)!"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        tr(language, "Noch gesperrt: $badgeTitle ($target Tage benötigt, aktuell: $longestStreak)", "ჩაკეტილი: $badgeTitle (საჭიროა $target დღე, მიმდინარე: $longestStreak )", "未解锁：$badgeTitle（需达成 $target 天，当前：$longestStreak 天）", "Locked: $badgeTitle ($target days required, current: $longestStreak)"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AchievementBadge(
                                type = "STREAK",
                                tier = tierStr,
                                isUnlocked = isUnlocked,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(AppCard, CircleShape)
                                        .border(1.dp, TextSecondary.copy(alpha = 0.4f), CircleShape)
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(language, "${target} Tage", "${target} დღეები", "${target} 天", "${target} Days"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalAchievementCard(
    type: String, // "COMPLETIONS" or "PERFECT_DAYS"
    tier: String, // "COMP_10", etc.
    title: String,
    description: String,
    targetValue: Int,
    currentValue: Int,
    isUnlocked: Boolean,
    language: String
) {
    val borderTint = when (tier) {
        "COMP_10" -> Color(0xFF4CAF50)
        "COMP_50" -> Color(0xFF2196F3)
        "COMP_200" -> Color(0xFF9C27B0)
        "COMP_500" -> Color(0xFFFF5722)
        "COMP_1000" -> Color(0xFFF44336)
        "PERF_7" -> Color(0xFF00BCD4)
        "PERF_30" -> Color(0xFFE91E63)
        "PERF_365" -> Color(0xFFFF9800)
        "PERF_1000" -> Color(0xFFFFD700)
        else -> Color(0xFFFFD700)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) AppCard else AppCard.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) borderTint.copy(alpha = 0.4f) else AppBorder.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AchievementBadge(
                type = type,
                tier = tier,
                isUnlocked = isUnlocked,
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isUnlocked) TextPrimary else TextPrimary.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnlocked) {
                        Text(
                            text = tr(language, "Freigeschaltet", "განბლოკილია", "已解锁", "Unlocked"),
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnlocked) TextSecondary else TextSecondary.copy(alpha = 0.5f)
                )

                if (!isUnlocked) {
                    Spacer(modifier = Modifier.height(10.dp))

                    val progressFraction = if (targetValue > 0) {
                        (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
                    } else 0f

                    var hasAnimatedAchieveProgress by rememberSaveable(title) { mutableStateOf(false) }
                    val animAchieveFraction = remember { Animatable(if (hasAnimatedAchieveProgress) progressFraction else 0f) }

                    LaunchedEffect(progressFraction) {
                        if (!hasAnimatedAchieveProgress) {
                            animAchieveFraction.snapTo(0f)
                            animAchieveFraction.animateTo(
                                targetValue = progressFraction,
                                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                            )
                            hasAnimatedAchieveProgress = true
                        } else {
                            animAchieveFraction.animateTo(
                                targetValue = progressFraction,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            )
                        }
                    }

                    val animatedAchieveFraction = animAchieveFraction.value

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "de") "Fortschritt: $currentValue / $targetValue" else if (language == "ka") "პროგრესი: $currentValue / $targetValue" else "Progress: $currentValue / $targetValue",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${(animatedAchieveFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedAchieveFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = borderTint.copy(alpha = 0.6f),
                        trackColor = ProgressTrack.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun UnlockedMilestoneRewardCard(
    rewardTitle: String,
    streakTarget: Int,
    unlockedAt: Long,
    habitName: String?,
    language: String
) {
    val dateStr = remember(unlockedAt) {
        val sdf = java.text.SimpleDateFormat("dd. MMM yyyy", when (language) {
            "de" -> java.util.Locale.GERMAN
            else -> java.util.Locale.US
        })
        sdf.format(java.util.Date(unlockedAt))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryViolet.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, PrimaryViolet.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎁", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rewardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "🏆 $streakTarget " + tr(language, "Tage", "დღე", "天", "Days"),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (!habitName.isNullOrBlank()) "$habitName • $dateStr" else dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
