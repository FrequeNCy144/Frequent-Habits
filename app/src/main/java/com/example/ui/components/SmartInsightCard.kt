package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.*
import com.example.data.*
import com.example.ui.theme.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.tr

@Composable
fun SmartInsightCard(
    language: String,
    userName: String,
    todayDateString: String,
    smartInsightDismissedDate: String,
    onDismiss: (String) -> Unit,
    perfectDaysStats: PerfectDaysStats,
    habitsWithStats: List<HabitStatModel>,
    allLogs: List<HabitLog>,
    allHabits: List<Habit>,
    dismissedReviews: Set<String> = emptySet(),
    onDismissReview: (String) -> Unit = {},
    onOpenMonthlyReview: (Int, Int) -> Unit = { _, _ -> },
    onOpenYearlyReview: (Int) -> Unit = {}
) {
    if (smartInsightDismissedDate == todayDateString) return

    val context = LocalContext.current

    val uniqueLoggedDays = remember(allLogs) {
        SmartInsightEngine.getUniqueLoggedDaysCount(allLogs)
    }

    if (!SmartInsightEngine.hasUnlockedSmartInsights(allLogs)) {
        return
    }

    val insights = remember(allLogs, habitsWithStats, perfectDaysStats, language, userName, todayDateString, dismissedReviews) {
        SmartInsightEngine.generateInsights(
            context = context,
            allHabits = allHabits,
            allLogs = allLogs,
            language = language,
            userName = userName,
            todayDateString = todayDateString,
            currentStreak = perfectDaysStats.currentStreak,
            completionRate = perfectDaysStats.totalCompletionRate,
            dismissedReviews = dismissedReviews,
            checkCooldowns = true
        )
    }

    if (insights.isEmpty()) return

    val selectedOption = SmartInsightEngine.selectBestInsight(insights, todayDateString) ?: return

    LaunchedEffect(selectedOption.type, todayDateString) {
        SmartInsightEngine.recordInsightTypeShown(context, selectedOption.type, todayDateString)
    }

    val (insightIcon, iconColor) = when (selectedOption.iconType) {
        SmartInsightIconType.REVIEW -> if (selectedOption.reviewMonth != null) Pair(Icons.Default.HourglassTop, PrimaryViolet) else Pair(Icons.Default.AutoAwesome, PrimaryViolet)
        SmartInsightIconType.COMPLETION -> Pair(Icons.Default.CheckCircle, SecondaryViolet)
        SmartInsightIconType.BOLT -> Pair(Icons.Default.Bolt, PrimaryViolet)
        SmartInsightIconType.TRENDING_UP -> Pair(Icons.Default.TrendingUp, SuccessGreen)
        SmartInsightIconType.FIRE -> Pair(Icons.Default.LocalFireDepartment, HabitOrange)
        SmartInsightIconType.TARGET -> Pair(Icons.Default.TrackChanges, HabitCyan)
        SmartInsightIconType.STAR -> Pair(Icons.Default.Star, HabitYellow)
        SmartInsightIconType.SPARKLE -> Pair(Icons.Default.AutoAwesome, PrimaryViolet)
        SmartInsightIconType.LIGHTBULB -> Pair(Icons.Default.Lightbulb, HabitOrange)
        SmartInsightIconType.TRENDING_DOWN -> Pair(Icons.Default.TrendingDown, HabitRed)
        SmartInsightIconType.DATE_RANGE -> Pair(Icons.Default.DateRange, PrimaryViolet)
        SmartInsightIconType.CHART -> Pair(Icons.Default.ShowChart, SecondaryViolet)
        SmartInsightIconType.ARROW_UP -> Pair(Icons.Default.ArrowUpward, HabitGreen)
    }

    val onClickAction: (() -> Unit)? = if (selectedOption.isReviewPrompt) {
        {
            if (selectedOption.reviewMonth != null && selectedOption.reviewYear != null) {
                val key = "monthly_${selectedOption.reviewYear}_${selectedOption.reviewMonth}"
                onDismissReview(key)
                onOpenMonthlyReview(selectedOption.reviewYear, selectedOption.reviewMonth)
            } else if (selectedOption.reviewYear != null) {
                val key = "yearly_${selectedOption.reviewYear}"
                onDismissReview(key)
                onOpenYearlyReview(selectedOption.reviewYear)
            }
        }
    } else null

    val insightText = selectedOption.text

    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(500)),
        exit = shrinkVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .then(
                    if (onClickAction != null) {
                        Modifier.clickable { onClickAction.invoke() }
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = insightIcon,
                        contentDescription = "Insight Icon",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(language, "Smart Insight", "ჭკვიანი ინსაითი", "智能洞察", "Smart Insight"),
                            style = MaterialTheme.typography.labelMedium,
                            color = iconColor,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    visible = false
                                    onDismiss(todayDateString)
                                    if (selectedOption.isReviewPrompt) {
                                        val key = if (selectedOption.reviewMonth == null) {
                                            "yearly_${selectedOption.reviewYear}"
                                        } else {
                                            "monthly_${selectedOption.reviewYear}_${selectedOption.reviewMonth}"
                                        }
                                        onDismissReview(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val annotatedText = remember(insightText) {
                        val parts = insightText.split("**")
                        buildAnnotatedString {
                            parts.forEachIndexed { index, part ->
                                if (index % 2 == 1) {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                                        append(part)
                                    }
                                } else {
                                    append(part)
                                }
                            }
                        }
                    }

                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}