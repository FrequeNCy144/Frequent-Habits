package com.example.ui.screens.createhabit

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.tr

@Composable
fun CreateHabitStepZiel(
    language: String,
    activeColor: Color,
    // Target Goal parameters
    type: String,
    onTypeChange: (String) -> Unit,
    targetValueStr: String,
    onTargetValueChange: (String) -> Unit,
    clickIncrementStr: String,
    onClickIncrementChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    selectedChip: String,
    onChipSelect: (String) -> Unit,
    isFinishable: Boolean,
    onFinishableChange: (Boolean) -> Unit,
    totalTargetValueStr: String,
    onTotalTargetValueChange: (String) -> Unit,
    endDateStr: String?,
    onEndDateClick: () -> Unit,
    onClearEndDate: () -> Unit,
    isNegative: Boolean,
    onNegativeChange: (Boolean) -> Unit,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    challengeMode: String,
    onChallengeModeChange: (String) -> Unit,
    challengeSuccessRate: Int,
    onChallengeSuccessRateChange: (Int) -> Unit,
    isEditMode: Boolean,
    // Schedule parameters
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    timesWeekly: Int,
    onTimesWeeklyChange: (Int) -> Unit,
    targetDaysPerMonth: Int,
    onTargetDaysPerMonthChange: (Int) -> Unit,
    intervalDays: Int,
    onIntervalDaysChange: (Int) -> Unit,
    selectedWeekdays: Set<Int>,
    onToggleWeekday: (Int) -> Unit,
    // Start Date parameters
    startDateStr: String,
    onStartDateClick: () -> Unit,
    // Info dialog
    onShowInfoDialog: (String, String) -> Unit
) {
    // 1. Target / Goal Card (Messung, Typ, Einheit, Zielwert & Challenge)
    HabitTargetGoalCard(
        language = language,
        isNumeric = type == "NUMBER",
        onNumericChange = { isNum ->
            onTypeChange(if (isNum) "NUMBER" else "BINARY")
            if (isNum && unit.isBlank()) {
                val defaultMinutesUnit = tr(language, "Minuten", "წუთები", "分钟", "Minutes")
                onUnitChange(defaultMinutesUnit)
                onChipSelect(defaultMinutesUnit)
            }
        },
        targetValue = targetValueStr,
        onTargetValueChange = onTargetValueChange,
        targetValueError = false,
        clickIncrement = clickIncrementStr,
        onClickIncrementChange = onClickIncrementChange,
        unit = unit,
        onUnitChange = onUnitChange,
        selectedChip = selectedChip,
        onChipSelect = onChipSelect,
        activeColor = activeColor,
        isFinishable = isFinishable,
        onFinishableChange = onFinishableChange,
        totalTargetValue = totalTargetValueStr,
        onTotalTargetValueChange = onTotalTargetValueChange,
        endDateStr = endDateStr,
        onEndDateClick = onEndDateClick,
        onClearEndDate = onClearEndDate,
        isNegative = isNegative,
        onNegativeChange = onNegativeChange,
        difficulty = difficulty,
        onDifficultyChange = onDifficultyChange,
        challengeMode = challengeMode,
        onChallengeModeChange = onChallengeModeChange,
        challengeSuccessRate = challengeSuccessRate,
        onChallengeSuccessRateChange = onChallengeSuccessRateChange,
        frequencyType = frequency,
        isEditMode = isEditMode,
        onShowInfoDialog = onShowInfoDialog
    )

    // 2. Schedule Card (Häufigkeit & Wochentage)
    HabitScheduleCard(
        language = language,
        frequencyType = frequency,
        onFrequencyTypeChange = onFrequencyChange,
        isNumeric = (type == "NUMBER"),
        targetDaysPerWeek = timesWeekly,
        onTargetDaysPerWeekChange = onTimesWeeklyChange,
        targetDaysPerMonth = targetDaysPerMonth,
        onTargetDaysPerMonthChange = onTargetDaysPerMonthChange,
        intervalDays = intervalDays,
        onIntervalDaysChange = onIntervalDaysChange,
        selectedWeekdays = selectedWeekdays,
        onToggleWeekday = onToggleWeekday,
        activeColor = activeColor,
        onShowInfoDialog = onShowInfoDialog
    )

    // 3. Start Date Card (Beginn des Trackings)
    HabitStartDateCard(
        language = language,
        startDateStr = startDateStr,
        onStartDateClick = onStartDateClick,
        accentColor = activeColor,
        onShowInfoDialog = onShowInfoDialog
    )
}
