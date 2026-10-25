package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.data.HabitUiItem
import com.example.ui.*
import com.example.ui.components.CreateFirstHabitArrowHint
import com.example.ui.screens.today.*
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
    viewModel: HabitsViewModel,
    language: String,
    onAddClick: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onOpenSettings: (String) -> Unit,
    showCreateFirstHabitHint: Boolean,
    onDismissFirstHabitHint: () -> Unit,
    onNavigateToReorder: (() -> Unit)? = null,
    isReorderMode: Boolean = false,
    onReorderModeChange: (Boolean) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val activeHabitUiItemsForSelectedDate by viewModel.activeHabitUiItemsForSelectedDate.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgressTuple.collectAsStateWithLifecycle()
    val justCompletedEvent by viewModel.justCompletedEvent.collectAsStateWithLifecycle()
    val allHabits by viewModel.habits.collectAsStateWithLifecycle()
    val allLogs by viewModel.logs.collectAsStateWithLifecycle()
    val gameMode by viewModel.gameMode.collectAsStateWithLifecycle()
    val unlockedStorySlots by viewModel.unlockedStorySlots.collectAsStateWithLifecycle()
    val smartInsightsInAppEnabled by viewModel.smartInsightsInAppEnabled.collectAsStateWithLifecycle()
    val smartInsightDismissedDate by viewModel.smartInsightDismissedDate.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.habitsWithStats.collectAsStateWithLifecycle()
    val dismissedReviews by viewModel.dismissedReviews.collectAsStateWithLifecycle()
    val allMilestoneRewards by viewModel.milestoneRewards.collectAsStateWithLifecycle()
    val todayDateString by viewModel.todayDateString.collectAsStateWithLifecycle()
    val minWeekStartMillis by viewModel.minWeekStartMillis.collectAsStateWithLifecycle()
    val weekStartCalendar by viewModel.weekStartCalendar.collectAsStateWithLifecycle()
    val minDateStr by viewModel.minDateStr.collectAsStateWithLifecycle()
    val earliestHabitDateStr by viewModel.earliestHabitDateStr.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    val formattedDisplayDate by viewModel.formattedDisplayDate.collectAsStateWithLifecycle()
    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val dismissedGoalIds by viewModel.dismissedGoalIds.collectAsStateWithLifecycle()

    var showDayOverviewSheet by remember { mutableStateOf(false) }
    var longPressedHabit by remember { mutableStateOf<Habit?>(null) }
    var showDeleteConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var showArchiveConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var manualAddValueHabitId by remember { mutableStateOf<Int?>(null) }
    var showSaskiaDialog by remember { mutableStateOf(false) }
    var todayClickCount by remember { mutableIntStateOf(0) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<HabitFolder?>(null) }
    var longPressedFolder by remember { mutableStateOf<HabitFolder?>(null) }

    var currentlyDraggedHabitId by remember { mutableStateOf<Int?>(null) }
    var originalSortOrders by remember { mutableStateOf<Map<Int, Int>?>(null) }
    var goalToMaster by remember { mutableStateOf<Pair<Habit, Float>?>(null) }

    var localReorderList by remember { mutableStateOf<List<HabitUiItem>?>(null) }
    var totalDragDeltaY by remember { mutableFloatStateOf(0f) }
    var dragStartLayoutOffset by remember { mutableIntStateOf(0) }
    val dragTranslationAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val displayedHabits = if (isReorderMode && localReorderList != null) localReorderList!! else activeHabitUiItemsForSelectedDate
    val displayItems = remember(displayedHabits, allFolders, selectedDate, todayDateString) {
        buildTodayDisplayItems(displayedHabits, allFolders, isToday = (selectedDate == todayDateString))
    }

    LaunchedEffect(activeHabitUiItemsForSelectedDate, dismissedGoalIds) {
        val readyGoal = activeHabitUiItemsForSelectedDate.firstOrNull {
            it.isGoalTargetReached && !it.habit.isCompletedGoal && it.habit.id !in dismissedGoalIds
        }
        if (readyGoal != null && goalToMaster == null) {
            goalToMaster = readyGoal.habit to readyGoal.totalAchievedValue
        }
    }

    LaunchedEffect(isReorderMode, activeHabitUiItemsForSelectedDate, allHabits) {
        if (!isReorderMode) {
            localReorderList = null
        } else {
            val allActive = allHabits.filter { !it.isArchived && !(it.isFinishable && it.isCompletedGoal) }
            val activeMap = activeHabitUiItemsForSelectedDate.associateBy { it.habit.id }
            val fullList = allActive.map { habit ->
                activeMap[habit.id] ?: HabitUiItem(
                    habit = habit,
                    currentValue = 0f,
                    isCompleted = false,
                    isFailed = false,
                    isPaused = false,
                    hasLog = false,
                    isMinimalViable = false
                )
            }
            if (localReorderList == null) {
                localReorderList = fullList
            } else if (currentlyDraggedHabitId == null) {
                val currentOrder = localReorderList!!
                val fullMap = fullList.associateBy { it.habit.id }
                val updatedList = currentOrder.mapNotNull { item ->
                    fullMap[item.habit.id]?.copy(habit = item.habit) ?: item
                }
                val existingIds = updatedList.map { it.habit.id }.toSet()
                val newItems = fullList.filter { it.habit.id !in existingIds }
                localReorderList = updatedList + newItems
            }
        }
    }

    LaunchedEffect(currentlyDraggedHabitId) {
        val draggedId = currentlyDraggedHabitId
        if (draggedId != null) {
            while (currentlyDraggedHabitId == draggedId) {
                val visibleInfo = listState.layoutInfo.visibleItemsInfo
                val item = visibleInfo.find { it.key == "loose_$draggedId" || it.key == draggedId }
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
    val currentNote = remember(allDailyNotes, selectedDate) {
        allDailyNotes.find { it.date == selectedDate }?.content ?: ""
    }
    var showDailyNoteDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = isReorderMode) {
        originalSortOrders?.let { savedOrders -> viewModel.revertHabitOrders(savedOrders) }
        onReorderModeChange(false)
        localReorderList = null
    }

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
        TodayHabitsLazyList(
            listState = listState,
            formattedDisplayDate = formattedDisplayDate,
            hasDailyNote = currentNote.isNotEmpty(),
            accentColor = accentColor,
            language = language,
            todayDateString = todayDateString,
            minWeekStartMillis = minWeekStartMillis,
            weekStartCalendar = weekStartCalendar,
            selectedDate = selectedDate,
            minDateStr = minDateStr,
            earliestHabitDateStr = earliestHabitDateStr,
            todayProgressTuple = todayProgressTuple,
            perfectDaysStats = perfectDaysStats,
            progressGlowColor = accentColor,
            gameMode = gameMode,
            unlockedStorySlots = unlockedStorySlots,
            allHabits = allHabits,
            allLogs = allLogs,
            smartInsightsInAppEnabled = smartInsightsInAppEnabled,
            userName = userName,
            smartInsightDismissedDate = smartInsightDismissedDate,
            habitsWithStats = habitsWithStats,
            dismissedReviews = dismissedReviews,
            isReorderMode = isReorderMode,
            displayItems = displayItems,
            displayedHabits = displayedHabits,
            currentlyDraggedHabitId = currentlyDraggedHabitId,
            dragTranslationY = dragTranslationAnim.value,
            justCompletedEvent = justCompletedEvent,
            vibrationEnabled = vibrationEnabled,
            viewModel = viewModel,
            onOpenDailyNote = { showDailyNoteDialog = true },
            onTitleClick = {
                todayClickCount++
                if (todayClickCount >= 10) {
                    viewModel.unlockSaskia()
                    showSaskiaDialog = true
                }
            },
            onAddHabit = {
                onDismissFirstHabitHint()
                onAddClick()
            },
            onAddFolder = {
                editingFolder = null
                showFolderDialog = true
            },
            onSelectDay = { viewModel.selectDate(it) },
            onOpenDayOverview = { showDayOverviewSheet = true },
            onOpenSettings = onOpenSettings,
            onCancelReorder = {
                originalSortOrders?.let { savedOrders -> viewModel.revertHabitOrders(savedOrders) }
                onReorderModeChange(false)
                localReorderList = null
            },
            onDoneReorder = {
                localReorderList?.let { list ->
                    viewModel.saveNewHabitOrder(list.map { it.habit.id })
                }
                onReorderModeChange(false)
                localReorderList = null
            },
            onHabitLongClick = { habit -> longPressedHabit = habit },
            onManualAddValue = { habitId -> manualAddValueHabitId = habitId },
            onGoalMastered = { habit, achievedVal -> goalToMaster = habit to achievedVal },
            onMoveHabitUp = { habitId ->
                val currentList = localReorderList ?: activeHabitUiItemsForSelectedDate
                val updated = HabitReorderHelper.moveHabitInList(currentList, habitId, moveUp = true)
                localReorderList = updated
                viewModel.saveNewHabitOrder(updated.map { it.habit.id })
            },
            onMoveHabitDown = { habitId ->
                val currentList = localReorderList ?: activeHabitUiItemsForSelectedDate
                val updated = HabitReorderHelper.moveHabitInList(currentList, habitId, moveUp = false)
                localReorderList = updated
                viewModel.saveNewHabitOrder(updated.map { it.habit.id })
            },
            onMoveFolderUp = { folderId ->
                val currentList = localReorderList ?: activeHabitUiItemsForSelectedDate
                val updated = HabitReorderHelper.moveFolderInList(currentList, folderId, moveUp = true)
                localReorderList = updated
                viewModel.saveNewHabitOrder(updated.map { it.habit.id })
                viewModel.moveFolder(folderId, up = true)
            },
            onMoveFolderDown = { folderId ->
                val currentList = localReorderList ?: activeHabitUiItemsForSelectedDate
                val updated = HabitReorderHelper.moveFolderInList(currentList, folderId, moveUp = false)
                localReorderList = updated
                viewModel.saveNewHabitOrder(updated.map { it.habit.id })
                viewModel.moveFolder(folderId, up = false)
            },
            onDragStart = { habitId ->
                currentlyDraggedHabitId = habitId
                totalDragDeltaY = 0f
                val layoutInfo = listState.layoutInfo
                val item = layoutInfo.visibleItemsInfo.find { it.key == "loose_$habitId" || it.key == habitId }
                dragStartLayoutOffset = item?.offset ?: 0
                coroutineScope.launch { dragTranslationAnim.snapTo(0f) }
            },
            onDragDelta = { dragDelta ->
                totalDragDeltaY += dragDelta
                coroutineScope.launch { dragTranslationAnim.snapTo(totalDragDeltaY) }
                val currentList = localReorderList ?: displayedHabits
                val draggedIndex = currentList.indexOfFirst { it.habit.id == currentlyDraggedHabitId }
                if (draggedIndex != -1) {
                    val approxItemHeight = 72f
                    val targetIndex = (draggedIndex + (totalDragDeltaY / approxItemHeight).toInt())
                        .coerceIn(0, currentList.size - 1)
                    if (targetIndex != draggedIndex) {
                        val mutableList = currentList.toMutableList()
                        val item = mutableList.removeAt(draggedIndex)
                        mutableList.add(targetIndex, item)
                        val reordered = mutableList.mapIndexed { i, uiItem ->
                            uiItem.copy(habit = uiItem.habit.copy(sortOrder = i))
                        }
                        localReorderList = reordered
                        totalDragDeltaY -= (targetIndex - draggedIndex) * approxItemHeight
                        coroutineScope.launch { dragTranslationAnim.snapTo(totalDragDeltaY) }
                    }
                }
            },
            onDragEnd = {
                currentlyDraggedHabitId = null
                coroutineScope.launch { dragTranslationAnim.animateTo(0f) }
                localReorderList?.let { list ->
                    viewModel.saveNewHabitOrder(list.map { it.habit.id })
                }
            },
            onEditFolder = { folder ->
                longPressedFolder = folder
            }
        )

        if (showCreateFirstHabitHint && activeHabitUiItemsForSelectedDate.isEmpty()) {
            CreateFirstHabitArrowHint(
                language = language,
                onDismiss = onDismissFirstHabitHint
            )
        }

        TodayDialogs(
            viewModel = viewModel,
            language = language,
            selectedDate = selectedDate,
            currentNote = currentNote,
            accentColor = accentColor,
            activeHabitUiItemsForSelectedDate = activeHabitUiItemsForSelectedDate,
            allMilestoneRewards = allMilestoneRewards,
            allArePaused = allArePaused,
            showDailyNoteDialog = showDailyNoteDialog,
            onDismissDailyNoteDialog = { showDailyNoteDialog = false },
            showFolderDialog = showFolderDialog,
            editingFolder = editingFolder,
            onDismissFolderDialog = { showFolderDialog = false },
            onRequestCreateFolder = {
                editingFolder = null
                showFolderDialog = true
            },
            showDayOverviewSheet = showDayOverviewSheet,
            onDismissDayOverviewSheet = { showDayOverviewSheet = false },
            onStartReorder = {
                showDayOverviewSheet = false
                if (onNavigateToReorder != null) {
                    onNavigateToReorder.invoke()
                } else {
                    val allActive = allHabits.filter { !it.isArchived && !(it.isFinishable && it.isCompletedGoal) }
                    originalSortOrders = allActive.associate { it.id to it.sortOrder }
                    val activeMap = activeHabitUiItemsForSelectedDate.associateBy { it.habit.id }
                    localReorderList = allActive.map { habit ->
                        activeMap[habit.id] ?: HabitUiItem(
                            habit = habit,
                            currentValue = 0f,
                            isCompleted = false,
                            isFailed = false,
                            isPaused = false,
                            hasLog = false,
                            isMinimalViable = false
                        )
                    }
                    onReorderModeChange(true)
                }
            },
            longPressedHabit = longPressedHabit,
            onDismissLongPressAction = { longPressedHabit = null },
            onEditHabit = onEditHabit,
            showDeleteConfirmHabit = showDeleteConfirmHabit,
            onDismissDeleteConfirm = { showDeleteConfirmHabit = null },
            showArchiveConfirmHabit = showArchiveConfirmHabit,
            onDismissArchiveConfirm = { showArchiveConfirmHabit = null },
            manualAddValueHabitId = manualAddValueHabitId,
            onDismissManualAddValue = { manualAddValueHabitId = null },
            showSaskiaDialog = showSaskiaDialog,
            onDismissSaskiaDialog = { showSaskiaDialog = false },
            goalToMaster = goalToMaster,
            onDismissGoalDialog = { habitId ->
                if (habitId != null) {
                    viewModel.dismissGoalDialog(habitId)
                }
                goalToMaster = null
            },
            onRequestArchiveHabit = { habitToArchive -> showArchiveConfirmHabit = habitToArchive },
            onRequestDeleteHabit = { habitToDelete -> showDeleteConfirmHabit = habitToDelete }
        )

        if (longPressedFolder != null) {
            FolderActionBottomSheet(
                folder = longPressedFolder!!,
                language = language,
                onEditFolder = {
                    editingFolder = longPressedFolder
                    showFolderDialog = true
                    longPressedFolder = null
                },
                onDeleteFolder = {
                    viewModel.deleteFolder(longPressedFolder!!)
                    longPressedFolder = null
                },
                onDismiss = {
                    longPressedFolder = null
                }
            )
        }
    }
}
