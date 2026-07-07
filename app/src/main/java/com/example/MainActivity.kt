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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    var widgetAddValueHabitId by remember { mutableStateOf<Int?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var longPressedHabit by remember { mutableStateOf<Habit?>(null) }
    var showDeleteConfirmHabit by remember { mutableStateOf<Habit?>(null) }

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

    // Synchronize homescreen widget whenever habits/logs change without causing root recompositions
    LaunchedEffect(Unit) {
        kotlinx.coroutines.flow.combine(viewModel.allHabits, viewModel.allLogs) { _, _ -> Unit }
            .collect {
                HabitWidgetProvider.triggerUpdate(context)
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != null && !currentRoute.startsWith("DETAIL") && currentRoute != "MORE" && editingHabit == null) {
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
                        onSave = { name, isNeg, cat, icon, color, type, unit, target, freq, start ->
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
                                    startDate = start
                                )
                                viewModel.updateHabit(updated)
                                editingHabit = null
                                Toast.makeText(
                                    context,
                                    if (language == "de") "Gewohnheit aktualisiert!" else "Habit updated!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.addHabit(name, cat, icon, color, isNeg, type, unit, target, freq, start)
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
            if (currentRoute != null && !currentRoute.startsWith("DETAIL") && currentRoute != "MORE" && editingHabit == null) {
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
                AlertDialog(
                    onDismissRequest = { longPressedHabit = null },
                    containerColor = DarkCard,
                    title = {
                        Text(
                            text = longPressedHabit!!.name,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = if (language == "de") {
                                "Wähle eine Aktion für diese Gewohnheit:"
                            } else {
                                "Choose an action for this habit:"
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            onClick = {
                                val habitToEdit = longPressedHabit!!
                                longPressedHabit = null
                                editingHabit = habitToEdit
                                navController.navigate("CREATE") {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text(if (language == "de") "Bearbeiten" else "Edit")
                        }
                    },
                    dismissButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            onClick = {
                                val habitToDelete = longPressedHabit!!
                                longPressedHabit = null
                                showDeleteConfirmHabit = habitToDelete
                            }
                        ) {
                            Text(if (language == "de") "Löschen" else "Delete")
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
            selected = selectedTab == "CREATE",
            onClick = { onTabSelected("CREATE") },
            icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
            label = { 
                Text(
                    text = if (language == "de") "Erstellen" else "Create",
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
            modifier = Modifier.testTag("nav_add_habit")
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
            color = if (isSelected) TextPrimary else if (isToday) PrimaryViolet else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Date Number with a highlight circle/capsule if selected or today
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = if (isSelected) PrimaryViolet else if (isToday) PrimaryViolet.copy(alpha = 0.15f) else Color.Transparent,
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
                color = if (isSelected) DarkBg else if (isToday) PrimaryViolet else TextSecondary,
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
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dateMs = try {
        val baseTime = sdfDb.parse(dateStr)?.time ?: 0L
        baseTime + 86399000L
    } catch (e: Exception) {
        0L
    }
    val activeHabits = habits.filter { it.startDate <= dateMs }
    if (activeHabits.isEmpty()) return 0 to 0

    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
    var completedCount = 0
    activeHabits.forEach { habit ->
        val log = logsMap[habit.id]
        if (log != null) {
            if (habit.type == "BINARY") {
                completedCount++
            } else {
                if (log.value >= habit.targetValue) {
                    completedCount++
                }
            }
        } else if (habit.isNegative) {
            completedCount++
        }
    }
    return completedCount to activeHabits.size
}

fun getDayCombinedStatus(dateStr: String, habits: List<Habit>, logs: List<HabitLog>): String {
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdfDb.format(Date())
    if (dateStr > todayStr) return "INACTIVE"

    val dateMs = try {
        val baseTime = sdfDb.parse(dateStr)?.time ?: 0L
        baseTime + 86399000L
    } catch (e: Exception) {
        0L
    }
    val activeHabits = habits.filter { it.startDate <= dateMs }
    if (activeHabits.isEmpty()) return "INACTIVE"

    val logsMap = logs.filter { it.date == dateStr }.associateBy { it.habitId }
    
    var anyPending = false
    var anyFailed = false
    var anySuccess = false

    activeHabits.forEach { habit ->
        val log = logsMap[habit.id]
        val isCompleted = if (habit.isNegative) {
            log == null
        } else {
            log != null && (habit.type == "BINARY" || log.value >= habit.targetValue)
        }

        val hStatus = when {
            isCompleted -> "SUCCESS"
            habit.isNegative -> "FAILED"
            else -> {
                if (log != null && log.value > 0f) {
                    "FAILED"
                } else {
                    "PENDING"
                }
            }
        }

        if (hStatus == "PENDING") anyPending = true
        if (hStatus == "FAILED") anyFailed = true
        if (hStatus == "SUCCESS") anySuccess = true
    }

    return when {
        anyPending -> "PENDING"
        anyFailed -> "FAILED"
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

    var isMonthViewExpanded by remember { mutableStateOf(false) }
    var monthViewCalendar by remember(selectedDate) {
        val cal = Calendar.getInstance()
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val d = sdfDb.parse(selectedDate)
            if (d != null) cal.time = d
        } catch (e: Exception) {}
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
            
            val canPrevWeek = remember(weekStartCalendar, oldestHabitDateMs) {
                val minWeekStart = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    timeInMillis = oldestHabitDateMs
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                weekStartCalendar.timeInMillis > minWeekStart.timeInMillis
            }

            val canNextWeek = remember(weekStartCalendar) {
                val maxWeekStart = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    add(Calendar.WEEK_OF_YEAR, 4)
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

                // Small arrow/chevron under the weekly calendar scroller to toggle Month View
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isMonthViewExpanded = !isMonthViewExpanded },
                        modifier = Modifier.size(36.dp).testTag("toggle_month_view_button")
                    ) {
                        Icon(
                            imageVector = if (isMonthViewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isMonthViewExpanded) "Collapse Month View" else "Expand Month View",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Month View Card
                AnimatedVisibility(
                    visible = isMonthViewExpanded,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
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

                                            val isSelected = dateStr == selectedDate
                                            val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
                                            val isToday = dateStr == todayStr
                                            val isFuture = dateStr > todayStr

                                            val combinedStatus = if (isFuture) "INACTIVE" else getDayCombinedStatus(dateStr, allHabits, allLogs)
                                            val bgColor = when (combinedStatus) {
                                                "SUCCESS" -> SuccessGreen
                                                "FAILED" -> ErrorRed
                                                "PENDING" -> HabitYellow
                                                else -> PrimaryViolet.copy(alpha = 0.15f) // INACTIVE/Future (Soft Lila)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                                    .background(bgColor, RoundedCornerShape(8.dp))
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.border(2.dp, PrimaryViolet, RoundedCornerShape(8.dp))
                                                        } else if (isToday) {
                                                            Modifier.border(1.5.dp, PrimaryViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        } else Modifier
                                                    )
                                                    .clickable {
                                                        viewModel.selectDateAndSyncWeek(dateStr)
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
                        }
                    }
                }
            }
        }

        item(key = "today_habits_header") {
            Text(
                text = if (language == "de") "Gewohnheiten" else "Habits",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "de") "TAGESFORTSCHRITT" else "DAILY PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${(fraction * 100).toInt()}% ($progressText)",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (fraction >= 1.0f) SuccessGreen else PrimaryViolet,
                    trackColor = ProgressTrack
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = encouragementText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
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
                
                val status = when {
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
                    hasLog = hasLog,
                    onToggle = onToggleRemembered,
                    onAddQuantity = onAddQuantityRemembered,
                    onLongClick = onHabitLongClick,
                    language = language
                )
            }
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
    hasLog: Boolean,
    onToggle: (Int, Boolean) -> Unit,
    onAddQuantity: (Int, Float, Float) -> Unit,
    onLongClick: (Habit) -> Unit,
    language: String
) {
    val habitColor = HabitIconMapping.getColor(habit.color)

    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isCompleted -> SuccessBg
            isFailed -> FailedBg
            else -> DarkCard
        },
        animationSpec = tween(120),
        label = "bgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> SuccessGreen
            isFailed -> FailedRed
            else -> DarkBorder
        },
        animationSpec = tween(120),
        label = "borderColor"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (isCompleted || isFailed) 1.05f else 1.0f,
        animationSpec = tween(100),
        label = "checkboxScale"
    )

    val borderBrush = when {
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
                        text = if (habit.type == "BINARY") {
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
                            isCompleted -> SuccessGreen
                            isFailed -> FailedRed
                            else -> TextSecondary
                        }
                    )
                }
            }

            if (habit.type == "NUMBER") {
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
                                isCompleted -> SuccessGreen
                                isFailed -> FailedRed
                                else -> Color.Transparent
                            }, 
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isCompleted -> SuccessGreen
                                isFailed -> FailedRed
                                else -> TextSecondary.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
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
    onHabitClick: (Int) -> Unit
) {
    val strength by viewModel.totalStrength.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreakOfAll.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.statsScreenData.collectAsStateWithLifecycle()
    val todayProgressTuple by viewModel.todayProgress.collectAsStateWithLifecycle()
    val perfectDaysStats by viewModel.perfectDaysStats.collectAsStateWithLifecycle()

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isExpanded by remember { mutableStateOf(true) }

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

            // Overview Section Header
            item {
                Text(
                    text = if (language == "de") "STATISTIKEN INSGESAMT" else "OVERALL STATISTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
            }

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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = encouragementText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Row with the 3 most important metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Habits Count Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "Habits Count",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "de") "GEWOHN." else "HABITS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                InfoIconButton(
                                    title = if (language == "de") "Gewohnheiten" else "Habits",
                                    explanation = if (language == "de") {
                                        "Die Gesamtzahl der aktiven Gewohnheiten, die du derzeit verfolgst."
                                    } else {
                                        "The total number of active habits you are currently tracking."
                                    },
                                    onClick = { t, e -> activeExplanation = t to e }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = habitsWithStats.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Perfect Days Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Perfect Days",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "de") "PERF. TG." else "PERF. DAYS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = perfectDaysStats.totalPerfectDays.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Total Completed Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Logs",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "de") "ERLEDIGT" else "LOGS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = perfectDaysStats.totalCompletedHabits.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Collapsible Row for less common metrics
            if (isExpanded) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Perfect Days Streak Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (language == "de") "PERF. SERIE" else "PERF. STREAK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
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
                                Spacer(modifier = Modifier.height(4.dp))
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
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = "Completion Rate",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (language == "de") "ERFOLGSQUOTE" else "COMPL. RATE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
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
                                Spacer(modifier = Modifier.height(4.dp))
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
                    val status = model.past7DaysStatuses[i]
                    val cellColor = when (status) {
                        "SUCCESS" -> SuccessGreen
                        "FAILED" -> ErrorRed
                        "PENDING" -> HabitYellow
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
                                .background(cellColor)
                        )
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

        // Milestones Unlocked row
        item(key = "detail_milestones") {
            StreakMilestonesCard(longestStreak = longestStreak, language = language)
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
        else -> PrimaryViolet.copy(alpha = 0.15f) // Soft lila tint for inactive/future days
    }
    val textColor = when (status) {
        "SUCCESS", "FAILED", "PENDING" -> DarkBg
        else -> TextPrimary
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

        // Legende (Erreicht, Gescheitert, Ausstehend)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (language == "de") "Erreicht" else "Completed", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ErrorRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (language == "de") "Gescheitert" else "Failed", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(HabitYellow)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (language == "de") "Ausstehend" else "Pending", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
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
                    .background(color.copy(alpha = 0.12f), CircleShape)
                    .border(1.5.dp, color, CircleShape)
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
                Icon(
                    imageVector = rank.icon,
                    contentDescription = rankName,
                    tint = if (milestone.isUnlocked) color else TextSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.size(30.dp)
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
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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

        // Push notification reminder card
        item {
            val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
            val notificationsHour by viewModel.notificationsHour.collectAsStateWithLifecycle()
            val notificationsMinute by viewModel.notificationsMinute.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (language == "de") "Erinnerungen" else "Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = PrimaryViolet,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ProgressTrack
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == "de") {
                            "Erhalte eine tägliche Push-Benachrichtigung für deine ausstehenden Gewohnheiten zur gewünschten Uhrzeit."
                        } else {
                            "Receive a daily push notification reminder for your pending habits at your preferred time."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (notificationsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (language == "de") "Erinnerungszeit einstellen:" else "Set reminder time:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "de") "Stunde" else "Hour",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val newHour = (notificationsHour - 1 + 24) % 24
                                            viewModel.updateNotificationTime(newHour, notificationsMinute)
                                        },
                                        modifier = Modifier.size(36.dp).background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease Hour", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text(
                                        text = String.format("%02d", notificationsHour),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = {
                                            val newHour = (notificationsHour + 1) % 24
                                            viewModel.updateNotificationTime(newHour, notificationsMinute)
                                        },
                                        modifier = Modifier.size(36.dp).background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase Hour", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displaySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp),
                                fontWeight = FontWeight.Bold
                            )

                            // Minute Selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Minute",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val newMinute = (notificationsMinute - 5 + 60) % 60
                                            viewModel.updateNotificationTime(notificationsHour, newMinute)
                                        },
                                        modifier = Modifier.size(36.dp).background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease Minute", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text(
                                        text = String.format("%02d", notificationsMinute),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = {
                                            val newMinute = (notificationsMinute + 5) % 60
                                            viewModel.updateNotificationTime(notificationsHour, newMinute)
                                        },
                                        modifier = Modifier.size(36.dp).background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase Minute", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
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

// CREATE HABIT SCREEN
@Composable
fun CreateHabitScreen(
    language: String,
    onDismiss: () -> Unit,
    onSave: (name: String, isNegative: Boolean, category: String, icon: String, color: String, type: String, unit: String, target: Float, freq: String, startMs: Long) -> Unit,
    editingHabit: Habit? = null
) {
    var isNegative by remember { mutableStateOf(editingHabit?.isNegative ?: false) } // Aufbauen = false, Abgewöhnen = true
    var showGoalExplanation by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(editingHabit?.name ?: "") }
    var category by remember { mutableStateOf(editingHabit?.category ?: (if (language == "de") "Allgemein" else "General")) }
    var selectedIcon by remember { mutableStateOf(editingHabit?.icon ?: "sparkle") }
    var selectedColor by remember { mutableStateOf(editingHabit?.color ?: "purple") }
    var type by remember { mutableStateOf(editingHabit?.type ?: "BINARY") } // "BINARY" or "NUMBER"
    var unit by remember { mutableStateOf(editingHabit?.unit ?: (if (language == "de") "Liter" else "liters")) }
    var targetValueStr by remember { mutableStateOf(editingHabit?.targetValue?.toInt()?.toString() ?: "1") }
    var frequency by remember { mutableStateOf(editingHabit?.frequency ?: "DAILY") }
    
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingHabit != null) {
                        if (language == "de") "Gewohnheit bearbeiten" else "Edit Habit"
                    } else {
                        if (language == "de") "Neue Gewohnheit" else "New Habit"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Goal Segment (Aufbauen / Abgewöhnen)
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
                    IconButton(
                        onClick = { showGoalExplanation = !showGoalExplanation },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Goal Info",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showGoalExplanation) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProgressTrack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (language == "de") "💡 Gewohnheitstypen" else "💡 Habit Types",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "de") {
                                    "• Aufbauen (Aufsteigend): Positive Gewohnheiten (z.B. Sport, Lesen), die du etablieren willst.\n" +
                                    "• Abgewöhnen (Absteigend): Negative Muster (z.B. Rauchen, Fastfood), die du reduzieren oder ganz vermeiden willst."
                                } else {
                                    "• Build (Upward): Positive habits (e.g. sports, reading) you want to establish.\n" +
                                    "• Quit (Downward): Negative patterns (e.g. smoking, fast food) you want to reduce or avoid."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isNegative = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isNegative) PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Build Up",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "de") "Aufbauen" else "Build",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "de") "Routine etablieren" else "Establish routine",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 10.sp,
                                    color = TextPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { isNegative = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNegative) PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Quit Down",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "de") "Abgewöhnen" else "Quit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "de") "Verhalten reduzieren" else "Reduce habit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 10.sp,
                                    color = TextPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text(if (language == "de") "z.B. Meditieren" else "e.g., Meditate") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("habit_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = PrimaryViolet
                    )
                )

                // Icons grid
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HabitIconMapping.iconList.forEach { (key, vector) ->
                        val isSelected = selectedIcon == key
                        IconButton(
                            onClick = { selectedIcon = key },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isSelected) PrimaryViolet else ProgressTrack,
                                    CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryViolet else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(vector, contentDescription = key, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Color selector dots
                Text(
                    text = if (language == "de") "Farbe" else "Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HabitIconMapping.colorList.forEach { (key, colorObj) ->
                        val isSelected = selectedColor == key
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(colorObj, CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = key },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Type Toggle (Ja/Nein vs Zahlenbasiert)
                Text(
                    text = if (language == "de") "Typ" else "Type",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { type = "BINARY" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "BINARY") PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "de") "Ja / Nein" else "Yes / No",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == "de") "Einmal abhaken" else "Simple checkbox",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 10.sp,
                                color = TextPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Button(
                        onClick = { type = "NUMBER" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "NUMBER") PrimaryViolet else ProgressTrack
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "de") "Zahlenbasiert" else "Numeric",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == "de") "Mengen tracken" else "Track quantity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 10.sp,
                                color = TextPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (type == "NUMBER") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(if (language == "de") "Einheit" else "Unit") },
                            modifier = Modifier.weight(1f),
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryViolet,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = PrimaryViolet
                            )
                        )
                    }
                }

                // Start Date Trigger Row
                Text(
                    text = if (language == "de") "Startdatum" else "Start Date",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = ProgressTrack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = !showDatePicker }
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = PrimaryViolet)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = formattedStartDate, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Open Date Picker", tint = TextSecondary)
                    }
                }

                if (showDatePicker) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(12.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        SimpleDatePickerView(
                            initialTimeMs = startDateMillis,
                            onDateSelected = {
                                startDateMillis = it
                                showDatePicker = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action buttons (Speichern, Abbrechen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (language == "de") "Abbrechen" else "Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val targetVal = targetValueStr.toFloatOrNull() ?: 1f
                                onSave(name, isNegative, category, selectedIcon, selectedColor, type, unit, targetVal, frequency, startDateMillis)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_habit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (language == "de") "Speichern" else "Save", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
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
    val title: String,
    val description: String,
    val habitName: String? = null,
    val habitColor: Color? = null,
    val targetValue: Int
)

@Composable
fun ProfileScreen(
    viewModel: HabitsViewModel,
    language: String,
    onSettingsClick: () -> Unit
) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val perfectDaysState by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val totalPerfectDays = perfectDaysState.totalPerfectDays

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }

    // List of unified Achievements
    val achievementsList = remember(habits, allLogs, totalPerfectDays, language) {
        val list = mutableListOf<Achievement>()
        
        // 1. Streak achievements
        val streakTargets = listOf(7, 14, 30, 60, 100, 180, 270, 365, 500, 1000)
        habits.forEach { habit ->
            val (_, longestStreak) = viewModel.calculateStreak(habit, allLogs)
            streakTargets.forEach { target ->
                if (longestStreak >= target) {
                    val label = if (language == "de") "$target Tage Meilenstein" else "$target Days Milestone"
                    list.add(
                        Achievement(
                            type = "STREAK",
                            title = label,
                            description = if (language == "de") {
                                "Gewohnheit '${habit.name}' ohne Unterbrechung für $target Tage durchgezogen!"
                            } else {
                                "Maintained a streak of $target days for habit '${habit.name}'!"
                            },
                            habitName = habit.name,
                            habitColor = HabitIconMapping.getColor(habit.color),
                            targetValue = target
                        )
                    )
                }
            }
        }
        
        // 2. Habits Finished achievements (Global total completions across all habits)
        val totalGlobalCompletions = allLogs.count { log ->
            val habit = habits.find { it.id == log.habitId }
            habit != null && (habit.type == "BINARY" || log.value >= habit.targetValue)
        }
        val completionTargets = listOf(10, 50, 100, 250, 500, 1000)
        completionTargets.forEach { target ->
            if (totalGlobalCompletions >= target) {
                list.add(
                    Achievement(
                        type = "COMPLETIONS",
                        title = if (language == "de") {
                            when (target) {
                                10 -> "Scharfschütze Bronze ($target)"
                                50 -> "Scharfschütze Silber ($target)"
                                100 -> "Scharfschütze Gold ($target)"
                                250 -> "Meisterschütze ($target)"
                                500 -> "Unfehlbarer Schütze ($target)"
                                else -> "Legendärer Schütze ($target)"
                            }
                        } else {
                            when (target) {
                                10 -> "Bronze Marksman ($target)"
                                50 -> "Silver Marksman ($target)"
                                100 -> "Gold Marksman ($target)"
                                250 -> "Master Archer ($target)"
                                500 -> "Infallible Archer ($target)"
                                else -> "Legendary Archer ($target)"
                            }
                        },
                        description = if (language == "de") {
                            "Insgesamt $target Gewohnheits-Abschlüsse erfolgreich aufgezeichnet!"
                        } else {
                            "Logged a total of $target habit completions across all habits!"
                        },
                        targetValue = target
                    )
                )
            }
        }
        
        // 3. Perfect Days achievements
        val perfectDaysTargets = listOf(3, 10, 30, 100, 365)
        perfectDaysTargets.forEach { target ->
            if (totalPerfectDays >= target) {
                list.add(
                    Achievement(
                        type = "PERFECT_DAYS",
                        title = if (language == "de") {
                            when (target) {
                                3 -> "Dreiklang ($target)"
                                10 -> "Zehner-Triumph ($target)"
                                30 -> "Monats-Krone ($target)"
                                100 -> "Jahrhundert-Klub ($target)"
                                else -> "Kosmischer Kreislauf ($target)"
                            }
                        } else {
                            when (target) {
                                3 -> "Trifecta ($target)"
                                10 -> "Decade of Perfection ($target)"
                                30 -> "Monthly Crown ($target)"
                                100 -> "Century Club ($target)"
                                else -> "Cosmic Cycle ($target)"
                            }
                        },
                        description = if (language == "de") {
                            "Insgesamt $target perfekte Tage erreicht!"
                        } else {
                            "Logged a total of $target perfect days across all habits!"
                        },
                        targetValue = target
                    )
                )
            }
        }
        
        // Sort achievements so newest/highest target value or grouped nicely
        list.sortedWith(compareByDescending<Achievement> { it.targetValue }.thenBy { it.type })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(PrimaryViolet.copy(alpha = 0.15f), CircleShape)
                            .border(3.dp, PrimaryViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (language == "de") "Mein Profil" else "My Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Frequent Habits User",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

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
                                text = achievementsList.size.toString(),
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

            // Collapsible Achievement Legend Card
            item {
                var showLegend by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showLegend = !showLegend },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Legend",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "de") "ERFOLGE-LEGENDE" else "ACHIEVEMENT LEGEND",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (showLegend) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                              )
                        }

                        if (showLegend) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Legend item 1: Streaks
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(HabitYellow.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, HabitYellow, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Trophy",
                                        tint = HabitYellow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (language == "de") "Streak-Meilensteine" else "Streak Milestones",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        InfoIconButton(
                                            title = if (language == "de") "Streak-Serien" else "Streak Milestones",
                                            explanation = if (language == "de") {
                                                "Sammle Serien-Abzeichen, indem du eine einzelne Gewohnheit an aufeinanderfolgenden Tagen abschließt.\n\nDie Stufen entsprechen den Rängen:\n• 7 Tage (Holz)\n• 14 Tage (Bronze)\n• 30 Tage (Silber)\n• 60 Tage (Gold)\n• 100 Tage (Platin)\n• 180 Tage (Diamant)\n• 270 Tage (Rubin)\n• 365 Tage (Meister)\n• 500 Tage (Legende)\n• 1000 Tage (Unreal)"
                                            } else {
                                                "Collect streak badges by completing a single habit on consecutive days.\n\nMilestones correspond to Ranks:\n• 7 Days (Wood)\n• 14 Days (Bronze)\n• 30 Days (Silver)\n• 60 Days (Gold)\n• 100 Days (Platinum)\n• 180 Days (Diamond)\n• 270 Days (Ruby)\n• 365 Days (Master)\n• 500 Days (Legend)\n• 1000 Days (Unreal)"
                                            },
                                            onClick = { t, e -> activeExplanation = t to e }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (language == "de") {
                                            "Kontinuierliche tägliche Serien bei einzelnen Gewohnheiten (7, 14, 30, 60, 100+ Tage)."
                                        } else {
                                            "Maintaining continuous daily streaks for individual habits (7, 14, 30, 60, 100+ days)."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend item 2: Habits Finished (Completions)
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TargetWithArrowsIcon(
                                        arrowCount = 3,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (language == "de") "Gewohnheiten geschafft" else "Habits Finished",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        InfoIconButton(
                                            title = if (language == "de") "Zielscheiben-Abschlüsse" else "Target Completions",
                                            explanation = if (language == "de") {
                                                "Zeigt an, wie viele Gewohnheiten du insgesamt in deinem gesamten Verlauf abgeschlossen hast. Je mehr du schaffst, desto mehr Pfeile stecken in der Zielscheibe!\n\nMeilensteine:\n🎯 10 Abschlüsse -> 1 Pfeil (Bronze)\n🎯 50 Abschlüsse -> 2 Pfeile (Silber)\n🎯 100 Abschlüsse -> 3 Pfeile (Gold)\n🎯 250 Abschlüsse -> 4 Pfeile (Meister)\n🎯 500 Abschlüsse -> 5 Pfeile (Unfehlbar)\n🎯 1000 Abschlüsse -> 6 Pfeile (Legendär)"
                                            } else {
                                                "Shows how many times you have successfully completed habits across your entire log history. The more you achieve, the more arrows stick in your target!\n\nMilestones:\n🎯 10 Logs -> 1 Arrow (Bronze)\n🎯 50 Logs -> 2 Arrows (Silver)\n🎯 100 Logs -> 3 Arrows (Gold)\n🎯 250 Logs -> 4 Arrows (Master)\n🎯 500 Logs -> 5 Arrows (Infallible)\n🎯 1000 Logs -> 6 Arrows (Legendary)"
                                            },
                                            onClick = { t, e -> activeExplanation = t to e }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (language == "de") {
                                            "Anzahl aller erfolgreich aufgezeichneten Einträge (10, 50, 100, 250, 500, 1000+)."
                                        } else {
                                            "Total count of successfully completed logs across all habits combined."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend item 3: Perfect Days
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CalendarWithRingsIcon(
                                        ringCount = 3,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (language == "de") "Perfekte Tage" else "Perfect Days",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        InfoIconButton(
                                            title = if (language == "de") "Perfekte Tage" else "Perfect Days",
                                            explanation = if (language == "de") {
                                                "Ein perfekter Tag ist ein Tag, an dem du alle deine aktiven Gewohnheiten zu 100% erledigt hast. Sammle perfekte Tage, um farbige Umlaufringe um deinen Kalender zu zeichnen!\n\nMeilensteine:\n📅 3 perfekte Tage -> 1 Ring (Grün)\n📅 10 perfekte Tage -> 2 Ringe (Gelb)\n📅 30 perfekte Tage -> 3 Ringe (Violett)\n📅 100 perfekte Tage -> 4 Ringe (Gold)\n📅 365 perfekte Tage -> 5 Ringe (Kosmisch)"
                                            } else {
                                                "A perfect day is when you complete 100% of all active habits on that day. Accumulate perfect days to draw celestial rings around your calendar!\n\nMilestones:\n📅 3 Perfect Days -> 1 Ring (Green)\n📅 10 Perfect Days -> 2 Rings (Yellow)\n📅 30 Perfect Days -> 3 Rings (Violet)\n📅 100 Perfect Days -> 4 Rings (Gold)\n📅 365 Perfect Days -> 5 Rings (Cosmic)"
                                            },
                                            onClick = { t, e -> activeExplanation = t to e }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (language == "de") {
                                            "Gleichzeitiges erfolgreiches Erledigen ALLER aktiven Gewohnheiten an einem Tag (3, 10, 30, 100, 365 Tage)."
                                        } else {
                                            "Completing ALL active habits on the same day (3, 10, 30, 100, 365 days)."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (language == "de") "MEINE ERFOLGE" else "MY ACHIEVEMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (achievementsList.isEmpty()) {
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
                                text = if (language == "de") "Noch keine Erfolge" else "No Achievements Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "de") 
                                    "Ziehe eine Gewohnheit für 7 Tage durch oder erreiche 10 Gesamt-Abschlüsse, um deinen ersten Erfolg freizuschalten!" 
                                else 
                                    "Maintain a habit streak for 7 days or reach 10 lifetime completions to unlock your first achievement!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(achievementsList) { achievement ->
                    val bgTint = when (achievement.type) {
                        "STREAK" -> HabitYellow.copy(alpha = 0.15f)
                        "COMPLETIONS" -> PrimaryViolet.copy(alpha = 0.15f)
                        else -> SuccessGreen.copy(alpha = 0.15f)
                    }
                    val borderTint = when (achievement.type) {
                        "STREAK" -> HabitYellow
                        "COMPLETIONS" -> PrimaryViolet
                        else -> SuccessGreen
                    }
                    val iconTint = when (achievement.type) {
                        "STREAK" -> HabitYellow
                        "COMPLETIONS" -> PrimaryViolet
                        else -> SuccessGreen
                    }
                    val iconImage = when (achievement.type) {
                        "STREAK" -> Icons.Default.EmojiEvents
                        "COMPLETIONS" -> Icons.Default.Adjust
                        else -> Icons.Default.DateRange
                    }

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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (achievement.type == "COMPLETIONS") {
                                val arrows = when (achievement.targetValue) {
                                    10 -> 1
                                    50 -> 2
                                    100 -> 3
                                    250 -> 4
                                    500 -> 5
                                    else -> 6
                                }
                                TargetWithArrowsIcon(
                                    arrowCount = arrows,
                                    modifier = Modifier.size(50.dp)
                                )
                            } else if (achievement.type == "PERFECT_DAYS") {
                                val rings = when (achievement.targetValue) {
                                    3 -> 1
                                    10 -> 2
                                    30 -> 3
                                    100 -> 4
                                    else -> 5
                                }
                                CalendarWithRingsIcon(
                                    ringCount = rings,
                                    modifier = Modifier.size(50.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(bgTint, CircleShape)
                                        .border(1.5.dp, borderTint, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconImage,
                                        contentDescription = "Achievement",
                                        tint = iconTint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = achievement.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = achievement.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                if (achievement.habitColor != null && achievement.habitName != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(achievement.habitColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = achievement.habitName,
                                            style = MaterialTheme.typography.labelSmall,
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

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .testTag("profile_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
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
    val totalValidDays = totalDays.size.coerceAtLeast(1)

    val successRate = ((successCount.toFloat() / totalValidDays.toFloat()) * 100).toInt()

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
                            cal.add(Calendar.MONTH, verlaufOffset)
                            val daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            for (i in 1..daysCount) {
                                cal.set(Calendar.DAY_OF_MONTH, i)
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

                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp)
                        .then(
                            if (activeFilter == "MONTH") {
                                Modifier.horizontalScroll(scrollState)
                            } else Modifier
                        ),
                    horizontalArrangement = if (activeFilter == "MONTH") Arrangement.spacedBy(3.dp) else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    barData.forEach { (label, ratio) ->
                        val barWidth = when (activeFilter) {
                            "WEEK" -> 16.dp
                            "MONTH" -> 6.dp
                            else -> 16.dp
                        }
                        Column(
                            modifier = if (activeFilter == "MONTH") Modifier.width(9.dp) else Modifier.weight(1f),
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