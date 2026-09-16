package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FolderStorageHelper {
    private const val PREFS_NAME = "habits_settings"
    private const val KEY_FOLDERS = "saved_habit_folders"

    fun loadFolders(context: Context): List<HabitFolder> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_FOLDERS, null)
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<HabitFolder>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HabitFolder(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", ""),
                            icon = obj.optString("icon", "folder"),
                            color = obj.optString("color", "purple"),
                            isCollapsed = obj.optBoolean("isCollapsed", false),
                            sortOrder = obj.optInt("sortOrder", i)
                        )
                    )
                }
                list.sortedBy { it.sortOrder }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveFolders(context: Context, folders: List<HabitFolder>) {
        try {
            val array = JSONArray()
            folders.forEach { folder ->
                val obj = JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("icon", folder.icon)
                    put("color", folder.color)
                    put("isCollapsed", folder.isCollapsed)
                    put("sortOrder", folder.sortOrder)
                }
                array.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FOLDERS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleFolderCollapse(context: Context, folderId: Int): List<HabitFolder> {
        val folders = loadFolders(context)
        val updated = folders.map {
            if (it.id == folderId) it.copy(isCollapsed = !it.isCollapsed) else it
        }
        saveFolders(context, updated)
        return updated
    }
}
