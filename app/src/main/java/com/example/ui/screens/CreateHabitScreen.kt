package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.data.MilestoneReward
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.screens.createhabit.*
import com.example.ui.theme.AppBg
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun CreateHabitScreen(
    editingHabit: Habit? = null,
    initialMilestoneRewards: List<MilestoneReward> = emptyList(),
    initialStackedHabitId: Int? = null,
    activeHabits: List<Habit> = emptyList(),
    allFolders: List<HabitFolder> = emptyList(),
    language: String,
    accentColor: androidx.compose.ui.graphics.Color = com.example.ui.theme.PrimaryViolet,
    onCreateFolder: ((name: String, icon: String, color: String) -> Int)? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        isNegative: Boolean,
        category: String,
        icon: String,
        color: String,
        type: String,
        unit: String,
        targetValue: Float,
        clickIncrement: Float,
        frequency: String,
        startDateMillis: Long,
        specificDays: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        customReminders: String,
        description: String,
        milestoneRewards: List<MilestoneReward>,
        minimalViableValue: Float?,
        minimalViableText: String,
        placeholderWhy: String,
        stackedOnHabitId: Int?,
        isFinishable: Boolean,
        totalTargetValue: Float?,
        difficulty: String,
        folderId: Int?,
        challengeMode: String,
        challengeSuccessRate: Int
    ) -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember(editingHabit?.id) { mutableStateOf(editingHabit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var description by remember(editingHabit?.id) { mutableStateOf(editingHabit?.description ?: "") }

    var isNegative by remember(editingHabit?.id) { mutableStateOf(editingHabit?.isNegative ?: false) }
    var selectedColor by remember(editingHabit?.id) { mutableStateOf(editingHabit?.color ?: "purple") }
    var selectedIcon by remember(editingHabit?.id) { mutableStateOf(editingHabit?.icon ?: "fitness_center") }
    var iconSearchQuery by remember { mutableStateOf("") }
    var category by remember(editingHabit?.id) { mutableStateOf(editingHabit?.category ?: "Sonstiges") }
    var selectedFolderId by remember(editingHabit?.id) { mutableStateOf<Int?>(editingHabit?.folderId) }

    var type by remember(editingHabit?.id) { mutableStateOf(editingHabit?.type ?: "BINARY") }
    var unit by remember(editingHabit?.id) { mutableStateOf(editingHabit?.unit ?: "") }
    var selectedChip by remember(editingHabit?.id) { mutableStateOf(if (editingHabit?.type == "NUMBER") editingHabit.unit else "") }

    var targetValueStr by remember(editingHabit?.id) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.targetValue > 0f) {
                if (editingHabit.targetValue % 1f == 0f) editingHabit.targetValue.toInt().toString()
                else editingHabit.targetValue.toString()
            } else "1"
        )
    }

    var clickIncrementStr by remember(editingHabit?.id) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.clickIncrement > 0f) {
                if (editingHabit.clickIncrement % 1f == 0f) editingHabit.clickIncrement.toInt().toString()
                else editingHabit.clickIncrement.toString()
            } else "1"
        )
    }

    var frequency by remember(editingHabit?.id) { mutableStateOf(editingHabit?.frequency ?: "EVERYDAY") }
    var specificDaysSet by remember(editingHabit?.id) {
        val rawDays = editingHabit?.specificDays
        val days = if (rawDays != null) {
            rawDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else {
            null
        }
        mutableStateOf(if (!days.isNullOrEmpty()) days else setOf(1, 2, 3, 4, 5))
    }
    var timesWeekly by remember(editingHabit?.id) {
        val num = if (editingHabit?.frequency == "TIMES_WEEKLY") editingHabit.specificDays.toIntOrNull() ?: 3 else 3
        mutableIntStateOf(num)
    }
    var targetDaysPerMonth by remember(editingHabit?.id) {
        val num = if (editingHabit?.frequency == "TIMES_MONTHLY") editingHabit.specificDays.toIntOrNull() ?: 15 else 15
        mutableIntStateOf(num)
    }
    var intervalDays by remember(editingHabit?.id) {
        val num = if (editingHabit?.frequency == "INTERVAL") editingHabit.specificDays.toIntOrNull() ?: 2 else 2
        mutableIntStateOf(num)
    }

    var startDateMillis by remember(editingHabit?.id) {
        mutableLongStateOf(editingHabit?.startDate ?: System.currentTimeMillis())
    }
    var showStartDatePicker by remember { mutableStateOf(false) }

    var endDateMillis by remember(editingHabit?.id) {
        mutableStateOf<Long?>(null)
    }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var reminderEnabled by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.reminderEnabled ?: false)
    }
    var remindersList by remember(editingHabit?.id) {
        if (editingHabit == null) {
            mutableStateOf<List<String>>(emptyList())
        } else {
            val list = mutableListOf<String>()
            if (editingHabit.reminderEnabled) {
                list.add(String.format(Locale.US, "%02d:%02d", editingHabit.reminderHour, editingHabit.reminderMinute))
            }
            if (editingHabit.customReminders.isNotBlank()) {
                val extras = editingHabit.customReminders.split(",").map { it.trim() }.filter { it.isNotBlank() }
                list.addAll(extras)
            }
            mutableStateOf(list.distinct().sorted())
        }
    }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var reminderToEditTime by remember { mutableStateOf<String?>(null) }

    var minimalViableValueStr by remember(editingHabit?.id) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.minimalViableValue != null) {
                if (editingHabit.minimalViableValue % 1f == 0f) editingHabit.minimalViableValue.toInt().toString()
                else editingHabit.minimalViableValue.toString()
            } else ""
        )
    }
    var minimalViableText by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.minimalViableText ?: "")
    }

    var placeholderWhy by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.why ?: "")
    }

    var stackedOnHabitId by remember(editingHabit?.id, initialStackedHabitId) {
        mutableStateOf(editingHabit?.stackedOnHabitId ?: initialStackedHabitId)
    }

    var isFinishable by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.isFinishable ?: false)
    }
    var difficulty by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.difficulty ?: "MEDIUM")
    }
    var challengeMode by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.challengeMode ?: "ACCUMULATE")
    }
    var challengeSuccessRate by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.challengeSuccessRate ?: 90)
    }
    var totalTargetValueStr by remember(editingHabit?.id) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.totalTargetValue != null) {
                if (editingHabit.totalTargetValue % 1f == 0f) editingHabit.totalTargetValue.toInt().toString()
                else editingHabit.totalTargetValue.toString()
            } else ""
        )
    }

    var milestoneRewards by remember(editingHabit?.id, initialMilestoneRewards) {
        mutableStateOf(if (initialMilestoneRewards.isNotEmpty()) initialMilestoneRewards else emptyList<MilestoneReward>())
    }
    var showAddRewardDialog by remember { mutableStateOf(false) }

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    val nameFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (editingHabit == null) {
            kotlinx.coroutines.delay(200)
            try {
                nameFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Focus requester not attached yet
            }
        }
    }

    val activeColor = HabitIconMapping.getColor(selectedColor)

    val allIcons = remember { HabitIconMapping.iconList }
    val filteredIcons = remember(iconSearchQuery) {
        if (iconSearchQuery.isBlank()) allIcons
        else allIcons.filter { it.first.contains(iconSearchQuery, ignoreCase = true) }
    }

    val selectedStartDate = remember(startDateMillis) {
        val instant = java.time.Instant.ofEpochMilli(startDateMillis)
        instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }
    val startDateStr = "${selectedStartDate.dayOfMonth.toString().padStart(2, '0')}.${selectedStartDate.monthValue.toString().padStart(2, '0')}.${selectedStartDate.year}"

    val endDateStr = remember(endDateMillis) {
        endDateMillis?.let {
            val instant = java.time.Instant.ofEpochMilli(it)
            val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthValue.toString().padStart(2, '0')}.${date.year}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 88.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Basic Info Card
            HabitBasicInfoCard(
                language = language,
                name = name,
                onNameChange = { name = it },
                nameError = nameError,
                onNameErrorChange = { nameError = it },
                description = description,
                onDescriptionChange = { description = it },
                nameFocusRequester = nameFocusRequester
            )

            // 2. Target / Goal Card
            HabitTargetGoalCard(
                language = language,
                isNumeric = type == "NUMBER",
                onNumericChange = { isNum ->
                    type = if (isNum) "NUMBER" else "BINARY"
                    if (isNum && unit.isBlank()) {
                        val defaultMinutesUnit = tr(language, "Minuten", "წუთები", "分钟", "Minutes")
                        unit = defaultMinutesUnit
                        selectedChip = defaultMinutesUnit
                    }
                },
                targetValue = targetValueStr,
                onTargetValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                        targetValueStr = input
                    }
                },
                targetValueError = false,
                clickIncrement = clickIncrementStr,
                onClickIncrementChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                        clickIncrementStr = input
                    }
                },
                unit = unit,
                onUnitChange = { unit = it },
                selectedChip = selectedChip,
                onChipSelect = { chip ->
                    selectedChip = chip
                    if (chip != "Anderes..." && chip != "Custom...") {
                        unit = chip
                    } else {
                        unit = ""
                    }
                },
                activeColor = activeColor,
                isFinishable = isFinishable,
                onFinishableChange = { isFinishable = it },
                totalTargetValue = totalTargetValueStr,
                onTotalTargetValueChange = { totalTargetValueStr = it },
                endDateStr = endDateStr,
                onEndDateClick = { showEndDatePicker = true },
                onClearEndDate = { endDateMillis = null },
                isNegative = isNegative,
                onNegativeChange = { isNegative = it },
                difficulty = difficulty,
                onDifficultyChange = { difficulty = it },
                challengeMode = challengeMode,
                onChallengeModeChange = { challengeMode = it },
                challengeSuccessRate = challengeSuccessRate,
                onChallengeSuccessRateChange = { challengeSuccessRate = it },
                isEditMode = editingHabit != null,
                onShowInfoDialog = { title, explanation ->
                    activeExplanation = title to explanation
                }
            )

            // 3. Appearance Card (Color & Icon)
            HabitAppearanceCard(
                language = language,
                selectedColor = selectedColor,
                onColorSelect = { selectedColor = it },
                selectedIcon = selectedIcon,
                onIconSelect = { selectedIcon = it },
                iconSearchQuery = iconSearchQuery,
                onIconSearchQueryChange = { iconSearchQuery = it },
                activeColor = activeColor,
                filteredIcons = filteredIcons
            )

            // Folder Selection Card
            HabitFolderSelectionCard(
                allFolders = allFolders,
                selectedFolderId = selectedFolderId,
                onFolderSelected = { selectedFolderId = it },
                language = language,
                accentColor = accentColor,
                onCreateFolder = onCreateFolder
            )

            // 4. Schedule Card
            HabitScheduleCard(
                language = language,
                frequencyType = frequency,
                onFrequencyTypeChange = { frequency = it },
                targetDaysPerWeek = timesWeekly,
                onTargetDaysPerWeekChange = { timesWeekly = it },
                targetDaysPerMonth = targetDaysPerMonth,
                onTargetDaysPerMonthChange = { targetDaysPerMonth = it },
                intervalDays = intervalDays,
                onIntervalDaysChange = { intervalDays = it },
                selectedWeekdays = specificDaysSet,
                onToggleWeekday = { day ->
                    specificDaysSet = if (day in specificDaysSet) {
                        if (specificDaysSet.size > 1) specificDaysSet - day else specificDaysSet
                    } else {
                        specificDaysSet + day
                    }
                },
                activeColor = activeColor
            )

            // 5. Reminders Card
            HabitRemindersCard(
                language = language,
                reminders = remindersList,
                onAddReminderClick = {
                    reminderToEditTime = null
                    showAddCustomDialog = true
                },
                onRemoveReminder = { index ->
                    val item = remindersList.getOrNull(index)
                    if (item != null) {
                        remindersList = remindersList - item
                        if (remindersList.isEmpty()) {
                            reminderEnabled = false
                        }
                    }
                },
                onEditReminder = { _, time ->
                    reminderToEditTime = time
                    showAddCustomDialog = true
                },
                activeColor = activeColor
            )

            // 6. Milestone Rewards Card
            HabitMilestoneRewardsCard(
                language = language,
                milestoneRewards = milestoneRewards,
                onAddRewardClick = { showAddRewardDialog = true },
                onRemoveReward = { reward ->
                    milestoneRewards = milestoneRewards.filter { it.id != reward.id }
                },
                onShowInfoDialog = {
                    activeExplanation = Pair(
                        tr(language, "Meilenstein-Belohnungen", "Milestone ჯილდოები", "里程碑奖励", "Milestone Rewards"),
                        tr(language, "Verknüpfe Belohnungen direkt mit Erfolgen (z.B. 30 Tage Streak = Massage oder neues Buch). Sobald du den Meilenstein erreichst, wird dir die Belohnung freigeschaltet!", "დააკავშირეთ ჯილდოები მიღწევებთან. როდესაც მიაღწევთ ეტაპს, ჯილდო განბლოკილია!", "将奖励直接与成就关联（如达到 30 天连续打卡 = 奖励自己一次按摩或一本新书）。一旦达到里程碑，奖励即刻解锁！", "Link rewards directly to milestones (e.g., 30-day streak = get a massage or buy a book). Once you hit the milestone, your reward is unlocked!")
                    )
                }
            )

            // 7. Advanced Settings Cards
            HabitAdvancedSettingsCards(
                language = language,
                startDateStr = startDateStr,
                onStartDateClick = { showStartDatePicker = true },
                motivationWhy = placeholderWhy,
                onMotivationWhyChange = { placeholderWhy = it },
                minimalViableRule = if (type == "NUMBER") minimalViableValueStr else minimalViableText,
                onMinimalViableRuleChange = { input ->
                    if (type == "NUMBER") {
                        if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                            minimalViableValueStr = input
                        }
                    } else {
                        minimalViableText = input
                    }
                },
                isNumeric = type == "NUMBER",
                unit = unit,
                selectedStackedHabit = activeHabits.find { it.id == stackedOnHabitId },
                onSelectStackedHabit = { habit ->
                    stackedOnHabitId = habit?.id
                },
                activeHabits = activeHabits.filter { it.id != (editingHabit?.id ?: -1) },
                onShowInfoDialog = { title, explanation ->
                    activeExplanation = title to explanation
                },
                activeColor = activeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Cancel / Save)
            CreateHabitBottomBar(
                isEditing = editingHabit != null,
                language = language,
                onCancel = onDismiss,
                onSaveClick = {
                    if (name.isNotBlank()) {
                        val targetVal = targetValueStr.replace(',', '.').toFloatOrNull()?.coerceAtLeast(0.01f) ?: 1f
                        val clickIncrementVal = clickIncrementStr.replace(',', '.').toFloatOrNull()?.coerceAtLeast(0.01f) ?: 1f
                        val specDays = when (frequency) {
                            "SPECIFIC" -> specificDaysSet.sorted().joinToString(",")
                            "TIMES_WEEKLY" -> timesWeekly.toString()
                            "TIMES_MONTHLY" -> targetDaysPerMonth.toString()
                            "INTERVAL" -> intervalDays.toString()
                            else -> ""
                        }

                        val firstReminder = remindersList.firstOrNull() ?: "18:00"
                        val firstParts = firstReminder.split(":")
                        val firstHour = firstParts.getOrNull(0)?.toIntOrNull() ?: 18
                        val firstMin = firstParts.getOrNull(1)?.toIntOrNull() ?: 0

                        val restReminders = if (remindersList.size > 1) remindersList.drop(1) else emptyList()
                        val customRemindersStr = restReminders.joinToString(",")

                        val minViableValue = if (type == "NUMBER") minimalViableValueStr.replace(',', '.').toFloatOrNull() else null
                        val minViableTextStr = if (type == "BINARY") minimalViableText else ""
                        val totalTargetVal = if (isFinishable) {
                            val parsed = totalTargetValueStr.replace(',', '.').toFloatOrNull()
                            if (parsed != null && parsed > 0f) parsed else 30f
                        } else null

                        // Ensure clean defaults for numeric vs binary
                        val finalUnit = if (type == "NUMBER") unit else ""
                        val finalTargetVal = if (type == "NUMBER") targetVal else 1f
                        val finalClickIncrement = if (type == "NUMBER") clickIncrementVal else 1f

                        onSave(
                            name, isNegative, category, selectedIcon, selectedColor, type, finalUnit, finalTargetVal, finalClickIncrement,
                            frequency, startDateMillis, specDays, reminderEnabled, firstHour, firstMin, customRemindersStr, description, milestoneRewards,
                            minViableValue, minViableTextStr, placeholderWhy, stackedOnHabitId,
                            isFinishable, totalTargetVal, if (isFinishable) difficulty else "MEDIUM",
                            selectedFolderId, challengeMode, challengeSuccessRate
                        )
                    } else {
                        nameError = true
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                            kotlinx.coroutines.delay(100)
                            try {
                                nameFocusRequester.requestFocus()
                                keyboardController?.show()
                            } catch (e: Exception) {
                                // Focus requester error ignored
                            }
                        }
                    }
                }
            )
        }

        // Overlays & Dialogs
        CreateHabitDialogsOverlay(
            showStartDatePicker = showStartDatePicker,
            startDateMillis = startDateMillis,
            onStartDateSelected = {
                startDateMillis = it
                showStartDatePicker = false
            },
            onDismissStartDatePicker = { showStartDatePicker = false },
            showEndDatePicker = showEndDatePicker,
            endDateMillis = endDateMillis,
            onEndDateSelected = {
                endDateMillis = it
                showEndDatePicker = false
            },
            onDismissEndDatePicker = { showEndDatePicker = false },
            showAddRewardDialog = showAddRewardDialog,
            onAddReward = { newReward ->
                milestoneRewards = milestoneRewards + newReward
                showAddRewardDialog = false
            },
            onDismissRewardDialog = { showAddRewardDialog = false },
            activeExplanation = activeExplanation,
            onDismissExplanation = { activeExplanation = null },
            showAddCustomDialog = showAddCustomDialog,
            reminderToEditTime = reminderToEditTime,
            onConfirmReminder = { hour, min ->
                val formatted = String.format(Locale.US, "%02d:%02d", hour, min)
                if (reminderToEditTime != null) {
                    remindersList = (remindersList.filter { it != reminderToEditTime } + formatted).distinct().sorted()
                } else {
                    if (formatted !in remindersList) {
                        remindersList = (remindersList + formatted).sorted()
                    }
                }
                reminderToEditTime = null
                showAddCustomDialog = false
                reminderEnabled = true
            },
            onDismissReminderDialog = {
                showAddCustomDialog = false
                reminderToEditTime = null
                if (remindersList.isEmpty()) {
                    reminderEnabled = false
                }
            },
            language = language
        )

        // Floating Header Overlay with vertical gradient fade-out
        CreateHabitHeaderOverlay(
            isEditing = editingHabit != null,
            language = language,
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
