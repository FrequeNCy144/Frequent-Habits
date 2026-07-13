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

    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    private val _pendingWidgetHabitId = MutableStateFlow<Int?>(null)
    val pendingWidgetHabitId: StateFlow<Int?> = _pendingWidgetHabitId.asStateFlow()

    fun setPendingWidgetHabitId(id: Int) {
        _pendingWidgetHabitId.value = id
    }

    fun clearPendingWidgetHabitId() {
        _pendingWidgetHabitId.value = null
    }

    // Reactively loaded habits and logs
    val allHabits: StateFlow<List<Habit>> = repository.allHabits.map { habits ->
        habits.filter { !it.isArchived }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val minWeekStartMillis: StateFlow<Long> = allHabits.map { habits ->
        val earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
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

    val minDateStr: StateFlow<String> = allHabits.map { habits ->
        val earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(earliestMs))
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
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

    val allDailyNotes: StateFlow<List<DailyNote>> = repository.allDailyNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI States
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _language = MutableStateFlow(sharedPrefs.getString("language", "de") ?: "de")
    val language: StateFlow<String> = _language.asStateFlow()

    // JSON Backup State
    private val _backupFolderUri = MutableStateFlow(sharedPrefs.getString("backup_folder_uri", "") ?: "")
    val backupFolderUri = _backupFolderUri.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

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

    fun updateNotificationTime(hour: Int, minute: Int) {
        _notificationsHour.value = hour
        _notificationsMinute.value = minute
        sharedPrefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
        if (_notificationsEnabled.value) {
            com.example.NotificationHelper.scheduleReminder(getApplication(), hour, minute)
        }
    }

    // Selected habit for detail screen view
    private val _selectedHabitIdForDetail = MutableStateFlow<Int?>(null)
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

    val currentWeekDaysData: StateFlow<List<Triple<String, String, String>>> = _currentWeekStart
        .map { calendar ->
            val startLocalDate = Instant.ofEpochMilli(calendar.timeInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val dayNumFormatter = DateTimeFormatter.ofPattern("d", Locale.GERMANY)
            val dayNameFormatter = DateTimeFormatter.ofPattern("E", Locale.GERMANY)
            (0 until 7).map { i ->
                val date = startLocalDate.plusDays(i.toLong())
                val dayStr = date.format(dbFormatter)
                val dayNum = date.format(dayNumFormatter)
                val dayName = date.format(dayNameFormatter).uppercase(Locale.GERMANY).take(2)
                Triple(dayStr, dayNum, dayName)
            }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val formattedDisplayDate: StateFlow<String> = combine(selectedDate, language) { dateStr, lang ->
        try {
            val localDate = LocalDate.parse(dateStr)
            val formatter = DateTimeFormatter.ofPattern("d. MMMM", if (lang == "de") Locale.GERMANY else Locale.US)
            val displayDate = localDate.format(formatter)
            val todayStr = LocalDate.now().toString()
            if (dateStr == todayStr) {
                "$displayDate (${if (lang == "de") "Heute" else "Today"})"
            } else {
                displayDate
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
                val formatter = DateTimeFormatter.ofPattern("d. MMM", if (lang == "de") Locale.GERMANY else Locale.US)
                "${targetMonday.format(formatter)} - ${targetSunday.format(formatter)} ${targetSunday.year}"
            }
            "MONTH" -> {
                val targetMonthDate = today.withDayOfMonth(1).plusMonths(coercedOffset.toLong())
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", if (lang == "de") Locale.GERMANY else Locale.US)
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
                val labels = if (lang == "de") listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                
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
                if (isLogCompleted(habit, log)) {
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

    val activeHabitsForSelectedDate: StateFlow<List<Habit>> = combine(allHabits, selectedDate) { habits, date ->
        habits.filter { isHabitActiveOnDate(it, date) }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeHabitUiItemsForSelectedDate: StateFlow<List<HabitUiItem>> = combine(
        allHabits,
        allLogs,
        selectedDate
    ) { habits, logs, date ->
        val activeHabits = habits.filter { isHabitActiveOnDate(it, date) }
        val logsForDateMap = logs.filter { it.date == date }.associateBy { it.habitId }
        activeHabits.map { habit ->
            val log = logsForDateMap[habit.id]
            val currentValue = log?.value ?: 0f
            val hasLog = log != null
            val isPaused = log?.isPaused == true
            
            val status = when {
                isPaused -> "PAUSED"
                log == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
                log.value == -1f -> "FAILED"
                log.value == -2f -> "SUCCESS"
                else -> {
                    if (habit.type == "BINARY") {
                        if (habit.isNegative) "FAILED" else "SUCCESS"
                    } else {
                        if (habit.isNegative) {
                            if (log.value >= habit.targetValue) "FAILED" else "PENDING"
                        } else {
                            if (log.value >= habit.targetValue) "SUCCESS" else "PENDING"
                        }
                    }
                }
            }
            val isCompleted = status == "SUCCESS"
            val isFailed = status == "FAILED"
            HabitUiItem(
                habit = habit,
                currentValue = currentValue,
                isCompleted = isCompleted,
                isFailed = isFailed,
                isPaused = isPaused,
                hasLog = hasLog
            )
        }
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

    val perfectDaysStats: StateFlow<PerfectDaysStats> = combine(allHabits, allLogs) { habits, logs ->
        calculatePerfectDaysStats(habits, logs)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PerfectDaysStats()
    )

    val profileStats: StateFlow<ProfileStats> = combine(allHabits, allLogs, perfectDaysStats) { habits, logs, perfectDaysState ->
        val totalGlobalCompletions = logs.count { log ->
            val habit = habits.find { it.id == log.habitId }
            habit != null && isLogCompleted(habit, log)
        }

        val unlockedCompletions = listOf(10, 50, 200, 500).count { totalGlobalCompletions >= it }
        val perfectDaysStreak = perfectDaysState.perfectDaysStreak
        val unlockedPerfectDays = listOf(7, 30, 100).count { perfectDaysStreak >= it }

        val habitStreaks = habits.map { habit ->
            val (_, longestStreak) = calculateStreak(habit, logs)
            ProfileHabitStreak(habit, longestStreak)
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

        val totalUnlockedCount = unlockedCompletions + unlockedPerfectDays + unlockedHabitStreaks
        val totalPossibleCount = 4 + 3 + (habits.size * 4)

        ProfileStats(
            totalGlobalCompletions = totalGlobalCompletions,
            unlockedCompletions = unlockedCompletions,
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
                
                val activeHabits = habits.filter { isHabitActiveOnDate(it, dateStr) }
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
                        if (!isPaused) {
                            nonPausedActiveCount++
                            if (isLogCompleted(habit, log)) {
                                completedCount++
                            }
                        }
                        
                        val hStatus = getLogStatus(habit, log, dateStr, "1970-01-01", todayStr)
                        if (hStatus == "PENDING") anyPending = true
                        if (hStatus == "FAILED") anyFailed = true
                        if (hStatus == "SUCCESS") anySuccess = true
                        if (hStatus == "PAUSED") anyPaused = true
                    }
                    
                    progressMap[dateStr] = completedCount to nonPausedActiveCount
                    
                    val combinedStatus = if (dateStr > todayStr) {
                        "INACTIVE"
                    } else {
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
        val sdfDayInitial = DateTimeFormatter.ofPattern("E", if (lang == "de") Locale.GERMANY else Locale.US)
        val shortNames = mutableListOf<String>()
        val dayNums = mutableListOf<String>()
        val today = LocalDate.now()
        for (i in 0 until 7) {
            val d = today.minusDays(i.toLong())
            shortNames.add(d.format(sdfDayInitial).take(2).uppercase(if (lang == "de") Locale.GERMANY else Locale.US))
            dayNums.add(d.dayOfMonth.toString())
        }
        shortNames.reverse()
        dayNums.reverse()
        Pair(shortNames, dayNums)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

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

    val heatmapCanPrevMonth: StateFlow<Boolean> = combine(heatmapMonthCalendar, allHabits) { monthCal, habits ->
        val oldestHabitDateMs = habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
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
        val sdfHeader = SimpleDateFormat("MMMM yyyy", if (lang == "de") Locale.GERMANY else Locale.US)
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
                    val isOutOfRange = dateStr < minDate
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
                val isOutOfRange = dateStr < minDate
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
        val sdfMonth = SimpleDateFormat("MMM", if (lang == "de") Locale.GERMANY else Locale.US)
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

    val oldestHabitDateMs: StateFlow<Long> = allHabits.map { habits ->
        habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    val statsScreenData: StateFlow<List<HabitStatModel>> = combine(allHabits, allLogs, language) { habits, logs, lang ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val daysList = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            daysList.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        daysList.reverse()

        habits.map { habit ->
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
            
            HabitStatModel(
                habit = habit,
                strength = strength,
                past7DaysStatuses = past7DaysStatuses
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

        val today = LocalDate.now()
        val diffMonths = ChronoUnit.MONTHS.between(
            startDate.withDayOfMonth(1),
            today.withDayOfMonth(1)
        )
        val minMonthOffset = -diffMonths.toInt()

        val actualOffset = monthOffset.coerceIn(minMonthOffset, 0)
        val canPrevMonth = actualOffset > minMonthOffset
        val canNextMonth = actualOffset < 0

        val (currentStreak, longestStreak) = calculateStreak(habit, logs)
        val strength = calculateHabitStrength(habit, logs)
        val completionRate = calculateCompletionRate(habit, logs)

        val thisWeekCount = getCompletedLogsCount(habit, logs, "WEEK")
        val thisMonthCount = getCompletedLogsCount(habit, logs, "MONTH")
        val thisYearCount = getCompletedLogsCount(habit, logs, "YEAR")
        val totalCount = getCompletedLogsCount(habit, logs, "ALL")

        val habitLogs = logs.filter { it.habitId == habitId }
        val logsByDate = habitLogs.associateBy { it.date }

        val targetMonthDate = today.withDayOfMonth(1).plusMonths(actualOffset.toLong())
        val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", if (lang == "de") Locale.GERMANY else Locale.US)
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

        val targetStats = com.example.data.calculateTargetPeriodStats(habit, logs)

        // 1. Calculate historical weekday frequency (0 = Mon, ..., 6 = Sun)
        val completedDates = habitLogs.filter { log ->
            isLogCompleted(habit, log)
        }.map { it.date }.toSet()

        val occurrences = IntArray(7)
        val completions = IntArray(7)

        var currentDay = startDate
        if (!currentDay.isAfter(today)) {
            while (!currentDay.isAfter(today)) {
                val dayOfWeekIndex = currentDay.dayOfWeek.value - 1
                if (dayOfWeekIndex in 0..6) {
                    occurrences[dayOfWeekIndex]++
                    if (completedDates.contains(currentDay.toString())) {
                        completions[dayOfWeekIndex]++
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
                val isCompleted = completedDates.contains(dateStr)
                val isFuture = cellDate.isAfter(today)
                val isBeforeStart = cellDate.isBefore(startDate)
                
                val status = when {
                    isBeforeStart || isFuture -> "INACTIVE"
                    isCompleted -> "SUCCESS"
                    else -> "FAILED"
                }
                
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
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM", if (lang == "de") Locale.GERMANY else Locale.US)
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
        val habits = allHabits.value
        val earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
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
        val habits = allHabits.value
        val earliestMs = if (habits.isNotEmpty()) {
            habits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(earliestMs))
    }

    fun adjustDateAndWeekIfOutOfRange() {
        val minDate = getMinDateStr()
        val currentSelected = _selectedDate.value
        if (currentSelected < minDate) {
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

    fun setLanguage(lang: String) {
        _language.value = lang
        sharedPrefs.edit().putString("language", lang).apply()
    }

    // CRUD Habits
    fun addHabit(
        name: String,
        category: String,
        icon: String,
        color: String,
        isNegative: Boolean,
        type: String,
        unit: String,
        targetValue: Float,
        frequency: String,
        startDate: Long,
        specificDays: String = "",
        reminderEnabled: Boolean = false,
        reminderHour: Int = 18,
        reminderMinute: Int = 0
    ) {
        viewModelScope.launch {
            val habit = Habit(
                name = name,
                category = category,
                icon = icon,
                color = color,
                isNegative = isNegative,
                type = type,
                unit = unit,
                targetValue = targetValue,
                frequency = frequency,
                startDate = startDate,
                specificDays = specificDays,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            val insertedId = repository.insertHabit(habit).toInt()
            if (reminderEnabled) {
                com.example.NotificationHelper.scheduleHabitReminder(
                    getApplication(),
                    insertedId,
                    name,
                    reminderHour,
                    reminderMinute
                )
            }
            // Trigger widget update
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            if (habit.reminderEnabled) {
                com.example.NotificationHelper.scheduleHabitReminder(
                    getApplication(),
                    habit.id,
                    habit.name,
                    habit.reminderHour,
                    habit.reminderMinute
                )
            } else {
                com.example.NotificationHelper.cancelHabitReminder(
                    getApplication(),
                    habit.id
                )
            }
            // Trigger widget update
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            
            // Clean up notes with no active habits
            try {
                val remainingHabits = repository.allHabits.first()
                val allNotes = repository.allDailyNotes.first()
                allNotes.forEach { note ->
                    val hasActiveHabit = remainingHabits.any { isHabitActiveOnDate(it, note.date) }
                    if (!hasActiveHabit) {
                        repository.saveDailyNote(note.date, "")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Adjust current selected date and week start if they became out-of-bounds
            adjustDateAndWeekIfOutOfRange()

            com.example.NotificationHelper.cancelHabitReminder(
                getApplication(),
                habit.id
            )
            if (_selectedHabitIdForDetail.value == habit.id) {
                _selectedHabitIdForDetail.value = null
            }
            // Trigger widget update
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun archiveHabit(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(isArchived = true)
            repository.updateHabit(updated)
            com.example.NotificationHelper.cancelHabitReminder(
                getApplication(),
                habit.id
            )
            // Trigger widget update
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun unarchiveHabit(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(isArchived = false)
            repository.updateHabit(updated)
            if (updated.reminderEnabled) {
                com.example.NotificationHelper.scheduleHabitReminder(
                    getApplication(),
                    updated.id,
                    updated.name,
                    updated.reminderHour,
                    updated.reminderMinute
                )
            }
            // Trigger widget update
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun moveHabitUp(habitId: Int) {
        viewModelScope.launch {
            val list = allHabits.value
            val index = list.indexOfFirst { it.id == habitId }
            if (index > 0) {
                val updatedHabits = list.mapIndexed { idx, h ->
                    h.copy(sortOrder = idx)
                }.toMutableList()
                
                val temp = updatedHabits[index]
                updatedHabits[index] = updatedHabits[index - 1].copy(sortOrder = index)
                updatedHabits[index - 1] = temp.copy(sortOrder = index - 1)
                
                updatedHabits.forEach { h ->
                    repository.updateHabit(h)
                }
                HabitWidgetProvider.triggerUpdate(getApplication())
            }
        }
    }

    fun moveHabitDown(habitId: Int) {
        viewModelScope.launch {
            val list = allHabits.value
            val index = list.indexOfFirst { it.id == habitId }
            if (index != -1 && index < list.size - 1) {
                val updatedHabits = list.mapIndexed { idx, h ->
                    h.copy(sortOrder = idx)
                }.toMutableList()
                
                val temp = updatedHabits[index]
                updatedHabits[index] = updatedHabits[index + 1].copy(sortOrder = index)
                updatedHabits[index + 1] = temp.copy(sortOrder = index + 1)
                
                updatedHabits.forEach { h ->
                    repository.updateHabit(h)
                }
                HabitWidgetProvider.triggerUpdate(getApplication())
            }
        }
    }

    // Log tracking
    fun toggleBinaryHabit(habitId: Int, date: String, isCurrentlyCompleted: Boolean) {
        viewModelScope.launch {
            val habit = allHabits.value.find { it.id == habitId } ?: return@launch
            val existingLogs = repository.getLogsForHabitOnDate(habitId, date)
            val log = existingLogs.firstOrNull()

            val currentStatus = when {
                log == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
                log.value == -1f -> "FAILED"
                log.value == -2f -> "SUCCESS"
                else -> {
                    if (habit.type == "BINARY") {
                        if (habit.isNegative) "FAILED" else "SUCCESS"
                    } else {
                        if (habit.isNegative) {
                            if (log.value >= habit.targetValue) "FAILED" else "PENDING"
                        } else {
                            if (log.value >= habit.targetValue) "SUCCESS" else "PENDING"
                        }
                    }
                }
            }

            val nextStatus = when (currentStatus) {
                "PENDING" -> "SUCCESS"
                "SUCCESS" -> "FAILED"
                else -> "PENDING"
            }

            if (nextStatus == "PENDING") {
                repository.unlogHabit(habitId, date)
            } else {
                val nextValue = if (nextStatus == "SUCCESS") {
                    if (habit.type == "BINARY") -2f else habit.targetValue
                } else {
                    -1f
                }
                repository.logHabit(habitId, date, nextValue)
            }
            // Refresh widget state
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun logNumericalHabit(habitId: Int, date: String, value: Float) {
        viewModelScope.launch {
            if (value == 0f) {
                repository.unlogHabit(habitId, date)
            } else {
                repository.logHabit(habitId, date, value)
            }
            // Refresh widget state
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun togglePauseHabit(habitId: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.togglePauseHabit(habitId, date)
            // Refresh widget state
            HabitWidgetProvider.triggerUpdate(getApplication())
        }
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
        _syncStatus.value = if (language.value == "de") "Sicherungsordner gespeichert! Täglicher Export ist aktiv." else "Backup folder saved! Daily export is active."
    }

    fun triggerManualBackup() {
        viewModelScope.launch {
            _syncStatus.value = if (language.value == "de") "Sicherung wird erstellt..." else "Creating backup..."
            val uriStr = _backupFolderUri.value
            if (uriStr.isEmpty()) {
                _syncStatus.value = if (language.value == "de") "Fehler: Kein Ordner ausgewählt!" else "Error: No folder selected!"
                return@launch
            }
            val success = BackupManager.performBackup(getApplication(), uriStr)
            _syncStatus.value = if (success) {
                if (language.value == "de") "Sicherung erfolgreich erstellt! 🎉" else "Backup created successfully! 🎉"
            } else {
                if (language.value == "de") "Fehler beim Erstellen der Sicherung!" else "Error creating backup!"
            }
        }
    }

    fun triggerManualRestore(jsonString: String) {
        viewModelScope.launch {
            _syncStatus.value = if (language.value == "de") "Daten werden wiederhergestellt..." else "Restoring data..."
            val success = BackupManager.restoreDatabaseFromJson(getApplication(), jsonString)
            _syncStatus.value = if (success) {
                if (language.value == "de") "Sicherung erfolgreich wiederhergestellt! 🎉" else "Backup successfully restored! 🎉"
            } else {
                if (language.value == "de") "Fehler bei der Wiederherstellung! Ungültige Datei." else "Error during restore! Invalid file."
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
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
            java.time.LocalDate.ofInstant(instant, zoneId).toEpochDay().toInt()
        } catch (e: Exception) {
            (millis / 86400000L).toInt()
        }
    }

    // STATS CALCULATION FUNCTIONS
    fun calculateStreak(habit: Habit, logs: List<HabitLog>): Pair<Int, Int> {
        // Return (currentStreak, longestStreak)
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr }
        if (habitLogs.isEmpty() && !habit.isNegative) return 0 to 0

        val completedDates = mutableSetOf<String>()
        val loggedDates = mutableSetOf<String>()
        val pausedDates = mutableSetOf<String>()
        habitLogs.forEach { log ->
            if (log.isPaused) {
                pausedDates.add(log.date)
            }
            val isCompleted = isLogCompleted(habit, log)
            if (isCompleted && !log.isPaused) {
                completedDates.add(log.date)
            }
            if (!log.isPaused) {
                loggedDates.add(log.date)
            }
        }

        if (habit.frequency == "TIMES_WEEKLY") {
            val targetTimes = habit.specificDays.toIntOrNull() ?: 3
            val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            // Setup start of start week and today's week
            val cal = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = validStartMillis
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startWeekMonday = cal.timeInMillis

            val todayCal = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = System.currentTimeMillis()
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endWeekSunday = todayCal.timeInMillis

            val currentWeekMonday = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = System.currentTimeMillis()
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            } .timeInMillis

            val weekRanges = mutableListOf<Pair<Long, Long>>()
            val loopCal = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = startWeekMonday
            }

            while (loopCal.timeInMillis <= endWeekSunday) {
                val mon = loopCal.timeInMillis
                loopCal.add(Calendar.DAY_OF_WEEK, 6)
                val sun = loopCal.timeInMillis
                weekRanges.add(mon to sun)
                loopCal.add(Calendar.DAY_OF_WEEK, 1)
            }

            val weekSuccessList = mutableListOf<Boolean>()
            for (i in weekRanges.indices) {
                val (mon, sun) = weekRanges[i]
                var completedCount = 0
                var pausedCount = 0
                val dayCal = Calendar.getInstance(Locale.GERMANY).apply {
                    timeInMillis = mon
                }
                for (d in 0 until 7) {
                    val dStr = sdfDb.format(dayCal.time)
                    if (completedDates.contains(dStr)) {
                        completedCount++
                    }
                    if (pausedDates.contains(dStr)) {
                        pausedCount++
                    }
                    dayCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val adjustedTarget = (targetTimes - pausedCount).coerceAtLeast(0)
                val isWeekSuccessful = completedCount >= adjustedTarget

                val isCurrentWeek = (mon == currentWeekMonday)
                if (isCurrentWeek) {
                    if (isWeekSuccessful) {
                        weekSuccessList.add(true)
                    }
                } else {
                    weekSuccessList.add(isWeekSuccessful)
                }
            }

            var longestW = 0
            var tempW = 0
            for (success in weekSuccessList) {
                if (success) {
                    tempW++
                    if (tempW > longestW) longestW = tempW
                } else {
                    tempW = 0
                }
            }

            var currentW = 0
            for (j in weekSuccessList.indices.reversed()) {
                if (weekSuccessList[j]) {
                    currentW++
                } else {
                    break
                }
            }

            return currentW to longestW
        }

        if (habit.isNegative && loggedDates.isEmpty() && validStartMillis >= System.currentTimeMillis()) return 0 to 0

        val todayEpoch = millisToEpochDays(System.currentTimeMillis())
        val startEpoch = millisToEpochDays(validStartMillis)

        if (startEpoch > todayEpoch) return 0 to 0

        val completedEpochDays = completedDates.map { dateToEpochDaysFast(it) }.toSet()
        val loggedEpochDays = loggedDates.map { dateToEpochDaysFast(it) }.toSet()
        val pausedEpochDays = pausedDates.map { dateToEpochDaysFast(it) }.toSet()

        val getDayStr = { ep: Int ->
            val localDate = java.time.LocalDate.ofEpochDay(ep.toLong())
            String.format(Locale.US, "%04d-%02d-%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
        }

        var longest = 0
        var tempStreak = 0

        for (d in startEpoch..todayEpoch) {
            val dStr = getDayStr(d)
            if (!isHabitActiveOnDate(habit, dStr) || pausedEpochDays.contains(d)) {
                continue
            }

            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(d) || completedEpochDays.contains(d)
            } else {
                completedEpochDays.contains(d)
            }

            if (successful) {
                tempStreak++
                if (tempStreak > longest) longest = tempStreak
            } else {
                tempStreak = 0
            }
        }

        // Current streak backwards
        var currentStreak = 0
        var cursor = todayEpoch
        var continueChecking = true

        while (continueChecking && cursor >= startEpoch) {
            val dStr = getDayStr(cursor)
            if (!isHabitActiveOnDate(habit, dStr) || pausedEpochDays.contains(cursor)) {
                cursor--
                continue
            }

            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(cursor) || completedEpochDays.contains(cursor)
            } else {
                completedEpochDays.contains(cursor)
            }

            if (successful) {
                currentStreak++
                cursor--
            } else {
                if (cursor == todayEpoch) {
                    // Check yesterday (and skip inactive days)
                    var prev = cursor - 1
                    var prevStr = getDayStr(prev)
                    while (prev >= startEpoch && (!isHabitActiveOnDate(habit, prevStr) || pausedEpochDays.contains(prev))) {
                        prev--
                        if (prev >= startEpoch) {
                            prevStr = getDayStr(prev)
                        }
                    }
                    
                    val yesterdaySuccessful = if (prev >= startEpoch) {
                        if (habit.isNegative) {
                            !loggedEpochDays.contains(prev) || completedEpochDays.contains(prev)
                        } else {
                            completedEpochDays.contains(prev)
                        }
                    } else {
                        false
                    }

                    if (yesterdaySuccessful && prev >= startEpoch) {
                        cursor = prev
                    } else {
                        continueChecking = false
                    }
                } else {
                    continueChecking = false
                }
            }
        }

        return currentStreak to longest
    }

    fun calculateTotalStrength(habits: List<Habit>, logs: List<HabitLog>): Int {
        if (habits.isEmpty()) return 0
        // Calculate average strength score of all habits
        val strengths = habits.map { calculateHabitStrength(it, logs) }
        return strengths.average().toInt().coerceIn(0, 100)
    }

    fun calculateHabitStrength(habit: Habit, logs: List<HabitLog>): Int {
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr }
        
        val completedEpochDays = habitLogs.filter { log ->
            isLogCompleted(habit, log)
        }.map { dateToEpochDaysFast(it.date) }.toSet()
        
        if (habit.frequency == "TIMES_WEEKLY") {
            val targetTimes = habit.specificDays.toIntOrNull() ?: 3
            val activeDays = (System.currentTimeMillis() - validStartMillis) / (24 * 3600 * 1000) + 1
            val activeWeeks = (activeDays / 7.0).coerceAtMost(4.0).coerceAtLeast(1.0)
            val expectedCompletions = (activeWeeks * targetTimes).toInt().coerceAtLeast(1)
            
            val limitMillis = System.currentTimeMillis() - 28L * 24 * 3600 * 1000
            val limitSdfStr = startSdf.format(Date(limitMillis.coerceAtLeast(validStartMillis)))
            val completedInLast4Weeks = habitLogs.filter { log ->
                val isCompleted = when (log.value) {
                    -1f -> false
                    -2f -> true
                    else -> if (habit.type == "BINARY") true else log.value >= habit.targetValue
                }
                isCompleted && !log.isPaused && log.date >= limitSdfStr
            }.size
            
            return (completedInLast4Weeks.toFloat() / expectedCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
        }
        
        val loggedEpochDays = habitLogs.map { dateToEpochDaysFast(it.date) }.toSet()

        val todayEpoch = millisToEpochDays(System.currentTimeMillis())
        val startEpoch = millisToEpochDays(validStartMillis)

        var weightedCompleted = 0
        var totalPossibleWeight = 0
        val totalDaysToCheck = 30

        val maxDays = if (todayEpoch - startEpoch + 1 < totalDaysToCheck) {
            todayEpoch - startEpoch + 1
        } else {
            totalDaysToCheck
        }

        if (maxDays <= 0) return 0

        for (i in 0 until maxDays) {
            val currentEpoch = todayEpoch - i
            val dayWeight = maxDays - i
            
            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(currentEpoch)
            } else {
                completedEpochDays.contains(currentEpoch)
            }
            
            if (successful) {
                weightedCompleted += dayWeight
            }
            totalPossibleWeight += dayWeight
        }

        val percentage = if (totalPossibleWeight > 0) {
            (weightedCompleted.toFloat() / totalPossibleWeight.toFloat() * 100).toInt()
        } else {
            0
        }
        return percentage.coerceIn(0, 100)
    }

    fun calculateCompletionRate(habit: Habit, logs: List<HabitLog>): Int {
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr }
        
        val completedEpochDays = habitLogs.filter { log ->
            isLogCompleted(habit, log)
        }.map { dateToEpochDaysFast(it.date) }.toSet()
        
        if (habit.frequency == "TIMES_WEEKLY") {
            val targetTimes = habit.specificDays.toIntOrNull() ?: 3
            val cal = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = validStartMillis
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startWeekMonday = cal.timeInMillis

            val todayCal = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = System.currentTimeMillis()
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endWeekSunday = todayCal.timeInMillis

            var totalExpectedCompletions = 0
            val loopCal = Calendar.getInstance(Locale.GERMANY).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = startWeekMonday
            }
            while (loopCal.timeInMillis <= endWeekSunday) {
                totalExpectedCompletions += targetTimes
                loopCal.add(Calendar.WEEK_OF_YEAR, 1)
            }

            if (totalExpectedCompletions == 0) return 0
            val completedCount = habitLogs.filter { log ->
                isLogCompleted(habit, log)
            }.size
            return (completedCount.toFloat() / totalExpectedCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
        }
        
        val loggedEpochDays = habitLogs.map { dateToEpochDaysFast(it.date) }.toSet()

        val todayEpoch = millisToEpochDays(System.currentTimeMillis())
        val startEpoch = millisToEpochDays(validStartMillis)

        if (startEpoch > todayEpoch) return 0

        var completedDays = 0
        val totalDays = todayEpoch - startEpoch + 1

        for (d in startEpoch..todayEpoch) {
            val successful = if (habit.isNegative) {
                !loggedEpochDays.contains(d) || completedEpochDays.contains(d)
            } else {
                completedEpochDays.contains(d)
            }
            if (successful) completedDays++
        }

        return if (totalDays > 0) {
            (completedDays.toFloat() / totalDays.toFloat() * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    fun getCompletedLogsCount(habit: Habit, logs: List<HabitLog>, period: String): Int {
        val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val habitLogs = logs.filter { it.habitId == habit.id && it.date >= startSdfStr }
        val cal = Calendar.getInstance()
        val limit = when (period) {
            "WEEK" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "YEAR" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }

        if (!habit.isNegative) {
            return habitLogs.count { log ->
                val logTime = parseDateStringToMillis(log.date)
                logTime >= limit && isLogCompleted(habit, log)
            }
        } else {
            val startEpoch = millisToEpochDays(validStartMillis)
            val todayEpoch = millisToEpochDays(System.currentTimeMillis())
            val limitEpoch = if (limit > 0L) millisToEpochDays(limit) else startEpoch
            
            val searchStart = maxOf(startEpoch, limitEpoch)
            if (searchStart > todayEpoch) return 0

            val completedEpochDays = habitLogs.filter { log ->
                isLogCompleted(habit, log)
            }.map { dateToEpochDaysFast(it.date) }.toSet()

            val loggedEpochDays = habitLogs.map { dateToEpochDaysFast(it.date) }.toSet()

            var completedDays = 0
            val getDayStr = { ep: Int ->
                val localDate = java.time.LocalDate.ofEpochDay(ep.toLong())
                String.format(Locale.US, "%04d-%02d-%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
            }

            for (d in searchStart..todayEpoch) {
                val dStr = getDayStr(d)
                if (!isHabitActiveOnDate(habit, dStr)) {
                    continue
                }
                val successful = !loggedEpochDays.contains(d) || completedEpochDays.contains(d)
                if (successful) {
                    completedDays++
                }
            }
            return completedDays
        }
    }

    // Helper functions for dates
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun calculatePerfectDaysStats(habits: List<Habit>, logs: List<HabitLog>): PerfectDaysStats {
        if (habits.isEmpty()) return PerfectDaysStats(0, 0, 0, 0)

        val todayEpoch = millisToEpochDays(System.currentTimeMillis())
        
        // Oldest start epoch
        var oldestStartEpoch = todayEpoch
        habits.forEach { habit ->
            val validStartMillis = if (habit.startDate > 946684800000L) {
                habit.startDate
            } else if (habit.createdAt > 946684800000L) {
                habit.createdAt
            } else {
                System.currentTimeMillis()
            }
            val startEpoch = millisToEpochDays(validStartMillis)
            if (startEpoch < oldestStartEpoch) {
                oldestStartEpoch = startEpoch
            }
        }

        if (oldestStartEpoch > todayEpoch) return PerfectDaysStats(0, 0, 0, 0)

        // Pre-group logs by date for fast lookup
        val logsByDateAndHabit = logs.groupBy { it.date }.mapValues { entry ->
            entry.value.associateBy { it.habitId }
        }

        var totalPerfectDays = 0
        var perfectDaysStreak = 0
        var currentPerfectStreak = 0

        var totalCompletedCompletions = 0
        var totalPossibleCompletions = 0

        for (epochDay in oldestStartEpoch..todayEpoch) {
            val date = java.time.LocalDate.ofEpochDay(epochDay.toLong())
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)
            val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            val activeHabits = habits.filter { habit ->
                isHabitActiveOnDate(habit, dateStr)
            }

            if (activeHabits.isEmpty()) {
                continue
            }

            val dayLogs = logsByDateAndHabit[dateStr] ?: emptyMap()
            var allCompletedThisDay = true
            var checkedAnyOnDay = false

            activeHabits.forEach { habit ->
                if (habit.frequency == "TIMES_WEEKLY") {
                    return@forEach
                }
                val log = dayLogs[habit.id]
                val isPaused = log != null && log.isPaused
                if (!isPaused) {
                    checkedAnyOnDay = true
                    totalPossibleCompletions++
                    val successful = if (log != null) {
                        when (log.value) {
                            -1f -> false
                            -2f -> true
                            else -> {
                                if (habit.type == "BINARY") {
                                    if (habit.isNegative) false else true
                                } else {
                                    if (habit.isNegative) log.value < habit.targetValue else log.value >= habit.targetValue
                                }
                            }
                        }
                    } else {
                        habit.isNegative
                    }

                    if (successful) {
                        totalCompletedCompletions++
                    } else {
                        allCompletedThisDay = false
                    }
                }
            }

            if (checkedAnyOnDay && allCompletedThisDay) {
                totalPerfectDays++
                currentPerfectStreak++
                if (currentPerfectStreak > perfectDaysStreak) {
                    perfectDaysStreak = currentPerfectStreak
                }
            } else if (checkedAnyOnDay) {
                currentPerfectStreak = 0
            }
        }

        val completionRate = if (totalPossibleCompletions > 0) {
            (totalCompletedCompletions.toFloat() / totalPossibleCompletions.toFloat() * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return PerfectDaysStats(
            totalPerfectDays = totalPerfectDays,
            perfectDaysStreak = perfectDaysStreak,
            totalCompletedHabits = totalCompletedCompletions,
            totalCompletionRate = completionRate
        )
    }

    private val dateToMillisCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun parseDateStringToMillis(dateStr: String): Long {
        return dateToMillisCache.getOrPut(dateStr) {
            try {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    val localDate = java.time.LocalDate.of(year, month, day)
                    localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    System.currentTimeMillis()
                }
            } catch (e: Exception) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (ex: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            selectedDate.collect {
                _heatmapMonthOffset.value = 0
            }
        }
        viewModelScope.launch {
            try {
                val list = database.habitDao().getAllHabitsRaw()
                list.forEach { habit ->
                    if (habit.reminderEnabled) {
                        com.example.NotificationHelper.scheduleHabitReminder(
                            getApplication(),
                            habit.id,
                            habit.name,
                            habit.reminderHour,
                            habit.reminderMinute
                        )
                    }
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
