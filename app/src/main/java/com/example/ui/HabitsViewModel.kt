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

class HabitsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    init {
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

    // Reactively loaded habits and logs
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
        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startSdfStr = startSdf.format(Date(validStartMillis))

        val currentCal = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { timeInMillis = validStartMillis }
        val diffYears = currentCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        val diffMonths = currentCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        val minMonthOffset = -(diffYears * 12 + diffMonths)

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
        val cal = Calendar.getInstance()
        if (actualOffset != 0) {
            cal.add(Calendar.MONTH, actualOffset)
        }
        val sdfMonthName = SimpleDateFormat("MMMM yyyy", if (lang == "de") Locale.GERMANY else Locale.US)
        val monthNameStr = sdfMonthName.format(cal.time)
        val currentMonthDaysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekOffset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdfDb.format(Date())
        val daysList = mutableListOf<CalendarCellState?>()

        for (i in 0 until firstDayOfWeekOffset) {
            daysList.add(null)
        }
        for (i in 1..currentMonthDaysCount) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val dateStr = sdfDb.format(cal.time)
            val log = logsByDate[dateStr]
            val isCompleted = isLogCompleted(habit, log)
            val status = getLogStatus(habit, log, dateStr, startSdfStr, todayStr)
            
            daysList.add(CalendarCellState(id = dateStr, dayNum = i.toString(), isCompleted = isCompleted, status = status))
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
            completionRate = completionRate
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectDate(date: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (date > todayStr) return
        _selectedDate.value = date
    }

    fun selectDateAndSyncWeek(dateStr: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dateStr > todayStr) return
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
            if (value <= 0f) {
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
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.timeInMillis
            }
            "YEAR" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.timeInMillis
            }
            else -> 0L
        }

        return habitLogs.count { log ->
            val logTime = parseDateStringToMillis(log.date)
            logTime >= limit && isLogCompleted(habit, log)
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
            val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
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
}
