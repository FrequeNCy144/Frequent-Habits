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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class SmartInsightNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("insight_notifications_enabled", true)
        if (!enabled) return

        val lastSent = prefs.getLong("insight_notification_last_sent", 0L)
        val now = System.currentTimeMillis()
        // Wait at least 6 days between insights (roughly once a week)
        if (now - lastSent < 6L * 24 * 60 * 60 * 1000) {
            NotificationHelper.scheduleSmartInsightNotifications(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                evaluateAndShowInsight(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
                NotificationHelper.scheduleSmartInsightNotifications(context)
            }
        }
    }

    private suspend fun evaluateAndShowInsight(context: Context) {
        val db = AppDatabase.getDatabase(context).habitDao()
        val allHabits = db.getAllHabitsRaw()
        if (allHabits.isEmpty()) return
        val allLogs = db.getAllLogsRaw()

        // 7-day rule check: Never send push notifications before unlocking smart insights
        if (!SmartInsightEngine.hasUnlockedSmartInsights(allLogs)) {
            return
        }
        
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        val userName = prefs.getString("user_name", "") ?: ""
        val dismissedReviews = prefs.getStringSet("dismissed_reviews", emptySet()) ?: emptySet()
        
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayDateString = sdfDate.format(java.util.Date())

        val insights = SmartInsightEngine.generateInsights(
            context = context,
            allHabits = allHabits,
            allLogs = allLogs,
            language = language,
            userName = userName,
            todayDateString = todayDateString,
            dismissedReviews = dismissedReviews,
            checkCooldowns = true
        )

        val selected = SmartInsightEngine.selectBestInsight(insights, todayDateString) ?: return
        val cleanText = selected.cleanText

        val channelName = when (language) {
            "de" -> "Smart Insights"
            "ka" -> "ჭკვიანი ანალიტიკა"
            "zh" -> "智能洞察"
            "fr" -> "Smart Insights"
            else -> "Smart Insights"
        }
        val channelDesc = when (language) {
            "de" -> "Wöchentliche Smart Insights Benachrichtigungen"
            "ka" -> "ყოველკვირეული ჭკვიანი ანალიტიკის შეტყობინებები"
            "zh" -> "每周智能洞察通知"
            "fr" -> "Notifications hebdomadaires Smart Insights"
            else -> "Weekly smart insight notifications"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "insight_notifications"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDesc
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9903,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = tr(language, "Neuer Smart Insight 💡", "ახალი ჭკვიანი ანალიტიკა 💡", "新的智能洞察 💡", "New Smart Insight 💡", "Nouveau Smart Insight 💡")
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(cleanText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            
        notificationManager.notify(9903, notification)
        
        // Record cooldown and last sent timestamp
        SmartInsightEngine.recordInsightTypeShown(context, selected.type, todayDateString)
        prefs.edit().putLong("insight_notification_last_sent", System.currentTimeMillis()).apply()
    }
}
