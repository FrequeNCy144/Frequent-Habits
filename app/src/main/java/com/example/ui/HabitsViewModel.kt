package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitWidgetProvider
import com.example.data.*
import com.example.ui.theme.updateAccentColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun SharedPreferences.safeGetString(key: String, def: String): String =
    try { getString(key, def) ?: def } catch (_: Throwable) { def }

fun SharedPreferences.safeGetInt(key: String, def: Int): Int =
    try { getInt(key, def) } catch (_: Throwable) {
        try { getString(key, null)?.toIntOrNull() ?: def } catch (_: Throwable) { def }
    }

fun SharedPreferences.safeGetBoolean(key: String, def: Boolean): Boolean =
    try { getBoolean(key, def) } catch (_: Throwable) {
        try { getString(key, null)?.toBooleanStrictOrNull() ?: def } catch (_: Throwable) { def }
    }

fun SharedPreferences.safeGetFloat(key: String, def: Float): Float =
    try { getFloat(key, def) } catch (_: Throwable) {
        try { getString(key, null)?.toFloatOrNull() ?: def } catch (_: Throwable) { def }
    }

fun SharedPreferences.safeGetStringSet(key: String, def: Set<String>): Set<String> =
    try { getStringSet(key, def) ?: def } catch (_: Throwable) { def }

open class HabitsViewModel(application: Application) : AndroidViewModel(application) {

    internal val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("habit_app_prefs", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application)
    internal val repository = HabitRepository(database.habitDao())

    // User / Profile
    internal val avatarFile = File(application.filesDir, "profile_avatar.jpg")
    internal val _userName = MutableStateFlow(
        sharedPrefs.safeGetString("user_name", "").let { if (it == "Inlitx") "" else it }
    )
    val userName: StateFlow<String> = _userName.asStateFlow()

    internal val _profileImageUri = MutableStateFlow(
        when {
            avatarFile.exists() && avatarFile.length() > 0 -> Uri.fromFile(avatarFile).toString()
            else -> {
                val pUri = sharedPrefs.safeGetString("profile_image_uri", "")
                if (pUri.startsWith("file://")) {
                    val f = File(pUri.removePrefix("file://"))
                    if (f.exists() && f.length() > 0) pUri else ""
                } else pUri
            }
        }
    )
    val profileImageUri: StateFlow<String> = _profileImageUri.asStateFlow()

    internal val _pendingWidgetHabitId = MutableStateFlow<Int?>(null)
    val pendingWidgetHabitId: StateFlow<Int?> = _pendingWidgetHabitId.asStateFlow()

    internal val _smartInsightDismissedDate = MutableStateFlow(
        sharedPrefs.safeGetString("smart_insight_dismissed_date", "")
    )
    val smartInsightDismissedDate: StateFlow<String> = _smartInsightDismissedDate.asStateFlow()

    internal val _dismissedGoalIds = MutableStateFlow(
        sharedPrefs.safeGetStringSet("dismissed_goal_dialog_ids", emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet()
    )
    val dismissedGoalIds: StateFlow<Set<Int>> = _dismissedGoalIds.asStateFlow()

    // Achievement Notification State
    internal val _newlyUnlockedAchievement = MutableStateFlow<com.example.data.UnlockedAchievementInfo?>(null)
    val newlyUnlockedAchievement: StateFlow<com.example.data.UnlockedAchievementInfo?> = _newlyUnlockedAchievement.asStateFlow()
    internal val achievementQueue = MutableStateFlow<List<com.example.data.UnlockedAchievementInfo>>(emptyList())
    internal var knownUnlockedAchievementIds: MutableSet<String>? = null

    internal val _justCompletedHabitEvent = MutableStateFlow<Pair<Int, Long>?>(null)
    val justCompletedHabitEvent: StateFlow<Pair<Int, Long>?> = _justCompletedHabitEvent.asStateFlow()

    // Preferences
    internal val _language = MutableStateFlow(sharedPrefs.safeGetString("language", "en"))
    val language: StateFlow<String> = _language.asStateFlow()

    internal val _accentColorName = MutableStateFlow(sharedPrefs.safeGetString("accent_color_name", "PURPLE"))
    val accentColorName: StateFlow<String> = _accentColorName.asStateFlow()

    internal val _themeMode = MutableStateFlow(
        try {
            sharedPrefs.getString("theme_mode", null) ?: if (sharedPrefs.contains("dark_mode_enabled")) {
                if (sharedPrefs.safeGetBoolean("dark_mode_enabled", true)) "DARK" else "LIGHT"
            } else {
                "DARK"
            }
        } catch (_: Throwable) {
            "DARK"
        }
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    internal val _darkModeEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("dark_mode_enabled", true))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    internal val _widgetOpacity = MutableStateFlow(sharedPrefs.safeGetFloat("widget_opacity", 0.9f))
    val widgetOpacity: StateFlow<Float> = _widgetOpacity.asStateFlow()

    internal val _vibrationEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("vibration_enabled", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    internal val _infoCardsEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("info_cards_enabled", true))
    val infoCardsEnabled: StateFlow<Boolean> = _infoCardsEnabled.asStateFlow()

    internal val _coloredFolderCardsEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("colored_folder_cards_enabled", true))
    val coloredFolderCardsEnabled: StateFlow<Boolean> = _coloredFolderCardsEnabled.asStateFlow()

    internal val _reviewNotificationsEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("notifications_enabled", true))
    val reviewNotificationsEnabled: StateFlow<Boolean> = _reviewNotificationsEnabled.asStateFlow()

    internal val _dismissedReviews = MutableStateFlow<Set<String>>(
        sharedPrefs.safeGetStringSet("dismissed_reviews", emptySet())
    )
    val dismissedReviews: StateFlow<Set<String>> = _dismissedReviews.asStateFlow()

    internal val _insightNotificationsEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("insight_notifications_enabled", true))
    val insightNotificationsEnabled: StateFlow<Boolean> = _insightNotificationsEnabled.asStateFlow()

    internal val _smartInsightsInAppEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("smart_insights_in_app_enabled", true))
    val smartInsightsInAppEnabled: StateFlow<Boolean> = _smartInsightsInAppEnabled.asStateFlow()

    internal val _monthlyReviewEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("monthly_review_enabled", true))
    val monthlyReviewEnabled: StateFlow<Boolean> = _monthlyReviewEnabled.asStateFlow()

    internal val _yearlyReviewEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("yearly_review_enabled", true))
    val yearlyReviewEnabled: StateFlow<Boolean> = _yearlyReviewEnabled.asStateFlow()

    internal val _deepLinkReviewMonth = MutableStateFlow<Pair<Int, Int>?>(null)
    val deepLinkReviewMonth: StateFlow<Pair<Int, Int>?> = _deepLinkReviewMonth.asStateFlow()

    internal val _deepLinkReviewYear = MutableStateFlow<Int?>(null)
    val deepLinkReviewYear: StateFlow<Int?> = _deepLinkReviewYear.asStateFlow()

    internal val _hasOnboarded = MutableStateFlow(sharedPrefs.safeGetBoolean("has_onboarded", false))
    val hasOnboarded: StateFlow<Boolean> = _hasOnboarded.asStateFlow()

    internal val _backupFolderUri = MutableStateFlow(sharedPrefs.safeGetString("backup_folder_uri", ""))
    val backupFolderUri: StateFlow<String> = _backupFolderUri.asStateFlow()

    internal val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    internal val _csvPreviewState = MutableStateFlow<CsvImportPreview?>(null)
    val csvPreviewState: StateFlow<CsvImportPreview?> = _csvPreviewState.asStateFlow()

    internal val _isAnalyzingCsv = MutableStateFlow(false)
    val isAnalyzingCsv: StateFlow<Boolean> = _isAnalyzingCsv.asStateFlow()

    internal val _gameMode = MutableStateFlow(sharedPrefs.safeGetString("game_mode", "FREE"))
    val gameMode: StateFlow<String> = _gameMode.asStateFlow()

    internal val _unlockedStorySlots = MutableStateFlow(sharedPrefs.safeGetInt("unlocked_story_slots", 1))
    val unlockedStorySlots: StateFlow<Int> = _unlockedStorySlots.asStateFlow()

    internal val _notificationsEnabled = MutableStateFlow(sharedPrefs.safeGetBoolean("reminder_enabled", false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    fun setEditingHabit(habit: Habit?) {
        _editingHabit.value = habit
    }

    internal val _notificationsHour = MutableStateFlow(sharedPrefs.safeGetInt("reminder_hour", 18))
    val notificationsHour: StateFlow<Int> = _notificationsHour.asStateFlow()

    internal val _notificationsMinute = MutableStateFlow(sharedPrefs.safeGetInt("reminder_minute", 0))
    val notificationsMinute: StateFlow<Int> = _notificationsMinute.asStateFlow()

    internal val _isSaskiaUnlocked = MutableStateFlow(sharedPrefs.safeGetBoolean("is_saskia_unlocked", false))
    val isSaskiaUnlocked: StateFlow<Boolean> = _isSaskiaUnlocked.asStateFlow()

    // Selected habit for detail screen view
    internal val _selectedHabitIdForDetail = MutableStateFlow<Int?>(null)
    val selectedHabitIdForDetail: StateFlow<Int?> = _selectedHabitIdForDetail.asStateFlow()

    internal val _calendarMonthOffset = MutableStateFlow(0)
    val calendarMonthOffset: StateFlow<Int> = _calendarMonthOffset.asStateFlow()

    // Selected date & week navigation
    internal val _currentWeekStart = MutableStateFlow(Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    })
    val currentWeekStart: StateFlow<Calendar> = _currentWeekStart.asStateFlow()

    internal val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Analytics state
    internal val _analyticsFilter = MutableStateFlow("WEEK")
    val analyticsFilter: StateFlow<String> = _analyticsFilter.asStateFlow()

    internal val _analyticsVerlaufOffset = MutableStateFlow(0)
    val analyticsVerlaufOffset: StateFlow<Int> = _analyticsVerlaufOffset.asStateFlow()

    // Heatmap state
    internal val _heatmapMonthOffset = MutableStateFlow(0)
    val heatmapMonthOffset: StateFlow<Int> = _heatmapMonthOffset.asStateFlow()

    internal val _selectedHeatmapCell = MutableStateFlow<CalendarGridCellData?>(null)
    val selectedHeatmapCell: StateFlow<CalendarGridCellData?> = _selectedHeatmapCell.asStateFlow()

    internal val _heatmapViewMode = MutableStateFlow("month")
    val heatmapViewMode: StateFlow<String> = _heatmapViewMode.asStateFlow()

    // Core Data Flows
    internal val _allFolders = MutableStateFlow<List<HabitFolder>>(
        com.example.data.FolderStorageHelper.loadFolders(application)
    )
    val allFolders: StateFlow<List<HabitFolder>> = _allFolders.asStateFlow()

    val allMilestoneRewards: StateFlow<List<MilestoneReward>> = repository.allMilestoneRewards
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedHabits: StateFlow<List<Habit>> = allHabits.map { habits -> habits.filter { it.isArchived } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDailyNotes: StateFlow<List<DailyNote>> = repository.allDailyNotes
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<HabitLog>> = repository.allLogs
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val minWeekStartMillis: StateFlow<Long> = combine(allHabits, archivedHabits, allLogs) { habits, arch, logs ->
        HabitUiEngine.calculateMinWeekStartMillis(habits, arch, logs)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    val minDateStrFlow: StateFlow<String> = combine(allHabits, archivedHabits, allLogs) { habits, arch, logs ->
        HabitUiEngine.calculateMinDateStr(habits, arch, logs)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1970-01-01")

    val minDateStr: StateFlow<String> = minDateStrFlow

    val canPrevWeek: StateFlow<Boolean> = combine(currentWeekStart, minWeekStartMillis) { cur, min -> cur.timeInMillis > min }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canNextWeek: StateFlow<Boolean> = currentWeekStart.map { cur ->
        val maxCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val d = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -d); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        cur.timeInMillis < maxCal.timeInMillis
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentWeekDaysData: StateFlow<List<Pair<String, Pair<String, String>>>> = combine(currentWeekStart, language) { s, l ->
        HabitUiEngine.calculateCurrentWeekDays(s, l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val formattedDisplayDate: StateFlow<String> = combine(selectedDate, language) { d, l ->
        HabitUiEngine.formatDisplayDate(d, l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val todayDateString: StateFlow<String> = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ).asStateFlow()

    val todayProgress: StateFlow<Pair<Int, Int>> = combine(allHabits, allLogs, selectedDate) { habits, logs, date ->
        HabitUiEngine.calculateTodayProgress(habits, logs, date)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    val activeHabitsForSelectedDate: StateFlow<List<Habit>> = combine(allHabits, allLogs, selectedDate) { habits, logs, date ->
        val loggedIds = logs.filter { it.date == date }.map { it.habitId }.toSet()
        habits.filter { isHabitActiveOnDate(it, date) || loggedIds.contains(it.id) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHabitUiItemsForSelectedDate: StateFlow<List<HabitUiItem>> = combine(allHabits, allLogs, selectedDate) { habits, logs, date ->
        HabitUiEngine.calculateActiveHabitUiItems(habits, logs, date)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val masteredGoals: StateFlow<List<Habit>> = allHabits.map { habits ->
        habits.filter { it.isFinishable && it.isCompletedGoal }.sortedByDescending { it.completedAt ?: it.createdAt }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogsByHabitId: StateFlow<Map<Int, HabitLog>> = combine(allLogs, selectedDate) { logs, date ->
        logs.filter { it.date == date }.associateBy { it.habitId }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalStrength: StateFlow<Int> = combine(allHabits, allLogs) { h, l ->
        HabitCalculationEngine.calculateTotalStrength(h, l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val longestStreakOfAll: StateFlow<Int> = combine(allHabits, allLogs) { h, l ->
        h.map { HabitCalculationEngine.calculateStreak(it, l).second }.maxOrNull() ?: 0
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val perfectDaysStats: StateFlow<PerfectDaysStats> = combine(allHabits, allLogs, selectedDate) { h, l, d ->
        HabitCalculationEngine.calculatePerfectDaysStats(h, l, targetDateStr = d)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerfectDaysStats())

    val profileStats: StateFlow<ProfileStats> = combine(allHabits, allLogs, perfectDaysStats) { h, l, p ->
        HabitUiEngine.calculateProfileStats(h, l, p)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileStats())

    init {
        try {
            HabitIconMapping.initSystemAccentColor(getApplication())
            updateAccentColors(_accentColorName.value)
        } catch (_: Exception) {}
        initAchievementObserver()
    }

    val overallCalendarData: StateFlow<OverallCalendarData> = combine(allHabits, allLogs) { h, l ->
        HabitStatsEngine.calculateOverallCalendar(h, l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallCalendarData())

    val baseHeatmapCalendar: StateFlow<Calendar> = selectedDate.map { dateStr ->
        val cal = Calendar.getInstance()
        try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.let { cal.time = it } } catch (e: Exception) {}
        cal
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.getInstance())

    val heatmapMonthCalendar: StateFlow<Calendar> = combine(baseHeatmapCalendar, _heatmapMonthOffset) { base, offset ->
        (base.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.getInstance())

    val heatmapCanPrevMonth: StateFlow<Boolean> = combine(heatmapMonthCalendar, allHabits, archivedHabits, allLogs) { m, h, a, l ->
        HabitUiEngine.calculateHeatmapCanPrevMonth(m, h, a, l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val heatmapCanNextMonth: StateFlow<Boolean> = heatmapMonthCalendar.map { m ->
        val today = Calendar.getInstance()
        (m.get(Calendar.MONTH) + m.get(Calendar.YEAR) * 12) < (today.get(Calendar.MONTH) + today.get(Calendar.YEAR) * 12)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val heatmapMonthNameAndYear: StateFlow<String> = combine(heatmapMonthCalendar, language) { m, lang ->
        val loc = when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US }
        SimpleDateFormat("MMMM yyyy", loc).format(m.time)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val heatmapMonthGridData: StateFlow<List<List<CalendarGridCellData?>>> = combine(heatmapMonthCalendar, overallCalendarData, allHabits, minDateStr) { m, o, h, min ->
        HabitHeatmapEngine.calculateMonthGridData(m, o, h, min)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapYearGridData: StateFlow<List<List<CalendarGridCellData>>> = combine(overallCalendarData, allHabits, minDateStr) { o, h, min ->
        HabitHeatmapEngine.calculateYearGridData(o, h, min)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapYearMonthLabels: StateFlow<List<Pair<Int, String>>> = combine(heatmapYearGridData, language) { g, l ->
        HabitHeatmapEngine.calculateYearMonthLabels(g, l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val firstHabitDateStr: StateFlow<String?> = combine(allHabits, archivedHabits, allLogs) { h, a, l ->
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(HabitUiEngine.calculateOldestHabitDateMs(h, a, l)))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeHeatmapCell: StateFlow<CalendarGridCellData?> = combine(_selectedHeatmapCell, heatmapMonthGridData, heatmapYearGridData, heatmapViewMode, firstHabitDateStr) { sel, mGrid, yGrid, mode, firstDate ->
        if (sel != null) {
            if (firstDate != null && sel.dateStr < firstDate) null else sel
        } else {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            if (firstDate != null && todayStr < firstDate) null
            else if (mode == "month") mGrid.flatten().filterNotNull().find { it.isToday } ?: mGrid.flatten().filterNotNull().lastOrNull()
            else yGrid.flatten().find { it.isToday } ?: yGrid.flatten().lastOrNull()
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val formattedActiveCellDate: StateFlow<String> = combine(activeHeatmapCell, language) { cell, lang ->
        if (cell == null) ""
        else try {
            val loc = when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US }
            java.time.LocalDate.parse(cell.dateStr).format(java.time.format.DateTimeFormatter.ofPattern("d. MMMM yyyy", loc))
        } catch (e: Exception) { cell.dateStr }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val statsDayNamesAndNumbers: StateFlow<List<Pair<String, String>>> = combine(language, todayDateString) { l, _ ->
        HabitUiEngine.calculateStatsDayNamesAndNumbers(l)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsScreenData: StateFlow<List<HabitStatModel>> = combine(allHabits, allLogs, heatmapMonthCalendar, language, allFolders) { h, l, m, lang, f ->
        HabitStatsEngine.calculateStatsScreenData(h, l, m, lang, f)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedHabitDetailState: StateFlow<HabitDetailUiState?> = combine(_selectedHabitIdForDetail, allHabits, allLogs, language, _calendarMonthOffset) { id, h, l, lang, off ->
        HabitDetailStatsEngine.calculateHabitDetailUiState(id, h, l, lang, off)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val habitAnalyticsState: StateFlow<HabitAnalyticsUiState?> = combine(
        combine(_selectedHabitIdForDetail, allHabits, allLogs) { id, h, l -> Triple(id, h, l) },
        combine(language, _analyticsFilter, _analyticsVerlaufOffset) { lang, f, vOff -> Triple(lang, f, vOff) }
    ) { (id, h, l), (lang, f, vOff) ->
        HabitAnalyticsEngine.calculateAnalyticsUiState(id, h, l, lang, f, vOff)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Flow Aliases
    val habits: StateFlow<List<Habit>> get() = allHabits
    val logs: StateFlow<List<HabitLog>> get() = allLogs
    val milestoneRewards: StateFlow<List<MilestoneReward>> get() = allMilestoneRewards
    val todayProgressTuple: StateFlow<Pair<Int, Int>> get() = todayProgress
    val justCompletedEvent: StateFlow<Pair<Int, Long>?> get() = justCompletedHabitEvent
    val weekStartCalendar: StateFlow<Calendar> get() = currentWeekStart
    val earliestHabitDateStr: StateFlow<String> get() = minDateStr
    val habitsWithStats: StateFlow<List<HabitStatModel>> get() = statsScreenData
    val accentColor: StateFlow<androidx.compose.ui.graphics.Color> = accentColorName.map {
        HabitIconMapping.getColor(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), androidx.compose.ui.graphics.Color(0xFF8B5CF6))

    // Folders
    fun saveFoldersOrder(folders: List<HabitFolder>) {
        val updated = folders.mapIndexed { i, f -> f.copy(sortOrder = i) }
        persistFolders(updated)
    }

    private fun persistFolders(folders: List<HabitFolder>) {
        _allFolders.value = folders
        com.example.data.FolderStorageHelper.saveFolders(getApplication(), folders)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }

    fun toggleFolderCollapsed(folderId: Int) {
        val updated = _allFolders.value.map {
            if (it.id == folderId) it.copy(isCollapsed = !it.isCollapsed) else it
        }
        persistFolders(updated)
    }

    fun deleteFolder(folder: HabitFolder) {
        viewModelScope.launch {
            val habitsInFolder = allHabits.value.filter { it.folderId == folder.id }
            habitsInFolder.forEach { habit ->
                repository.updateHabit(habit.copy(folderId = null))
            }
            val updated = _allFolders.value.filter { it.id != folder.id }
            persistFolders(updated)
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun moveFolder(folderId: Int, up: Boolean) {
        val list = _allFolders.value.sortedBy { it.sortOrder }.toMutableList()
        val index = list.indexOfFirst { it.id == folderId }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in list.indices) {
            val item = list.removeAt(index)
            list.add(targetIndex, item)
            val updated = list.mapIndexed { i, f -> f.copy(sortOrder = i) }
            persistFolders(updated)
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun moveLooseHabit(habitId: Int, up: Boolean) {
        viewModelScope.launch {
            val habitList = allHabits.value.filter { it.folderId == null }.sortedBy { it.sortOrder }.toMutableList()
            val index = habitList.indexOfFirst { it.id == habitId }
            if (index == -1) return@launch
            val targetIndex = if (up) index - 1 else index + 1
            if (targetIndex in habitList.indices) {
                val item = habitList.removeAt(index)
                habitList.add(targetIndex, item)
                habitList.forEachIndexed { i, h ->
                    repository.updateHabit(h.copy(sortOrder = i))
                }
                HabitWidgetProvider.triggerUpdate(getApplication())
            }
        }
    }

    fun moveNestedHabit(folderId: Int, habitId: Int, up: Boolean) {
        viewModelScope.launch {
            val habitList = allHabits.value.filter { it.folderId == folderId }.sortedBy { it.sortOrder }.toMutableList()
            val index = habitList.indexOfFirst { it.id == habitId }
            if (index == -1) return@launch
            val targetIndex = if (up) index - 1 else index + 1
            if (targetIndex in habitList.indices) {
                val item = habitList.removeAt(index)
                habitList.add(targetIndex, item)
                habitList.forEachIndexed { i, h ->
                    repository.updateHabit(h.copy(sortOrder = i))
                }
                HabitWidgetProvider.triggerUpdate(getApplication())
            }
        }
    }

    fun addFolder(name: String, icon: String, color: String): Int {
        val newId = (_allFolders.value.maxOfOrNull { it.id } ?: 0) + 1
        val newFolder = HabitFolder(id = newId, name = name, icon = icon, color = color, sortOrder = _allFolders.value.size)
        val updated = _allFolders.value + newFolder
        persistFolders(updated)
        return newId
    }

    fun saveFolder(name: String, icon: String, color: String): Int {
        return addFolder(name, icon, color)
    }

    fun updateFolder(folder: HabitFolder) {
        val updated = _allFolders.value.map { if (it.id == folder.id) folder else it }
        persistFolders(updated)
    }

    // Calculation helper delegates
    fun calculateStreak(habit: Habit, logs: List<HabitLog>, targetDateStr: String? = null): Pair<Int, Int> =
        HabitCalculationEngine.calculateStreak(habit, logs, targetDateStr)

    fun calculateTotalAchievedValue(habit: Habit, logs: List<HabitLog>): Float =
        HabitCalculationEngine.calculateTotalAchievedValue(habit, logs)

    fun calculateTotalStrength(habits: List<Habit>, logs: List<HabitLog>): Int =
        HabitCalculationEngine.calculateTotalStrength(habits, logs)

    fun calculateHabitStrength(habit: Habit, logs: List<HabitLog>, todayStr: String? = null): Int =
        HabitCalculationEngine.calculateHabitStrength(habit, logs, todayStr)

    fun calculateCompletionRate(habit: Habit, logs: List<HabitLog>, todayStr: String? = null): Int =
        HabitCalculationEngine.calculateCompletionRate(habit, logs, todayStr)

    fun getCompletedLogsCount(habit: Habit, logs: List<HabitLog>, period: String, referenceTimeMs: Long? = null): Int =
        HabitCalculationEngine.getCompletedLogsCount(habit, logs, period, referenceTimeMs)

    fun calculatePerfectDaysStats(habits: List<Habit>, logs: List<HabitLog>, targetDateStr: String? = null): PerfectDaysStats =
        HabitCalculationEngine.calculatePerfectDaysStats(habits, logs, targetDateStr)
}
