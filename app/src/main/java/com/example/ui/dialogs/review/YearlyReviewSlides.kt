package com.example.ui.dialogs.review

import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
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
import com.example.data.YearlyReview
import com.example.ui.HabitIconMapping
import com.example.ui.dialogs.animatedGlowingBorder


@Composable
fun YearlyReviewCoverSlide(year: Int, language: String, reviewData: YearlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            color = PrimaryViolet.copy(alpha = 0.3f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.6f))
        ) {
            Text(
                text = "✨ $year IN REVIEW ✨",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr(language, "Dein Jahresrückblick $year", "თქვენი $year მიმოხილვა", "你的 $year 年度回顾", "Your $year Review"),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = tr(language, "Ein Jahr voller Gewohnheiten, Erfolge und persönlichem Wachstum. Lass uns deine Meilensteine feiern!", "ჩვევების, მიღწევებისა და პიროვნული ზრდის წელიწადი. მოდით აღვნიშნოთ თქვენი ეტაპები!", "充满自律、成就与个人成长的一年。让我们共同庆祝你的里程碑！", "A year of habits, achievements, and personal growth. Let's celebrate your milestones!"),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun YearlyReviewVolumeSlide(year: Int, language: String, reviewData: YearlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tr(language, "Dein Einsatz in $year", "თქვენი თავდადება $year -ში", "你在 $year 年的全情投入", "Your Dedication in $year"),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card 1: Total Completions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "Completions",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${reviewData.totalCompletions} ${tr(language, "Abschlüsse", "დასრულება", "完成次数", "Completions")}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = tr(language, "Erfüllte Gewohnheiten insgesamt", "სულ დასრულებული ჩვევები", "累计完成习惯", "Total habits completed"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 2: Total Check-ins
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Checkins",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${reviewData.totalCheckIns} ${tr(language, "Check-ins insgesamt", "სულ ჩექინები", "累计打卡次数", "Total Check-ins")}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "Erfasste Einträge & Fortschritte in diesem Jahr", "ამ წელს ჩაწერილი ჩანაწერები და პროგრესი", "本年度记录的打卡与进度", "Logged entries & progress this year"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: Active Days
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Active Days",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${reviewData.activeDaysCount} ${tr(language, "Tage", "დღეები", "天", "Days")}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "${reviewData.activeDaysPercentage}% aller Tage in $year aktiv gewesen", "აქტიურია ${reviewData.activeDaysPercentage} დღეების % $year -ში", "在 $year 年中活跃了 ${reviewData.activeDaysPercentage}% 的天数", "Active on ${reviewData.activeDaysPercentage}% of days in $year"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 3: Perfect Days
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Perfect Days",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${reviewData.perfectDaysCount} ${tr(language, "Perfekte Tage", "იდეალური დღეები", "完美天数", "Perfect Days")}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "100% aller Gewohnheiten an diesen Tagen abgeschlossen", "ყველა ჩვევა დასრულებულია ამ დღეებში", "这些天内所有习惯全部完成", "All habits completed on these days"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun YearlyReviewTopHabitSlide(year: Int, language: String, reviewData: YearlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⭐ " + (tr(language, "Star-Gewohnheit $year", "$year ვარსკვლავის ჩვევა", "$year 年度明星习惯", "$year Star Habit")),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(language, "Deine beständigste und erfolgreichste Gewohnheit", "თქვენი ყველაზე თანმიმდევრული და დასრულებული ჩვევა", "你最持之以恒、完成率最高的习惯", "Your most consistent and completed habit"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (reviewData.topHabit != null) {
            val h = reviewData.topHabit
            val hColor = parseHabitColor(h.color)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, hColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(hColor.copy(alpha = 0.25f), CircleShape)
                            .border(2.dp, hColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIconMapping.getIcon(h.icon),
                            contentDescription = h.name,
                            tint = hColor,
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

                    Surface(
                        color = hColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${reviewData.topHabitCompletions} " + (tr(language, "Abschlüsse in $year", "დასრულებები $year -ში", "$year 年累计完成", "Completions in $year")),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tr(language, "Du hast hier herausragende Disziplin gezeigt! Behalte diesen Schwung bei.", "აქ გამორჩეული დისციპლინა გამოავლინეთ! გააგრძელე ეს საოცარი იმპულსი.", "你在这里展现了非凡的自律！继续保持这股强劲势头。", "You showed outstanding discipline here! Keep up this amazing momentum."),
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
                    text = tr(language, "Noch keine ausreichenden Daten für $year.", "$year -სთვის ჩვევების მონაცემები ჯერ არ არის ხელმისაწვდომი.", "暂无 $year 年度的习惯数据。", "No habit data available for $year yet."),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
