package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.frequent.habits.R
import com.example.data.AppDatabase
import com.example.data.isHabitActiveOnDate
import com.example.data.isLogCompleted
import com.example.data.Habit
import com.example.data.HabitLog
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
        val reminderHour = intent.getIntExtra("reminderHour", -1)
        val reminderMinute = intent.getIntExtra("reminderMinute", -1)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                if (habitId != -1) {
                    // Individual Reminder Flow
                    val habit = db.habitDao().getHabitByIdSuspend(habitId)
                    if (habit != null && !habit.isArchived) {
                        if (isHabitActiveOnDate(habit, todayStr)) {
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
                                val allLogs = db.habitDao().getLogsForHabitRaw(habitId)
                                val missedDays = if (habit.frequency == "TIMES_WEEKLY") {
                                    hasMissedRollingWeekly(habit, allLogs)
                                } else {
                                    hasMissedLast3ActiveDays(habit, allLogs)
                                }
                                showIndividualNotification(context, habitId, habit.name, habit.why, missedDays)
                            }
                        }
                        // Reschedule alarm for next occurrence
                        NotificationHelper.scheduleAllHabitReminders(context, habit)
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
                        !isCompleted && !habit.isArchived && isHabitActiveOnDate(habit, todayStr)
                    }

                    if (pendingHabits.isNotEmpty()) {
                        showNotification(context, pendingHabits.size)
                    }

                    // Reschedule general reminder for tomorrow
                    val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
                    val isGeneralEnabled = prefs.getBoolean("reminder_enabled", false)
                    if (isGeneralEnabled) {
                        val h = if (reminderHour != -1) reminderHour else prefs.getInt("reminder_hour", 20)
                        val m = if (reminderMinute != -1) reminderMinute else prefs.getInt("reminder_minute", 0)
                        NotificationHelper.scheduleReminder(context, h, m)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun hasMissedLast3ActiveDays(habit: Habit, logs: List<HabitLog>): Boolean {
        val completedDates = logs.filter { isLogCompleted(habit, it) }.map { it.date }.toSet()
        val pausedDates = logs.filter { it.isPaused }.map { it.date }.toSet()
        
        var missedCount = 0
        var dayOffset = 0
        var activeDaysFound = 0
        
        while (activeDaysFound < 3 && dayOffset < 365) {
            val date = java.time.LocalDate.now().minusDays(dayOffset.toLong())
            val dateStr = date.toString()
            
            if (isHabitActiveOnDate(habit, dateStr) && !pausedDates.contains(dateStr)) {
                activeDaysFound++
                if (!completedDates.contains(dateStr)) {
                    missedCount++
                }
            }
            dayOffset++
        }
        
        return activeDaysFound >= 3 && missedCount == 3
    }

    private fun hasMissedRollingWeekly(habit: Habit, logs: List<HabitLog>): Boolean {
        val completedDates = logs.filter { isLogCompleted(habit, it) }.map { it.date }.toSet()
        
        val missedLast3Days = (0..2).all { offset ->
            val dateStr = java.time.LocalDate.now().minusDays(offset.toLong()).toString()
            !completedDates.contains(dateStr)
        }
        
        if (!missedLast3Days) return false
        
        val completedIn7Days = (0..6).count { offset ->
            val dateStr = java.time.LocalDate.now().minusDays(offset.toLong()).toString()
            completedDates.contains(dateStr)
        }
        val targetTimes = habit.specificDays.toIntOrNull() ?: 3
        return completedIn7Days < targetTimes
    }

    private fun showIndividualNotification(context: Context, habitId: Int, habitName: String, habitWhy: String, shouldShowWhy: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"

        val channelName = when (language) {
            "de" -> "Gewohnheiten Erinnerungen"
            "ka" -> "ჩვევების შეხსენებები"
            "zh" -> "习惯提醒"
            "fr" -> "Rappels d'habitudes"
            else -> "Habit Reminders"
        }
        val channelDesc = when (language) {
            "de" -> "Erinnert an noch nicht erledigte Gewohnheiten"
            "ka" -> "შეხსენება შეუსრულებელი ჩვევების შესახებ"
            "zh" -> "提醒尚未完成的习惯"
            "fr" -> "Rappelle les habitudes non complétées"
            else -> "Reminds you of pending habits"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableVibration(true)
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

        val title = when (language) {
            "de" -> "Zeit für deine Gewohnheit! 🚀"
            "ka" -> "დროა თქვენი ჩვევისთვის! 🚀"
            "zh" -> "该完成你的习惯了！🚀"
            "fr" -> "C'est l'heure de votre habitude ! 🚀"
            else -> "Time for your habit! 🚀"
        }
        val text = if (shouldShowWhy && habitWhy.isNotBlank()) {
            when (language) {
                "de" -> "Dein „Warum“: „$habitWhy“"
                "ka" -> "შენი „რატომ“: „$habitWhy“"
                "zh" -> "你的“为什么”：“$habitWhy”"
                "fr" -> "Votre « Pourquoi » : « $habitWhy »"
                else -> "Your 'Why': \"$habitWhy\""
            }
        } else {
            when (language) {
                "de" -> "Hast du '$habitName' heute schon erledigt? Bleib dran!"
                "ka" -> "შეასრულეთ '$habitName' დღეს? განაგრძეთ!"
                "zh" -> "今天完成“$habitName”了吗？坚持下去！"
                "fr" -> "Avez-vous complété '$habitName' aujourd'hui ? Continuez !"
                else -> "Did you complete '$habitName' today? Keep it up!"
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(habitId, notification)
    }

    private fun showNotification(context: Context, pendingCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"

        val channelName = when (language) {
            "de" -> "Gewohnheiten Erinnerungen"
            "ka" -> "ჩვევების შეხსენებები"
            "zh" -> "习惯提醒"
            "fr" -> "Rappels d'habitudes"
            else -> "Habit Reminders"
        }
        val channelDesc = when (language) {
            "de" -> "Erinnert an noch nicht erledigte Gewohnheiten"
            "ka" -> "შეხსენება შეუსრულებელი ჩვევების შესახებ"
            "zh" -> "提醒尚未完成的习惯"
            "fr" -> "Rappelle les habitudes non complétées"
            else -> "Reminds you of pending habits"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableVibration(true)
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

        val title = when (language) {
            "de" -> "Vergiss deine Habits nicht! 🚀"
            "ka" -> "არ დაგავიწყდეთ თქვენი ჩვევები! 🚀"
            "zh" -> "别忘了你的习惯！🚀"
            "fr" -> "N'oubliez pas vos habitudes ! 🚀"
            else -> "Don't forget your habits! 🚀"
        }
        val text = when (language) {
            "de" -> "Du hast heute noch $pendingCount Gewohnheit(en) offen. Bleib dran!"
            "ka" -> "დღეს ჯერ კიდევ გაქვთ $pendingCount ჩვევა შესასრულებელი. განაგრძეთ!"
            "zh" -> "你今天还有 $pendingCount 个习惯待完成。加油！"
            "fr" -> "Il vous reste $pendingCount habitude(s) à accomplir aujourd'hui. Continuez !"
            else -> "You still have $pendingCount habit(s) pending today. Keep going!"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(101, notification)
    }
}
