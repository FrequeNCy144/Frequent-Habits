package com.example.ui.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.tr
import com.example.ui.components.Achievement
import com.example.ui.theme.*
import com.frequent.habits.R

fun LazyListScope.achievementsTabSection(
    displayedHabitStreaks: List<Triple<Habit, Int, Pair<Color, String>>>,
    displayedCompletions: List<Achievement>,
    displayedPerfectDays: List<Achievement>,
    language: String,
    onShowExplanation: (String, String) -> Unit
) {
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
                        painter = painterResource(id = R.drawable.ic_trophy),
                        contentDescription = null,
                        tint = HabitYellow.copy(alpha = 0.35f),
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
                    icon = painterResource(id = R.drawable.ic_fire),
                    color = HabitStreakFlame,
                    explanationTitle = explanationTitle,
                    explanationText = explanationText,
                    onInfoClick = { t, e -> onShowExplanation(t, e) }
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
                    icon = painterResource(id = R.drawable.ic_trophy),
                    color = HabitYellow,
                    explanationTitle = explanationTitle,
                    explanationText = explanationText,
                    onInfoClick = { t, e -> onShowExplanation(t, e) }
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
                    icon = painterResource(id = R.drawable.ic_fire),
                    color = PerfectStreakFlame,
                    explanationTitle = explanationTitle,
                    explanationText = explanationText,
                    onInfoClick = { t, e -> onShowExplanation(t, e) }
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
