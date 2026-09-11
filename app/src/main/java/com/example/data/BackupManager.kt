package com.example.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {

    suspend fun exportDatabaseToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val habits = db.habitDao().getAllHabitsRaw()
        val logs = db.habitDao().getAllLogsRaw()
        val notes = db.habitDao().getAllDailyNotesRaw()
        val timeCapsuleNotes = db.habitDao().getAllTimeCapsuleNotesRaw()
        val milestoneRewards = db.habitDao().getAllMilestoneRewardsRaw()

        val rootJson = JSONObject()
        rootJson.put("version", 3)

        val habitsArray = JSONArray()
        for (habit in habits) {
            val hJson = JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("category", habit.category)
                put("icon", habit.icon)
                put("color", habit.color)
                put("isNegative", habit.isNegative)
                put("type", habit.type)
                put("unit", habit.unit)
                put("targetValue", habit.targetValue.toDouble())
                put("frequency", habit.frequency)
                put("specificDays", habit.specificDays)
                put("startDate", habit.startDate)
                put("createdAt", habit.createdAt)
                put("sortOrder", habit.sortOrder)
                put("reminderEnabled", habit.reminderEnabled)
                put("reminderHour", habit.reminderHour)
                put("reminderMinute", habit.reminderMinute)
                put("customReminders", habit.customReminders)
                put("isArchived", habit.isArchived)
                put("description", habit.description)
                put("clickIncrement", habit.clickIncrement.toDouble())
                if (habit.minimalViableValue != null) {
                    put("minimalViableValue", habit.minimalViableValue.toDouble())
                }
                put("minimalViableText", habit.minimalViableText)
                put("why", habit.why)
                if (habit.stackedOnHabitId != null) {
                    put("stackedOnHabitId", habit.stackedOnHabitId)
                }
                put("isFinishable", habit.isFinishable)
                if (habit.totalTargetValue != null) {
                    put("totalTargetValue", habit.totalTargetValue.toDouble())
                }
                put("isCompletedGoal", habit.isCompletedGoal)
                if (habit.completedAt != null) {
                    put("completedAt", habit.completedAt)
                }
                put("completionNote", habit.completionNote)
            }
            habitsArray.put(hJson)
        }
        rootJson.put("habits", habitsArray)

        val logsArray = JSONArray()
        for (log in logs) {
            val lJson = JSONObject().apply {
                put("id", log.id)
                put("habitId", log.habitId)
                put("date", log.date)
                put("value", log.value.toDouble())
                put("timestamp", log.timestamp)
                put("isPaused", log.isPaused)
                put("isMinimalViable", log.isMinimalViable)
            }
            logsArray.put(lJson)
        }
        rootJson.put("logs", logsArray)

        val notesArray = JSONArray()
        for (note in notes) {
            val nJson = JSONObject().apply {
                put("date", note.date)
                put("content", note.content)
            }
            notesArray.put(nJson)
        }
        rootJson.put("dailyNotes", notesArray)

        val timeCapsuleArray = JSONArray()
        for (tcNote in timeCapsuleNotes) {
            val tcJson = JSONObject().apply {
                put("id", tcNote.id)
                put("type", tcNote.type)
                put("targetPeriod", tcNote.targetPeriod)
                put("content", tcNote.content)
                put("createdAt", tcNote.createdAt)
            }
            timeCapsuleArray.put(tcJson)
        }
        rootJson.put("timeCapsuleNotes", timeCapsuleArray)

        val milestoneArray = JSONArray()
        for (mReward in milestoneRewards) {
            val mJson = JSONObject().apply {
                put("id", mReward.id)
                put("habitId", mReward.habitId)
                put("rewardText", mReward.rewardText)
                put("description", mReward.description)
                put("isRedeemed", mReward.isRedeemed)
                put("unlockedAt", mReward.unlockedAt)
                put("conditionType", mReward.conditionType)
                put("conditionValue", mReward.conditionValue)
                put("trophyId", mReward.trophyId)
            }
            milestoneArray.put(mJson)
        }
        rootJson.put("milestoneRewards", milestoneArray)

        // Settings and Preferences Export
        val settingsJson = JSONObject().apply {
            val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
            put("user_name", prefs.getString("user_name", ""))
            put("profile_image_uri", prefs.getString("profile_image_uri", ""))
            
            // Backup profile image file if it exists
            val avatarFile = java.io.File(context.filesDir, "profile_avatar.jpg")
            if (avatarFile.exists() && avatarFile.length() > 0) {
                try {
                    val bytes = avatarFile.readBytes()
                    val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    put("profile_image_base64", base64Str)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            put("language", prefs.getString("language", "en"))
            put("accent_color_name", prefs.getString("accent_color_name", "PURPLE"))
            put("dark_mode_enabled", prefs.getBoolean("dark_mode_enabled", true))
            put("widget_opacity", prefs.getFloat("widget_opacity", 1.0f).toDouble())
            put("vibration_enabled", prefs.getBoolean("vibration_enabled", true))
            put("info_cards_enabled", prefs.getBoolean("info_cards_enabled", true))
            put("notifications_enabled", prefs.getBoolean("notifications_enabled", true))
            put("insight_notifications_enabled", prefs.getBoolean("insight_notifications_enabled", true))
            put("monthly_review_enabled", prefs.getBoolean("monthly_review_enabled", true))
            put("yearly_review_enabled", prefs.getBoolean("yearly_review_enabled", true))
            put("reminder_enabled", prefs.getBoolean("reminder_enabled", false))
            put("reminder_hour", prefs.getInt("reminder_hour", 18))
            put("reminder_minute", prefs.getInt("reminder_minute", 0))
            put("is_saskia_unlocked", prefs.getBoolean("is_saskia_unlocked", false))
            put("smart_insight_dismissed_date", prefs.getString("smart_insight_dismissed_date", ""))
            put("has_onboarded", prefs.getBoolean("has_onboarded", false))
            put("current_perfect_streak", prefs.getInt("current_perfect_streak", 0))

            val dismissedSet = prefs.getStringSet("dismissed_reviews", emptySet()) ?: emptySet()
            val dismissedArr = JSONArray()
            dismissedSet.forEach { dismissedArr.put(it) }
            put("dismissed_reviews", dismissedArr)

            val achievementSet = prefs.getStringSet("known_unlocked_achievement_ids", emptySet()) ?: emptySet()
            val achievementArr = JSONArray()
            achievementSet.forEach { achievementArr.put(it) }
            put("known_unlocked_achievement_ids", achievementArr)

            val colorPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
            put("user_saved_color_slots", colorPrefs.getString("user_saved_color_slots", ""))

            val audioPrefs = context.getSharedPreferences("audio_soundscape_prefs", Context.MODE_PRIVATE)
            put("last_selected_audio_filename", audioPrefs.getString("last_selected_audio_filename", ""))
            put("recent_audio_filenames", audioPrefs.getString("recent_audio_filenames", ""))
        }
        rootJson.put("settings", settingsJson)

        rootJson.toString(2)
    }

    suspend fun restoreDatabaseFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject(jsonString)
            val habitsArray = rootJson.getJSONArray("habits")
            val logsArray = rootJson.getJSONArray("logs")
            val notesArray = rootJson.optJSONArray("dailyNotes")
            val tcNotesArray = rootJson.optJSONArray("timeCapsuleNotes")
            val milestoneArray = rootJson.optJSONArray("milestoneRewards")
            val settingsJson = rootJson.optJSONObject("settings")

            val db = AppDatabase.getDatabase(context)
            
            // Perform restore sequentially
            db.habitDao().clearAllLogs()
            db.habitDao().clearAllHabits()
            db.habitDao().clearAllDailyNotes()
            db.habitDao().clearAllTimeCapsuleNotes()
            db.habitDao().clearAllMilestoneRewards()

            for (i in 0 until habitsArray.length()) {
                val hJson = habitsArray.getJSONObject(i)
                val habit = Habit(
                    id = hJson.getInt("id"),
                    name = hJson.getString("name"),
                    category = hJson.optString("category", "Allgemein"),
                    icon = hJson.optString("icon", "sparkle"),
                    color = hJson.optString("color", "purple"),
                    isNegative = hJson.optBoolean("isNegative", false),
                    type = hJson.optString("type", "BINARY"),
                    unit = hJson.optString("unit", ""),
                    targetValue = hJson.optDouble("targetValue", 1.0).toFloat(),
                    frequency = hJson.optString("frequency", "DAILY"),
                    specificDays = hJson.optString("specificDays", ""),
                    startDate = hJson.optLong("startDate", System.currentTimeMillis()),
                    createdAt = hJson.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = hJson.optInt("sortOrder", 0),
                    reminderEnabled = hJson.optBoolean("reminderEnabled", false),
                    reminderHour = hJson.optInt("reminderHour", 18),
                    reminderMinute = hJson.optInt("reminderMinute", 0),
                    customReminders = hJson.optString("customReminders", ""),
                    isArchived = hJson.optBoolean("isArchived", false),
                    description = hJson.optString("description", ""),
                    clickIncrement = hJson.optDouble("clickIncrement", 1.0).toFloat(),
                    minimalViableValue = if (hJson.has("minimalViableValue") && !hJson.isNull("minimalViableValue")) hJson.getDouble("minimalViableValue").toFloat() else null,
                    minimalViableText = hJson.optString("minimalViableText", ""),
                    why = hJson.optString("why", ""),
                    stackedOnHabitId = if (hJson.has("stackedOnHabitId") && !hJson.isNull("stackedOnHabitId")) hJson.getInt("stackedOnHabitId") else null,
                    isFinishable = hJson.optBoolean("isFinishable", false),
                    totalTargetValue = if (hJson.has("totalTargetValue") && !hJson.isNull("totalTargetValue")) hJson.getDouble("totalTargetValue").toFloat() else null,
                    isCompletedGoal = hJson.optBoolean("isCompletedGoal", false),
                    completedAt = if (hJson.has("completedAt") && !hJson.isNull("completedAt")) hJson.getLong("completedAt") else null,
                    completionNote = hJson.optString("completionNote", "")
                )
                db.habitDao().insertHabit(habit)
            }

            for (i in 0 until logsArray.length()) {
                val lJson = logsArray.getJSONObject(i)
                val log = HabitLog(
                    id = lJson.getInt("id"),
                    habitId = lJson.getInt("habitId"),
                    date = lJson.getString("date"),
                    value = lJson.optDouble("value", 1.0).toFloat(),
                    timestamp = lJson.optLong("timestamp", System.currentTimeMillis()),
                    isPaused = lJson.optBoolean("isPaused", false),
                    isMinimalViable = lJson.optBoolean("isMinimalViable", false)
                )
                db.habitDao().insertLog(log)
            }

            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val nJson = notesArray.getJSONObject(i)
                    val note = DailyNote(
                        date = nJson.getString("date"),
                        content = nJson.getString("content")
                    )
                    db.habitDao().insertDailyNote(note)
                }
            }

            if (tcNotesArray != null) {
                for (i in 0 until tcNotesArray.length()) {
                    val tcJson = tcNotesArray.getJSONObject(i)
                    val tcNote = TimeCapsuleNote(
                        id = tcJson.optInt("id", 0),
                        type = tcJson.getString("type"),
                        targetPeriod = tcJson.getString("targetPeriod"),
                        content = tcJson.getString("content"),
                        createdAt = tcJson.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.habitDao().insertTimeCapsuleNote(tcNote)
                }
            }

            if (milestoneArray != null) {
                for (i in 0 until milestoneArray.length()) {
                    val mJson = milestoneArray.getJSONObject(i)
                    val mReward = MilestoneReward(
                        id = mJson.optInt("id", 0),
                        habitId = mJson.getInt("habitId"),
                        rewardText = mJson.getString("rewardText"),
                        description = mJson.optString("description", ""),
                        isRedeemed = mJson.optBoolean("isRedeemed", false),
                        unlockedAt = mJson.optLong("unlockedAt", 0L),
                        conditionType = mJson.optString("conditionType", ""),
                        conditionValue = mJson.optInt("conditionValue", 0),
                        trophyId = mJson.optString("trophyId", "")
                    )
                    db.habitDao().insertMilestoneReward(mReward)
                }
            }

            if (settingsJson != null) {
                val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
                val editor = prefs.edit()

                if (settingsJson.has("user_name")) editor.putString("user_name", settingsJson.optString("user_name", ""))
                if (settingsJson.has("profile_image_uri")) editor.putString("profile_image_uri", settingsJson.optString("profile_image_uri", ""))

                // Restore profile image file if present in backup
                if (settingsJson.has("profile_image_base64")) {
                    val base64Str = settingsJson.optString("profile_image_base64", "")
                    if (base64Str.isNotEmpty()) {
                        try {
                            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                            val avatarFile = java.io.File(context.filesDir, "profile_avatar.jpg")
                            avatarFile.writeBytes(bytes)
                            editor.putString("profile_image_uri", android.net.Uri.fromFile(avatarFile).toString())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (settingsJson.has("language")) editor.putString("language", settingsJson.optString("language", "en"))
                if (settingsJson.has("accent_color_name")) editor.putString("accent_color_name", settingsJson.optString("accent_color_name", "PURPLE"))
                if (settingsJson.has("dark_mode_enabled")) editor.putBoolean("dark_mode_enabled", settingsJson.optBoolean("dark_mode_enabled", true))
                if (settingsJson.has("widget_opacity")) editor.putFloat("widget_opacity", settingsJson.optDouble("widget_opacity", 1.0).toFloat())
                if (settingsJson.has("vibration_enabled")) editor.putBoolean("vibration_enabled", settingsJson.optBoolean("vibration_enabled", true))
                if (settingsJson.has("info_cards_enabled")) editor.putBoolean("info_cards_enabled", settingsJson.optBoolean("info_cards_enabled", true))
                if (settingsJson.has("notifications_enabled")) editor.putBoolean("notifications_enabled", settingsJson.optBoolean("notifications_enabled", true))
                if (settingsJson.has("insight_notifications_enabled")) editor.putBoolean("insight_notifications_enabled", settingsJson.optBoolean("insight_notifications_enabled", true))
                if (settingsJson.has("monthly_review_enabled")) editor.putBoolean("monthly_review_enabled", settingsJson.optBoolean("monthly_review_enabled", true))
                if (settingsJson.has("yearly_review_enabled")) editor.putBoolean("yearly_review_enabled", settingsJson.optBoolean("yearly_review_enabled", true))
                if (settingsJson.has("reminder_enabled")) editor.putBoolean("reminder_enabled", settingsJson.optBoolean("reminder_enabled", false))
                if (settingsJson.has("reminder_hour")) editor.putInt("reminder_hour", settingsJson.optInt("reminder_hour", 18))
                if (settingsJson.has("reminder_minute")) editor.putInt("reminder_minute", settingsJson.optInt("reminder_minute", 0))
                if (settingsJson.has("is_saskia_unlocked")) editor.putBoolean("is_saskia_unlocked", settingsJson.optBoolean("is_saskia_unlocked", false))
                if (settingsJson.has("smart_insight_dismissed_date")) editor.putString("smart_insight_dismissed_date", settingsJson.optString("smart_insight_dismissed_date", ""))
                if (settingsJson.has("has_onboarded")) editor.putBoolean("has_onboarded", settingsJson.optBoolean("has_onboarded", false))
                if (settingsJson.has("current_perfect_streak")) editor.putInt("current_perfect_streak", settingsJson.optInt("current_perfect_streak", 0))

                val dismissedArr = settingsJson.optJSONArray("dismissed_reviews")
                if (dismissedArr != null) {
                    val set = mutableSetOf<String>()
                    for (i in 0 until dismissedArr.length()) {
                        set.add(dismissedArr.getString(i))
                    }
                    editor.putStringSet("dismissed_reviews", set)
                }

                val achievementArr = settingsJson.optJSONArray("known_unlocked_achievement_ids")
                if (achievementArr != null) {
                    val set = mutableSetOf<String>()
                    for (i in 0 until achievementArr.length()) {
                        set.add(achievementArr.getString(i))
                    }
                    editor.putStringSet("known_unlocked_achievement_ids", set)
                }

                editor.apply()

                if (settingsJson.has("user_saved_color_slots")) {
                    val colorPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
                    colorPrefs.edit().putString("user_saved_color_slots", settingsJson.optString("user_saved_color_slots", "")).apply()
                }

                if (settingsJson.has("last_selected_audio_filename") || settingsJson.has("recent_audio_filenames")) {
                    val audioPrefs = context.getSharedPreferences("audio_soundscape_prefs", Context.MODE_PRIVATE)
                    audioPrefs.edit()
                        .putString("last_selected_audio_filename", settingsJson.optString("last_selected_audio_filename", ""))
                        .putString("recent_audio_filenames", settingsJson.optString("recent_audio_filenames", ""))
                        .apply()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreDatabaseFromInputStream(context: Context, inputStream: java.io.InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.readBytes()
            if (bytes.isEmpty()) return@withContext false

            var jsonString: String? = null

            // Try reading as ZIP archive
            try {
                val zis = java.util.zip.ZipInputStream(bytes.inputStream())
                var entry = zis.nextEntry
                var hasZipEntries = false
                val audioDir = java.io.File(context.filesDir, "audios")
                if (!audioDir.exists()) audioDir.mkdirs()

                while (entry != null) {
                    hasZipEntries = true
                    val entryName = entry.name
                    if (entryName == "backup.json" || entryName.endsWith(".json")) {
                        jsonString = zis.bufferedReader().readText()
                    } else if (entryName.startsWith("audios/")) {
                        val audioFileName = entryName.removePrefix("audios/")
                        if (audioFileName.isNotEmpty()) {
                            val destFile = java.io.File(audioDir, audioFileName)
                            destFile.outputStream().use { os ->
                                zis.copyTo(os)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zis.close()
                if (!hasZipEntries) jsonString = null
            } catch (e: Exception) {
                jsonString = null
            }

            // Fallback to plain JSON string if not a ZIP archive or if ZIP extraction didn't yield jsonString
            if (jsonString == null) {
                jsonString = String(bytes, Charsets.UTF_8)
            }

            restoreDatabaseFromJson(context, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performBackup(context: Context, treeUriStr: String): Boolean = withContext(Dispatchers.IO) {
        if (treeUriStr.isEmpty()) return@withContext false
        try {
            val treeUri = Uri.parse(treeUriStr)
            val rootFolder = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            if (!rootFolder.exists() || !rootFolder.canWrite()) return@withContext false

            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val dateStr = sdf.format(Date())
            val filename = "backup_frequent_habits_$dateStr.zip"

            val jsonContent = exportDatabaseToJson(context)
            val audioDir = java.io.File(context.filesDir, "audios")
            val audioFiles = if (audioDir.exists()) {
                audioDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "aac") } ?: emptyList()
            } else emptyList()

            val backupFile = rootFolder.createFile("application/zip", filename) ?: return@withContext false
            context.contentResolver.openOutputStream(backupFile.uri)?.use { os ->
                val zos = java.util.zip.ZipOutputStream(os)
                // 1. Write backup.json
                zos.putNextEntry(java.util.zip.ZipEntry("backup.json"))
                zos.write(jsonContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Write custom audio files
                audioFiles.forEach { aFile ->
                    try {
                        zos.putNextEntry(java.util.zip.ZipEntry("audios/${aFile.name}"))
                        aFile.inputStream().use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                zos.close()
            }

            // Cleanup oldest backups if count > 3
            val files = rootFolder.listFiles()
            val backupFiles = mutableListOf<DocumentFile>()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        val name = file.name
                        if (name != null && name.startsWith("backup_frequent_habits_") && (name.endsWith(".zip") || name.endsWith(".json"))) {
                            backupFiles.add(file)
                        }
                    }
                }
            }
            backupFiles.sortBy { it.name }

            if (backupFiles.size > 3) {
                val toDeleteCount = backupFiles.size - 3
                for (i in 0 until toDeleteCount) {
                    backupFiles[i].delete()
                }
            }

            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
                .edit()
                .putString("last_backup_time", timeStr)
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLastBackupTime(context: Context): String {
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        return prefs.getString("last_backup_time", "") ?: ""
    }
}

