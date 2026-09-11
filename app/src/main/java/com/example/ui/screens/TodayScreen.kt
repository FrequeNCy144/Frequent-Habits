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
import com.example.ui.dialogs.DailyNoteDialog
import com.example.ui.dialogs.MasterGoalDialog
import com.example.ui.screens.profile.ZettelMitStiftIcon
import com.example.ui.screens.profile.BoldPlusIcon
import com.example.ui.screens.today.*
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODAY SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: HabitsViewModel,
    language: String,
    onAddClick: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onOpenHabitDetail: (Habit) -> Unit = {},
    onOpenSettings: (String?) -> Unit = {},
    showCreateFirstHabitHint: Boolean = false,
    onDismissFirstHabitHint: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekStartCalendar by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()
    val dismissedReviews by viewModel.dismissedReviews.collectAsStateWithLifecycle()

    val activeHabitUiItemsForSelectedDate by viewModel.activeHabitUiItemsForSelectedDate.collectAsStateWithLifecycle()
    val justCompletedEvent by viewModel.justCompletedHabitEvent.collectAsStateWithLifecycle()
    val minWeekStartMillis by viewModel.minWeekStartMillis.collectAsStateWithLifecycle()
    val minDateStr by viewModel.minDateStr.collectAsStateWithLifecycle()

    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }
    val hasOnboarded by viewModel.hasOnboarded.collectAsStateWithLifecycle()

    val gameMode by viewModel.gameMode.collectAsStateWithLifecycle()
    val unlockedStorySlots by viewModel.unlockedStorySlots.collectAsStateWithLifecycle()
    val todayDateString by viewModel.todayDateString.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val smartInsightsInAppEnabled by viewModel.smartInsightsInAppEnabled.collectAsStateWithLifecycle()
    val smartInsightDismissedDate by viewModel.smartInsightDismissedDate.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val canPrevWeek by viewModel.canPrevWeek.collectAsStateWithLifecycle()
    val canNextWeek by viewModel.canNextWeek.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allMilestoneRewards by viewModel.allMilestoneRewards.collectAsStateWithLifecycle(initialValue = emptyList())

    val earliestHabitDateStr = remember(allHabits) {
        if (allHabits.isEmpty()) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.format(java.util.Date())
        } else {
            val minStartMs = allHabits.map { 
                if (it.startDate > 946684800000L) it.startDate else it.createdAt 
            }.minOrNull() ?: System.currentTimeMillis()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.format(java.util.Date(minStartMs))
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity

    var todayClickCount by remember { mutableStateOf(0) }
    var showSaskiaDialog by remember { mutableStateOf(false) }
    var showDayOverviewSheet by remember { mutableStateOf(false) }
    var longPressedHabit by remember { mutableStateOf<Habit?>(null) }
    var showDeleteConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var showArchiveConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var manualAddValueHabitId by remember { mutableStateOf<Int?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    var currentlyDraggedHabitId by remember { mutableStateOf<Int?>(null) }
    var originalSortOrders by remember { mutableStateOf<Map<Int, Int>?>(null) }
    var goalToMaster by remember { mutableStateOf<Pair<Habit, Float>?>(null) }
    val dismissedGoalIds by viewModel.dismissedGoalIds.collectAsStateWithLifecycle()

    LaunchedEffect(activeHabitUiItemsForSelectedDate, dismissedGoalIds) {
        val readyGoal = activeHabitUiItemsForSelectedDate.firstOrNull {
            it.isGoalTargetReached && !it.habit.isCompletedGoal && it.habit.id !in dismissedGoalIds
        }
        if (readyGoal != null && goalToMaster == null) {
            goalToMaster = readyGoal.habit to readyGoal.totalAchievedValue
        }
    }

    var localReorderList by remember { mutableStateOf<List<HabitUiItem>?>(null) }
    var totalDragDeltaY by remember { mutableFloatStateOf(0f) }
    var dragStartLayoutOffset by remember { mutableIntStateOf(0) }
    val dragTranslationAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val displayedHabits = if (isReorderMode && localReorderList != null) localReorderList!! else activeHabitUiItemsForSelectedDate

    LaunchedEffect(isReorderMode, activeHabitUiItemsForSelectedDate) {
        if (!isReorderMode) {
            localReorderList = null
        } else {
            if (localReorderList == null) {
                localReorderList = activeHabitUiItemsForSelectedDate
            } else if (currentlyDraggedHabitId == null) {
                val currentOrder = localReorderList!!
                val activeMap = activeHabitUiItemsForSelectedDate.associateBy { it.habit.id }
                val updatedList = currentOrder.mapNotNull { item ->
                    activeMap[item.habit.id]?.copy(habit = item.habit) ?: item
                }
                val existingIds = updatedList.map { it.habit.id }.toSet()
                val newItems = activeHabitUiItemsForSelectedDate.filter { it.habit.id !in existingIds }
                localReorderList = updatedList + newItems
            }
        }
    }

    LaunchedEffect(currentlyDraggedHabitId) {
        val draggedId = currentlyDraggedHabitId
        if (draggedId != null) {
            while (currentlyDraggedHabitId == draggedId) {
                val visibleInfo = listState.layoutInfo.visibleItemsInfo
                val item = visibleInfo.find { it.key == draggedId }
                if (item != null) {
                    val currentVisualTop = item.offset + dragTranslationAnim.value
                    val viewportStart = listState.layoutInfo.viewportStartOffset
                    val viewportEnd = listState.layoutInfo.viewportEndOffset
                    val scrollZone = 120f

                    if (currentVisualTop < viewportStart + scrollZone) {
                        val ratio = ((viewportStart + scrollZone - currentVisualTop) / scrollZone).coerceIn(0f, 1f)
                        val speed = -15f * ratio
                        listState.dispatchRawDelta(speed)
                        dragStartLayoutOffset = (dragStartLayoutOffset - speed).toInt()
                    } else if (currentVisualTop + item.size > viewportEnd - scrollZone) {
                        val ratio = ((currentVisualTop + item.size - (viewportEnd - scrollZone)) / scrollZone).coerceIn(0f, 1f)
                        val speed = 15f * ratio
                        listState.dispatchRawDelta(speed)
                        dragStartLayoutOffset = (dragStartLayoutOffset - speed).toInt()
                    }
                }
                kotlinx.coroutines.delay(16L)
            }
        }
    }

    val allArePaused = remember(activeHabitUiItemsForSelectedDate) {
        activeHabitUiItemsForSelectedDate.isNotEmpty() && activeHabitUiItemsForSelectedDate.all { it.isPaused }
    }
    val progressGlowColor = accentColor

    val onToggleRemembered = remember(viewModel, selectedDate) {
        { habitId: Int, currentlyHasLog: Boolean ->
            viewModel.toggleBinaryHabit(habitId, selectedDate, currentlyHasLog)
        }
    }
    val onAddQuantityRemembered = remember(viewModel, selectedDate) {
        { habitId: Int, currentVal: Float, diff: Float ->
            val newVal = (currentVal + diff).coerceAtLeast(0f)
            viewModel.logNumericalHabit(habitId, selectedDate, newVal)
        }
    }
    val onSelectDayRemembered = remember(viewModel) {
        { dateStr: String ->
            viewModel.selectDate(dateStr)
        }
    }

    val currentWeekDaysData by viewModel.currentWeekDaysData.collectAsStateWithLifecycle()
    val formattedDisplayDate by viewModel.formattedDisplayDate.collectAsStateWithLifecycle()

    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val currentNote = remember(allDailyNotes, selectedDate) {
        allDailyNotes.find { it.date == selectedDate }?.content ?: ""
    }
    var showDailyNoteDialog by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 28.dp)
        ) {
            item(key = "today_top_row") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .defaultMinSize(minHeight = 44.dp)
                ) {
                    // Top Left: Daily Note Button (Notizbuch)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable { showDailyNoteDialog = true }
                            .align(Alignment.CenterStart)
                            .testTag("btn_daily_note"),
                        contentAlignment = Alignment.Center
                    ) {
                        ZettelMitStiftIcon(
                            modifier = Modifier.size(22.dp),
                            color = accentColor
                        )
                        if (currentNote.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                        }
                    }

                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 48.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                todayClickCount++
                                if (todayClickCount >= 10) {
                                    viewModel.unlockSaskia()
                                    showSaskiaDialog = true
                                }
                            },
                        text = formattedDisplayDate,
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Top Right Action Button: Add Habit (Balanced symmetrical layout)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable {
                                onDismissFirstHabitHint()
                                onAddClick()
                            }
                            .align(Alignment.CenterEnd)
                            .testTag("btn_add_habit"),
                        contentAlignment = Alignment.Center
                    ) {
                        BoldPlusIcon(
                            modifier = Modifier.size(18.dp),
                            color = accentColor
                        )
                    }
                }
            }

        item(key = "today_calendar_strip") {
            com.example.ui.screens.today.TodayCalendarStripSection(
                todayDateString = todayDateString,
                minWeekStartMillis = minWeekStartMillis,
                weekStartCalendar = weekStartCalendar,
                selectedDate = selectedDate,
                minDateStr = minDateStr,
                earliestHabitDateStr = earliestHabitDateStr,
                language = language,
                onSelectDay = onSelectDayRemembered,
                onSelectDateAndSyncWeek = { viewModel.selectDateAndSyncWeek(it) },
                accentColor = accentColor
            )
        }

        item(key = "today_progress_card") {
            TodayProgressSummaryCard(
                selectedDate = selectedDate,
                todayDateString = todayDateString,
                todayProgressTuple = todayProgressTuple,
                perfectDaysStats = perfectDaysStats,
                progressGlowColor = progressGlowColor,
                language = language,
                onClick = { showDayOverviewSheet = true }
            )
        }

        item(key = "today_story_insight_banners") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StoryModeBannerCard(
                    gameMode = gameMode,
                    unlockedSlots = unlockedStorySlots,
                    currentHabitCount = allHabits.filter { !it.isArchived }.size,
                    allLogs = allLogs,
                    allHabits = allHabits,
                    language = language,
                    onOpenSettings = { onOpenSettings("appearance") },
                    onNavigateToCreate = onAddClick,
                    onUnlockNextSlot = { viewModel.setUnlockedStorySlots(unlockedStorySlots + 1) }
                )

                if (smartInsightsInAppEnabled) {
                    SmartInsightCard(
                        language = language,
                        userName = userName,
                        todayDateString = todayDateString,
                        smartInsightDismissedDate = smartInsightDismissedDate,
                        onDismiss = { viewModel.dismissSmartInsight(it) },
                        perfectDaysStats = perfectDaysStats,
                        habitsWithStats = habitsWithStats,
                        allLogs = allLogs,
                        allHabits = allHabits,
                        dismissedReviews = dismissedReviews,
                        onDismissReview = { viewModel.dismissReview(it) },
                        onOpenMonthlyReview = { year, month -> viewModel.setDeepLinkReviewMonth(year to month) },
                        onOpenYearlyReview = { year -> viewModel.setDeepLinkReviewYear(year) }
                    )
                }
            }
        }

        if (isReorderMode) {
            item(key = "today_reorder_banner") {
                TodayReorderBannerCard(
                    language = language,
                    onCancel = {
                        originalSortOrders?.let { savedOrders ->
                            viewModel.revertHabitOrders(savedOrders)
                        }
                        isReorderMode = false
                        localReorderList = null
                    },
                    onDone = {
                        localReorderList?.let { list ->
                            viewModel.saveNewHabitOrder(list.map { it.habit.id })
                        }
                        isReorderMode = false
                        localReorderList = null
                    }
                )
            }
        }

        if (displayedHabits.isEmpty()) {
            item(key = "today_habits_empty") {
                TodayEmptyStateCard(language = language)
            }
        } else {
            items(
                items = displayedHabits, 
                key = { it.habit.id },
                contentType = { "HABIT_CARD" }
            ) { uiItem ->
                val currentHabit = uiItem.habit
                val isBeingDragged = currentlyDraggedHabitId == currentHabit.id
                
                val onLongClickRemembered = remember(currentHabit.id) {
                    { habit: Habit -> longPressedHabit = habit }
                }

                val onToggleClick = remember(currentHabit.id, currentHabit.type, currentHabit.unit, onToggleRemembered) {
                    { habitId: Int, hasLog: Boolean ->
                        val isNumerical = currentHabit.type == "NUMBER" || currentHabit.type == "NUMERICAL" || (currentHabit.targetValue > 1f && currentHabit.type != "BINARY")
                        if (isNumerical) {
                            manualAddValueHabitId = habitId
                        } else {
                            onToggleRemembered(habitId, hasLog)
                        }
                    }
                }

                val anchorHabit = currentHabit.stackedOnHabitId?.let { anchorId ->
                    displayedHabits.find { it.habit.id == anchorId }
                }
                val anchorName = anchorHabit?.habit?.name
                val anchorCompleted = anchorHabit?.isCompleted == true
                val isAnchorJustCompleted = anchorCompleted &&
                    currentHabit.stackedOnHabitId != null &&
                    justCompletedEvent?.first == currentHabit.stackedOnHabitId &&
                    (System.currentTimeMillis() - (justCompletedEvent?.second ?: 0L)) < 4000L

                HabitItemRow(
                    modifier = if (isBeingDragged) {
                        Modifier.zIndex(1000f)
                    } else {
                        Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    },
                    habit = currentHabit,
                    currentValue = uiItem.currentValue,
                    isCompleted = uiItem.isCompleted,
                    isFailed = uiItem.isFailed,
                    isPaused = uiItem.isPaused,
                    hasLog = uiItem.hasLog,
                    isMinimalViable = uiItem.isMinimalViable,
                    onToggle = onToggleClick,
                    onAddQuantity = onAddQuantityRemembered,
                    onLongClick = onLongClickRemembered,
                    language = language,
                    vibrationEnabled = vibrationEnabled,
                    selectedDate = selectedDate,
                    isWeeklyTargetReached = uiItem.isWeeklyTargetReached,
                    weeklyLoggedCount = uiItem.weeklyLoggedCount,
                    weeklyTargetCount = uiItem.weeklyTargetCount,
                    streak = uiItem.streak,
                    isReorderMode = isReorderMode,
                    anchorName = anchorName,
                    anchorCompleted = anchorCompleted,
                    anchorJustCompleted = isAnchorJustCompleted,
                    onMoveUp = { habitId ->
                        val newOrder = com.example.ui.screens.today.HabitReorderHelper.moveHabitInList(displayedHabits, habitId, moveUp = true)
                        if (newOrder != displayedHabits) {
                            localReorderList = newOrder
                            viewModel.saveNewHabitOrder(newOrder.map { it.habit.id })
                        }
                    },
                    onMoveDown = { habitId ->
                        val newOrder = com.example.ui.screens.today.HabitReorderHelper.moveHabitInList(displayedHabits, habitId, moveUp = false)
                        if (newOrder != displayedHabits) {
                            localReorderList = newOrder
                            viewModel.saveNewHabitOrder(newOrder.map { it.habit.id })
                        }
                    },
                    listState = listState,
                    isBeingDragged = isBeingDragged,
                    dragTranslationY = if (isBeingDragged) dragTranslationAnim.value else 0f,
                    onDragStart = {
                        currentlyDraggedHabitId = currentHabit.id
                        totalDragDeltaY = 0f
                        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == currentHabit.id }
                        dragStartLayoutOffset = itemInfo?.offset ?: 0
                        coroutineScope.launch {
                            dragTranslationAnim.snapTo(0f)
                        }
                    },
                    onDragDelta = { delta ->
                        if (currentlyDraggedHabitId == currentHabit.id) {
                            totalDragDeltaY += delta
                            val currentList = displayedHabits
                            val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == currentHabit.id }
                            val currentLayoutOffset = itemInfo?.offset ?: dragStartLayoutOffset
                            val currentItemSize = itemInfo?.size ?: 100

                            val desiredVisualTop = dragStartLayoutOffset + totalDragDeltaY
                            val desiredVisualCenter = desiredVisualTop + (currentItemSize / 2f)
                            val currentTranslationY = desiredVisualTop - currentLayoutOffset

                            coroutineScope.launch {
                                dragTranslationAnim.snapTo(currentTranslationY)
                            }

                            val newOrder = com.example.ui.screens.today.HabitReorderHelper.findTargetSwap(
                                currentList = currentList,
                                draggedHabitId = currentHabit.id,
                                draggedVisualCenter = desiredVisualCenter,
                                visibleItems = listState.layoutInfo.visibleItemsInfo
                            )
                            if (newOrder != null) {
                                localReorderList = newOrder
                                viewModel.saveNewHabitOrder(newOrder.map { it.habit.id })
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentlyDraggedHabitId == currentHabit.id) {
                            val finalList = displayedHabits
                            coroutineScope.launch {
                                dragTranslationAnim.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                )
                                currentlyDraggedHabitId = null
                                viewModel.saveNewHabitOrder(finalList.map { it.habit.id })
                            }
                        }
                    },
                    totalAchievedValue = uiItem.totalAchievedValue,
                    isGoalTargetReached = uiItem.isGoalTargetReached,
                    onCompleteGoal = { habitToComplete ->
                        goalToMaster = habitToComplete to uiItem.totalAchievedValue
                    }
                )
            }
        }
    } // Close LazyColumn

    if (showDayOverviewSheet) {
        val eligibleMvHabits = activeHabitUiItemsForSelectedDate.filter { 
            (it.habit.minimalViableValue != null && it.habit.minimalViableValue > 0f) || it.habit.minimalViableText.isNotBlank()
        }
        val hasMinimalViableHabits = eligibleMvHabits.isNotEmpty()
        val allAreMinimalViable = hasMinimalViableHabits && eligibleMvHabits.all { it.isMinimalViable }

        DayOverviewBottomSheet(
            language = language,
            selectedDate = selectedDate,
            allArePaused = allArePaused,
            hasMinimalViableHabits = hasMinimalViableHabits,
            allAreMinimalViable = allAreMinimalViable,
            onTogglePauseAll = {
                viewModel.togglePauseAllHabitsForSelectedDate()
                showDayOverviewSheet = false
            },
            onToggleMinimalViableDay = {
                viewModel.toggleMinimalViableAllHabitsForSelectedDate()
                showDayOverviewSheet = false
            },
            onStartReorder = {
                showDayOverviewSheet = false
                val habitsList = activeHabitUiItemsForSelectedDate.map { it.habit }
                originalSortOrders = habitsList.associate { it.id to it.sortOrder }
                localReorderList = activeHabitUiItemsForSelectedDate
                isReorderMode = true
            },
            onDismiss = { showDayOverviewSheet = false }
        )
    }
    if (longPressedHabit != null) {
        val habit = longPressedHabit!!
        val currentUiItem = activeHabitUiItemsForSelectedDate.find { it.habit.id == habit.id }
        val isPausedOnSelectedDate = currentUiItem?.isPaused == true
        val isMinimalViableOnSelectedDate = currentUiItem?.isMinimalViable == true

        HabitActionBottomSheet(
            habit = habit,
            language = language,
            isPausedOnSelectedDate = isPausedOnSelectedDate,
            isMinimalViableOnSelectedDate = isMinimalViableOnSelectedDate,
            onTogglePause = {
                viewModel.togglePauseHabit(habit.id)
                longPressedHabit = null
            },
            onToggleMinimalViable = {
                viewModel.toggleMinimalViableHabit(habit.id)
                longPressedHabit = null
            },
            onEditHabit = {
                val habitToEdit = habit
                longPressedHabit = null
                onEditHabit(habitToEdit)
            },
            onArchiveHabit = {
                val habitToArchive = habit
                longPressedHabit = null
                showArchiveConfirmHabit = habitToArchive
            },
            onDeleteHabit = {
                val habitToDelete = habit
                longPressedHabit = null
                showDeleteConfirmHabit = habitToDelete
            },
            onDismiss = { longPressedHabit = null }
        )
    }
    showDeleteConfirmHabit?.let { habitToDelete ->
        TodayDeleteConfirmDialog(
            habit = habitToDelete,
            viewModel = viewModel,
            language = language,
            onDismiss = { showDeleteConfirmHabit = null }
        )
    }

    showArchiveConfirmHabit?.let { habitToArchive ->
        TodayArchiveConfirmDialog(
            habit = habitToArchive,
            viewModel = viewModel,
            language = language,
            onDismiss = { showArchiveConfirmHabit = null }
        )
    }

    if (manualAddValueHabitId != null) {
        WidgetAddValueDialog(
            habitId = manualAddValueHabitId!!,
            viewModel = viewModel,
            language = language,
            onDismiss = { manualAddValueHabitId = null }
        )
    }

    if (showCreateFirstHabitHint && activeHabitUiItemsForSelectedDate.isEmpty()) {
        CreateFirstHabitArrowHint(
            language = language
        )
    }

    if (showSaskiaDialog) {
        TodaySaskiaUnlockDialog(
            language = language,
            onDismiss = { showSaskiaDialog = false }
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

    goalToMaster?.let { (habit, totalAchieved) ->
        val matchingReward = allMilestoneRewards.find { it.habitId == habit.id }
        MasterGoalDialog(
            habit = habit,
            totalAchievedValue = totalAchieved,
            rewardTitle = matchingReward?.rewardText,
            language = language,
            onDismiss = {
                val hId = goalToMaster?.first?.id
                if (hId != null) {
                    viewModel.dismissGoalDialog(hId)
                }
                goalToMaster = null
            },
            onConfirmMastered = { note, claimReward ->
                val hId = habit.id
                viewModel.completeFinishableGoal(hId, note, claimReward = claimReward)
                viewModel.dismissGoalDialog(hId)
                goalToMaster = null
            }
        )
    }
}
}

