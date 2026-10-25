package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.StatsScreen
import com.example.data.Habit
import com.example.tr
import com.example.ui.*
import com.example.ui.addHabit
import com.example.ui.updateHabit
import com.example.ui.screens.*
import com.example.ui.theme.PrimaryViolet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainNavHost(
    navController: NavHostController,
    viewModel: HabitsViewModel,
    language: String,
    profileListState: LazyListState,
    todayListState: LazyListState,
    statsListState: LazyListState,
    editingHabit: Habit?,
    onSetEditingHabit: (Habit?) -> Unit,
    showCreateFirstHabitHint: Boolean,
    onSetShowCreateFirstHabitHint: (Boolean) -> Unit,
    handleAddHabit: () -> Unit,
    allHabitsForPopup: List<Habit>,
    innerPadding: PaddingValues,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    val mainTabRoutes = remember { listOf("TODAY", "STATS", "PROFILE") }
    fun isMainTabRoute(route: String?): Boolean {
        if (route.isNullOrEmpty()) return true
        return mainTabRoutes.contains(route)
    }

    NavHost(
        navController = navController,
        startDestination = "TODAY",
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            val initialRoute = initialState.destination.route
            val targetRoute = targetState.destination.route
            if (isMainTabRoute(initialRoute) && isMainTabRoute(targetRoute)) {
                EnterTransition.None
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        exitTransition = {
            val initialRoute = initialState.destination.route
            val targetRoute = targetState.destination.route
            if (isMainTabRoute(initialRoute) && isMainTabRoute(targetRoute)) {
                ExitTransition.None
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            }
        },
        popEnterTransition = {
            val initialRoute = initialState.destination.route
            val targetRoute = targetState.destination.route
            if (isMainTabRoute(initialRoute) && isMainTabRoute(targetRoute)) {
                EnterTransition.None
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        popExitTransition = {
            val initialRoute = initialState.destination.route
            val targetRoute = targetState.destination.route
            if (isMainTabRoute(initialRoute) && isMainTabRoute(targetRoute)) {
                ExitTransition.None
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            }
        }
    ) {
        composable("PROFILE") {
            ProfileScreen(
                viewModel = viewModel,
                language = language,
                onSettingsClick = { subpage ->
                    val dest = if (subpage != null) "MORE?subpage=$subpage" else "MORE"
                    navController.navigate(dest) {
                        launchSingleTop = true
                    }
                },
                onOpenHabitDetail = { habit ->
                    val currentDest = navController.currentDestination
                    val currentRoute = currentDest?.route
                    if (currentRoute?.startsWith("DETAIL") != true) {
                        navController.navigate("DETAIL/${habit.id}") {
                            launchSingleTop = true
                        }
                    }
                },
                listState = profileListState
            )
        }
        composable("TODAY") {
            TodayScreen(
                viewModel = viewModel,
                language = language,
                onAddClick = handleAddHabit,
                onEditHabit = { habit ->
                    onSetEditingHabit(habit)
                    navController.navigate("CREATE") {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = { subpage ->
                    val dest = if (subpage != null) "MORE?subpage=$subpage" else "MORE"
                    navController.navigate(dest) {
                        launchSingleTop = true
                    }
                },
                showCreateFirstHabitHint = showCreateFirstHabitHint,
                onDismissFirstHabitHint = { onSetShowCreateFirstHabitHint(false) },
                onNavigateToReorder = {
                    navController.navigate("REORDER") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("CREATE") {
            val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
            val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
            val allMilestoneRewards by viewModel.allMilestoneRewards.collectAsStateWithLifecycle(initialValue = emptyList())
            val habitMilestones = remember(editingHabit?.id, allMilestoneRewards) {
                if (editingHabit != null) {
                    allMilestoneRewards.filter { it.habitId == editingHabit.id }
                } else {
                    emptyList()
                }
            }
            CreateHabitScreen(
                language = language,
                accentColor = accentColor,
                editingHabit = editingHabit,
                initialMilestoneRewards = habitMilestones,
                activeHabits = allHabitsForPopup.filter { !it.isArchived },
                allFolders = allFolders,
                onCreateFolder = { name, icon, color -> viewModel.addFolder(name, icon, color) },
                onDismiss = {
                    onSetEditingHabit(null)
                    navController.popBackStack()
                },
                onSave = { name, isNeg, cat, icon, color, type, unit, target, clickIncrement, freq, start, specDays, remEnabled, remHour, remMin, customReminders, description, milestoneRewards, minViableValue, minViableText, why, stackedOnHabitId, isFinishable, totalTargetValue, difficulty, folderId, challengeMode, challengeSuccessRate ->
                    if (editingHabit != null) {
                        val updated = editingHabit.copy(
                            name = name,
                            isNegative = isNeg,
                            category = cat,
                            icon = icon,
                            color = color,
                            type = type,
                            unit = unit,
                            targetValue = target,
                            clickIncrement = clickIncrement,
                            frequency = freq,
                            startDate = start,
                            specificDays = specDays,
                            reminderEnabled = remEnabled,
                            reminderHour = remHour,
                            reminderMinute = remMin,
                            customReminders = customReminders,
                            description = description,
                            minimalViableValue = minViableValue,
                            minimalViableText = minViableText,
                            why = why,
                            stackedOnHabitId = stackedOnHabitId,
                            isFinishable = isFinishable,
                            totalTargetValue = totalTargetValue,
                            difficulty = difficulty,
                            folderId = folderId,
                            challengeMode = challengeMode,
                            challengeSuccessRate = challengeSuccessRate
                        )
                        viewModel.updateHabit(updated, milestoneRewards)
                        onSetEditingHabit(null)
                        Toast.makeText(
                            context,
                            tr(language, "Gewohnheit aktualisiert!", "ჩვევა განახლებულია!", "习惯已更新！", "Habit updated!"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.addHabit(name, cat, icon, color, isNeg, type, unit, target, freq, start, specDays, remEnabled, remHour, remMin, customReminders, description, clickIncrement, milestoneRewards, minViableValue, minViableText, why, stackedOnHabitId, isFinishable, totalTargetValue, difficulty, folderId, challengeMode, challengeSuccessRate)
                        Toast.makeText(
                            context,
                            tr(language, "Gewohnheit hinzugefügt!", "ჩვევა დაემატა!", "习惯已添加！", "Habit added!"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    navController.popBackStack()
                }
            )
        }
        composable("STATS") {
            val onAddClick = remember {
                {
                    onSetEditingHabit(null)
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
                    val currentDest = navController.currentDestination
                    val currentRoute = currentDest?.route
                    if (currentRoute?.startsWith("DETAIL") != true) {
                        navController.navigate("DETAIL/$habitId") {
                            launchSingleTop = true
                        }
                    }
                },
                onOverallClick = {
                    val currentDest = navController.currentDestination
                    val currentRoute = currentDest?.route
                    if (currentRoute != "OVERALL_STATS") {
                        navController.navigate("OVERALL_STATS") {
                            launchSingleTop = true
                        }
                    }
                },
                onEditHabit = { habit ->
                    onSetEditingHabit(habit)
                    navController.navigate("CREATE") {
                        launchSingleTop = true
                    }
                },
                onReorderClick = {
                    navController.navigate("REORDER") {
                        launchSingleTop = true
                    }
                },
                listState = statsListState
            )
        }
        composable("REORDER") {
            ReorderHabitsScreen(
                viewModel = viewModel,
                language = language,
                onBack = {
                    navController.popBackStack()
                },
                onEditHabit = { habit ->
                    onSetEditingHabit(habit)
                    navController.navigate("CREATE") {
                        launchSingleTop = true
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
                },
                onNavigateToToday = {
                    navController.navigate("TODAY") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    coroutineScope.launch {
                        todayListState.scrollToItem(0, 0)
                    }
                }
            )
        }
        composable(
            route = "MORE?subpage={subpage}",
            arguments = listOf(
                navArgument("subpage") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val subpage = backStackEntry.arguments?.getString("subpage")
            SettingsScreen(
                viewModel = viewModel,
                language = language,
                initialSubpage = subpage,
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
}
