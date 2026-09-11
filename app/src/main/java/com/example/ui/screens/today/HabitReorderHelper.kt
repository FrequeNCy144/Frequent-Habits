package com.example.ui.screens.today

import androidx.compose.foundation.lazy.LazyListItemInfo
import com.example.data.HabitUiItem

object HabitReorderHelper {

    /**
     * Reorders an entire group (root + all its stacked children) within a habit list.
     */
    fun <T> moveSublist(list: List<T>, fromStart: Int, fromEnd: Int, insertBeforeIndex: Int): List<T> {
        if (fromStart < 0 || fromEnd >= list.size || fromStart > fromEnd) return list
        val moving = list.subList(fromStart, fromEnd + 1)
        val remaining = list.filterIndexed { idx, _ -> idx < fromStart || idx > fromEnd }
        val adjustedInsertIndex = if (insertBeforeIndex > fromStart) {
            (insertBeforeIndex - (fromEnd - fromStart + 1)).coerceIn(0, remaining.size)
        } else {
            insertBeforeIndex.coerceIn(0, remaining.size)
        }
        val result = remaining.toMutableList()
        result.addAll(adjustedInsertIndex, moving)
        return result
    }

    /**
     * Swaps two adjacent elements in a list.
     */
    fun swapItems(list: List<HabitUiItem>, indexA: Int, indexB: Int): List<HabitUiItem> {
        if (indexA !in list.indices || indexB !in list.indices || indexA == indexB) return list
        val mutable = list.toMutableList()
        val temp = mutable[indexA]
        mutable[indexA] = mutable[indexB]
        mutable[indexB] = temp
        return mutable
    }

    /**
     * Reorders a habit by moving it up or down, respecting habit stacks (groups).
     */
    fun moveHabitInList(
        currentList: List<HabitUiItem>,
        habitId: Int,
        moveUp: Boolean
    ): List<HabitUiItem> {
        val currentIndex = currentList.indexOfFirst { it.habit.id == habitId }
        if (currentIndex == -1) return currentList

        val draggedItem = currentList[currentIndex]
        val isChild = draggedItem.habit.stackedOnHabitId != null &&
                currentList.any { it.habit.id == draggedItem.habit.stackedOnHabitId }

        if (isChild) {
            // Sibling child movement within the same parent stack
            val parentId = draggedItem.habit.stackedOnHabitId
            if (moveUp) {
                val prevIndex = currentIndex - 1
                if (prevIndex >= 0 && currentList[prevIndex].habit.stackedOnHabitId == parentId) {
                    return swapItems(currentList, currentIndex, prevIndex)
                }
            } else {
                val nextIndex = currentIndex + 1
                if (nextIndex < currentList.size && currentList[nextIndex].habit.stackedOnHabitId == parentId) {
                    return swapItems(currentList, currentIndex, nextIndex)
                }
            }
            return currentList
        }

        // Root habit (moves as a full stack group)
        val groupStart = currentIndex
        var groupEnd = currentIndex
        while (groupEnd + 1 < currentList.size && currentList[groupEnd + 1].habit.stackedOnHabitId == draggedItem.habit.id) {
            groupEnd++
        }

        if (moveUp) {
            val precedingEnd = groupStart - 1
            if (precedingEnd >= 0) {
                var precedingStart = precedingEnd
                while (precedingStart > 0 && currentList[precedingStart].habit.stackedOnHabitId != null &&
                    currentList.any { it.habit.id == currentList[precedingStart].habit.stackedOnHabitId }
                ) {
                    precedingStart--
                }
                return moveSublist(currentList, groupStart, groupEnd, precedingStart)
            }
        } else {
            val succeedingStart = groupEnd + 1
            if (succeedingStart < currentList.size) {
                var succeedingEnd = succeedingStart
                while (succeedingEnd + 1 < currentList.size &&
                    currentList[succeedingEnd + 1].habit.stackedOnHabitId == currentList[succeedingStart].habit.id
                ) {
                    succeedingEnd++
                }
                return moveSublist(currentList, groupStart, groupEnd, succeedingEnd + 1)
            }
        }
        return currentList
    }

    /**
     * Computes whether the dragged habit has crossed the midpoint of a neighboring habit
     * or entire habit stack group. Returns the new list if a swap should happen, or null.
     */
    fun findTargetSwap(
        currentList: List<HabitUiItem>,
        draggedHabitId: Int,
        draggedVisualCenter: Float,
        visibleItems: List<LazyListItemInfo>
    ): List<HabitUiItem>? {
        val currentIndex = currentList.indexOfFirst { it.habit.id == draggedHabitId }
        if (currentIndex == -1) return null

        val draggedItem = currentList[currentIndex]
        val isChild = draggedItem.habit.stackedOnHabitId != null &&
                currentList.any { it.habit.id == draggedItem.habit.stackedOnHabitId }

        if (isChild) {
            val parentId = draggedItem.habit.stackedOnHabitId
            // Check sibling above
            val prevIndex = currentIndex - 1
            if (prevIndex >= 0 && currentList[prevIndex].habit.stackedOnHabitId == parentId) {
                val targetInfo = visibleItems.find { it.key == currentList[prevIndex].habit.id }
                if (targetInfo != null) {
                    val targetCenter = targetInfo.offset + (targetInfo.size / 2f)
                    if (draggedVisualCenter < targetCenter) {
                        return swapItems(currentList, currentIndex, prevIndex)
                    }
                }
            }
            // Check sibling below
            val nextIndex = currentIndex + 1
            if (nextIndex < currentList.size && currentList[nextIndex].habit.stackedOnHabitId == parentId) {
                val targetInfo = visibleItems.find { it.key == currentList[nextIndex].habit.id }
                if (targetInfo != null) {
                    val targetCenter = targetInfo.offset + (targetInfo.size / 2f)
                    if (draggedVisualCenter > targetCenter) {
                        return swapItems(currentList, currentIndex, nextIndex)
                    }
                }
            }
            return null
        }

        // Dragged item is a root habit (independent or anchor of a stack)
        val groupStart = currentIndex
        var groupEnd = currentIndex
        while (groupEnd + 1 < currentList.size && currentList[groupEnd + 1].habit.stackedOnHabitId == draggedItem.habit.id) {
            groupEnd++
        }

        // Check preceding group (moving UP)
        val precedingEnd = groupStart - 1
        if (precedingEnd >= 0) {
            var precedingStart = precedingEnd
            while (precedingStart > 0 && currentList[precedingStart].habit.stackedOnHabitId != null &&
                currentList.any { it.habit.id == currentList[precedingStart].habit.stackedOnHabitId }
            ) {
                precedingStart--
            }
            val topInfo = visibleItems.find { it.key == currentList[precedingStart].habit.id }
            val bottomInfo = visibleItems.find { it.key == currentList[precedingEnd].habit.id }
            val targetGroupCenter: Float? = when {
                topInfo != null && bottomInfo != null -> {
                    (topInfo.offset + (bottomInfo.offset + bottomInfo.size)) / 2f
                }
                topInfo != null -> {
                    val groupCount = precedingEnd - precedingStart + 1
                    topInfo.offset + (groupCount * topInfo.size) / 2f
                }
                bottomInfo != null -> {
                    val groupCount = precedingEnd - precedingStart + 1
                    bottomInfo.offset + bottomInfo.size - (groupCount * bottomInfo.size) / 2f
                }
                else -> null
            }
            if (targetGroupCenter != null && draggedVisualCenter < targetGroupCenter) {
                return moveSublist(currentList, groupStart, groupEnd, precedingStart)
            }
        }

        // Check succeeding group (moving DOWN)
        val succeedingStart = groupEnd + 1
        if (succeedingStart < currentList.size) {
            var succeedingEnd = succeedingStart
            while (succeedingEnd + 1 < currentList.size &&
                currentList[succeedingEnd + 1].habit.stackedOnHabitId == currentList[succeedingStart].habit.id
            ) {
                succeedingEnd++
            }
            val topInfo = visibleItems.find { it.key == currentList[succeedingStart].habit.id }
            val bottomInfo = visibleItems.find { it.key == currentList[succeedingEnd].habit.id }
            val targetGroupCenter: Float? = when {
                topInfo != null && bottomInfo != null -> {
                    (topInfo.offset + (bottomInfo.offset + bottomInfo.size)) / 2f
                }
                topInfo != null -> {
                    val groupCount = succeedingEnd - succeedingStart + 1
                    topInfo.offset + (groupCount * topInfo.size) / 2f
                }
                bottomInfo != null -> {
                    val groupCount = succeedingEnd - succeedingStart + 1
                    bottomInfo.offset + bottomInfo.size - (groupCount * bottomInfo.size) / 2f
                }
                else -> null
            }
            if (targetGroupCenter != null && draggedVisualCenter > targetGroupCenter) {
                return moveSublist(currentList, groupStart, groupEnd, succeedingEnd + 1)
            }
        }

        return null
    }
}
