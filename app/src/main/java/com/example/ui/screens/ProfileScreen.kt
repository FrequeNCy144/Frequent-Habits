package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.LocalInfoCardsEnabled
import com.example.data.*
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.deleteHabit
import com.example.ui.components.*
import com.example.ui.components.Achievement
import com.example.ui.dialogs.*
import com.example.ui.screens.profile.*
import com.example.ui.share.ProfileShareDialog
import com.example.ui.theme.*

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
    var tempUserName by remember(userName) { mutableStateOf(userName) }
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Freigeschaltet, 1 = Alle Erfolge

    val totalGlobalCompletions = profileStats.totalGlobalCompletions
    val unlockedCompletions = profileStats.unlockedCompletions
    val unlockedPerfectDays = profileStats.unlockedPerfectDays

    val habitStreaksList = remember(profileStats.habitStreaks) {
        profileStats.habitStreaks.filter { !it.habit.isFinishable }.map { streakInfo ->
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
        AchievementExplanations.getCompletionsAchievements(totalGlobalCompletions, language)
    }

    val globalPerfectDaysAchievements = remember(perfectDaysStreak, currentPerfectStreak, language) {
        AchievementExplanations.getPerfectDaysAchievements(perfectDaysStreak, currentPerfectStreak, language)
    }

    // Visual filter tabs: 2 intuitive tabs: 0 = "Erfolge" (with Unlocked first, then remaining), 1 = "Gemeisterte Ziele"
    val displayedHabitStreaks = habitStreaksList
    val displayedCompletions = globalCompletionsAchievements
    val displayedPerfectDays = globalPerfectDaysAchievements

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed Top Header Row with Share on Top Left and Settings on Top Right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp)
        ) {
            // Top Left: Share Profile Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { showProfileShareDialog = true }
                    .align(Alignment.CenterStart)
                    .testTag("profile_share_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Profile",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center Title
            Text(
                text = tr(language, "Profil", "პროფილი", "我的", "Profile"),
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            // Top Right: Settings Gear Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick(null) }
                    .align(Alignment.CenterEnd)
                    .testTag("profile_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

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
                    // Profile Avatar & Username Header (Matching Screenshot 1)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clickable { onSettingsClick("profile") },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(104.dp),
                                shape = CircleShape,
                                color = AppCard,
                                border = BorderStroke(2.dp, PrimaryViolet)
                            ) {
                                if (profileImageUri.isNotEmpty()) {
                                    AsyncImage(
                                        model = profileImageUri,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PrimaryViolet.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Avatar Placeholder",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSettingsClick("profile") }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userName.isBlank()) {
                                Text(
                                    text = "______",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

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
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(language, "Erfolge freigeschaltet", "მიღწევები განბლოკილია", "已解锁成就", "Achievements Unlocked"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$totalUnlockedCount / $totalPossibleCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HabitYellow
                                )
                            }
                            val progressFraction = if (totalPossibleCount > 0) totalUnlockedCount.toFloat() / totalPossibleCount.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progressFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = HabitYellow,
                                trackColor = ProgressTrack
                            )
                        }
                    }
                }

            // Visual filter tabs to toggle: 0 = "Erfolge", 1 = "Gemeisterte Ziele"
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
                // TAB 1: DEDICATED MASTERED GOALS / RUHMESHALLE
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
                                    imageVector = Icons.Default.EmojiEvents,
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
                    item {
                        CategoryHeader(
                            title = tr(language, "Gemeisterte Fähigkeiten & Ziele", "მიღწეული უნარები და მიზნები", "已掌握技能与达成目标", "Mastered Skills & Goals"),
                            icon = Icons.Default.EmojiEvents,
                            color = HabitYellow,
                            explanationTitle = tr(language, "Gemeisterte Fähigkeiten", "მიღწევების დარბაზი", "技能殿堂", "Mastered Skills"),
                            explanationText = tr(language, "Hier findest du all deine erfolgreich gemeisterten Gewohnheiten und Fähigkeiten für die Ewigkeit festgehalten.", "Here you find all your successfully mastered habits and skills honored forever."),
                            onInfoClick = { t, e -> activeExplanation = t to e }
                        )
                    }
                    items(masteredGoals, key = { it.id }) { masteredHabit ->
                        MasteredGoalCard(
                            habit = masteredHabit,
                            language = language,
                            onDelete = { habit ->
                                habitToDelete = habit
                            },
                            onViewStats = { habit ->
                                viewModel.selectHabitForDetail(habit.id)
                                onOpenHabitDetail?.invoke(habit)
                            }
                        )
                    }
                }
            } else {
                // TAB 0: ALL ACHIEVEMENTS (Streaks, Completions, Perfect Days)
                if (displayedHabitStreaks.isEmpty() && displayedCompletions.isEmpty() && displayedPerfectDays.isEmpty()) {
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
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = tr(language, "Noch keine Erfolge", "მიღწევები ჯერ არ არის", "暂无成就", "No Achievements Yet"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == "de") "Arbeite an deinen Gewohnheiten, um deinen ersten Meilenstein freizuschalten!" else if (language == "ka") "იმუშავეთ თქვენს ჩვევებზე, რათა გახსნათ თქვენი პირველი ეტაპი!" else "Work on your habits to unlock your first milestone!",
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
                            val explanationTitle = tr(language, "Gewohnheiten-Serien", "ჩვევების ზოლები", "习惯连续记录", "Habit Streaks")
                            val explanationText = AchievementExplanations.habitStreaksText(language)
                            CategoryHeader(
                                title = tr(language, "Gewohnheiten-Serien", "ჩვევების ზოლები", "习惯连续记录", "Habit Streaks"),
                                icon = Icons.Default.LocalFireDepartment,
                                color = HabitStreakFlame,
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

                    // CATEGORY 3: COMPLETIONS
                    if (displayedCompletions.isNotEmpty()) {
                        item {
                            if (displayedHabitStreaks.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = AppBorder,
                                    thickness = 1.dp
                                )
                            }
                            val explanationTitle = tr(language, "Gesamt-Abschlüsse", "სულ დასრულებები", "累计完成次数", "Total Completions")
                            val explanationText = AchievementExplanations.completionsText(language)
                            CategoryHeader(
                                title = tr(language, "Gesamt-Abschlüsse", "სულ დასრულებები", "累计完成次数", "Total Completions"),
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

                    // CATEGORY 4: PERFECT DAYS
                    if (displayedPerfectDays.isNotEmpty()) {
                        item {
                            if (displayedHabitStreaks.isNotEmpty() || displayedCompletions.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = AppBorder,
                                    thickness = 1.dp
                                )
                            }
                            val explanationTitle = tr(language, "Perfekte Tage", "იდეალური დღეები", "完美日", "Perfect Days")
                            val explanationText = AchievementExplanations.perfectDaysText(language)
                            CategoryHeader(
                                title = tr(language, "Perfekte Tage", "იდეალური დღეები", "完美日", "Perfect Days"),
                                icon = Icons.Default.LocalFireDepartment,
                                color = PerfectStreakFlame,
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
        }

        if (showEditNameDialog) {
            EditProfileDialog(
                userName = userName,
                profileImageUri = profileImageUri,
                language = language,
                onSaveName = { newName -> viewModel.updateUserName(newName) },
                onChangePhoto = { profileImagePickerLauncher.launch("image/*") },
                onDismiss = { showEditNameDialog = false }
            )
        }

        if (activeExplanation != null) {
            ExplanationDialog(
                title = activeExplanation!!.first,
                explanation = activeExplanation!!.second,
                onDismiss = { activeExplanation = null }
            )
        }

        if (showProfileShareDialog) {
            ProfileShareDialog(
                userName = userName,
                profileImageUri = profileImageUri,
                habits = habits,
                logs = allLogs,
                language = language,
                onDismiss = { showProfileShareDialog = false }
            )
        }

        habitToDelete?.let { habit ->
            AlertDialog(
                onDismissRequest = { habitToDelete = null },
                title = {
                    Text(
                        text = tr(language, "Ziel löschen?", "მიზნის წაშლა?", "删除目标？", "Delete Goal?"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = tr(
                            language,
                            "Möchtest du „${habit.name}“ wirklich unwiderruflich aus deinen gemeisterten Zielen löschen?",
                            "ნამდვილად გსურთ წაშალოთ „${habit.name}“ მიღწეული მიზნებიდან?",
                            "确定要从已掌握的目标中永久删除“${habit.name}”吗？",
                            "Do you really want to permanently delete \"${habit.name}\" from your mastered goals?"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit(habit)
                            habitToDelete = null
                        }
                    ) {
                        Text(
                            text = tr(language, "Löschen", "წაშლა", "删除", "Delete"),
                            color = FailedRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { habitToDelete = null }) {
                        Text(
                            text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                            color = TextSecondary
                        )
                    }
                }
            )
        }
    }
}
}


