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
import com.example.data.MonthlyReview
import com.example.ui.HabitIconMapping
import com.example.ui.dialogs.animatedGlowingBorder

@Composable
fun MonthlyReviewCoverSlide(year: Int, month: Int, language: String, reviewData: MonthlyReviewData) {
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
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Calendar",
                tint = Color(0xFFA78BFA),
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
                text = "✨ ${reviewData.monthName.uppercase(java.util.Locale.US)} $year ✨",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr(language, "Dein Monatsrückblick", "თქვენი ყოველთვიური მიმოხილვა", "你的月度回顾", "Your Monthly Review"),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = tr(language, "Deine Routine, deine Gewohnheiten und dein Fortschritt im ${reviewData.monthName}. Lass uns deine Entwicklung feiern!", "თქვენი რუტინა, თქვენი ჩვევები და თქვენი პროგრესი ${reviewData.monthName} -ში. მოდით აღვნიშნოთ თქვენი ზრდა!", "在 ${reviewData.monthName} 中你的日常、习惯与成长轨迹。让我们共同庆祝你的进步！", "Your routine, your habits, and your progress in ${reviewData.monthName}. Let's celebrate your growth!"),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MonthlyReviewVolumeSlide(year: Int, month: Int, language: String, reviewData: MonthlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tr(language, "Dein Einsatz im ${reviewData.monthName}", "თქვენი თავდადება ${reviewData.monthName} -ში", "你在 ${reviewData.monthName} 的全情投入", "Your Dedication in ${reviewData.monthName}"),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Completions Card
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
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Completions",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${reviewData.totalCompletions} ${tr(language, "Abschlüsse", "დასრულებები", "完成次数", "Completions")}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    val compText = if (reviewData.prevMonthCompletions > 0) {
                        val sign = if (reviewData.growthPercentage >= 0) "+" else ""
                        tr(language, "$sign${reviewData.growthPercentage}% im Vergleich zum Vormonat (${reviewData.prevMonthCompletions}) 📈", "$sign ${reviewData.growthPercentage} % გასულ თვესთან შედარებით ( ${reviewData.prevMonthCompletions} ) 📈", "较上月 $sign${reviewData.growthPercentage}%（${reviewData.prevMonthCompletions} 次）📈", "$sign${reviewData.growthPercentage}% compared to last month (${reviewData.prevMonthCompletions}) 📈")
                    } else {
                        tr(language, "Dein erster aktiver Monat! 🌟", "თქვენი პირველი აქტიური თვე! 🌟", "你的第一个活跃月份！🌟", "Your first active month! 🌟")
                    }
                    Text(
                        text = compText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Check-ins Card
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
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "Checkins",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${reviewData.totalCheckIns} ${tr(language, "Check-ins insgesamt", "სულ ჩექინები", "累计打卡次数", "Total Check-ins")}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "Erfasste Einträge & Fortschritte in diesem Monat", "ამ თვეში ჩაწერილი ჩანაწერები და პროგრესი", "本月记录的打卡与进度", "Logged entries & progress this month"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Score Card
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
                        imageVector = Icons.Default.Star,
                        contentDescription = "Account Score",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = tr(language, "Account-Stärke", "ანგარიშის სიძლიერე", "账号稳固度", "Account Strength"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Score: ${reviewData.startScore} ➔ ${reviewData.endScore}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    val deltaSign = if (reviewData.scoreDelta >= 0) "+" else ""
                    val deltaColor = if (reviewData.scoreDelta >= 0) "🟢" else "🔴"
                    Text(
                        text = tr(language, "Veränderung: $deltaSign${reviewData.scoreDelta} Punkte $deltaColor", "შეცვლა: $deltaSign ${reviewData.scoreDelta} წერტილები $deltaColor", "变动：$deltaSign${reviewData.scoreDelta} 分 $deltaColor", "Change: $deltaSign${reviewData.scoreDelta} points $deltaColor"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyReviewMVPSlide(year: Int, month: Int, language: String, reviewData: MonthlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👑 " + (tr(language, "Gewohnheits-MVP", "ჩვევა MVP", "习惯 MVP", "Habit MVP")),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(language, "Deine absolut stärkste Routine in diesem Monat", "თქვენი ყველაზე ძლიერი რუტინა თვის", "你本月最稳固的日常习惯", "Your strongest routine of the month"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (reviewData.mvpHabit != null) {
            val h = reviewData.mvpHabit
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

                    Text(
                        text = tr(language, "${reviewData.mvpCompletions} Abschlüsse im ${reviewData.monthName}", "${reviewData.mvpCompletions} სრულდება ${reviewData.monthName} -ში", "在 ${reviewData.monthName} 累计完成 ${reviewData.mvpCompletions} 次", "${reviewData.mvpCompletions} completions in ${reviewData.monthName}"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tr(language, "Gewohnheitsstärke: ${reviewData.mvpStrength}%", "ჩვევის სიძლიერე: ${reviewData.mvpStrength} %", "习惯稳固度：${reviewData.mvpStrength}%", "Habit strength: ${reviewData.mvpStrength}%"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tr(language, "Unfassbar beständig! Behalte diese überragende Routine im nächsten Monat bei.", "წარმოუდგენელი თანმიმდევრულობა! შეინარჩუნეთ ეს უმაღლესი რუტინა შემდეგ თვეში.", "令人惊叹的自律！下个月继续保持这份卓越的日常。", "Incredible consistency! Maintain this superior routine next month."),
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
                    text = tr(language, "Noch keine ausreichenden Daten für diesen Monat.", "ჩვევების მონაცემები ამ თვისთვის ჯერ არ არის ხელმისაწვდომი.", "本月暂无可用的习惯数据。", "No habit data available for this month yet."),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


