package com.example.ui.screens.stats

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.screens.profile.AchievementBadge
import com.example.ui.screens.profile.ConfettiAnimation
import com.example.ui.theme.*

data class MilestoneItem(
    val target: Int,
    val isUnlocked: Boolean,
    val text: String
)

data class RankTier(
    val nameDe: String,
    val nameKa: String,
    val nameZh: String,
    val nameEn: String,
    val color: Color,
    val icon: ImageVector
)

fun getRankTierForTarget(target: Int): RankTier {
    return when (target) {
        7 -> RankTier("Holz", "ხე", "木质", "Wood", Color(0xFF8B5A2B), Icons.Default.Terrain)
        14 -> RankTier("Bronze", "ბრინჯაო", "青铜", "Bronze", Color(0xFFCD7F32), Icons.Default.WorkspacePremium)
        30 -> RankTier("Silber", "ვერცხლი", "白银", "Silver", Color(0xFFC0C0C0), Icons.Default.WorkspacePremium)
        60 -> RankTier("Gold", "ოქრო", "黄金", "Gold", Color(0xFFFFD700), Icons.Default.EmojiEvents)
        100 -> RankTier("Platin", "პლატინა", "铂金", "Platinum", Color(0xFFE5E4E2), Icons.Default.MilitaryTech)
        180 -> RankTier("Diamant", "ალმასი", "钻石", "Diamond", Color(0xFF00E5FF), Icons.Default.Diamond)
        270 -> RankTier("Rubin", "ლალი", "红宝石", "Ruby", Color(0xFFFF1744), Icons.Default.OfflineBolt)
        365 -> RankTier("Meister", "ოსტატი", "大师", "Master", Color(0xFFD500F9), Icons.Default.AutoAwesome)
        500 -> RankTier("Legende", "ლეგენდა", "传奇", "Legend", Color(0xFFFF9100), Icons.Default.Stars)
        1000 -> RankTier("Unreal", "არარეალური", "极境", "Unreal", Color(0xFF76FF03), Icons.Default.Whatshot)
        else -> RankTier("Rang", "რანგი", "等级", "Rank", Color.White, Icons.Default.EmojiEvents)
    }
}

@Composable
fun MilestoneBadge(milestone: MilestoneItem, language: String) {
    val rank = getRankTierForTarget(milestone.target)
    val color = if (milestone.isUnlocked) rank.color else TextSecondary.copy(alpha = 0.3f)
    val context = LocalContext.current
    val rankName = if (language == "de") rank.nameDe else rank.nameEn

    val scale by animateFloatAsState(
        targetValue = if (milestone.isUnlocked) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "milestone_scale"
    )

    var showConfetti by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable {
                        if (milestone.isUnlocked) {
                            showConfetti = true
                            Toast.makeText(
                                context,
                                tr(language, "Erreicht: $rankName!", "განბლოკილია: $rankName !", "已解锁：$rankName！", "Unlocked: $rankName!"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                tr(language, "Noch gesperrt: $rankName (${milestone.target} Tage)", "ჩაკეტილი: $rankName ( ${milestone.target} დღე)", "未解锁：$rankName（需达成 ${milestone.target} 天）", "Locked: $rankName (${milestone.target} days)"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val tierStr = when (milestone.target) {
                    7 -> "WOOD"
                    14 -> "BRONZE"
                    30 -> "SILVER"
                    60 -> "GOLD"
                    100 -> "PLATINUM"
                    180 -> "DIAMOND"
                    270 -> "RUBY"
                    365 -> "MASTER"
                    500 -> "LEGEND"
                    1000 -> "UNREAL"
                    else -> "WOOD"
                }

                AchievementBadge(
                    type = "STREAK",
                    tier = tierStr,
                    isUnlocked = milestone.isUnlocked,
                    modifier = Modifier.fillMaxSize()
                )

                if (!milestone.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(AppCard, CircleShape)
                            .border(1.5.dp, TextSecondary.copy(alpha = 0.4f), CircleShape)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = TextSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = milestone.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (milestone.isUnlocked) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        if (showConfetti && milestone.isUnlocked) {
            ConfettiAnimation(
                onFinished = { showConfetti = false }
            )
        }
    }
}

@Composable
fun StreakMilestonesCard(longestStreak: Int, language: String) {
    val milestoneTargets = remember { listOf(7, 14, 30, 60, 100, 180, 270, 365, 500, 1000) }
    val items = remember(longestStreak) {
        milestoneTargets.map { target ->
            MilestoneItem(
                target = target,
                isUnlocked = longestStreak >= target,
                text = "$target"
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = "Milestones",
                    tint = HabitStreakFlame,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "ERFOLGSSTRÄHNE", "სტრიქის ეტაპები", "连续里程碑", "STREAK MILESTONES"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val chunks = items.chunked(5)
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { milestone ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                MilestoneBadge(milestone = milestone, language = language)
                            }
                        }
                    }
                }
            }
        }
    }
}

