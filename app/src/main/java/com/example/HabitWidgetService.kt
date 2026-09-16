package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.data.AppDatabase
import com.example.data.FolderStorageHelper
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.data.HabitLog
import com.frequent.habits.R
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking

sealed class WidgetListItem {
    data class FolderHeader(
        val folder: HabitFolder,
        val totalCount: Int,
        val completedCount: Int,
        val isAllCompleted: Boolean,
        val isAllFailed: Boolean = false
    ) : WidgetListItem()

    data class HabitItem(
        val habit: Habit,
        val isNested: Boolean,
        val folderColor: String? = null
    ) : WidgetListItem()
}

class HabitWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetFactory(applicationContext, intent)
    }
}

class HabitWidgetFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private var widgetItems = listOf<WidgetListItem>()
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

                val allFolders = FolderStorageHelper.loadFolders(context)
                val folderMap = allFolders.associateBy { it.id }

                val folderElements = allFolders.map { Pair(true, it.sortOrder to it.id) }
                val looseHabits = active.filter { it.folderId == null || !folderMap.containsKey(it.folderId) }
                val looseElements = looseHabits.map { Pair(false, it.sortOrder to it.id) }

                val sortedElements = (folderElements + looseElements).sortedWith(
                    compareBy<Pair<Boolean, Pair<Int, Int>>> { it.second.first }.thenByDescending { it.second.second }
                )

                val items = mutableListOf<WidgetListItem>()
                val looseHabitMap = looseHabits.associateBy { it.id }

                sortedElements.forEach { element ->
                    val isFolder = element.first
                    val id = element.second.second
                    if (isFolder) {
                        val folder = folderMap[id] ?: return@forEach
                        val habitsInFolder = active
                            .filter { it.folderId == folder.id }
                            .sortedWith(compareBy<Habit> { it.sortOrder }.thenByDescending { it.id })

                        if (habitsInFolder.isNotEmpty()) {
                            val completedCount = habitsInFolder.count { h ->
                                val log = logsMap[h.id]
                                if (h.isNegative) {
                                    log == null || com.example.data.isLogCompleted(h, log)
                                } else {
                                    log != null && com.example.data.isLogCompleted(h, log)
                                }
                            }
                            val isAllCompleted = completedCount == habitsInFolder.size && habitsInFolder.isNotEmpty()
                            val isAllFailed = habitsInFolder.isNotEmpty() && habitsInFolder.all { h ->
                                val log = logsMap[h.id]
                                log != null && log.value == -1f
                            }
                            items.add(
                                WidgetListItem.FolderHeader(
                                    folder = folder,
                                    totalCount = habitsInFolder.size,
                                    completedCount = completedCount,
                                    isAllCompleted = isAllCompleted,
                                    isAllFailed = isAllFailed
                                )
                            )

                            if (!folder.isCollapsed) {
                                habitsInFolder.forEach { habit ->
                                    items.add(WidgetListItem.HabitItem(habit = habit, isNested = true, folderColor = folder.color))
                                }
                            }
                        }
                    } else {
                        val habit = looseHabitMap[id]
                        if (habit != null) {
                            items.add(WidgetListItem.HabitItem(habit = habit, isNested = false))
                        }
                    }
                }

                widgetItems = items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        widgetItems = emptyList()
    }

    override fun getCount(): Int = widgetItems.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= widgetItems.size) return null

        try {
            val sharedPrefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
            val habitPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
            val isDark = sharedPrefs.getBoolean("dark_mode_enabled", habitPrefs.getBoolean("dark_mode_enabled", true))

            return when (val item = widgetItems[position]) {
                is WidgetListItem.FolderHeader -> renderFolderHeader(item, isDark)
                is WidgetListItem.HabitItem -> renderHabitItem(item.habit, item.isNested, item.folderColor, isDark)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun renderFolderHeader(item: WidgetListItem.FolderHeader, isDark: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, if (isDark) R.layout.widget_folder_item else R.layout.widget_folder_item_light)
        val folder = item.folder

        val folderColorInt = getColorInt(folder.color)
        val iconResId = com.example.ui.HabitIconMapping.getIconDrawableId(folder.icon)
        views.setImageViewResource(R.id.widget_folder_icon, iconResId)
        views.setInt(R.id.widget_folder_icon, "setColorFilter", folderColorInt)

        views.setTextViewText(R.id.widget_folder_name, folder.name)
        views.setTextColor(R.id.widget_folder_name, if (isDark) 0xFFE4E3EC.toInt() else 0xFF111115.toInt())

        val chevronResId = if (folder.isCollapsed) R.drawable.ic_arrow_drop_down else R.drawable.ic_arrow_drop_up
        views.setImageViewResource(R.id.widget_folder_chevron, chevronResId)
        views.setInt(R.id.widget_folder_chevron, "setColorFilter", if (isDark) 0xFF9E9E9E.toInt() else 0xFF757575.toInt())

        val checkResId = when {
            item.isAllCompleted && item.totalCount > 0 -> R.drawable.ic_widget_circle_checked
            item.isAllFailed -> R.drawable.ic_widget_failed_cross
            else -> R.drawable.ic_widget_circle_unchecked
        }
        views.setImageViewResource(R.id.widget_folder_check, checkResId)
        views.setInt(R.id.widget_folder_check, "setColorFilter", 0)

        val collapseIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(HabitWidgetProvider.EXTRA_FOLDER_ID, folder.id)
            putExtra("WIDGET_ACTION", "TOGGLE_FOLDER_COLLAPSE")
        }
        views.setOnClickFillInIntent(R.id.widget_folder_chevron, collapseIntent)
        views.setOnClickFillInIntent(R.id.widget_folder_name, collapseIntent)
        views.setOnClickFillInIntent(R.id.widget_folder_icon, collapseIntent)
        views.setOnClickFillInIntent(R.id.widget_folder_layout, collapseIntent)

        val completionIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(HabitWidgetProvider.EXTRA_FOLDER_ID, folder.id)
            putExtra("WIDGET_ACTION", "TOGGLE_FOLDER_COMPLETION")
        }
        views.setOnClickFillInIntent(R.id.widget_folder_check, completionIntent)

        return views
    }

    private fun renderHabitItem(habit: Habit, isNested: Boolean, folderColor: String?, isDark: Boolean): RemoteViews {
        val log = logsMap[habit.id]
        val views = RemoteViews(context.packageName, if (isDark) R.layout.widget_habit_item else R.layout.widget_habit_item_light)

        if (isNested) {
            views.setViewVisibility(R.id.widget_folder_indicator, android.view.View.VISIBLE)
            val fColorInt = getColorInt(folderColor ?: "purple")
            views.setInt(R.id.widget_folder_indicator, "setBackgroundColor", fColorInt)
            val padLeft = dpToPx(16)
            val padTop = dpToPx(8)
            val padRight = dpToPx(10)
            val padBottom = dpToPx(8)
            views.setViewPadding(R.id.widget_habit_layout, padLeft, padTop, padRight, padBottom)
        } else {
            views.setViewVisibility(R.id.widget_folder_indicator, android.view.View.GONE)
            val padLeft = dpToPx(10)
            val padTop = dpToPx(8)
            val padRight = dpToPx(10)
            val padBottom = dpToPx(8)
            views.setViewPadding(R.id.widget_habit_layout, padLeft, padTop, padRight, padBottom)
        }

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
        views.setOnClickFillInIntent(R.id.widget_folder_indicator, openAppIntent)

        return views
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= widgetItems.size) return position.toLong()
        return when (val item = widgetItems[position]) {
            is WidgetListItem.FolderHeader -> -(item.folder.id.toLong() + 100000L)
            is WidgetListItem.HabitItem -> item.habit.id.toLong()
        }
    }

    override fun hasStableIds(): Boolean = true

    private fun getColorInt(colorName: String): Int {
        return com.example.widget.WidgetBitmapUtils.getColorInt(colorName)
    }
}

