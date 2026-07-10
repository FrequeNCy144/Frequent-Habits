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

        val rootJson = JSONObject()
        rootJson.put("version", 2)

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
                put("startDate", habit.startDate)
                put("createdAt", habit.createdAt)
                put("sortOrder", habit.sortOrder)
                put("reminderEnabled", habit.reminderEnabled)
                put("reminderHour", habit.reminderHour)
                put("reminderMinute", habit.reminderMinute)
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

        rootJson.toString(2)
    }

    suspend fun restoreDatabaseFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject(jsonString)
            val habitsArray = rootJson.getJSONArray("habits")
            val logsArray = rootJson.getJSONArray("logs")
            val notesArray = rootJson.optJSONArray("dailyNotes")

            val db = AppDatabase.getDatabase(context)
            
            // Perform restore sequentially
            db.habitDao().clearAllLogs()
            db.habitDao().clearAllHabits()
            db.habitDao().clearAllDailyNotes()

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
                    startDate = hJson.optLong("startDate", System.currentTimeMillis()),
                    createdAt = hJson.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = hJson.optInt("sortOrder", 0),
                    reminderEnabled = hJson.optBoolean("reminderEnabled", false),
                    reminderHour = hJson.optInt("reminderHour", 18),
                    reminderMinute = hJson.optInt("reminderMinute", 0)
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
                    isPaused = lJson.optBoolean("isPaused", false)
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
            true
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
            val filename = "backup_frequent_habits_$dateStr.json"

            val jsonContent = exportDatabaseToJson(context)

            val backupFile = rootFolder.createFile("application/json", filename) ?: return@withContext false
            context.contentResolver.openOutputStream(backupFile.uri)?.use { os ->
                os.write(jsonContent.toByteArray())
            }

            // Cleanup oldest backups if count > 3
            val files = rootFolder.listFiles()
            val backupFiles = mutableListOf<DocumentFile>()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        val name = file.name
                        if (name != null && name.startsWith("backup_frequent_habits_") && name.endsWith(".json")) {
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

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
