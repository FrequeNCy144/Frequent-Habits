package com.example.ui.dialogs.review

import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.share.*
import com.example.tr

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MonthlyReview
import com.example.ui.HabitIconMapping

@Composable
fun MonthlyReviewFocusSlide(year: Int, month: Int, language: String, reviewData: MonthlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌱 " + (tr(language, "Fokus-Bereich", "ფოკუსის ზონა", "重点提升区", "Focus Area")),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(language, "Hier gab es die meisten Lücken – ein super Potenzial für den neuen Monat!", "სადაც ყველაზე მეტი ხარვეზი გამოჩნდა - დიდი პოტენციალი ახალი თვისთვის!", "出现缺卡较多的地方——新月份的大好提升机会！", "Where most gaps appeared – a great potential for the new month!"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (reviewData.focusHabit != null) {
            val h = reviewData.focusHabit
            val hColor = parseHabitColor(h.color)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape)
                            .border(2.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIconMapping.getIcon(h.icon),
                            contentDescription = h.name,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = h.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = tr(language, "${reviewData.focusCompletions} von ${reviewData.focusExpected} Einheiten erledigt", "დასრულდა ${reviewData.focusExpected} ერთეულების ${reviewData.focusCompletions}", "完成 ${reviewData.focusCompletions} / ${reviewData.focusExpected} 个单位", "${reviewData.focusCompletions} of ${reviewData.focusExpected} units completed"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFCD34D),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tr(language, "Keine Sorge! Nimm dir für den neuen Monat vor, diese Gewohnheit als Erstes am Tag zu erledigen.", "არ ინერვიულო! შეეცადეთ დაგეგმოთ ეს ჩვევა დილით პირველ რიგში ახალი თვისთვის.", "别灰心！在新月份尝试将这个习惯安排在清晨第一件事来做。", "Don't worry! Try scheduling this habit first thing in the morning for the new month."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = tr(language, "Du liegst bei allen deinen Gewohnheiten voll im Plan! Super!", "თქვენ ანადგურებთ ყველა თქვენს ჩვევას! დიდი სამუშაო!", "你的所有习惯都在完美推进中！太棒了！", "You are crushing all your habits! Great job!"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MonthlyReviewSummarySlide(
    year: Int,
    month: Int,
    language: String,
    reviewData: MonthlyReviewData,
    onDismiss: () -> Unit
) {
    var showShareDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Awesome",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr(language, "Zusammenfassung ${reviewData.monthName}", "${reviewData.monthName} რეზიუმე", "${reviewData.monthName} 月度总结", "${reviewData.monthName} Summary"),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(language, "Gesamt-Abschlüsse:", "სრული დასრულებები:", "累计完成：", "Total completions:"),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = reviewData.totalCompletions.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(language, "Score:", "ქულა:", "得分：", "Score:"),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = reviewData.endScore.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (reviewData.mvpHabit != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(language, "Habit MVP:", "ჩვევა MVP:", "习惯 MVP：", "Habit MVP:"),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = HabitIconMapping.getIcon(reviewData.mvpHabit.icon),
                                contentDescription = null,
                                tint = parseHabitColor(reviewData.mvpHabit.color),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = reviewData.mvpHabit.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(language, "Power day:", "დენის დღე:", "能量单日：", "Power day:"),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = reviewData.bestDayOfWeekName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                showShareDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tr(language, "Rückblick teilen", "გააზიარეთ მიმოხილვა", "分享回顾", "Share Review"),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr(language, "Großartige Arbeit! Jeder Tag ist eine Chance, besser zu werden. Weiter so!", "დიდი სამუშაო! ყოველი დღე არის ზრდის ახალი შანსი. ასე გააგრძელე!", "做得很棒！每一天都是成长的新契机。继续加油！", "Great job! Every day is a new chance to grow. Keep it up!"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        if (showShareDialog) {
            com.example.ui.share.MonthlyReviewShareDialog(
                year = year,
                reviewData = reviewData,
                language = language,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}
