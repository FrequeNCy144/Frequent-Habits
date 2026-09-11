package com.example.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UnlockedAchievementInfo
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.FeedbackHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AchievementUnlockedOverlay(
    achievement: UnlockedAchievementInfo,
    language: String,
    onDismiss: () -> Unit,
    onClaimReward: ((Int?) -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(achievement) {
        FeedbackHelper.playCompletionFeedback(
            context = context,
            vibrationEnabled = true
        )
    }

    val scaleAnim = remember { Animatable(0.3f) }
    val rotationAnim = remember { Animatable(-25f) }
    val glowAnim = rememberInfiniteTransition(label = "achievement_glow")
    val glowScale by glowAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    var saturation by remember { mutableStateOf(0f) }
    val saturationAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val job1 = launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val job2 = launch {
            rotationAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val job3 = launch {
            delay(400)
            saturationAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            ) {
                saturation = this.value
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())

            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(2.dp, PrimaryViolet.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tr(language, "ERFOLG FREIGESCHALTET!", "მიღწევა განბლოკილია!", "成就解锁！", "ACHIEVEMENT UNLOCKED!"),
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .graphicsLayer {
                                    scaleX = glowScale
                                    scaleY = glowScale
                                    alpha = glowAlpha
                                }
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            PrimaryViolet.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scaleAnim.value
                                    scaleY = scaleAnim.value
                                }
                                .saturationFilter(saturation)
                        ) {
                            AchievementBadge(
                                type = achievement.type,
                                tier = achievement.tier,
                                isUnlocked = true,
                                modifier = Modifier.size(150.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    if (!achievement.habitName.isNullOrEmpty() && achievement.habitColor != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val hColor = HabitIconMapping.getColor(achievement.habitColor)
                        Surface(
                            color = hColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, hColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (!achievement.habitIcon.isNullOrEmpty()) {
                                    Icon(
                                        imageVector = HabitIconMapping.getIcon(achievement.habitIcon),
                                        contentDescription = null,
                                        tint = hColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = achievement.habitName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    if (!achievement.rewardText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryViolet.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, PrimaryViolet),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Reward",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tr(language, "Deine persönliche Belohnung:", "თქვენი პირადი ჯილდო:", "你的专属奖励：", "Your Personal Reward:"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PrimaryViolet,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = achievement.rewardText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                if (!achievement.rewardDescription.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = achievement.rewardDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PrimaryViolet,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(language, "Du hast diese Belohnung freigeschaltet! Du kannst sie jederzeit in den Meilenstein-Belohnungen auf der Statistik-Seite einlösen.", "თქვენ განბლოკეთ ეს ჯილდო! შეგიძლიათ გამოისყიდოთ ის სტატისტიკის გვერდზე.", "你解锁了这项奖励！你可以在统计页面的里程碑奖励中查看与兑换。", "You unlocked this reward! You can view and redeem it anytime in Milestone Rewards on the Statistics page."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    val hasReward = !achievement.rewardText.isNullOrEmpty() || achievement.milestoneRewardId != null
                    if (hasReward) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AppBorder),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppBorder.copy(alpha = 0.55f),
                                    contentColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("dismiss_achievement_button")
                            ) {
                                Text(
                                    text = tr(language, "Klasse! 🎉", "გასაოცარია! 🎉", "太棒了！🎉", "Awesome! 🎉"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    onClaimReward?.invoke(achievement.milestoneRewardId) ?: onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("claim_achievement_button")
                            ) {
                                Text(
                                    text = tr(language, "Einlösen 🎁", "გამოისყიდე 🎁", "兑换奖励 🎁", "Claim 🎁"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("dismiss_achievement_button")
                        ) {
                            Text(
                                text = tr(language, "Klasse! 🎉", "გასაოცარია! 🎉", "太棒了！🎉", "Awesome! 🎉"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
