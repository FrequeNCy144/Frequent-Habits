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
import java.util.Calendar

class ReviewNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reviewType = intent.getStringExtra("OPEN_REVIEW_TYPE") ?: "MONTHLY"
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "review_notifications"

        val language = prefs.getString("language", "en") ?: "en"
        val channelName = when (language) {
            "de" -> "Rückblick Benachrichtigungen"
            "ka" -> "მიმოხილვის შეტყობინებები"
            "zh" -> "回顾提醒通知"
            "fr" -> "Notifications de rétrospectives"
            else -> "Review Notifications"
        }
        val channelDesc = when (language) {
            "de" -> "Erinnert an Monats- und Jahresrückblicke"
            "ka" -> "შეხსენება ყოველთვიური და წლიური მიმოხილვებისთვის"
            "zh" -> "提醒查看月度和年度回顾"
            "fr" -> "Rappelle les bilans mensuels et annuels"
            else -> "Reminds of monthly and annual reviews"
        }

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

        if (reviewType == "YEARLY") {
            val yearlyEnabled = prefs.getBoolean("yearly_review_enabled", true)
            if (!yearlyEnabled) return

            val targetYear = currentYear - 1
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_REVIEW_TYPE", "YEARLY")
                putExtra("REVIEW_YEAR", targetYear)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                9901,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val language = prefs.getString("language", "en") ?: "en"
            val title = tr(
                language,
                "Dein Jahres-Review ist bereit!",
                "თქვენი წლიური მიმოხილვა მზად არის!",
                "你的年度回顾已准备就绪！",
                "Your Year in Review is Ready!",
                "Votre rétrospective annuelle est prête !"
            )
            val text = tr(
                language,
                "Schau nach, was dein vergangenes Ich dir in der Zeitkapsel hinterlassen hat.",
                "შეამოწმეთ რა დაგიტოვათ თქვენმა წარსულმა მე-მ დროის კაფსულაში.",
                "快来看看过去的你在时间胶囊中留下了什么。",
                "Check out what your past self left you in the time capsule.",
                "Découvrez ce que votre vous du passé vous a laissé dans la capsule temporelle."
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(9901, notification)
        } else {
            val monthlyEnabled = prefs.getBoolean("monthly_review_enabled", true)
            if (!monthlyEnabled) return

            var targetYear = currentYear
            var targetMonth = currentMonth - 1
            if (targetMonth < 1) {
                targetMonth = 12
                targetYear -= 1
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_REVIEW_TYPE", "MONTHLY")
                putExtra("REVIEW_YEAR", targetYear)
                putExtra("REVIEW_MONTH", targetMonth)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                9902,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val language = prefs.getString("language", "en") ?: "en"
            val title = tr(
                language,
                "Dein Monats-Review ist bereit!",
                "თქვენი ყოველთვიური მიმოხილვა მზად არის!",
                "你的月度回顾已准备就绪！",
                "Your Monthly Review is Ready!",
                "Votre rétrospective mensuelle est prête !"
            )
            val text = tr(
                language,
                "Schau nach, was dein vergangenes Ich dir in der Zeitkapsel hinterlassen hat.",
                "შეამოწმეთ რა დაგიტოვათ თქვენმა წარსულმა მე-მ დროის კაფსულაში.",
                "快来看看过去的你在时间胶囊中留下了什么。",
                "Check out what your past self left you in the time capsule.",
                "Découvrez ce que votre vous du passé vous a laissé dans la capsule temporelle."
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(9902, notification)
        }

        // Reschedule next review alarms
        NotificationHelper.scheduleReviewNotifications(context)
    }
}
