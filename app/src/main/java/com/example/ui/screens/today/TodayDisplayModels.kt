package com.example.ui.screens.today

import com.example.data.HabitFolder
import com.example.data.HabitUiItem

sealed class TodayDisplayItem {
    data class LooseHabit(val uiItem: HabitUiItem) : TodayDisplayItem()
    data class FolderGroup(
        val folder: HabitFolder,
        val containedHabits: List<HabitUiItem>,
        val progressPercent: Float,
        val completedCount: Int,
        val totalCount: Int
    ) : TodayDisplayItem()
}

fun buildTodayDisplayItems(
    displayedHabits: List<HabitUiItem>,
    allFolders: List<HabitFolder>,
    isToday: Boolean = true
): List<TodayDisplayItem> {
    val validFolderIds = allFolders.map { it.id }.toSet()
    val habitsByFolder = displayedHabits
        .filter { it.habit.folderId != null && validFolderIds.contains(it.habit.folderId) }
        .groupBy { it.habit.folderId!! }

    val looseHabits = displayedHabits.filter { it.habit.folderId == null || !validFolderIds.contains(it.habit.folderId) }

    val visibleFolders = allFolders.filter { folder ->
        val contained = habitsByFolder[folder.id] ?: emptyList()
        contained.isNotEmpty() || isToday
    }

    val folderGroupItems = visibleFolders.map { folder ->
        val contained = habitsByFolder[folder.id] ?: emptyList()
        val total = contained.size
        val completed = contained.count { it.isCompleted }
        val progress = if (total > 0) completed.toFloat() / total else 0f
        val pos = contained.firstOrNull()?.let { displayedHabits.indexOf(it) } ?: (10000 + folder.sortOrder)
        Pair(
            pos,
            TodayDisplayItem.FolderGroup(
                folder = folder,
                containedHabits = contained,
                progressPercent = progress,
                completedCount = completed,
                totalCount = total
            )
        )
    }

    val looseHabitItems = looseHabits.map { uiItem ->
        val pos = displayedHabits.indexOf(uiItem)
        Pair(pos, TodayDisplayItem.LooseHabit(uiItem))
    }

    return (folderGroupItems + looseHabitItems)
        .sortedWith(compareBy<Pair<Int, TodayDisplayItem>> { it.first })
        .map { it.second }
}

