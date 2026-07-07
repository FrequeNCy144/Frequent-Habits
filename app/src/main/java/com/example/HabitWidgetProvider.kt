package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.widget.RemoteViews
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_UPDATE_HABITS) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            updateAllWidgets(context, appWidgetManager, ids)
        } else if (action == ACTION_PREV_DAY || action == ACTION_NEXT_DAY) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            val targetWidgetId = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) widgetId else ids.firstOrNull() ?: return
            
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val dateKey = "widget_date_$targetWidgetId"
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())
            val currentSelectedDate = prefs.getString(dateKey, todayStr) ?: todayStr
            
            val cal = Calendar.getInstance().apply {
                time = sdf.parse(currentSelectedDate) ?: Date()
            }
            
            if (action == ACTION_PREV_DAY) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val allHabits = db.habitDao().getAllHabitsRaw()
                        val oldestMs = allHabits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
                        val oldestDateStr = sdf.format(Date(oldestMs))
                        
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        val newDateStr = sdf.format(cal.time)
                        if (newDateStr >= oldestDateStr) {
                            prefs.edit().putString(dateKey, newDateStr).apply()
                        }
                        triggerUpdate(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val newDateStr = sdf.format(cal.time)
                if (newDateStr <= todayStr) {
                    prefs.edit().putString(dateKey, newDateStr).apply()
                }
                triggerUpdate(context)
            }
        } else if (action == ACTION_TOGGLE_BINARY_HABIT) {
            val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
            val targetWidgetId = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) widgetId else -1
            if (habitId != -1) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val habit = db.habitDao().getHabitByIdSuspend(habitId)
                        if (habit != null) {
                            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val todayStr = sdf.format(Date())
                            val dateKey = "widget_date_$targetWidgetId"
                            val selectedDate = if (targetWidgetId != -1) {
                                prefs.getString(dateKey, todayStr) ?: todayStr
                            } else {
                                todayStr
                            }
                            
                            val logs = db.habitDao().getLogsForHabitOnDate(habitId, selectedDate)
                            val currentLog = logs.firstOrNull()
                            
                            val currentStatus = when {
                                currentLog == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
                                currentLog.value == -1f -> "FAILED"
                                currentLog.value == -2f -> "SUCCESS"
                                else -> {
                                    if (habit.type == "BINARY") {
                                        if (habit.isNegative) "FAILED" else "SUCCESS"
                                    } else {
                                        if (habit.isNegative) {
                                            if (currentLog.value >= habit.targetValue) "FAILED" else "PENDING"
                                        } else {
                                            if (currentLog.value >= habit.targetValue) "SUCCESS" else "PENDING"
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
                                db.habitDao().deleteLogsForHabitOnDate(habitId, selectedDate)
                            } else {
                                val nextValue = if (nextStatus == "SUCCESS") {
                                    if (habit.type == "BINARY") -2f else habit.targetValue
                                } else {
                                    -1f
                                }
                                val newLog = com.example.data.HabitLog(
                                    id = currentLog?.id ?: 0,
                                    habitId = habitId,
                                    date = selectedDate,
                                    value = nextValue
                                )
                                db.habitDao().insertLog(newLog)
                            }
                        }
                        triggerUpdate(context)
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
            val targetWidgetId = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) widgetId else -1
            if (habitId != -1 && delta != 0f) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val habit = db.habitDao().getHabitByIdSuspend(habitId)
                        if (habit != null) {
                            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val todayStr = sdf.format(Date())
                            val dateKey = "widget_date_$targetWidgetId"
                            val selectedDate = if (targetWidgetId != -1) {
                                prefs.getString(dateKey, todayStr) ?: todayStr
                            } else {
                                todayStr
                            }
                            
                            val logs = db.habitDao().getLogsForHabitOnDate(habitId, selectedDate)
                            val currentLog = logs.firstOrNull()
                            val currentValue = when (currentLog?.value) {
                                null -> 0f
                                -1f -> 0f
                                -2f -> habit.targetValue
                                else -> currentLog.value
                            }
                            val newValue = (currentValue + delta).coerceAtLeast(0f)
                            
                            if (newValue <= 0f) {
                                db.habitDao().deleteLogsForHabitOnDate(habitId, selectedDate)
                            } else {
                                val newLog = com.example.data.HabitLog(
                                    id = currentLog?.id ?: 0,
                                    habitId = habitId,
                                    date = selectedDate,
                                    value = newValue
                                )
                                db.habitDao().insertLog(newLog)
                            }
                        }
                        triggerUpdate(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allHabits = db.habitDao().getAllHabitsRaw()

                appWidgetIds.forEach { widgetId ->
                    val dateKey = "widget_date_$widgetId"
                    val selectedDate = prefs.getString(dateKey, todayStr) ?: todayStr
                    val widgetLogs = db.habitDao().getLogsForDateRaw(selectedDate)
                    val logsMap = widgetLogs.associateBy { it.habitId }

                    val dateCal = Calendar.getInstance().apply {
                        time = sdf.parse(selectedDate) ?: Date()
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val dateMs = dateCal.timeInMillis

                    val activeHabits = allHabits
                        .filter { it.startDate <= dateMs }
                        .sortedBy { it.id }

                    var completed = 0
                    val total = activeHabits.size

                    activeHabits.forEach { habit ->
                        val log = logsMap[habit.id]
                        val isCompleted = if (log != null) {
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

                        if (isCompleted) {
                            completed++
                        }
                    }

                    val progressPercent = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0

                    val views = RemoteViews(context.packageName, R.layout.habit_widget)
                    views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
                    
                    val displayDate = getDisplayDate(selectedDate)
                    views.setTextViewText(R.id.widget_date_title, displayDate)

                    val prevIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                        action = ACTION_PREV_DAY
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    val prevPending = PendingIntent.getBroadcast(
                        context,
                        widgetId * 100 + 1,
                        prevIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_prev_day, prevPending)

                    val nextIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                        action = ACTION_NEXT_DAY
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    val nextPending = PendingIntent.getBroadcast(
                        context,
                        widgetId * 100 + 2,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_next_day, nextPending)

                    val maxSlots = 3
                    val slotsCount = activeHabits.size.coerceAtMost(maxSlots)
                    
                    val slotLayouts = intArrayOf(
                        R.id.widget_habit_1_layout,
                        R.id.widget_habit_2_layout,
                        R.id.widget_habit_3_layout
                    )
                    val slotIcons = intArrayOf(
                        R.id.widget_habit_1_icon,
                        R.id.widget_habit_2_icon,
                        R.id.widget_habit_3_icon
                    )
                    val slotNames = intArrayOf(
                        R.id.widget_habit_1_name,
                        R.id.widget_habit_2_name,
                        R.id.widget_habit_3_name
                    )
                    val slotChecks = intArrayOf(
                        R.id.widget_habit_1_check,
                        R.id.widget_habit_2_check,
                        R.id.widget_habit_3_check
                    )
                    val slotMinuses = intArrayOf(
                        R.id.widget_habit_1_minus,
                        R.id.widget_habit_2_minus,
                        R.id.widget_habit_3_minus
                    )
                    val slotPluses = intArrayOf(
                        R.id.widget_habit_1_plus,
                        R.id.widget_habit_2_plus,
                        R.id.widget_habit_3_plus
                    )

                    for (i in 0 until maxSlots) {
                        if (i < slotsCount) {
                            val habit = activeHabits[i]
                            val log = logsMap[habit.id]
                            
                            val status = when {
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

                            views.setViewVisibility(slotLayouts[i], android.view.View.VISIBLE)
                            
                            val bgRes = when (status) {
                                "SUCCESS" -> R.drawable.widget_item_completed_bg
                                "FAILED" -> R.drawable.widget_item_failed_bg
                                else -> R.drawable.widget_item_normal_bg
                            }
                            views.setInt(slotLayouts[i], "setBackgroundResource", bgRes)
                            
                            val habitColorInt = getColorInt(habit.color)
                            val iconBitmap = drawIconToBitmap(context, habit.icon, habitColorInt)
                            views.setImageViewBitmap(slotIcons[i], iconBitmap)
                            views.setViewVisibility(slotIcons[i], android.view.View.VISIBLE)
                            
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
                            views.setTextViewText(slotNames[i], nameText)
                            
                            val checkIcon = when (status) {
                                "SUCCESS" -> R.drawable.ic_widget_circle_checked
                                "FAILED" -> R.drawable.ic_widget_failed_cross
                                else -> R.drawable.ic_widget_circle_unchecked
                            }
                            views.setImageViewResource(slotChecks[i], checkIcon)
                            views.setInt(slotChecks[i], "setColorFilter", habitColorInt)
                            
                            val toggleIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                action = ACTION_TOGGLE_BINARY_HABIT
                                putExtra(EXTRA_HABIT_ID, habit.id)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            }
                            val pIntent = PendingIntent.getBroadcast(
                                context,
                                widgetId * 1000 + habit.id,
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(slotChecks[i], pIntent)
                            
                            if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
                                views.setViewVisibility(slotMinuses[i], android.view.View.VISIBLE)
                                views.setViewVisibility(slotPluses[i], android.view.View.VISIBLE)
                                
                                val minusIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                    action = ACTION_WIDGET_ADD_VALUE_DIRECT
                                    putExtra(EXTRA_HABIT_ID, habit.id)
                                    putExtra(EXTRA_DELTA, -1f)
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                }
                                val minusPIntent = PendingIntent.getBroadcast(
                                    context,
                                    widgetId * 2000 + habit.id,
                                    minusIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(slotMinuses[i], minusPIntent)

                                val plusIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                    action = ACTION_WIDGET_ADD_VALUE_DIRECT
                                    putExtra(EXTRA_HABIT_ID, habit.id)
                                    putExtra(EXTRA_DELTA, 1f)
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                }
                                val plusPIntent = PendingIntent.getBroadcast(
                                    context,
                                    widgetId * 3000 + habit.id,
                                    plusIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(slotPluses[i], plusPIntent)
                            } else {
                                views.setViewVisibility(slotMinuses[i], android.view.View.GONE)
                                views.setViewVisibility(slotPluses[i], android.view.View.GONE)
                            }

                            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                                if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
                                    action = ACTION_WIDGET_ADD_VALUE
                                    putExtra(EXTRA_HABIT_ID, habit.id)
                                }
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            val openAppPendingIntent = PendingIntent.getActivity(
                                context,
                                widgetId * 4000 + habit.id,
                                openAppIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(slotNames[i], openAppPendingIntent)
                            views.setOnClickPendingIntent(slotLayouts[i], openAppPendingIntent)
                        } else {
                            views.setViewVisibility(slotLayouts[i], android.view.View.GONE)
                        }
                    }
                    
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
                    
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getDisplayDate(dateStr: String): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return if (dateStr == todayStr) {
            "Heute"
        } else {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                if (date != null) {
                    SimpleDateFormat("d.M", Locale.GERMAN).format(date)
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        }
    }

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
            else -> 0xFF7356FF.toInt() // Default to PrimaryViolet
        }
    }

    private fun drawIconToBitmap(context: Context, iconName: String, colorInt: Int): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = colorInt
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 3f
        }
        
        val half = size / 2f
        
        when (iconName.lowercase()) {
            "sparkle" -> {
                val path = Path().apply {
                    moveTo(half, 4f)
                    quadTo(half, half, size - 4f, half)
                    quadTo(half, half, half, size - 4f)
                    quadTo(half, half, 4f, half)
                    quadTo(half, half, half, 4f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "moon" -> {
                val path = Path().apply {
                    addCircle(half + 4f, half, half - 6f, Path.Direction.CW)
                    val subtraction = Path().apply {
                        addCircle(half, half, half - 6f, Path.Direction.CW)
                    }
                    op(subtraction, Path.Op.DIFFERENCE)
                }
                canvas.drawPath(path, paint)
            }
            "sun" -> {
                canvas.drawCircle(half, half, size / 5f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4
                    val startX = half + (size / 3.5f) * Math.cos(angle).toFloat()
                    val startY = half + (size / 3.5f) * Math.sin(angle).toFloat()
                    val endX = half + (size / 2.2f) * Math.cos(angle).toFloat()
                    val endY = half + (size / 2.2f) * Math.sin(angle).toFloat()
                    canvas.drawLine(startX, startY, endX, endY, paint)
                }
            }
            "water" -> {
                val path = Path().apply {
                    moveTo(half, 6f)
                    cubicTo(size - 8f, half + 4f, size - 8f, size - 6f, half, size - 6f)
                    cubicTo(8f, size - 6f, 8f, half + 4f, half, 6f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "heart" -> {
                val heartPath = Path().apply {
                    moveTo(half, size * 0.3f)
                    cubicTo(size * 0.2f, size * 0.05f, size * 0.02f, size * 0.25f, size * 0.05f, size * 0.5f)
                    cubicTo(size * 0.08f, size * 0.75f, size * 0.35f, size * 0.9f, half, size * 0.95f)
                    cubicTo(size * 0.65f, size * 0.9f, size * 0.92f, size * 0.75f, size * 0.95f, size * 0.5f)
                    cubicTo(size * 0.98f, size * 0.25f, size * 0.8f, size * 0.05f, half, size * 0.3f)
                    close()
                }
                canvas.drawPath(heartPath, paint)
            }
            "dumbbell" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                canvas.drawLine(8f, half, size - 8f, half, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(10f, half, 8f, paint)
                canvas.drawCircle(size - 10f, half, 8f, paint)
            }
            "book" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val path = Path().apply {
                    moveTo(half, size - 10f)
                    lineTo(half, 10f)
                }
                canvas.drawPath(path, paint)
                val leftPage = Path().apply {
                    moveTo(half, 12f)
                    cubicTo(half - 8f, 8f, 8f, 8f, 8f, 12f)
                    lineTo(8f, size - 12f)
                    cubicTo(8f, size - 16f, half - 8f, size - 16f, half, size - 12f)
                }
                canvas.drawPath(leftPage, paint)
                val rightPage = Path().apply {
                    moveTo(half, 12f)
                    cubicTo(half + 8f, 8f, size - 8f, 8f, size - 8f, 12f)
                    lineTo(size - 8f, size - 12f)
                    cubicTo(size - 8f, size - 16f, half + 8f, size - 16f, half, size - 12f)
                }
                canvas.drawPath(rightPage, paint)
            }
            "coffee" -> {
                val cup = Path().apply {
                    moveTo(10f, 16f)
                    lineTo(size - 14f, 16f)
                    cubicTo(size - 14f, size - 10f, 14f, size - 10f, 10f, 16f)
                    close()
                }
                canvas.drawPath(cup, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                val handle = Path().apply {
                    moveTo(size - 14f, 20f)
                    cubicTo(size - 6f, 20f, size - 6f, size - 16f, size - 14f, size - 16f)
                }
                canvas.drawPath(handle, paint)
                val steam1 = Path().apply {
                    moveTo(16f, 12f)
                    quadTo(18f, 9f, 16f, 6f)
                }
                val steam2 = Path().apply {
                    moveTo(half - 2f, 12f)
                    quadTo(half, 9f, half - 2f, 6f)
                }
                canvas.drawPath(steam1, paint)
                canvas.drawPath(steam2, paint)
            }
            "run" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.style = Paint.Style.FILL
                canvas.drawCircle(half + 4f, 10f, 4f, paint)
                paint.style = Paint.Style.STROKE
                val torso = Path().apply {
                    moveTo(half + 2f, 14f)
                    lineTo(half - 2f, size * 0.6f)
                    moveTo(half - 2f, size * 0.6f)
                    lineTo(half - 8f, size * 0.75f)
                    lineTo(half - 4f, size * 0.9f)
                    moveTo(half - 2f, size * 0.6f)
                    lineTo(half + 6f, size * 0.75f)
                    lineTo(half + 2f, size * 0.9f)
                    moveTo(half + 2f, 16f)
                    lineTo(half + 8f, 20f)
                    lineTo(half + 12f, 16f)
                    moveTo(half + 2f, 16f)
                    lineTo(half - 6f, 20f)
                    lineTo(half - 10f, 24f)
                }
                canvas.drawPath(torso, paint)
            }
            "code" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val left = Path().apply {
                    moveTo(12f, 10f)
                    lineTo(6f, half)
                    lineTo(12f, size - 10f)
                }
                val right = Path().apply {
                    moveTo(size - 12f, 10f)
                    lineTo(size - 6f, half)
                    lineTo(size - 12f, size - 10f)
                }
                val slash = Path().apply {
                    moveTo(size - 10f, 6f)
                    lineTo(10f, size - 6f)
                }
                canvas.drawPath(left, paint)
                canvas.drawPath(right, paint)
                canvas.drawPath(slash, paint)
            }
            "music" -> {
                canvas.drawCircle(12f, size - 12f, 6f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val stem = Path().apply {
                    moveTo(16f, size - 12f)
                    lineTo(16f, 8f)
                    lineTo(size - 10f, 12f)
                }
                canvas.drawPath(stem, paint)
            }
            "phone" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                val phone = Path().apply {
                    moveTo(10f, 10f)
                    cubicTo(6f, 14f, 14f, size - 6f, size - 10f, size - 10f)
                }
                canvas.drawPath(phone, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(10f, 10f, 4f, paint)
                canvas.drawCircle(size - 10f, size - 10f, 4f, paint)
            }
            "meditation" -> {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(half, 12f, 5f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val body = Path().apply {
                    moveTo(half, 17f)
                    lineTo(half, size * 0.65f)
                    moveTo(half, size * 0.65f)
                    cubicTo(8f, size * 0.65f, 10f, size * 0.9f, half, size * 0.85f)
                    moveTo(half, size * 0.65f)
                    cubicTo(size - 8f, size * 0.65f, size - 10f, size * 0.9f, half, size * 0.85f)
                    moveTo(half, 22f)
                    cubicTo(8f, 22f, 8f, size * 0.6f, 12f, size * 0.65f)
                    moveTo(half, 22f)
                    cubicTo(size - 8f, 22f, size - 8f, size * 0.6f, size - 12f, size * 0.65f)
                }
                canvas.drawPath(body, paint)
            }
            "clock" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(half, half, size / 2f - 6f, paint)
                canvas.drawLine(half, half, half, 12f, paint)
                canvas.drawLine(half, half, half + 8f, half, paint)
            }
            "food" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawCircle(half, half, 12f, paint)
                canvas.drawLine(10f, 16f, 10f, size - 16f, paint)
                canvas.drawLine(size - 10f, 16f, size - 10f, size - 16f, paint)
            }
            "money" -> {
                paint.style = Paint.Style.FILL
                paint.textSize = 34f
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                canvas.drawText("$", half, half + 12f, paint)
            }
            "work" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                val rect = Path().apply {
                    addRoundRect(10f, 16f, size - 10f, size - 10f, 4f, 4f, Path.Direction.CW)
                }
                canvas.drawPath(rect, paint)
                val handle = Path().apply {
                    moveTo(half - 6f, 16f)
                    lineTo(half - 6f, 10f)
                    lineTo(half + 6f, 10f)
                    lineTo(half + 6f, 16f)
                }
                canvas.drawPath(handle, paint)
            }
            "clean" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawLine(10f, size - 10f, size - 16f, 16f, paint)
                canvas.drawLine(10f, size - 10f, 6f, size - 6f, paint)
                canvas.drawLine(10f, size - 10f, 14f, size - 6f, paint)
                canvas.drawLine(10f, size - 10f, 6f, size - 14f, paint)
            }
            else -> {
                val path = Path().apply {
                    moveTo(half, 4f)
                    quadTo(half, half, size - 4f, half)
                    quadTo(half, half, half, size - 4f)
                    quadTo(half, half, 4f, half)
                    quadTo(half, half, half, 4f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
        
        return bitmap
    }

    companion object {
        const val ACTION_UPDATE_HABITS = "com.example.widget.ACTION_UPDATE_HABITS"
        const val ACTION_TOGGLE_BINARY_HABIT = "com.example.widget.ACTION_TOGGLE_BINARY_HABIT"
        const val ACTION_WIDGET_ADD_VALUE = "com.example.widget.ACTION_WIDGET_ADD_VALUE"
        const val ACTION_PREV_DAY = "com.example.widget.ACTION_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.example.widget.ACTION_NEXT_DAY"
        const val ACTION_WIDGET_ADD_VALUE_DIRECT = "com.example.widget.ACTION_WIDGET_ADD_VALUE_DIRECT"
        const val EXTRA_HABIT_ID = "com.example.widget.EXTRA_HABIT_ID"
        const val EXTRA_DELTA = "com.example.widget.EXTRA_DELTA"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_HABITS
            }
            context.sendBroadcast(intent)
        }
    }
}
