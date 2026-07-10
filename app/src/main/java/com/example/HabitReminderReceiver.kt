package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.isHabitActiveOnDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra("habitId", -1)
        val habitName = intent.getStringExtra("habitName") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                if (habitId != -1) {
                    // Individual Reminder Flow
                    val habit = db.habitDao().getHabitByIdSuspend(habitId)
                    if (habit != null && isHabitActiveOnDate(habit, todayStr)) {
                        val logs = db.habitDao().getLogsForHabitOnDate(habitId, todayStr)
                        val log = logs.firstOrNull()
                        val isCompleted = if (log != null) {
                            when (log.value) {
                                -1f -> false // Explicitly failed
                                -2f -> true  // Explicitly succeeded
                                else -> {
                                    if (habit.type == "BINARY") {
                                        if (habit.isNegative) false else true
                                    } else {
                                        if (habit.isNegative) log.value < habit.targetValue else log.value >= habit.targetValue
                                    }
                                }
                            }
                        } else {
                            habit.isNegative // negative default is completed (success)
                        }

                        if (!isCompleted) {
                            showIndividualNotification(context, habitId, habit.name)
                        }
                    }
                } else {
                    // Fallback to General Reminder Flow
                    val allHabits = db.habitDao().getAllHabitsRaw()
                    val todayLogs = db.habitDao().getLogsForDateRaw(todayStr)
                    val logsMap = todayLogs.associateBy { it.habitId }

                    val pendingHabits = allHabits.filter { habit ->
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
                        !isCompleted && isHabitActiveOnDate(habit, todayStr)
                    }

                    if (pendingHabits.isNotEmpty()) {
                        showNotification(context, pendingHabits.size)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showIndividualNotification(context: Context, habitId: Int, habitName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gewohnheiten Erinnerung",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Erinnert an noch nicht erledigte Gewohnheiten"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Zeit für deine Gewohnheit! 🚀"
        val text = "Hast du '$habitName' heute schon erledigt? Bleib dran!"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(habitId, notification)
    }

    private fun showNotification(context: Context, pendingCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gewohnheiten Erinnerung",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Erinnert an noch nicht erledigte Gewohnheiten"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            99,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Vergiss deine Habits nicht! 🚀"
        val text = "Du hast heute noch $pendingCount Gewohnheit(en) offen. Bleib dran!"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(101, notification)
    }
}
