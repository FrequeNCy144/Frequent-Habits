package com.example.ui.screens

import java.util.Calendar
import com.example.data.*

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LocalInfoCardsEnabled
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.MilestoneReward
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.profile.*
import com.example.ui.screens.stats.*
import com.example.ui.share.isNewYearReviewPeriod
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun OverallStatsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit,
    onNavigateToToday: () -> Unit
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val overallCalData by viewModel.overallCalendarData.collectAsStateWithLifecycle()
    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }

    val heatmapMonthOffset by viewModel.heatmapMonthOffset.collectAsStateWithLifecycle()
    val heatmapCanPrevMonth by viewModel.heatmapCanPrevMonth.collectAsStateWithLifecycle()
    val heatmapCanNextMonth by viewModel.heatmapCanNextMonth.collectAsStateWithLifecycle()
    val heatmapMonthNameAndYear by viewModel.heatmapMonthNameAndYear.collectAsStateWithLifecycle()
    val heatmapMonthGridData by viewModel.heatmapMonthGridData.collectAsStateWithLifecycle()
    val heatmapYearGridData by viewModel.heatmapYearGridData.collectAsStateWithLifecycle()
    val heatmapYearMonthLabels by viewModel.heatmapYearMonthLabels.collectAsStateWithLifecycle()

    val selectedHeatmapCell by viewModel.selectedHeatmapCell.collectAsStateWithLifecycle()
    val heatmapViewMode by viewModel.heatmapViewMode.collectAsStateWithLifecycle()
    val activeCell by viewModel.activeHeatmapCell.collectAsStateWithLifecycle()
    val formattedDate by viewModel.formattedActiveCellDate.collectAsStateWithLifecycle()
    val firstHabitDateStr by viewModel.firstHabitDateStr.collectAsStateWithLifecycle()

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedReviewYear by remember { mutableStateOf<Int?>(null) }
    var selectedReviewMonth by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showReviewArchive by remember { mutableStateOf(false) }
    var showReviewExplanation by remember { mutableStateOf(false) }
    val isNYReviewWindow = remember { isNewYearReviewPeriod() }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val availableYears = remember(allLogs, currentYear) {
        val logYears = allLogs.mapNotNull {
            if (it.date.length >= 4) it.date.substring(0, 4).toIntOrNull() else null
        }
        logYears.filter { y -> y < currentYear }
            .distinct()
            .sortedDescending()
    }

    val currentYearMonth = remember {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1 // 1-indexed
        y to m
    }
    val availableMonths = remember(allLogs, currentYearMonth) {
        val logMonths = allLogs.mapNotNull { log ->
            if (log.date.length >= 7) {
                val parts = log.date.split("-")
                if (parts.size >= 2) {
                    val y = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    if (y != null && m != null) {
                        y to m
                    } else null
                } else null
            } else null
        }
        val cal = Calendar.getInstance()
        val cY = cal.get(Calendar.YEAR)
        val cM = cal.get(Calendar.MONTH) + 1 // 1-indexed
        
        // A month is only available if it's strictly in the past (before current year/month)
        logMonths.filter { (y, m) -> 
            y < cY || (y == cY && m < cM)
        }.distinct()
        .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 84.dp)
        ) {
            // Yearly Review Banner (Shown strictly during New Year period)
            if (isNYReviewWindow) {
                item(key = "yearly_review_banner") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentColor,
                                        accentColor.copy(alpha = 0.85f),
                                        accentColor.copy(alpha = 0.70f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "✨ " + (tr(language, "RÜCKBLICK $currentYear", "$currentYear მიმოხილვა", "$currentYear 年度回顾", "$currentYear REVIEW")),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(language, "Dein Jahresrückblick $currentYear", "თქვენი $currentYear ამბავი", "你的 $currentYear 年度故事", "Your $currentYear Story"),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tr(language, "Highlights, Statistiken & Star-Gewohnheiten entdecken", "აღმოაჩინეთ თქვენი მაჩვენებლები, სტატისტიკა და ვარსკვლავების ჩვევები", "探索你的高光时刻、数据统计与明星习惯", "Discover your highlights, stats & star habits"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { selectedReviewYear = currentYear },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = accentColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(language, "Starten", "თამაში", "开始体验", "Play"),
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
            }

            // 1. Overall Account Strength Gauge
            item(key = "overall_strength") {
                var hasAnimatedStatsStrength by rememberSaveable { mutableStateOf(false) }
                val animStrength = remember { Animatable(if (hasAnimatedStatsStrength) strength.toFloat() else 0f) }

                LaunchedEffect(strength) {
                    if (!hasAnimatedStatsStrength) {
                        animStrength.snapTo(0f)
                        animStrength.animateTo(
                            targetValue = strength.toFloat(),
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                        )
                        hasAnimatedStatsStrength = true
                    } else {
                        animStrength.animateTo(
                            targetValue = strength.toFloat(),
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        )
                    }
                }
                val animatedStrength = animStrength.value
                val ringColor = remember(strength) {
                    when {
                        strength < 35 -> HabitRed
                        strength < 70 -> HabitYellow
                        else -> SuccessGreen
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppBorder, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title and info button above the semi-circle gauge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(accentColor.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Strength",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(language, "GESAMT-STÄRKE", "საერთო სიძლიერე", "综合稳固度", "OVERALL STRENGTH"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                InfoIconButton(
                                    title = tr(language, "Gesamt-Stärke Score", "სიძლიერის საერთო ქულა", "综合稳固度评分", "Overall Strength Score"),
                                    explanation = if (language == "de") "Der Durchschnitt der Stärke-Scores (0-100) all deiner aktiven Gewohnheiten. Der Score jeder Gewohnheit basiert auf den letzten 30 Tagen, wobei jüngere Einträge leicht höher gewichtet werden und heute noch ausstehende Aufgaben den Score nicht reduzieren." else "The average strength score (0-100) of all your active habits. Each habit's score is based on the last 30 days, with recent entries weighted slightly higher. Pending tasks for today do not reduce the score.",
                                    onClick = { t, e -> activeExplanation = t to e },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp)
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val sweepHalfVal = (animatedStrength / 100f) * 180f
                            Canvas(
                                modifier = Modifier
                                    .width(290.dp)
                                    .height(150.dp)
                            ) {
                                val strokeWidth = 18.dp.toPx()
                                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                                val arcSize = Size(size.width - strokeWidth, (size.height * 2) - strokeWidth)

                                drawArc(
                                    color = ProgressTrack,
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = stroke
                                )

                                if (sweepHalfVal > 0f) {
                                    drawArc(
                                        color = ringColor,
                                        startAngle = 180f,
                                        sweepAngle = sweepHalfVal,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = stroke
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "${animatedStrength.toInt()}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                val strLabel = getStrengthLabel(strength, language)
                                if (strLabel.isNotBlank()) {
                                    Text(
                                        text = strLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ringColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Account Streaks (Current Streak & Best Streak)
            item(key = "overall_account_streaks_grid") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverallPerfectDaysCard(
                        perfectDaysStats = perfectDaysStats,
                        language = language,
                        accentColor = accentColor,
                        onInfoClick = { t, e -> activeExplanation = t to e }
                    )
                }
            }
            // 3. Activity Heatmap Card
            item(key = "overall_calendar") {
                OverallActivityHeatmapCard(
                    language = language,
                    viewModel = viewModel,
                    heatmapYearGridData = heatmapYearGridData,
                    heatmapYearMonthLabels = heatmapYearMonthLabels,
                    activeCell = activeCell,
                    formattedDate = formattedDate,
                    firstHabitDateStr = firstHabitDateStr,
                    allDailyNotes = allDailyNotes,
                    accentColor = accentColor,
                    onNavigateToToday = onNavigateToToday,
                    onExplanationClick = { t, e -> activeExplanation = t to e }
                )
            }

            // 4. Focus Highlights
            item(key = "overall_focus_highlights") {
                OverallFocusHighlightsCard(
                    habitsWithStats = habitsWithStats,
                    language = language,
                    onExplanationClick = { t, e -> activeExplanation = t to e }
                )
            }
            // 5. Overall Success Rate Card (Donut Chart + Timeframe Switcher Tabs)
            item(key = "overall_success_rate") {
                var selectedTimeframeIndex by rememberSaveable { mutableIntStateOf(0) }
                val overallStats = remember(allHabits, allLogs, selectedTimeframeIndex) {
                    calculateOverallSuccessStats(allHabits, allLogs, selectedTimeframeIndex)
                }

                SuccessRateCard(
                    title = tr(language, "Gesamt-Erfolgsquote", "საერთო წარმატების მაჩვენებელი", "综合成功率", "Overall Success Rate"),
                    doneCount = overallStats.doneCount,
                    missedCount = overallStats.missedCount,
                    skippedCount = overallStats.skippedCount,
                    pendingCount = overallStats.pendingCount,
                    language = language,
                    infoTitle = tr(language, "Gesamt-Erfolgsquote", "საერთო წარმატების მაჩვენებელი", "综合成功率", "Overall Success Rate"),
                    infoExplanation = if (language == "de") "Gesamte Aufschlüsselung aller absolvierten Gewohnheitstage nach Status: Erreicht, Gescheitert, Skipped und Ausstehend." else if (language == "ka") "ყველა დაგეგმილი ჩვევის დღეების საერთო დაყოფა სტატუსის მიხედვით: დასრულებული, გამოტოვებული, გამოტოვებული და მომლოდინე." else "Overall breakdown of all scheduled habit days by status: Done, Missed, Skipped, and Pending.",
                    onInfoClick = { t, e -> activeExplanation = t to e },
                    showTimeframeSelector = true,
                    selectedTimeframeIndex = selectedTimeframeIndex,
                    accentColor = accentColor,
                    onTimeframeSelected = { selectedTimeframeIndex = it }
                )
            }

            // 6. Overall Score Trend Card (Line Chart)
            item(key = "overall_score_trend") {
                OverallScoreTrendCard(
                    allHabits = allHabits,
                    allLogs = allLogs,
                    language = language,
                    accentColor = accentColor,
                    onInfoClick = { t, e -> activeExplanation = t to e }
                )
            }

            // 7. Overall Volume Progression Card (Bar Chart)
            item(key = "overall_volume_progression") {
                OverallVolumeProgressionCard(
                    allHabits = allHabits,
                    allLogs = allLogs,
                    language = language,
                    accentColor = accentColor,
                    onInfoClick = { t, e -> activeExplanation = t to e }
                )
            }
        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }

        if (selectedReviewYear != null) {
            val year = selectedReviewYear!!
            YearlyReviewDialog(
                year = year,
                allHabits = allHabits,
                allLogs = allLogs,
                language = language,
                viewModel = viewModel,
                onDismiss = {
                    viewModel.dismissReview("yearly_${year}")
                    selectedReviewYear = null
                }
            )
        }

        if (selectedReviewMonth != null) {
            val (year, month) = selectedReviewMonth!!
            MonthlyReviewDialog(
                year = year,
                month = month,
                allHabits = allHabits,
                allLogs = allLogs,
                language = language,
                viewModel = viewModel,
                onDismiss = {
                    viewModel.dismissReview("monthly_${year}_${month}")
                    selectedReviewMonth = null
                }
            )
        }

        if (showReviewExplanation) {
            ExplanationDialog(
                title = tr(language, "Rückblicke & Berichte", "მიმოხილვები და ანგარიშები", "回顾与报告", "Reviews & Reports"),
                explanation = if (language == "de") 
                    """Rückblicke sind Zusammenfassungen deiner Gewohnheiten.

• Sie werden immer am 1. Tag des Folgemonats bzw. Folgejahres freigeschaltet.
• Der Score ist der Durchschnitt aller Habit-Stärken zu diesem Zeitpunkt.
• Du kannst Monats- und Jahresrückblicke einzeln in den Einstellungen aktivieren."""
                else 
                    """Reviews are summaries of your habit progress.

• They are unlocked on the 1st day of the following month or year.
• The score is the average of all habit strengths at that time.
• You can toggle Monthly and Yearly reviews independently in settings.""",
                onDismiss = { showReviewExplanation = false }
            )
        }
        
        ReviewArchiveDialog(
            showReviewArchive = showReviewArchive,
            availableYears = availableYears,
            availableMonths = availableMonths,
            allHabits = allHabits,
            allLogs = allLogs,
            language = language,
            onShowReviewExplanation = { showReviewExplanation = true },
            onSelectReviewMonth = { (y, m) ->
                selectedReviewMonth = y to m
                showReviewArchive = false
            },
            onSelectReviewYear = { y ->
                selectedReviewYear = y
                showReviewArchive = false
            },
            onDismiss = { showReviewArchive = false }
        )

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
                    text = tr(language, "Gesamt-Statistiken", "საერთო სტატისტიკა", "综合统计", "Overall Statistics"),
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

