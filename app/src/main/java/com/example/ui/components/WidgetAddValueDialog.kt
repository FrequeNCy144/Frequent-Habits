package com.example.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.FocusTimerManager
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.logNumericalHabit
import com.example.ui.theme.*
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

        val isMinutesUnit = habit.unit.lowercase() in listOf("minuten", "minutes", "min", "minute", "m")
        val hasTimer = habit.type == "NUMBER" || habit.type == "NUMERICAL" || (habit.targetValue > 1f && habit.type != "BINARY") || isMinutesUnit

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
        var isAudioDropdownOpen by remember { mutableStateOf(false) }
        var audioSearchQuery by remember { mutableStateOf("") }

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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF1B1C28),
                            border = BorderStroke(1.dp, Color(0xFF2E3042)),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // Reset / Restart Button
                                IconButton(
                                    onClick = {
                                        if (isCurrentHabitInTimer) {
                                            FocusTimerManager.stopTimer(context, logProgress = false)
                                        }
                                        val mins = manualInputValue.toIntOrNull() ?: defaultMinutes
                                        timerDurationSeconds = mins * 60
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF262838), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Timer",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Center Play / Pause Button
                                IconButton(
                                    onClick = {
                                        if (isCurrentHabitInTimer) {
                                            if (globalIsRunning) {
                                                FocusTimerManager.pauseTimer(context)
                                            } else {
                                                if (globalSecondsRemaining <= 0) {
                                                    FocusTimerManager.startTimer(
                                                        context = context,
                                                        habitId = habit.id.toLong(),
                                                        habitName = habit.name,
                                                        habitColor = habit.color,
                                                        durationMinutes = timerDurationSeconds / 60f,
                                                        selectedDate = selectedDate,
                                                        currentValue = currentValue,
                                                        audioFile = selectedAudioFile,
                                                        totalSecondsOverride = timerDurationSeconds
                                                    )
                                                } else {
                                                    FocusTimerManager.resumeTimer(context)
                                                }
                                            }
                                        } else {
                                            FocusTimerManager.startTimer(
                                                context = context,
                                                habitId = habit.id.toLong(),
                                                habitName = habit.name,
                                                habitColor = habit.color,
                                                durationMinutes = timerDurationSeconds / 60f,
                                                selectedDate = selectedDate,
                                                currentValue = currentValue,
                                                audioFile = selectedAudioFile,
                                                totalSecondsOverride = timerDurationSeconds
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(PrimaryViolet, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Done / Complete Button
                                IconButton(
                                    onClick = {
                                        val loggedMins = if (isCurrentHabitInTimer && initialTimerSeconds > timerSecondsRemaining) {
                                            ((initialTimerSeconds - timerSecondsRemaining + 59) / 60).coerceAtLeast(1)
                                        } else {
                                            (timerDurationSeconds / 60).coerceAtLeast(1)
                                        }
                                        if (isCurrentHabitInTimer) {
                                            FocusTimerManager.stopTimer(context, logProgress = false)
                                        }
                                        viewModel.logNumericalHabit(habit.id, selectedDate, currentValue + loggedMins)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF262838), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete Session",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. BACKGROUND AUDIO SECTION
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr(language, "Hintergrund-Audio", "ფონური აუდიო", "背景白噪音", "Background Audio"),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextSecondary
                        )

                        // Active Selected Audio Card
                        if (selectedAudioFile != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E1A33),
                                border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedAudioFile?.nameWithoutExtension ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tr(language, "Aktiver Timer-Sound", "აქტიური ტაიმერის ხმა", "当前计时器背景音", "Active Timer Sound"),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp
                                            ),
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedAudioFile = null
                                            AudioSoundscapeManager.setLastSelectedAudio(context, "")
                                            FocusTimerManager.updateAudio(context, null)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove audio",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Search & Add Sound Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Dropdown trigger box
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    onClick = { isAudioDropdownOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF181924),
                                    border = BorderStroke(1.dp, Color(0xFF2A2B3A)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (selectedAudioFile == null) {
                                                    tr(language, "Sound suchen...", "ხმის ძებნა...", "搜索背景音...", "Search sound...")
                                                } else {
                                                    selectedAudioFile!!.nameWithoutExtension
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = isAudioDropdownOpen,
                                    onDismissRequest = { isAudioDropdownOpen = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1E1F2C))
                                        .border(1.dp, Color(0xFF33354A), RoundedCornerShape(8.dp))
                                ) {
                                    if (importedAudios.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = tr(language, "Keine Sounds importiert (+ tippen)", "No sounds imported (tap +)"),
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                isAudioDropdownOpen = false
                                                audioPickerLauncher.launch("audio/*")
                                            }
                                        )
                                    } else {
                                        importedAudios.forEach { file ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = file.nameWithoutExtension,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        tint = PrimaryViolet,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                onClick = {
                                                    selectedAudioFile = file
                                                    AudioSoundscapeManager.setLastSelectedAudio(context, file.name)
                                                    FocusTimerManager.updateAudio(context, file)
                                                    isAudioDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Add Button (+)
                            Surface(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryViolet,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add audio",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. MANUAL LOG SECTION
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr(
                            language,
                            "Manuell eintragen (${habit.unit}):",
                            "ხელით შეყვანა (${habit.unit}):",
                            "手动记录 (${habit.unit}):",
                            "Manual log (${habit.unit}):"
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Input box
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(46.dp)
                                .background(Color(0xFF181924), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2E3042), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            var isManualFocused by remember { mutableStateOf(false) }
                            BasicTextField(
                                value = manualInputValue,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                                        manualInputValue = input
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                ),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(PrimaryViolet),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("widget_value_input")
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused && !isManualFocused) {
                                            isManualFocused = true
                                            manualInputValue = ""
                                        } else if (!focusState.isFocused) {
                                            isManualFocused = false
                                        }
                                    }
                            )
                        }

                        // Presets
                        val presets = if (habit.clickIncrement > 0f && habit.clickIncrement != 1f) {
                            val inc = habit.clickIncrement.toInt()
                            listOf(1, inc, inc * 2, inc * 5)
                        } else {
                            listOf(1, 5, 10, 15)
                        }
                        presets.forEach { preset ->
                            Surface(
                                onClick = {
                                    manualInputValue = preset.toString()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1B1C28),
                                border = BorderStroke(1.dp, Color(0xFF2E3042)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. BOTTOM ACTION BUTTONS
                // Row with Reset & Failed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = {
                            if (isCurrentHabitInTimer) {
                                FocusTimerManager.stopTimer(context, logProgress = false)
                            }
                            viewModel.logNumericalHabit(habit.id, selectedDate, 0f)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1B1C28)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF4A3830))
                    ) {
                        Text(
                            text = tr(language, "Reset", "გადატვირთვა", "重置", "Reset"),
                            color = Color(0xFFFFA726),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Failed Button
                    OutlinedButton(
                        onClick = {
                            if (isCurrentHabitInTimer) {
                                FocusTimerManager.stopTimer(context, logProgress = false)
                            }
                            viewModel.logNumericalHabit(habit.id, selectedDate, -1f)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1B1C28)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = tr(language, "Failed", "ჩაიშალა", "失败", "Failed"),
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Full-width Save Button
                Button(
                    onClick = {
                        val fValue = manualInputValue.replace(',', '.').toFloatOrNull() ?: (timerDurationSeconds / 60f)
                        if (isCurrentHabitInTimer) {
                            FocusTimerManager.stopTimer(context, logProgress = false)
                        }
                        if (fValue <= 0f) {
                            viewModel.logNumericalHabit(habit.id, selectedDate, 0f)
                        } else {
                            viewModel.logNumericalHabit(habit.id, selectedDate, fValue)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("widget_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Text(
                        text = tr(language, "Save", "შენახვა", "保存", "Save"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }
}

private fun parseTimeToSeconds(input: String): Int {
    val clean = input.trim()
    if (clean.isEmpty()) return 0
    if (clean.contains(":")) {
        val parts = clean.split(":").map { it.toIntOrNull() ?: 0 }
        return when (parts.size) {
            1 -> parts[0] * 60
            2 -> (parts[0] * 60) + parts[1]
            3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
            else -> 0
        }
    }
    val digits = clean.filter { it.isDigit() }
    if (digits.isEmpty()) return 0
    val num = digits.toIntOrNull() ?: return 0
    return when (digits.length) {
        1, 2 -> num * 60
        3 -> {
            val m = digits.substring(0, 1).toInt()
            val s = digits.substring(1).toInt()
            (m * 60) + s
        }
        4 -> {
            val m = digits.substring(0, 2).toInt()
            val s = digits.substring(2).toInt()
            (m * 60) + s
        }
        5, 6 -> {
            val h = digits.dropLast(4).toInt()
            val m = digits.dropLast(2).takeLast(2).toInt()
            val s = digits.takeLast(2).toInt()
            (h * 3600) + (m * 60) + s
        }
        else -> num * 60
    }
}

private fun formatDurationHMS(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }
}

/**
 * Custom Circular Timer Gauge matching the reference screenshot:
 * - Outer bold vibrant purple ring
 * - Draggable white indicator dot at current progress angle
 * - Dark inner clock face with radial ticks
 * - Interactive direct time input field in center
 */
@Composable
fun CircularTimerGauge(
    secondsRemaining: Int,
    initialSeconds: Int,
    isTimerRunning: Boolean,
    hasActiveSession: Boolean = false,
    language: String = "de",
    onSecondsSelected: (Int) -> Unit
) {
    val totalTargetSeconds = if (initialSeconds > 0) initialSeconds else 900
    val sweepFraction = if (hasActiveSession || isTimerRunning) {
        if (totalTargetSeconds > 0) (secondsRemaining.toFloat() / totalTargetSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    } else {
        1.0f
    }

    // Soft, gentle "wabend" ambient glow in the center behind the digits
    val infiniteTransition = rememberInfiniteTransition(label = "timer_wabend_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isInputFocused by remember { mutableStateOf(false) }

    var textValue by remember {
        val formatted = formatDurationHMS(secondsRemaining)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    LaunchedEffect(secondsRemaining, isTimerRunning, hasActiveSession) {
        if (!isInputFocused) {
            val formatted = formatDurationHMS(secondsRemaining)
            textValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }
    }

    Box(
        modifier = Modifier
            .size(220.dp)
            .pointerInput(isTimerRunning, hasActiveSession) {
                if (!isTimerRunning && !hasActiveSession) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touch = change.position
                        val dx = touch.x - center.x
                        val dy = touch.y - center.y
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        angleDeg = (angleDeg + 90f + 360f) % 360f
                        val selectedMinutes = ((angleDeg / 360f) * 60f).toInt().coerceIn(1, 180)
                        val newSecs = selectedMinutes * 60
                        textValue = TextFieldValue(text = formatDurationHMS(newSecs))
                        onSecondsSelected(newSecs)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) - 12.dp.toPx()
            val innerRadius = outerRadius - 14.dp.toPx()

            // 1. Draw inner dark background circle
            drawCircle(
                color = Color(0xFF13141E),
                radius = innerRadius,
                center = center
            )

            // 2. Atmospheric "wabend" ambient glow in the center behind the digits
            if (isTimerRunning || hasActiveSession) {
                val currentAlpha = if (isTimerRunning) glowAlpha else 0.14f
                val currentScale = if (isTimerRunning) glowScale else 0.55f
                val currentGlowRadius = innerRadius * currentScale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryViolet.copy(alpha = currentAlpha * 0.85f),
                            PrimaryViolet.copy(alpha = currentAlpha * 0.40f),
                            PrimaryViolet.copy(alpha = currentAlpha * 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentGlowRadius
                    ),
                    radius = currentGlowRadius,
                    center = center
                )
            }

            // 3. Draw 60 clock dial tick marks
            for (i in 0 until 60) {
                val tickAngleDeg = (i * 6f) - 90f
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
                val isMajor = i % 5 == 0
                val tickLength = if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                val tickColor = if (isMajor) PrimaryViolet.copy(alpha = 0.8f) else Color(0xFF424458)
                val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                val startR = innerRadius - 4.dp.toPx()
                val endR = startR - tickLength

                val startX = (center.x + startR * cos(tickAngleRad)).toFloat()
                val startY = (center.y + startR * sin(tickAngleRad)).toFloat()
                val endX = (center.x + endR * cos(tickAngleRad)).toFloat()
                val endY = (center.y + endR * sin(tickAngleRad)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // 4. Draw background ring track
            drawCircle(
                color = Color(0xFF232438),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 9.dp.toPx())
            )

            // 5. Draw active purple arc (clean and crisp, without outer harsh pulse)
            val sweepAngle = sweepFraction * 360f
            if (sweepAngle > 0f) {
                // Main crisp arc
                drawArc(
                    color = PrimaryViolet,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                )

                // 6. Draw indicator knob at the end of the arc
                val knobAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                val knobX = (center.x + outerRadius * cos(knobAngleRad)).toFloat()
                val knobY = (center.y + outerRadius * sin(knobAngleRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(knobX, knobY)
                )
                drawCircle(
                    color = Color(0xFF13141E),
                    radius = 2.5.dp.toPx(),
                    center = Offset(knobX, knobY)
                )
            }
        }

        // Center Time Display / Editable Field
        if (isTimerRunning || hasActiveSession) {
            val mins = (secondsRemaining / 60) % 60
            val secs = secondsRemaining % 60
            val hours = secondsRemaining / 3600
            val displayRunning = if (hours > 0) {
                String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, mins, secs)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayRunning,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                if (!isTimerRunning && hasActiveSession) {
                    Text(
                        text = tr(language, "Pausiert", "დაპაუზებული", "已暂停", "Paused").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = PrimaryViolet.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val customSelectionColors = TextSelectionColors(
                    handleColor = Color.Transparent,
                    backgroundColor = Color.Transparent
                )
                CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = textValue,
                            onValueChange = { newTf ->
                                val typedStr = newTf.text
                                val secs = parseTimeToSeconds(typedStr)
                                textValue = TextFieldValue(text = typedStr, selection = TextRange(typedStr.length))
                                if (secs > 0) {
                                    onSecondsSelected(secs)
                                }
                            },
                            modifier = Modifier
                                .wrapContentWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isInputFocused = true
                                        textValue = TextFieldValue(text = "", selection = TextRange(0))
                                    } else {
                                        isInputFocused = false
                                        val secs = parseTimeToSeconds(textValue.text)
                                        if (secs > 0) {
                                            onSecondsSelected(secs)
                                            val formatted = formatDurationHMS(secs)
                                            textValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                        } else {
                                            val formatted = formatDurationHMS(secondsRemaining)
                                            textValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                        }
                                    }
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.sp
                            ),
                            cursorBrush = SolidColor(PrimaryViolet)
                        )

                        if (!isInputFocused) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        textValue = TextFieldValue(text = "", selection = TextRange(0))
                                        isInputFocused = true
                                        focusRequester.requestFocus()
                                    }
                            )
                        }
                    }
                }
                Text(
                    text = if (isInputFocused && textValue.text.isEmpty()) "Tippe Zeit (z.B. 15, 13000)" else "hh:mm:ss",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

