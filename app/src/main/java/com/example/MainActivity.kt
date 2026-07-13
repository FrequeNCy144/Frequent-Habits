package com.example

import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.StableHabitLogs
import com.example.data.StableHabitList
import com.example.data.StableHabitLogsMap
import com.example.data.CalendarCellState
import com.example.data.HabitStatModel
import com.example.data.HabitDetailUiState
import com.example.data.isHabitActiveOnDate
import com.example.data.isLogCompleted
import com.example.data.getLogStatus
import com.example.data.CalendarGridCellData
import com.example.data.HabitUiItem
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.theme.*
import java.util.*
import android.media.MediaPlayer
import android.media.RingtoneManager
import java.io.File
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[HabitsViewModel::class.java]
        handleWidgetIntent(intent, viewModel)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[HabitsViewModel::class.java]
        handleWidgetIntent(intent, viewModel)
    }

    private fun handleWidgetIntent(intent: Intent?, viewModel: HabitsViewModel) {
        if (intent?.action == "com.example.widget.ACTION_WIDGET_ADD_VALUE") {
            val habitId = intent.getIntExtra("com.example.widget.EXTRA_HABIT_ID", -1)
            if (habitId != -1) {
                viewModel.setPendingWidgetHabitId(habitId)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: HabitsViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var selectedTab by remember { mutableStateOf("TODAY") }
    val navController = rememberNavController()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pendingWidgetHabitId by viewModel.pendingWidgetHabitId.collectAsStateWithLifecycle()

    LaunchedEffect(pendingWidgetHabitId) {
        if (pendingWidgetHabitId != null) {
            if (currentRoute != "TODAY" && currentRoute != null) {
                navController.navigate("TODAY") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    // Keep tab selected state in synchronization if user navigates
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && !currentRoute.startsWith("DETAIL") && selectedTab != currentRoute) {
            selectedTab = currentRoute
        }
    }



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != null && !currentRoute.startsWith("DETAIL") && currentRoute != "CREATE" && currentRoute != "OVERALL_STATS" && currentRoute != "MORE" && editingHabit == null) {
                HabitBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (selectedTab != tab) {
                            selectedTab = tab
                            navController.navigate(tab) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onAddClick = {},
                    language = language
                )
            }
        },
        containerColor = DarkBg,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = "TODAY",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(100)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(100)) }
            ) {
                composable("PROFILE") {
                    ProfileScreen(
                        viewModel = viewModel,
                        language = language,
                        onSettingsClick = {
                            navController.navigate("MORE") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("TODAY") {
                    val onAddClick = remember {
                        {
                            navController.navigate("CREATE") {
                                launchSingleTop = true
                            }
                        }
                    }
                    TodayScreen(
                        viewModel = viewModel,
                        language = language,
                        onAddClick = onAddClick,
                        onEditHabit = { habit ->
                            editingHabit = habit
                            navController.navigate("CREATE") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("CREATE") {
                    CreateHabitScreen(
                        language = language,
                        editingHabit = editingHabit,
                        onDismiss = {
                            editingHabit = null
                            navController.navigate("TODAY") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                            }
                        },
                        onSave = { name, isNeg, cat, icon, color, type, unit, target, freq, start, specDays, remEnabled, remHour, remMin ->
                            if (editingHabit != null) {
                                val updated = editingHabit!!.copy(
                                    name = name,
                                    isNegative = isNeg,
                                    category = cat,
                                    icon = icon,
                                    color = color,
                                    type = type,
                                    unit = unit,
                                    targetValue = target,
                                    frequency = freq,
                                    startDate = start,
                                    specificDays = specDays,
                                    reminderEnabled = remEnabled,
                                    reminderHour = remHour,
                                    reminderMinute = remMin
                                )
                                viewModel.updateHabit(updated)
                                editingHabit = null
                                Toast.makeText(
                                    context,
                                    if (language == "de") "Gewohnheit aktualisiert!" else "Habit updated!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.addHabit(name, cat, icon, color, isNeg, type, unit, target, freq, start, specDays, remEnabled, remHour, remMin)
                                Toast.makeText(
                                    context,
                                    if (language == "de") "Gewohnheit hinzugefügt!" else "Habit added!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            navController.navigate("TODAY") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }
                composable("STATS") {
                    val onAddClick = remember {
                        {
                            navController.navigate("CREATE") {
                                launchSingleTop = true
                            }
                        }
                    }
                    StatsScreen(
                        viewModel = viewModel,
                        language = language,
                        onAddClick = onAddClick,
                        onHabitClick = { habitId ->
                            if (navController.currentDestination?.route?.startsWith("DETAIL") != true) {
                                navController.navigate("DETAIL/$habitId") {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOverallClick = {
                            if (navController.currentDestination?.route != "OVERALL_STATS") {
                                navController.navigate("OVERALL_STATS") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable("OVERALL_STATS") {
                    OverallStatsScreen(
                        viewModel = viewModel,
                        language = language,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("MORE") {
                    SettingsScreen(
                        viewModel = viewModel,
                        language = language,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(
                    route = "DETAIL/{habitId}",
                    arguments = listOf(navArgument("habitId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getInt("habitId") ?: -1
                    LaunchedEffect(habitId) {
                        viewModel.selectHabitForDetail(habitId)
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            viewModel.selectHabitForDetail(null)
                        }
                    }

                    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
                    val habit = remember(habitId, habits) { habits.find { it.id == habitId } }

                    if (habit != null) {
                        val detailState by viewModel.selectedHabitDetailState.collectAsStateWithLifecycle()
                        val isStateMatching = detailState != null && detailState!!.habit.id == habitId
                        val onBack = remember { {
                            navController.popBackStack()
                            Unit
                        } }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            HabitDetailScreen(
                                habit = habit,
                                state = if (isStateMatching) detailState else null,
                                viewModel = viewModel,
                                language = language,
                                onBack = onBack
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryViolet)
                        }
                    }
                }
            }

            // Fading overlay at the bottom so content beautifully fades out behind the floating nav bar
            if (currentRoute != null && !currentRoute.startsWith("DETAIL") && currentRoute != "CREATE" && currentRoute != "OVERALL_STATS" && currentRoute != "MORE" && editingHabit == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DarkBg.copy(alpha = 0.8f),
                                    DarkBg
                                )
                            )
                        )
                )
            }
        }
    }

    if (pendingWidgetHabitId != null) {
        WidgetAddValueDialog(
            habitId = pendingWidgetHabitId!!,
            viewModel = viewModel,
            language = language,
            onDismiss = { viewModel.clearPendingWidgetHabitId() }
        )
    }
}

@Composable
fun WidgetAddValueDialog(
    habitId: Int,
    viewModel: HabitsViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()

    var habitToLog by remember { mutableStateOf<com.example.data.Habit?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(habitId) {
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            val habit = db.habitDao().getHabitByIdSuspend(habitId)
            habitToLog = habit
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoaded = true
        }
    }

    if (!isLoaded) {
        // Wait for database loading, do not dismiss!
    } else if (habitToLog != null) {
        val existingLog = logs.find { it.habitId == habitToLog!!.id && it.date == selectedDate }
        val currentValue = existingLog?.value ?: 0f

        val hasTimer = remember(habitToLog) {
            habitToLog!!.unit.lowercase() in listOf("minuten", "minutes", "min", "minute")
        }

        var inputVal by remember(habitId, currentValue) {
            val target = if (currentValue > 0f) currentValue else (habitToLog?.targetValue ?: 0f)
            val displayVal = if (currentValue == -1f) 0f else target
            val formatted = if (displayVal <= 0f) "" else {
                if (displayVal % 1f == 0f) displayVal.toInt().toString() else displayVal.toString()
            }
            mutableStateOf(formatted)
        }

        val defaultMins = remember(habitToLog) {
            val target = habitToLog?.targetValue ?: 25f
            if (target % 1f == 0f) target.toInt().toString() else target.toString()
        }

        // Timer State variables
        var timerDurationMinutes by remember(habitId) { mutableStateOf(defaultMins) }
        var timerSecondsRemaining by remember(habitId) { 
            val mins = defaultMins.toFloatOrNull() ?: 25f
            mutableStateOf((mins * 60).toInt()) 
        }
        var initialTimerSeconds by remember(habitId) { 
            val mins = defaultMins.toFloatOrNull() ?: 25f
            mutableStateOf((mins * 60).toInt()) 
        }
        var isTimerRunning by remember { mutableStateOf(false) }

        // Audio State variables
        var importedAudios by remember { mutableStateOf<List<File>>(emptyList()) }
        var selectedAudioFile by remember { mutableStateOf<File?>(null) }
        var showAudioDropdown by remember { mutableStateOf(false) }
        var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

        // Refresh audio function
        val refreshAudios = remember(context) {
            {
                val dir = File(context.filesDir, "audios")
                if (!dir.exists()) dir.mkdirs()
                importedAudios = dir.listFiles()?.filter { 
                    it.isFile && (it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "aac")) 
                }?.sortedBy { it.name } ?: emptyList()
            }
        }

        LaunchedEffect(Unit) {
            refreshAudios()
        }

        // File picker for audio import
        val audioPickerLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
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
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        destFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    refreshAudios()
                    selectedAudioFile = destFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Import failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set duration initially and when duration text changes
        LaunchedEffect(timerDurationMinutes) {
            val mins = timerDurationMinutes.toFloatOrNull() ?: 25f
            if (!isTimerRunning) {
                timerSecondsRemaining = (mins * 60).toInt()
                initialTimerSeconds = (mins * 60).toInt()
            }
        }

        // Ticker loop
        LaunchedEffect(isTimerRunning, timerSecondsRemaining) {
            if (isTimerRunning && timerSecondsRemaining > 0) {
                delay(1000L)
                timerSecondsRemaining--

                if (timerSecondsRemaining == 0) {
                    isTimerRunning = false
                    try {
                        val toneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = android.media.RingtoneManager.getRingtone(context, toneUri)
                        ringtone.play()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val totalSecondsElapsed = initialTimerSeconds - timerSecondsRemaining
                    val minutesToLog = Math.round(totalSecondsElapsed / 60f).toInt().coerceAtLeast(1)

                    viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, currentValue + minutesToLog)

                    android.widget.Toast.makeText(
                        context,
                        if (language == "de") "Timer beendet! $minutesToLog Min. wurden eingetragen." else "Timer finished! $minutesToLog min logged.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    onDismiss()
                }
            }
        }

        // Media player control
        LaunchedEffect(isTimerRunning, selectedAudioFile) {
            if (isTimerRunning && selectedAudioFile != null) {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()

                    val mp = MediaPlayer().apply {
                        setDataSource(selectedAudioFile!!.absolutePath)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    mediaPlayer?.pause()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (e: Exception) {
                    // ignored
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (language == "de") "Eintrag: ${habitToLog!!.name}" else "Log: ${habitToLog!!.name}",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasTimer) {
                        // TIMER SECTION
                        val timeText = remember(timerSecondsRemaining) {
                            val displayMinutes = timerSecondsRemaining / 60
                            val displaySeconds = timerSecondsRemaining % 60
                            String.format(Locale.US, "%02d:%02d", displayMinutes, displaySeconds)
                        }
                        val progressFraction = if (initialTimerSeconds > 0) {
                            timerSecondsRemaining.toFloat() / initialTimerSeconds.toFloat()
                        } else {
                            1f
                        }

                        var isEditingDuration by remember { mutableStateOf(false) }
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                        LaunchedEffect(isEditingDuration) {
                            if (isEditingDuration) {
                                timerDurationMinutes = ""
                                try {
                                    kotlinx.coroutines.delay(100)
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                } catch (e: Exception) {
                                    // ignored
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Circular Clock View
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(4.dp)
                                        .drawWithCache {
                                            val strokeWidth = 6.dp.toPx()
                                            val trackStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                            val progressStroke = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                            onDrawBehind {
                                                drawCircle(
                                                    color = ProgressTrack,
                                                    radius = size.minDimension / 2f,
                                                    style = trackStroke
                                                )
                                                drawArc(
                                                    color = PrimaryViolet,
                                                    startAngle = -90f,
                                                    sweepAngle = 360f * progressFraction,
                                                    useCenter = false,
                                                    style = progressStroke
                                                )
                                            }
                                        }
                                ) {

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (isEditingDuration && !isTimerRunning) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = timerDurationMinutes,
                                                onValueChange = { newValue ->
                                                    if (newValue.all { it.isDigit() }) {
                                                        timerDurationMinutes = newValue
                                                    }
                                                },
                                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 22.sp,
                                                    textAlign = TextAlign.Center
                                                ),
                                                cursorBrush = SolidColor(PrimaryViolet),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                                ),
                                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                                    onDone = {
                                                        if (timerDurationMinutes.isEmpty() || timerDurationMinutes.toIntOrNull() == null || timerDurationMinutes.toIntOrNull()!! <= 0) {
                                                            timerDurationMinutes = defaultMins
                                                        }
                                                        isEditingDuration = false
                                                        keyboardController?.hide()
                                                    }
                                                ),
                                                modifier = Modifier.width(60.dp).focusRequester(focusRequester),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (language == "de") "Fertig" else "Done",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryViolet,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    if (timerDurationMinutes.isEmpty() || timerDurationMinutes.toIntOrNull() == null || timerDurationMinutes.toIntOrNull()!! <= 0) {
                                                        timerDurationMinutes = defaultMins
                                                    }
                                                    isEditingDuration = false
                                                    keyboardController?.hide()
                                                }
                                            )
                                        } else {
                                            Text(
                                                text = timeText,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 24.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isTimerRunning) {
                                                    if (language == "de") "AKTIV..." else "ACTIVE..."
                                                  } else {
                                                    if (language == "de") "BEREIT" else "READY"
                                                  },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isTimerRunning) SuccessGreen else TextSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Large Edit button outside the circular timer
                                if (!isTimerRunning && !isEditingDuration) {
                                    IconButton(
                                        onClick = { isEditingDuration = true },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 24.dp)
                                            .size(44.dp)
                                            .background(ProgressTrack, CircleShape)
                                            .border(1.dp, DarkBorder, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Duration",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Play/Pause, Reset, and Finish Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        isTimerRunning = false
                                        val mins = timerDurationMinutes.toIntOrNull() ?: 25
                                        timerSecondsRemaining = mins * 60
                                        initialTimerSeconds = mins * 60
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ProgressTrack, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryViolet)
                                        .clickable {
                                            if (timerSecondsRemaining <= 0) {
                                                val mins = timerDurationMinutes.toIntOrNull() ?: 25
                                                timerSecondsRemaining = mins * 60
                                                initialTimerSeconds = mins * 60
                                            }
                                            isTimerRunning = !isTimerRunning
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isTimerRunning) "Pause" else "Play",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                val totalSecondsElapsed = initialTimerSeconds - timerSecondsRemaining
                                val canLogElapsed = totalSecondsElapsed >= 1

                                IconButton(
                                    onClick = {
                                        isTimerRunning = false
                                        val minutesToLog = Math.round(totalSecondsElapsed / 60f).toInt().coerceAtLeast(0)
                                        if (minutesToLog > 0) {
                                            viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, currentValue + minutesToLog)
                                            android.widget.Toast.makeText(
                                                context,
                                                if (language == "de") "$minutesToLog Min. wurden eingetragen!" else "$minutesToLog min logged!",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, -1f)
                                            android.widget.Toast.makeText(
                                                context,
                                                if (language == "de") "Unter 30 Sek. vergangen. Als fehlgeschlagen markiert." else "Under 30s elapsed. Marked as failed.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (canLogElapsed) SuccessGreen.copy(alpha = 0.2f) else ProgressTrack, CircleShape),
                                    enabled = canLogElapsed
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Finish",
                                        tint = if (canLogElapsed) SuccessGreen else TextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Background Audio Selection
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (language == "de") "Hintergrund-Audio" else "Background Audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ProgressTrack, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable { showAudioDropdown = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedAudioFile?.name ?: (if (language == "de") "Kein Sound" else "No Sound"),
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    audioPickerLauncher.launch("audio/*")
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = "Import",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showAudioDropdown,
                                        onDismissRequest = { showAudioDropdown = false },
                                        modifier = Modifier
                                            .background(DarkCard)
                                            .border(1.dp, DarkBorder)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (language == "de") "Kein Sound" else "No Sound", color = TextPrimary) },
                                            onClick = {
                                                selectedAudioFile = null
                                                showAudioDropdown = false
                                            }
                                        )
                                        importedAudios.forEach { file ->
                                            DropdownMenuItem(
                                                text = { Text(file.name, color = TextPrimary) },
                                                onClick = {
                                                    selectedAudioFile = file
                                                    showAudioDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // MANUAL INPUT & SUGGESTIONS SECTION (Combined)
                    Text(
                        text = if (language == "de") {
                            "Wert eintragen (Einheit: ${habitToLog!!.unit}):"
                        } else {
                            "Enter value (Unit: ${habitToLog!!.unit}):"
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { inputVal = it },
                        placeholder = { Text("Z.B. 1.5", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_value_input")
                    )

                    // Value suggestions (Presets replacing timer presets when hasTimer is true, or standard numeric selection)
                    Text(
                        text = if (language == "de") "Schnellauswahl" else "Suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    val suggestions = remember(habitToLog) {
                        val target = habitToLog?.targetValue ?: 0f
                        when {
                            habitToLog?.unit?.lowercase() in listOf("l", "liter", "liters") -> listOf(0.5f, 1.0f, 1.5f, 2.0f, target)
                            habitToLog?.unit?.lowercase() in listOf("ml") -> listOf(250f, 500f, 750f, target)
                            habitToLog?.unit?.lowercase() in listOf("min", "minutes", "minuten") -> listOf(15f, 30f, 45f, 60f, target)
                            habitToLog?.unit?.lowercase() in listOf("h", "stunden", "hours") -> listOf(1f, 2f, 3f, target)
                            else -> {
                                if (target > 1f) {
                                    val half = target / 2f
                                    val halfSuggested = if (half % 1f == 0f) half else (Math.round(half * 10f) / 10f).toFloat()
                                    listOfNotNull(
                                        1f,
                                        if (halfSuggested > 1f && halfSuggested != target) halfSuggested else null,
                                        target
                                    ).distinct()
                                } else {
                                    listOf(1f, 2f, 5f, target)
                                }
                            }
                        }.distinct().sorted()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { value ->
                            val label = if (value % 1f == 0f) value.toInt().toString() else value.toString()
                            val isSelected = inputVal == label
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) PrimaryViolet else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryViolet else PrimaryViolet.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        inputVal = label
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentValue > 0f || existingLog != null) {
                                TextButton(
                                    onClick = {
                                        viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, 0f)
                                        onDismiss()
                                    },
                                    modifier = Modifier.testTag("dialog_reset_button")
                                ) {
                                    Text(if (language == "de") "Reset" else "Reset", color = HabitOrange)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("dialog_cancel_button")
                                ) {
                                    Text(if (language == "de") "Abbrechen" else "Cancel", color = TextSecondary)
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, -1f)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FailedRed),
                                    modifier = Modifier.testTag("dialog_fail_button")
                                ) {
                                    Text(if (language == "de") "Fehlgeschlagen" else "Failed")
                                }
                            }
                        }

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            onClick = {
                                val fValue = inputVal.toFloatOrNull() ?: 0f
                                if (fValue <= 0f) {
                                    viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, -1f)
                                } else {
                                    viewModel.logNumericalHabit(habitToLog!!.id, selectedDate, fValue)
                                }
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("dialog_save_button")
                        ) {
                            Text(if (language == "de") "Speichern" else "Save", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = null
        )
    } else {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }
}

// BOTTOM NAVIGATION
@Composable
fun HabitBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    language: String
) {
    NavigationBar(
        containerColor = DarkCard,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp, top = 12.dp)
            .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
    ) {
        NavigationBarItem(
            selected = selectedTab == "TODAY",
            onClick = { onTabSelected("TODAY") },
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Today") },
            label = { 
                Text(
                    text = if (language == "de") "Heute" else "Today",
                    style = MaterialTheme.typography.labelLarge
                ) 
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                unselectedIconColor = TextSecondary,
                selectedTextColor = PrimaryViolet,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryViolet
            ),
            modifier = Modifier.testTag("nav_today")
        )

        NavigationBarItem(
            selected = selectedTab == "STATS",
            onClick = { onTabSelected("STATS") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
            label = { 
                Text(
                    text = if (language == "de") "Statistik" else "Stats",
                    style = MaterialTheme.typography.labelLarge
                ) 
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                unselectedIconColor = TextSecondary,
                selectedTextColor = PrimaryViolet,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryViolet
            ),
            modifier = Modifier.testTag("nav_stats")
        )

        NavigationBarItem(
            selected = selectedTab == "PROFILE",
            onClick = { onTabSelected("PROFILE") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { 
                Text(
                    text = if (language == "de") "Profil" else "Profile",
                    style = MaterialTheme.typography.labelLarge
                ) 
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                unselectedIconColor = TextSecondary,
                selectedTextColor = PrimaryViolet,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryViolet
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}

@Composable
fun CalendarDayItem(
    dayStr: String,
    dayNum: String,
    dayName: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val dayScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(120),
        label = "dayScale"
    )

    val weekdayColor = when {
        !isEnabled -> TextSecondary.copy(alpha = 0.3f)
        isSelected -> Color.White
        isToday -> PrimaryViolet
        isFuture -> TextSecondary.copy(alpha = 0.4f)
        else -> TextSecondary
    }

    val dateColor = when {
        !isEnabled -> TextSecondary.copy(alpha = 0.3f)
        isSelected -> Color.White
        isToday -> PrimaryViolet
        isFuture -> TextSecondary.copy(alpha = 0.4f)
        else -> TextPrimary
    }

    // Selected day gets the full pill background
    val bgModifier = if (isSelected) {
        Modifier
            .background(color = PrimaryViolet, shape = RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp)
    } else {
        Modifier
            .padding(vertical = 10.dp, horizontal = 4.dp)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = dayScale
                scaleY = dayScale
            }
            .clickable(enabled = isEnabled) { onSelect(dayStr) }
            .then(bgModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Weekday Name (e.g. MO)
        Text(
            text = dayName.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = weekdayColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        // Date Number
        Text(
            text = dayNum,
            style = MaterialTheme.typography.titleMedium,
            color = dateColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

fun getHabitProgressForDate(dateStr: String, habits: List<Habit>, logs: List<HabitLog>): Pair<Int, Int> {
    val activeHabits = habits.filter { isHabitActiveOnDate(it, dateStr) }
    if (activeHabits.isEmpty()) return 0 to 0

    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
    var completedCount = 0
    var nonPausedActiveCount = 0
    activeHabits.forEach { habit ->
        val log = logsMap[habit.id]
        val isPaused = log != null && log.isPaused
        if (!isPaused) {
            nonPausedActiveCount++
            if (isLogCompleted(habit, log)) {
                completedCount++
            }
        }
    }
    return completedCount to nonPausedActiveCount
}

fun getDayCombinedStatus(dateStr: String, habits: List<Habit>, logs: List<HabitLog>): String {
    val todayStr = java.time.LocalDate.now().toString()
    if (dateStr > todayStr) return "INACTIVE"

    val activeHabits = habits.filter { isHabitActiveOnDate(it, dateStr) }
    if (activeHabits.isEmpty()) return "INACTIVE"

    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
    
    var anyPending = false
    var anyFailed = false
    var anySuccess = false
    var anyPaused = false

    activeHabits.forEach { habit ->
        val log = logsMap[habit.id]
        val hStatus = getLogStatus(habit, log, dateStr, "1970-01-01", todayStr)

        if (hStatus == "PENDING") anyPending = true
        if (hStatus == "FAILED") anyFailed = true
        if (hStatus == "SUCCESS") anySuccess = true
        if (hStatus == "PAUSED") anyPaused = true
    }

    return when {
        anyPending -> "PENDING"
        anyFailed -> "FAILED"
        anySuccess -> "SUCCESS"
        anyPaused -> "PAUSED"
        else -> "SUCCESS"
    }
}

// TODAY SCREEN
@Composable
fun TodayScreen(
    viewModel: HabitsViewModel,
    language: String,
    onAddClick: () -> Unit,
    onEditHabit: (Habit) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekStartCalendar by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()

    val activeHabitUiItemsForSelectedDate by viewModel.activeHabitUiItemsForSelectedDate.collectAsStateWithLifecycle()
    val minWeekStartMillis by viewModel.minWeekStartMillis.collectAsStateWithLifecycle()
    val minDateStr by viewModel.minDateStr.collectAsStateWithLifecycle()

    val todayDateString by viewModel.todayDateString.collectAsStateWithLifecycle()
    val canPrevWeek by viewModel.canPrevWeek.collectAsStateWithLifecycle()
    val canNextWeek by viewModel.canNextWeek.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity

    var longPressedHabit by remember { mutableStateOf<Habit?>(null) }
    var showDeleteConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var showArchiveConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var manualAddValueHabitId by remember { mutableStateOf<Int?>(null) }

    val onToggleRemembered = remember(viewModel, selectedDate) {
        { habitId: Int, currentlyHasLog: Boolean ->
            viewModel.toggleBinaryHabit(habitId, selectedDate, currentlyHasLog)
        }
    }
    val onAddQuantityRemembered = remember(viewModel, selectedDate) {
        { habitId: Int, currentVal: Float, diff: Float ->
            val newVal = (currentVal + diff).coerceAtLeast(0f)
            viewModel.logNumericalHabit(habitId, selectedDate, newVal)
        }
    }
    val onSelectDayRemembered = remember(viewModel) {
        { dateStr: String ->
            viewModel.selectDate(dateStr)
        }
    }

    val currentWeekDaysData by viewModel.currentWeekDaysData.collectAsStateWithLifecycle()
    val formattedDisplayDate by viewModel.formattedDisplayDate.collectAsStateWithLifecycle()

    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val currentNote = remember(allDailyNotes, selectedDate) {
        allDailyNotes.find { it.date == selectedDate }?.content ?: ""
    }
    var showDailyNoteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 16.dp)
        ) {
        item(key = "today_top_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDisplayDate,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .clickable { showDailyNoteDialog = true }
                            .testTag("btn_daily_note"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Daily Note",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (currentNote.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(6.dp)
                                    .background(PrimaryViolet, CircleShape)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .clickable { onAddClick() }
                            .testTag("btn_add_habit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Habit",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item(key = "today_calendar_strip") {
            var offsetX by remember { mutableStateOf(0f) }
            val swipeThreshold = 120f

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (canPrevWeek) viewModel.prevWeek() },
                        enabled = canPrevWeek,
                        modifier = Modifier.size(36.dp).testTag("prev_week_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Week",
                            tint = if (canPrevWeek) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                            .graphicsLayer {
                                translationX = offsetX
                            }
                            .pointerInput(canPrevWeek, canNextWeek) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX > swipeThreshold && canPrevWeek) {
                                            viewModel.prevWeek()
                                        } else if (offsetX < -swipeThreshold && canNextWeek) {
                                            viewModel.nextWeek()
                                        }
                                        offsetX = 0f
                                    },
                                    onDragCancel = {
                                        offsetX = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        val newOffset = offsetX + dragAmount
                                        if (newOffset > 0 && !canPrevWeek) {
                                            offsetX = (offsetX + dragAmount * 0.2f).coerceAtMost(20f)
                                        } else if (newOffset < 0 && !canNextWeek) {
                                            offsetX = (offsetX + dragAmount * 0.2f).coerceAtLeast(-20f)
                                        } else {
                                            offsetX += dragAmount
                                        }
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        currentWeekDaysData.forEach { (dayStr, dayNum, dayName) ->
                            val isSelected = dayStr == selectedDate
                            val isDayEnabled = dayStr >= minDateStr
                            key(dayStr) {
                                CalendarDayItem(
                                    dayStr = dayStr,
                                    dayNum = dayNum,
                                    dayName = dayName,
                                    isSelected = isSelected,
                                    onSelect = onSelectDayRemembered,
                                    isToday = dayStr == todayDateString,
                                    isFuture = dayStr > todayDateString,
                                    modifier = Modifier.weight(1f),
                                    isEnabled = isDayEnabled
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { if (canNextWeek) viewModel.nextWeek() },
                        enabled = canNextWeek,
                        modifier = Modifier.size(36.dp).testTag("next_week_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Week",
                            tint = if (canNextWeek) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item(key = "today_progress_card") {
            val isPastDay = remember(selectedDate, todayDateString) {
                selectedDate < todayDateString
            }
            val (completed, total) = todayProgressTuple
            val fraction = remember(todayProgressTuple) {
                if (total > 0) completed.toFloat() / total else 0f
            }
            val progressText = remember(completed, total) { "$completed/$total" }
            val encouragementText = remember(completed, total, language) {
                getEncouragementText(completed, total, language)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ProgressTrack)
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(if (fraction >= 1.0f) SuccessGreen else PrimaryViolet)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "de") "Tagesfortschritt" else "Daily Progress",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${(fraction * 100).toInt()}% ($progressText)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isPastDay) " " else encouragementText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (activeHabitUiItemsForSelectedDate.isEmpty()) {
            item(key = "today_habits_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "No habits",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (language == "de") "Noch keine Habits angelegt." else "No habits yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == "de") {
                                "Tippe oben rechts auf +, um deine erste Gewohnheit zu erstellen."
                            } else {
                                "Tap + in the top right to create your first habit."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
} else {
            items(
                items = activeHabitUiItemsForSelectedDate, 
                key = { it.habit.id },
                contentType = { "HABIT_CARD" }
            ) { uiItem ->
                val currentHabit = uiItem.habit
                
                val onLongClickRemembered = remember(currentHabit.id) {
                    { habit: Habit -> longPressedHabit = habit }
                }

                val onToggleClick = remember(currentHabit.id, currentHabit.type, onToggleRemembered) {
                    { habitId: Int, hasLog: Boolean ->
                        if (currentHabit.type == "NUMBER") {
                            manualAddValueHabitId = habitId
                        } else {
                            onToggleRemembered(habitId, hasLog)
                        }
                    }
                }

                HabitItemRow(
                    habit = currentHabit,
                    currentValue = uiItem.currentValue,
                    isCompleted = uiItem.isCompleted,
                    isFailed = uiItem.isFailed,
                    isPaused = uiItem.isPaused,
                    hasLog = uiItem.hasLog,
                    onToggle = onToggleClick,
                    onAddQuantity = onAddQuantityRemembered,
                    onLongClick = onLongClickRemembered,
                    language = language
                )
            }
        }
    }

    if (showDailyNoteDialog) {
        DailyNoteDialog(
            currentNote = currentNote,
            language = language,
            onDismiss = { showDailyNoteDialog = false },
            onSave = { noteText ->
                viewModel.saveDailyNote(selectedDate, noteText)
                showDailyNoteDialog = false
            }
        )
    }
}

    if (longPressedHabit != null) {
        val habit = longPressedHabit!!
        val isPausedOnSelectedDate = activeHabitUiItemsForSelectedDate.find { it.habit.id == habit.id }?.isPaused == true

        AlertDialog(
            onDismissRequest = { longPressedHabit = null },
            containerColor = DarkCard,
            title = {
                Text(
                    text = habit.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (language == "de") {
                            "Wähle eine Aktion für diese Gewohnheit:"
                        } else {
                            "Choose an action for this habit:"
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Pausieren / Fortsetzen Action
                    Button(
                        onClick = {
                            viewModel.togglePauseHabit(habit.id)
                            longPressedHabit = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPausedOnSelectedDate) SuccessGreen else HabitOrange,
                            contentColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPausedOnSelectedDate) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPausedOnSelectedDate) {
                                if (language == "de") "Pausierung beenden" else "Resume Habit"
                            } else {
                                if (language == "de") "Heute pausieren" else "Pause today"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Bearbeiten Action
                    Button(
                        onClick = {
                            val habitToEdit = habit
                            longPressedHabit = null
                            onEditHabit(habitToEdit)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryViolet,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "Bearbeiten" else "Edit",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Reorder Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.moveHabitUp(habit.id)
                                longPressedHabit = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBorder,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "de") "Nach oben" else "Move Up",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.moveHabitDown(habit.id)
                                longPressedHabit = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBorder,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "de") "Nach unten" else "Move Down",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Archivieren Action
                    Button(
                        onClick = {
                            val habitToArchive = habit
                            longPressedHabit = null
                            showArchiveConfirmHabit = habitToArchive
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HabitOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "Archivieren" else "Archive",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Löschen Action
                    OutlinedButton(
                        onClick = {
                            val habitToDelete = habit
                            longPressedHabit = null
                            showDeleteConfirmHabit = habitToDelete
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorRed
                        ),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "Unwiderruflich löschen" else "Delete permanently",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { longPressedHabit = null }
                ) {
                    Text(
                        text = if (language == "de") "Abbrechen" else "Cancel",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (showDeleteConfirmHabit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmHabit = null },
            containerColor = DarkCard,
            title = { Text(text = if (language == "de") "Habit löschen?" else "Delete Habit?") },
            text = { Text(text = if (language == "de") "Möchtest du diese Gewohnheit wirklich unwiderruflich löschen?" else "Are you sure you want to delete this habit permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val habit = showDeleteConfirmHabit!!
                        viewModel.deleteHabit(habit)
                        showDeleteConfirmHabit = null
                        Toast.makeText(
                            context,
                            if (language == "de") "Gewohnheit gelöscht" else "Habit deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text(if (language == "de") "Löschen" else "Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmHabit = null }) {
                    Text(if (language == "de") "Abbrechen" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showArchiveConfirmHabit != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmHabit = null },
            containerColor = DarkCard,
            title = { Text(text = if (language == "de") "Gewohnheit archivieren?" else "Archive Habit?") },
            text = { Text(text = if (language == "de") "Möchtest du diese Gewohnheit archivieren? Sie wird vom Dashboard und den Statistiken ausgeblendet, kann aber in den Einstellungen jederzeit wieder reaktiviert werden." else "Do you want to archive this habit? It will be hidden from the dashboard and stats, but can be reactivated at any time in settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val habit = showArchiveConfirmHabit!!
                        viewModel.archiveHabit(habit)
                        showArchiveConfirmHabit = null
                        Toast.makeText(
                            context,
                            if (language == "de") "Gewohnheit archiviert" else "Habit archived",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text(if (language == "de") "Archivieren" else "Archive", color = HabitOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmHabit = null }) {
                    Text(if (language == "de") "Abbrechen" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (manualAddValueHabitId != null) {
        WidgetAddValueDialog(
            habitId = manualAddValueHabitId!!,
            viewModel = viewModel,
            language = language,
            onDismiss = { manualAddValueHabitId = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitItemRow(
    habit: Habit,
    currentValue: Float,
    isCompleted: Boolean,
    isFailed: Boolean,
    isPaused: Boolean,
    hasLog: Boolean,
    onToggle: (Int, Boolean) -> Unit,
    onAddQuantity: (Int, Float, Float) -> Unit,
    onLongClick: (Habit) -> Unit,
    language: String
) {
    val habitColor = HabitIconMapping.getColor(habit.color)

    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isPaused -> PausedBg
            isCompleted -> SuccessBg
            isFailed -> FailedBg
            else -> DarkCard
        },
        animationSpec = tween(120),
        label = "bgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isPaused -> HabitOrange
            isCompleted -> SuccessGreen
            isFailed -> FailedRed
            else -> DarkBorder
        },
        animationSpec = tween(120),
        label = "borderColor"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (isPaused || isCompleted || isFailed) 1.05f else 1.0f,
        animationSpec = tween(100),
        label = "checkboxScale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // 1. Draw animated background
                drawRoundRect(
                    color = animatedBgColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )

                // 2. Draw animated or gradient border
                val brush = when {
                    isPaused -> {
                        Brush.linearGradient(
                            colors = listOf(
                                HabitOrange,
                                HabitOrange.copy(alpha = 0.4f),
                                HabitOrange
                            )
                        )
                    }
                    isCompleted -> {
                        Brush.linearGradient(
                            colors = listOf(
                                SuccessGreen,
                                SuccessGreen.copy(alpha = 0.4f),
                                SuccessGreen
                            )
                        )
                    }
                    isFailed -> {
                        Brush.linearGradient(
                            colors = listOf(
                                FailedRed,
                                FailedRed.copy(alpha = 0.4f),
                                FailedRed
                            )
                        )
                    }
                    else -> {
                        SolidColor(animatedBorderColor)
                    }
                }

                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    brush = brush,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
            .combinedClickable(
                onClick = { onToggle(habit.id, hasLog) },
                onLongClick = { onLongClick(habit) }
            )
            .testTag("habit_card_${habit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = checkboxScale
                            scaleY = checkboxScale
                        }
                        .background(habitColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, habitColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = HabitIconMapping.getIconDrawableId(habit.icon)),
                        contentDescription = habit.name,
                        tint = habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPaused) {
                            if (language == "de") "Pausiert" else "Paused"
                        } else if (habit.type == "BINARY") {
                            when {
                                isCompleted -> if (language == "de") "Erledigt" else "Done"
                                isFailed -> if (language == "de") "Fehlgeschlagen" else "Failed"
                                else -> if (language == "de") "Tippen zum Abhaken" else "Tap to check off"
                            }
                        } else {
                            val displayValue = if (isFailed) 0f else currentValue
                            val formattedVal = if (displayValue % 1f == 0f) displayValue.toInt().toString() else displayValue.toString()
                            val formattedTarget = if (habit.targetValue % 1f == 0f) habit.targetValue.toInt().toString() else habit.targetValue.toString()
                            if (isFailed) {
                                val failText = if (language == "de") "Fehlgeschlagen" else "Failed"
                                "$failText • $formattedVal / $formattedTarget ${habit.unit}"
                            } else {
                                "$formattedVal / $formattedTarget ${habit.unit}"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isPaused -> HabitOrange
                            isCompleted -> SuccessGreen
                            isFailed -> FailedRed
                            else -> TextSecondary
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status Checkbox (zum schnellen Abhaken/Zurücksetzen bzw. um 1 Erhöhen für Zahlenbasiert)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            scaleX = checkboxScale
                            scaleY = checkboxScale
                        }
                        .background(
                            when {
                                isPaused -> HabitOrange
                                isCompleted -> SuccessGreen
                                isFailed -> FailedRed
                                else -> Color.Transparent
                            }, 
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isPaused -> HabitOrange
                                isCompleted -> SuccessGreen
                                isFailed -> FailedRed
                                else -> TextSecondary.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        )
                        .clickable(enabled = !isPaused) {
                            if (habit.type == "NUMBER") {
                                // Mit einem Klick auf den Kreis rechts kann man den Wert um 1 erhöhen, nicht um mehrere
                                onAddQuantity(habit.id, currentValue, 1f)
                            } else {
                                onToggle(habit.id, hasLog)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isPaused -> {
                            Icon(Icons.Default.Pause, contentDescription = "Paused", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        isCompleted -> {
                            Icon(Icons.Default.Check, contentDescription = "Completed", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        isFailed -> {
                            Icon(Icons.Default.Close, contentDescription = "Failed", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// STATS SCREEN
@Composable
fun StatsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onAddClick: () -> Unit,
    onHabitClick: (Int) -> Unit,
    onOverallClick: () -> Unit
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreakOfAll.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekStartCalendar by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val minWeekStartMillis by viewModel.minWeekStartMillis.collectAsStateWithLifecycle()
    val minDateStr by viewModel.minDateStr.collectAsStateWithLifecycle()
    val currentWeekDaysData by viewModel.currentWeekDaysData.collectAsStateWithLifecycle()
    val formattedDisplayDate by viewModel.formattedDisplayDate.collectAsStateWithLifecycle()

    val todayDateString by viewModel.todayDateString.collectAsStateWithLifecycle()
    val canPrevWeek by viewModel.canPrevWeek.collectAsStateWithLifecycle()
    val canNextWeek by viewModel.canNextWeek.collectAsStateWithLifecycle()
    val statsDayNamesAndNumbers by viewModel.statsDayNamesAndNumbers.collectAsStateWithLifecycle()

    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val currentNote = remember(allDailyNotes, selectedDate) {
        allDailyNotes.find { it.date == selectedDate }?.content ?: ""
    }

    var showDailyNoteDialog by remember { mutableStateOf(false) }

    val onSelectDayRemembered = remember(viewModel) {
        { dateStr: String ->
            viewModel.selectDate(dateStr)
        }
    }

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isPastDay = remember(selectedDate, todayDateString) {
        selectedDate < todayDateString
    }

    val encouragementText = remember(todayProgressTuple, language) {
        getEncouragementText(todayProgressTuple.first, todayProgressTuple.second, language)
    }

    val onStatItemClick = remember(onHabitClick) {
        { habitId: Int ->
            onHabitClick(habitId)
        }
    }

    val shortDayNames = statsDayNamesAndNumbers.first
    val dayNumbers = statsDayNamesAndNumbers.second

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 16.dp)
        ) {
            item(key = "stats_top_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDisplayDate,
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .clickable { showDailyNoteDialog = true }
                                .testTag("btn_daily_note_stats"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Daily Note",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (currentNote.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .size(6.dp)
                                        .background(PrimaryViolet, CircleShape)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .clickable { onAddClick() }
                                .testTag("btn_add_habit_stats"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Habit",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item(key = "stats_calendar_strip") {
                var offsetX by remember { mutableStateOf(0f) }
                val swipeThreshold = 120f

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (canPrevWeek) viewModel.prevWeek() },
                            enabled = canPrevWeek,
                            modifier = Modifier.size(36.dp).testTag("stats_prev_week_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Week",
                                tint = if (canPrevWeek) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 8.dp)
                                .graphicsLayer {
                                    translationX = offsetX
                                }
                                .pointerInput(canPrevWeek, canNextWeek) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (offsetX > swipeThreshold && canPrevWeek) {
                                                viewModel.prevWeek()
                                            } else if (offsetX < -swipeThreshold && canNextWeek) {
                                                viewModel.nextWeek()
                                            }
                                            offsetX = 0f
                                        },
                                        onDragCancel = {
                                            offsetX = 0f
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            val newOffset = offsetX + dragAmount
                                            if (newOffset > 0 && !canPrevWeek) {
                                                offsetX = (offsetX + dragAmount * 0.2f).coerceAtMost(20f)
                                            } else if (newOffset < 0 && !canNextWeek) {
                                                offsetX = (offsetX + dragAmount * 0.2f).coerceAtLeast(-20f)
                                            } else {
                                                offsetX += dragAmount
                                            }
                                        }
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentWeekDaysData.forEach { (dayStr, dayNum, dayName) ->
                                val isSelected = dayStr == selectedDate
                                val isDayEnabled = dayStr >= minDateStr
                                key(dayStr) {
                                    CalendarDayItem(
                                        dayStr = dayStr,
                                        dayNum = dayNum,
                                        dayName = dayName,
                                        isSelected = isSelected,
                                        onSelect = onSelectDayRemembered,
                                        isToday = dayStr == todayDateString,
                                        isFuture = dayStr > todayDateString,
                                        modifier = Modifier.weight(1f),
                                        isEnabled = isDayEnabled
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { if (canNextWeek) viewModel.nextWeek() },
                            enabled = canNextWeek,
                            modifier = Modifier.size(36.dp).testTag("stats_next_week_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Week",
                                tint = if (canNextWeek) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Overall Strength Card
            item {
                val sweepAngleVal = remember(strength) { (strength.toFloat() / 100f) * 360f }
                val strengthText = remember(strength) { "$strength/100" }
                val strengthLabel = remember(strength, language) { getStrengthLabel(strength, language) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(20.dp))
                        .clickable { onOverallClick() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .drawWithCache {
                                            val strokeWidth = 8.dp.toPx()
                                            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            onDrawBehind {
                                                drawArc(
                                                    color = ProgressTrack,
                                                    startAngle = -90f,
                                                    sweepAngle = 360f,
                                                    useCenter = false,
                                                    style = stroke
                                                )
                                                drawArc(
                                                    color = SuccessGreen,
                                                    startAngle = -90f,
                                                    sweepAngle = sweepAngleVal,
                                                    useCenter = false,
                                                    style = stroke
                                                )
                                            }
                                        }
                                ) {
                                    Text(
                                        text = strengthText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Overall Strength",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (language == "de") "GESAMT-STÄRKE" else "OVERALL STRENGTH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        InfoIconButton(
                                            title = if (language == "de") "Gesamt-Stärke Score" else "Overall Strength Score",
                                            explanation = if (language == "de") {
                                                "Die durchschnittliche gewichtete Stärke aller deiner Gewohnheiten zusammen."
                                            } else {
                                                "The average weighted strength score of all of your active habits combined."
                                            },
                                            onClick = { t, e -> activeExplanation = t to e }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = strengthLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ProgressEndText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "View Details",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isPastDay) " " else encouragementText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Individual Habits Header
            item {
                Text(
                    text = if (language == "de") "GEWOHNHEITEN IM DETAIL" else "HABITS IN DETAIL",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }

            if (habitsWithStats.isEmpty()) {
                item {
                    Text(
                        text = if (language == "de") "Keine Gewohnheiten vorhanden." else "No habits configured yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                items(
                    items = habitsWithStats, 
                    key = { it.habit.id },
                    contentType = { "STAT_CARD" } // FIX: Jetpack Compose recycelt jetzt auch die Statistik-Karten beim Scrollen!
                ) { model ->
                    HabitStatItem(
                        model = model,
                        shortDayNames = shortDayNames,
                        dayNumbers = dayNumbers,
                        language = language,
                        onClick = onStatItemClick
                    )
                }
            }
        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }

        if (showDailyNoteDialog) {
            DailyNoteDialog(
                currentNote = currentNote,
                language = language,
                onDismiss = { showDailyNoteDialog = false },
                onSave = { noteText ->
                    viewModel.saveDailyNote(selectedDate, noteText)
                    showDailyNoteDialog = false
                }
            )
        }
    }
}

@Composable
fun HabitStatItem(
    model: HabitStatModel,
    shortDayNames: List<String>,
    dayNumbers: List<String>,
    language: String,
    onClick: (Int) -> Unit
) {
    val habit = model.habit
    val strength = model.strength
    val habitColor = HabitIconMapping.getColor(habit.color)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable { onClick(habit.id) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(habitColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, habitColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = HabitIconMapping.getIconDrawableId(habit.icon)),
                            contentDescription = habit.name,
                            tint = habitColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == "de") "Stärke: $strength/100" else "Strength: $strength/100",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View Details", tint = TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 7) {
                    val dayName = shortDayNames[i]
                    val dayNum = dayNumbers[i]
                    val status = model.past7DaysStatuses[i]
                    val cellColor = when (status) {
                        "SUCCESS" -> SuccessGreen
                        "FAILED" -> ErrorRed
                        "PENDING" -> HabitYellow
                        "PAUSED" -> PrimaryViolet.copy(alpha = 0.15f)
                        else -> PrimaryViolet.copy(alpha = 0.15f)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status == "SUCCESS" || status == "FAILED" || status == "PENDING") DarkBg else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// HABIT DETAIL SCREEN
@Composable
fun HabitDetailScreen(
    habit: Habit,
    state: HabitDetailUiState?,
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentStreak = state?.currentStreak ?: 0
    val longestStreak = state?.longestStreak ?: 0
    val strength = state?.strength ?: 0

    val thisWeekCount = state?.thisWeekCount ?: 0
    val thisMonthCount = state?.thisMonthCount ?: 0
    val thisYearCount = state?.thisYearCount ?: 0
    val totalCount = state?.totalCount ?: 0

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 16.dp)
        ) {
        // Toolbar
        item(key = "detail_toolbar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Strength score card
        item(key = "detail_strength") {
            val habitColor = remember(habit.color) { HabitIconMapping.getColor(habit.color) }
            val sweepAngleVal = remember(strength) { (strength.toFloat() / 100f) * 360f }
            val strengthText = remember(strength) { "$strength/100" }
            val strengthLabel = remember(strength, language) { getStrengthLabel(strength, language) }

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .drawWithCache {
                                val strokeWidth = 6.dp.toPx()
                                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                onDrawBehind {
                                    drawArc(
                                        color = ProgressTrack,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = stroke
                                    )
                                    drawArc(
                                        color = SuccessGreen,
                                        startAngle = -90f,
                                        sweepAngle = sweepAngleVal,
                                        useCenter = false,
                                        style = stroke
                                    )
                                }
                            }
                    ) {
                        Text(
                            text = strengthText,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == "de") "STÄRKE" else "STRENGTH",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            InfoIconButton(
                                title = if (language == "de") "Stärke-Wert" else "Strength Score",
                                explanation = if (language == "de") {
                                    "Die gewichtete Stärke dieser Gewohnheit. Neuere Einträge haben ein deutlich höheres Gewicht, sodass sich dein Score schnell auf 100/100 erholen kann, wenn du am Ball bleibst."
                                } else {
                                    "The weighted strength score of this habit. Recent entries carry significantly more weight, allowing your score to recover to 100/100 quickly if you stay consistent."
                                },
                                onClick = { t, e -> activeExplanation = t to e }
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strengthLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ProgressEndText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Streaks card
        item(key = "detail_streaks") {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "ERFOLGSSTRÄHNE & STATUS" else "STREAK & STATS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (language == "de") "AKTUELL" else "CURRENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                InfoIconButton(
                                    title = if (language == "de") "Aktueller Streak" else "Current Streak",
                                    explanation = if (language == "de") {
                                        "Die Anzahl der aufeinanderfolgenden Tage, an denen du diese Gewohnheit bis heute erfolgreich abgeschlossen hast."
                                    } else {
                                        "The number of consecutive days you have completed this habit up to today."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "de") "$currentStreak Tage" else "$currentStreak Days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (language == "de") "LÄNGSTER" else "LONGEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                InfoIconButton(
                                    title = if (language == "de") "Längster Streak" else "Longest Streak",
                                    explanation = if (language == "de") {
                                        "Deine historische Bestleistung an aufeinanderfolgenden Tagen, an denen du diese Gewohnheit abgeschlossen hast."
                                    } else {
                                        "Your highest historical record of consecutive days completing this habit."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "de") "$longestStreak Tage" else "$longestStreak Days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (language == "de") "QUOTE" else "RATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                InfoIconButton(
                                    title = if (language == "de") "Erfolgsquote" else "Completion Rate",
                                    explanation = if (language == "de") {
                                        "Der ungewichtete historische Prozentsatz der Tage, an denen du diese Gewohnheit seit ihrer Erstellung abgeschlossen hast."
                                    } else {
                                        "The unweighted historical percentage of days you have successfully completed this habit since starting it."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val completionRate = state?.completionRate ?: 0
                            Text(
                                text = "$completionRate%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Achievements counters
        item(key = "detail_achievements") {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Achieved",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "ERREICHT" else "ACHIEVED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        InfoIconButton(
                            title = if (language == "de") "Erledigungs-Verlauf" else "Completion History",
                            explanation = if (language == "de") {
                                "Verfolgt die absolute Anzahl der abgeschlossenen Einträge für diese spezifische Gewohnheit über verschiedene Zeiträume (Diese Woche, Diesen Monat, Dieses Jahr, Gesamt)."
                            } else {
                                "Tracks the absolute count of completed logs for this specific habit over different time periods (This Week, This Month, This Year, Total)."
                            },
                            onClick = { t, e -> activeExplanation = t to e }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AchievementCounterCol(label = if (language == "de") "Diese Woche" else "This Week", count = thisWeekCount)
                        AchievementCounterCol(label = if (language == "de") "Diesen Monat" else "This Month", count = thisMonthCount)
                        AchievementCounterCol(label = if (language == "de") "Dieses Jahr" else "This Year", count = thisYearCount)
                        AchievementCounterCol(label = if (language == "de") "Gesamt" else "Total", count = totalCount)
                    }
                }
            }
        }

        // Target Performance Card (Only shown for numerical habits!)
        if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
            item(key = "detail_target_performance") {
                HabitTargetSection(
                    state = state,
                    language = language
                )
            }
        }

        // Calendar Grid Card
        item(key = "detail_calendar") {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "KALENDER" else "CALENDAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        InfoIconButton(
                            title = if (language == "de") "Kalender-Historie" else "Calendar History",
                            explanation = if (language == "de") {
                                "Bietet eine vollständige Monatsübersicht deiner Abschlüsse. Grün steht für erfolgreiche Tage, Rot für fehlgeschlagene Tage, Gelb für ausstehende Tage und Dunkelgrau für inaktive Tage oder Pausen."
                            } else {
                                "Provides a full monthly overview of your completions. Green represents a successful day, red represents a failed day, yellow/amber represents a pending log, and dark grey indicates inactive/rest days."
                            },
                            onClick = { t, e -> activeExplanation = t to e }
                        )
                    }

                    val habitColor = HabitIconMapping.getColor(habit.color)
                    // HIER IST DAS PERFORMANCE-UPGRADE: Wir übergeben nur dumme Primitiven an den Kalender!
                    // Calendar Month navigation and display grid
                    CalendarMonthGrid(
                        gridRows = state?.calendarGridRows ?: emptyList(),
                        monthName = state?.monthName ?: "",
                        habitColor = habitColor,
                        language = language,
                        canPrevMonth = state?.canPrevMonth ?: true,
                        canNextMonth = state?.canNextMonth ?: true,
                        onPrevMonth = { viewModel.navigateCalendarMonth(-1) },
                        onNextMonth = { viewModel.navigateCalendarMonth(1) }
                    )
                }
            }
        }

        // Weekday Frequency Section (For ALL habits!)
        item(key = "detail_frequency") {
            WeekdayFrequencySection(
                weekdayStats = state?.weekdayStats ?: emptyList(),
                gridData = state?.weekdayGridData ?: emptyList(),
                weeksWithMonthLabels = state?.weeksWithMonthLabels ?: emptyList(),
                language = language,
                onInfoClick = { t, e -> activeExplanation = t to e }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = if (language == "de") "Habit löschen?" else "Delete Habit?") },
            text = { Text(text = if (language == "de") "Möchtest du diese Gewohnheit wirklich unwiderruflich löschen?" else "Are you sure you want to delete this habit permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(habit)
                        showDeleteConfirm = false
                        Toast.makeText(
                            context,
                            if (language == "de") "Gewohnheit gelöscht" else "Habit deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text(if (language == "de") "Löschen" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = if (language == "de") "Abbrechen" else "Cancel")
                }
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
}
}

@SuppressLint("NewApi")
@Composable
fun WeekdayFrequencySection(
    weekdayStats: List<Triple<Int, Int, Int>>,
    gridData: List<List<Pair<java.time.LocalDate, String>>>,
    weeksWithMonthLabels: List<String>,
    language: String,
    onInfoClick: (String, String) -> Unit
) {
    val daysAbbr = if (language == "de") {
        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Frequency",
                    tint = PrimaryViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == "de") "WOCHENTAGS-FREQUENZ" else "WEEKDAY FREQUENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = if (language == "de") "Wochentags-Frequenz" else "Weekday Frequency",
                    explanation = if (language == "de") {
                        "Zeigt, an welchen Wochentagen du diese Gewohnheit historisch am häufigsten abgeschlossen hast. Je heller der Kreis, desto höher die Erfolgsquote."
                    } else {
                        "Shows on which weekdays you have historically completed this habit most often. The brighter the circle, the higher your completion rate."
                    },
                    onClick = onInfoClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Horizontal circles for each of the 7 weekdays
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekdayStats.forEach { (index, completed, pct) ->
                    val dayName = daysAbbr[index]
                    val circleBgColor = when {
                        pct == 0 -> ProgressTrack
                        pct <= 25 -> SuccessGreen.copy(alpha = 0.25f)
                        pct <= 50 -> SuccessGreen.copy(alpha = 0.5f)
                        pct <= 75 -> SuccessGreen.copy(alpha = 0.75f)
                        else -> SuccessGreen
                    }
                    val textColor = if (pct > 50) DarkBg else TextPrimary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(circleBgColor)
                                .border(1.dp, if (pct == 0) DarkBorder else SuccessGreen.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName.take(2),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Git-style Weekly Heatmap (Last 15 Weeks)
            Text(
                text = if (language == "de") "AKTIVITÄT (LETZTE 15 WOCHEN)" else "ACTIVITY (LAST 15 WEEKS)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // 7 weekday rows with background line and evenly spaced circles
                for (dayIndex in 0 until 7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Subtle background line for track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DarkBorder.copy(alpha = 0.4f))
                            )

                            // Row of circles for each week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                gridData.forEach { weekDays ->
                                    val cell = weekDays[dayIndex]
                                    val status = cell.second
                                    
                                    val isSuccess = status == "SUCCESS"
                                    val size = if (isSuccess) 14.dp else 4.dp
                                    val color = when (status) {
                                        "SUCCESS" -> SuccessGreen
                                        "FAILED" -> ProgressTrack
                                        else -> ProgressTrack.copy(alpha = 0.3f)
                                    }

                                    Box(
                                        modifier = Modifier.size(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(size)
                                                .background(color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Weekday label on the right side
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = daysAbbr[dayIndex].take(2),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Month labels row aligned beautifully with the week columns above
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        gridData.forEachIndexed { index, _ ->
                            val label = weeksWithMonthLabels[index]
                            Box(
                                modifier = Modifier.width(14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (label.isNotEmpty()) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(40.dp)) // Matches 12.dp spacer + 28.dp label
                }
            }
        }
    }
}

@Composable
fun HabitTargetSection(
    state: HabitDetailUiState?,
    language: String
) {
    val stats = state?.targetStats ?: return
    val habit = state.habit

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = "Target",
                    tint = PrimaryViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == "de") "ZIERLEISTUNG (ZIEL)" else "TARGET PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Today, Week, Month, Quarter, Year rows
            val periodLabels = if (language == "de") {
                listOf("Heute", "Woche", "Monat", "Quartal", "Jahr")
            } else {
                listOf("Today", "Week", "Month", "Quarter", "Year")
            }

            val periodStatsList = listOf(
                stats.today,
                stats.week,
                stats.month,
                stats.quarter,
                stats.year
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                periodLabels.zip(periodStatsList).forEach { (label, periodStat) ->
                    TargetProgressRow(
                        label = label,
                        actual = periodStat.actualValue,
                        target = periodStat.targetValue,
                        isNumerical = periodStat.isNumerical,
                        unit = habit.unit,
                        language = language
                    )
                }
            }
        }
    }
}

@Composable
fun TargetProgressRow(
    label: String,
    actual: Float,
    target: Float,
    isNumerical: Boolean,
    unit: String,
    language: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val progressFraction = remember(actual, target) {
            if (target > 0f) (actual / target).coerceIn(0f, 1f) else 0f
        }
        val hasOverachieved = remember(actual, target) {
            target > 0f && actual > target
        }

        val formattedActual = remember(actual) {
            if (actual % 1f == 0f) actual.toInt().toString() else String.format(Locale.US, "%.1f", actual)
        }
        val formattedTarget = remember(target) {
            if (target % 1f == 0f) target.toInt().toString() else String.format(Locale.US, "%.1f", target)
        }
        val unitSuffix = remember(isNumerical, unit) {
            if (isNumerical && unit.isNotEmpty()) " $unit" else ""
        }

        // Row 1: Period label and Value text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period label (e.g. Heute, Woche, etc.)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            // Value text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$formattedActual$unitSuffix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (target > 0f) {
                    val formattedDiff = remember(actual, target) {
                        val diff = actual - target
                        if (diff % 1f == 0f) diff.toInt().toString() else String.format(Locale.US, "%.1f", diff)
                    }
                    val targetLabel = remember(language, formattedTarget, unitSuffix) {
                        if (language == "de") " / Ziel: $formattedTarget$unitSuffix" else " / Target: $formattedTarget$unitSuffix"
                    }

                    Text(
                        text = buildAnnotatedString {
                            append(targetLabel)
                            if (hasOverachieved) {
                                withStyle(style = SpanStyle(color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)) {
                                    append(" (+$formattedDiff$unitSuffix)")
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Text(
                        text = if (language == "de") " (Kein Ziel)" else " (No Target)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DarkBorder)
        ) {
            // Progress bar (colored portion)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (progressFraction > 0f) progressFraction else 0.001f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (hasOverachieved) {
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryViolet, SuccessGreen)
                            )
                        } else {
                            SolidColor(PrimaryViolet)
                        }
                    )
            )
        }
    }
}

@Composable
fun OverallStatsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val overallCalData by viewModel.overallCalendarData.collectAsStateWithLifecycle()
    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()

    val heatmapMonthOffset by viewModel.heatmapMonthOffset.collectAsStateWithLifecycle()
    val heatmapCanPrevMonth by viewModel.heatmapCanPrevMonth.collectAsStateWithLifecycle()
    val heatmapCanNextMonth by viewModel.heatmapCanNextMonth.collectAsStateWithLifecycle()
    val heatmapMonthNameAndYear by viewModel.heatmapMonthNameAndYear.collectAsStateWithLifecycle()
    val heatmapMonthGridData by viewModel.heatmapMonthGridData.collectAsStateWithLifecycle()
    val heatmapYearGridData by viewModel.heatmapYearGridData.collectAsStateWithLifecycle()
    val heatmapYearMonthLabels by viewModel.heatmapYearMonthLabels.collectAsStateWithLifecycle()

    val selectedHeatmapCell by viewModel.selectedHeatmapCell.collectAsStateWithLifecycle()
    val heatmapViewMode by viewModel.heatmapViewMode.collectAsStateWithLifecycle()
    val activeCell by viewModel.activeHeatmapCell.collectAsStateWithLifecycle()
    val formattedDate by viewModel.formattedActiveCellDate.collectAsStateWithLifecycle()

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 16.dp)
        ) {
            // Toolbar
            item(key = "overall_toolbar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "de") "Gesamt-Statistiken" else "Overall Statistics",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Overall Strength Card
            item(key = "overall_strength") {
                val sweepAngleVal = remember(strength) { (strength.toFloat() / 100f) * 360f }
                val strengthText = remember(strength) { "$strength/100" }
                val strengthLabel = remember(strength, language) { getStrengthLabel(strength, language) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(80.dp)
                                    .drawWithCache {
                                        val strokeWidth = 8.dp.toPx()
                                        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        onDrawBehind {
                                            drawArc(
                                                color = ProgressTrack,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = stroke
                                            )
                                            drawArc(
                                                color = SuccessGreen,
                                                startAngle = -90f,
                                                sweepAngle = sweepAngleVal,
                                                useCenter = false,
                                                style = stroke
                                            )
                                        }
                                    }
                            ) {
                                Text(
                                    text = strengthText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Overall Strength",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "de") "GESAMT-STÄRKE" else "OVERALL STRENGTH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    InfoIconButton(
                                        title = if (language == "de") "Gesamt-Stärke Score" else "Overall Strength Score",
                                        explanation = if (language == "de") {
                                            "Die durchschnittliche gewichtete Stärke aller deiner Gewohnheiten zusammen."
                                        } else {
                                            "The average weighted strength score of all of your active habits combined."
                                        },
                                        onClick = { t, e -> activeExplanation = t to e }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strengthLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ProgressEndText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Activity Heatmap Card (GitHub-style, with Month/Year Switcher!)
            item(key = "overall_calendar") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header info row with Month / Year view switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Calendar",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "de") "AKTIVITÄTS-HEATMAP" else "ACTIVITY HEATMAP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                InfoIconButton(
                                    title = if (language == "de") "Aktivitäts-Heatmap" else "Activity Heatmap",
                                    explanation = if (language == "de") {
                                        "Zeigt deinen täglichen Gewohnheitsfortschritt für den ausgewählten Zeitraum, ähnlich wie das GitHub-Beitragssystem. Dunklere grüne Felder stehen für eine höhere Anzahl an abgeschlossenen Gewohnheiten an dem Tag."
                                    } else {
                                        "Shows your daily habit completion progress for the selected timeframe, styled like GitHub's contribution board. Darker green squares represent a higher ratio of completed habits on that day."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            
                            // Beautiful Segmented Control for Month/Year View Mode
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF13131F), RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val optMonth = if (language == "de") "Monat" else "Month"
                                val optYear = if (language == "de") "Jahr" else "Year"
                                
                                listOf("month" to optMonth, "year" to optYear).forEach { (mode, label) ->
                                    val isSelected = heatmapViewMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) PrimaryViolet else Color.Transparent)
                                            .clickable { viewModel.setHeatmapViewMode(mode) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (heatmapViewMode == "month") {
                            // --- MONTH VIEW (Beautifully Large, Fills Available Width, No Scroll!) ---
                            
                            // Month Navigation Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (heatmapCanPrevMonth) {
                                            viewModel.navigateHeatmapMonth(-1)
                                        }
                                    },
                                    enabled = heatmapCanPrevMonth,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = if (heatmapCanPrevMonth) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = heatmapMonthNameAndYear,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        if (heatmapCanNextMonth) {
                                            viewModel.navigateHeatmapMonth(1)
                                        }
                                    },
                                    enabled = heatmapCanNextMonth,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (heatmapCanNextMonth) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Build monthly grid data
                            val gridData = heatmapMonthGridData

                            val cellSize = 38.dp
                            val cellSpacing = 6.dp

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Top
                            ) {
                                // 1. Weekday labels column
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(cellSpacing),
                                    modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                                ) {
                                    val weekdays = if (language == "de") {
                                        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                                    } else {
                                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    }
                                    weekdays.forEach { dayLabel ->
                                        Box(
                                            modifier = Modifier.height(cellSize),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // 2. Large Squares Grid filling remaining width
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    gridData.forEach { weekDays ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(cellSpacing),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            weekDays.forEach { cell ->
                                                if (cell == null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(cellSize)
                                                            .background(Color.Transparent)
                                                    )
                                                } else {
                                                    val ratio = if (cell.total > 0) cell.completed.toFloat() / cell.total.toFloat() else -1f
                                                    val bgColor = when {
                                                        cell.isOutOfRange -> Color.Transparent
                                                        cell.isFuture -> Color(0xFF1A172E).copy(alpha = 0.3f)
                                                        ratio < 0f -> ProgressTrack
                                                        ratio == 0f -> ProgressTrack
                                                        ratio <= 0.25f -> SuccessGreen.copy(alpha = 0.25f)
                                                        ratio <= 0.5f -> SuccessGreen.copy(alpha = 0.5f)
                                                        ratio <= 0.75f -> SuccessGreen.copy(alpha = 0.75f)
                                                        else -> SuccessGreen
                                                    }
                                                    
                                                    val isSelectedSquare = activeCell?.dateStr == cell.dateStr
                                                    val isTodayBorder = cell.isToday
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .size(cellSize)
                                                            .background(bgColor, RoundedCornerShape(8.dp))
                                                            .then(
                                                                if (isSelectedSquare) {
                                                                    Modifier.border(2.5.dp, PrimaryViolet, RoundedCornerShape(8.dp))
                                                                } else if (isTodayBorder) {
                                                                    Modifier.border(1.5.dp, PrimaryViolet.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                                } else Modifier
                                                            )
                                                            .clickable(enabled = !cell.isOutOfRange) {
                                                                viewModel.selectHeatmapCell(cell)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val textColor = when {
                                                            cell.isOutOfRange -> TextSecondary.copy(alpha = 0.3f)
                                                            cell.isFuture || ratio < 0f -> TextSecondary.copy(alpha = 0.6f)
                                                            ratio == 0f -> TextSecondary
                                                            else -> DarkBg
                                                        }
                                                        Text(
                                                            text = cell.day.toString(),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                                            color = textColor,
                                                            fontWeight = FontWeight.Bold
                                                        )

                                                        val hasNote = remember(allDailyNotes, cell.dateStr) {
                                                            allDailyNotes.any { it.date == cell.dateStr && it.content.isNotEmpty() }
                                                        }
                                                        if (hasNote) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(top = 4.dp, end = 4.dp)
                                                                    .size(5.dp)
                                                                    .background(PrimaryViolet, CircleShape)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- YEAR VIEW (Horizontal Scrollable 24-Weeks GitHub board!) ---
                            val yearGridData = heatmapYearGridData
                            val yearMonthLabels = heatmapYearMonthLabels

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Top
                            ) {
                                // 1. Fixed Weekday Labels Column on the left
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(top = 18.dp, end = 8.dp)
                                ) {
                                    val weekdays = if (language == "de") {
                                        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                                    } else {
                                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    }
                                    weekdays.forEach { dayLabel ->
                                        Box(
                                            modifier = Modifier.height(13.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // 2. Horizontally scrollable Content containing Month labels and Week columns
                                val scrollState = rememberScrollState()
                                
                                LaunchedEffect(yearGridData) {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(scrollState)
                                ) {
                                    // Month Labels Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.height(18.dp)
                                    ) {
                                        for (index in 0 until 24) {
                                            val label = yearMonthLabels.firstOrNull { it.first == index }
                                            Box(
                                                modifier = Modifier.width(13.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (label != null) {
                                                    Text(
                                                        text = label.second,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary,
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Weeks Columns Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        yearGridData.forEach { weekDays ->
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                weekDays.forEach { cell ->
                                                    val ratio = if (cell.total > 0) cell.completed.toFloat() / cell.total.toFloat() else -1f
                                                    val bgColor = when {
                                                        cell.isOutOfRange -> Color.Transparent
                                                        cell.isFuture -> Color(0xFF13131F)
                                                        ratio < 0f -> ProgressTrack
                                                        ratio == 0f -> ProgressTrack
                                                        ratio <= 0.25f -> SuccessGreen.copy(alpha = 0.25f)
                                                        ratio <= 0.5f -> SuccessGreen.copy(alpha = 0.5f)
                                                        ratio <= 0.75f -> SuccessGreen.copy(alpha = 0.75f)
                                                        else -> SuccessGreen
                                                    }
                                                    
                                                    val isSelectedSquare = activeCell?.dateStr == cell.dateStr
                                                    val isTodayBorder = cell.isToday
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .size(13.dp)
                                                            .background(bgColor, RoundedCornerShape(3.dp))
                                                            .then(
                                                                if (isSelectedSquare) {
                                                                    Modifier.border(1.5.dp, PrimaryViolet, RoundedCornerShape(3.dp))
                                                                } else if (isTodayBorder) {
                                                                    Modifier.border(1.dp, PrimaryViolet.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                                                } else Modifier
                                                            )
                                                            .clickable(enabled = !cell.isOutOfRange) {
                                                                viewModel.selectHeatmapCell(cell)
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Heatmap Horizontal Legend (GitHub-style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == "de") "Weniger " else "Less ",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            
                            listOf(
                                ProgressTrack,
                                SuccessGreen.copy(alpha = 0.25f),
                                SuccessGreen.copy(alpha = 0.5f),
                                SuccessGreen.copy(alpha = 0.75f),
                                SuccessGreen
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(12.dp)
                                        .background(color, RoundedCornerShape(3.dp))
                                )
                            }
                            
                            Text(
                                text = if (language == "de") " Mehr" else " More",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Tapped cell details container
                        val cell = activeCell
                        if (cell != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val statusText = when {
                                        cell.isFuture -> {
                                            if (language == "de") "Zukünftiger Tag" else "Future day"
                                        }
                                        cell.total == 0 -> {
                                            if (language == "de") "Keine Gewohnheiten an diesem Tag" else "No habits active on this day"
                                        }
                                        else -> {
                                            val pct = (cell.completed.toFloat() / cell.total.toFloat() * 100).toInt()
                                            if (language == "de") {
                                                "${cell.completed} von ${cell.total} Gewohnheiten abgeschlossen ($pct%)"
                                            } else {
                                                "${cell.completed} of ${cell.total} habits completed ($pct%)"
                                            }
                                        }
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (cell.total > 0 && cell.completed == cell.total) SuccessGreen else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (!cell.isFuture) {
                                    TextButton(
                                        onClick = {
                                            viewModel.selectDateAndSyncWeek(cell.dateStr)
                                            onBack()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryViolet)
                                    ) {
                                        Text(
                                            text = if (language == "de") "Anzeigen" else "View",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Go to date",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Overall stats - Top & bottom habits "Läuft super" & "Braucht Fokus"
            item(key = "overall_focus_habits") {
                val topHabits = remember(habitsWithStats) {
                    habitsWithStats.sortedByDescending { it.strength }.take(2)
                }
                val bottomHabits = remember(habitsWithStats) {
                    habitsWithStats.sortedBy { it.strength }.take(2)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // "Läuft super" card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Great performance",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "de") "Läuft super" else "Doing great",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                InfoIconButton(
                                    title = if (language == "de") "Läuft super" else "Doing great",
                                    explanation = if (language == "de") {
                                        "Deine 2 Gewohnheiten mit der höchsten historischen Erfolgsquote (Stärke)."
                                    } else {
                                        "Your 2 habits with the highest historical completion strength."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (topHabits.isEmpty()) {
                                Text(
                                    text = if (language == "de") "Keine Daten" else "No data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    topHabits.forEach { stat ->
                                        val habitColor = remember(stat.habit.color) { HabitIconMapping.getColor(stat.habit.color) }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(habitColor.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, habitColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = HabitIconMapping.getIconDrawableId(stat.habit.icon)),
                                                    contentDescription = null,
                                                    tint = habitColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stat.habit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${stat.strength}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "Braucht Fokus" card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Needs focus",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "de") "Braucht Fokus" else "Needs focus",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                InfoIconButton(
                                    title = if (language == "de") "Braucht Fokus" else "Needs focus",
                                    explanation = if (language == "de") {
                                        "Deine 2 Gewohnheiten mit der niedrigsten historischen Erfolgsquote (Stärke)."
                                    } else {
                                        "Your 2 habits with the lowest historical completion strength."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (bottomHabits.isEmpty()) {
                                Text(
                                    text = if (language == "de") "Keine Daten" else "No data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    bottomHabits.forEach { stat ->
                                        val habitColor = remember(stat.habit.color) { HabitIconMapping.getColor(stat.habit.color) }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(habitColor.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, habitColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = HabitIconMapping.getIconDrawableId(stat.habit.icon)),
                                                    contentDescription = null,
                                                    tint = habitColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stat.habit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${stat.strength}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ErrorRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4 fully spelled-out Metric Cards inside overall screen in a beautifully balanced 2x2 grid
            item(key = "overall_metrics") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Perfect Days Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Perfect Days",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == "de") "Perfekte Tage" else "Perfect Days",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    InfoIconButton(
                                        title = if (language == "de") "Perfekte Tage insgesamt" else "Total Perfect Days",
                                        explanation = if (language == "de") {
                                            "Die Gesamtzahl der Tage, an denen du 100% deiner aktiven Gewohnheiten erfolgreich abgeschlossen hast."
                                        } else {
                                            "The total number of days in your history where you completed 100% of your active habits."
                                        },
                                        onClick = { t, e -> activeExplanation = t to e }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = perfectDaysStats.totalPerfectDays.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Total Completed Logs Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Logs",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == "de") "Einträge" else "Logs/Entries",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    InfoIconButton(
                                        title = if (language == "de") "Erledigte Gewohnheiten" else "Total Logs/Completed",
                                        explanation = if (language == "de") {
                                            "Die Gesamtzahl aller Erledigungen und Einträge über alle Gewohnheiten hinweg."
                                        } else {
                                            "The total number of completions and logs across all of your habits combined."
                                        },
                                        onClick = { t, e -> activeExplanation = t to e }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = profileStats.totalGlobalCompletions.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Perfect Days Streak Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == "de") "Beste Serie" else "Best Streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    InfoIconButton(
                                        title = if (language == "de") "Längste Perfect Streak" else "Longest Perfect Streak",
                                        explanation = if (language == "de") {
                                            "Die längste aufeinanderfolgende Serie an Tagen, an denen du 100% deiner aktiven Gewohnheiten erledigt hast."
                                        } else {
                                            "The longest consecutive streak of days where you completed 100% of your active habits."
                                        },
                                        onClick = { t, e -> activeExplanation = t to e }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = perfectDaysStats.perfectDaysStreak.toString() + if (language == "de") " Tage" else " Days",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Total Completion Rate Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = "Completion Rate",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == "de") "Erfolgsquote" else "Success Rate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    InfoIconButton(
                                        title = if (language == "de") "Gesamt-Erfolgsquote" else "Overall Completion Rate",
                                        explanation = if (language == "de") {
                                            "Die prozentuale Erfolgsquote über all deine Gewohnheiten seit dem Start."
                                        } else {
                                            "The overall percentage of habits completed out of all possible habit occurrences since you started."
                                        },
                                        onClick = { t, e -> activeExplanation = t to e }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${perfectDaysStats.totalCompletionRate}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }


        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }
    }
}

@Composable
fun AchievementCounterCol(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun CalendarDayCell(
    dayNum: String,
    status: String,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        "SUCCESS" -> SuccessGreen
        "FAILED" -> ErrorRed
        "PENDING" -> HabitYellow
        "PAUSED" -> HabitOrange
        else -> PrimaryViolet.copy(alpha = 0.15f) // Soft lila tint for inactive/future days
    }
    val textColor = when (status) {
        "SUCCESS", "FAILED", "PENDING", "PAUSED" -> DarkBg
        else -> if (status == "INACTIVE") TextSecondary else TextPrimary
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNum,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CalendarMonthGrid(
    gridRows: List<List<CalendarCellState?>>,
    monthName: String,
    habitColor: Color,
    language: String,
    canPrevMonth: Boolean,
    canNextMonth: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                enabled = canPrevMonth
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = if (canPrevMonth) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                )
            }
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onNextMonth,
                enabled = canNextMonth
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = if (canNextMonth) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val shortDayNames = if (language == "de") listOf("MO", "DI", "MI", "DO", "FR", "SA", "SO") else listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
            shortDayNames.forEach {
                key(it) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        gridRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (c in 0 until 7) {
                    val cell = if (c < row.size) row[c] else null
                    if (cell != null) {
                        key(cell.id) {
                            CalendarDayCell(
                                dayNum = cell.dayNum,
                                status = cell.status,
                                habitColor = habitColor,
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legende (Erreicht, Gescheitert, Ausstehend, Pausiert) - Screen-resilient 2x2 centered grid
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (language == "de") "Erreicht" else "Completed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (language == "de") "Gescheitert" else "Failed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(HabitYellow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (language == "de") "Ausstehend" else "Pending", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(HabitOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (language == "de") "Pausiert" else "Paused", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Immutable
data class MilestoneItem(
    val target: Int,
    val isUnlocked: Boolean,
    val text: String
)

data class RankTier(
    val nameDe: String,
    val nameEn: String,
    val color: Color,
    val icon: ImageVector
)

fun getRankTierForTarget(target: Int): RankTier {
    return when (target) {
        7 -> RankTier("Holz", "Wood", Color(0xFF8B5A2B), Icons.Default.Terrain)
        14 -> RankTier("Bronze", "Bronze", Color(0xFFCD7F32), Icons.Default.WorkspacePremium)
        30 -> RankTier("Silber", "Silver", Color(0xFFC0C0C0), Icons.Default.WorkspacePremium)
        60 -> RankTier("Gold", "Gold", Color(0xFFFFD700), Icons.Default.EmojiEvents)
        100 -> RankTier("Platin", "Platinum", Color(0xFFE5E4E2), Icons.Default.MilitaryTech)
        180 -> RankTier("Diamant", "Diamond", Color(0xFF00E5FF), Icons.Default.Diamond)
        270 -> RankTier("Rubin", "Ruby", Color(0xFFFF1744), Icons.Default.OfflineBolt)
        365 -> RankTier("Meister", "Master", Color(0xFFD500F9), Icons.Default.AutoAwesome)
        500 -> RankTier("Legende", "Legend", Color(0xFFFF9100), Icons.Default.Stars)
        1000 -> RankTier("Unreal", "Unreal", Color(0xFF76FF03), Icons.Default.Whatshot)
        else -> RankTier("Rang", "Rank", Color.White, Icons.Default.EmojiEvents)
    }
}

@Composable
fun MilestoneBadge(milestone: MilestoneItem, language: String) {
    val rank = getRankTierForTarget(milestone.target)
    val color = if (milestone.isUnlocked) rank.color else TextSecondary.copy(alpha = 0.3f)
    val context = LocalContext.current
    val rankName = if (language == "de") rank.nameDe else rank.nameEn

    val scale by animateFloatAsState(
        targetValue = if (milestone.isUnlocked) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "milestone_scale"
    )

    var showConfetti by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable {
                        if (milestone.isUnlocked) {
                            showConfetti = true
                            Toast.makeText(
                                context,
                                if (language == "de") "Erreicht: $rankName!" else "Unlocked: $rankName!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                if (language == "de") "Noch gesperrt: $rankName (${milestone.target} Tage)" else "Locked: $rankName (${milestone.target} days)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val tierStr = when (milestone.target) {
                    7 -> "WOOD"
                    14 -> "BRONZE"
                    30 -> "SILVER"
                    60 -> "GOLD"
                    100 -> "PLATINUM"
                    180 -> "DIAMOND"
                    270 -> "RUBY"
                    365 -> "MASTER"
                    500 -> "LEGEND"
                    1000 -> "UNREAL"
                    else -> "WOOD"
                }

                AchievementBadge(
                    type = "STREAK",
                    tier = tierStr,
                    isUnlocked = milestone.isUnlocked,
                    modifier = Modifier.fillMaxSize()
                )

                if (!milestone.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(DarkCard, CircleShape)
                            .border(1.5.dp, TextSecondary.copy(alpha = 0.4f), CircleShape)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = TextSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = milestone.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (milestone.isUnlocked) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        if (showConfetti && milestone.isUnlocked) {
            ConfettiAnimation(
                onFinished = { showConfetti = false }
            )
        }
    }
}

@Composable
fun StreakMilestonesCard(longestStreak: Int, language: String) {
    val milestoneTargets = remember { listOf(7, 14, 30, 60, 100, 180, 270, 365, 500, 1000) }
    val items = remember(longestStreak) {
        milestoneTargets.map { target ->
            MilestoneItem(
                target = target,
                isUnlocked = longestStreak >= target,
                text = "$target"
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = "Milestones",
                    tint = PrimaryViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == "de") "ERFOLGSSTRÄHNE" else "STREAK MILESTONES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val chunks = items.chunked(5)
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { milestone ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                MilestoneBadge(milestone = milestone, language = language)
                            }
                        }
                    }
                }
            }
        }
    }
}

// SETTINGS SCREEN
@Composable
fun SettingsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val backupFolderUri by viewModel.backupFolderUri.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    var showWipeConfirm by remember { mutableStateOf(false) }
    var showArchivedList by remember { mutableStateOf(false) }

    if (showArchivedList) {
        val archivedHabits by viewModel.archivedHabits.collectAsStateWithLifecycle()
        var showDeleteConfirmInArchive by remember { mutableStateOf<Habit?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showArchivedList = false }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (language == "de") "Archiv" else "Archive",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (language == "de") {
                    "Archivierte Gewohnheiten sind pausiert und werden nicht im Dashboard oder in Statistiken angezeigt. Du kannst sie jederzeit wieder aktivieren."
                } else {
                    "Archived habits are paused and do not appear in the dashboard or statistics. You can reactivate them at any time."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            )

            if (archivedHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == "de") "Keine archivierten Gewohnheiten." else "No archived habits.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(archivedHabits, key = { it.id }) { habit ->
                        val habitColor = remember(habit.color) { HabitIconMapping.getColor(habit.color) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(habitColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = HabitIconMapping.getIconDrawableId(habit.icon)),
                                            contentDescription = null,
                                            tint = habitColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = habit.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reaktivieren Button
                                    IconButton(
                                        onClick = {
                                            viewModel.unarchiveHabit(habit)
                                            Toast.makeText(
                                                context,
                                                if (language == "de") "${habit.name} reaktiviert" else "${habit.name} reactivated",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Restore",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Löschen Button
                                    IconButton(
                                        onClick = { showDeleteConfirmInArchive = habit },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmInArchive != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmInArchive = null },
                containerColor = DarkCard,
                title = { Text(text = if (language == "de") "Gewohnheit endgültig löschen?" else "Delete Habit Permanently?") },
                text = { Text(text = if (language == "de") "Möchtest du '${showDeleteConfirmInArchive?.name}' wirklich unwiderruflich löschen? Alle Verlaufsdaten gehen verloren." else "Are you sure you want to delete '${showDeleteConfirmInArchive?.name}' permanently? All tracking history will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val h = showDeleteConfirmInArchive!!
                            viewModel.deleteHabit(h)
                            showDeleteConfirmInArchive = null
                            Toast.makeText(
                                context,
                                if (language == "de") "Gewohnheit gelöscht" else "Habit deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(if (language == "de") "Löschen" else "Delete", color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmInArchive = null }) {
                        Text(if (language == "de") "Abbrechen" else "Cancel", color = TextSecondary)
                    }
                }
            )
        }
    } else {

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.saveBackupFolderUri(uri.toString())
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    viewModel.triggerManualRestore(jsonStr)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (language == "de") "Einstellungen" else "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Language setup card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "de") "Sprache" else "Language",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setLanguage("de") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (language == "de") PrimaryViolet else ProgressTrack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Deutsch")
                        }
                        Button(
                            onClick = { viewModel.setLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (language == "en") PrimaryViolet else ProgressTrack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("English")
                        }
                    }
                }
            }
        }

        // Archivierte Gewohnheiten card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .clickable { showArchivedList = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Archive",
                            tint = HabitOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == "de") "Archivierte Gewohnheiten" else "Archived Habits",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == "de") "Inaktive Gewohnheiten reaktivieren" else "Reactivate suspended habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }



        // SAF Backup & Restore configuration card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "de") "Lokales SAF Backup & Restore" else "Local SAF Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (language == "de") {
                            "Wähle einen Ordner aus. Die App erstellt dort täglich automatisch ein Backup der letzten 3 Tage. Du kannst auch jederzeit manuell sichern oder wiederherstellen."
                        } else {
                            "Select a local folder. The app will automatically save daily JSON exports there (retaining only the 3 latest). You can also back up or restore manually."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected folder info
                    Text(
                        text = if (language == "de") "Ausgewählter Ordner:" else "Selected Folder:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProgressTrack)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (backupFolderUri.isEmpty()) {
                                if (language == "de") "Kein Ordner ausgewählt" else "No folder selected"
                            } else {
                                backupFolderUri
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (backupFolderUri.isEmpty()) ErrorRed else SuccessGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { folderLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Folder")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "de") "Ordner auswählen" else "Select Folder")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerManualBackup() },
                            enabled = backupFolderUri.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = "Backup")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "de") "Jetzt Sichern" else "Backup Now")
                        }

                        Button(
                            onClick = { fileLauncher.launch(arrayOf("application/json", "*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "de") "Einspielen" else "Restore File")
                        }
                    }

                    if (syncStatus != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = syncStatus ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearSyncStatus() }
                        )
                    }
                }
            }
        }

        // Wipe Data card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "de") "Gefahrenbereich" else "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showWipeConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Wipe")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "de") "Alles löschen" else "Wipe All Data")
                    }
                }
            }
        }

        // Community & Support card
        item {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "de") "Community & Support" else "Community & Support",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // GitHub Link Button
                    Button(
                        onClick = { uriHandler.openUri("https://github.com/FrequeNCy144/Frequent-Habits") },
                        colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub",
                            tint = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "GitHub Repository" else "GitHub Repository",
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Liberapay Link Button
                    Button(
                        onClick = { uriHandler.openUri("https://liberapay.com/FrequeNCy/donate") },
                        colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Donate",
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "de") "Spenden via Liberapay" else "Donate via Liberapay",
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Dedication (Widmung) at the very bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "„Für Saskia. Du bist mein Liebling-Feature im Leben.“",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Light
                    ),
                    color = TextSecondary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text(text = if (language == "de") "ALLE DATEN LÖSCHEN?" else "WIPE ALL DATA?") },
            text = { Text(text = if (language == "de") "Möchtest du wirklich alle angelegten Gewohnheiten und Log-Einträge restlos entfernen? Das kann nicht rückgängig gemacht werden!" else "Are you sure you want to clear all habits and historic progress permanently? This action cannot be undone!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllData()
                        showWipeConfirm = false
                        Toast.makeText(
                            context,
                            if (language == "de") "Alle Daten gelöscht" else "All data wiped",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text(if (language == "de") "Ja, Löschen" else "Yes, Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text(if (language == "de") "Abbrechen" else "Cancel")
                }
            }
        )
    }
    }
}

// CREATE HABIT SCREEN
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateHabitScreen(
    language: String,
    onDismiss: () -> Unit,
    onSave: (name: String, isNegative: Boolean, category: String, icon: String, color: String, type: String, unit: String, target: Float, freq: String, startMs: Long, specificDays: String, reminderEnabled: Boolean, reminderHour: Int, reminderMinute: Int) -> Unit,
    editingHabit: Habit? = null
) {
    var isNegative by remember { mutableStateOf(editingHabit?.isNegative ?: false) } // Aufbauen = false, Abgewöhnen = true
    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var name by remember { mutableStateOf(editingHabit?.name ?: "") }
    var category by remember { mutableStateOf(editingHabit?.category ?: (if (language == "de") "Allgemein" else "General")) }
    var selectedIcon by remember { mutableStateOf(editingHabit?.icon ?: "sparkle") }
    var selectedColor by remember { mutableStateOf(editingHabit?.color ?: "purple") }
    var type by remember { mutableStateOf(editingHabit?.type ?: "BINARY") } // "BINARY" or "NUMBER"
    var unit by remember { mutableStateOf(editingHabit?.unit ?: (if (language == "de") "Minuten" else "Minutes")) }
    var selectedChip by remember {
        val standardUnitsDe = listOf("Minuten", "Liter", "ml", "km", "Stunden", "Mal")
        val standardUnitsEn = listOf("Minutes", "Liters", "ml", "km", "Hours", "Times")
        val initialChip = when {
            unit.isEmpty() -> if (language == "de") "Minuten" else "Minutes"
            language == "de" && unit in standardUnitsDe -> unit
            language != "de" && unit in standardUnitsEn -> unit
            unit.lowercase() in listOf("minuten", "minutes", "min") -> if (language == "de") "Minuten" else "Minutes"
            unit.lowercase() in listOf("liter", "liters", "l") -> if (language == "de") "Liter" else "Liters"
            unit.lowercase() == "ml" -> "ml"
            unit.lowercase() == "km" -> "km"
            unit.lowercase() in listOf("stunden", "hours", "h") -> if (language == "de") "Stunden" else "Hours"
            unit.lowercase() in listOf("mal", "times") -> if (language == "de") "Mal" else "Times"
            else -> if (language == "de") "Anderes..." else "Custom..."
        }
        mutableStateOf(initialChip)
    }
    var targetValueStr by remember { mutableStateOf(editingHabit?.targetValue?.toInt()?.toString() ?: "1") }
    var frequency by remember { mutableStateOf(editingHabit?.frequency ?: "DAILY") }
    var timesWeekly by remember {
        val initialTimes = if (editingHabit?.frequency == "TIMES_WEEKLY") {
            editingHabit.specificDays.toIntOrNull() ?: 3
        } else {
            3
        }
        mutableStateOf(initialTimes)
    }
    var specificDaysSet by remember {
        val initialSet = editingHabit?.specificDays
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet() ?: emptySet()
        mutableStateOf(initialSet)
    }
    var reminderEnabled by remember { mutableStateOf(editingHabit?.reminderEnabled ?: false) }
    var reminderHour by remember { mutableStateOf(editingHabit?.reminderHour ?: 18) }
    var reminderMinute by remember { mutableStateOf(editingHabit?.reminderMinute ?: 0) }
    
    // Default start date is today or habit's startDate (cleared to midnight for new habits so they start today!)
    val initialDateMillis = remember {
        if (editingHabit != null) {
            editingHabit.startDate
        } else {
            java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }
    var startDateMillis by remember { mutableStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedStartDate = remember(startDateMillis) {
        val localDate = java.time.Instant.ofEpochMilli(startDateMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d. MMMM yyyy", java.util.Locale.GERMANY)
        localDate.format(formatter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header top bar with Back Arrow and smaller header size matching displayMedium
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag("create_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingHabit != null) {
                        if (language == "de") "Gewohnheit bearbeiten" else "Edit Habit"
                    } else {
                        if (language == "de") "Neue Gewohnheit" else "New Habit"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text(if (language == "de") "z.B. Meditieren" else "e.g., Meditate") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = PrimaryViolet
                    ),
                    singleLine = true
                )

                // Goal Segment (Aufbauen / Abgewöhnen) - highly compact Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == "de") "Ziel" else "Goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    InfoIconButton(
                        title = if (language == "de") "Gewohnheitstyp" else "Habit Type",
                        explanation = if (language == "de") {
                            "Aufbauende Gewohnheit: Perfekt für neue Routinen (z.B. Sport, Meditation). Jeder Log erhöht deinen Fortschritt bis zum Tagesziel.\n\nAbgewöhnende Gewohnheit: Perfekt um schlechte Angewohnheiten loszuwerden (z.B. Rauchen). Standardmäßig als 'Erfolgreich' markiert, solange du dein Limit nicht überschreitest."
                        } else {
                            "Building Habit: Ideal for establishing new routines (e.g., exercise, meditation). Each log increases progress toward your daily goal.\n\nQuitting Habit: Ideal for breaking bad habits (e.g., smoking). Marked as 'Successful' by default as long as you stay within limits."
                        },
                        onClick = { t, e -> activeExplanation = t to e }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isNegative = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isNegative) PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Build Up", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == "de") "Aufbauen" else "Build", style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    Button(
                        onClick = { isNegative = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNegative) PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Quit Down", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == "de") "Abgewöhnen" else "Quit", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                // Icons selector
                Text(
                    text = if (language == "de") "Icon auswählen" else "Select Icon",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                val icons = HabitIconMapping.iconList
                val activeColor = HabitIconMapping.getColor(selectedColor)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    icons.forEach { (key, _) ->
                        val isSelected = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (isSelected) activeColor.copy(alpha = 0.2f) else ProgressTrack,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) activeColor else DarkBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = HabitIconMapping.getIconDrawableId(key)),
                                contentDescription = key,
                                tint = if (isSelected) activeColor else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Color selector dots
                Text(
                    text = if (language == "de") "Farbe auswählen" else "Select Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HabitIconMapping.colorList.forEach { (key, colorObj) ->
                        val isSelected = selectedColor == key
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(colorObj.copy(alpha = 0.15f), CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) colorObj else colorObj.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 24.dp else 28.dp)
                                    .background(colorObj, CircleShape)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = TextPrimary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

                // Type Toggle (Ja/Nein vs Zahlenbasiert)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { type = "BINARY" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "BINARY") PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(if (language == "de") "Ja / Nein" else "Yes / No", style = MaterialTheme.typography.titleSmall)
                    }

                    Button(
                        onClick = { type = "NUMBER" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "NUMBER") PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(if (language == "de") "Zahlenbasiert" else "Numeric", style = MaterialTheme.typography.titleSmall)
                    }
                }

                if (type == "NUMBER") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (language == "de") "Einheit auswählen" else "Select Unit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        val standardUnits = if (language == "de") {
                            listOf("Minuten", "Liter", "ml", "km", "Stunden", "Mal", "Anderes...")
                        } else {
                            listOf("Minutes", "Liters", "ml", "km", "Hours", "Times", "Custom...")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            standardUnits.forEach { standardUnit ->
                                val isSelected = selectedChip == standardUnit
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        selectedChip = standardUnit
                                        if (standardUnit != "Anderes..." && standardUnit != "Custom...") {
                                            unit = standardUnit
                                        } else {
                                            unit = ""
                                        }
                                    },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (standardUnit.lowercase() in listOf("minuten", "minutes")) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                                )
                                            }
                                            Text(standardUnit)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryViolet,
                                        selectedLabelColor = TextPrimary,
                                        selectedLeadingIconColor = TextPrimary,
                                        containerColor = ProgressTrack,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = DarkBorder,
                                        selectedBorderColor = PrimaryViolet
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (selectedChip == "Anderes..." || selectedChip == "Custom...") {
                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = { unit = it },
                                    label = { Text(if (language == "de") "Eigene Einheit" else "Custom Unit") },
                                    placeholder = { Text(if (language == "de") "z. B. Tassen" else "e.g. cups") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryViolet,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedLabelColor = PrimaryViolet,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = targetValueStr,
                                onValueChange = { targetValueStr = it },
                                label = { Text(if (language == "de") "Tagesziel" else "Daily Target") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryViolet,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedLabelColor = PrimaryViolet,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }
                }

                // Frequenz selector section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val freqOptions = listOf(
                        "DAILY" to (if (language == "de") "Täglich" else "Daily"),
                        "TIMES_WEEKLY" to (if (language == "de") "X mal/Wo" else "X times/Wk"),
                        "SPECIFIC" to (if (language == "de") "Tage" else "Days")
                    )
                    freqOptions.forEach { (key, label) ->
                        val isSelected = frequency == key
                        Button(
                            onClick = { 
                                frequency = key 
                                if (key == "SPECIFIC" && specificDaysSet.isEmpty()) {
                                    specificDaysSet = setOf(1, 2, 3, 4, 5, 6, 7)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryViolet else ProgressTrack,
                                contentColor = if (isSelected) TextPrimary else TextSecondary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        }
                    }
                }

                if (frequency == "TIMES_WEEKLY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..7).forEach { num ->
                            val isSelected = timesWeekly == num
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (isSelected) SuccessGreen else ProgressTrack,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        timesWeekly = num
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = num.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBg else TextPrimary
                                )
                            }
                        }
                    }
                }

                if (frequency == "SPECIFIC") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val weekdays = if (language == "de") {
                            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                        } else {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        }
                        weekdays.forEachIndexed { index, dayName ->
                            val dayNum = index + 1
                            val isSelected = specificDaysSet.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (isSelected) SuccessGreen else ProgressTrack,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        specificDaysSet = if (isSelected) {
                                            specificDaysSet - dayNum
                                        } else {
                                            specificDaysSet + dayNum
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayName.take(2),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBg else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Compact Daily Reminder Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = ProgressTrack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "de") "Erinnerung" else "Reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryViolet,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkBg
                                )
                            )
                        }

                        if (reminderEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour Picker
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { reminderHour = (reminderHour + 23) % 24 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease Hour",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = reminderHour.toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { reminderHour = (reminderHour + 1) % 24 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase Hour",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(":", color = TextPrimary, fontWeight = FontWeight.Bold)

                                // Minute Picker
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { reminderMinute = (reminderMinute + 55) % 60 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease Minute",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = reminderMinute.toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { reminderMinute = (reminderMinute + 5) % 60 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase Minute",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact Start Date Trigger Row
                Card(
                    colors = CardDefaults.cardColors(containerColor = ProgressTrack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = !showDatePicker }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = PrimaryViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = formattedStartDate, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Open Date Picker", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                if (showDatePicker) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        SimpleDatePickerView(
                            initialTimeMs = startDateMillis,
                            onDateSelected = {
                                startDateMillis = it
                                showDatePicker = false
                            }
                        )
                    }
                }

                // Action buttons (Speichern, Abbrechen)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(if (language == "de") "Abbrechen" else "Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val targetVal = targetValueStr.toFloatOrNull() ?: 1f
                                val specDays = when (frequency) {
                                    "SPECIFIC" -> specificDaysSet.sorted().joinToString(",")
                                    "TIMES_WEEKLY" -> timesWeekly.toString()
                                    else -> ""
                                }
                                onSave(name, isNegative, category, selectedIcon, selectedColor, type, unit, targetVal, frequency, startDateMillis, specDays, reminderEnabled, reminderHour, reminderMinute)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_habit_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(if (language == "de") "Speichern" else "Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }
    }
}

// COMPACT CUSTOM DATE PICKER
@Composable
fun SimpleDatePickerView(initialTimeMs: Long, onDateSelected: (Long) -> Unit) {
    val initialDate = remember(initialTimeMs) {
        java.time.Instant.ofEpochMilli(initialTimeMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    }
    var currentMonth by remember { mutableStateOf(initialDate.monthValue) } // 1..12
    var currentYear by remember { mutableStateOf(initialDate.year) }

    val monthNames = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val yearMonth = remember(currentMonth, currentYear) {
        java.time.YearMonth.of(currentYear, currentMonth)
    }
    val firstDayOfMonth = remember(yearMonth) {
        yearMonth.atDay(1)
    }
    val offset = remember(firstDayOfMonth) {
        (firstDayOfMonth.dayOfWeek.value - 1) // 0 is Monday, ..., 6 is Sunday
    }
    val maxDays = remember(yearMonth) {
        yearMonth.lengthOfMonth()
    }

    val gridItems = remember(offset, maxDays) {
        val list = mutableListOf<Int?>()
        for (i in 0 until offset) {
            list.add(null)
        }
        for (i in 1..maxDays) {
            list.add(i)
        }
        list
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth == 1) {
                        currentMonth = 12
                        currentYear--
                    } else {
                        currentMonth--
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", tint = TextPrimary)
            }

            Text(
                text = "${monthNames[currentMonth]} $currentYear",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    if (currentMonth == 12) {
                        currentMonth = 1
                        currentYear++
                    } else {
                        currentMonth++
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val rowItems = remember(gridItems) { gridItems.chunked(7) }
        rowItems.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = if (c < week.size) week[c] else null
                    if (day != null) {
                        val isSelected = initialDate.dayOfMonth == day &&
                                initialDate.monthValue == currentMonth &&
                                initialDate.year == currentYear

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryViolet else Color.Transparent)
                                .clickable {
                                    val localDate = java.time.LocalDate.of(currentYear, currentMonth, day)
                                    val epochMs = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    onDateSelected(epochMs)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.8f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

fun getEncouragementText(completed: Int, total: Int, language: String): String {
    if (total == 0) {
        val german = listOf(
            "Heute ist ein wunderbarer Tag, um eine neue Gewohnheit zu starten! 🌟",
            "Jeder Tag ist eine neue Chance, über dich hinauszuwachsen.",
            "Fang heute an und danke dir selbst morgen.",
            "Der beste Zeitpunkt zu starten ist genau jetzt.",
            "Kleine Schritte führen zu großen Veränderungen. Erstelle deine erste Gewohnheit!",
            "Träume nicht dein Leben, sondern gestalte deine Routinen.",
            "Ein neues Kapitel beginnt mit einer einzigen Entscheidung.",
            "Deine Zukunft wird durch deine heutigen Gewohnheiten geformt."
        )
        val english = listOf(
            "Today is a wonderful day to start a new habit! 🌟",
            "Every day is a new chance to grow beyond yourself.",
            "Start today and thank yourself tomorrow.",
            "The best time to start is right now.",
            "Small steps lead to big changes. Create your first habit!",
            "Don't dream your life, design your routines.",
            "A new chapter starts with a single decision.",
            "Your future is shaped by your habits today."
        )
        val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
        return if (language == "de") german[index] else english[index]
    }
    
    val fraction = completed.toFloat() / total
    return when {
        fraction == 0f -> {
            val german = listOf(
                "Fang klein an – heute ist der perfekte Tag für den ersten Schritt! 🚀",
                "Der erste Schritt ist immer der schwerste. Du schaffst das!",
                "Ein kleiner Schritt heute ist der Anfang eines großen Weges.",
                "Zögere nicht – mach einfach den ersten kleinen Haken für heute.",
                "Motivation bringt dich in Gang. Gewohnheit hält dich am Laufen.",
                "Die geheime Zutat des Erfolgs ist das Anfangen.",
                "Jeder Weg von tausend Meilen beginnt mit einem einzigen Schritt.",
                "Du musst nicht perfekt sein, du musst nur anfangen."
            )
            val english = listOf(
                "Start small - today is the perfect day for the first step! 🚀",
                "The first step is always the hardest. You can do this!",
                "A small step today is the beginning of a great journey.",
                "Don't hesitate - just check off that first tiny step for today.",
                "Motivation is what gets you started. Habit is what keeps you going.",
                "The secret of getting ahead is getting started.",
                "The journey of a thousand miles begins with a single step.",
                "You don't have to be perfect, you just have to start."
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
        fraction < 0.5f -> {
            val german = listOf(
                "Schritt für Schritt vorwärts! 🚀 Jeder Fortschritt zählt.",
                "Du bist gestartet! Bleib dran, es lohnt sich.",
                "Jede erledigte Gewohnheit bringt dich deinem Ziel näher.",
                "Konsequenz ist der Schlüssel zum Erfolg. Mach weiter so!",
                "Kleine Erfolge summieren sich. Jeder Haken ist ein Sieg!",
                "Du bist auf dem Weg. Lass dich nicht aufhalten.",
                "Besser 1% Fortschritt als 0%. Dranbleiben!",
                "Dein zukünftiges Ich klatscht bereits Beifall."
            )
            val english = listOf(
                "Step by step forward! 🚀 Every progress counts.",
                "You have started! Keep going, it's worth it.",
                "Every completed habit brings you closer to your goal.",
                "Consistency is the key to success. Keep it up!",
                "Small wins add up. Every checkmark is a victory!",
                "You are on your way. Don't let anything stop you.",
                "Better 1% progress than 0%. Keep pushing!",
                "Your future self is already applauding."
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
        fraction < 1.0f -> {
            val german = listOf(
                "Du bist auf einem fantastischen Weg! Fast geschafft für heute! 💪",
                "Großartige Arbeit! Nur noch ein kleines Stück.",
                "Spürst du die Energie? Du machst das hervorragend heute!",
                "Du ziehst es heute wirklich durch. Dranbleiben für den perfekten Tag!",
                "Die Ziellinie ist in Sicht. Gib jetzt noch mal alles!",
                "Unglaubliche Disziplin heute! Mach den Tag perfekt.",
                "Du bist fast am Ziel. Lass uns den Tag gemeinsam krönen!",
                "Deine Routine wird immer stärker. Fast vollendet!"
            )
            val english = listOf(
                "You are on a fantastic path! Almost done for today! 💪",
                "Great work! Just a little bit more.",
                "Do you feel the energy? You are doing outstandingly today!",
                "You are really pulling through today. Keep going for a perfect day!",
                "The finish line is in sight. Give it your all!",
                "Incredible discipline today! Make today perfect.",
                "You are almost there. Let's crown the day together!",
                "Your routine is growing stronger. Almost complete!"
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
        else -> {
            val german = listOf(
                "Tag perfekt gemeistert! ✨ Du bist unaufhaltsam!",
                "Alle Gewohnheiten erledigt! Du kannst stolz auf dich sein. 🎉",
                "100% geschafft! Ein perfekter Tag für deine persönliche Entwicklung.",
                "Hervorragend! Du hast heute alles gegeben und gewonnen!",
                "Du hast die Messlatte hoch gelegt. Atemberaubende Leistung! 🏆",
                "Perfekter Tag! Deine Disziplin ist deine Superkraft.",
                "Alles abgehakt! Genieße deinen wohlverdienten Feierabend.",
                "Meisterhaft! Du beweist dir selbst jeden Tag, was in dir steckt."
            )
            val english = listOf(
                "Day crushed! ✨ You are unstoppable!",
                "All habits completed! You can be proud of yourself. 🎉",
                "100% completed! A perfect day for your personal growth.",
                "Outstanding! You gave it your all today and won!",
                "You've set the bar high. Breathtaking performance! 🏆",
                "Perfect day! Your discipline is your superpower.",
                "Everything checked! Enjoy your well-deserved evening.",
                "Masterful! You prove to yourself every day what you are capable of."
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
    }
}

fun getStrengthLabel(strength: Int, language: String): String {
    return when {
        strength < 30 -> if (language == "de") "Ausstehend - Auf geht's!" else "Pending - Let's go!"
        strength < 60 -> if (language == "de") "Mittelmäßig - Dranbleiben!" else "Fair - Keep it up!"
        strength < 85 -> if (language == "de") "Solide - Starker Einsatz! \uD83D\uDCAA" else "Solid - Going strong! \uD83D\uDCAA"
        else -> if (language == "de") "Exzellent - Unaufhaltsam! \uD83D\uDD25" else "Excellent - Unstoppable! \uD83D\uDD25"
    }
}

data class Achievement(
    val type: String, // "STREAK", "COMPLETIONS", "PERFECT_DAYS"
    val tier: String, // e.g. "WOOD", "COMP_10", "PERF_7" etc.
    val title: String,
    val description: String,
    val habitName: String? = null,
    val habitColor: Color? = null,
    val targetValue: Int,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false
)

@Composable
fun CategoryHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    explanationTitle: String? = null,
    explanationText: String? = null,
    onInfoClick: ((String, String) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        if (explanationTitle != null && explanationText != null && onInfoClick != null) {
            InfoIconButton(
                title = explanationTitle,
                explanation = explanationText,
                onClick = onInfoClick
            )
        }
    }
}

@Composable
fun HabitStreakAchievementCard(
    habitName: String,
    habitColor: Color,
    habitIcon: String,
    longestStreak: Int,
    language: String
) {
    val context = LocalContext.current
    val targets = listOf(7, 14, 30, 100)
    val tiers = listOf("WOOD", "BRONZE", "SILVER", "GOLD")
    val tierNamesDe = listOf("Holz-Streak", "Bronze-Streak", "Silber-Streak", "Gold-Streak")
    val tierNamesEn = listOf("Wood Streak", "Bronze Streak", "Silver Streak", "Gold Streak")

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Habit Name and Category Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(habitColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = HabitIconMapping.getIconDrawableId(habitIcon)),
                        contentDescription = null,
                        tint = habitColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habitName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == "de") "Beste Serie: $longestStreak Tage" else "Longest Streak: $longestStreak Days",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar to next streak milestone
            val nextTargetIndex = when {
                longestStreak < 7 -> 0
                longestStreak < 14 -> 1
                longestStreak < 30 -> 2
                longestStreak < 100 -> 3
                else -> -1
            }

            val (labelText, progressFraction, percent) = if (nextTargetIndex != -1) {
                val prevTarget = if (nextTargetIndex == 0) 0 else targets[nextTargetIndex - 1]
                val nextTarget = targets[nextTargetIndex]
                val targetName = if (language == "de") {
                    listOf("Holz-Serie", "Bronze-Serie", "Silber-Serie", "Gold-Serie")[nextTargetIndex]
                } else {
                    listOf("Wood Streak", "Bronze Streak", "Silver Streak", "Gold Streak")[nextTargetIndex]
                }
                
                val range = nextTarget - prevTarget
                val earned = longestStreak - prevTarget
                val fraction = (earned.toFloat() / range.toFloat()).coerceIn(0f, 1f)
                val pct = (fraction * 100).toInt()
                
                val label = if (language == "de") {
                    "Weg zur $targetName (${longestStreak}/${nextTarget} Tage)"
                } else {
                    "Path to $targetName (${longestStreak}/${nextTarget} Days)"
                }
                Triple(label, fraction, pct)
            } else {
                val label = if (language == "de") {
                    "Gold-Serie meisterhaft erreicht! 🎉"
                } else {
                    "Gold Streak mastered! 🎉"
                }
                Triple(label, 1f, 100)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = habitColor,
                trackColor = ProgressTrack.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Row of Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                targets.forEachIndexed { idx, target ->
                    val isUnlocked = longestStreak >= target
                    val tierStr = tiers[idx]
                    val badgeTitle = if (language == "de") tierNamesDe[idx] else tierNamesEn[idx]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (isUnlocked) {
                                    Toast.makeText(
                                        context,
                                        if (language == "de") "Freigeschaltet: $badgeTitle ($target Tage)!" else "Unlocked: $badgeTitle ($target days)!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        if (language == "de") "Noch gesperrt: $badgeTitle ($target Tage benötigt, aktuell: $longestStreak)" else "Locked: $badgeTitle ($target days required, current: $longestStreak)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AchievementBadge(
                                type = "STREAK",
                                tier = tierStr,
                                isUnlocked = isUnlocked,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(DarkCard, CircleShape)
                                        .border(1.dp, TextSecondary.copy(alpha = 0.4f), CircleShape)
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == "de") "${target} Tage" else "${target} Days",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalAchievementCard(
    type: String, // "COMPLETIONS" or "PERFECT_DAYS"
    tier: String, // "COMP_10", etc.
    title: String,
    description: String,
    targetValue: Int,
    currentValue: Int,
    isUnlocked: Boolean,
    language: String
) {
    val borderTint = when (tier) {
        "COMP_10" -> Color(0xFF4CAF50)
        "COMP_50" -> Color(0xFF2196F3)
        "COMP_200" -> Color(0xFF9C27B0)
        "COMP_500" -> Color(0xFFFF5722)
        "PERF_7" -> Color(0xFF00BCD4)
        "PERF_30" -> Color(0xFFE91E63)
        "PERF_100" -> Color(0xFFFFD700)
        else -> Color(0xFFFFD700)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) DarkCard else DarkCard.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) borderTint.copy(alpha = 0.4f) else DarkBorder.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AchievementBadge(
                type = type,
                tier = tier,
                isUnlocked = isUnlocked,
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isUnlocked) TextPrimary else TextPrimary.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnlocked) {
                        Text(
                            text = if (language == "de") "Freigeschaltet" else "Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnlocked) TextSecondary else TextSecondary.copy(alpha = 0.5f)
                )

                if (!isUnlocked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val progressFraction = if (targetValue > 0) {
                        (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
                    } else 0f
                    val percent = (progressFraction * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "de") {
                                "Fortschritt: $currentValue / $targetValue"
                            } else {
                                "Progress: $currentValue / $targetValue"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = borderTint.copy(alpha = 0.6f),
                        trackColor = ProgressTrack.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: HabitsViewModel,
    language: String,
    onSettingsClick: () -> Unit
) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle()
    val perfectDaysState by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val perfectDaysStreak = perfectDaysState.perfectDaysStreak

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Freigeschaltet, 1 = Alle Erfolge

    val totalGlobalCompletions = profileStats.totalGlobalCompletions
    val unlockedCompletions = profileStats.unlockedCompletions
    val unlockedPerfectDays = profileStats.unlockedPerfectDays

    val habitStreaksList = remember(profileStats.habitStreaks) {
        profileStats.habitStreaks.map { streakInfo ->
            val habit = streakInfo.habit
            val longestStreak = streakInfo.longestStreak
            val hColor = HabitIconMapping.getColor(habit.color)
            val hIcon = habit.icon
            Triple(habit, longestStreak, Pair(hColor, hIcon))
        }
    }

    val totalUnlockedCount = profileStats.totalUnlockedCount
    val totalPossibleCount = profileStats.totalPossibleCount

    val globalCompletionsAchievements = remember(totalGlobalCompletions, language) {
        listOf(
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_10",
                title = if (language == "de") "Erster Schritt" else "First Step",
                description = if (language == "de") "Trage insgesamt 10 Erledigungen ein." else "Log a total of 10 completions across all habits.",
                targetValue = 10,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 10
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_50",
                title = if (language == "de") "Gewohnheits-Routine" else "Habit Routine",
                description = if (language == "de") "Trage insgesamt 50 Erledigungen ein." else "Log a total of 50 completions across all habits.",
                targetValue = 50,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 50
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_200",
                title = if (language == "de") "Eiserner Wille" else "Iron Will",
                description = if (language == "de") "Trage insgesamt 200 Erledigungen ein." else "Log a total of 200 completions across all habits.",
                targetValue = 200,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 200
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_500",
                title = if (language == "de") "Lebensstil-Transformation" else "Lifestyle Transformation",
                description = if (language == "de") "Trage insgesamt 500 Erledigungen ein." else "Log a total of 500 completions across all habits.",
                targetValue = 500,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 500
            )
        )
    }

    val globalPerfectDaysAchievements = remember(perfectDaysStreak, language) {
        listOf(
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_7",
                title = if (language == "de") "Perfekte Woche" else "Perfect Week",
                description = if (language == "de") "Erreiche eine Serie von 7 perfekten Tagen am Stück." else "Achieve a streak of 7 consecutive perfect days.",
                targetValue = 7,
                currentValue = perfectDaysStreak,
                isUnlocked = perfectDaysStreak >= 7
            ),
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_30",
                title = if (language == "de") "Perfekter Monat" else "Perfect Month",
                description = if (language == "de") "Erreiche eine Serie von 30 perfekten Tagen am Stück." else "Achieve a streak of 30 consecutive perfect days.",
                targetValue = 30,
                currentValue = perfectDaysStreak,
                isUnlocked = perfectDaysStreak >= 30
            ),
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_100",
                title = if (language == "de") "Perfektion" else "Perfection",
                description = if (language == "de") "Erreiche eine Serie von 100 perfekten Tagen am Stück." else "Achieve a streak of 100 consecutive perfect days.",
                targetValue = 100,
                currentValue = perfectDaysStreak,
                isUnlocked = perfectDaysStreak >= 100
            )
        )
    }

    // Filtered lists for rendering
    val displayedHabitStreaks = remember(habitStreaksList, selectedTab) {
        if (selectedTab == 0) {
            habitStreaksList.filter { (_, longestStreak, _) -> longestStreak >= 7 }
        } else {
            habitStreaksList
        }
    }

    val displayedCompletions = remember(globalCompletionsAchievements, selectedTab) {
        if (selectedTab == 0) {
            globalCompletionsAchievements.filter { it.isUnlocked }
        } else {
            globalCompletionsAchievements
        }
    }

    val displayedPerfectDays = remember(globalPerfectDaysAchievements, selectedTab) {
        if (selectedTab == 0) {
            globalPerfectDaysAchievements.filter { it.isUnlocked }
        } else {
            globalPerfectDaysAchievements
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed Top Header Row with Settings Gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() }
                    .testTag("profile_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = habits.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == "de") "Gewohnheiten" else "Habits",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalUnlockedCount / $totalPossibleCount",
                                style = MaterialTheme.typography.titleLarge,
                                color = HabitYellow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == "de") "Erfolge" else "Achievements",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val title = if (language == "de") "Was sind Einträge?" else "What are Entries?"
                                    val expl = if (language == "de") {
                                        "Dieser Wert zeigt deine gesamten erfolgreichen Erledigungen an:\n\n" +
                                        "• Bei aufbauenden Gewohnheiten zählt jeder Tag, an dem du dein Ziel erreicht hast.\n" +
                                        "• Bei abgewöhnenden Gewohnheiten zählt jeder Tag, an dem du die Gewohnheit erfolgreich vermieden hast."
                                    } else {
                                        "This value represents your total successful completions:\n\n" +
                                        "• For positive habits, each day you meet your goal counts as a completion.\n" +
                                        "• For negative habits, each day you successfully avoid the bad habit counts as a completion."
                                    }
                                    activeExplanation = title to expl
                                }
                                .padding(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = totalGlobalCompletions.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (language == "de") "Einträge" else "Logs",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Visual filter tabs to toggle: Freigeschaltet, Alle Erfolge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = if (language == "de") listOf("Freigeschaltet", "Alle Erfolge") else listOf("Unlocked", "All Achievements")
                    tabs.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PrimaryViolet else DarkCard)
                                .border(1.dp, if (isSelected) PrimaryViolet else DarkBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }



            // Checks if everything is empty
            if (displayedHabitStreaks.isEmpty() && displayedCompletions.isEmpty() && displayedPerfectDays.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == "de") {
                                    "Noch keine Erfolge"
                                } else {
                                    "No Achievements Yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "de") {
                                    "Arbeite an deinen Gewohnheiten, um deinen ersten Meilenstein freizuschalten!"
                                } else {
                                    "Work on your habits to unlock your first milestone!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // CATEGORY 1: HABIT STREAKS
                if (displayedHabitStreaks.isNotEmpty()) {
                    item {
                        val explanationTitle = if (language == "de") "Gewohnheiten-Serien" else "Habit Streaks"
                        val explanationText = if (language == "de") {
                            "Gewohnheiten-Serien belohnen deine Beständigkeit bei einzelnen Gewohnheiten!\n\n" +
                            "Jede Gewohnheit hat ihre eigene Erfolgssträhne. Du schaltest die Stufen frei, indem du die Gewohnheit an aufeinanderfolgenden Tagen erfüllst:\n\n" +
                            "• 🪵 Holz-Serie: 7 Tage am Stück\n" +
                            "• 🥉 Bronze-Serie: 14 Tage am Stück\n" +
                            "• 🥈 Silber-Serie: 30 Tage am Stück\n" +
                            "• 🥇 Gold-Serie: 100 Tage am Stück\n\n" +
                            "Tippe auf die Abzeichen der einzelnen Gewohnheits-Karten, um Details zu sehen!"
                        } else {
                            "Habit Streaks reward your consistency for individual habits!\n\n" +
                            "Each habit has its own streak card. You unlock tiers by completing the habit on consecutive days:\n\n" +
                            "• 🪵 Wood Streak: 7 days streak\n" +
                            "• 🥉 Bronze Streak: 14 days streak\n" +
                            "• 🥈 Silver Streak: 30 days streak\n" +
                            "• 🥇 Gold Streak: 100 days streak\n\n" +
                            "Tap on the badges in each habit card to see more details!"
                        }
                        CategoryHeader(
                            title = if (language == "de") "Gewohnheiten-Serien" else "Habit Streaks",
                            icon = Icons.Default.Whatshot,
                            color = HabitRed,
                            explanationTitle = explanationTitle,
                            explanationText = explanationText,
                            onInfoClick = { t, e -> activeExplanation = t to e }
                        )
                    }
                    items(displayedHabitStreaks) { (habit, longestStreak, style) ->
                        HabitStreakAchievementCard(
                            habitName = habit.name,
                            habitColor = style.first,
                            habitIcon = style.second,
                            longestStreak = longestStreak,
                            language = language
                        )
                    }
                }

                // CATEGORY 2: COMPLETIONS
                if (displayedCompletions.isNotEmpty()) {
                    item {
                        if (displayedHabitStreaks.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = DarkBorder,
                                thickness = 1.dp
                            )
                        }
                        val explanationTitle = if (language == "de") "Gesamt-Erledigungen" else "Total Completions"
                        val explanationText = if (language == "de") {
                            "Gesamt-Erledigungen zählen, wie oft du insgesamt Gewohnheiten abgehakt hast!\n\n" +
                            "Hierbei werden alle deine Einträge über alle Gewohnheiten hinweg addiert. Erreiche folgende Meilensteine:\n\n" +
                            "• 🌱 Erster Schritt: 10 Erledigungen insgesamt\n" +
                            "• 🔄 Gewohnheits-Routine: 50 Erledigungen insgesamt\n" +
                            "• 🧠 Eiserner Wille: 200 Erledigungen insgesamt\n" +
                            "• 🏆 Lebensstil-Transformation: 500 Erledigungen insgesamt"
                        } else {
                            "Total Completions count how many times you checked off habits in total!\n\n" +
                            "All your entries across all habits are added together. Achieve these milestones:\n\n" +
                            "• 🌱 First Step: 10 completions in total\n" +
                            "• 🔄 Habit Routine: 50 completions in total\n" +
                            "• 🧠 Iron Will: 200 completions in total\n" +
                            "• 🏆 Lifestyle Transformation: 500 completions in total"
                        }
                        CategoryHeader(
                            title = if (language == "de") "Gesamt-Erledigungen" else "Total Completions",
                            icon = Icons.Default.EmojiEvents,
                            color = HabitYellow,
                            explanationTitle = explanationTitle,
                            explanationText = explanationText,
                            onInfoClick = { t, e -> activeExplanation = t to e }
                        )
                    }
                    items(displayedCompletions) { achievement ->
                        GlobalAchievementCard(
                            type = achievement.type,
                            tier = achievement.tier,
                            title = achievement.title,
                            description = achievement.description,
                            targetValue = achievement.targetValue,
                            currentValue = achievement.currentValue,
                            isUnlocked = achievement.isUnlocked,
                            language = language
                        )
                    }
                }

                // CATEGORY 3: PERFECT DAYS
                if (displayedPerfectDays.isNotEmpty()) {
                    item {
                        if (displayedHabitStreaks.isNotEmpty() || displayedCompletions.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = DarkBorder,
                                thickness = 1.dp
                            )
                        }
                        val explanationTitle = if (language == "de") "Perfekte Serien" else "Perfect Streaks"
                        val explanationText = if (language == "de") {
                            "Perfekte Serien belohnen Tage, an denen du deine Disziplin zu 100% gehalten hast!\n\n" +
                            "Ein perfekter Tag ist ein Tag, an dem du alle deine für diesen Tag geplanten bzw. aktiven Gewohnheiten vollständig erledigt hast. Wenn du diese perfekten Tage hintereinander schaffst, erreichst du:\n\n" +
                            "• 📅 Perfekte Woche: 7 perfekte Tage am Stück\n" +
                            "• 🗓️ Perfekter Monat: 30 perfekte Tage am Stück\n" +
                            "• 👑 Perfektion: 100 perfekte Tage am Stück"
                        } else {
                            "Perfect Streaks reward consecutive days where you maintained 100% discipline!\n\nA perfect day is a day where you successfully complete all of your scheduled/active habits. By stringing perfect days together, you achieve:\n\n• 📅 Perfect Week: 7 consecutive perfect days\n• 🗓️ Perfect Month: 30 consecutive perfect days\n• 👑 Perfection: 100 consecutive perfect days"
                        }
                        CategoryHeader(
                            title = if (language == "de") "Perfekte Serien" else "Perfect Streaks",
                            icon = Icons.Default.WorkspacePremium,
                            color = SuccessGreen,
                            explanationTitle = explanationTitle,
                            explanationText = explanationText,
                            onInfoClick = { t, e -> activeExplanation = t to e }
                        )
                    }
                    items(displayedPerfectDays) { achievement ->
                        GlobalAchievementCard(
                            type = achievement.type,
                            tier = achievement.tier,
                            title = achievement.title,
                            description = achievement.description,
                            targetValue = achievement.targetValue,
                            currentValue = achievement.currentValue,
                            isUnlocked = achievement.isUnlocked,
                            language = language
                        )
                    }
                }
            }
        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }
    }
}
}

@Composable
fun HabitAnalyticsSection(
    habit: Habit,
    language: String,
    calendarRows: List<List<CalendarCellState?>>,
    viewModel: HabitsViewModel,
    onInfoClick: (String, String) -> Unit
) {
    val analyticsState by viewModel.habitAnalyticsState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.analyticsFilter.collectAsStateWithLifecycle()

    val minVerlaufOffset = analyticsState?.minVerlaufOffset ?: 0
    val verlaufTitle = analyticsState?.verlaufTitle ?: ""
    val canPrevVerlauf = analyticsState?.canPrevVerlauf ?: false
    val canNextVerlauf = analyticsState?.canNextVerlauf ?: false
    val barData = analyticsState?.barData ?: emptyList()

    val totalDays = calendarRows.flatten().filterNotNull().filter { it.status != "INACTIVE" }
    val successCount = totalDays.count { it.status == "SUCCESS" }
    val failedCount = totalDays.count { it.status == "FAILED" }
    val pendingCount = totalDays.count { it.status == "PENDING" }
        Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Card 1: Verlauf (Balkendiagramm) ---
        if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Verlauf",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "de") "VERLAUF" else "HISTORY",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            InfoIconButton(
                                title = if (language == "de") "Habit-Verlauf" else "Habit History",
                                explanation = if (language == "de") {
                                    "Zeigt an, wie viel du an jedem Tag eingetragen hast. Bei numerischen Gewohnheiten wird das Erfüllungsverhältnis (z.B. 50% deines Ziels) dargestellt. Wechsle zwischen Woche, Monat und Jahr, um langfristige Muster zu erkennen."
                                } else {
                                    "Shows how much you logged on each day. For numeric habits, it displays the completion ratio (e.g. 50% of your target). Switch between Week, Month, and Year views to spot long-term patterns."
                                },
                                onClick = onInfoClick
                            )
                        }

                        Text(
                            text = if (language == "de") "Mal pro Tag" else "Times per day",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (canPrevVerlauf) viewModel.navigateAnalyticsVerlauf(-1) },
                            enabled = canPrevVerlauf
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Prev",
                                tint = if (canPrevVerlauf) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                            )
                        }

                        Text(
                            text = verlaufTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { if (canNextVerlauf) viewModel.navigateAnalyticsVerlauf(1) },
                            enabled = canNextVerlauf
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = if (canNextVerlauf) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        barData.forEach { (label, ratio) ->
                            val barWidth = when (activeFilter) {
                                "WEEK" -> 16.dp
                                "MONTH" -> 4.dp
                                else -> 16.dp
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(100.dp)
                                        .width(barWidth)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(ProgressTrack),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(ratio.coerceIn(0f, 1f))
                                            .fillMaxWidth()
                                            .background(SuccessGreen)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 7.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "WEEK" to (if (language == "de") "Woche" else "Week"),
                            "MONTH" to (if (language == "de") "Monat" else "Month"),
                            "YEAR" to (if (language == "de") "Jahr" else "Year")
                        ).forEach { (key, display) ->
                            val isSelected = activeFilter == key
                            Button(
                                onClick = {
                                    viewModel.setAnalyticsFilter(key)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) PrimaryViolet else ProgressTrack,
                                    contentColor = if (isSelected) TextPrimary else TextSecondary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(text = display, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val cosVal: Float,
    val sinVal: Float,
    val speed: Float,
    val color: Color
)

@Composable
fun ConfettiAnimation(onFinished: () -> Unit) {
    val particles = remember {
        List(24) {
            val angle = (0..360).random().toFloat()
            val radian = Math.toRadians(angle.toDouble())
            ConfettiParticle(
                cosVal = Math.cos(radian).toFloat(),
                sinVal = Math.sin(radian).toFloat(),
                speed = (15..45).random().toFloat(),
                color = listOf(SuccessGreen, HabitYellow, HabitRed, PrimaryViolet).random()
            )
        }
    }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            progress = value
        }
        onFinished()
    }

    Canvas(modifier = Modifier.size(80.dp)) {
        val radiusMultiplier = 6f * (1f - progress)
        val alphaVal = (1f - progress).coerceIn(0f, 1f)
        val progressFactor = progress * 1.5f

        particles.forEach { particle ->
            val distance = particle.speed * progressFactor
            val x = center.x + particle.cosVal * distance
            val y = center.y + particle.sinVal * distance
            
            drawCircle(
                color = particle.color,
                radius = radiusMultiplier,
                center = androidx.compose.ui.geometry.Offset(x, y),
                alpha = alphaVal
            )
        }
    }
}

@Composable
fun InfoIconButton(
    title: String,
    explanation: String,
    onClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = { onClick(title, explanation) },
        modifier = modifier.size(44.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Help Info",
            tint = PrimaryViolet,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ExplanationDialog(
    title: String,
    explanation: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryViolet
                )
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DailyNoteDialog(
    currentNote: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember(currentNote) { mutableStateOf(currentNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == "de") "Tägliche Notiz" else "Daily Note",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = {
                    Text(
                        text = if (language == "de") "Gedanken, Erfolge oder Notizen für diesen Tag..." else "Thoughts, achievements, or notes for this day...",
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryViolet,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkBg,
                    unfocusedContainerColor = DarkBg,
                    cursorColor = PrimaryViolet
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(noteText) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (language == "de") "Speichern" else "Save",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (language == "de") "Abbrechen" else "Cancel",
                    color = TextSecondary
                )
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
    )
}

@Composable
fun AchievementBadge(
    type: String, // "STREAK", "COMPLETIONS", "PERFECT_DAYS"
    tier: String, // e.g. "WOOD", "BRONZE", "SILVER", "GOLD", "COMP_10", "PERF_7" etc.
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = if (isUnlocked) 1f else 0.35f
    val modifierWithAlpha = modifier.graphicsLayer { this.alpha = alpha }

    when (type) {
        "COMPLETIONS" -> {
            val arrowCount = when (tier) {
                "COMP_10" -> 1
                "COMP_50" -> 2
                "COMP_200" -> 3
                "COMP_500" -> 4
                else -> 1
            }
            TargetWithArrowsIcon(arrowCount = arrowCount, modifier = modifierWithAlpha)
        }
        "PERFECT_DAYS" -> {
            val ringCount = when (tier) {
                "PERF_7" -> 1
                "PERF_30" -> 2
                "PERF_100" -> 3
                else -> 1
            }
            CalendarWithRingsIcon(ringCount = ringCount, modifier = modifierWithAlpha)
        }
        else -> {
            // Draw original STREAK flame
            val tierColor = when (tier) {
                "WOOD" -> Color(0xFF8B5A2B) // Rich wood brown
                "BRONZE" -> Color(0xFFCD7F32) // Warm bronze copper
                "SILVER" -> Color(0xFFC0C0C0) // Metallic light silver
                "GOLD" -> Color(0xFFFFD700) // Shiny Gold
                "PLATINUM" -> Color(0xFFE5E4E2) // Ice Platinum
                "DIAMOND" -> Color(0xFF00E5FF) // Diamond Cyan
                "RUBY" -> Color(0xFFFF1744) // Ruby Red
                "MASTER" -> Color(0xFFD500F9) // Master Purple
                "LEGEND" -> Color(0xFFFF9100) // Legend Orange
                "UNREAL" -> Color(0xFF76FF03) // Unreal Green
                else -> Color(0xFFFFD700) // Shiny Gold default
            }

            val tierSecondary = when (tier) {
                "WOOD" -> Color(0xFF5C3A21) // Dark wood bark
                "BRONZE" -> Color(0xFF8C501C) // Deep bronze
                "SILVER" -> Color(0xFF8A9A86) // Darker grey silver
                "GOLD" -> Color(0xFFB59000) // Deep yellow gold
                "PLATINUM" -> Color(0xFF9E9E9E)
                "DIAMOND" -> Color(0xFF00ACC1)
                "RUBY" -> Color(0xFFB71C1C)
                "MASTER" -> Color(0xFF4A148C)
                "LEGEND" -> Color(0xFFE65100)
                "UNREAL" -> Color(0xFF33691E)
                else -> Color(0xFFB59000)
            }

            Spacer(
                modifier = modifier.drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                    val outerRadius = width * 0.45f

                    val borderStroke = Stroke(width = 3f * density)
                    val woodStroke1 = Stroke(width = 1.5f * density)
                    val woodStroke2 = Stroke(width = 1f * density)

                    val flameWidth = width * 0.35f
                    val flameHeight = height * 0.5f

                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x, center.y - flameHeight * 0.5f)
                        cubicTo(
                            center.x + flameWidth * 0.4f, center.y - flameHeight * 0.1f,
                            center.x + flameWidth * 0.6f, center.y + flameHeight * 0.2f,
                            center.x, center.y + flameHeight * 0.5f
                        )
                        cubicTo(
                            center.x - flameWidth * 0.6f, center.y + flameHeight * 0.2f,
                            center.x - flameWidth * 0.4f, center.y - flameHeight * 0.1f,
                            center.x, center.y - flameHeight * 0.5f
                        )
                    }

                    val innerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x, center.y - flameHeight * 0.2f)
                        cubicTo(
                            center.x + flameWidth * 0.2f, center.y + flameHeight * 0.05f,
                            center.x + flameWidth * 0.3f, center.y + flameHeight * 0.2f,
                            center.x, center.y + flameHeight * 0.4f
                        )
                        cubicTo(
                            center.x - flameWidth * 0.3f, center.y + flameHeight * 0.2f,
                            center.x - flameWidth * 0.2f, center.y + flameHeight * 0.05f,
                            center.x, center.y - flameHeight * 0.2f
                        )
                    }

                    val innerFlameColor = when (tier) {
                        "WOOD" -> Color(0xFFFFA726)
                        "BRONZE" -> Color(0xFFFFD180)
                        "SILVER" -> Color(0xFFE0F7FA)
                        "GOLD" -> Color(0xFFFFF9C4)
                        "PLATINUM" -> Color(0xFFE0F2F1)
                        "DIAMOND" -> Color(0xFFE0F7FA)
                        "RUBY" -> Color(0xFFFFEBEE)
                        "MASTER" -> Color(0xFFF3E5F5)
                        "LEGEND" -> Color(0xFFFFF3E0)
                        "UNREAL" -> Color(0xFFF1F8E9)
                        else -> Color.White
                    }

                    onDrawBehind {
                        // Draw background base circle
                        drawCircle(
                            color = if (isUnlocked) Color(0xFF1E1E2E) else Color(0xFF111116),
                            radius = outerRadius,
                            center = center
                        )

                        // Draw Tier Border
                        drawCircle(
                            color = tierColor.copy(alpha = alpha),
                            radius = outerRadius,
                            center = center,
                            style = borderStroke
                        )

                        // Grain/texture for streak tiers
                        when (tier) {
                            "WOOD" -> {
                                drawCircle(
                                    color = tierSecondary.copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.75f,
                                    center = center,
                                    style = woodStroke1
                                )
                                drawCircle(
                                    color = tierSecondary.copy(alpha = 0.15f * alpha),
                                    radius = outerRadius * 0.5f,
                                    center = center,
                                    style = woodStroke2
                                )
                            }
                            "BRONZE" -> {
                                drawCircle(
                                    color = tierColor.copy(alpha = 0.1f * alpha),
                                    radius = outerRadius * 0.8f,
                                    center = center
                                )
                            }
                            "SILVER" -> {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.15f * alpha),
                                    radius = outerRadius * 0.8f,
                                    center = center
                                )
                            }
                            "GOLD" -> {
                                drawCircle(
                                    color = Color(0xFFFFE082).copy(alpha = 0.2f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "PLATINUM" -> {
                                drawCircle(
                                    color = Color(0xFFE0F7FA).copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "DIAMOND" -> {
                                drawCircle(
                                    color = Color(0xFFE0F7FA).copy(alpha = 0.35f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "RUBY" -> {
                                drawCircle(
                                    color = Color(0xFFFFCDD2).copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "MASTER" -> {
                                drawCircle(
                                    color = Color(0xFFE1BEE7).copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "LEGEND" -> {
                                drawCircle(
                                    color = Color(0xFFFFE0B2).copy(alpha = 0.3f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "UNREAL" -> {
                                drawCircle(
                                    color = Color(0xFFDCEDC8).copy(alpha = 0.35f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                        }

                        // Draw Streak Flame
                        drawPath(
                            path = path,
                            color = tierColor.copy(alpha = alpha)
                        )

                        // Inner flame core
                        drawPath(
                            path = innerPath,
                            color = innerFlameColor.copy(alpha = 0.8f * alpha)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun TargetWithArrowsIcon(
    arrowCount: Int,
    modifier: Modifier = Modifier
) {
    val baseAngles = remember { listOf(-45f, 135f, 15f, 75f, -105f, -165f) }

    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
            val outerRadius = width * 0.42f
            
            val stroke1 = Stroke(width = 1.5f * density)
            val stroke2 = Stroke(width = 2f * density)
            
            val shaftLength = width * 0.65f
            val startX = center.x - shaftLength * 0.6f
            val endX = center.x + shaftLength * 0.35f
            val y = center.y
            val arrowHeadSize = width * 0.08f
            
            val shaftStart = androidx.compose.ui.geometry.Offset(startX, y)
            val shaftEnd = androidx.compose.ui.geometry.Offset(endX, y)
            
            val fletch1Start = androidx.compose.ui.geometry.Offset(startX, y)
            val fletch1End = androidx.compose.ui.geometry.Offset(startX - arrowHeadSize * 0.8f, y - arrowHeadSize * 0.5f)
            val fletch2Start = androidx.compose.ui.geometry.Offset(startX, y)
            val fletch2End = androidx.compose.ui.geometry.Offset(startX - arrowHeadSize * 0.8f, y + arrowHeadSize * 0.5f)
            
            val arrowHeadPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(endX, y)
                lineTo(endX - arrowHeadSize, y - arrowHeadSize * 0.5f)
                lineTo(endX - arrowHeadSize * 0.7f, y)
                lineTo(endX - arrowHeadSize, y + arrowHeadSize * 0.5f)
                close()
            }
            
            onDrawBehind {
                drawCircle(
                    color = Color(0xFF673AB7).copy(alpha = 0.15f),
                    radius = outerRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF673AB7),
                    radius = outerRadius,
                    center = center,
                    style = stroke1
                )
                // Red / Crimson ring
                drawCircle(
                    color = Color(0xFFE91E63),
                    radius = outerRadius * 0.7f,
                    center = center,
                    style = stroke2
                )
                // Golden Center Bullseye
                drawCircle(
                    color = Color(0xFFFFC107),
                    radius = outerRadius * 0.3f,
                    center = center
                )
                
                // Draw arrows piercing the bullseye at different angles
                val limit = arrowCount.coerceAtMost(6)
                for (i in 0 until limit) {
                    val angleDeg = baseAngles[i]
                    withTransform({
                        rotate(angleDeg, center)
                    }) {
                        // 1. Arrow Shaft
                        drawLine(
                            color = Color(0xFF8D6E63),
                            start = shaftStart,
                            end = shaftEnd,
                            strokeWidth = 2f * density,
                            cap = StrokeCap.Round
                        )
                        
                        // 2. Arrow Head
                        drawPath(
                            path = arrowHeadPath,
                            color = Color(0xFFB0BEC5)
                        )
                        
                        // 3. Fletching
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = fletch1Start,
                            end = fletch1End,
                            strokeWidth = 1.5f * density,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = fletch2Start,
                            end = fletch2End,
                            strokeWidth = 1.5f * density,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    )
}

private class RingOrbitData(
    val radius: Float,
    val stroke: Stroke,
    val dotCenter: androidx.compose.ui.geometry.Offset,
    val rotationAngle: Float
)

@Composable
fun CalendarWithRingsIcon(
    ringCount: Int,
    modifier: Modifier = Modifier
) {
    val ringColors = remember { 
        listOf(
            Color(0xFF4CAF50), // Green for 3 days
            Color(0xFFFFC107), // Yellow/Bronze for 10 days
            Color(0xFF9C27B0), // Purple for 30 days
            Color(0xFFFF9800), // Gold/Amber for 100 days
            Color(0xFF00BCD4)  // Cyan/Cosmic for 365 days
        ) 
    }

    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
            
            val orbits = (0 until ringCount.coerceAtMost(5)).map { i ->
                val radius = width * (0.35f + (i * 0.045f))
                val stroke = Stroke(
                    width = 1.2f * density,
                    cap = StrokeCap.Round
                )
                val angleRad = Math.toRadians((i * 72f).toDouble())
                val dotX = center.x + radius * Math.cos(angleRad).toFloat()
                val dotY = center.y + radius * Math.sin(angleRad).toFloat()
                RingOrbitData(
                    radius = radius,
                    stroke = stroke,
                    dotCenter = androidx.compose.ui.geometry.Offset(dotX, dotY),
                    rotationAngle = i * 35f
                )
            }
            
            val calSize = width * 0.38f
            val calLeft = center.x - calSize / 2f
            val calTop = center.y - calSize / 2f
            
            val calSizeObj = androidx.compose.ui.geometry.Size(calSize, calSize)
            val calTopLeft = androidx.compose.ui.geometry.Offset(calLeft, calTop)
            val calCornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            
            val headerHeight = calSize * 0.3f
            val headerSize = androidx.compose.ui.geometry.Size(calSize, headerHeight)
            val headerRectSize = androidx.compose.ui.geometry.Size(calSize, headerHeight - 2.dp.toPx())
            val headerRectTopLeft = androidx.compose.ui.geometry.Offset(calLeft, calTop + 2.dp.toPx())
            
            val ringSizeWidth = calSize * 0.1f
            val ringHeight = calSize * 0.2f
            val ringOffsetLeft = calLeft + calSize * 0.2f
            val ringOffsetRight = calLeft + calSize * 0.7f
            
            val ringSize = androidx.compose.ui.geometry.Size(ringSizeWidth, ringHeight)
            val ring1TopLeft = androidx.compose.ui.geometry.Offset(ringOffsetLeft, calTop - ringHeight * 0.5f)
            val ring2TopLeft = androidx.compose.ui.geometry.Offset(ringOffsetRight, calTop - ringHeight * 0.5f)
            val ringCornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * density, 1.5f * density)
            
            val dotRadius = 1.5f * density
            val startGridX = calLeft + calSize * 0.2f
            val startGridY = calTop + headerHeight + calSize * 0.18f
            val gridSpacing = calSize * 0.2f
            
            val gridDots = List(6) { index ->
                val row = index / 3
                val col = index % 3
                androidx.compose.ui.geometry.Offset(
                    startGridX + col * gridSpacing,
                    startGridY + row * gridSpacing
                )
            }
            
            val checkSize = calSize * 0.35f
            val checkLeft = calLeft + calSize * 0.45f
            val checkTop = calTop + headerHeight + calSize * 0.15f
            
            val checkPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(checkLeft, checkTop + checkSize * 0.5f)
                lineTo(checkLeft + checkSize * 0.35f, checkTop + checkSize * 0.85f)
                lineTo(checkLeft + checkSize * 0.9f, checkTop + checkSize * 0.2f)
            }
            val checkStroke = Stroke(width = 2.5f * density, cap = StrokeCap.Round)
            
            onDrawBehind {
                // Draw outer orbits
                val limit = ringCount.coerceAtMost(5)
                for (i in 0 until limit) {
                    val rData = orbits[i]
                    withTransform({
                        rotate(rData.rotationAngle, center)
                    }) {
                        drawCircle(
                            color = ringColors[i],
                            radius = rData.radius,
                            center = center,
                            style = rData.stroke
                        )
                        drawCircle(
                            color = ringColors[i],
                            radius = 3f * density,
                            center = rData.dotCenter
                        )
                    }
                }
                
                // Calendar Background Card
                drawRoundRect(
                    color = Color(0xFF1E1E2E),
                    size = calSizeObj,
                    topLeft = calTopLeft,
                    cornerRadius = calCornerRadius
                )
                
                // Calendar Header Banner (Red/Pinkish)
                drawRoundRect(
                    color = Color(0xFFE91E63),
                    size = headerSize,
                    topLeft = calTopLeft,
                    cornerRadius = calCornerRadius
                )
                // Fill lower rounded corners back to square for calendar card divider
                drawRect(
                    color = Color(0xFFE91E63),
                    size = headerRectSize,
                    topLeft = headerRectTopLeft
                )
                
                // Calendar Binder Rings
                drawRoundRect(
                    color = Color(0xFFB0BEC5),
                    size = ringSize,
                    topLeft = ring1TopLeft,
                    cornerRadius = ringCornerRadius
                )
                drawRoundRect(
                    color = Color(0xFFB0BEC5),
                    size = ringSize,
                    topLeft = ring2TopLeft,
                    cornerRadius = ringCornerRadius
                )
                
                // Calendar Grid/Dots in body
                gridDots.forEach { dotOffset ->
                    drawCircle(
                        color = Color(0xFF94A3B8),
                        radius = dotRadius,
                        center = dotOffset
                    )
                }
                
                // Checkmark overlay
                drawPath(
                    path = checkPath,
                    color = Color(0xFF4CAF50),
                    style = checkStroke
                )
            }
        }
    )
}

private class DonutSweeps(
    val success: Float,
    val failed: Float,
    val pending: Float,
    val paused: Float
)