package com.example.ui.dialogs

import com.example.ui.components.*

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.calculateMonthlyReviewData
import com.example.data.calculateYearlyReviewData
import com.example.tr
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewArchiveDialog(
    showReviewArchive: Boolean,
    availableYears: List<Int>,
    availableMonths: List<Pair<Int, Int>>,
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    language: String,
    onShowReviewExplanation: () -> Unit,
    onSelectReviewMonth: (Pair<Int, Int>) -> Unit,
    onSelectReviewYear: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!showReviewArchive) return

    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Monthly, 1 = Yearly
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(language, "Deine Rückblicke", "თქვენი მიმოხილვები", "你的回顾", "Your Reviews"),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onShowReviewExplanation,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TimeframeSelectorPills(
                selectedTimeframeIndex = selectedTabIndex,
                onTimeframeSelected = { selectedTabIndex = it },
                language = language,
                customLabels = listOf(
                    tr(language, "Monatlich (${availableMonths.size})", "ყოველთვიური (${availableMonths.size})", "月度回顾 (${availableMonths.size})", "Monthly (${availableMonths.size})"),
                    tr(language, "Jährlich (${availableYears.size})", "ყოველწლიურად (${availableYears.size})", "年度回顾 (${availableYears.size})", "Yearly (${availableYears.size})")
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTabIndex == 0) {
                    if (availableMonths.isEmpty()) {
                        item {
                            Text(
                                text = tr(language, "Noch keine Monatsrückblicke verfügbar.", "ყოველთვიური მიმოხილვები ჯერ არ არის ხელმისაწვდომი.", "暂无月度回顾。", "No monthly reviews available yet."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(availableMonths) { (y, m) ->
                            val monthLogsCount = calculateMonthlyReviewData(y, m, allHabits, allLogs, language).totalCompletions
                            val mName = when (m) {
                                1 -> tr(language, "Januar", "იანვარი", "1月", "January")
                                2 -> tr(language, "Februar", "თებერვალი", "2月", "February")
                                3 -> tr(language, "März", "მარტი", "3月", "March")
                                4 -> tr(language, "April", "აპრილი", "4月", "April")
                                5 -> tr(language, "Mai", "მაისი", "5月", "May")
                                6 -> tr(language, "Juni", "ივნისი", "6月", "June")
                                7 -> tr(language, "Juli", "ივლისი", "7月", "July")
                                8 -> tr(language, "August", "აგვისტო", "8月", "August")
                                9 -> tr(language, "September", "სექტემბერი", "9月", "September")
                                10 -> tr(language, "Oktober", "ოქტომბერი", "10月", "October")
                                11 -> tr(language, "November", "ნოემბერი", "11月", "November")
                                12 -> tr(language, "Dezember", "დეკემბერი", "12月", "December")
                                else -> ""
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectReviewMonth(y to m)
                                    },
                                color = AppBg,
                                border = BorderStroke(1.dp, AppBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "📊", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tr(language, "Rückblick $mName $y", "მიმოხილვა $mName $y", "$y 年 $mName 回顾", "Review $mName $y"),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$monthLogsCount " + (tr(language, "Abschlüsse", "დასრულებები", "完成次数", "completions")),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (availableYears.isEmpty()) {
                        item {
                            Text(
                                text = tr(language, "Noch keine Jahresrückblicke verfügbar.", "ჯერ არ არის ხელმისაწვდომი წლიური მიმოხილვები.", "暂无年度回顾。", "No yearly reviews available yet."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(availableYears) { y ->
                            val yearLogsCount = calculateYearlyReviewData(y, allHabits, allLogs, language).totalCompletions
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectReviewYear(y)
                                    },
                                color = AppBg,
                                border = BorderStroke(1.dp, AppBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(PrimaryViolet.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "🎆", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tr(language, "Jahresrückblick $y", "$y წელი მიმოხილვა", "$y 年度回顾", "$y Year in Review"),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$yearLogsCount " + (tr(language, "Abschlüsse", "დასრულებები", "完成次数", "completions")),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(20.dp)
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
