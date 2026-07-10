package com.example

import android.app.Activity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: HabitsViewModel = viewModel()
    
    var selectedTab by remember { mutableStateOf("TODAY") }
    val navController = rememberNavController()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val habitLogsByHabitId by viewModel.habitLogsByHabitId.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    var widgetAddValueHabitId by remember { mutableStateOf<Int?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var longPressedHabit by remember { mutableStateOf<Habit?>(null) }
    var showDeleteConfirmHabit by remember { mutableStateOf<Habit?>(null) }
    var showArchiveConfirmHabit by remember { mutableStateOf<Habit?>(null) }

    // Keep tab selected state in synchronization if user navigates
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && !currentRoute.startsWith("DETAIL") && selectedTab != currentRoute) {
            selectedTab = currentRoute
        }
    }

    // Intercept intent from Widget for numerical habit logging
    LaunchedEffect(activity?.intent) {
        val intent = activity?.intent
        if (intent?.action == "com.example.widget.ACTION_WIDGET_ADD_VALUE") {
            val habitId = intent.getIntExtra("com.example.widget.EXTRA_HABIT_ID", -1)
            if (habitId != -1) {
                widgetAddValueHabitId = habitId
                // Reset action so it doesn't trigger again on configuration change
                intent.action = null
            }
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
                        onHabitLongClick = { habit ->
                            longPressedHabit = habit
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
                    StatsScreen(
                        viewModel = viewModel,
                        language = language,
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

            if (longPressedHabit != null) {
                val habit = longPressedHabit!!
                val isPausedOnSelectedDate = habitLogsByHabitId[habit.id]?.isPaused == true

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
                                    editingHabit = habitToEdit
                                    navController.navigate("CREATE") {
                                        launchSingleTop = true
                                    }
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

            if (widgetAddValueHabitId != null) {
                WidgetAddValueDialog(
                    habitId = widgetAddValueHabitId!!,
                    viewModel = viewModel,
                    language = language,
                    onDismiss = { widgetAddValueHabitId = null }
                )
            }
        }
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
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val habitToLog = habits.find { it.id == habitId }
    if (habitToLog != null) {
        var inputVal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (language == "de") "Wert hinzufügen" else "Add Value",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == "de") {
                            "Wie viel möchtest du für '${habitToLog.name}' hinzufügen? (Einheit: ${habitToLog.unit})"
                        } else {
                            "How much do you want to add for '${habitToLog.name}'? (Unit: ${habitToLog.unit})"
                        },
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    onClick = {
                        val fValue = inputVal.toFloatOrNull() ?: 0f
                        if (fValue > 0f) {
                            val existingLog = logs.find { it.habitId == habitToLog.id && it.date == selectedDate }
                            val existingValue = existingLog?.value ?: 0f
                            viewModel.logNumericalHabit(habitToLog.id, selectedDate, existingValue + fValue)
                        }
                        onDismiss()
                    }
                ) {
                    Text(if (language == "de") "Speichern" else "Save", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(if (language == "de") "Abbrechen" else "Cancel", color = TextSecondary)
                }
            }
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
    modifier: Modifier = Modifier
) {
    val dayScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(120),
        label = "dayScale"
    )

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val isToday = dayStr == todayStr
    val isFuture = dayStr > todayStr
    val isPast = dayStr < todayStr

    val weekdayColor = when {
        isSelected -> TextPrimary
        isToday -> PrimaryViolet
        isFuture -> TextSecondary.copy(alpha = 0.4f)
        else -> TextSecondary
    }

    val dateColor = when {
        isSelected -> DarkBg
        isToday -> PrimaryViolet
        isFuture -> TextSecondary.copy(alpha = 0.4f)
        else -> TextPrimary
    }

    val circleColor = when {
        isSelected -> PrimaryViolet
        isToday -> PrimaryViolet.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = dayScale
                scaleY = dayScale
            }
            .clickable { onSelect(dayStr) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Weekday Name (e.g. MON)
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = weekdayColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Date Number with a highlight circle/capsule if selected or today
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = circleColor,
                    shape = CircleShape
                )
                .then(
                    if (isToday) {
                        Modifier.border(2.dp, if (isSelected) TextPrimary else PrimaryViolet, CircleShape)
                    } else if (isSelected) {
                        Modifier.border(1.dp, PrimaryViolet.copy(alpha = 0.5f), CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNum,
                style = MaterialTheme.typography.titleMedium,
                color = dateColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Indicator dot/line under day
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(3.dp)
                .background(
                    color = if (isSelected) {
                        if (isToday) TextPrimary else PrimaryViolet
                    } else if (isToday) {
                        PrimaryViolet.copy(alpha = 0.5f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(1.5.dp)
                )
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
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdfDb.format(Date())
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
    onHabitLongClick: (Habit) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekStartCalendar by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()

    val activeHabitsForSelectedDate by viewModel.activeHabitsForSelectedDate.collectAsStateWithLifecycle()
    val habitLogsByHabitId by viewModel.habitLogsByHabitId.collectAsStateWithLifecycle()
    
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    val oldestHabitDateMs = remember(allHabits) {
        allHabits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
    }

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

    val currentWeekDaysData = remember(weekStartCalendar) {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDayNum = SimpleDateFormat("d", Locale.GERMANY)
        val sdfDayName = SimpleDateFormat("E", Locale.GERMANY)
        val cal = weekStartCalendar.clone() as Calendar
        val list = mutableListOf<Triple<String, String, String>>() 
        for (i in 0 until 7) {
            val date = cal.time
            val dayStr = sdfDb.format(date)
            val dayNum = sdfDayNum.format(date)
            val dayName = sdfDayName.format(date).uppercase(Locale.GERMANY).take(2)
            list.add(Triple(dayStr, dayNum, dayName))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val sdfDisplay = remember { SimpleDateFormat("d. MMMM", Locale.GERMANY) }
    val sdfDb = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    val formattedDisplayDate = remember(selectedDate) {
        try {
            val date = sdfDb.parse(selectedDate)
            if (selectedDate == sdfDb.format(Date())) {
                "${sdfDisplay.format(date ?: Date())} (${if (language == "de") "Heute" else "Today"})"
            } else {
                sdfDisplay.format(date ?: Date())
            }
        } catch (e: Exception) {
            selectedDate
        }
    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = formattedDisplayDate,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        item(key = "today_calendar_strip") {
            var offsetX by remember { mutableStateOf(0f) }
            val swipeThreshold = 120f
            
            val canPrevWeek = remember(weekStartCalendar, allHabits) {
                weekStartCalendar.timeInMillis > viewModel.getMinWeekStartMillis()
            }

            val canNextWeek = remember(weekStartCalendar) {
                val maxWeekStart = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                weekStartCalendar.timeInMillis < maxWeekStart.timeInMillis
            }

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
                            key(dayStr) {
                                CalendarDayItem(
                                    dayStr = dayStr,
                                    dayNum = dayNum,
                                    dayName = dayName,
                                    isSelected = isSelected,
                                    onSelect = onSelectDayRemembered,
                                    modifier = Modifier.weight(1f)
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
                    text = encouragementText,
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

        if (activeHabitsForSelectedDate.isEmpty()) {
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
            items(activeHabitsForSelectedDate, key = { it.id }) { habit ->
                val log = habitLogsByHabitId[habit.id]
                val currentProgressValue = log?.value ?: 0f
                val hasLog = log != null
                val isPaused = log?.isPaused == true
                
                val status = when {
                    isPaused -> "PAUSED"
                    log == null -> if (habit.isNegative) "SUCCESS" else "PENDING"
                    log.value == -1f -> "FAILED"
                    log.value == -2f -> "SUCCESS"
                    else -> {
                        if (habit.type == "BINARY") {
                            if (habit.isNegative) "FAILED" else "SUCCESS"
                        } else {
                            if (habit.isNegative) {
                                if (log.value >= habit.targetValue) "FAILED" else "PENDING"
                            } else {
                                if (log.value >= habit.targetValue) "SUCCESS" else "PENDING"
                            }
                        }
                    }
                }
                
                val isCompleted = status == "SUCCESS"
                val isFailed = status == "FAILED"

                HabitItemRow(
                    habit = habit,
                    currentValue = currentProgressValue,
                    isCompleted = isCompleted,
                    isFailed = isFailed,
                    isPaused = isPaused,
                    hasLog = hasLog,
                    onToggle = onToggleRemembered,
                    onAddQuantity = onAddQuantityRemembered,
                    onLongClick = onHabitLongClick,
                    language = language
                )
            }
        }

        item(key = "today_daily_note") {
            val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
            val currentNote = remember(allDailyNotes, selectedDate) {
                allDailyNotes.find { it.date == selectedDate }?.content ?: ""
            }

            var isEditing by remember(selectedDate) { mutableStateOf(false) }
            var noteText by remember(selectedDate, currentNote) { mutableStateOf(currentNote) }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(isEditing) {
                if (isEditing) {
                    focusRequester.requestFocus()
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Daily Note",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "de") "MINI-NOTIZBUCH" else "MINI NOTEBOOK",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!isEditing && currentNote.isNotEmpty()) {
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Note",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditing) {
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
                                .heightIn(min = 100.dp, max = 200.dp)
                                .focusRequester(focusRequester),
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { 
                                noteText = currentNote
                                isEditing = false 
                            }) {
                                Text(
                                    text = if (language == "de") "Abbrechen" else "Cancel",
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveDailyNote(selectedDate, noteText)
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (language == "de") "Speichern" else "Save",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        if (currentNote.isNotEmpty()) {
                            Text(
                                text = currentNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isEditing = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { isEditing = true }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Note",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "de") "Notiz für diesen Tag hinzufügen..." else "Add a note for this day...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = PrimaryViolet,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 24.dp)
                .testTag("fab_add_habit")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Habit",
                modifier = Modifier.size(24.dp)
            )
        }
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

    val borderBrush = when {
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

    Card(
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(16.dp))
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
                        HabitIconMapping.getIcon(habit.icon),
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
                            val formattedVal = if (currentValue % 1f == 0f) currentValue.toInt().toString() else currentValue.toString()
                            val formattedTarget = if (habit.targetValue % 1f == 0f) habit.targetValue.toInt().toString() else habit.targetValue.toString()
                            "$formattedVal / $formattedTarget ${habit.unit}"
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

            if (habit.type == "NUMBER" && !isPaused) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onAddQuantity(habit.id, currentValue, -1f) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onAddQuantity(habit.id, currentValue, 1f) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryViolet, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
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
                        ),
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
    onHabitClick: (Int) -> Unit,
    onOverallClick: () -> Unit
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreakOfAll.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    val encouragementText = remember(todayProgressTuple, language) {
        getEncouragementText(todayProgressTuple.first, todayProgressTuple.second, language)
    }

    val onStatItemClick = remember(onHabitClick) {
        { habitId: Int ->
            onHabitClick(habitId)
        }
    }

    val shortDayNames = remember(language) {
        val sdfDayInitial = SimpleDateFormat("E", if (language == "de") Locale.GERMANY else Locale.US)
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            list.add(sdfDayInitial.format(cal.time).take(2).uppercase(if (language == "de") Locale.GERMANY else Locale.US))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list.reverse()
        list
    }

    val dayNumbers = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            list.add(cal.get(Calendar.DAY_OF_MONTH).toString())
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list.reverse()
        list
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 140.dp, top = 16.dp)
        ) {
            item {
                Text(
                    text = if (language == "de") "Statistik" else "Statistics",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
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
                            text = encouragementText,
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
                items(habitsWithStats, key = { it.habit.id }) { model ->
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
                            HabitIconMapping.getIcon(habit.icon),
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

    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val habitLogs = remember(allLogs, habit.id) { allLogs.filter { it.habitId == habit.id } }

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
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
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

        // Streaks card
        item(key = "detail_streaks") {
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "de") "$currentStreak Tage" else "$currentStreak Days",
                                style = MaterialTheme.typography.titleMedium,
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "de") "$longestStreak Tage" else "$longestStreak Days",
                                style = MaterialTheme.typography.titleMedium,
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
                            Spacer(modifier = Modifier.height(4.dp))
                            val completionRate = state?.completionRate ?: 0
                            Text(
                                text = "$completionRate%",
                                style = MaterialTheme.typography.titleMedium,
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

        // Calendar Grid Card
        item(key = "detail_calendar") {
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

        // Analytics Charts (Bar Chart & Donut Chart)
        item(key = "detail_analytics") {
            HabitAnalyticsSection(
                habit = habit,
                logs = habitLogs,
                language = language,
                calendarRows = state?.calendarGridRows ?: emptyList(),
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

@Composable
fun OverallStatsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    var monthViewCalendar by remember(selectedDate) {
        val cal = Calendar.getInstance()
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val d = sdfDb.parse(selectedDate)
            if (d != null) cal.time = d
        } catch (e: Exception) {}
        cal.set(Calendar.DAY_OF_MONTH, 1)
        mutableStateOf(cal)
    }

    val oldestHabitDateMs = remember(allHabits) {
        allHabits.map { if (it.startDate > 946684800000L) it.startDate else it.createdAt }.minOrNull() ?: System.currentTimeMillis()
    }

    val canPrevMonth = remember(monthViewCalendar, oldestHabitDateMs) {
        val oldestCal = Calendar.getInstance().apply { timeInMillis = oldestHabitDateMs }
        val currentCalMonth = monthViewCalendar.get(Calendar.MONTH) + monthViewCalendar.get(Calendar.YEAR) * 12
        val oldestCalMonth = oldestCal.get(Calendar.MONTH) + oldestCal.get(Calendar.YEAR) * 12
        currentCalMonth > oldestCalMonth
    }

    val canNextMonth = remember(monthViewCalendar) {
        val today = Calendar.getInstance()
        val currentCalMonth = monthViewCalendar.get(Calendar.MONTH) + monthViewCalendar.get(Calendar.YEAR) * 12
        val todayCalMonth = today.get(Calendar.MONTH) + today.get(Calendar.YEAR) * 12
        currentCalMonth < todayCalMonth
    }

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

            // Month View Card (Static!)
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
                        // Header info row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "de") "MONATSÜBERSICHT" else "MONTHLY VIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            InfoIconButton(
                                title = if (language == "de") "Gesamt-Monatsübersicht" else "Overall Monthly View",
                                explanation = if (language == "de") {
                                    "Bietet eine vollständige Monatsübersicht deines Fortschritts über alle Gewohnheiten hinweg. Grün steht für erfolgreiche Tage, Rot für fehlgeschlagene Tage, Gelb für ausstehende Tage und Dunkelgrau für inaktive Tage."
                                } else {
                                    "Provides a monthly overview of your progress across all habits combined. Green represents successful days, red represents failed days, yellow/amber represents pending logs, and dark grey indicates inactive days."
                                },
                                onClick = { t, e -> activeExplanation = t to e }
                            )
                        }

                        // Month Navigation Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (canPrevMonth) {
                                        val cal = monthViewCalendar.clone() as Calendar
                                        cal.add(Calendar.MONTH, -1)
                                        monthViewCalendar = cal
                                    }
                                },
                                enabled = canPrevMonth
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Month",
                                    tint = if (canPrevMonth) TextPrimary else TextPrimary.copy(alpha = 0.3f)
                                )
                            }

                            val monthNameAndYear = remember(monthViewCalendar, language) {
                                val sdfHeader = SimpleDateFormat("MMMM yyyy", if (language == "de") Locale.GERMANY else Locale.US)
                                sdfHeader.format(monthViewCalendar.time)
                            }
                            Text(
                                text = monthNameAndYear,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    if (canNextMonth) {
                                        val cal = monthViewCalendar.clone() as Calendar
                                        cal.add(Calendar.MONTH, 1)
                                        monthViewCalendar = cal
                                    }
                                },
                                enabled = canNextMonth
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = if (canNextMonth) TextPrimary else TextPrimary.copy(alpha = 0.3f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid Weekdays Row
                        val weekdaysInitials = remember(language) {
                            if (language == "de") listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                            else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekdaysInitials.forEach { weekday ->
                                Text(
                                    text = weekday,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid Days
                        val sdfDb = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
                        val temp = monthViewCalendar.clone() as Calendar
                        temp.set(Calendar.DAY_OF_MONTH, 1)
                        val daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
                        val leadEmptyDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
                        val totalGridCells = leadEmptyDays + daysInMonth

                        val rows = (totalGridCells + 6) / 7
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    if (cellIndex < leadEmptyDays || cellIndex >= totalGridCells) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val day = cellIndex - leadEmptyDays + 1
                                        val dayCal = (monthViewCalendar.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }
                                        val dateStr = sdfDb.format(dayCal.time)
                                        val (completed, total) = getHabitProgressForDate(dateStr, allHabits, allLogs)

                                        val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
                                        val isToday = dateStr == todayStr
                                        val isFuture = dateStr > todayStr

                                        val combinedStatus = if (isFuture) "INACTIVE" else getDayCombinedStatus(dateStr, allHabits, allLogs)
                                        val bgColor = when (combinedStatus) {
                                            "SUCCESS" -> SuccessGreen
                                            "FAILED" -> ErrorRed
                                            "PENDING" -> HabitYellow
                                            "PAUSED" -> PrimaryViolet.copy(alpha = 0.15f)
                                            else -> PrimaryViolet.copy(alpha = 0.15f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .background(bgColor, RoundedCornerShape(8.dp))
                                                .then(
                                                    if (isToday) {
                                                        Modifier.border(1.5.dp, PrimaryViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    } else Modifier
                                                )
                                                .clickable(enabled = !isFuture) {
                                                    viewModel.selectDate(dateStr)
                                                    onBack()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val textColor = when (combinedStatus) {
                                                "SUCCESS", "FAILED", "PENDING" -> DarkBg
                                                else -> if (total > 0) TextPrimary else TextSecondary
                                            }
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
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
                                            .background(PrimaryViolet.copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (language == "de") "Pausiert" else "Paused", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                                    text = perfectDaysStats.totalCompletedHabits.toString(),
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
        "PAUSED" -> PrimaryViolet.copy(alpha = 0.15f)
        else -> PrimaryViolet.copy(alpha = 0.15f) // Soft lila tint for inactive/future days
    }
    val textColor = when (status) {
        "SUCCESS", "FAILED", "PENDING" -> DarkBg
        else -> if (status == "INACTIVE" || status == "PAUSED") TextSecondary else TextPrimary
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
                    .scale(scale)
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
                                            imageVector = HabitIconMapping.getIcon(habit.icon),
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
    var unit by remember { mutableStateOf(editingHabit?.unit ?: (if (language == "de") "Liter" else "liters")) }
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
    val cal = remember { 
        Calendar.getInstance().apply { 
            if (editingHabit != null) {
                timeInMillis = editingHabit.startDate 
            } else {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } 
    }
    var startDateMillis by remember { mutableStateOf(cal.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedStartDate = remember(startDateMillis) {
        val sdf = SimpleDateFormat("d. MMMM yyyy", Locale.GERMANY)
        sdf.format(Date(startDateMillis))
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
                    text = "Icon",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HabitIconMapping.iconList.forEach { (key, vector) ->
                        val isSelected = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) PrimaryViolet else ProgressTrack,
                                    CircleShape
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = key,
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Color selector dots
                Text(
                    text = if (language == "de") "Farbe" else "Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HabitIconMapping.colorList.forEach { (key, colorObj) ->
                        val isSelected = selectedColor == key
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(colorObj, CircleShape)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = key },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(if (language == "de") "Einheit" else "Unit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryViolet,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = PrimaryViolet
                            )
                        )

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
                                focusedLabelColor = PrimaryViolet
                            )
                        )
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
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialTimeMs } }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val tempCal = Calendar.getInstance().apply {
        set(Calendar.MONTH, currentMonth)
        set(Calendar.YEAR, currentYear)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val offset = (tempCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val gridItems = mutableListOf<Int?>()
    for (i in 0 until offset) {
        gridItems.add(null)
    }
    for (i in 1..maxDays) {
        gridItems.add(i)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth == 0) {
                        currentMonth = 11
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
                    if (currentMonth == 11) {
                        currentMonth = 0
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

        val rowItems = gridItems.chunked(7)
        rowItems.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = if (c < week.size) week[c] else null
                    if (day != null) {
                        val isSelected = calendar.get(Calendar.DAY_OF_MONTH) == day &&
                                calendar.get(Calendar.MONTH) == currentMonth &&
                                calendar.get(Calendar.YEAR) == currentYear

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryViolet else Color.Transparent)
                                .clickable {
                                    val finalCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, currentYear)
                                        set(Calendar.MONTH, currentMonth)
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    onDateSelected(finalCal.timeInMillis)
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
        val index = (completed + total + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % german.size
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
            val index = (completed + total + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % german.size
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
            val index = (completed + total + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % german.size
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
            val index = (completed + total + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % german.size
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
            val index = (completed + total + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % german.size
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
    categoryIcon: ImageVector,
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
                        imageVector = categoryIcon,
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

            // Progress bar to Gold (100 days)
            val progressFraction = (longestStreak.toFloat() / 100f).coerceIn(0f, 1f)
            val percent = (progressFraction * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == "de") "Weg zur Gold-Serie" else "Path to Golden Streak",
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
    val perfectDaysState by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val perfectDaysStreak = perfectDaysState.perfectDaysStreak

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Freigeschaltet, 1 = Alle Erfolge

    // Calculate totals dynamically!
    val totalGlobalCompletions = remember(allLogs, habits) {
        allLogs.count { log ->
            val habit = habits.find { it.id == log.habitId }
            habit != null && isLogCompleted(habit, log)
        }
    }

    val unlockedCompletions = remember(totalGlobalCompletions) {
        listOf(10, 50, 200, 500).count { totalGlobalCompletions >= it }
    }
    
    val unlockedPerfectDays = remember(perfectDaysStreak) {
        listOf(7, 30, 100).count { perfectDaysStreak >= it }
    }

    val habitStreaksList = remember(habits, allLogs) {
        habits.map { habit ->
            val (_, longestStreak) = viewModel.calculateStreak(habit, allLogs)
            val hColor = HabitIconMapping.getColor(habit.color)
            val hIcon = HabitIconMapping.getIcon(habit.icon)
            Triple(habit, longestStreak, Pair(hColor, hIcon))
        }
    }

    val unlockedHabitStreaks = remember(habitStreaksList) {
        habitStreaksList.sumOf { (_, longestStreak, _) ->
            var count = 0
            if (longestStreak >= 7) count++
            if (longestStreak >= 14) count++
            if (longestStreak >= 30) count++
            if (longestStreak >= 100) count++
            count
        }
    }

    val totalUnlockedCount = unlockedCompletions + unlockedPerfectDays + unlockedHabitStreaks
    val totalPossibleCount = 4 + 3 + (habits.size * 4)

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
        // Fixed Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == "de") "Profil" else "Profile",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("profile_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
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

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalLogs = allLogs.size
                            Text(
                                text = totalLogs.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
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

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == "de") "ERFOLGE-FORTSCHRITT" else "ACHIEVEMENT PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
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
                            categoryIcon = style.second,
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
    logs: List<HabitLog>,
    language: String,
    calendarRows: List<List<CalendarCellState?>>,
    onInfoClick: (String, String) -> Unit
) {
    var activeFilter by remember { mutableStateOf("WEEK") }
    var verlaufOffset by remember { mutableStateOf(0) }

    val minVerlaufOffset = remember(activeFilter, habit.startDate) {
        val today = Calendar.getInstance()
        val start = Calendar.getInstance().apply { timeInMillis = habit.startDate }
        
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val rawOffset = when (activeFilter) {
            "WEEK" -> {
                val todayMonday = today.clone() as Calendar
                todayMonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                
                val startMonday = start.clone() as Calendar
                startMonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                
                val diffMillis = todayMonday.timeInMillis - startMonday.timeInMillis
                val diffWeeks = (diffMillis / (7 * 24 * 60 * 60 * 1000L)).toInt()
                -diffWeeks
            }
            "MONTH" -> {
                val diffYears = today.get(Calendar.YEAR) - start.get(Calendar.YEAR)
                val diffMonths = today.get(Calendar.MONTH) - start.get(Calendar.MONTH)
                -(diffYears * 12 + diffMonths)
            }
            "YEAR" -> {
                val diffYears = today.get(Calendar.YEAR) - start.get(Calendar.YEAR)
                -diffYears
            }
            else -> 0
        }
        rawOffset.coerceAtMost(0)
    }

    LaunchedEffect(minVerlaufOffset) {
        if (verlaufOffset < minVerlaufOffset) {
            verlaufOffset = minVerlaufOffset
        }
    }

    val totalDays = calendarRows.flatten().filterNotNull().filter { it.status != "INACTIVE" }
    val successCount = totalDays.count { it.status == "SUCCESS" }
    val failedCount = totalDays.count { it.status == "FAILED" }
    val pendingCount = totalDays.count { it.status == "PENDING" }
    val pausedCount = totalDays.count { it.status == "PAUSED" }
    val totalValidDays = totalDays.size.coerceAtLeast(1)
    val activeDaysCount = totalDays.filter { it.status != "PAUSED" }.size.coerceAtLeast(1)

    val successRate = ((successCount.toFloat() / activeDaysCount.toFloat()) * 100).toInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Card 1: Verlauf (Balkendiagramm) ---
        if (habit.type == "NUMBER" || habit.type == "NUMERICAL") {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            ) {
            Column(modifier = Modifier.padding(20.dp)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                val verlaufTitle = remember(activeFilter, verlaufOffset, language) {
                    val cal = Calendar.getInstance()
                    when (activeFilter) {
                        "WEEK" -> {
                            cal.add(Calendar.WEEK_OF_YEAR, verlaufOffset)
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            val start = cal.time
                            cal.add(Calendar.DAY_OF_YEAR, 6)
                            val end = cal.time
                            val sdf = SimpleDateFormat("d. MMM", if (language == "de") Locale.GERMANY else Locale.US)
                            "${sdf.format(start)} - ${sdf.format(end)} ${cal.get(Calendar.YEAR)}"
                        }
                        "MONTH" -> {
                            cal.add(Calendar.MONTH, verlaufOffset)
                            val sdf = SimpleDateFormat("MMMM yyyy", if (language == "de") Locale.GERMANY else Locale.US)
                            sdf.format(cal.time)
                        }
                        else -> {
                            cal.add(Calendar.YEAR, verlaufOffset)
                            "${cal.get(Calendar.YEAR)}"
                        }
                    }
                }

                val canPrevVerlauf = verlaufOffset > minVerlaufOffset
                val canNextVerlauf = verlaufOffset < 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (canPrevVerlauf) verlaufOffset-- },
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
                        onClick = { if (canNextVerlauf) verlaufOffset++ },
                        enabled = canNextVerlauf
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (canNextVerlauf) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val barData = remember(activeFilter, verlaufOffset, logs, habit) {
                    val list = mutableListOf<Pair<String, Float>>()
                    val cal = Calendar.getInstance()
                    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    
                    val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val startSdfStr = startSdf.format(Date(habit.startDate))
                    val todayStr = sdfDb.format(Date())

                    when (activeFilter) {
                        "WEEK" -> {
                            cal.add(Calendar.WEEK_OF_YEAR, verlaufOffset)
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            val labels = if (language == "de") listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                            for (i in 0 until 7) {
                                val dStr = sdfDb.format(cal.time)
                                val isOutOfRange = dStr < startSdfStr || dStr > todayStr
                                val ratio = if (isOutOfRange) {
                                    0f
                                } else {
                                    val logForDate = logs.find { it.date == dStr }
                                    if (habit.type == "BINARY") {
                                        val isSuccess = if (logForDate == null) {
                                            habit.isNegative
                                        } else {
                                            when (logForDate.value) {
                                                -2f -> true
                                                -1f -> false
                                                else -> !habit.isNegative
                                            }
                                        }
                                        if (isSuccess) 1f else 0f
                                    } else {
                                        if (logForDate == null) {
                                            if (habit.isNegative) 1f else 0f
                                        } else {
                                            if (habit.isNegative) {
                                                if (logForDate.value >= habit.targetValue) 0f else 1f
                                            } else {
                                                (logForDate.value / habit.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            }
                                        }
                                    }
                                }
                                list.add(labels[i] to ratio)
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        "MONTH" -> {
                            val baseCal = Calendar.getInstance().apply {
                                timeInMillis = cal.timeInMillis
                                add(Calendar.MONTH, verlaufOffset)
                            }
                            val daysCount = baseCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            for (i in 1..daysCount) {
                                val dayCal = Calendar.getInstance().apply {
                                    timeInMillis = baseCal.timeInMillis
                                    set(Calendar.DAY_OF_MONTH, i)
                                }
                                val dStr = sdfDb.format(dayCal.time)
                                val isOutOfRange = dStr < startSdfStr || dStr > todayStr
                                val ratio = if (isOutOfRange) {
                                    0f
                                } else {
                                    val logForDate = logs.find { it.date == dStr }
                                    if (habit.type == "BINARY") {
                                        val isSuccess = if (logForDate == null) {
                                            habit.isNegative
                                        } else {
                                            when (logForDate.value) {
                                                -2f -> true
                                                -1f -> false
                                                else -> !habit.isNegative
                                            }
                                        }
                                        if (isSuccess) 1f else 0f
                                    } else {
                                        if (logForDate == null) {
                                            if (habit.isNegative) 1f else 0f
                                        } else {
                                            if (habit.isNegative) {
                                                if (logForDate.value >= habit.targetValue) 0f else 1f
                                            } else {
                                                (logForDate.value / habit.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            }
                                        }
                                    }
                                }
                                list.add(i.toString() to ratio)
                            }
                        }
                        else -> {
                            cal.add(Calendar.YEAR, verlaufOffset)
                            val yearNum = cal.get(Calendar.YEAR)
                            val months = if (language == "de") 
                                listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
                            else 
                                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            
                            for (m in 0 until 12) {
                                cal.set(Calendar.YEAR, yearNum)
                                cal.set(Calendar.MONTH, m)
                                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                var completedDays = 0
                                var validDaysInMonth = 0
                                for (d in 1..maxDays) {
                                    cal.set(Calendar.DAY_OF_MONTH, d)
                                    val dStr = sdfDb.format(cal.time)
                                    if (dStr >= startSdfStr && dStr <= todayStr) {
                                        validDaysInMonth++
                                        val logForDate = logs.find { it.date == dStr }
                                        val isSuccess = if (habit.type == "BINARY") {
                                            if (logForDate == null) {
                                                habit.isNegative
                                            } else {
                                                when (logForDate.value) {
                                                    -2f -> true
                                                    -1f -> false
                                                    else -> !habit.isNegative
                                                }
                                            }
                                        } else {
                                            if (logForDate == null) {
                                                habit.isNegative
                                            } else {
                                                if (habit.isNegative) {
                                                    logForDate.value < habit.targetValue
                                                } else {
                                                    logForDate.value >= habit.targetValue
                                                }
                                            }
                                        }
                                        if (isSuccess) completedDays++
                                    }
                                }
                                val ratio = if (validDaysInMonth > 0) completedDays.toFloat() / validDaysInMonth.toFloat() else 0f
                                list.add(months[m] to ratio)
                            }
                        }
                    }
                    list
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
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
                                    .height(110.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

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
                                activeFilter = key
                                verlaufOffset = 0
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

        // --- Card 2: Erfolg / Misserfolg (Donut-Diagramm) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Erfolg / Misserfolg",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "de") "ERFOLG / MISSERFOLG" else "SUCCESS / FAILURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    InfoIconButton(
                        title = if (language == "de") "Erfolgs-Verteilung" else "Success Distribution",
                        explanation = if (language == "de") {
                            "Eine ungewichtete Übersicht der Erledigungszustände deiner Gewohnheit über alle Tage hinweg. Grün steht für erfolgreiche Einträge, Rot für gescheiterte Tage und Gelb für ausstehende Einträge."
                        } else {
                            "An unweighted overview of the completion states of your habit over all days. Green represents successful logs, red represents failed days, and yellow represents pending entries."
                        },
                        onClick = onInfoClick
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            val sweepSuccess = (successCount.toFloat() / totalValidDays.toFloat()) * 360f
                            val sweepFailed = (failedCount.toFloat() / totalValidDays.toFloat()) * 360f
                            val sweepPending = (pendingCount.toFloat() / totalValidDays.toFloat()) * 360f
                            val sweepPaused = (pausedCount.toFloat() / totalValidDays.toFloat()) * 360f

                            val stroke = Stroke(width = 30f)

                            drawArc(
                                color = SuccessGreen,
                                startAngle = -90f,
                                sweepAngle = sweepSuccess,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = ErrorRed,
                                startAngle = -90f + sweepSuccess,
                                sweepAngle = sweepFailed,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = HabitYellow,
                                startAngle = -90f + sweepSuccess + sweepFailed,
                                sweepAngle = sweepPending,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = HabitOrange,
                                startAngle = -90f + sweepSuccess + sweepFailed + sweepPending,
                                sweepAngle = sweepPaused,
                                useCenter = false,
                                style = stroke
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${successRate}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "QUOTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SuccessGreen))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (language == "de") "Erreicht" else "Completed"}: $successCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ErrorRed))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (language == "de") "Gescheitert" else "Failed"}: $failedCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(HabitYellow))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (language == "de") "Ausstehend" else "Pending"}: $pendingCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(HabitOrange))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (language == "de") "Pausiert" else "Paused"}: $pausedCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiAnimation(onFinished: () -> Unit) {
    val particles = remember {
        List(24) {
            Triple(
                (0..360).random().toFloat(),
                (15..45).random().toFloat(),
                listOf(SuccessGreen, HabitYellow, HabitRed, PrimaryViolet).random()
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
        particles.forEach { (angle, speed, color) ->
            val radian = Math.toRadians(angle.toDouble())
            val distance = speed * progress * 1.5f
            val x = (center.x + Math.cos(radian) * distance).toFloat()
            val y = (center.y + Math.sin(radian) * distance).toFloat()
            val alpha = (1f - progress).coerceIn(0f, 1f)
            
            drawCircle(
                color = color,
                radius = 6f * (1f - progress),
                center = androidx.compose.ui.geometry.Offset(x, y),
                alpha = alpha
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
        modifier = modifier.size(18.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Help Info",
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
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

            Canvas(modifier = modifier) {
                val width = size.width
                val height = size.height
                val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                val outerRadius = width * 0.45f

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
                    style = Stroke(width = 3f * density)
                )

                // Grain/texture for streak tiers
                when (tier) {
                    "WOOD" -> {
                        drawCircle(
                            color = tierSecondary.copy(alpha = 0.25f * alpha),
                            radius = outerRadius * 0.75f,
                            center = center,
                            style = Stroke(width = 1.5f * density)
                        )
                        drawCircle(
                            color = tierSecondary.copy(alpha = 0.15f * alpha),
                            radius = outerRadius * 0.5f,
                            center = center,
                            style = Stroke(width = 1f * density)
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
                drawPath(
                    path = path,
                    color = tierColor.copy(alpha = alpha)
                )

                // Inner flame core
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

                drawPath(
                    path = innerPath,
                    color = innerFlameColor.copy(alpha = 0.8f * alpha)
                )
            }
        }
    }
}

@Composable
fun TargetWithArrowsIcon(
    arrowCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
        val outerRadius = width * 0.42f
        
        // Draw the target concentric rings
        // Outermost: Dark purple/violet background border
        drawCircle(
            color = Color(0xFF673AB7).copy(alpha = 0.15f),
            radius = outerRadius,
            center = center
        )
        drawCircle(
            color = Color(0xFF673AB7),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 1.5f * density)
        )
        // Red / Crimson ring
        drawCircle(
            color = Color(0xFFE91E63),
            radius = outerRadius * 0.7f,
            center = center,
            style = Stroke(width = 2f * density)
        )
        // Golden Center Bullseye
        drawCircle(
            color = Color(0xFFFFC107),
            radius = outerRadius * 0.3f,
            center = center
        )
        
        // Draw arrows piercing the bullseye at different angles
        // Based on arrowCount (e.g. 1 to 6 arrows)
        val baseAngles = listOf(-45f, 135f, 15f, 75f, -105f, -165f)
        for (i in 0 until arrowCount.coerceAtMost(6)) {
            val angleDeg = baseAngles[i]
            val angleRad = Math.toRadians(angleDeg.toDouble())
            
            // We rotate the draw coordinates around center
            withTransform({
                rotate(angleDeg, center)
            }) {
                val shaftLength = width * 0.65f
                val startX = center.x - shaftLength * 0.6f
                val endX = center.x + shaftLength * 0.35f
                val y = center.y
                
                // 1. Arrow Shaft (wood brown color)
                drawLine(
                    color = Color(0xFF8D6E63),
                    start = androidx.compose.ui.geometry.Offset(startX, y),
                    end = androidx.compose.ui.geometry.Offset(endX, y),
                    strokeWidth = 2f * density,
                    cap = StrokeCap.Round
                )
                
                // 2. Arrow Head (Steel silver color) pointing into the gold center
                // The arrowhead points to the right (towards endX)
                val arrowHeadSize = width * 0.08f
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(endX, y)
                        lineTo(endX - arrowHeadSize, y - arrowHeadSize * 0.5f)
                        lineTo(endX - arrowHeadSize * 0.7f, y)
                        lineTo(endX - arrowHeadSize, y + arrowHeadSize * 0.5f)
                        close()
                    },
                    color = Color(0xFFB0BEC5)
                )
                
                // 3. Fletching (colored feathers on the back startX)
                // Feather 1
                drawLine(
                    color = Color(0xFFFF5252),
                    start = androidx.compose.ui.geometry.Offset(startX, y),
                    end = androidx.compose.ui.geometry.Offset(startX - arrowHeadSize * 0.8f, y - arrowHeadSize * 0.5f),
                    strokeWidth = 1.5f * density,
                    cap = StrokeCap.Round
                )
                // Feather 2
                drawLine(
                    color = Color(0xFFFF5252),
                    start = androidx.compose.ui.geometry.Offset(startX, y),
                    end = androidx.compose.ui.geometry.Offset(startX - arrowHeadSize * 0.8f, y + arrowHeadSize * 0.5f),
                    strokeWidth = 1.5f * density,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun CalendarWithRingsIcon(
    ringCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
        
        // Define orbit ring colors
        val ringColors = listOf(
            Color(0xFF4CAF50), // Green for 3 days
            Color(0xFFFFC107), // Yellow/Bronze for 10 days
            Color(0xFF9C27B0), // Purple for 30 days
            Color(0xFFFF9800), // Gold/Amber for 100 days
            Color(0xFF00BCD4)  // Cyan/Cosmic for 365 days
        )
        
        // Draw outer orbits (concentric orbital rings with varying rotation offsets)
        for (i in 0 until ringCount.coerceAtMost(5)) {
            val radius = width * (0.35f + (i * 0.045f))
            withTransform({
                rotate(i * 35f, center)
            }) {
                drawCircle(
                    color = ringColors[i],
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 1.2f * density,
                        cap = StrokeCap.Round
                    )
                )
                // Draw a tiny orbital glowing planet/dot on the ring
                val angleRad = Math.toRadians((i * 72f).toDouble())
                val dotX = center.x + radius * Math.cos(angleRad).toFloat()
                val dotY = center.y + radius * Math.sin(angleRad).toFloat()
                drawCircle(
                    color = ringColors[i],
                    radius = 3f * density,
                    center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                )
            }
        }
        
        // Draw the main calendar body in the center
        val calSize = width * 0.38f
        val calLeft = center.x - calSize / 2f
        val calTop = center.y - calSize / 2f
        
        // Calendar Background Card
        drawRoundRect(
            color = Color(0xFF1E1E2E),
            size = androidx.compose.ui.geometry.Size(calSize, calSize),
            topLeft = androidx.compose.ui.geometry.Offset(calLeft, calTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        
        // Calendar Header Banner (Red/Pinkish)
        val headerHeight = calSize * 0.3f
        drawRoundRect(
            color = Color(0xFFE91E63),
            size = androidx.compose.ui.geometry.Size(calSize, headerHeight),
            topLeft = androidx.compose.ui.geometry.Offset(calLeft, calTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Fill lower rounded corners back to square for calendar card divider
        drawRect(
            color = Color(0xFFE91E63),
            size = androidx.compose.ui.geometry.Size(calSize, headerHeight - 2.dp.toPx()),
            topLeft = androidx.compose.ui.geometry.Offset(calLeft, calTop + 2.dp.toPx())
        )
        
        // Calendar Binder Rings (two little binder hooks on top)
        val ringSizeWidth = calSize * 0.1f
        val ringHeight = calSize * 0.2f
        val ringOffsetLeft = calLeft + calSize * 0.2f
        val ringOffsetRight = calLeft + calSize * 0.7f
        
        drawRoundRect(
            color = Color(0xFFB0BEC5),
            size = androidx.compose.ui.geometry.Size(ringSizeWidth, ringHeight),
            topLeft = androidx.compose.ui.geometry.Offset(ringOffsetLeft, calTop - ringHeight * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * density, 1.5f * density)
        )
        drawRoundRect(
            color = Color(0xFFB0BEC5),
            size = androidx.compose.ui.geometry.Size(ringSizeWidth, ringHeight),
            topLeft = androidx.compose.ui.geometry.Offset(ringOffsetRight, calTop - ringHeight * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * density, 1.5f * density)
        )
        
        // Calendar Grid/Dots in body (mimics days)
        val dotRadius = 1.5f * density
        val startGridX = calLeft + calSize * 0.2f
        val startGridY = calTop + headerHeight + calSize * 0.18f
        val gridSpacing = calSize * 0.2f
        
        for (row in 0..1) {
            for (col in 0..2) {
                drawCircle(
                    color = Color(0xFF94A3B8),
                    radius = dotRadius,
                    center = androidx.compose.ui.geometry.Offset(
                        startGridX + col * gridSpacing,
                        startGridY + row * gridSpacing
                    )
                )
            }
        }
        
        // Checkmark overlay (symbolizing completed perfect day)
        val checkSize = calSize * 0.35f
        val checkLeft = calLeft + calSize * 0.45f
        val checkTop = calTop + headerHeight + calSize * 0.15f
        
        val checkPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(checkLeft, checkTop + checkSize * 0.5f)
            lineTo(checkLeft + checkSize * 0.35f, checkTop + checkSize * 0.85f)
            lineTo(checkLeft + checkSize * 0.9f, checkTop + checkSize * 0.2f)
        }
        drawPath(
            path = checkPath,
            color = Color(0xFF4CAF50),
            style = Stroke(width = 2.5f * density, cap = StrokeCap.Round)
        )
    }
}