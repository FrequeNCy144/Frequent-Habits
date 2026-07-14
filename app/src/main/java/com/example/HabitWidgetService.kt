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
    private var selectedDate = ""

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val allHabits = db.habitDao().getAllHabitsRaw()

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = sdf.format(Date())
                selectedDate = todayStr

                val widgetLogs = db.habitDao().getLogsForDateRaw(selectedDate)
                logsMap = widgetLogs.associateBy { it.habitId }

                val dateCal = Calendar.getInstance().apply {
                    time = sdf.parse(selectedDate) ?: Date()
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val dateMs = dateCal.timeInMillis

                val active = allHabits
                    .filter { !it.isArchived && com.example.data.isHabitActiveOnDate(it, selectedDate) }
                    .sortedWith(compareBy<Habit> { it.sortOrder }.thenByDescending { it.id })
                activeHabits = active

                // Also update the progress bar here to ensure it's in sync!
                var completed = 0
                var nonPausedCount = 0

                active.forEach { habit ->
                    val log = logsMap[habit.id]
                    val isPaused = log != null && log.isPaused
                    if (!isPaused) {
                        nonPausedCount++
                        if (com.example.data.isLogCompleted(habit, log)) {
                            completed++
                        }
                    }
                }

                val progressPercent = if (nonPausedCount > 0) (completed.toFloat() / nonPausedCount * 100).toInt() else 0
                val isCompleted = progressPercent >= 100 && nonPausedCount > 0

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.habit_widget)

                    views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
                    views.setProgressBar(R.id.widget_progress_bar_completed, 100, progressPercent, false)

                    if (isCompleted) {
                        views.setViewVisibility(R.id.widget_progress_bar_completed, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.widget_progress_bar_completed, android.view.View.GONE)
                    }

                    val progressTextVal = "${progressPercent}%"
                    views.setTextViewText(R.id.widget_progress_text, progressTextVal)

                    // Partially update the widget with just the progress views
                    appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
                }
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

            val views = RemoteViews(context.packageName, R.layout.widget_habit_item)

            val status = com.example.data.getLogStatus(habit, log, selectedDate, "1970-01-01", selectedDate)

            val bgRes = when (status) {
                "SUCCESS" -> R.drawable.widget_item_completed_bg
                "FAILED" -> R.drawable.widget_item_failed_bg
                "PAUSED" -> R.drawable.widget_item_paused_bg
                else -> R.drawable.widget_item_normal_bg
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

            val nameText = if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
                val formattedCurrent = if (currentVal % 1f == 0f) currentVal.toInt().toString() else String.format(Locale.US, "%.1f", currentVal)
                val formattedTarget = if (habit.targetValue % 1f == 0f) habit.targetValue.toInt().toString() else String.format(Locale.US, "%.1f", habit.targetValue)
                "${habit.name} ($formattedCurrent/$formattedTarget)"
            } else {
                habit.name
            }
            views.setTextViewText(R.id.widget_habit_name, nameText)

            val checkIcon = when (status) {
                "SUCCESS" -> R.drawable.ic_widget_circle_checked
                "FAILED" -> R.drawable.ic_widget_failed_cross
                "PAUSED" -> R.drawable.ic_widget_circle_paused
                else -> R.drawable.ic_widget_circle_unchecked
            }
            views.setImageViewResource(R.id.widget_habit_check, checkIcon)
            views.setInt(R.id.widget_habit_check, "setColorFilter", 0)

            // 1. Fill-In intent for check toggle
            val toggleIntent = Intent().apply {
                putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("WIDGET_ACTION", "TOGGLE")
            }

            if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
                views.setViewVisibility(R.id.widget_habit_minus, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_habit_plus, android.view.View.GONE)

                // When clicking layout or name, open the app
                val openAppIntent = Intent().apply {
                    putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("WIDGET_ACTION", "OPEN_APP")
                }
                views.setOnClickFillInIntent(R.id.widget_habit_layout, openAppIntent)
                views.setOnClickFillInIntent(R.id.widget_habit_name, openAppIntent)

                // Direct delta increment when clicking the check circle on numerical habits
                val directIncrementIntent = Intent().apply {
                    putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra(HabitWidgetProvider.EXTRA_DELTA, 1f)
                    putExtra("WIDGET_ACTION", "DELTA")
                }
                views.setOnClickFillInIntent(R.id.widget_habit_check, directIncrementIntent)
            } else {
                views.setViewVisibility(R.id.widget_habit_minus, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_habit_plus, android.view.View.GONE)
                
                // For binary habits, clicking the layout or check circle toggles the habit
                views.setOnClickFillInIntent(R.id.widget_habit_layout, toggleIntent)
                views.setOnClickFillInIntent(R.id.widget_habit_check, toggleIntent)
            }

            return views
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position < activeHabits.size) activeHabits[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean = true

    private fun getColorInt(colorName: String): Int {
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
            else -> 0xFF7356FF.toInt()
        }
    }
}
