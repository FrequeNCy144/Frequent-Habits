package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.frequent.habits.R
import com.example.data.*
import com.example.ui.*
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.stats.*
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// STATS SCREEN
@Composable
fun StatsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onAddClick: () -> Unit,
    onHabitClick: (Int) -> Unit,
    onOverallClick: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreakOfAll.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekStartCalendar by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val minWeekStartMillis by viewModel.minWeekStartMillis.collectAsStateWithLifecycle()
    val minDateStr by viewModel.minDateStr.collectAsStateWithLifecycle()
    val currentWeekDaysData by viewModel.currentWeekDaysData.collectAsStateWithLifecycle()
    val formattedDisplayDate by viewModel.formattedDisplayDate.collectAsStateWithLifecycle()

    val todayDateString by viewModel.todayDateString.collectAsStateWithLifecycle()
    val canPrevWeek by viewModel.canPrevWeek.collectAsStateWithLifecycle()
    val canNextWeek by viewModel.canNextWeek.collectAsStateWithLifecycle()
    val statsDayNamesAndNumbers by viewModel.statsDayNamesAndNumbers.collectAsStateWithLifecycle()
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }

    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val currentNote = remember(allDailyNotes, selectedDate) {
        allDailyNotes.find { it.date == selectedDate }?.content ?: ""
    }

    val allMilestoneRewards by viewModel.allMilestoneRewards.collectAsStateWithLifecycle(initialValue = emptyList())
    val redeemableRewardsCount = remember(allMilestoneRewards) {
        allMilestoneRewards.count { it.unlockedAt > 0L && !it.isRedeemed }
    }

    var showDailyNoteDialog by remember { mutableStateOf(false) }

    val onSelectDayRemembered = remember(viewModel) {
        { dateStr: String ->
            viewModel.selectDate(dateStr)
        }
    }

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isPastDay = remember(selectedDate, todayDateString) {
        selectedDate < todayDateString
    }

    val encouragementText = remember(todayProgressTuple, language) {
        getEncouragementText(todayProgressTuple.first, todayProgressTuple.second, language)
    }

    val onStatItemClick = remember(onHabitClick) {
        { habitId: Int ->
            onHabitClick(habitId)
        }
    }

    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    var selectedReviewYear by remember { mutableStateOf<Int?>(null) }
    var selectedReviewMonth by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showReviewArchive by remember { mutableStateOf(false) }
    var showRewardsOverview by remember { mutableStateOf(false) }
    var showReviewExplanation by remember { mutableStateOf(false) }

    val deepLinkReviewMonth by viewModel.deepLinkReviewMonth.collectAsStateWithLifecycle()
    val deepLinkReviewYear by viewModel.deepLinkReviewYear.collectAsStateWithLifecycle()

    LaunchedEffect(deepLinkReviewMonth) {
        deepLinkReviewMonth?.let {
            selectedReviewMonth = it
            viewModel.setDeepLinkReviewMonth(null)
        }
    }

    LaunchedEffect(deepLinkReviewYear) {
        deepLinkReviewYear?.let {
            selectedReviewYear = it
            viewModel.setDeepLinkReviewYear(null)
        }
    }
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

    val shortDayNames = statsDayNamesAndNumbers.first
    val dayNumbers = statsDayNamesAndNumbers.second

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 28.dp)
        ) {
            item(key = "stats_top_row") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .defaultMinSize(minHeight = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, PrimaryViolet.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .background(PrimaryViolet.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable { showRewardsOverview = true }
                            .align(Alignment.CenterStart)
                            .testTag("btn_milestones_overview"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Meilensteine",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )

                        if (redeemableRewardsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(HabitStreakFlame, CircleShape)
                                    .border(1.5.dp, AppBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (redeemableRewardsCount > 9) "9+" else "$redeemableRewardsCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = tr(language, "Statistiken", "სტატისტიკა", "统计", "Statistics"),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable { showReviewArchive = true }
                            .align(Alignment.CenterEnd)
                            .testTag("btn_review_archive"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Rückblicke",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Subheading: Overall Statistiken
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(accentColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Overall Statistiken", "საერთო სტატისტიკა", "综合统计", "Overall Statistics"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Beautifully designed Overall Statistics Card with enlarged semi-circle gauge
            item {
                StatsOverallCard(
                    strength = strength,
                    language = language,
                    accentColor = accentColor,
                    onOverallClick = onOverallClick
                )
            }

            // Subheading 2: Habits im Detail
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(accentColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Habits im Detail", "ჩვევები დეტალურად", "习惯明细", "Habits in Detail"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (habitsWithStats.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tr(language, "Keine Gewohnheiten verfügbar", "არ არის ხელმისაწვდომი ჩვევები", "暂无可用习惯", "No habits available"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddClick,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(language, "Gewohnheit erstellen", "შექმენი ჩვევა", "创建习惯", "Create Habit"),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(
                    items = habitsWithStats, 
                    key = { it.habit.id },
                    contentType = { "STAT_CARD" }
                ) { model ->
                    HabitStatItem(
                        model = model,
                        shortDayNames = shortDayNames,
                        dayNumbers = dayNumbers,
                        language = language,
                        accentColor = accentColor,
                        onClick = onStatItemClick
                    )
                }
            }
        } // Close LazyColumn

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }

        if (showDailyNoteDialog) {
            DailyNoteDialog(
                currentNote = currentNote,
                language = language,
                onDismiss = { showDailyNoteDialog = false },
                onSave = { noteText ->
                    viewModel.saveDailyNote(selectedDate, noteText)
                    showDailyNoteDialog = false
                }
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

        if (showRewardsOverview) {
            com.example.ui.dialogs.RewardsOverviewSheet(
                viewModel = viewModel,
                language = language,
                onDismiss = { showRewardsOverview = false }
            )
        }
    }
}
