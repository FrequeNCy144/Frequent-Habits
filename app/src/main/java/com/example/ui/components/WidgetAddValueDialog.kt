package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.FocusTimerManager
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.ui.*
import com.example.ui.components.dialogs.addvalue.CircularTimerGauge
import com.example.ui.components.dialogs.addvalue.WidgetAddValueActionButtons
import com.example.ui.components.dialogs.addvalue.WidgetAddValueManualSection
import com.example.ui.components.dialogs.addvalue.WidgetTimerAudioPicker
import com.example.ui.components.dialogs.addvalue.WidgetTimerControlCard
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetAddValueDialog(
    habitId: Int,
    viewModel: HabitsViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()

    var habitToLog by remember { mutableStateOf<Habit?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(habitId) {
        try {
            val db = AppDatabase.getDatabase(context)
            val habit = db.habitDao().getHabitByIdSuspend(habitId)
            habitToLog = habit
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoaded = true
        }
    }

    if (!isLoaded) {
        // Loading state
    } else if (habitToLog != null) {
        val habit = habitToLog!!
        val existingLog = logs.find { it.habitId == habit.id && it.date == selectedDate }
        val currentValue = existingLog?.value ?: 0f

        val unitLower = habit.unit.lowercase().trim()
        val isMinutesUnit = unitLower in listOf("minuten", "minutes", "min", "minute", "m")
        val isHoursUnit = unitLower in listOf("stunden", "hours", "std", "hour", "h", "stunde")
        val isSecondsUnit = unitLower in listOf("sekunden", "seconds", "sec", "s")
        val isTimeUnit = isMinutesUnit || isHoursUnit || isSecondsUnit
        val hasTimer = (habit.type == "NUMBER" || habit.type == "NUMERICAL") && isTimeUnit

        val defaultMinutes = remember(habit) {
            val target = habit.targetValue
            if (target > 0f) target.toInt().coerceAtLeast(1) else 10
        }
        val defaultSeconds = defaultMinutes * 60

        val globalActiveHabitId by FocusTimerManager.activeHabitId.collectAsState()
        val isCurrentHabitInTimer = globalActiveHabitId == habitId.toLong()
        val globalIsRunning by FocusTimerManager.isRunning.collectAsState()
        val globalSecondsRemaining by FocusTimerManager.secondsRemaining.collectAsState()
        val globalInitialSeconds by FocusTimerManager.initialSeconds.collectAsState()

        var timerDurationSeconds by remember(habitId) { mutableIntStateOf(defaultSeconds) }
        var manualInputValue by remember(habitId, currentValue) {
            val initVal = if (currentValue > 0f) {
                if (currentValue % 1f == 0f) currentValue.toInt().toString() else currentValue.toString()
            } else {
                val inc = habit.clickIncrement
                if (inc > 0f) {
                    if (inc % 1f == 0f) inc.toInt().toString() else inc.toString()
                } else "1"
            }
            mutableStateOf(initVal)
        }

        val isTimerRunning = isCurrentHabitInTimer && globalIsRunning
        val timerSecondsRemaining = if (isCurrentHabitInTimer) globalSecondsRemaining else timerDurationSeconds
        val initialTimerSeconds = if (isCurrentHabitInTimer && globalInitialSeconds > 0) globalInitialSeconds else timerDurationSeconds

        var importedAudios by remember { mutableStateOf<List<File>>(emptyList()) }
        var selectedAudioFile by remember { mutableStateOf<File?>(null) }

        val refreshAudios = remember(context) {
            {
                val dir = File(context.filesDir, "audios")
                if (!dir.exists()) dir.mkdirs()
                val files = dir.listFiles()?.filter {
                    it.isFile && (it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "aac"))
                }?.sortedBy { it.name } ?: emptyList()
                importedAudios = files

                val lastSaved = AudioSoundscapeManager.getLastSelectedAudio(context)
                if (selectedAudioFile == null && lastSaved.isNotEmpty()) {
                    val matching = files.find { it.name == lastSaved }
                    if (matching != null) {
                        selectedAudioFile = matching
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            refreshAudios()
        }

        val audioPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val contentResolver = context.contentResolver
                    var fileName = "imported_audio_${System.currentTimeMillis()}.mp3"
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val dispName = cursor.getString(nameIndex)
                            if (!dispName.isNullOrEmpty()) {
                                fileName = dispName
                            }
                        }
                    }
                    val destDir = File(context.filesDir, "audios")
                    if (!destDir.exists()) destDir.mkdirs()
                    val destFile = File(destDir, fileName)
                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    refreshAudios()
                    selectedAudioFile = destFile
                    AudioSoundscapeManager.setLastSelectedAudio(context, destFile.name)
                    FocusTimerManager.updateAudio(context, destFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF14151E),
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFF3E4050), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Title: "Log: <HabitName>"
                Text(
                    text = "Log: ${habit.name}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                if (hasTimer) {
                    val hasActiveSession = isCurrentHabitInTimer && initialTimerSeconds > 0

                    // 1. CIRCULAR TIMER DIAL
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularTimerGauge(
                            secondsRemaining = timerSecondsRemaining,
                            initialSeconds = initialTimerSeconds,
                            isTimerRunning = isTimerRunning,
                            hasActiveSession = hasActiveSession,
                            language = language,
                            onSecondsSelected = { newSecs ->
                                if (!isTimerRunning && !hasActiveSession) {
                                    timerDurationSeconds = newSecs
                                }
                            }
                        )
                    }

                    // 2. MEDIA CONTROLS CAPSULE
                    WidgetTimerControlCard(
                        context = context,
                        habit = habit,
                        selectedDate = selectedDate,
                        currentValue = currentValue,
                        selectedAudioFile = selectedAudioFile,
                        isCurrentHabitInTimer = isCurrentHabitInTimer,
                        globalIsRunning = globalIsRunning,
                        globalSecondsRemaining = globalSecondsRemaining,
                        initialTimerSeconds = initialTimerSeconds,
                        timerSecondsRemaining = timerSecondsRemaining,
                        timerDurationSeconds = timerDurationSeconds,
                        defaultMinutes = defaultMinutes,
                        manualInputValue = manualInputValue,
                        viewModel = viewModel,
                        onResetTimerDuration = { timerDurationSeconds = it },
                        onDismiss = onDismiss
                    )

                    // 3. BACKGROUND AUDIO SECTION
                    WidgetTimerAudioPicker(
                        context = context,
                        language = language,
                        importedAudios = importedAudios,
                        selectedAudioFile = selectedAudioFile,
                        onSelectAudio = { selectedAudioFile = it },
                        onLaunchAudioPicker = { audioPickerLauncher.launch("audio/*") }
                    )
                }

                // 4. MANUAL LOG SECTION
                WidgetAddValueManualSection(
                    habit = habit,
                    language = language,
                    manualInputValue = manualInputValue,
                    onValueChange = { manualInputValue = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 5. BOTTOM ACTION BUTTONS
                WidgetAddValueActionButtons(
                    context = context,
                    habit = habit,
                    selectedDate = selectedDate,
                    manualInputValue = manualInputValue,
                    timerDurationSeconds = timerDurationSeconds,
                    isCurrentHabitInTimer = isCurrentHabitInTimer,
                    viewModel = viewModel,
                    language = language,
                    onDismiss = onDismiss
                )
            }
        }
    } else {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }
}
