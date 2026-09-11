package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.HabitWidgetProvider
import com.example.tr
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.Immutable

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private var knownUnlockedAchievementIds: MutableSet<String>? = null
    private val achievementQueue = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.UnlockedAchievementInfo>>(emptyList())
    private val _newlyUnlockedAchievement = kotlinx.coroutines.flow.MutableStateFlow<com.example.data.UnlockedAchievementInfo?>(null)
    val newlyUnlockedAchievement: kotlinx.coroutines.flow.StateFlow<com.example.data.UnlockedAchievementInfo?> = _newlyUnlockedAchievement.asStateFlow()

    private fun getTodayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())


    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application)
    internal val repository = HabitRepository(database.habitDao())

    private val _pendingWidgetHabitId = MutableStateFlow<Int?>(null)
    val pendingWidgetHabitId: StateFlow<Int?> = _pendingWidgetHabitId.asStateFlow()

    private val initialDismissedGoals = sharedPrefs.getStringSet("dismissed_goal_dialog_ids", emptySet())
        ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    private val _dismissedGoalIds = MutableStateFlow<Set<Int>>(initialDismissedGoals)
    val dismissedGoalIds: StateFlow<Set<Int>> = _dismissedGoalIds.asStateFlow()

    fun dismissGoalDialog(habitId: Int) {
        val newSet = _dismissedGoalIds.value + habitId
        _dismissedGoalIds.value = newSet
        sharedPrefs.edit().putStringSet("dismissed_goal_dialog_ids", newSet.map { it.toString() }.toSet()).apply()
    }

    fun unDismissGoalDialog(habitId: Int) {
        val newSet = _dismissedGoalIds.value - habitId
        _dismissedGoalIds.value = newSet
        sharedPrefs.edit().putStringSet("dismissed_goal_dialog_ids", newSet.map { it.toString() }.toSet()).apply()
    }

    private val initialUserName = sharedPrefs.getString("user_name", "") ?: ""
    private val _userName = MutableStateFlow(if (initialUserName == "Inlitx") "" else initialUserName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val avatarFile = File(application.filesDir, "profile_avatar.jpg")
    private val initialProfileImageUri = sharedPrefs.getString("profile_image_uri", "") ?: ""
    private val resolvedProfileUri = when {
        avatarFile.exists() && avatarFile.length() > 0 -> android.net.Uri.fromFile(avatarFile).toString()
        initialProfileImageUri.startsWith("file://") -> {
            val f = File(initialProfileImageUri.removePrefix("file://"))
            if (f.exists() && f.length() > 0) initialProfileImageUri else ""
        }
        else -> ""
    }
    private val _profileImageUri = MutableStateFlow(resolvedProfileUri)
    val profileImageUri: StateFlow<String> = _profileImageUri.asStateFlow()

    private val _smartInsightDismissedDate = MutableStateFlow(sharedPrefs.getString("smart_insight_dismissed_date", "") ?: "")
    val smartInsightDismissedDate: StateFlow<String> = _smartInsightDismissedDate.asStateFlow()

    fun dismissSmartInsight(dateStr: String) {
        _smartInsightDismissedDate.value = dateStr
        sharedPrefs.edit().putString("smart_insight_dismissed_date", dateStr).apply()
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim()
        _userName.value = trimmed
        sharedPrefs.edit().putString("user_name", trimmed).apply()
    }

    fun updateProfileImageUri(uri: String) {
        if (uri.isEmpty()) {
            if (avatarFile.exists()) {
                try { avatarFile.delete() } catch (e: Exception) { e.printStackTrace() }
            }
            _profileImageUri.value = ""
            sharedPrefs.edit().putString("profile_image_uri", "").apply()
        } else {
            _profileImageUri.value = uri
            sharedPrefs.edit().putString("profile_image_uri", uri).apply()
        }
    }

    fun setPendingWidgetHabitId(id: Int) {
        _pendingWidgetHabitId.value = id
    }

    fun clearPendingWidgetHabitId() {
        _pendingWidgetHabitId.value = null
    }

    // Reactively loaded habits and logs
    val allMilestoneRewards = repository.allMilestoneRewards
    val allHabits: StateFlow<List<Habit>> = repository.allHabits.map { habits ->
        habits.filter { !it.isArchived }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedHabits: StateFlow<List<Habit>> = repository.allHabits.map { habits ->
        habits.filter { it.isArchived }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLogs: StateFlow<List<HabitLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val minWeekStartMillis: StateFlow<Long> = combine(
        allHabits,
        archivedHabits,
        allLogs
    ) { activeHabits, archHabits, logs ->
        val habits = activeHabits + archHabits
        var earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try {
                    sdf.parse(log.date)?.time
                } catch (e: Exception) {
                    null
                }
            }.minOrNull()
            if (earliestLogMs != null) {
                earliestMs = minOf(earliestMs, earliestLogMs)
            }
        }

        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = earliestMs
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = System.currentTimeMillis()
    )

    val minDateStr: StateFlow<String> = combine(
        allHabits,
        archivedHabits,
        allLogs
    ) { activeHabits, archHabits, logs ->
        val habits = activeHabits + archHabits
        var earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try {
                    sdf.parse(log.date)?.time
                } catch (e: Exception) {
                    null
                }
            }.minOrNull()
            if (earliestLogMs != null) {
                earliestMs = minOf(earliestMs, earliestLogMs)
            }
        }

        val oneYearAgoMs = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        val finalEarliestMs = minOf(earliestMs, oneYearAgoMs)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(finalEarliestMs))
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val allDailyNotes: StateFlow<List<DailyNote>> = repository.allDailyNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI States
    internal val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Tracks habit ID and timestamp of a habit actively completed by the user during this session
    private val _justCompletedHabitEvent = MutableStateFlow<Pair<Int, Long>?>(null)
    val justCompletedHabitEvent: StateFlow<Pair<Int, Long>?> = _justCompletedHabitEvent.asStateFlow()

    fun recordHabitCompleted(habitId: Int) {
        _justCompletedHabitEvent.value = Pair(habitId, System.currentTimeMillis())
    }

    private val _language = MutableStateFlow(sharedPrefs.getString("language", "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _accentColorName = MutableStateFlow(sharedPrefs.getString("accent_color_name", "PURPLE") ?: "PURPLE")
    val accentColorName: StateFlow<String> = _accentColorName.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("dark_mode_enabled", true))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val _widgetOpacity = MutableStateFlow(sharedPrefs.getFloat("widget_opacity", 1.0f))
    val widgetOpacity: StateFlow<Float> = _widgetOpacity.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(sharedPrefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _infoCardsEnabled = MutableStateFlow(sharedPrefs.getBoolean("info_cards_enabled", true))
    val infoCardsEnabled: StateFlow<Boolean> = _infoCardsEnabled.asStateFlow()

    private val _reviewNotificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications_enabled", true))
    val reviewNotificationsEnabled: StateFlow<Boolean> = _reviewNotificationsEnabled.asStateFlow()

    private val _dismissedReviews = MutableStateFlow(sharedPrefs.getStringSet("dismissed_reviews", emptySet()) ?: emptySet())
    val dismissedReviews: StateFlow<Set<String>> = _dismissedReviews.asStateFlow()

    private val _insightNotificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("insight_notifications_enabled", true))
    val insightNotificationsEnabled: StateFlow<Boolean> = _insightNotificationsEnabled.asStateFlow()

    private val _smartInsightsInAppEnabled = MutableStateFlow(sharedPrefs.getBoolean("smart_insights_in_app_enabled", true))
    val smartInsightsInAppEnabled: StateFlow<Boolean> = _smartInsightsInAppEnabled.asStateFlow()

    private val _monthlyReviewEnabled = MutableStateFlow(sharedPrefs.getBoolean("monthly_review_enabled", true))
    val monthlyReviewEnabled: StateFlow<Boolean> = _monthlyReviewEnabled.asStateFlow()


    private val _yearlyReviewEnabled = MutableStateFlow(sharedPrefs.getBoolean("yearly_review_enabled", true))
    val yearlyReviewEnabled: StateFlow<Boolean> = _yearlyReviewEnabled.asStateFlow()


    private val _deepLinkReviewMonth = MutableStateFlow<Pair<Int, Int>?>(null)
    val deepLinkReviewMonth: StateFlow<Pair<Int, Int>?> = _deepLinkReviewMonth.asStateFlow()

    private val _deepLinkReviewYear = MutableStateFlow<Int?>(null)
    val deepLinkReviewYear: StateFlow<Int?> = _deepLinkReviewYear.asStateFlow()

    fun setDeepLinkReviewMonth(value: Pair<Int, Int>?) {
        _deepLinkReviewMonth.value = value
    }

    fun setDeepLinkReviewYear(value: Int?) {
        _deepLinkReviewYear.value = value
    }

    private val _hasOnboarded = MutableStateFlow(sharedPrefs.getBoolean("has_onboarded", false))
    val hasOnboarded: StateFlow<Boolean> = _hasOnboarded.asStateFlow()

    // JSON Backup State
    private val _backupFolderUri = MutableStateFlow(sharedPrefs.getString("backup_folder_uri", "") ?: "")
    val backupFolderUri = _backupFolderUri.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    // CSV / ZIP Importer State
    private val _csvPreviewState = MutableStateFlow<com.example.data.CsvImportPreview?>(null)
    val csvPreviewState = _csvPreviewState.asStateFlow()

    private val _isAnalyzingCsv = MutableStateFlow(false)
    val isAnalyzingCsv = _isAnalyzingCsv.asStateFlow()

    // Game Mode State: "STORY" vs "FREE"
    private val _gameMode = MutableStateFlow(sharedPrefs.getString("game_mode", "FREE") ?: "FREE")
    val gameMode: StateFlow<String> = _gameMode.asStateFlow()

    private val _unlockedStorySlots = MutableStateFlow(sharedPrefs.getInt("unlocked_story_slots", 1))
    val unlockedStorySlots: StateFlow<Int> = _unlockedStorySlots.asStateFlow()

    fun setGameMode(mode: String, activeHabitCount: Int = 0) {
        _gameMode.value = mode
        sharedPrefs.edit().putString("game_mode", mode).apply()
        if (mode == "STORY") {
            val currentUnlocked = _unlockedStorySlots.value
            if (activeHabitCount > currentUnlocked) {
                setUnlockedStorySlots(activeHabitCount)
            }
        }
    }

    fun setUnlockedStorySlots(slots: Int) {
        val count = slots.coerceAtLeast(1)
        _unlockedStorySlots.value = count
        sharedPrefs.edit().putInt("unlocked_story_slots", count).apply()
    }

    fun canCreateNewHabit(currentCount: Int): Boolean {
        if (_gameMode.value == "FREE") return true
        return currentCount < _unlockedStorySlots.value
    }

    // Notification Reminder State
    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("reminder_enabled", false))
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _notificationsHour = MutableStateFlow(sharedPrefs.getInt("reminder_hour", 18))
    val notificationsHour = _notificationsHour.asStateFlow()

    private val _notificationsMinute = MutableStateFlow(sharedPrefs.getInt("reminder_minute", 0))
    val notificationsMinute = _notificationsMinute.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("reminder_enabled", enabled).apply()
        if (enabled) {
            com.example.NotificationHelper.scheduleReminder(getApplication(), _notificationsHour.value, _notificationsMinute.value)
        } else {
            com.example.NotificationHelper.cancelReminder(getApplication())
        }
    }

    private val _isSaskiaUnlocked = MutableStateFlow(sharedPrefs.getBoolean("is_saskia_unlocked", false))
    val isSaskiaUnlocked: StateFlow<Boolean> = _isSaskiaUnlocked.asStateFlow()

    fun unlockSaskia() {
        _isSaskiaUnlocked.value = true
        sharedPrefs.edit().putBoolean("is_saskia_unlocked", true).apply()
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        _notificationsHour.value = hour
        _notificationsMinute.value = minute
        sharedPrefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
        if (_notificationsEnabled.value) {
            com.example.NotificationHelper.scheduleReminder(getApplication(), hour, minute)
        }
    }

    // Selected habit for detail screen view
    internal val _selectedHabitIdForDetail = MutableStateFlow<Int?>(null)
    val selectedHabitIdForDetail: StateFlow<Int?> = _selectedHabitIdForDetail.asStateFlow()

    // Calendar month navigation offset
    private val _calendarMonthOffset = MutableStateFlow(0)
    val calendarMonthOffset: StateFlow<Int> = _calendarMonthOffset.asStateFlow()

    fun navigateCalendarMonth(offsetDelta: Int) {
        _calendarMonthOffset.update { (it + offsetDelta).coerceAtMost(0) }
    }

    // Selected date details
    private val _currentWeekStart = MutableStateFlow(Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    })
    val currentWeekStart: StateFlow<Calendar> = _currentWeekStart.asStateFlow()

    val currentWeekDaysData: StateFlow<List<Triple<String, String, String>>> = combine(_currentWeekStart, language) { calendar, lang ->
            val startLocalDate = Instant.ofEpochMilli(calendar.timeInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val dayNumFormatter = DateTimeFormatter.ofPattern("d", Locale.US)
            val loc = when (lang) {
                "de" -> Locale.GERMANY
                "ka" -> Locale.forLanguageTag("ka")
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "fr" -> Locale.FRANCE
                else -> Locale.US
            }
            val dayNameFormatter = DateTimeFormatter.ofPattern("E", loc)
            (0 until 7).map { i ->
                val date = startLocalDate.plusDays(i.toLong())
                val dayStr = date.format(dbFormatter)
                val dayNum = date.format(dayNumFormatter)
                val dayName = if (lang == "ka") date.format(dayNameFormatter).take(3) else if (lang == "zh") date.format(dayNameFormatter) else date.format(dayNameFormatter).uppercase(loc).take(2)
                Triple(dayStr, dayNum, dayName)
            }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val formattedDisplayDate: StateFlow<String> = combine(selectedDate, language) { dateStr, lang ->
        try {
            val todayStr = LocalDate.now().toString()
            if (dateStr == todayStr) {
                when (lang) {
                    "de" -> "Heute"
                    "ka" -> "დღეს"
                    "zh" -> "今天"
                    "fr" -> "Aujourd'hui"
                    else -> "Today"
                }
            } else {
                val localDate = LocalDate.parse(dateStr)
                val loc = when (lang) {
                    "de" -> Locale.GERMANY
                    "ka" -> Locale.forLanguageTag("ka")
                    "zh" -> Locale.SIMPLIFIED_CHINESE
                    "fr" -> Locale.FRANCE
                    else -> Locale.US
                }
                val pattern = when (lang) {
                    "de" -> "d. MMMM"
                    "ka" -> "d MMMM"
                    "zh" -> "M月d日"
                    "fr" -> "d MMMM"
                    else -> "MMMM d"
                }
                val formatter = DateTimeFormatter.ofPattern(pattern, loc)
                localDate.format(formatter)
            }
        } catch (e: Exception) {
            dateStr
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Habit Analytics States
    private val _analyticsFilter = MutableStateFlow("WEEK")
    val analyticsFilter: StateFlow<String> = _analyticsFilter.asStateFlow()

    private val _analyticsVerlaufOffset = MutableStateFlow(0)
    val analyticsVerlaufOffset: StateFlow<Int> = _analyticsVerlaufOffset.asStateFlow()

    fun setAnalyticsFilter(filter: String) {
        _analyticsFilter.value = filter
        _analyticsVerlaufOffset.value = 0
    }

    fun navigateAnalyticsVerlauf(delta: Int) {
        _analyticsVerlaufOffset.update { it + delta }
    }

    val habitAnalyticsState: StateFlow<HabitAnalyticsUiState?> = combine(
        _selectedHabitIdForDetail,
        allHabits,
        allLogs,
        language,
        _analyticsFilter,
        _analyticsVerlaufOffset
    ) { flowsArray ->
        val habitId = flowsArray[0] as Int?
        @Suppress("UNCHECKED_CAST")
        val habits = flowsArray[1] as List<Habit>
        @Suppress("UNCHECKED_CAST")
        val logs = flowsArray[2] as List<HabitLog>
        val lang = flowsArray[3] as String
        val filter = flowsArray[4] as String
        val verlaufOffset = flowsArray[5] as Int

        if (habitId == null) return@combine null
        val habit = habits.find { it.id == habitId } ?: return@combine null
        val habitLogs = logs.filter { it.habitId == habitId }
        
        val today = LocalDate.now()
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startDate = Instant.ofEpochMilli(validStartMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        
        val minVerlaufOffset = when (filter) {
            "WEEK" -> {
                val todayMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                val startMonday = startDate.minusDays((startDate.dayOfWeek.value - 1).toLong())
                val diffDays = ChronoUnit.DAYS.between(startMonday, todayMonday)
                val diffWeeks = diffDays / 7
                -diffWeeks.toInt()
            }
            "MONTH" -> {
                val diffMonths = ChronoUnit.MONTHS.between(
                    startDate.withDayOfMonth(1),
                    today.withDayOfMonth(1)
                )
                -diffMonths.toInt()
            }
            "YEAR" -> {
                val diffYears = ChronoUnit.YEARS.between(
                    startDate.withDayOfYear(1),
                    today.withDayOfYear(1)
                )
                -diffYears.toInt()
            }
            else -> 0
        }.coerceAtMost(0)
        
        val coercedOffset = verlaufOffset.coerceAtLeast(minVerlaufOffset).coerceAtMost(0)
        
        val verlaufTitle = when (filter) {
            "WEEK" -> {
                val targetMonday = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(coercedOffset.toLong())
                val targetSunday = targetMonday.plusDays(6)
                val formatter = DateTimeFormatter.ofPattern("d. MMM", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US })
                "${targetMonday.format(formatter)} - ${targetSunday.format(formatter)} ${targetSunday.year}"
            }
            "MONTH" -> {
                val targetMonthDate = today.withDayOfMonth(1).plusMonths(coercedOffset.toLong())
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US })
                targetMonthDate.format(formatter)
            }
            else -> {
                val targetYearDate = today.plusYears(coercedOffset.toLong())
                "${targetYearDate.year}"
            }
        }
        
        val canPrevVerlauf = coercedOffset > minVerlaufOffset
        val canNextVerlauf = coercedOffset < 0
        
        val barData = mutableListOf<Pair<String, Float>>()
        val todayStr = today.toString()
        val startSdfStr = startDate.toString()
        
        when (filter) {
            "WEEK" -> {
                val targetMonday = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(coercedOffset.toLong())
                val labels = if (lang == "de") listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So") else if (lang == "ka") listOf("ორშ", "სამ", "ოთხ", "ხუთ", "პარ", "შაბ", "კვი") else if (lang == "zh") listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                
                for (i in 0 until 7) {
                    val dDate = targetMonday.plusDays(i.toLong())
                    val dStr = dDate.toString()
                    val isOutOfRange = dStr < startSdfStr || dStr > todayStr
                    val ratio = if (isOutOfRange) {
                        0f
                    } else {
                        val logForDate = habitLogs.find { it.date == dStr }
                        if (habit.type == "BINARY") {
                            val isSuccess = if (logForDate == null) {
                                habit.isNegative
                            } else {
                                when (logForDate.value) {
                                    -2f -> true
                                    -1f -> false
                                    else -> !habit.isNegative
                                }
                            }
                            if (isSuccess) 1f else 0f
                        } else {
                            if (logForDate == null) {
                                if (habit.isNegative) 1f else 0f
                            } else {
                                if (habit.isNegative) {
                                    if (logForDate.value >= habit.targetValue) 0f else 1f
                                } else {
                                    (logForDate.value / habit.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                }
                            }
                        }
                    }
                    barData.add(labels[i] to ratio)
                }
            }
            "MONTH" -> {
                val targetMonthDate = today.withDayOfMonth(1).plusMonths(coercedOffset.toLong())
                val daysCount = targetMonthDate.lengthOfMonth()
                
                for (i in 1..daysCount) {
                    val dDate = targetMonthDate.withDayOfMonth(i)
                    val dStr = dDate.toString()
                    val isOutOfRange = dStr < startSdfStr || dStr > todayStr
                    val ratio = if (isOutOfRange) {
                        0f
                    } else {
                        val logForDate = habitLogs.find { it.date == dStr }
                        if (habit.type == "BINARY") {
                            val isSuccess = if (logForDate == null) {
                                habit.isNegative
                            } else {
                                when (logForDate.value) {
                                    -2f -> true
                                    -1f -> false
                                    else -> !habit.isNegative
                                }
                            }
                            if (isSuccess) 1f else 0f
                        } else {
                            if (logForDate == null) {
                                if (habit.isNegative) 1f else 0f
                            } else {
                                if (habit.isNegative) {
                                    if (logForDate.value >= habit.targetValue) 0f else 1f
                                } else {
                                    (logForDate.value / habit.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                }
                            }
                        }
                    }
                    barData.add(i.toString() to ratio)
                }
            }
            else -> {
                val targetYear = today.year + coercedOffset
                val months = if (lang == "de") 
                    listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
                else if (lang == "ka")
                    listOf("იან", "თებ", "მარ", "აპრ", "მაი", "ივნ", "ივლ", "აგვ", "სექ", "ოქტ", "ნოე", "დეკ")
                else if (lang == "zh")
                    listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                else 
                    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                
                for (m in 1..12) {
                    val targetMonthDate = LocalDate.of(targetYear, m, 1)
                    val maxDays = targetMonthDate.lengthOfMonth()
                    var completedDays = 0
                    var validDaysInMonth = 0
                    
                    for (d in 1..maxDays) {
                        val dDate = LocalDate.of(targetYear, m, d)
                        val dStr = dDate.toString()
                        if (dStr >= startSdfStr && dStr <= todayStr) {
                            validDaysInMonth++
                            val logForDate = habitLogs.find { it.date == dStr }
                            val isSuccess = if (habit.type == "BINARY") {
                                if (logForDate == null) {
                                    habit.isNegative
                                } else {
                                    when (logForDate.value) {
                                        -2f -> true
                                        -1f -> false
                                        else -> !habit.isNegative
                                    }
                                }
                            } else {
                                if (logForDate == null) {
                                    habit.isNegative
                                } else {
                                    if (habit.isNegative) {
                                        logForDate.value < habit.targetValue
                                    } else {
                                        logForDate.value >= habit.targetValue
                                    }
                                }
                            }
                            if (isSuccess) completedDays++
                        }
                    }
                    val ratio = if (validDaysInMonth > 0) completedDays.toFloat() / validDaysInMonth.toFloat() else 0f
                    barData.add(months[m - 1] to ratio)
                }
            }
        }
        
        HabitAnalyticsUiState(
            minVerlaufOffset = minVerlaufOffset,
            verlaufTitle = verlaufTitle,
            canPrevVerlauf = canPrevVerlauf,
            canNextVerlauf = canNextVerlauf,
            barData = barData
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // Cache computed stats to keep the UI smooth
    val todayProgress: StateFlow<Pair<Int, Int>> = combine(allHabits, allLogs, selectedDate) { habits, logs, date ->
        val activeHabits = habits.filter { habit ->
            isHabitActiveOnDate(habit, date)
        }
        if (activeHabits.isEmpty()) return@combine 0 to 0

        val logsMap = logs.filter { it.date == date }.associateBy { it.habitId }
        var completedCount = 0
        var nonPausedActiveCount = 0
        activeHabits.forEach { habit ->
            val log = logsMap[habit.id]
            val isPaused = log != null && log.isPaused
            if (!isPaused) {
                nonPausedActiveCount++
                val isCompleted = isLogCompleted(habit, log)
                if (isCompleted) {
                    completedCount++
                }
            }
        }
        completedCount to nonPausedActiveCount
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0 to 0
    )

    val activeHabitsForSelectedDate: StateFlow<List<Habit>> = combine(repository.allHabits, allLogs, selectedDate) { habits, logs, date ->
        val loggedIds = logs.filter { it.date == date }.map { it.habitId }.toSet()
        habits.filter { isHabitActiveOnDate(it, date) || loggedIds.contains(it.id) }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeHabitUiItemsForSelectedDate: StateFlow<List<HabitUiItem>> = combine(
        repository.allHabits,
        allLogs,
        selectedDate
    ) { habits, logs, date ->
        val logsForDateMap = logs.filter { it.date == date }.associateBy { it.habitId }
        val activeHabits = habits.filter { habit ->
            val isCompletedInFuture = if (habit.isFinishable && habit.isCompletedGoal && habit.completedAt != null) {
                val compDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(habit.completedAt))
                date > compDateStr
            } else {
                false
            }
            !isCompletedInFuture && (isHabitActiveOnDate(habit, date) || logsForDateMap.containsKey(habit.id))
        }
        val mappedItems = activeHabits.map { habit ->
            val log = logsForDateMap[habit.id]
            val currentValue = log?.value ?: 0f
            val hasLog = log != null
            val isPaused = log?.isPaused == true
            val isMinimalViable = log?.isMinimalViable == true
            
            val effectiveTargetValue = if (isMinimalViable && habit.type == "NUMBER" && habit.minimalViableValue != null) {
                habit.minimalViableValue
            } else {
                habit.targetValue
            }
            
            val status = when {
                isPaused -> "PAUSED"
                log == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
                log.value == -1f -> "FAILED"
                log.value == -2f -> "SUCCESS"
                log.value == 0f -> if (habit.isNegative) "SUCCESS" else "PENDING"
                else -> {
                    if (habit.type == "BINARY") {
                        if (habit.isNegative) "FAILED" else "SUCCESS"
                    } else {
                        if (habit.isNegative) {
                            if (log.value >= effectiveTargetValue) "FAILED" else "PENDING"
                        } else {
                            if (log.value >= effectiveTargetValue) "SUCCESS" else "PENDING"
                        }
                    }
                }
            }
            val isCompleted = status == "SUCCESS"
            val isFailed = status == "FAILED"

            var isWeeklyTargetReached = false
            var weeklyLoggedCount = 0
            var weeklyTargetCount = 0

            if (habit.frequency == "TIMES_WEEKLY") {
                weeklyTargetCount = habit.specificDays.toIntOrNull() ?: 3
                val curDate = try { java.time.LocalDate.parse(date) } catch (e: Exception) { java.time.LocalDate.now() }
                val startOf7Days = curDate.minusDays(6)
                val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
                val habitStartDate = try {
                    java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L)).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } catch (e: Exception) { curDate }

                if (habit.isNegative) {
                    val habitLogsMap = logs.filter { it.habitId == habit.id }.associateBy { it.date }
                    var cleanInWindow = 0
                    var pausedInWindow = 0
                    for (d in 0..6) {
                        val checkDate = startOf7Days.plusDays(d.toLong())
                        if (!checkDate.isBefore(habitStartDate) && !checkDate.isAfter(curDate)) {
                            val log = habitLogsMap[checkDate.toString()]
                            if (log?.isPaused == true) {
                                pausedInWindow++
                            } else if (log == null || isLogCompleted(habit, log)) {
                                cleanInWindow++
                            }
                        }
                    }
                    weeklyLoggedCount = cleanInWindow
                    val adjustedTarget = (weeklyTargetCount - pausedInWindow).coerceAtLeast(1)
                    isWeeklyTargetReached = weeklyLoggedCount >= adjustedTarget
                } else {
                    val startOf7DaysStr = startOf7Days.toString()
                    val endOf7DaysStr = curDate.toString()
                    val habitLogsInWindow = logs.filter { l ->
                        l.habitId == habit.id && l.date >= startOf7DaysStr && l.date <= endOf7DaysStr
                    }
                    val pausedInWindow = habitLogsInWindow.count { it.isPaused }
                    weeklyLoggedCount = habitLogsInWindow.count { !it.isPaused && isLogCompleted(habit, it) }
                    val adjustedTarget = (weeklyTargetCount - pausedInWindow).coerceAtLeast(1)
                    isWeeklyTargetReached = weeklyLoggedCount >= adjustedTarget
                }
            }

            val (streakVal, _) = calculateStreak(habit, logs, targetDateStr = date)

            val totalAchievedValue = if (habit.isFinishable) {
                calculateTotalAchievedValue(habit, logs)
            } else 0f

            val isGoalTargetReached = habit.isFinishable && !habit.isCompletedGoal &&
                (habit.totalTargetValue != null && totalAchievedValue >= habit.totalTargetValue)

            HabitUiItem(
                habit = habit,
                currentValue = currentValue,
                isCompleted = isCompleted,
                isFailed = isFailed,
                isPaused = isPaused,
                hasLog = hasLog,
                isWeeklyTargetReached = isWeeklyTargetReached,
                weeklyLoggedCount = weeklyLoggedCount,
                weeklyTargetCount = weeklyTargetCount,
                streak = streakVal,
                isMinimalViable = isMinimalViable,
                totalAchievedValue = totalAchievedValue,
                isGoalTargetReached = isGoalTargetReached
            )
        }

        val itemsById = mappedItems.associateBy { it.habit.id }
        val rootItems = mappedItems.filter { it.habit.stackedOnHabitId == null || !itemsById.containsKey(it.habit.stackedOnHabitId) }
        val childrenMap = mappedItems.filter { it.habit.stackedOnHabitId != null && itemsById.containsKey(it.habit.stackedOnHabitId) }
            .groupBy { it.habit.stackedOnHabitId!! }

        val orderedItems = mutableListOf<HabitUiItem>()
        fun addWithChildren(item: HabitUiItem) {
            orderedItems.add(item)
            childrenMap[item.habit.id]?.forEach { child ->
                addWithChildren(child)
            }
        }

        rootItems.forEach { root ->
            addWithChildren(root)
        }
        
        orderedItems
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val masteredGoals: StateFlow<List<Habit>> = allHabits.map { habits ->
        habits.filter { it.isFinishable && it.isCompletedGoal }
            .sortedByDescending { it.completedAt ?: it.createdAt }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habitLogsByHabitId: StateFlow<Map<Int, HabitLog>> = combine(allLogs, selectedDate) { logs, date ->
        logs.filter { it.date == date }.associateBy { it.habitId }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val totalStrength: StateFlow<Int> = combine(allHabits, allLogs) { habits, logs ->
        calculateTotalStrength(habits, logs)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val longestStreakOfAll: StateFlow<Int> = combine(allHabits, allLogs) { habits, logs ->
        habits.map { calculateStreak(it, logs).second }.maxOrNull() ?: 0
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val perfectDaysStats: StateFlow<PerfectDaysStats> = combine(allHabits, allLogs, selectedDate) { habits, logs, date ->
        calculatePerfectDaysStats(habits, logs, targetDateStr = date)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PerfectDaysStats()
    )

    val profileStats: StateFlow<ProfileStats> = combine(allHabits, allLogs, perfectDaysStats) { habits, logs, perfectDaysState ->
        val totalGlobalCompletions = habits.sumOf { habit ->
            val compDateStr = habit.completedAt?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it))
            }
            val effectiveLogs = if (habit.isFinishable && habit.isCompletedGoal && compDateStr != null) {
                logs.filter { it.date <= compDateStr }
            } else {
                logs
            }
            getCompletedLogsCount(habit, effectiveLogs, "ALL", referenceTimeMs = if (habit.isFinishable && habit.isCompletedGoal) habit.completedAt else null)
        }

        val unlockedCompletions = listOf(10, 50, 200, 500).count { totalGlobalCompletions >= it }
        val perfectDaysStreak = perfectDaysState.perfectDaysStreak
        val unlockedPerfectDays = listOf(7, 30, 100).count { perfectDaysStreak >= it }

        val habitStreaks = habits.map { habit ->
            val compDateStr = habit.completedAt?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it))
            }
            val effectiveLogs = if (habit.isFinishable && habit.isCompletedGoal && compDateStr != null) {
                logs.filter { it.date <= compDateStr }
            } else {
                logs
            }
            val (_, longestStreak) = calculateStreak(habit, effectiveLogs, targetDateStr = compDateStr)
            val completions = getCompletedLogsCount(habit, effectiveLogs, "ALL", referenceTimeMs = if (habit.isFinishable && habit.isCompletedGoal) habit.completedAt else null)
            ProfileHabitStreak(habit, longestStreak, completions)
        }

        val unlockedHabitStreaks = habitStreaks.sumOf { streakInfo ->
            val longestStreak = streakInfo.longestStreak
            var count = 0
            if (longestStreak >= 7) count++
            if (longestStreak >= 14) count++
            if (longestStreak >= 30) count++
            if (longestStreak >= 100) count++
            count
        }

        val unlockedMasteredGoals = habits.count { it.isFinishable && it.isCompletedGoal }
        val finishableHabitsCount = habits.count { it.isFinishable }
        val totalUnlockedCount = unlockedCompletions + unlockedPerfectDays + unlockedHabitStreaks + unlockedMasteredGoals
        val totalPossibleCount = 4 + 3 + (habits.filter { !it.isCompletedGoal }.size * 4) + finishableHabitsCount

        ProfileStats(
            totalGlobalCompletions = totalGlobalCompletions,
            unlockedCompletions = unlockedCompletions,
            perfectDaysStreak = perfectDaysStreak,
            unlockedPerfectDays = unlockedPerfectDays,
            habitStreaks = habitStreaks,
            unlockedHabitStreaks = unlockedHabitStreaks,
            totalUnlockedCount = totalUnlockedCount,
            totalPossibleCount = totalPossibleCount
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileStats()
    )

    init {
        initAchievementObserver()
    }

    val overallCalendarData: StateFlow<OverallCalendarData> = combine(allHabits, allLogs) { habits, logs ->
        val statusMap = mutableMapOf<String, String>()
        val progressMap = mutableMapOf<String, Pair<Int, Int>>()
        if (habits.isNotEmpty()) {
            val oldestHabitDateMs = habits.map {
                if (it.startDate > 946684800000L) {
                    it.startDate
                } else if (it.createdAt > 946684800000L) {
                    it.createdAt
                } else {
                    System.currentTimeMillis()
                }
            }.minOrNull() ?: System.currentTimeMillis()
            val startCal = Calendar.getInstance().apply { timeInMillis = oldestHabitDateMs }
            val todayCal = Calendar.getInstance()
            val maxCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            
            val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdfDb.format(todayCal.time)
            
            val cal = startCal.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            while (cal.timeInMillis <= maxCal.timeInMillis) {
                val dateStr = sdfDb.format(cal.time)
                
                val activeHabits = habits.filter { habit ->
                    val isCompletedInFuture = if (habit.isFinishable && habit.isCompletedGoal && habit.completedAt != null) {
                        val compDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(habit.completedAt))
                        dateStr > compDateStr
                    } else {
                        false
                    }
                    !isCompletedInFuture && isHabitActiveOnDate(habit, dateStr)
                }
                if (activeHabits.isNotEmpty()) {
                    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
                    var completedCount = 0
                    var nonPausedActiveCount = 0
                    
                    var anyPending = false
                    var anyFailed = false
                    var anySuccess = false
                    var anyPaused = false
                    
                    activeHabits.forEach { habit ->
                        val log = logsMap[habit.id]
                        val isPaused = log != null && log.isPaused
                        val isWeeklyTargetReached = if (habit.frequency == "TIMES_WEEKLY") {
                            val weeklyTargetCount = habit.specificDays.toIntOrNull() ?: 3
                            val curDate = try { java.time.LocalDate.parse(dateStr) } catch (e: Exception) { java.time.LocalDate.now() }
                            val startOf7Days = curDate.minusDays(6).toString()
                            val endOf7Days = curDate.toString()
                            val weeklyLoggedCount = logs.filter { l ->
                                l.habitId == habit.id && l.date >= startOf7Days && l.date <= endOf7Days && isLogCompleted(habit, l)
                            }.size
                            weeklyLoggedCount >= weeklyTargetCount
                        } else false

                        if (!isPaused) {
                            nonPausedActiveCount++
                            val isCompleted = isLogCompleted(habit, log)
                            if (isCompleted) {
                                completedCount++
                            }
                        }
                        
                        val hStatus = getLogStatus(habit, log, dateStr, "1970-01-01", todayStr, isWeeklyTargetReached)
                        if (hStatus == "PENDING") anyPending = true
                        if (hStatus == "FAILED") anyFailed = true
                        if (hStatus == "SUCCESS") anySuccess = true
                        if (hStatus == "PAUSED") anyPaused = true
                    }
                    
                    progressMap[dateStr] = completedCount to nonPausedActiveCount
                    
                    val combinedStatus = if (dateStr > todayStr) "INACTIVE" else {
                        when {
                            anyPending -> "PENDING"
                            anyFailed -> "FAILED"
                            anySuccess -> "SUCCESS"
                            anyPaused -> "PAUSED"
                            else -> "SUCCESS"
                        }
                    }
                    statusMap[dateStr] = combinedStatus
                } else {
                    progressMap[dateStr] = 0 to 0
                    statusMap[dateStr] = "INACTIVE"
                }
                
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        OverallCalendarData(statusMap, progressMap)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverallCalendarData()
    )

    val todayDateString: StateFlow<String> = MutableStateFlow(getTodayDateString()).asStateFlow()

    val canPrevWeek: StateFlow<Boolean> = combine(currentWeekStart, minWeekStartMillis) { weekStart, minMillis ->
        weekStart.timeInMillis > minMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val canNextWeek: StateFlow<Boolean> = currentWeekStart.map { weekStart ->
        val maxWeekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        weekStart.timeInMillis < maxWeekStart.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val statsDayNamesAndNumbers: StateFlow<Pair<List<String>, List<String>>> = language.map { lang ->
        val sdfDayInitial = DateTimeFormatter.ofPattern("E", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "ka" -> Locale.forLanguageTag("ka"); "fr" -> Locale.FRANCE; else -> Locale.US })
        val shortNames = mutableListOf<String>()
        val dayNums = mutableListOf<String>()
        val today = LocalDate.now()
        for (i in 0 until 7) {
            val d = today.minusDays(i.toLong())
            shortNames.add(d.format(sdfDayInitial).take(2).uppercase(when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US }))
            dayNums.add(d.dayOfMonth.toString())
        }
        shortNames.reverse()
        dayNums.reverse()
        Pair(shortNames, dayNums)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), run {
         val lang = _language.value
         val loc = when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "ka" -> Locale.forLanguageTag("ka"); "fr" -> Locale.FRANCE; else -> Locale.US }
         val sdfDayInitial = DateTimeFormatter.ofPattern("E", loc)
         val shortNames = mutableListOf<String>()
         val dayNums = mutableListOf<String>()
         val today = LocalDate.now()
         for (i in 0 until 7) {
             val d = today.minusDays(i.toLong())
             shortNames.add(d.format(sdfDayInitial).take(2).uppercase(loc))
             dayNums.add(d.dayOfMonth.toString())
         }
         shortNames.reverse()
         dayNums.reverse()
         Pair(shortNames, dayNums)
     })

    private val _heatmapMonthOffset = MutableStateFlow(0)
    val heatmapMonthOffset: StateFlow<Int> = _heatmapMonthOffset.asStateFlow()

    fun navigateHeatmapMonth(delta: Int) {
        _heatmapMonthOffset.update { it + delta }
    }

    val baseHeatmapCalendar: StateFlow<Calendar> = selectedDate.map { dateStr ->
        val cal = Calendar.getInstance()
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val d = sdfDb.parse(dateStr)
            if (d != null) cal.time = d
        } catch (e: Exception) {}
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) })

    val heatmapMonthCalendar: StateFlow<Calendar> = combine(baseHeatmapCalendar, _heatmapMonthOffset) { baseCal, offset ->
        val cal = baseCal.clone() as Calendar
        cal.add(Calendar.MONTH, offset)
        cal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) })

    val heatmapCanPrevMonth: StateFlow<Boolean> = combine(
        heatmapMonthCalendar,
        allHabits,
        archivedHabits,
        allLogs
    ) { monthCal, activeHabits, archHabits, logs ->
        val habits = activeHabits + archHabits
        var oldestHabitDateMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try {
                    sdf.parse(log.date)?.time
                } catch (e: Exception) {
                    null
                }
            }.minOrNull()
            if (earliestLogMs != null) {
                oldestHabitDateMs = minOf(oldestHabitDateMs, earliestLogMs)
            }
        }

        val oldestCal = Calendar.getInstance().apply { timeInMillis = oldestHabitDateMs }
        val currentCalMonth = monthCal.get(Calendar.MONTH) + monthCal.get(Calendar.YEAR) * 12
        val oldestCalMonth = oldestCal.get(Calendar.MONTH) + oldestCal.get(Calendar.YEAR) * 12
        currentCalMonth > oldestCalMonth
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val heatmapCanNextMonth: StateFlow<Boolean> = heatmapMonthCalendar.map { monthCal ->
        val today = Calendar.getInstance()
        val currentCalMonth = monthCal.get(Calendar.MONTH) + monthCal.get(Calendar.YEAR) * 12
        val todayCalMonth = today.get(Calendar.MONTH) + today.get(Calendar.YEAR) * 12
        currentCalMonth < todayCalMonth
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val heatmapMonthNameAndYear: StateFlow<String> = combine(heatmapMonthCalendar, language) { monthCal, lang ->
        val sdfHeader = SimpleDateFormat("MMMM yyyy", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US })
        sdfHeader.format(monthCal.time)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val heatmapMonthGridData: StateFlow<List<List<CalendarGridCellData?>>> = combine(
        heatmapMonthCalendar,
        overallCalendarData,
        allHabits,
        minDateStr
    ) { monthCal, overallCal, habits, minDate ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())
        
        val temp = monthCal.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val year = temp.get(Calendar.YEAR)
        val month = temp.get(Calendar.MONTH)
        
        temp.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
        temp.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        
        val weeksList = mutableListOf<List<CalendarGridCellData?>>()
        val cursor = temp.clone() as Calendar
        
        for (w in 0 until 6) {
            if (w >= 4 && cursor.get(Calendar.MONTH) != month) {
                break
            }
            
            val weekDays = mutableListOf<CalendarGridCellData?>()
            var hasDaysInMonth = false
            for (d in 0 until 7) {
                val cellYear = cursor.get(Calendar.YEAR)
                val cellMonth = cursor.get(Calendar.MONTH)
                val dateStr = sdf.format(cursor.time)
                
                if (cellYear == year && cellMonth == month) {
                    hasDaysInMonth = true
                    val progress = overallCal.progressMap[dateStr] ?: (0 to 0)
                    val combinedStatus = overallCal.statusMap[dateStr] ?: "INACTIVE"
                    val isToday = dateStr == today
                    val isFuture = dateStr > today
                    val isOutOfRange = false
                    val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)
                    
                    weekDays.add(
                        CalendarGridCellData(
                            day = dayOfMonth,
                            dateStr = dateStr,
                            combinedStatus = combinedStatus,
                            isToday = isToday,
                            isFuture = isFuture,
                            total = progress.second,
                            completed = progress.first,
                            isOutOfRange = isOutOfRange
                        )
                    )
                } else {
                    weekDays.add(null)
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (hasDaysInMonth) {
                weeksList.add(weekDays)
            }
        }
        weeksList
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapYearGridData: StateFlow<List<List<CalendarGridCellData>>> = combine(
        overallCalendarData,
        allHabits,
        minDateStr
    ) { overallCal, habits, minDate ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())
        
        val currentCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        }
        
        val startCal = (currentCal.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, -23)
        }
        
        val weeksList = mutableListOf<List<CalendarGridCellData>>()
        val cursor = startCal.clone() as Calendar
        for (w in 0 until 24) {
            val weekDays = mutableListOf<CalendarGridCellData>()
            for (d in 0 until 7) {
                val dateStr = sdf.format(cursor.time)
                val progress = overallCal.progressMap[dateStr] ?: (0 to 0)
                val combinedStatus = overallCal.statusMap[dateStr] ?: "INACTIVE"
                val isToday = dateStr == today
                val isFuture = dateStr > today
                val isOutOfRange = false
                val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)
                
                weekDays.add(
                    CalendarGridCellData(
                        day = dayOfMonth,
                        dateStr = dateStr,
                        combinedStatus = combinedStatus,
                        isToday = isToday,
                        isFuture = isFuture,
                        total = progress.second,
                        completed = progress.first,
                        isOutOfRange = isOutOfRange
                    )
                )
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            weeksList.add(weekDays)
        }
        weeksList
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapYearMonthLabels: StateFlow<List<Pair<Int, String>>> = combine(
        heatmapYearGridData,
        language
    ) { yearGrid, lang ->
        val labels = mutableListOf<Pair<Int, String>>()
        val sdfMonth = SimpleDateFormat("MMM", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; "fr" -> Locale.FRANCE; else -> Locale.US })
        var lastAddedIndex = -10
        var lastMonthStr = ""
        
        yearGrid.forEachIndexed { index, weekDays ->
            val mondayDate = weekDays.first().dateStr
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(mondayDate)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val monthStr = sdfMonth.format(cal.time)
                if (monthStr != lastMonthStr) {
                    if (index - lastAddedIndex >= 3) {
                        labels.add(index to monthStr)
                        lastAddedIndex = index
                    }
                    lastMonthStr = monthStr
                }
            }
        }
        labels
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHeatmapCell = MutableStateFlow<CalendarGridCellData?>(null)
    val selectedHeatmapCell = _selectedHeatmapCell.asStateFlow()
    
    private val _heatmapViewMode = MutableStateFlow("month")
    val heatmapViewMode = _heatmapViewMode.asStateFlow()
    
    fun selectHeatmapCell(cell: CalendarGridCellData?) {
        if (cell != null) {
            val firstDate = firstHabitDateStr.value
            if (firstDate != null && cell.dateStr < firstDate) {
                return // Prevent selection of cells before first habit creation
            }
        }
        _selectedHeatmapCell.value = cell
    }
    
    fun setHeatmapViewMode(mode: String) {
        _heatmapViewMode.value = mode
    }

    val activeHeatmapCell: StateFlow<CalendarGridCellData?> = combine(
        _selectedHeatmapCell,
        heatmapMonthGridData,
        heatmapYearGridData
    ) { selectedCell, monthGrid, yearGrid ->
        val activeGrid = yearGrid.flatten() + monthGrid.flatten().filterNotNull()
        if (selectedCell != null) {
            activeGrid.firstOrNull { it.dateStr == selectedCell.dateStr } ?: selectedCell
        } else {
            activeGrid.firstOrNull { it.isToday } ?: activeGrid.firstOrNull()
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val formattedActiveCellDate: StateFlow<String> = combine(
        activeHeatmapCell,
        language
    ) { activeCell, lang ->
        if (activeCell == null) return@combine ""
        try {
            val localDate = java.time.LocalDate.parse(activeCell.dateStr)
            val loc = when (lang) {
                "de" -> Locale.GERMANY
                "ka" -> Locale.forLanguageTag("ka")
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "fr" -> Locale.FRANCE
                else -> Locale.US
            }
            val pattern = when (lang) {
                "de" -> "EEEE, d. MMMM yyyy"
                "ka" -> "EEEE, d MMMM, yyyy"
                "fr" -> "EEEE d MMMM yyyy"
                else -> "EEEE, MMMM d, yyyy"
            }
            val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern, loc)
            localDate.format(formatter)
        } catch (e: Exception) {
            activeCell.dateStr
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val oldestHabitDateMs: StateFlow<Long> = combine(
        allHabits,
        archivedHabits,
        allLogs
    ) { activeHabits, archHabits, logs ->
        val habits = activeHabits + archHabits
        var oldestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try {
                    sdf.parse(log.date)?.time
                } catch (e: Exception) {
                    null
                }
            }.minOrNull()
            if (earliestLogMs != null) {
                oldestMs = minOf(oldestMs, earliestLogMs)
            }
        }
        oldestMs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    val firstHabitDateStr: StateFlow<String?> = combine(
        allHabits,
        archivedHabits
    ) { active, archived ->
        val habits = active + archived
        if (habits.isEmpty()) {
            null
        } else {
            val oldestMs = habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull()
            if (oldestMs != null) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(oldestMs))
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val statsScreenData: StateFlow<List<HabitStatModel>> = combine(
        allHabits,
        allLogs,
        combine(language, heatmapMonthCalendar, minDateStr) { l, m, minD -> Triple(l, m, minD) }
    ) { habits, logs, (lang, monthCal, minDate) ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())
        val daysList = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            daysList.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        daysList.reverse()

        val activeHabits = habits.filter { !(it.isFinishable && it.isCompletedGoal) }
        activeHabits.map { habit ->
            val habitLogs = logs.filter { it.habitId == habit.id }
            val logsByDate = habitLogs.associateBy { it.date }
            
            val strength = calculateHabitStrength(habit, logs)
            
            val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
            val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startSdfStr = startSdf.format(Date(validStartMillis))
            val todayStr = startSdf.format(Date())

            val past7DaysStatuses = daysList.map { dateStr ->
                val log = logsByDate[dateStr]
                getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
            }

            // Month Grid calculation for this specific habit
            val temp = monthCal.clone() as Calendar
            temp.set(Calendar.DAY_OF_MONTH, 1)
            val year = temp.get(Calendar.YEAR)
            val month = temp.get(Calendar.MONTH)
            
            temp.firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            temp.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            
            val monthGridList = mutableListOf<List<CalendarGridCellData?>>()
            val cursor = temp.clone() as Calendar
            
            for (w in 0 until 6) {
                if (w >= 4 && cursor.get(Calendar.MONTH) != month) {
                    break
                }
                
                val weekDays = mutableListOf<CalendarGridCellData?>()
                var hasDaysInMonth = false
                for (d in 0 until 7) {
                    val cellYear = cursor.get(Calendar.YEAR)
                    val cellMonth = cursor.get(Calendar.MONTH)
                    val dateStr = sdf.format(cursor.time)
                    
                    if (cellYear == year && cellMonth == month) {
                        hasDaysInMonth = true
                        
                        val isActive = isHabitActiveOnDate(habit, dateStr)
                        var progressTotal = 0
                        var progressCompleted = 0
                        var combinedStatus = "INACTIVE"
                        
                        if (isActive) {
                            val log = logsByDate[dateStr]
                            val isPaused = log != null && log.isPaused
                            if (!isPaused) {
                                progressTotal = 1
                                if (isLogCompleted(habit, log)) {
                                    progressCompleted = 1
                                }
                            }
                            combinedStatus = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
                        }
                        
                        val isToday = dateStr == todayStr
                        val isFuture = dateStr > todayStr
                        val isOutOfRange = false
                        val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)
                        
                        weekDays.add(
                            CalendarGridCellData(
                                day = dayOfMonth,
                                dateStr = dateStr,
                                combinedStatus = combinedStatus,
                                isToday = isToday,
                                isFuture = isFuture,
                                total = progressTotal,
                                completed = progressCompleted,
                                isOutOfRange = isOutOfRange
                            )
                        )
                    } else {
                        weekDays.add(null)
                    }
                    cursor.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (hasDaysInMonth) {
                    monthGridList.add(weekDays)
                }
            }

            // Year Grid calculation for this specific habit
            val currentCal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            }
            
            val startCal = (currentCal.clone() as Calendar).apply {
                add(Calendar.WEEK_OF_YEAR, -23)
            }
            
            val yearWeeksList = mutableListOf<List<CalendarGridCellData>>()
            val yearCursor = startCal.clone() as Calendar
            for (w in 0 until 24) {
                val weekDays = mutableListOf<CalendarGridCellData>()
                for (d in 0 until 7) {
                    val dateStr = sdf.format(yearCursor.time)
                    
                    val isActive = isHabitActiveOnDate(habit, dateStr)
                    var progressTotal = 0
                    var progressCompleted = 0
                    var combinedStatus = "INACTIVE"
                    
                    if (isActive) {
                        val log = logsByDate[dateStr]
                        val isPaused = log != null && log.isPaused
                        if (!isPaused) {
                            progressTotal = 1
                            if (isLogCompleted(habit, log)) {
                                progressCompleted = 1
                            }
                        }
                        combinedStatus = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
                    }
                    
                    val isToday = dateStr == todayStr
                    val isFuture = dateStr > todayStr
                    val isOutOfRange = false
                    val dayOfMonth = yearCursor.get(Calendar.DAY_OF_MONTH)
                    
                    weekDays.add(
                        CalendarGridCellData(
                            day = dayOfMonth,
                            dateStr = dateStr,
                            combinedStatus = combinedStatus,
                            isToday = isToday,
                            isFuture = isFuture,
                            total = progressTotal,
                            completed = progressCompleted,
                            isOutOfRange = isOutOfRange
                        )
                    )
                    yearCursor.add(Calendar.DAY_OF_YEAR, 1)
                }
                yearWeeksList.add(weekDays)
            }

            // Year Month Labels for this specific habit
            val yearMonthLabels = mutableListOf<Pair<Int, String>>()
            val sdfMonth = SimpleDateFormat("MMM", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; else -> Locale.US })
            var lastAddedIndex = -10
            var lastMonthStr = ""
            
            yearWeeksList.forEachIndexed { index, weekDays ->
                val mondayDate = weekDays.first().dateStr
                val date = sdf.parse(mondayDate)
                if (date != null) {
                    val calForLabel = Calendar.getInstance().apply { time = date }
                    val monthStr = sdfMonth.format(calForLabel.time)
                    if (monthStr != lastMonthStr) {
                        if (index - lastAddedIndex >= 3) {
                            yearMonthLabels.add(index to monthStr)
                            lastAddedIndex = index
                        }
                        lastMonthStr = monthStr
                    }
                }
            }

            HabitStatModel(
                habit = habit,
                strength = strength,
                past7DaysStatuses = past7DaysStatuses,
                past7DaysDates = daysList,
                monthGridData = monthGridList,
                yearGridData = yearWeeksList,
                yearMonthLabels = yearMonthLabels
            )
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedHabitDetailState: StateFlow<HabitDetailUiState?> = combine(
        _selectedHabitIdForDetail,
        allHabits,
        allLogs,
        language,
        _calendarMonthOffset
    ) { habitId, habits, logs, lang, monthOffset ->
        if (habitId == null) return@combine null
        val habit = habits.find { it.id == habitId } ?: return@combine null

        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startDate = Instant.ofEpochMilli(validStartMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val startSdfStr = startDate.toString()

        val completedDateStr = habit.completedAt?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it))
        }
        val completedLocalDate = completedDateStr?.let {
            try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
        }

        val useCompletedTime = habit.isFinishable && habit.isCompletedGoal && completedLocalDate != null
        val effectiveLogs = if (useCompletedTime) {
            logs.filter { it.date <= completedDateStr!! }
        } else {
            logs
        }

        val referenceToday = if (useCompletedTime) completedLocalDate!! else LocalDate.now()
        val today = referenceToday

        val diffMonths = ChronoUnit.MONTHS.between(
            startDate.withDayOfMonth(1),
            today.withDayOfMonth(1)
        )
        val minMonthOffset = -diffMonths.toInt()

        val actualOffset = monthOffset.coerceIn(minMonthOffset, 0)
        val canPrevMonth = actualOffset > minMonthOffset
        val canNextMonth = actualOffset < 0

        val (currentStreak, longestStreak) = calculateStreak(habit, effectiveLogs, targetDateStr = if (useCompletedTime) completedDateStr else null)
        val strength = calculateHabitStrength(habit, effectiveLogs, todayStr = if (useCompletedTime) completedDateStr else null)
        val completionRate = calculateCompletionRate(habit, effectiveLogs, todayStr = if (useCompletedTime) completedDateStr else null)

        val thisWeekCount = getCompletedLogsCount(habit, effectiveLogs, "WEEK", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val thisMonthCount = getCompletedLogsCount(habit, effectiveLogs, "MONTH", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val thisYearCount = getCompletedLogsCount(habit, effectiveLogs, "YEAR", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)
        val totalCount = getCompletedLogsCount(habit, effectiveLogs, "ALL", referenceTimeMs = if (useCompletedTime) habit.completedAt else null)

        val habitLogs = effectiveLogs.filter { it.habitId == habitId }
        val logsByDate = habitLogs.associateBy { it.date }

        val targetMonthDate = today.withDayOfMonth(1).plusMonths(actualOffset.toLong())
        val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; else -> Locale.US })
        val monthNameStr = targetMonthDate.format(monthNameFormatter)

        val currentMonthDaysCount = targetMonthDate.lengthOfMonth()
        val firstDayOfWeekOffset = (targetMonthDate.dayOfWeek.value - 1) // 0 is Monday, ..., 6 is Sunday

        val todayStr = today.toString()
        val daysList = mutableListOf<CalendarCellState?>()

        for (i in 0 until firstDayOfWeekOffset) {
            daysList.add(null)
        }
        for (i in 1..currentMonthDaysCount) {
            val dateVal = targetMonthDate.withDayOfMonth(i)
            val dateStr = dateVal.toString()
            val log = logsByDate[dateStr]
            val isCompleted = isLogCompleted(habit, log)
            val status = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
            
            daysList.add(CalendarCellState(id = dateStr, dayNum = i.toString(), isCompleted = isCompleted, status = status))
        }

        val targetStats = com.example.data.calculateTargetPeriodStats(habit, effectiveLogs)

        // 1. Calculate historical weekday frequency (0 = Mon, ..., 6 = Sun)
        val occurrences = IntArray(7)
        val completions = IntArray(7)

        var currentDay = startDate
        if (!currentDay.isAfter(today)) {
            while (!currentDay.isAfter(today)) {
                val dateStr = currentDay.toString()
                if (isHabitActiveOnDate(habit, dateStr)) {
                    val log = logsByDate[dateStr]
                    val dayOfWeekIndex = currentDay.dayOfWeek.value - 1
                    if (dayOfWeekIndex in 0..6) {
                        occurrences[dayOfWeekIndex]++
                        if (getLogStatus(habit, log, dateStr, startSdfStr, todayStr) == "SUCCESS") {
                            completions[dayOfWeekIndex]++
                        }
                    }
                }
                currentDay = currentDay.plusDays(1)
            }
        }

        val weekdayStats = List(7) { index ->
            val total = occurrences[index]
            val completed = completions[index]
            val pct = if (total > 0) (completed.toFloat() / total.toFloat() * 100).toInt() else 0
            Triple(index, completed, pct)
        }

        // 2. Calculate last 15 weeks grid
        val mondayOfCurrentWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val numWeeks = 15
        val weekdayGridData = List(numWeeks) { weekOffset ->
            val weekStart = mondayOfCurrentWeek.minusWeeks((numWeeks - 1 - weekOffset).toLong())
            List(7) { dayOffset ->
                val cellDate = weekStart.plusDays(dayOffset.toLong())
                val dateStr = cellDate.toString()
                val log = logsByDate[dateStr]
                val status = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
                cellDate to status
            }
        }

        // 3. Month labels
        val weeksWithMonthLabels = Array(weekdayGridData.size) { "" }
        var lastMonth = -1
        var lastAddedIndex = -10
        weekdayGridData.forEachIndexed { index, weekDays ->
            val monday = weekDays.first().first
            val monthVal = monday.monthValue
            if (monthVal != lastMonth) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM", when (lang) { "de" -> Locale.GERMANY; "zh" -> Locale.SIMPLIFIED_CHINESE; else -> Locale.US })
                val labelStr = monday.format(formatter)
                if (index - lastAddedIndex >= 3) {
                    weeksWithMonthLabels[index] = labelStr
                    lastAddedIndex = index
                } else if (lastAddedIndex == 0) {
                    weeksWithMonthLabels[0] = ""
                    weeksWithMonthLabels[index] = labelStr
                    lastAddedIndex = index
                }
                lastMonth = monthVal
            }
        }

        HabitDetailUiState(
            habit = habit,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            strength = strength,
            thisWeekCount = thisWeekCount,
            thisMonthCount = thisMonthCount,
            thisYearCount = thisYearCount,
            totalCount = totalCount,
            calendarGridRows = daysList.chunked(7),
            monthName = monthNameStr,
            canPrevMonth = canPrevMonth,
            canNextMonth = canNextMonth,
            completionRate = completionRate,
            targetStats = targetStats,
            weekdayStats = weekdayStats,
            weekdayGridData = weekdayGridData,
            weeksWithMonthLabels = weeksWithMonthLabels.toList()
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectDate(date: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (date > todayStr) return
        val minDate = getMinDateStr()
        if (date < minDate) return
        _selectedDate.value = date
    }

    fun selectDateAndSyncWeek(dateStr: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dateStr > todayStr) return
        val minDate = getMinDateStr()
        if (dateStr < minDate) return
        _selectedDate.value = dateStr
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val date = sdfDb.parse(dateStr)
            if (date != null) {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    time = date
                    // Find Monday of this week
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                _currentWeekStart.value = cal
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMinWeekStartMillis(): Long {
        val habits = allHabits.value + archivedHabits.value
        var earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        val logs = allLogs.value
        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try {
                    sdf.parse(log.date)?.time
                } catch (e: Exception) {
                    null
                }
            }.minOrNull()
            if (earliestLogMs != null) {
                earliestMs = minOf(earliestMs, earliestLogMs)
            }
        }

        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = earliestMs
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getMinDateStr(): String {
        val habits = allHabits.value + archivedHabits.value
        var earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        val logs = allLogs.value
        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val earliestLogMs = logs.mapNotNull { log ->
                try {
                    sdf.parse(log.date)?.time
                } catch (e: Exception) {
                    null
                }
            }.minOrNull()
            if (earliestLogMs != null) {
                earliestMs = minOf(earliestMs, earliestLogMs)
            }
        }

        val oneYearAgoMs = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        val finalEarliestMs = minOf(earliestMs, oneYearAgoMs)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(finalEarliestMs))
    }

    fun adjustDateAndWeekIfOutOfRange() {
        val minDate = getMinDateStr()
        val currentSelected = _selectedDate.value
        if (currentSelected.compareTo(minDate) < 0) {
            _selectedDate.value = minDate
            val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            try {
                val parsed = sdfDb.parse(minDate)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.MONDAY
                        time = parsed
                        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                        val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                        add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    _currentWeekStart.value = cal
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectHabitForDetail(habitId: Int?) {
        _calendarMonthOffset.value = 0
        _selectedHabitIdForDetail.value = habitId
    }

    fun nextWeek() {
        updateWeekAndSelectedDate(1)
    }

    fun prevWeek() {
        updateWeekAndSelectedDate(-1)
    }

    private fun updateWeekAndSelectedDate(weeksDelta: Int) {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val currentSelected = _selectedDate.value
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
        }
        try {
            val date = sdfDb.parse(currentSelected)
            if (date != null) {
                cal.time = date
            }
        } catch (e: Exception) {}

        // Shift date by weeksDelta * 7 days
        cal.add(Calendar.DAY_OF_YEAR, weeksDelta * 7)
        val targetDate = cal.time
        val targetDateStr = sdfDb.format(targetDate)

        // Find Monday of this target week
        val mondayCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            time = targetDate
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Limit to current week (no future weeks allowed) and 1 year in the past
        val maxWeekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val minWeekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = getMinWeekStartMillis()
        }
        if (mondayCal.timeInMillis > maxWeekStart.timeInMillis || mondayCal.timeInMillis < minWeekStart.timeInMillis) {
            return
        }

        // Check if today is in this target week
        val todayCal = Calendar.getInstance()
        val todayStr = sdfDb.format(todayCal.time)
        val weekStartTest = mondayCal.clone() as Calendar
        var todayInWeek: String? = null
        for (i in 0 until 7) {
            val dStr = sdfDb.format(weekStartTest.time)
            if (dStr == todayStr) {
                todayInWeek = dStr
            }
            weekStartTest.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateToSelect = todayInWeek ?: targetDateStr

        _selectedDate.value = dateToSelect
        _currentWeekStart.value = mondayCal
    }

    fun saveDailyNote(date: String, content: String) {
        viewModelScope.launch {
            repository.saveDailyNote(date, content)
        }
    }

    fun getTimeCapsuleNote(type: String, targetPeriod: String): Flow<TimeCapsuleNote?> {
        return repository.getTimeCapsuleNote(type, targetPeriod)
    }

    fun saveTimeCapsuleNote(type: String, targetPeriod: String, content: String) {
        viewModelScope.launch {
            repository.saveTimeCapsuleNote(type, targetPeriod, content)
        }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        sharedPrefs.edit().putString("language", lang).apply()
    }

    fun setAccentColorName(name: String) {
        _accentColorName.value = name
        sharedPrefs.edit().putString("accent_color_name", name).apply()
        try {
            getApplication<android.app.Application>().getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
                .edit().putString("accent_color_name", name).apply()
        } catch (e: Exception) {}
        com.example.ui.theme.updateAccentColors(name)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("dark_mode_enabled", enabled).commit()
        try {
            getApplication<android.app.Application>().getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("dark_mode_enabled", enabled).commit()
        } catch (e: Exception) {}
        com.example.ui.theme.updateThemeColors(enabled)
        HabitWidgetProvider.triggerUpdate(getApplication())
    }

    fun setWidgetOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        _widgetOpacity.value = clamped
        sharedPrefs.edit().putFloat("widget_opacity", clamped).commit()
        HabitWidgetProvider.triggerUpdate(getApplication())
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        sharedPrefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun setInfoCardsEnabled(enabled: Boolean) {
        _infoCardsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("info_cards_enabled", enabled).apply()
    }

    fun setReviewNotificationsEnabled(enabled: Boolean) {
        _reviewNotificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
        com.example.NotificationHelper.scheduleReviewNotifications(getApplication())
    }

    fun dismissReview(reviewKey: String) {
        val updated = _dismissedReviews.value.toMutableSet()
        updated.add(reviewKey)
        _dismissedReviews.value = updated
        sharedPrefs.edit().putStringSet("dismissed_reviews", updated).apply()
    }

    fun setMonthlyReviewEnabled(enabled: Boolean) {
        _monthlyReviewEnabled.value = enabled
        sharedPrefs.edit().putBoolean("monthly_review_enabled", enabled).apply()
        com.example.NotificationHelper.scheduleReviewNotifications(getApplication())
    }

    fun setInsightNotificationsEnabled(enabled: Boolean) {
        _insightNotificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("insight_notifications_enabled", enabled).apply()
        if (enabled) {
            com.example.NotificationHelper.scheduleSmartInsightNotifications(getApplication())
        } else {
            com.example.NotificationHelper.cancelSmartInsightNotifications(getApplication())
        }
    }

    fun setSmartInsightsInAppEnabled(enabled: Boolean) {
        _smartInsightsInAppEnabled.value = enabled
        sharedPrefs.edit().putBoolean("smart_insights_in_app_enabled", enabled).apply()
    }


    fun setYearlyReviewEnabled(enabled: Boolean) {
        _yearlyReviewEnabled.value = enabled
        sharedPrefs.edit().putBoolean("yearly_review_enabled", enabled).apply()
        com.example.NotificationHelper.scheduleReviewNotifications(getApplication())
    }


    fun setOnboarded(completed: Boolean) {
        _hasOnboarded.value = completed
        sharedPrefs.edit().putBoolean("has_onboarded", completed).apply()
    }

    fun resetOnboarding() {
        setOnboarded(false)
    }

    // Settings / WebDAV Setup
    // Backup & Restore
    fun saveBackupFolderUri(uri: String) {
        _backupFolderUri.value = uri
        sharedPrefs.edit()
            .putString("backup_folder_uri", uri)
            .apply()
        // Schedule work
        DailyBackupWorker.scheduleDailyBackup(getApplication())
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Sicherungsordner gespeichert! Täglicher Export ist aktiv."
            "ka" -> "სარეზერვო საქაღალდე შენახულია! ყოველდღიური ექსპორტი აქტიურია."
            "zh" -> "备份文件夹已保存！每日自动导出已启用。"
            "fr" -> "Dossier de sauvegarde enregistré ! L'export quotidien est actif."
            else -> "Backup folder saved! Daily export is active."
        }
    }

    fun triggerManualBackup() {
        viewModelScope.launch {
            val lang = language.value
            _syncStatus.value = when (lang) {
                "de" -> "Sicherung wird erstellt..."
                "ka" -> "სარეზერვო ასლი იქმნება..."
                "zh" -> "正在创建备份..."
                "fr" -> "Création de la sauvegarde en cours..."
                else -> "Creating backup..."
            }
            val uriStr = _backupFolderUri.value
            if (uriStr.isEmpty()) {
                _syncStatus.value = when (lang) {
                    "de" -> "Fehler: Kein Ordner ausgewählt!"
                    "ka" -> "შეცდომა: საქაღალდე არ არის არჩეული!"
                    "zh" -> "错误：未选择文件夹！"
                    "fr" -> "Erreur : Aucun dossier sélectionné !"
                    else -> "Error: No folder selected!"
                }
                return@launch
            }
            val success = BackupManager.performBackup(getApplication(), uriStr)
            _syncStatus.value = if (success) {
                when (lang) {
                    "de" -> "Sicherung erfolgreich erstellt! 🎉"
                    "ka" -> "სარეზერვო ასლი წარმატებით შეიქმნა! 🎉"
                    "zh" -> "备份创建成功！🎉"
                    "fr" -> "Sauvegarde créée avec succès ! 🎉"
                    else -> "Backup created successfully! 🎉"
                }
            } else {
                when (lang) {
                    "de" -> "Fehler beim Erstellen der Sicherung!"
                    "ka" -> "შეცდომა სარეზერვო ასლის შექმნისას!"
                    "zh" -> "创建备份时出错！"
                    "fr" -> "Erreur lors de la création de la sauvegarde !"
                    else -> "Error creating backup!"
                }
            }
        }
    }

    fun triggerManualRestoreFromStream(inputStream: java.io.InputStream) {
        viewModelScope.launch {
            val lang = language.value
            _syncStatus.value = when (lang) {
                "de" -> "Daten werden wiederhergestellt..."
                "ka" -> "მონაცემები აღდგება..."
                "zh" -> "正在恢复数据..."
                "fr" -> "Restauration des données en cours..."
                else -> "Restoring data..."
            }
            val success = BackupManager.restoreDatabaseFromInputStream(getApplication(), inputStream)
            if (success) {
                reloadSettingsFromPrefs()
            }
            _syncStatus.value = if (success) {
                when (lang) {
                    "de" -> "Sicherung erfolgreich wiederhergestellt! 🎉"
                    "ka" -> "სარეზერვო ასლი წარმატებით აღდგა! 🎉"
                    "zh" -> "备份已成功恢复！🎉"
                    "fr" -> "Sauvegarde restaurée avec succès ! 🎉"
                    else -> "Backup successfully restored! 🎉"
                }
            } else {
                when (lang) {
                    "de" -> "Fehler bei der Wiederherstellung! Ungültige Datei."
                    "ka" -> "შეცდომა აღდგენისას! არასწორი ფაილი."
                    "zh" -> "恢复失败！无效的文件。"
                    "fr" -> "Échec de la restauration ! Fichier invalide."
                    else -> "Restore failed! Invalid file."
                }
            }
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun triggerManualRestore(jsonString: String) {
        viewModelScope.launch {
            val lang = language.value
            _syncStatus.value = when (lang) {
                "de" -> "Daten werden wiederhergestellt..."
                "ka" -> "მონაცემები აღდგება..."
                "zh" -> "正在恢复数据..."
                "fr" -> "Restauration des données en cours..."
                else -> "Restoring data..."
            }
            val success = BackupManager.restoreDatabaseFromJson(getApplication(), jsonString)
            if (success) {
                reloadSettingsFromPrefs()
            }
            _syncStatus.value = if (success) {
                when (lang) {
                    "de" -> "Sicherung erfolgreich wiederhergestellt! 🎉"
                    "ka" -> "სარეზერვო ასლი წარმატებით აღდგა! 🎉"
                    "zh" -> "备份已成功恢复！🎉"
                    "fr" -> "Sauvegarde restaurée avec succès ! 🎉"
                    else -> "Backup successfully restored! 🎉"
                }
            } else {
                when (lang) {
                    "de" -> "Fehler bei der Wiederherstellung! Ungültige Datei."
                    "ka" -> "შეცდომა აღდგენისას! არასწორი ფაილი."
                    "zh" -> "恢复失败！无效的文件。"
                    "fr" -> "Erreur lors de la restauration ! Fichier non valide."
                    else -> "Error during restore! Invalid file."
                }
            }
        }
    }

    fun reloadSettingsFromPrefs() {
        val uName = sharedPrefs.getString("user_name", "") ?: ""
        _userName.value = if (uName == "Inlitx") "" else uName
        
        val pUri = sharedPrefs.getString("profile_image_uri", "") ?: ""
        _profileImageUri.value = when {
            avatarFile.exists() && avatarFile.length() > 0 -> android.net.Uri.fromFile(avatarFile).toString()
            pUri.startsWith("file://") -> {
                val f = File(pUri.removePrefix("file://"))
                if (f.exists() && f.length() > 0) pUri else ""
            }
            else -> pUri
        }
        _smartInsightDismissedDate.value = sharedPrefs.getString("smart_insight_dismissed_date", "") ?: ""
        _language.value = sharedPrefs.getString("language", "en") ?: "en"
        _accentColorName.value = sharedPrefs.getString("accent_color_name", "PURPLE") ?: "PURPLE"
        _darkModeEnabled.value = sharedPrefs.getBoolean("dark_mode_enabled", true)
        _vibrationEnabled.value = sharedPrefs.getBoolean("vibration_enabled", true)
        _infoCardsEnabled.value = sharedPrefs.getBoolean("info_cards_enabled", true)
        _reviewNotificationsEnabled.value = sharedPrefs.getBoolean("notifications_enabled", true)
        _dismissedReviews.value = sharedPrefs.getStringSet("dismissed_reviews", emptySet()) ?: emptySet()
        _insightNotificationsEnabled.value = sharedPrefs.getBoolean("insight_notifications_enabled", true)
        _smartInsightsInAppEnabled.value = sharedPrefs.getBoolean("smart_insights_in_app_enabled", true)
        _monthlyReviewEnabled.value = sharedPrefs.getBoolean("monthly_review_enabled", true)
        _yearlyReviewEnabled.value = sharedPrefs.getBoolean("yearly_review_enabled", true)
        _hasOnboarded.value = sharedPrefs.getBoolean("has_onboarded", false)
        _backupFolderUri.value = sharedPrefs.getString("backup_folder_uri", "") ?: ""
        _notificationsEnabled.value = sharedPrefs.getBoolean("reminder_enabled", false)
        _notificationsHour.value = sharedPrefs.getInt("reminder_hour", 18)
        _notificationsMinute.value = sharedPrefs.getInt("reminder_minute", 0)
        _isSaskiaUnlocked.value = sharedPrefs.getBoolean("is_saskia_unlocked", false)
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun analyzeCsvImport(uri: android.net.Uri) {
        viewModelScope.launch {
            _isAnalyzingCsv.value = true
            val preview = com.example.data.CsvImporter.analyzeFile(getApplication(), uri)
            _csvPreviewState.value = preview
            _isAnalyzingCsv.value = false
        }
    }

    fun confirmCsvImport(replaceExisting: Boolean) {
        val preview = _csvPreviewState.value ?: return
        viewModelScope.launch {
            _isAnalyzingCsv.value = true
            val success = com.example.data.CsvImporter.importDataToDatabase(getApplication(), preview, replaceExisting)
            _isAnalyzingCsv.value = false
            _csvPreviewState.value = null
            val lang = language.value
            _syncStatus.value = if (success) {
                when (lang) {
                    "de" -> "CSV-Import erfolgreich! ${preview.habits.size} Gewohnheiten importiert. 🎉"
                    "ka" -> "CSV იმპორტი წარმატებულია! ${preview.habits.size} ჩვევა იმპორტირებულია. 🎉"
                    "zh" -> "CSV 导入成功！已导入 ${preview.habits.size} 个习惯。🎉"
                    else -> "CSV Import successful! ${preview.habits.size} habits imported. 🎉"
                }
            } else {
                when (lang) {
                    "de" -> "Fehler beim CSV-Import!"
                    "ka" -> "შეცდომა CSV იმპორტისას!"
                    "zh" -> "CSV 导入失败！"
                    else -> "Error during CSV import!"
                }
            }
        }
    }

    fun dismissCsvPreview() {
        _csvPreviewState.value = null
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            // Trigger widget update
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    private val epochDaysCache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private fun dateToEpochDaysFast(dateStr: String): Int {
        return epochDaysCache.getOrPut(dateStr) {
            try {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    java.time.LocalDate.of(year, month, day).toEpochDay().toInt()
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    private fun millisToEpochDays(millis: Long): Int {
        return try {
            val instant = java.time.Instant.ofEpochMilli(millis)
            val zoneId = java.time.ZoneId.systemDefault()
            instant.atZone(zoneId).toLocalDate().toEpochDay().toInt()
        } catch (e: Exception) {
            (millis / 86400000L).toInt()
        }
    }

    // STATS CALCULATION FUNCTIONS
    fun calculateStreak(habit: Habit, logs: List<HabitLog>, targetDateStr: String? = null): Pair<Int, Int> =
        HabitCalculationEngine.calculateStreak(habit, logs, targetDateStr)

    fun calculateTotalAchievedValue(habit: Habit, logs: List<HabitLog>): Float =
        HabitCalculationEngine.calculateTotalAchievedValue(habit, logs)

    fun completeFinishableGoal(habitId: Int, reflectionNote: String = "", claimReward: Boolean = false) {
        viewModelScope.launch {
            val habit = allHabits.value.find { it.id == habitId } ?: return@launch
            val updated = habit.copy(
                isCompletedGoal = true,
                completedAt = System.currentTimeMillis(),
                completionNote = reflectionNote.trim()
            )
            repository.updateHabit(updated)

            // Unlock and optionally redeem any milestone rewards associated with this habit
            val rewards = repository.getAllMilestoneRewardsRaw().filter { it.habitId == habitId }
            rewards.forEach { reward ->
                val updatedReward = reward.copy(
                    unlockedAt = if (reward.unlockedAt > 0L) reward.unlockedAt else System.currentTimeMillis(),
                    isRedeemed = if (claimReward) true else reward.isRedeemed
                )
                repository.updateMilestoneReward(updatedReward)
            }

            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun restartFinishableGoal(habitId: Int, newTotalTarget: Float? = null) {
        viewModelScope.launch {
            unDismissGoalDialog(habitId)
            val habit = allHabits.value.find { it.id == habitId } ?: return@launch
            val updated = habit.copy(
                isCompletedGoal = false,
                completedAt = null,
                totalTargetValue = newTotalTarget ?: habit.totalTargetValue
            )
            repository.updateHabit(updated)
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

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

    private fun initAchievementObserver() {
        viewModelScope.launch {
            combine(profileStats, language, allMilestoneRewards) { stats, lang, rewards -> 
                Triple(stats, lang, rewards)
            }.collect { (stats, lang, rewards) ->
                // Check all milestone rewards to ensure any completed conditions are marked unlocked
                rewards.forEach { reward ->
                    if (reward.unlockedAt == 0L) {
                        val habitStat = stats.habitStreaks.find { it.habit.id == reward.habitId }
                        val isReached = when (reward.conditionType) {
                            "STREAK" -> (habitStat?.longestStreak ?: 0) >= reward.conditionValue
                            "COMPLETIONS" -> (habitStat?.totalCompletions ?: 0) >= reward.conditionValue
                            "TROPHY_COUPLED" -> {
                                val streak = habitStat?.longestStreak ?: 0
                                when (reward.trophyId) {
                                    "WOOD" -> streak >= 7
                                    "BRONZE" -> streak >= 14
                                    "SILVER" -> streak >= 30
                                    "GOLD" -> streak >= 100
                                    "COMP_10" -> stats.totalGlobalCompletions >= 10
                                    "COMP_50" -> stats.totalGlobalCompletions >= 50
                                    "COMP_200" -> stats.totalGlobalCompletions >= 200
                                    "COMP_500" -> stats.totalGlobalCompletions >= 500
                                    "PERF_7" -> stats.perfectDaysStreak >= 7
                                    "PERF_30" -> stats.perfectDaysStreak >= 30
                                    "PERF_100" -> stats.perfectDaysStreak >= 100
                                    else -> false
                                }
                            }
                            else -> false
                        }
                        if (isReached) {
                            viewModelScope.launch {
                                repository.updateMilestoneReward(reward.copy(unlockedAt = System.currentTimeMillis()))
                            }
                        }
                    }
                }

                val currentUnlocked = AchievementEvaluator.calculateUnlockedAchievementsList(stats, lang, rewards)
                val currentIds = currentUnlocked.map { it.id }.toSet()

                if (knownUnlockedAchievementIds == null) {
                    val savedIds = sharedPrefs.getStringSet("known_unlocked_achievement_ids", null)
                    if (savedIds != null) {
                        knownUnlockedAchievementIds = savedIds.toMutableSet()
                    } else {
                        knownUnlockedAchievementIds = currentIds.toMutableSet()
                        sharedPrefs.edit().putStringSet("known_unlocked_achievement_ids", currentIds).apply()
                    }
                }

                val newlyUnlocked = currentUnlocked.filter { it.id !in knownUnlockedAchievementIds!! }
                if (newlyUnlocked.isNotEmpty()) {
                    newlyUnlocked.forEach { ach ->
                        knownUnlockedAchievementIds!!.add(ach.id)
                    }
                    sharedPrefs.edit().putStringSet("known_unlocked_achievement_ids", knownUnlockedAchievementIds).apply()

                    val updatedQueue = achievementQueue.value + newlyUnlocked
                    achievementQueue.value = updatedQueue
                    if (_newlyUnlockedAchievement.value == null) {
                        _newlyUnlockedAchievement.value = if (updatedQueue.isNotEmpty()) updatedQueue[0] else null
                    }
                }
            }
        }
    }

    fun dismissUnlockedAchievement() {
        val currentQueue = achievementQueue.value
        if (currentQueue.isNotEmpty()) {
            val nextQueue = currentQueue.drop(1)
            achievementQueue.value = nextQueue
            _newlyUnlockedAchievement.value = if (nextQueue.isNotEmpty()) nextQueue[0] else null
        } else {
            _newlyUnlockedAchievement.value = null
        }
    }

    fun redeemMilestoneReward(rewardId: Int) {
        toggleRedeemMilestoneReward(rewardId, true)
    }

    fun toggleRedeemMilestoneReward(rewardId: Int, isRedeemed: Boolean) {
        viewModelScope.launch {
            try {
                val rewards = repository.getAllMilestoneRewardsRaw()
                val reward = rewards.find { it.id == rewardId }
                if (reward != null) {
                    repository.updateMilestoneReward(reward.copy(isRedeemed = isRedeemed))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Immutable
data class HabitAnalyticsUiState(
    val minVerlaufOffset: Int = 0,
    val verlaufTitle: String = "",
    val canPrevVerlauf: Boolean = false,
    val canNextVerlauf: Boolean = false,
    val barData: List<Pair<String, Float>> = emptyList()
)
