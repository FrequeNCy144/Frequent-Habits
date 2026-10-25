package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.data.MilestoneReward
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.screens.createhabit.*
import com.example.ui.theme.*
import java.util.*
import kotlinx.coroutines.launch

private val intSetSaver = Saver<Set<Int>, ArrayList<Int>>(
    save = { ArrayList(it) },
    restore = { it.toSet() }
)

private val stringListSaver = Saver<List<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toList() }
)

private val milestoneRewardSaver = Saver<List<MilestoneReward>, ArrayList<MilestoneReward>>(
    save = { ArrayList(it) },
    restore = { it.toList() }
)

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

    var name by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.name ?: "") }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.description ?: "") }

    var isNegative by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.isNegative ?: false) }
    var selectedColor by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.color ?: "purple") }
    var selectedIcon by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.icon ?: "fitness_center") }
    var iconSearchQuery by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.category ?: "Sonstiges") }
    var selectedFolderId by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf<Int?>(editingHabit?.folderId) }

    var type by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.type ?: "BINARY") }
    var unit by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(editingHabit?.unit ?: "") }
    var selectedChip by rememberSaveable(inputs = arrayOf(editingHabit?.id)) { mutableStateOf(if (editingHabit?.type == "NUMBER") editingHabit.unit else "") }

    var targetValueStr by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.targetValue > 0f) {
                if (editingHabit.targetValue % 1f == 0f) editingHabit.targetValue.toInt().toString()
                else editingHabit.targetValue.toString()
            } else "1"
        )
    }

    var clickIncrementStr by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.clickIncrement > 0f) {
                if (editingHabit.clickIncrement % 1f == 0f) editingHabit.clickIncrement.toInt().toString()
                else editingHabit.clickIncrement.toString()
            } else "1"
        )
    }

    var frequency by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        val initialFreq = when (editingHabit?.frequency) {
            "EVERYDAY", null -> "DAILY"
            else -> editingHabit.frequency
        }
        mutableStateOf(initialFreq)
    }
    var specificDaysSet by rememberSaveable(inputs = arrayOf(editingHabit?.id), stateSaver = intSetSaver) {
        val rawDays = editingHabit?.specificDays
        val days = if (rawDays != null) {
            rawDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else {
            null
        }
        mutableStateOf(if (!days.isNullOrEmpty()) days else setOf(1, 2, 3, 4, 5))
    }
    var timesWeekly by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        val num = if (editingHabit?.frequency == "TIMES_WEEKLY") editingHabit.specificDays.toIntOrNull() ?: 3 else 3
        mutableIntStateOf(num)
    }
    var targetDaysPerMonth by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        val num = if (editingHabit?.frequency == "TIMES_MONTHLY") editingHabit.specificDays.toIntOrNull() ?: 15 else 15
        mutableIntStateOf(num)
    }
    var intervalDays by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        val num = if (editingHabit?.frequency == "INTERVAL") editingHabit.specificDays.toIntOrNull() ?: 2 else 2
        mutableIntStateOf(num)
    }

    var startDateMillis by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableLongStateOf(editingHabit?.startDate ?: System.currentTimeMillis())
    }
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }

    var endDateMillis by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf<Long?>(null)
    }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    var reminderEnabled by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.reminderEnabled ?: false)
    }
    var remindersList by rememberSaveable(inputs = arrayOf(editingHabit?.id), stateSaver = stringListSaver) {
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
    var showAddCustomDialog by rememberSaveable { mutableStateOf(false) }
    var reminderToEditTime by rememberSaveable { mutableStateOf<String?>(null) }

    var minimalViableValueStr by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.minimalViableValue != null) {
                if (editingHabit.minimalViableValue % 1f == 0f) editingHabit.minimalViableValue.toInt().toString()
                else editingHabit.minimalViableValue.toString()
            } else ""
        )
    }
    var minimalViableText by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.minimalViableText ?: "")
    }

    var placeholderWhy by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.why ?: "")
    }

    var stackedOnHabitId by rememberSaveable(inputs = arrayOf(editingHabit?.id, initialStackedHabitId)) {
        mutableStateOf(editingHabit?.stackedOnHabitId ?: initialStackedHabitId)
    }

    var isFinishable by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.isFinishable ?: false)
    }
    var difficulty by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.difficulty ?: "MEDIUM")
    }
    var challengeMode by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.challengeMode ?: "ACCUMULATE")
    }
    var challengeSuccessRate by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(editingHabit?.challengeSuccessRate ?: 90)
    }
    var totalTargetValueStr by rememberSaveable(inputs = arrayOf(editingHabit?.id)) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.totalTargetValue != null) {
                if (editingHabit.totalTargetValue % 1f == 0f) editingHabit.totalTargetValue.toInt().toString()
                else editingHabit.totalTargetValue.toString()
            } else ""
        )
    }

    var milestoneRewards by rememberSaveable(inputs = arrayOf(editingHabit?.id), stateSaver = milestoneRewardSaver) {
        mutableStateOf(if (initialMilestoneRewards.isNotEmpty()) initialMilestoneRewards else emptyList<MilestoneReward>())
    }
    var showAddRewardDialog by rememberSaveable { mutableStateOf(false) }

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

    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    val performSave = {
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
            if (currentStep != 1 && currentStep != 0) {
                currentStep = 1
            }
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

    val onStepSelected = { step: Int ->
        if (step != currentStep) {
            currentStep = step
            coroutineScope.launch {
                scrollState.scrollTo(0)
            }
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
                .padding(start = 16.dp, end = 16.dp, top = 164.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val renderBasisCards = @Composable {
                // 1. Basic Info Card (Name & Beschreibung)
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

                // 2. Appearance Card (Icon & Farbe)
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

                // 3. Folder Selection Card (Ordner / Lebensbereich)
                HabitFolderSelectionCard(
                    allFolders = allFolders,
                    selectedFolderId = selectedFolderId,
                    onFolderSelected = { selectedFolderId = it },
                    language = language,
                    accentColor = activeColor,
                    onCreateFolder = onCreateFolder
                )
            }

            val renderZielCards = @Composable {
                CreateHabitStepZiel(
                    language = language,
                    activeColor = activeColor,
                    type = type,
                    onTypeChange = { type = it },
                    targetValueStr = targetValueStr,
                    onTargetValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                            targetValueStr = input
                        }
                    },
                    clickIncrementStr = clickIncrementStr,
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
                    isFinishable = isFinishable,
                    onFinishableChange = { isFinishable = it },
                    totalTargetValueStr = totalTargetValueStr,
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
                    frequency = frequency,
                    onFrequencyChange = { frequency = it },
                    timesWeekly = timesWeekly,
                    onTimesWeeklyChange = { timesWeekly = it },
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
                    startDateStr = startDateStr,
                    onStartDateClick = { showStartDatePicker = true },
                    onShowInfoDialog = { title, explanation ->
                        activeExplanation = title to explanation
                    }
                )
            }

            val renderDetailsCards = @Composable {
                // Reminders Card (Erinnerungen & Benachrichtigungen)
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

                // Habit Stacking Card (Anker-Routine)
                HabitStackingCard(
                    language = language,
                    selectedStackedHabit = activeHabits.find { it.id == stackedOnHabitId },
                    onSelectStackedHabit = { habit ->
                        stackedOnHabitId = habit?.id
                    },
                    activeHabits = activeHabits.filter { it.id != (editingHabit?.id ?: -1) },
                    allFolders = allFolders,
                    accentColor = activeColor,
                    onShowInfoDialog = { title, explanation ->
                        activeExplanation = title to explanation
                    }
                )

                // Motivation Card (Dein „Warum“ - Psychologischer Anker)
                HabitMotivationCard(
                    language = language,
                    motivationWhy = placeholderWhy,
                    onMotivationWhyChange = { placeholderWhy = it },
                    accentColor = activeColor,
                    onShowInfoDialog = { title, explanation ->
                        activeExplanation = title to explanation
                    }
                )

                // Milestone Rewards Card (Erfolge & Meilensteine)
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

                // Minimal Viable Version (MVV / 2-Minuten-Regel für stressige Tage)
                HabitMinimalViableCard(
                    language = language,
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
                    accentColor = activeColor,
                    onShowInfoDialog = { title, explanation ->
                        activeExplanation = title to explanation
                    }
                )
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut())
                    }
                },
                label = "wizard_step_transition"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (step) {
                        1 -> renderBasisCards()
                        2 -> renderZielCards()
                        else -> renderDetailsCards()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons (Back / Next) - Habit saving is exclusively via the checkmark (Haken) at the top!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Cancel + Next: Ziel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("cancel_habit_button"),
                                border = BorderStroke(1.dp, AppBorder),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(
                                    text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = {
                                    if (name.isBlank()) {
                                        performSave() // Triggers error validation & focus on name
                                    } else {
                                        currentStep = 2
                                        coroutineScope.launch { scrollState.scrollTo(0) }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("step_next_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = tr(language, "Weiter: Ziel →", "შემდეგი: მიზანი →", "下一步：目标 →", "Next: Target →"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    2 -> {
                        // Step 2: Back to Step 1 + Next: Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = 1
                                    coroutineScope.launch { scrollState.scrollTo(0) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("step_back_button"),
                                border = BorderStroke(1.dp, AppBorder),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(
                                    text = tr(language, "Zurück", "უკან", "上一步", "Back"),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = {
                                    currentStep = 3
                                    coroutineScope.launch { scrollState.scrollTo(0) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("step_next_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = tr(language, "Weiter: Details →", "შემდეგი: დეტალები →", "下一步：详情 →", "Next: Details →"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    else -> {
                        // Step 3 (Details): Back to Step 2 + Create / Save button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = 2
                                    coroutineScope.launch { scrollState.scrollTo(0) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("step_back_button"),
                                border = BorderStroke(1.dp, AppBorder),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(
                                    text = tr(language, "← Zurück", "← უკან", "← 上一步", "← Back"),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = { performSave() },
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("step_create_habit_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (editingHabit != null) {
                                        tr(language, "Speichern", "შენახვა", "保存", "Save")
                                    } else {
                                        tr(language, "Erstellen", "შექმნა", "创建", "Create")
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
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

        // Sticky Header Overlay with pinned Zeitstrahl (stepper) and save checkmark (Haken oben)
        CreateHabitHeaderOverlay(
            isEditing = editingHabit != null,
            language = language,
            onDismiss = onDismiss,
            accentColor = activeColor,
            onSaveClick = { performSave() },
            modifier = Modifier.align(Alignment.TopCenter),
            stepperContent = {
                HabitWizardStepper(
                    currentStep = currentStep,
                    activeColor = activeColor,
                    language = language,
                    onStepSelected = onStepSelected
                )
            }
        )
    }
}

@Composable
private fun CreateHabitSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.15f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

