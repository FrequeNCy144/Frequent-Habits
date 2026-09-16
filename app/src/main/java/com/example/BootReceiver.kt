package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED" ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED") {

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NotificationHelper.scheduleSmartInsightNotifications(context)
                    NotificationHelper.scheduleReviewNotifications(context)

                    val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
                    val isGeneralEnabled = prefs.getBoolean("reminder_enabled", false)
                    if (isGeneralEnabled) {
                        val h = prefs.getInt("reminder_hour", 20)
                        val m = prefs.getInt("reminder_minute", 0)
                        NotificationHelper.scheduleReminder(context, h, m)
                    }

                    val db = AppDatabase.getDatabase(context).habitDao()
                    val habits = db.getAllHabitsRaw()
                    habits.forEach { habit ->
                        if (!habit.isArchived) {
                            NotificationHelper.scheduleAllHabitReminders(context, habit)
                        }
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
