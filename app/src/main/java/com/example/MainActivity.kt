package com.example
import com.example.ui.components.ClearFocusOnKeyboardDismiss
import com.example.ui.screens.stats.*
import com.example.ui.screens.profile.*
import com.example.ui.share.*

import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.tr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.frequent.habits.R
import com.example.data.*
import com.example.ui.*
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.components.AppSegmentedButton
import com.example.ui.components.AppSegmentedButtonWithIcons
import com.example.ui.components.AppTextField
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalInfoCardsEnabled = androidx.compose.runtime.compositionLocalOf { true }
val LocalDarkMode = androidx.compose.runtime.compositionLocalOf { false }
val LocalHapticsEnabled = androidx.compose.runtime.compositionLocalOf { true }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[HabitsViewModel::class.java]
        handleWidgetIntent(intent, viewModel)
        handleReviewIntent(intent, viewModel)
        enableEdgeToEdge()
        setContent {
            val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = darkModeEnabled) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HabitWidgetProvider.triggerUpdate(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[HabitsViewModel::class.java]
        handleWidgetIntent(intent, viewModel)
        handleReviewIntent(intent, viewModel)
    }

    private fun handleWidgetIntent(intent: Intent?, viewModel: HabitsViewModel) {
        if (intent?.action == "com.example.widget.ACTION_WIDGET_ADD_VALUE") {
            val habitId = intent.getIntExtra("com.example.widget.EXTRA_HABIT_ID", -1)
            if (habitId != -1) {
                viewModel.setPendingWidgetHabitId(habitId)
            }
        } else if (intent != null && intent.hasExtra(FocusTimerService.EXTRA_OPEN_HABIT_ID)) {
            val habitId = intent.getLongExtra(FocusTimerService.EXTRA_OPEN_HABIT_ID, -1L)
            if (habitId != -1L) {
                viewModel.setPendingWidgetHabitId(habitId.toInt())
            }
            intent.removeExtra(FocusTimerService.EXTRA_OPEN_HABIT_ID)
        }
    }

    private fun handleReviewIntent(intent: Intent?, viewModel: HabitsViewModel) {
        if (intent != null && intent.hasExtra("OPEN_REVIEW_TYPE")) {
            val reviewType = intent.getStringExtra("OPEN_REVIEW_TYPE")
            val year = intent.getIntExtra("REVIEW_YEAR", -1)
            val month = intent.getIntExtra("REVIEW_MONTH", -1)
            if (reviewType == "MONTHLY" && year != -1 && month != -1) {
                viewModel.setDeepLinkReviewMonth(year to month)
            } else if (reviewType == "YEARLY" && year != -1) {
                viewModel.setDeepLinkReviewYear(year)
            }
            intent.removeExtra("OPEN_REVIEW_TYPE")
        }
    }
}

@Composable
fun rememberResumeAnimationTrigger(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var trigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                trigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return trigger
}

@Composable
fun MainAppScreen(viewModel: HabitsViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    ClearFocusOnKeyboardDismiss()

    var selectedTab by remember { mutableStateOf("TODAY") }
    val navController = rememberNavController()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val infoCardsEnabled by viewModel.infoCardsEnabled.collectAsStateWithLifecycle()

    val todayListState = rememberLazyListState()
    val statsListState = rememberLazyListState()
    val profileListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val mainTabRoutes = remember { setOf("TODAY", "STATS", "PROFILE") }
    var previousRoute by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalInfoCardsEnabled provides infoCardsEnabled) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pendingWidgetHabitId by viewModel.pendingWidgetHabitId.collectAsStateWithLifecycle()
    val newlyUnlockedAchievement by viewModel.newlyUnlockedAchievement.collectAsStateWithLifecycle()
    val hasOnboarded by viewModel.hasOnboarded.collectAsStateWithLifecycle()
    var showCreateFirstHabitHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FeedbackHelper.init(context.applicationContext)
    }

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

    val allHabitsForPopup by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogsForPopup by viewModel.allLogs.collectAsStateWithLifecycle()

    val nyPrefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
    val isNYPopupWindow = remember { isNewYearPopupPeriod() }
    val currentYearVal = remember { java.time.LocalDate.now().year }
    val reviewYearVal = remember { if (java.time.LocalDate.now().monthValue == 1) currentYearVal - 1 else currentYearVal }
    val nyPopupKey = "has_seen_ny_popup_$reviewYearVal"

    var showNewYearPopup by remember {
        mutableStateOf(isNYPopupWindow && !nyPrefs.getBoolean(nyPopupKey, false))
    }
    var popupReviewYearState by remember { mutableStateOf<Int?>(null) }

    val deepLinkReviewMonth by viewModel.deepLinkReviewMonth.collectAsStateWithLifecycle()
    val deepLinkReviewYear by viewModel.deepLinkReviewYear.collectAsStateWithLifecycle()

    var activeMonthlyReviewState by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(deepLinkReviewMonth) {
        deepLinkReviewMonth?.let {
            activeMonthlyReviewState = it
            viewModel.setDeepLinkReviewMonth(null)
        }
    }

    LaunchedEffect(deepLinkReviewYear) {
        deepLinkReviewYear?.let {
            popupReviewYearState = it
            viewModel.setDeepLinkReviewYear(null)
        }
    }

    if (showNewYearPopup) {
        NewYearReviewPopupDialog(
            reviewYear = reviewYearVal,
            language = language,
            onStartReview = {
                showNewYearPopup = false
                nyPrefs.edit().putBoolean(nyPopupKey, true).apply()
                popupReviewYearState = reviewYearVal
            },
            onDismiss = {
                showNewYearPopup = false
                nyPrefs.edit().putBoolean(nyPopupKey, true).apply()
            }
        )
    }

    if (activeMonthlyReviewState != null) {
        val (year, month) = activeMonthlyReviewState!!
        MonthlyReviewDialog(
            year = year,
            month = month,
            allHabits = allHabitsForPopup,
            allLogs = allLogsForPopup,
            language = language,
            viewModel = viewModel,
            onDismiss = {
                viewModel.dismissReview("monthly_${year}_${month}")
                activeMonthlyReviewState = null
            }
        )
    }

    if (popupReviewYearState != null) {
        val year = popupReviewYearState!!
        YearlyReviewDialog(
            year = year,
            allHabits = allHabitsForPopup,
            allLogs = allLogsForPopup,
            language = language,
            viewModel = viewModel,
            onDismiss = {
                viewModel.dismissReview("yearly_${year}")
                popupReviewYearState = null
            }
        )
    }

    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }

    // Keep tab selected state in synchronization if user navigates & scroll to top when switching main tabs
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            if (mainTabRoutes.contains(currentRoute)) {
                if (selectedTab != currentRoute) {
                    selectedTab = currentRoute
                }
                if (previousRoute != null && mainTabRoutes.contains(previousRoute) && previousRoute != currentRoute) {
                    when (currentRoute) {
                        "TODAY" -> todayListState.scrollToItem(0, 0)
                        "STATS" -> statsListState.scrollToItem(0, 0)
                        "PROFILE" -> profileListState.scrollToItem(0, 0)
                    }
                }
            }
            previousRoute = currentRoute
        }
        if (currentRoute != "CREATE") {
            editingHabit = null
        }
    }



    val gameMode by viewModel.gameMode.collectAsStateWithLifecycle()
    val unlockedStorySlots by viewModel.unlockedStorySlots.collectAsStateWithLifecycle()
    var showStorySlotLockedDialog by remember { mutableStateOf(false) }

    val handleAddHabit: () -> Unit = {
        val activeCount = allHabitsForPopup.filter { !it.isArchived }.size
        if (gameMode == "STORY" && activeCount >= unlockedStorySlots) {
            showStorySlotLockedDialog = true
        } else {
            editingHabit = null
            navController.navigate("CREATE") {
                launchSingleTop = true
            }
        }
    }

    if (showStorySlotLockedDialog) {
        AlertDialog(
            onDismissRequest = { showStorySlotLockedDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = tr(language, "🔒 Slot gesperrt (Story-Modus)", "🔒 Slot Locked (Story Mode)"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                val activeCount = allHabitsForPopup.filter { !it.isArchived }.size
                Text(
                    text = tr(
                        language,
                        "Im Story-Modus kannst du erst eine weitere Gewohnheit erstellen, wenn du deinen nächsten Slot freigeschaltet hast:\n\n• Absolviere 30 Tage mit ≥ 85% Erfolgsquote\n• Aktuell: $activeCount / $unlockedStorySlots Slot belegt\n\nDu kannst in den Einstellungen auch jederzeit in das 'Freie Spiel' wechseln.",
                        "In Story Mode, you can only create another habit once you unlock your next slot:\n\n• Complete 30 days with ≥ 85% success rate\n• Currently: $activeCount / $unlockedStorySlots slot filled\n\nYou can also switch to 'Free Play' in settings at any time."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStorySlotLockedDialog = false
                        navController.navigate("MORE?subpage=appearance") {
                            launchSingleTop = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(text = tr(language, "Zu den Einstellungen", "Go to Settings"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStorySlotLockedDialog = false }
                ) {
                    Text(text = tr(language, "Verstanden", "Got it"), color = TextSecondary)
                }
            },
            containerColor = AppCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != null && !currentRoute.startsWith("DETAIL") && currentRoute != "CREATE" && currentRoute != "OVERALL_STATS" && !currentRoute.startsWith("MORE") && editingHabit == null) {
                HabitBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (tab == "STATS" && allHabitsForPopup.none { !it.isArchived }) {
                            Toast.makeText(
                                context,
                                tr(language, "Erstelle zuerst eine Gewohnheit, um Statistiken zu sehen.", "შექმენით ჩვევა ჯერ სტატისტიკის სანახავად.", "请先创建一个习惯以查看统计数据。", "Create a habit first to view statistics."),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val targetState = when (tab) {
                                "TODAY" -> todayListState
                                "STATS" -> statsListState
                                "PROFILE" -> profileListState
                                else -> null
                            }
                            selectedTab = tab
                            navController.navigate(tab) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            coroutineScope.launch {
                                targetState?.scrollToItem(0, 0)
                            }
                        }
                    },
                    onAddClick = handleAddHabit,
                    language = language
                )
            }
        },
        containerColor = AppBg,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            com.example.ui.navigation.MainNavHost(
                navController = navController,
                viewModel = viewModel,
                language = language,
                profileListState = profileListState,
                todayListState = todayListState,
                statsListState = statsListState,
                editingHabit = editingHabit,
                onSetEditingHabit = { editingHabit = it },
                showCreateFirstHabitHint = showCreateFirstHabitHint,
                onSetShowCreateFirstHabitHint = { showCreateFirstHabitHint = it },
                handleAddHabit = handleAddHabit,
                allHabitsForPopup = allHabitsForPopup,
                innerPadding = innerPadding,
                coroutineScope = coroutineScope
            )

            // Fading overlay at the bottom so content beautifully fades out behind the floating nav bar
            if (currentRoute != null && !currentRoute.startsWith("DETAIL") && currentRoute != "CREATE" && currentRoute != "OVERALL_STATS" && !currentRoute.startsWith("MORE") && editingHabit == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AppBg.copy(alpha = 0.8f),
                                    AppBg
                                )
                            )
                        )
                )
            }
        }
    }

    MainRootOverlays(
        pendingWidgetHabitId = pendingWidgetHabitId,
        newlyUnlockedAchievement = newlyUnlockedAchievement,
        hasOnboarded = hasOnboarded,
        viewModel = viewModel,
        language = language,
        onSetShowCreateFirstHabitHint = { showCreateFirstHabitHint = it }
    )
    }
}

// AUDIO SOUNDSCAPE MANAGER & PERSISTENCE
