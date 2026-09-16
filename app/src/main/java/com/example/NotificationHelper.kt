package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.Habit
import java.util.Calendar

object NotificationHelper {

    private fun setExactOrAllowWhileIdle(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("reminderHour", hour)
            putExtra("reminderMinute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Cancel previous alarm
        alarmManager.cancel(pendingIntent)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        setExactOrAllowWhileIdle(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleHabitReminder(context: Context, habitId: Int, habitName: String, hour: Int, minute: Int) {
        scheduleSingleReminder(context, habitId, habitId, habitName, hour, minute)
    }

    fun cancelHabitReminder(context: Context, habitId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAllHabitReminders(context: Context, habit: Habit) {
        // 1. Cancel default main reminder
        cancelHabitReminder(context, habit.id)
        // 2. Cancel custom reminders
        if (habit.customReminders.isNotEmpty()) {
            habit.customReminders.split(",").forEach { timeStr ->
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: return@forEach
                    val minute = parts[1].toIntOrNull() ?: return@forEach
                    val reqCode = (habit.id * 10000) + (hour * 100 + minute)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@forEach
                    val intent = Intent(context, HabitReminderReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }

    fun scheduleAllHabitReminders(context: Context, habit: Habit) {
        try {
            // First cancel all previous alarms for this habit
            cancelAllHabitReminders(context, habit)

            if (habit.isArchived) return

            // 1. Schedule main/default reminder if enabled
            if (habit.reminderEnabled) {
                scheduleSingleReminder(context, habit.id, habit.id, habit.name, habit.reminderHour, habit.reminderMinute)
            }
            // 2. Schedule custom reminders
            if (habit.customReminders.isNotEmpty()) {
                habit.customReminders.split(",").forEach { timeStr ->
                    val parts = timeStr.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: return@forEach
                        val minute = parts[1].toIntOrNull() ?: return@forEach
                        val reqCode = (habit.id * 10000) + (hour * 100 + minute)
                        scheduleSingleReminder(context, habit.id, reqCode, habit.name, hour, minute)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleSingleReminder(context: Context, habitId: Int, reqCode: Int, habitName: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habitId", habitId)
            putExtra("habitName", habitName)
            putExtra("reqCode", reqCode)
            putExtra("reminderHour", hour)
            putExtra("reminderMinute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        setExactOrAllowWhileIdle(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun scheduleReviewNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)

        // 1. Monthly Review Alarm (1st day of month at 00:00 AM)
        val monthlyIntent = Intent(context, ReviewNotificationReceiver::class.java).apply {
            putExtra("OPEN_REVIEW_TYPE", "MONTHLY")
        }
        val monthlyPendingIntent = PendingIntent.getBroadcast(
            context,
            88001,
            monthlyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(monthlyPendingIntent)

        if (prefs.getBoolean("monthly_review_enabled", true)) {
            val calMonthly = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.MONTH, 1)
                }
            }
            setExactOrAllowWhileIdle(alarmManager, calMonthly.timeInMillis, monthlyPendingIntent)
        }

        // 2. Yearly Review Alarm (Jan 1st at 00:00 AM)
        val yearlyIntent = Intent(context, ReviewNotificationReceiver::class.java).apply {
            putExtra("OPEN_REVIEW_TYPE", "YEARLY")
        }
        val yearlyPendingIntent = PendingIntent.getBroadcast(
            context,
            88002,
            yearlyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(yearlyPendingIntent)

        if (prefs.getBoolean("yearly_review_enabled", true)) {
            val calYearly = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.YEAR, 1)
                }
            }
            setExactOrAllowWhileIdle(alarmManager, calYearly.timeInMillis, yearlyPendingIntent)
        }
    }

    fun cancelReviewNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val monthlyIntent = Intent(context, ReviewNotificationReceiver::class.java)
        val monthlyPendingIntent = PendingIntent.getBroadcast(
            context,
            88001,
            monthlyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(monthlyPendingIntent)

        val yearlyIntent = Intent(context, ReviewNotificationReceiver::class.java)
        val yearlyPendingIntent = PendingIntent.getBroadcast(
            context,
            88002,
            yearlyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(yearlyPendingIntent)
    }

    fun scheduleSmartInsightNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val intent = Intent(context, SmartInsightNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            88003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        if (prefs.getBoolean("insight_notifications_enabled", true)) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            setExactOrAllowWhileIdle(alarmManager, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelSmartInsightNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SmartInsightNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            88003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
