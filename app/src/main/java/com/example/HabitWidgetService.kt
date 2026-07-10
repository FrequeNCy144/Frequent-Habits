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

                activeHabits = allHabits
                    .filter { !it.isArchived && com.example.data.isHabitActiveOnDate(it, selectedDate) }
                    .sortedWith(compareBy<Habit> { it.sortOrder }.thenByDescending { it.id })
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
            val provider = HabitWidgetProvider()
            val iconBitmap = provider.drawIconToBitmap(context, habit.icon, habitColorInt)
            views.setImageViewBitmap(R.id.widget_habit_icon, iconBitmap)
            views.setViewVisibility(R.id.widget_habit_icon, android.view.View.VISIBLE)

            val currentVal = when (log?.value) {
                null -> 0f
                -1f -> 0f
                -2f -> habit.targetValue
                else -> log.value
            }

            val nameText = if (habit.type == "BINARY") {
                habit.name
            } else {
                "${habit.name} (${currentVal.toInt()}/${habit.targetValue.toInt()} ${habit.unit})"
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
                views.setViewVisibility(R.id.widget_habit_minus, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_habit_plus, android.view.View.VISIBLE)

                // For numerical, clicks on check or name toggle the habit, separate from +/-
                views.setOnClickFillInIntent(R.id.widget_habit_check, toggleIntent)
                views.setOnClickFillInIntent(R.id.widget_habit_name, toggleIntent)

                val minusIntent = Intent().apply {
                    putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                    putExtra(HabitWidgetProvider.EXTRA_DELTA, -1f)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("WIDGET_ACTION", "DELTA")
                }
                views.setOnClickFillInIntent(R.id.widget_habit_minus, minusIntent)

                val plusIntent = Intent().apply {
                    putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                    putExtra(HabitWidgetProvider.EXTRA_DELTA, 1f)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("WIDGET_ACTION", "DELTA")
                }
                views.setOnClickFillInIntent(R.id.widget_habit_plus, plusIntent)
            } else {
                views.setViewVisibility(R.id.widget_habit_minus, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_habit_plus, android.view.View.GONE)
                
                // For binary habits, clicking the entire layout (the whole tile) toggles the habit
                views.setOnClickFillInIntent(R.id.widget_habit_layout, toggleIntent)
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
