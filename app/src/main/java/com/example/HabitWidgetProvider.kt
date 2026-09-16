package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.data.AppDatabase
import com.frequent.habits.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextMidnightAlarm(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scheduleNextMidnightAlarm(context)
        val pendingResult = goAsync()
        updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_UPDATE_HABITS) {
            scheduleNextMidnightAlarm(context)
            val pendingResult = goAsync()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            updateAllWidgets(context, appWidgetManager, ids, pendingResult, isFullUpdate = false)
        } else if (action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED" ||
            action == "android.intent.action.TIME_SET") {
            
            scheduleNextMidnightAlarm(context)
            if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.MY_PACKAGE_REPLACED") {
                com.example.NotificationHelper.scheduleSmartInsightNotifications(context)
                com.example.NotificationHelper.scheduleReviewNotifications(context)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val db = com.example.data.AppDatabase.getDatabase(context).habitDao()
                        val habits = db.getAllHabitsRaw()
                        habits.forEach { habit ->
                            com.example.NotificationHelper.scheduleAllHabitReminders(context, habit)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            val pendingResult = goAsync()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            updateAllWidgets(context, appWidgetManager, ids, pendingResult, isFullUpdate = true)
        } else if (action == ACTION_TOGGLE_BINARY_HABIT) {
            val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
            if (habitId != -1) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        performToggleHabit(context, habitId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (action == ACTION_WIDGET_ADD_VALUE_DIRECT) {
            val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
            val delta = intent.getFloatExtra(EXTRA_DELTA, 0f)
            if (habitId != -1 && delta != 0f) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        performDeltaHabit(context, habitId, delta)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (action == ACTION_WIDGET_ITEM_CLICK) {
            val itemAction = intent.getStringExtra("WIDGET_ACTION")
            val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
            val delta = intent.getFloatExtra(EXTRA_DELTA, 0f)

            if (itemAction == "TOGGLE" || itemAction == "DELTA") {
                val now = System.currentTimeMillis()
                if (habitId == lastClickedHabitId && (now - lastClickTime) < 350L) {
                    return
                }
                lastClickedHabitId = habitId
                lastClickTime = now
            }

            if (itemAction == "TOGGLE" && habitId != -1) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        performToggleHabit(context, habitId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else if (itemAction == "DELTA" && habitId != -1 && delta != 0f) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        performDeltaHabit(context, habitId, delta)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else if (itemAction == "TOGGLE_FOLDER_COLLAPSE") {
                val folderId = intent.getIntExtra(EXTRA_FOLDER_ID, -1)
                if (folderId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            com.example.data.FolderStorageHelper.toggleFolderCollapse(context, folderId)
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
                            val ids = appWidgetManager.getAppWidgetIds(componentName)
                            ids.forEach { wId ->
                                appWidgetManager.notifyAppWidgetViewDataChanged(wId, R.id.widget_habits_list)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            } else if (itemAction == "TOGGLE_FOLDER_COMPLETION") {
                val folderId = intent.getIntExtra(EXTRA_FOLDER_ID, -1)
                if (folderId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            performToggleFolderCompletion(context, folderId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            } else if (itemAction == "OPEN_APP" && habitId != -1) {
                val db = AppDatabase.getDatabase(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val habit = db.habitDao().getHabitByIdSuspend(habitId)
                        if (habit != null) {
                            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                                setAction(ACTION_WIDGET_ADD_VALUE)
                                putExtra(EXTRA_HABIT_ID, habitId)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            context.startActivity(openAppIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun performToggleHabit(context: Context, habitId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, HabitWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)

        updateMutex.withLock {
            val db = AppDatabase.getDatabase(context)
            val habit = db.habitDao().getHabitByIdSuspend(habitId) ?: return@withLock
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())
            db.habitDao().toggleHabitTransaction(
                habitId = habitId,
                selectedDate = todayStr,
                isNegative = habit.isNegative,
                type = habit.type,
                targetValue = habit.targetValue
            )
            updateAllWidgetsSuspendInternal(context, appWidgetManager, ids, isFullUpdate = false)
        }
    }

    private suspend fun performDeltaHabit(context: Context, habitId: Int, delta: Float) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, HabitWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)

        updateMutex.withLock {
            val db = AppDatabase.getDatabase(context)
            val habit = db.habitDao().getHabitByIdSuspend(habitId) ?: return@withLock
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())
            db.habitDao().deltaHabitTransaction(
                habitId = habitId,
                selectedDate = todayStr,
                delta = delta,
                targetValue = habit.targetValue
            )
            updateAllWidgetsSuspendInternal(context, appWidgetManager, ids, isFullUpdate = false)
        }
    }

    private suspend fun performToggleFolderCompletion(context: Context, folderId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, HabitWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)

        updateMutex.withLock {
            val db = AppDatabase.getDatabase(context)
            val allHabits = db.habitDao().getAllHabitsRaw()
            val habits = allHabits.filter { it.folderId == folderId && !it.isArchived }
            if (habits.isEmpty()) return@withLock
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())
            val logs = db.habitDao().getLogsForDateRaw(todayStr).associateBy { it.habitId }

            val activeHabits = habits.filter { com.example.data.isHabitActiveOnDate(it, todayStr) && logs[it.id]?.isPaused != true }
            if (activeHabits.isEmpty()) return@withLock

            val allCompleted = activeHabits.all { habit ->
                val log = logs[habit.id]
                if (habit.isNegative) {
                    log == null || com.example.data.isLogCompleted(habit, log)
                } else {
                    log != null && com.example.data.isLogCompleted(habit, log)
                }
            }

            val allFailed = activeHabits.all { habit ->
                val log = logs[habit.id]
                log != null && log.value == -1f
            }

            if (allCompleted) {
                // Stage 2: Mark all as failed (-1f)
                activeHabits.forEach { habit ->
                    db.habitDao().deleteLogsForHabitOnDate(habit.id, todayStr)
                    db.habitDao().insertLog(com.example.data.HabitLog(
                        habitId = habit.id,
                        date = todayStr,
                        value = -1f,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            } else if (allFailed) {
                // Stage 3: Reset all to pending/clean
                activeHabits.forEach { habit ->
                    db.habitDao().deleteLogsForHabitOnDate(habit.id, todayStr)
                }
            } else {
                // Stage 1: Mark all as completed
                activeHabits.forEach { habit ->
                    db.habitDao().deleteLogsForHabitOnDate(habit.id, todayStr)
                    if (habit.isNegative) {
                        // for negative habit, completed means no negative log (clean)
                    } else {
                        val valueToLog = if (habit.type == "BINARY") -2f else habit.targetValue
                        db.habitDao().insertLog(com.example.data.HabitLog(
                            habitId = habit.id,
                            date = todayStr,
                            value = valueToLog,
                            timestamp = System.currentTimeMillis()
                        ))
                    }
                }
            }
            updateAllWidgetsSuspendInternal(context, appWidgetManager, ids, isFullUpdate = false)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        pendingResult: BroadcastReceiver.PendingResult? = null,
        isFullUpdate: Boolean = true
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateAllWidgetsSuspend(context, appWidgetManager, appWidgetIds, isFullUpdate)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { pendingResult?.finish() } catch (e: Exception) {}
            }
        }
    }

    private suspend fun updateAllWidgetsSuspend(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        isFullUpdate: Boolean = true
    ) {
        updateMutex.withLock {
            updateAllWidgetsSuspendInternal(context, appWidgetManager, appWidgetIds, isFullUpdate)
        }
    }

    private suspend fun updateAllWidgetsSuspendInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        isFullUpdate: Boolean
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val db = AppDatabase.getDatabase(context)
        val allHabits = db.habitDao().getAllHabitsRaw()

        appWidgetIds.forEach { widgetId ->
            val selectedDate = todayStr
            val widgetLogs = db.habitDao().getLogsForDateRaw(selectedDate)
            val logsMap = widgetLogs.associateBy { it.habitId }
            val allLogs = db.habitDao().getAllLogsRaw()

            val activeHabits = allHabits
                .filter { !it.isArchived && com.example.data.isHabitActiveOnDate(it, todayStr) }
                .sortedWith(compareBy<com.example.data.Habit> { it.sortOrder }.thenByDescending { it.id })

            var completed = 0
            var nonPausedCount = 0

            val curDate = try { java.time.LocalDate.parse(todayStr) } catch (e: Exception) { java.time.LocalDate.now() }
            val startOf7Days = curDate.minusDays(6).toString()
            val endOf7Days = curDate.toString()

            activeHabits.forEach { habit ->
                val log = logsMap[habit.id]
                val isPaused = log != null && log.isPaused
                if (!isPaused) {
                    nonPausedCount++
                    val isDone = com.example.data.isLogCompleted(habit, log)
                    if (isDone) {
                        completed++
                    }
                }
            }

            val sharedPrefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
            val habitPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
            val isDark = sharedPrefs.getBoolean("dark_mode_enabled", habitPrefs.getBoolean("dark_mode_enabled", true))

            val accentColorName = sharedPrefs.getString("accent_color_name", null)
                ?: habitPrefs.getString("accent_color_name", "PURPLE")
                ?: "PURPLE"
            val accentColorInt = com.example.ui.HabitIconMapping.getColor(accentColorName).toArgb()

            val progressPercent = if (nonPausedCount > 0) (completed.toFloat() / nonPausedCount * 100).toInt() else 0
            val isCompleted = progressPercent >= 100 && nonPausedCount > 0

            val layoutId = if (isDark) R.layout.habit_widget else R.layout.habit_widget_light
            val views = RemoteViews(context.packageName, layoutId)

            val perfectStats = com.example.data.StreakCalculator.calculate(allHabits, allLogs, targetDateStr = todayStr)
            val currentStreak = perfectStats.currentStreak

            val streakBgRes = if (currentStreak >= 1) {
                if (isDark) R.drawable.widget_streak_bg_active else R.drawable.widget_streak_bg_light_active
            } else {
                if (isDark) R.drawable.widget_streak_bg else R.drawable.widget_streak_bg_light
            }
            views.setInt(R.id.widget_streak_container, "setBackgroundResource", streakBgRes)

            val progressBitmap = drawProgressBarBitmap(progressPercent, isCompleted, accentColorInt, isDark)
            views.setImageViewBitmap(R.id.widget_progress_bar, progressBitmap)

            sharedPrefs.edit().putInt("current_perfect_streak", currentStreak).apply()
            views.setTextViewText(R.id.widget_streak_text, "${currentStreak}d")
            views.setTextColor(R.id.widget_streak_text, if (isDark) Color.parseColor("#E4E3EC") else Color.parseColor("#111115"))
            val flameColor = if (currentStreak > 0) Color.parseColor("#00E5FF") else (if (isDark) Color.parseColor("#60808080") else Color.parseColor("#909090"))
            views.setInt(R.id.widget_streak_icon, "setColorFilter", flameColor)

            if (isFullUpdate) {
                val widgetOpacity = sharedPrefs.getFloat("widget_opacity", 1.0f)
                val bgBitmap = drawWidgetBackgroundBitmapStatic(isDark, widgetOpacity)
                views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

                val openAppInt = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingInt = PendingIntent.getActivity(
                    context,
                    widgetId * 5000,
                    openAppInt,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_progress_bar, pendingInt)
                views.setOnClickPendingIntent(R.id.widget_streak_container, pendingInt)

                val serviceIntent = Intent(context, HabitWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse("custom://widget/habits_list/$widgetId")
                }
                views.setRemoteAdapter(R.id.widget_habits_list, serviceIntent)

                val clickIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_ITEM_CLICK
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val clickPIntent = PendingIntent.getBroadcast(
                    context,
                    widgetId * 1000 + 5,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.widget_habits_list, clickPIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            } else {
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
            }

            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_habits_list)
        }
    }

    private fun scheduleNextMidnightAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val intent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_HABITS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                998877,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDisplayDate(dateStr: String): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return if (dateStr == todayStr) "Heute" else {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                if (date != null) {
                    SimpleDateFormat("dd.MM", Locale.GERMAN).format(date)
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        }
    }

    private fun drawProgressBarBitmap(progressPercent: Int, isCompleted: Boolean, accentColorInt: Int, isDark: Boolean): Bitmap {
        return com.example.widget.WidgetBitmapUtils.drawProgressBarBitmap(progressPercent, isCompleted, accentColorInt, isDark)
    }

    private fun getColorInt(colorName: String): Int {
        return com.example.widget.WidgetBitmapUtils.getColorInt(colorName)
    }

    fun drawIconToBitmap(context: Context, iconName: String, colorInt: Int): Bitmap {
        return com.example.widget.WidgetBitmapUtils.drawIconToBitmap(context, iconName, colorInt)
    }

    companion object {
        const val ACTION_UPDATE_HABITS = "com.example.widget.ACTION_UPDATE_HABITS"
        const val ACTION_TOGGLE_BINARY_HABIT = "com.example.widget.ACTION_TOGGLE_BINARY_HABIT"
        const val ACTION_WIDGET_ADD_VALUE = "com.example.widget.ACTION_WIDGET_ADD_VALUE"
        const val ACTION_WIDGET_ADD_VALUE_DIRECT = "com.example.widget.ACTION_WIDGET_ADD_VALUE_DIRECT"
        const val ACTION_WIDGET_ITEM_CLICK = "com.example.widget.ACTION_WIDGET_ITEM_CLICK"
        const val EXTRA_HABIT_ID = "com.example.widget.EXTRA_HABIT_ID"
        const val EXTRA_DELTA = "com.example.widget.EXTRA_DELTA"
        const val EXTRA_FOLDER_ID = "com.example.widget.EXTRA_FOLDER_ID"

        private val updateMutex = Mutex()

        @Volatile
        private var lastClickTime = 0L
        @Volatile
        private var lastUpdateAllTime = 0L

        @Volatile
        private var lastClickedHabitId = -1
        @Volatile
        private var lastUpdateTriggerTime = 0L

        private var debounceJob: kotlinx.coroutines.Job? = null

        fun triggerUpdate(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val intent = Intent(appContext, HabitWidgetProvider::class.java).apply {
                        action = ACTION_UPDATE_HABITS
                    }
                    appContext.sendBroadcast(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun drawProgressBarBitmapStatic(progressPercent: Int, isCompleted: Boolean, accentColorInt: Int, isDark: Boolean): Bitmap {
            return com.example.widget.WidgetBitmapUtils.drawProgressBarBitmap(progressPercent, isCompleted, accentColorInt, isDark)
        }

        fun drawWidgetBackgroundBitmapStatic(isDark: Boolean, opacity: Float): Bitmap {
            return com.example.widget.WidgetBitmapUtils.drawWidgetBackgroundBitmapStatic(isDark, opacity)
        }
    }
}
