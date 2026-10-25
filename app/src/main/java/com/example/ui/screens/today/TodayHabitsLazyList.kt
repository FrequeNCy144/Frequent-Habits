package com.example.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.theme.AppBg

@Composable
fun TodayHabitsLazyList(
    listState: LazyListState,
    formattedDisplayDate: String,
    hasDailyNote: Boolean,
    accentColor: Color,
    language: String,
    todayDateString: String,
    minWeekStartMillis: Long,
    weekStartCalendar: java.util.Calendar,
    selectedDate: String,
    minDateStr: String,
    earliestHabitDateStr: String,
    todayProgressTuple: Pair<Int, Int>,
    perfectDaysStats: PerfectDaysStats,
    progressGlowColor: Color,
    gameMode: String,
    unlockedStorySlots: Int,
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    smartInsightsInAppEnabled: Boolean,
    userName: String,
    smartInsightDismissedDate: String,
    habitsWithStats: List<HabitWithStats>,
    dismissedReviews: Set<String>,
    isReorderMode: Boolean,
    displayItems: List<TodayDisplayItem>,
    displayedHabits: List<HabitUiItem>,
    currentlyDraggedHabitId: Int?,
    dragTranslationY: Float,
    justCompletedEvent: Pair<Int, Long>?,
    vibrationEnabled: Boolean,
    viewModel: HabitsViewModel,
    onOpenDailyNote: () -> Unit,
    onTitleClick: () -> Unit,
    onAddHabit: () -> Unit,
    onAddFolder: () -> Unit,
    onSelectDay: (String) -> Unit,
    onOpenDayOverview: () -> Unit,
    onOpenSettings: (String) -> Unit,
    onCancelReorder: () -> Unit,
    onDoneReorder: () -> Unit,
    onHabitLongClick: (Habit) -> Unit,
    onManualAddValue: (Int) -> Unit,
    onGoalMastered: (Habit, Float) -> Unit,
    onDragStart: (Int) -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onEditFolder: (HabitFolder) -> Unit,
    onMoveHabitUp: ((Int) -> Unit)? = null,
    onMoveHabitDown: ((Int) -> Unit)? = null,
    onMoveFolderUp: ((Int) -> Unit)? = null,
    onMoveFolderDown: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coloredFolderCards by viewModel.coloredFolderCardsEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 140.dp, top = 28.dp)
    ) {
        item(key = "today_top_row") {
            TodayHeader(
                formattedDisplayDate = formattedDisplayDate,
                hasDailyNote = hasDailyNote,
                accentColor = accentColor,
                language = language,
                onOpenDailyNote = onOpenDailyNote,
                onTitleClick = onTitleClick,
                onAddHabit = onAddHabit,
                onAddFolder = onAddFolder
            )
        }

        item(key = "today_calendar_strip") {
            TodayCalendarStripSection(
                todayDateString = todayDateString,
                minWeekStartMillis = minWeekStartMillis,
                weekStartCalendar = weekStartCalendar,
                selectedDate = selectedDate,
                minDateStr = minDateStr,
                earliestHabitDateStr = earliestHabitDateStr,
                language = language,
                onSelectDay = onSelectDay,
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
                onClick = onOpenDayOverview
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
                    onNavigateToCreate = onAddHabit,
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
                    onCancel = onCancelReorder,
                    onDone = onDoneReorder
                )
            }
        }

        if (displayItems.isEmpty()) {
            item(key = "today_habits_empty") {
                TodayEmptyStateCard(language = language)
            }
        } else {
            items(
                items = displayItems,
                key = { item ->
                    when (item) {
                        is TodayDisplayItem.LooseHabit -> "loose_${item.uiItem.habit.id}"
                        is TodayDisplayItem.FolderGroup -> "folder_${item.folder.id}"
                    }
                },
                contentType = { item ->
                    when (item) {
                        is TodayDisplayItem.LooseHabit -> "LOOSE_HABIT_CARD"
                        is TodayDisplayItem.FolderGroup -> "FOLDER_GROUP_CARD"
                    }
                }
            ) { displayItem ->
                when (displayItem) {
                    is TodayDisplayItem.LooseHabit -> {
                        val uiItem = displayItem.uiItem
                        val isBeingDragged = currentlyDraggedHabitId == uiItem.habit.id
                        TodayHabitRowItem(
                            uiItem = uiItem,
                            displayedHabits = displayedHabits,
                            justCompletedEvent = justCompletedEvent,
                            isNested = false,
                            isReorderMode = isReorderMode,
                            isBeingDragged = isBeingDragged,
                            dragTranslationY = dragTranslationY,
                            listState = listState,
                            selectedDate = selectedDate,
                            language = language,
                            vibrationEnabled = vibrationEnabled,
                            onToggle = { hId, hasLog -> viewModel.toggleBinaryHabit(hId, selectedDate, hasLog) },
                            onManualAddValue = onManualAddValue,
                            onAddQuantity = { hId, currentVal, diff ->
                                val newVal = (currentVal + diff).coerceAtLeast(0f)
                                viewModel.logNumericalHabit(hId, selectedDate, newVal)
                            },
                            onLongClick = onHabitLongClick,
                            onCompleteGoal = { habitToComplete -> onGoalMastered(habitToComplete, uiItem.totalAchievedValue) },
                            onDragStart = { onDragStart(uiItem.habit.id) },
                            onDragDelta = onDragDelta,
                            onDragEnd = onDragEnd,
                            onMoveUp = { 
                                onMoveHabitUp?.invoke(uiItem.habit.id) ?: viewModel.moveLooseHabit(uiItem.habit.id, true) 
                            },
                            onMoveDown = { 
                                onMoveHabitDown?.invoke(uiItem.habit.id) ?: viewModel.moveLooseHabit(uiItem.habit.id, false) 
                            }
                        )
                    }
                    is TodayDisplayItem.FolderGroup -> {
                        FolderGroupCard(
                            folder = displayItem.folder,
                            containedHabits = displayItem.containedHabits,
                            completedCount = displayItem.completedCount,
                            totalCount = displayItem.totalCount,
                            progressPercent = displayItem.progressPercent,
                            language = language,
                            displayedHabits = displayedHabits,
                            justCompletedEvent = justCompletedEvent,
                            selectedDate = selectedDate,
                            vibrationEnabled = vibrationEnabled,
                            isReorderMode = isReorderMode,
                            listState = listState,
                            coloredFolderCards = coloredFolderCards,
                            onToggleCollapse = { viewModel.toggleFolderCollapsed(displayItem.folder.id) },
                            onToggleFolderCompletion = { viewModel.toggleFolderCompletion(displayItem.folder.id) },
                            onEditFolder = { onEditFolder(displayItem.folder) },
                            onMoveFolderUp = {
                                onMoveFolderUp?.invoke(displayItem.folder.id) ?: viewModel.moveFolder(displayItem.folder.id, true)
                            },
                            onMoveFolderDown = {
                                onMoveFolderDown?.invoke(displayItem.folder.id) ?: viewModel.moveFolder(displayItem.folder.id, false)
                            },
                            onToggleHabit = { hId, hasLog -> viewModel.toggleBinaryHabit(hId, selectedDate, hasLog) },
                            onManualAddValue = onManualAddValue,
                            onAddQuantity = { hId, currentVal, diff ->
                                val newVal = (currentVal + diff).coerceAtLeast(0f)
                                viewModel.logNumericalHabit(hId, selectedDate, newVal)
                            },
                            onHabitLongClick = onHabitLongClick,
                            onCompleteGoal = { habitToComplete ->
                                val valAchieved = displayItem.containedHabits.firstOrNull { it.habit.id == habitToComplete.id }?.totalAchievedValue ?: 0f
                                onGoalMastered(habitToComplete, valAchieved)
                            },
                            onMoveNestedHabitUp = { hId -> 
                                onMoveHabitUp?.invoke(hId) ?: viewModel.moveNestedHabit(displayItem.folder.id, hId, true) 
                            },
                            onMoveNestedHabitDown = { hId -> 
                                onMoveHabitDown?.invoke(hId) ?: viewModel.moveNestedHabit(displayItem.folder.id, hId, false) 
                            }
                        )
                    }
                }
            }
        }
    }
}
