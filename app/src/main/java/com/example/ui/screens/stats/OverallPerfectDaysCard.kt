package com.example.ui.screens.stats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PerfectDaysStats
import com.example.tr
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*

@Composable
fun OverallPerfectDaysCard(
    perfectDaysStats: PerfectDaysStats,
    language: String,
    accentColor: Color = PrimaryViolet,
    onInfoClick: (String, String) -> Unit
) {
    val currentStreakExplanation = if (language == "de") {
        "Die Anzahl aufeinanderfolgender Tage bis heute, an denen du ausnahmslos alle fälligen Gewohnheiten zu 100% abgeschlossen hast."
    } else if (language == "ka") {
        "ზედიზედ დღეების რაოდენობა დღემდე, როდესაც 100%-ით დაასრულეთ ყველა ჩვევა."
    } else {
        "The number of consecutive days up to today where you successfully completed 100% of all scheduled habits."
    }

    val longestStreakExplanation = if (language == "de") {
        "Deine historische Bestleistung an aufeinanderfolgenden perfekten Tagen, an denen alle fälligen Gewohnheiten vollständig abgeschlossen wurden."
    } else if (language == "ka") {
        "თქვენი ისტორიული რეკორდი ზედიზედ სრულყოფილ დღეებში."
    } else {
        "Your all-time highest record of consecutive perfect days with 100% habit completion."
    }

    val currentDaysText = "${perfectDaysStats.currentStreak} " + tr(
        language,
        if (perfectDaysStats.currentStreak == 1) "Tag" else "Tage",
        "დღე",
        "天",
        if (perfectDaysStats.currentStreak == 1) "Day" else "Days"
    )

    val longestDaysText = "${perfectDaysStats.perfectDaysStreak} " + tr(
        language,
        if (perfectDaysStats.perfectDaysStreak == 1) "Tag" else "Tage",
        "დღე",
        "天",
        if (perfectDaysStats.perfectDaysStreak == 1) "Day" else "Days"
    )

    // Row of 2 streak cards (Aktuelle Serie & Beste Serie) matching HabitDetailScreen design
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Current streak card (Flame)
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .border(
                    1.dp,
                    AppBorder,
                    RoundedCornerShape(20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    InfoIconButton(
                        title = tr(language, "Aktuelle Serie", "მიმდინარე სტრიქონი", "当前连续", "Current Streak"),
                        explanation = currentStreakExplanation,
                        onClick = { t, e -> onInfoClick(t, e) }
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
                            tint = if (perfectDaysStats.currentStreak > 0) PerfectStreakFlame else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = tr(language, "Aktuelle Serie", "მიმდინარე სერია", "当前连续", "Current streak"),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = currentDaysText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Best streak card (Trophy)
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .border(
                    1.dp,
                    AppBorder,
                    RoundedCornerShape(20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    InfoIconButton(
                        title = tr(language, "Beste Serie", "საუკეთესო სტრიქონი", "最佳连续", "Best Streak"),
                        explanation = longestStreakExplanation,
                        onClick = { t, e -> onInfoClick(t, e) }
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
                            tint = if (perfectDaysStats.perfectDaysStreak > 0) PerfectStreakFlame else TextSecondary.copy(alpha = 0.4f),
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
                        text = longestDaysText,
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
