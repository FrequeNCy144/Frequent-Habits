package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.HabitLog
import com.frequent.habits.R
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking

class HabitWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetFactory(applicationContext, intent)
    }
}

class HabitWidgetFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private var activeHabits = listOf<Habit>()
    private var logsMap = mapOf<Int, HabitLog>()
    private var allLogsList = listOf<HabitLog>()
    private var selectedDate = ""

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val allHabits = db.habitDao().getAllHabitsRaw()
                allLogsList = db.habitDao().getAllLogsRaw()

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = sdf.format(Date())
                selectedDate = todayStr

                val widgetLogs = db.habitDao().getLogsForDateRaw(selectedDate)
                logsMap = widgetLogs.associateBy { it.habitId }

                val active = allHabits
                    .filter { !it.isArchived && com.example.data.isHabitActiveOnDate(it, selectedDate) }
                    .sortedWith(compareBy<Habit> { it.sortOrder }.thenByDescending { it.id })
                activeHabits = active
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        activeHabits = emptyList()
    }

    override fun getCount(): Int = activeHabits.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= activeHabits.size) return null

        try {
            val habit = activeHabits[position]
            val log = logsMap[habit.id]

            val sharedPrefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
            val habitPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
            val isDark = sharedPrefs.getBoolean("dark_mode_enabled", habitPrefs.getBoolean("dark_mode_enabled", true))
            val views = RemoteViews(context.packageName, if (isDark) R.layout.widget_habit_item else R.layout.widget_habit_item_light)

            var isWeeklyTargetReached = false
            var weeklyCount = 0
            var weeklyTarget = 0
            if (habit.frequency == "TIMES_WEEKLY") {
                weeklyTarget = habit.specificDays.toIntOrNull() ?: 3
                val curDate = try { java.time.LocalDate.parse(selectedDate) } catch (e: Exception) { java.time.LocalDate.now() }
                val startOf7Days = curDate.minusDays(6)
                val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
                val habitStartDate = try {
                    java.time.Instant.ofEpochMilli(validStartMillis.coerceAtLeast(946684800000L)).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } catch (e: Exception) { curDate }

                if (habit.isNegative) {
                    val habitLogsMap = allLogsList.filter { it.habitId == habit.id }.associateBy { it.date }
                    var cleanInWindow = 0
                    var pausedInWindow = 0
                    for (d in 0..6) {
                        val checkDate = startOf7Days.plusDays(d.toLong())
                        if (!checkDate.isBefore(habitStartDate) && !checkDate.isAfter(curDate)) {
                            val l = habitLogsMap[checkDate.toString()]
                            if (l?.isPaused == true) {
                                pausedInWindow++
                            } else if (l == null || com.example.data.isLogCompleted(habit, l)) {
                                cleanInWindow++
                            }
                        }
                    }
                    weeklyCount = cleanInWindow
                    val adjustedTarget = (weeklyTarget - pausedInWindow).coerceAtLeast(1)
                    isWeeklyTargetReached = weeklyCount >= adjustedTarget
                } else {
                    val startStr = startOf7Days.toString()
                    val endStr = curDate.toString()
                    val habitLogsInWindow = allLogsList.filter { l ->
                        l.habitId == habit.id && l.date >= startStr && l.date <= endStr
                    }
                    val pausedInWindow = habitLogsInWindow.count { it.isPaused }
                    weeklyCount = habitLogsInWindow.count { !it.isPaused && com.example.data.isLogCompleted(habit, it) }
                    val adjustedTarget = (weeklyTarget - pausedInWindow).coerceAtLeast(1)
                    isWeeklyTargetReached = weeklyCount >= adjustedTarget
                }
            }

            val status = com.example.data.getLogStatus(habit, log, selectedDate, "1970-01-01", selectedDate, isWeeklyTargetReached)

            val accentColorName = sharedPrefs.getString("accent_color_name", null)
                ?: habitPrefs.getString("accent_color_name", "PURPLE")
                ?: "PURPLE"

            val bgRes = when {
                status == "PAUSED" -> if (isDark) R.drawable.widget_item_paused_bg else R.drawable.widget_item_paused_bg_light
                status == "SUCCESS" -> if (isDark) R.drawable.widget_item_completed_bg else R.drawable.widget_item_completed_bg_light
                status == "FAILED" -> if (isDark) R.drawable.widget_item_failed_bg else R.drawable.widget_item_failed_bg_light
                else -> if (isDark) R.drawable.widget_item_normal_bg else R.drawable.widget_item_normal_bg_light
            }
            views.setInt(R.id.widget_habit_layout, "setBackgroundResource", bgRes)

            val habitColorInt = if (status == "PAUSED") {
                getColorInt("orange")
            } else {
                getColorInt(habit.color)
            }
            val iconResId = com.example.ui.HabitIconMapping.getIconDrawableId(habit.icon)
            views.setImageViewResource(R.id.widget_habit_icon, iconResId)
            views.setInt(R.id.widget_habit_icon, "setColorFilter", habitColorInt)
            views.setViewVisibility(R.id.widget_habit_icon, android.view.View.VISIBLE)

            val currentVal = when (log?.value) {
                null -> 0f
                -1f -> 0f
                -2f -> habit.targetValue
                else -> log.value
            }

            val isNumerical = (habit.type == "NUMBER" || habit.type == "NUMERICAL")
            val unitLower = habit.unit.lowercase().trim()
            val isMinutesUnit = unitLower in listOf("minuten", "minutes", "min", "minute", "m")

            val nameText = if (isNumerical) {
                val formattedCurrent = if (currentVal % 1f == 0f) currentVal.toInt().toString() else String.format(Locale.US, "%.1f", currentVal)
                val formattedTarget = if (habit.targetValue % 1f == 0f) habit.targetValue.toInt().toString() else String.format(Locale.US, "%.1f", habit.targetValue)
                "${habit.name} ($formattedCurrent/$formattedTarget)"
            } else {
                habit.name
            }
            views.setTextViewText(R.id.widget_habit_name, nameText)
            views.setTextColor(R.id.widget_habit_name, if (isDark) 0xFFE4E3EC.toInt() else 0xFF111115.toInt())

            val checkIcon = when {
                status == "SUCCESS" -> R.drawable.ic_widget_circle_checked
                (isNumerical && currentVal >= habit.targetValue) -> R.drawable.ic_widget_circle_checked
                status == "FAILED" -> R.drawable.ic_widget_failed_cross
                status == "PAUSED" -> R.drawable.ic_widget_circle_paused
                habit.frequency == "TIMES_WEEKLY" && isWeeklyTargetReached -> R.drawable.ic_widget_circle_weekly_done
                isNumerical -> R.drawable.ic_widget_circle_plus
                else -> R.drawable.ic_widget_circle_unchecked
            }
            views.setImageViewResource(R.id.widget_habit_check, checkIcon)
            views.setInt(R.id.widget_habit_check, "setColorFilter", 0)

            // Fill-In intents for widget actions
            val toggleIntent = Intent().apply {
                putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("WIDGET_ACTION", "TOGGLE")
            }

            val openAppIntent = Intent().apply {
                putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("WIDGET_ACTION", "OPEN_APP")
            }

            if (isNumerical) {
                val step = if (habit.clickIncrement > 0f) habit.clickIncrement else 1f
                val deltaIntent = Intent().apply {
                    putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra(HabitWidgetProvider.EXTRA_DELTA, step)
                    putExtra("WIDGET_ACTION", "DELTA")
                }

                views.setOnClickFillInIntent(R.id.widget_habit_check, deltaIntent)
            } else {
                views.setOnClickFillInIntent(R.id.widget_habit_check, toggleIntent)
            }

            views.setOnClickFillInIntent(R.id.widget_habit_icon, openAppIntent)
            views.setOnClickFillInIntent(R.id.widget_habit_name, openAppIntent)
            views.setOnClickFillInIntent(R.id.widget_habit_layout, openAppIntent)

            return views
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long {
        return if (position < activeHabits.size) activeHabits[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean = true

    private fun getColorInt(colorName: String): Int {
        if (colorName.startsWith("#")) {
            try { return android.graphics.Color.parseColor(colorName) } catch (e: Exception) {}
        }
        if (colorName.startsWith("0x") || colorName.startsWith("0X")) {
            try { return (colorName.substring(2).toLong(16) or 0xFF000000).toInt() } catch (e: Exception) {}
        }
        return when (colorName.lowercase()) {
            "blue" -> 0xFF3B82F6.toInt()
            "purple" -> 0xFF9333EA.toInt()
            "cyan" -> 0xFF06B6D4.toInt()
            "green" -> 0xFF10B981.toInt()
            "yellow" -> 0xFFF59E0B.toInt()
            "orange" -> 0xFFF97316.toInt()
            "red" -> 0xFFEF4444.toInt()
            "pink" -> 0xFFEC4899.toInt()
            "slate", "grey", "gray" -> 0xFF64748B.toInt()
            else -> {
                try {
                    android.graphics.Color.parseColor(if (colorName.startsWith("#")) colorName else "#$colorName")
                } catch (e: Exception) {
                    0xFF7356FF.toInt()
                }
            }
        }
    }
}
