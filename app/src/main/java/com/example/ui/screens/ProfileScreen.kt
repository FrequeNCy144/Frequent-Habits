package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.*
import com.example.ui.components.AppSegmentedButton
import com.example.ui.screens.profile.*
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun ProfileScreen(
    viewModel: HabitsViewModel,
    language: String,
    onSettingsClick: (String?) -> Unit,
    onOpenHabitDetail: ((Habit) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val masteredGoals by viewModel.masteredGoals.collectAsStateWithLifecycle()
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle()
    val perfectDaysState by viewModel.perfectDaysStats.collectAsStateWithLifecycle()
    val perfectDaysStreak = perfectDaysState.perfectDaysStreak
    val currentPerfectStreak = perfectDaysState.currentStreak

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.profileImageUri.collectAsStateWithLifecycle()
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showProfileShareDialog by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    val context = LocalContext.current
    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val file = java.io.File(context.filesDir, "profile_avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateProfileImageUri(android.net.Uri.fromFile(file).toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var activeExplanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Erfolge, 1 = Gemeisterte Ziele

    val totalGlobalCompletions = profileStats.totalGlobalCompletions
    val totalUnlockedCount = profileStats.totalUnlockedCount
    val totalPossibleCount = profileStats.totalPossibleCount

    val habitStreaksList = remember(profileStats.habitStreaks) {
        profileStats.habitStreaks.filter { !it.habit.isFinishable }.map { streakInfo ->
            val habit = streakInfo.habit
            val longestStreak = streakInfo.longestStreak
            val hColor = HabitIconMapping.getColor(habit.color)
            val hIcon = habit.icon
            Triple(habit, longestStreak, Pair(hColor, hIcon))
        }
    }

    val globalCompletionsAchievements = remember(totalGlobalCompletions, language) {
        AchievementExplanations.getCompletionsAchievements(totalGlobalCompletions, language)
    }

    val globalPerfectDaysAchievements = remember(perfectDaysStreak, currentPerfectStreak, language) {
        AchievementExplanations.getPerfectDaysAchievements(perfectDaysStreak, currentPerfectStreak, language)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ProfileTopBar(
            language = language,
            accentColor = accentColor,
            onShareClick = { showProfileShareDialog = true },
            onSettingsClick = { onSettingsClick(null) }
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileAvatarAndProgressCard(
                        userName = userName,
                        profileImageUri = profileImageUri,
                        accentColor = accentColor,
                        totalUnlockedCount = totalUnlockedCount,
                        totalPossibleCount = totalPossibleCount,
                        language = language,
                        onEditNameClick = { showEditNameDialog = true }
                    )
                }

                // Tab Selector
                item {
                    val tabs = when (language) {
                        "de" -> listOf("Erfolge", if (masteredGoals.isNotEmpty()) "Gemeisterte Ziele (${masteredGoals.size})" else "Gemeisterte Ziele")
                        "ka" -> listOf("მიღწევები", if (masteredGoals.isNotEmpty()) "მიღწეული მიზნები (${masteredGoals.size})" else "მიღწეული მიზნები")
                        "zh" -> listOf("成就殿堂", if (masteredGoals.isNotEmpty()) "达成目标 (${masteredGoals.size})" else "达成目标")
                        else -> listOf("Achievements", if (masteredGoals.isNotEmpty()) "Mastered Goals (${masteredGoals.size})" else "Mastered Goals")
                    }
                    AppSegmentedButton(
                        options = tabs,
                        selectedIndex = selectedTab,
                        onOptionSelected = { selectedTab = it },
                        modifier = Modifier.padding(vertical = 4.dp),
                        testTagPrefix = "achievements_filter"
                    )
                }

                if (selectedTab == 1) {
                    if (masteredGoals.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AppCard),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_trophy),
                                        contentDescription = null,
                                        tint = HabitYellow.copy(alpha = 0.35f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = tr(language, "Noch keine Ziele gemeistert", "მიღწეული მიზნები ჯერ არ არის", "尚未达成目标", "No Mastered Goals Yet"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tr(
                                            language,
                                            "Setze dir beim Erstellen einer Gewohnheit ein Gesamtziel (z.B. ein Buch mit 300 Seiten oder 30 Tage Meditation). Sobald du das Ziel erreichst, wird es hier feierlich verewigt!",
                                            "დაისახეთ საბოლოო მიზანი ჩვევის შექმნისას. მიღწევის შემდეგ, ის აქ დარჩება!",
                                            "创建习惯时设定一个总目标（如阅读 300 页或冥想 30 天）。达成目标后，将在此作为已掌握的技能永久珍藏！",
                                            "Set an overall target when creating a habit (e.g., read a 300-page book or meditate for 30 days). Once achieved, it will be honored here forever!"
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(masteredGoals, key = { it.id }) { habit ->
                            MasteredGoalCard(
                                habit = habit,
                                language = language,
                                onViewStats = { onOpenHabitDetail?.invoke(habit) },
                                onDelete = { habitToDelete = habit }
                            )
                        }
                    }
                } else {
                    achievementsTabSection(
                        displayedHabitStreaks = habitStreaksList,
                        displayedCompletions = globalCompletionsAchievements,
                        displayedPerfectDays = globalPerfectDaysAchievements,
                        language = language,
                        onShowExplanation = { t, e -> activeExplanation = t to e }
                    )
                }
            }
        }

        ProfileDialogsOverlay(
            showEditNameDialog = showEditNameDialog,
            userName = userName,
            profileImageUri = profileImageUri,
            language = language,
            onSaveName = { viewModel.updateUserName(it) },
            onChangePhoto = { profileImagePickerLauncher.launch("image/*") },
            onDismissEditNameDialog = { showEditNameDialog = false },
            activeExplanation = activeExplanation,
            onDismissExplanation = { activeExplanation = null },
            showProfileShareDialog = showProfileShareDialog,
            onDismissProfileShareDialog = { showProfileShareDialog = false },
            habits = habits,
            allLogs = allLogs,
            habitToDelete = habitToDelete,
            onConfirmDeleteHabit = {
                viewModel.deleteHabit(it)
                habitToDelete = null
            },
            onDismissDeleteHabit = { habitToDelete = null }
        )
    }
}
