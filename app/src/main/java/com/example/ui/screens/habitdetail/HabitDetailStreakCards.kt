package com.example.ui.screens.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*

@Composable
fun HabitDetailStreakCards(
    habit: Habit,
    currentStreak: Int,
    longestStreak: Int,
    language: String,
    onInfoClick: (String, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val isWeeklyHabit = habit.frequency == "TIMES_WEEKLY"
        val currentStreakText = if (language == "de") "$currentStreak ${if (currentStreak == 1) "Tag" else "Tage"}"
            else if (language == "ka") "$currentStreak დღე"
            else "$currentStreak ${if (currentStreak == 1) "Day" else "Days"}"
        val longestStreakText = if (language == "de") "$longestStreak ${if (longestStreak == 1) "Tag" else "Tage"}"
            else if (language == "ka") "$longestStreak დღე"
            else "$longestStreak ${if (longestStreak == 1) "Day" else "Days"}"
        val currentStreakExplanation = if (isWeeklyHabit) {
            tr(language, "Die Anzahl der aufeinanderfolgenden Tage, an denen dein 7-Tage-Ziel (${habit.specificDays}x in den vorangegangenen 7 Tagen) erreicht war.", "ზედიზედ რამდენი დღე იყო თქვენი 7-დღიანი მიზანი (${habit.specificDays}x ბოლო 7 დღეში) მიღწეული.", "你的 7 天滚动目标（前 7 天内达成 ${habit.specificDays} 次）连续保持的天数。", "The number of consecutive days your 7-day rolling goal (${habit.specificDays}x in the previous 7 days) was maintained.")
        } else {
            tr(language, "Die Anzahl der aufeinanderfolgenden Tage, an denen du diese Gewohnheit bis heute erfolgreich abgeschlossen hast.", "ზედიზედ რამდენი დღე დაასრულეთ ეს ჩვევა დღემდე.", "截至今天，你连续成功完成该习惯的天数。", "The number of consecutive days you have completed this habit up to today.")
        }
        val longestStreakExplanation = if (isWeeklyHabit) {
            tr(language, "Deine historische Bestleistung an aufeinanderfolgenden Tagen, an denen dein 7-Tage-Ziel (${habit.specificDays}x in den vorangegangenen 7 Tagen) erreicht war.", "თქვენი ისტორიული რეკორდი ზედიზედ დღეებში 7-დღიანი მიზნისთვის (${habit.specificDays}x ბოლო 7 დღეში).", "你的 7 天滚动目标（前 7 天内达成 ${habit.specificDays} 次）连续保持的历史最高纪录。", "Your highest historical record of consecutive days maintaining your 7-day rolling goal (${habit.specificDays}x in the previous 7 days).")
        } else {
            tr(language, "Deine historische Bestleistung an aufeinanderfolgenden Tagen, an denen du diese Gewohnheit abgeschlossen hast.", "თქვენი უმაღლესი ისტორიული ჩანაწერი ზედიზედ დღეებში ამ ჩვევის დასრულების შესახებ.", "你连续完成该习惯的历史最高连续天数记录。", "Your highest historical record of consecutive days completing this habit.")
        }

        // Current streak card
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    InfoIconButton(
                        title = tr(language, "Aktueller Streak", "მიმდინარე სტრიქონი", "当前连续", "Current Streak"),
                        explanation = currentStreakExplanation,
                        onClick = onInfoClick
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ProgressTrack.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Current streak",
                            tint = if (currentStreak > 0) HabitStreakFlame else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = tr(language, "Aktuelle Serie", "მიმდინარე სერი", "当前连续", "Current streak"),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = currentStreakText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Best/longest streak card
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    InfoIconButton(
                        title = tr(language, "Bester Streak", "საუკეთესო სტრიქონი", "最佳连续", "Best Streak"),
                        explanation = longestStreakExplanation,
                        onClick = onInfoClick
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ProgressTrack.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Best streak",
                            tint = if (longestStreak > 0) HabitStreakFlame else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = tr(language, "Beste Serie", "საუკეთესო სერია", "最佳连续", "Best streak"),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = longestStreakText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
