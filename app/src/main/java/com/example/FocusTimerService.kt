package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.frequent.habits.R
import com.example.data.AppDatabase
import com.example.data.HabitLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FocusTimerManager {
    private val _activeHabitId = MutableStateFlow<Long?>(null)
    val activeHabitId = _activeHabitId.asStateFlow()

    private val _activeHabitName = MutableStateFlow("")
    val activeHabitName = _activeHabitName.asStateFlow()

    private val _activeHabitColor = MutableStateFlow("")
    val activeHabitColor = _activeHabitColor.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining = _secondsRemaining.asStateFlow()

    private val _initialSeconds = MutableStateFlow(0)
    val initialSeconds = _initialSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _selectedAudioPath = MutableStateFlow<String?>(null)
    val selectedAudioPath = _selectedAudioPath.asStateFlow()

    private val _selectedDate = MutableStateFlow("")
    val selectedDate = _selectedDate.asStateFlow()

    private val _currentLoggedValue = MutableStateFlow(0f)
    val currentLoggedValue = _currentLoggedValue.asStateFlow()

    fun startTimer(
        context: Context,
        habitId: Long,
        habitName: String,
        habitColor: String,
        durationMinutes: Float = 0f,
        selectedDate: String,
        currentValue: Float,
        audioFile: File?,
        totalSecondsOverride: Int? = null
    ) {
        val totalSecs = totalSecondsOverride ?: (durationMinutes * 60).toInt().coerceAtLeast(5)
        _activeHabitId.value = habitId
        _activeHabitName.value = habitName
        _activeHabitColor.value = habitColor
        _initialSeconds.value = totalSecs.coerceAtLeast(1)
        _secondsRemaining.value = totalSecs.coerceAtLeast(1)
        _isRunning.value = true
        _selectedDate.value = selectedDate
        _currentLoggedValue.value = currentValue
        _selectedAudioPath.value = audioFile?.absolutePath

        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun pauseTimer(context: Context) {
        _isRunning.value = false
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeTimer(context: Context) {
        _isRunning.value = true
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESUME
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun updateAudio(context: Context, audioFile: File?) {
        _selectedAudioPath.value = audioFile?.absolutePath
        if (_isRunning.value) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_UPDATE_AUDIO
            }
            context.startService(intent)
        }
    }

    fun stopTimer(context: Context, logProgress: Boolean = false) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = if (logProgress) FocusTimerService.ACTION_FINISH_AND_LOG else FocusTimerService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun setRemainingSeconds(secs: Int) {
        _secondsRemaining.value = secs
    }

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun reset() {
        _isRunning.value = false
        _activeHabitId.value = null
        _activeHabitName.value = ""
        _activeHabitColor.value = ""
        _secondsRemaining.value = 0
        _initialSeconds.value = 0
        _selectedAudioPath.value = null
    }
}

class FocusTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_OPEN_HABIT_ID = "extra_open_habit_id"

        const val ACTION_START = "com.example.focus_timer.ACTION_START"
        const val ACTION_PAUSE = "com.example.focus_timer.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.focus_timer.ACTION_RESUME"
        const val ACTION_STOP = "com.example.focus_timer.ACTION_STOP"
        const val ACTION_FINISH_AND_LOG = "com.example.focus_timer.ACTION_FINISH_AND_LOG"
        const val ACTION_UPDATE_AUDIO = "com.example.focus_timer.ACTION_UPDATE_AUDIO"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> {
                FocusTimerManager.setRunning(true)
                startForeground(NOTIFICATION_ID, buildNotification())
                startAudioIfConfigured()
                startTicker()
            }
            ACTION_PAUSE -> {
                FocusTimerManager.setRunning(false)
                pauseTicker()
                pauseAudio()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
            }
            ACTION_STOP -> {
                stopAllAndFinish(log = false)
            }
            ACTION_FINISH_AND_LOG -> {
                stopAllAndFinish(log = true)
            }
            ACTION_UPDATE_AUDIO -> {
                restartAudioIfConfigured()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAudioIfConfigured() {
        val path = FocusTimerManager.selectedAudioPath.value
        if (path.isNullOrEmpty()) {
            stopAudio()
            return
        }
        try {
            if (mediaPlayer == null) {
                val file = File(path)
                if (file.exists()) {
                    val mp = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                }
            } else {
                if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseAudio() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restartAudioIfConfigured() {
        stopAudio()
        if (FocusTimerManager.isRunning.value) {
            startAudioIfConfigured()
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
            mediaPlayer = null
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val current = FocusTimerManager.secondsRemaining.value
                if (current > 1) {
                    val next = current - 1
                    FocusTimerManager.setRemainingSeconds(next)
                    // With Live Updates (Chronometer countdown), the system status bar chip and notification
                    // automatically count down in real-time. We periodically sync every 30 seconds to prevent drift
                    // without spamming updates every 1000ms (which would interrupt the Live Update chip animation).
                    if (next % 30 == 0) {
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, buildNotification())
                    }
                } else {
                    FocusTimerManager.setRemainingSeconds(0)
                    onTimerFinished()
                    break
                }
            }
        }
    }

    private fun pauseTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun onTimerFinished() {
        stopAudio()
        try {
            val toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, toneUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val habitId = FocusTimerManager.activeHabitId.value ?: 0L
        val initialSecs = FocusTimerManager.initialSeconds.value
        val minutesToLog = Math.round(initialSecs / 60f).coerceAtLeast(1)
        val selectedDate = FocusTimerManager.selectedDate.value.ifEmpty {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val currentVal = FocusTimerManager.currentLoggedValue.value

        serviceScope.launch(Dispatchers.IO) {
            try {
                if (habitId > 0) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val newTotal = currentVal + minutesToLog
                    db.habitDao().insertLog(
                        HabitLog(
                            habitId = habitId.toInt(),
                            date = selectedDate,
                            value = newTotal
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                showFinishedNotification(minutesLogged = minutesToLog)
                FocusTimerManager.reset()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopAllAndFinish(log: Boolean) {
        timerJob?.cancel()
        stopAudio()

        if (log) {
            val habitId = FocusTimerManager.activeHabitId.value ?: 0L
            val elapsedSecs = FocusTimerManager.initialSeconds.value - FocusTimerManager.secondsRemaining.value
            val minutesToLog = Math.round(elapsedSecs / 60f).coerceAtLeast(1)
            val selectedDate = FocusTimerManager.selectedDate.value.ifEmpty {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }
            val currentVal = FocusTimerManager.currentLoggedValue.value

            serviceScope.launch(Dispatchers.IO) {
                try {
                    if (habitId > 0 && elapsedSecs >= 30) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val newTotal = currentVal + minutesToLog
                        db.habitDao().insertLog(
                            HabitLog(
                                habitId = habitId.toInt(),
                                date = selectedDate,
                                value = newTotal
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    FocusTimerManager.reset()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } else {
            FocusTimerManager.reset()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prefs = getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
            val language = prefs.getString("language", "en") ?: "en"
            val channelName = when (language) {
                "de" -> "Fokus-Timer"
                "ka" -> "ფოკუსის ტაიმერი"
                "zh" -> "专注计时器"
                "fr" -> "Minuteur de concentration"
                else -> "Focus Timer"
            }
            val channelDesc = when (language) {
                "de" -> "Zeigt den laufenden Fokus-Timer und Hintergrund-Sound"
                "ka" -> "აჩვენებს მიმდინარე ფოკუსის ტაიმერს და ფონის ხმას"
                "zh" -> "显示正在进行的专注计时器和背景声音"
                "fr" -> "Affiche le minuteur de concentration actif et l'ambiance sonore"
                else -> "Displays the active focus timer and background soundscape"
            }
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDesc
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val prefs = getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"

        val habitId = FocusTimerManager.activeHabitId.value ?: 0L
        val habitName = FocusTimerManager.activeHabitName.value.ifEmpty { "Focus" }
        val seconds = FocusTimerManager.secondsRemaining.value
        val mins = seconds / 60
        val secs = seconds % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

        val isRunning = FocusTimerManager.isRunning.value
        val statusText = if (isRunning) {
            when (language) {
                "de" -> "Noch $timeFormatted verbleibend"
                "ka" -> "დარჩენილია $timeFormatted"
                "zh" -> "剩余 $timeFormatted"
                "fr" -> "Encore $timeFormatted restant"
                else -> "$timeFormatted remaining"
            }
        } else {
            when (language) {
                "de" -> "Pausiert ($timeFormatted)"
                "ka" -> "შეჩერებულია ($timeFormatted)"
                "zh" -> "已暂停 ($timeFormatted)"
                "fr" -> "En pause ($timeFormatted)"
                else -> "Paused ($timeFormatted)"
            }
        }

        // Tap notification to open habit dialog in MainActivity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_HABIT_ID, habitId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Play / Pause toggle
        val toggleActionIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (isRunning) ACTION_PAUSE else ACTION_RESUME
        }
        val pendingToggle = PendingIntent.getService(
            this,
            if (isRunning) 101 else 102,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleTitle = if (isRunning) {
            when (language) {
                "de" -> "Pause"
                "ka" -> "პაუზა"
                "zh" -> "暂停"
                "fr" -> "Pause"
                else -> "Pause"
            }
        } else {
            when (language) {
                "de" -> "Weiter"
                "ka" -> "გაგრძელება"
                "zh" -> "继续"
                "fr" -> "Reprendre"
                else -> "Resume"
            }
        }

        // Action: Finish & Log
        val finishActionIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_FINISH_AND_LOG
        }
        val pendingFinish = PendingIntent.getService(
            this,
            2,
            finishActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val finishTitle = when (language) {
            "de" -> "Abschließen"
            "ka" -> "დასრულება"
            "zh" -> "完成并保存"
            "fr" -> "Terminer"
            else -> "Finish"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏱️ $habitName")
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColorized(false) // Promoted Ongoing / Live Updates requirement
            .addAction(0, toggleTitle, pendingToggle)
            .addAction(0, finishTitle, pendingFinish)

        if (isRunning) {
            // Live Updates / Chronometer count-down configuration:
            // The system chip in the status bar and lockscreen smoothly counts down by itself.
            val targetEndTime = System.currentTimeMillis() + (seconds * 1000L)
            builder.setWhen(targetEndTime)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setShowWhen(true)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }

        // 1. Request Promoted Ongoing via Builder extras (Android 16+ Live Updates / Status Bar Chip)
        val extras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
        }
        builder.addExtras(extras)

        // 2. Request via reflection if NotificationCompat.Builder or underlying platform builder has setRequestPromotedOngoing
        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, true)
        } catch (_: Throwable) {
            // Safe fallback for older AndroidX versions
        }

        val notification = builder.build()

        // 3. Ensure the extra is present in the final Notification object
        notification.extras.putBoolean("android.requestPromotedOngoing", true)

        // 4. Set FLAG_PROMOTED_ONGOING if exposed in Android 16+
        try {
            val flagField = android.app.Notification::class.java.getField("FLAG_PROMOTED_ONGOING")
            val flag = flagField.getInt(null)
            notification.flags = notification.flags or flag
        } catch (_: Throwable) {
            // Ignored on Android < 16
        }

        return notification
    }

    private fun showFinishedNotification(minutesLogged: Int) {
        val prefs = getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"

        val habitName = FocusTimerManager.activeHabitName.value
        val title = when (language) {
            "de" -> "🎉 Fokus-Timer beendet!"
            "ka" -> "🎉 ფოკუსის ტაიმერი დასრულდა!"
            "zh" -> "🎉 专注计时完成！"
            "fr" -> "🎉 Minuteur de concentration terminé !"
            else -> "🎉 Focus timer finished!"
        }
        val text = when (language) {
            "de" -> "$minutesLogged Min. für '$habitName' eingetragen."
            "ka" -> "$minutesLogged წთ ჩაწერილია '$habitName'-სთვის."
            "zh" -> "已为“$habitName”记录 $minutesLogged 分钟。"
            "fr" -> "$minutesLogged min enregistrées pour '$habitName'."
            else -> "Logged $minutesLogged min for '$habitName'."
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopAudio()
    }
}
