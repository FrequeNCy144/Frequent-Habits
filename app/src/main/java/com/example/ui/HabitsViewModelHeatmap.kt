package com.example.ui

import com.example.data.CalendarGridCellData
import kotlinx.coroutines.flow.update

fun HabitsViewModel.navigateHeatmapMonth(delta: Int) {
    _heatmapMonthOffset.update { it + delta }
}

fun HabitsViewModel.selectHeatmapCell(cell: CalendarGridCellData?) {
    if (cell != null) {
        val firstDate = firstHabitDateStr.value
        if (firstDate != null && cell.dateStr < firstDate) {
            return
        }
    }
    _selectedHeatmapCell.value = cell
}

fun HabitsViewModel.setHeatmapViewMode(mode: String) {
    _heatmapViewMode.value = mode
}
