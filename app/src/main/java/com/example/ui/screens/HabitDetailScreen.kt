package com.example.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.deleteHabit
import com.example.ui.toggleBinaryHabit
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.habitdetail.*
import com.example.ui.components.ModernBackButton
import com.example.ui.screens.stats.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habit: Habit,
    state: HabitDetailUiState?,
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    ClearFocusOnKeyboardDismiss()
    val currentStreak = state?.currentStreak ?: 0
    val longestStreak = state?.longestStreak ?: 0
    val strength = state?.strength ?: 0
    val habitColor = remember(habit.color) { HabitIconMapping.getColor(habit.color) }
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }

    val thisWeekCount = state?.thisWeekCount ?: 0
    val thisMonthCount = state?.thisMonthCount ?: 0
    val thisYearCount = state?.thisYearCount ?: 0
    val totalCount = state?.totalCount ?: 0

    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 84.dp)
        ) {

        // Strength score card
        item(key = "detail_strength") {
            HabitDetailStrengthCard(
                habit = habit,
                strength = strength,
                language = language,
                onInfoClick = { title, explanation -> activeExplanation = title to explanation }
            )
        }

        // Description card (direkt unter dem Stärke-Feld)
        if (habit.description.isNotBlank()) {
            item(key = "detail_description") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(PrimaryViolet.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Beschreibung",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = tr(language, "BESCHREIBUNG", "აღწერა", "描述说明", "DESCRIPTION"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Streaks cards (Two separate individual cards)
        item(key = "detail_streaks") {
            HabitDetailStreakCards(
                habit = habit,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                language = language,
                onInfoClick = { t, e -> activeExplanation = t to e }
            )
        }



        // 3. Interactive Monthly Calendar History Card (Placed right after Total Completions)
        if (state != null) {
            item(key = "detail_month_calendar") {
                HabitCalendarCard(
                    habit = habit,
                    gridRows = state.calendarGridRows,
                    monthName = state.monthName,
                    habitColor = habitColor,
                    accentColor = accentColor,
                    language = language,
                    canPrevMonth = state.canPrevMonth,
                    canNextMonth = state.canNextMonth,
                    onPrevMonth = { viewModel.navigateCalendarMonth(-1) },
                    onNextMonth = { viewModel.navigateCalendarMonth(1) },
                    onDayClick = { dateStr ->
                        viewModel.toggleBinaryHabit(habit.id, dateStr, false)
                    },
                    onInfoClick = { t, e -> activeExplanation = t to e }
                )
            }
        }

        // Finishable / Milestone Horizon Card with Prognosis
        if (habit.isFinishable && (habit.totalTargetValue ?: 0f) > 0f) {
            item(key = "detail_finishable_milestone") {
                val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
                FinishableHabitProgressCard(
                    habit = habit,
                    totalCount = totalCount,
                    currentStreak = currentStreak,
                    logs = allLogs,
                    language = language,
                    onInfoClick = { t, e -> activeExplanation = t to e }
                )
            }
        }

        // 4. Target performance card (Zielerfüllung) - only for numerical habits!
        val isNumericalHabit = habit.type == "NUMBER" || habit.type == "NUMERICAL" || (habit.targetValue > 1f && habit.type != "BINARY")
        if (isNumericalHabit && state?.targetStats != null) {
            item(key = "detail_target_stats") {
                HabitTargetSection(
                    state = state,
                    language = language,
                    onInfoClick = { t, e -> activeExplanation = t to e }
                )
            }
        }

        // 5. Success rate card (Erfolgsquote)
        item(key = "detail_success_rate") {
            val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
            var selectedTimeframeIndex by remember { mutableIntStateOf(0) }

            val stats = remember(selectedTimeframeIndex, habit, allLogs) {
                calculateHabitSuccessStatsForPeriod(
                    habit = habit,
                    logs = allLogs,
                    periodIndex = selectedTimeframeIndex
                )
            }

            SuccessRateCard(
                title = tr(language, "Erfolgsquote", "წარმატების მაჩვენებელი", "成功率", "Success Rate"),
                doneCount = stats.doneCount,
                missedCount = stats.missedCount,
                skippedCount = stats.skippedCount,
                pendingCount = stats.pendingCount,
                language = language,
                infoTitle = tr(language, "Erfolgsquote", "წარმატების მაჩვენებელი", "成功率", "Success Rate"),
                infoExplanation = if (language == "de") "Aufschlüsselung aller aktiven Tage im gewählten Zeitraum nach Status: Erreicht, Gescheitert, Skipped und Ausstehend." else if (language == "ka") "არჩეულ პერიოდში ყველა დაგეგმილი დღის დაყოფა სტატუსის მიხედვით: დასრულებული, გამოტოვებული, გამოტოვებული და მომლოდინე." else "Breakdown of all scheduled days in the selected period by status: Done, Missed, Skipped, and Pending.",
                onInfoClick = { t, e -> activeExplanation = t to e },
                showTimeframeSelector = true,
                selectedTimeframeIndex = selectedTimeframeIndex,
                onTimeframeSelected = { selectedTimeframeIndex = it }
            )
        }

        // 6. Habit Score Trend Card (Line Chart)
        item(key = "detail_score_trend") {
            val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
            HabitScoreTrendCard(
                habit = habit,
                logs = allLogs,
                language = language,
                onInfoClick = { t, e -> activeExplanation = t to e }
            )
        }

        // 7. Completion Volume Progression Card (Bar Chart)
        item(key = "detail_volume_progression") {
            val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
            HabitVolumeProgressionCard(
                habit = habit,
                logs = allLogs,
                language = language,
                accentColor = accentColor,
                onInfoClick = { t, e -> activeExplanation = t to e }
            )
        }

        // Weekday Frequency Section (For ALL habits!)
        item(key = "detail_frequency") {
            WeekdayFrequencySection(
                weekdayStats = state?.weekdayStats ?: emptyList(),
                gridData = state?.weekdayGridData ?: emptyList(),
                weeksWithMonthLabels = state?.weeksWithMonthLabels ?: emptyList(),
                language = language,
                onInfoClick = { t, e -> activeExplanation = t to e },
                allDailyNotes = allDailyNotes
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = tr(language, "Habit löschen?", "წაშალოთ ჩვევა?", "删除习惯？", "Delete Habit?")) },
            text = { Text(text = tr(language, "Möchtest du diese Gewohnheit wirklich unwiderruflich löschen?", "დარწმუნებული ხართ, რომ გსურთ სამუდამოდ წაშალოთ ეს ჩვევა?", "确定要永久删除该习惯吗？", "Are you sure you want to delete this habit permanently?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(habit)
                        showDeleteConfirm = false
                        Toast.makeText(
                            context,
                            tr(language, "Gewohnheit gelöscht", "ჩვევა წაშლილია", "习惯已删除", "Habit deleted"),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text(tr(language, "Löschen", "წაშლა", "删除", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"))
                }
            }
        )
    }

    if (activeExplanation != null) {
        ExplanationDialog(
            title = activeExplanation!!.first,
            explanation = activeExplanation!!.second,
            onDismiss = { activeExplanation = null }
        )
    }

    // Floating Header Overlay with vertical gradient fade-out
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppBg,
                        AppBg,
                        AppBg.copy(alpha = 0.9f),
                        AppBg.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
            .align(Alignment.TopCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = habit.name,
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
}

