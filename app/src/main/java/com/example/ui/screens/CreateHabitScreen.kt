package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.MilestoneReward
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.createhabit.*
import com.example.ui.components.ModernBackButton
import com.example.ui.theme.*
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateHabitScreen(
    editingHabit: Habit? = null,
    initialMilestoneRewards: List<MilestoneReward> = emptyList(),
    initialStackedHabitId: Int? = null,
    activeHabits: List<Habit> = emptyList(),
    language: String,
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
        difficulty: String
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember(editingHabit?.id) { mutableStateOf(editingHabit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var isNegative by remember(editingHabit?.id) { mutableStateOf(editingHabit?.isNegative ?: false) }
    var category by remember(editingHabit?.id) { mutableStateOf(editingHabit?.category ?: "Gesundheit") }
    var selectedIcon by remember(editingHabit?.id) { mutableStateOf(editingHabit?.icon ?: "fitness_center") }
    var iconSearchQuery by remember { mutableStateOf("") }
    var selectedColor by remember(editingHabit?.id) { mutableStateOf(editingHabit?.color ?: "HabitViolet") }
    var type by remember(editingHabit?.id) { mutableStateOf(editingHabit?.type ?: "BINARY") }
    var unit by remember(editingHabit?.id) { mutableStateOf(editingHabit?.unit ?: "") }

    var selectedChip by remember(editingHabit?.id, language) {
        val standardUnitsDe = listOf("Minuten", "Liter", "ml", "km", "Stunden", "Mal")
        val standardUnitsEn = listOf("Minutes", "Liters", "ml", "km", "Hours", "Times")
        val initialChip = when {
            unit.isEmpty() -> tr(language, "Anderes...", "მორგებული...", "自定义...", "Custom...")
            language == "de" && unit in standardUnitsDe -> unit
            language != "de" && unit in standardUnitsEn -> unit
            unit.lowercase() in listOf("minuten", "minutes", "min") -> tr(language, "Minuten", "წუთები", "分钟", "Minutes")
            unit.lowercase() in listOf("liter", "liters", "l") -> tr(language, "Liter", "ლიტრი", "升", "Liters")
            unit.lowercase() == "ml" -> "ml"
            unit.lowercase() == "km" -> "km"
            unit.lowercase() in listOf("stunden", "hours", "h") -> tr(language, "Stunden", "საათები", "小时", "Hours")
            unit.lowercase() in listOf("mal", "times") -> tr(language, "Mal", "ჯერ", "次", "Times")
            else -> tr(language, "Anderes...", "მორგებული...", "自定义...", "Custom...")
        }
        mutableStateOf(initialChip)
    }

    var targetValueStr by remember(editingHabit?.id) { 
        mutableStateOf(editingHabit?.let { if (it.targetValue % 1f == 0f) it.targetValue.toInt().toString() else it.targetValue.toString() } ?: "1") 
    }
    var clickIncrementStr by remember(editingHabit?.id) {
        mutableStateOf(editingHabit?.let { if (it.clickIncrement % 1f == 0f) it.clickIncrement.toInt().toString() else it.clickIncrement.toString() } ?: "1")
    }

    var frequency by remember(editingHabit?.id) { mutableStateOf(editingHabit?.frequency ?: "DAILY") }
    var timesWeekly by remember(editingHabit?.id) {
        val initialTimes = if (editingHabit?.frequency == "TIMES_WEEKLY") {
            editingHabit.specificDays.toIntOrNull() ?: 3
        } else 3
        mutableStateOf(initialTimes)
    }
    var targetDaysPerMonth by remember(editingHabit?.id) {
        val initialTimes = if (editingHabit?.frequency == "TIMES_MONTHLY") {
            editingHabit.specificDays.toIntOrNull() ?: 10
        } else 10
        mutableStateOf(initialTimes)
    }
    var intervalDays by remember(editingHabit?.id) {
        val initialInterval = if (editingHabit?.frequency == "INTERVAL") {
            editingHabit.specificDays.toIntOrNull() ?: 2
        } else 2
        mutableStateOf(initialInterval)
    }

    var specificDaysSet by remember(editingHabit?.id) {
        val initial = if (editingHabit?.frequency == "SPECIFIC" && !editingHabit.specificDays.isNullOrEmpty()) {
            editingHabit.specificDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else {
            setOf(1, 2, 3, 4, 5, 6, 7)
        }
        mutableStateOf(initial)
    }

    var startDateMillis by remember(editingHabit?.id) {
        mutableStateOf(
            if (editingHabit != null && editingHabit.startDate > 0) editingHabit.startDate
            else System.currentTimeMillis()
        )
    }
    var showStartDatePicker by remember { mutableStateOf(false) }

    var endDateMillis by remember(editingHabit?.id) {
        mutableStateOf<Long?>(null)
    }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var reminderEnabled by remember(editingHabit?.id) { mutableStateOf(editingHabit?.reminderEnabled ?: false) }
    var remindersList by remember(editingHabit?.id) {
        val initialList = mutableListOf<String>()
        if (editingHabit != null && editingHabit.reminderEnabled) {
            val primary = String.format(Locale.US, "%02d:%02d", editingHabit.reminderHour, editingHabit.reminderMinute)
            initialList.add(primary)
            if (!editingHabit.customReminders.isNullOrEmpty()) {
                val extras = editingHabit.customReminders.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                initialList.addAll(extras)
            }
        }
        mutableStateOf(initialList.distinct().sorted())
    }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var reminderToEditTime by remember { mutableStateOf<String?>(null) }

    var description by remember(editingHabit?.id) { mutableStateOf(editingHabit?.description ?: "") }
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

    // Auto-focus habit name textfield and open software keyboard when creating a habit
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(
                        text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                val buttonText = if (editingHabit != null) {
                    tr(language, "Speichern", "შენახვა", "保存", "Save")
                } else {
                    tr(language, "Erstellen", "შექმნა", "创建", "Create")
                }
                Button(
                    onClick = {
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
                                isFinishable, totalTargetVal, if (isFinishable) difficulty else "MEDIUM"
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("save_habit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        if (showStartDatePicker) {
            val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
            SimpleDatePickerDialog(
                initialYear = cal.get(Calendar.YEAR),
                initialMonth = cal.get(Calendar.MONTH),
                initialDay = cal.get(Calendar.DAY_OF_MONTH),
                onDateSelected = { year, month, day ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                    }
                    startDateMillis = selected.timeInMillis
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }

        if (showEndDatePicker) {
            val cal = Calendar.getInstance().apply { 
                timeInMillis = endDateMillis ?: (startDateMillis + 30L * 86400000L)
            }
            SimpleDatePickerDialog(
                initialYear = cal.get(Calendar.YEAR),
                initialMonth = cal.get(Calendar.MONTH),
                initialDay = cal.get(Calendar.DAY_OF_MONTH),
                onDateSelected = { year, month, day ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, day, 23, 59, 59)
                    }
                    endDateMillis = selected.timeInMillis
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }

        if (showAddRewardDialog) {
            AddMilestoneRewardDialog(
                language = language,
                onDismiss = { showAddRewardDialog = false },
                onAdd = { newReward ->
                    milestoneRewards = milestoneRewards + newReward
                    showAddRewardDialog = false
                }
            )
        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }

        if (showAddCustomDialog) {
            AddCustomReminderDialog(
                language = language,
                initialTime = reminderToEditTime,
                onDismiss = {
                    showAddCustomDialog = false
                    reminderToEditTime = null
                    if (remindersList.isEmpty()) {
                        reminderEnabled = false
                    }
                },
                onConfirm = { hour, min ->
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
                }
            )
        }

        // Floating Header Overlay with vertical gradient fade-out
        CreateHabitHeaderOverlay(
            isEditing = editingHabit != null,
            language = language,
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
