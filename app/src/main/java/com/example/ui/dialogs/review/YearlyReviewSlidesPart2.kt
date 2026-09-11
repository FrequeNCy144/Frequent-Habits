package com.example.ui.dialogs.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.YearlyReviewData
import com.example.data.parseHabitColor
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.share.*
import com.example.ui.theme.SuccessGreen

@Composable
fun YearlyReviewGrowthSlide(year: Int, language: String, reviewData: YearlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌱 " + (tr(language, "Wachstumsfeld", "ზრდის ზონა", "成长潜力区", "Growth Area")),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(language, "Hier schlummert großes Potenzial für das neue Jahr ${year + 1}", "დიდი პოტენციალი გელოდებათ ${year + 1} -ში", "在 ${year + 1} 年中等待释放的巨大潜力", "Great potential waiting for you in ${year + 1}"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (reviewData.growthHabit != null) {
            val h = reviewData.growthHabit
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
                        text = "${reviewData.growthHabitCompletions} " + (tr(language, "Abschlüsse in $year", "დასრულებები $year -ში", "$year 年累计完成", "Completions in $year")),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFCD34D),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tr(language, "Setze dir für ${year + 1} kleine, machbare Schritte, um diese Gewohnheit auf das nächste Level zu heben!", "დააყენეთ მცირე, ქმედითი ნაბიჯები ${year + 1} -სთვის, რომ ეს ჩვევა შემდეგ დონეზე გადაიზარდოს!", "为 ${year + 1} 年设定切实可行的小步骤，让这个习惯更上一层楼！", "Set small, actionable steps for ${year + 1} to take this habit to the next level!"),
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
fun YearlyReviewPeakMomentsSlide(year: Int, language: String, reviewData: YearlyReviewData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📅 " + (tr(language, "Höhepunkte & Muster", "პიკის მომენტები და შაბლონები", "高光时刻与模式分析", "Peak Moments & Patterns")),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = tr(language, "Deine stärksten Zeiten im Jahr $year", "თქვენი ყველაზე პროდუქტიული დრო $year -ში", "$year 年中你最具成效的时光", "Your most productive times in $year"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Best Month
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
                        .background(Color(0xFF06B6D4).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Peak Month",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = tr(language, "Stärkster Monat", "ტოპ თვე", "最佳月份", "Top Month"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = reviewData.bestMonthName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${reviewData.bestMonthCompletions} " + (tr(language, "Abschlüsse in diesem Monat 🚀", "დასრულებები ამ თვეში 🚀", "本月累计完成 🚀", "Completions in this month 🚀")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Best Day of Week
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
                        .background(Color(0xFFEC4899).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Peak Day",
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = tr(language, "Produktivster Wochentag", "ყველაზე პროდუქტიული დღე", "最高效的一天", "Most Productive Day"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = reviewData.bestDayOfWeekName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${reviewData.bestDayOfWeekCompletions} " + (tr(language, "Abschlüsse an diesem Wochentag ⚡", "დასრულებები ამ სამუშაო დღეს ⚡", "该星期的累计完成次数 ⚡", "Completions on this weekday ⚡")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun YearlyReviewSummarySlide(
    year: Int,
    language: String,
    reviewData: YearlyReviewData,
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
                .background(SuccessGreen.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Awesome",
                tint = SuccessGreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr(language, "Zusammenfassung $year", "$year რეზიუმე", "$year 年度总结", "$year Summary"),
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
                        text = tr(language, "Gesamt-Abschlüsse:", "სრული დასრულებები:", "累计完成：", "Total Completions:"),
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
                        text = tr(language, "Aktive Tage:", "აქტიური დღეები:", "活跃天数：", "Active Days:"),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${reviewData.activeDaysCount} (${reviewData.activeDaysPercentage}%)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (reviewData.topHabit != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(language, "Top Gewohnheit:", "მთავარი ჩვევა:", "最佳习惯：", "Top Habit:"),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = HabitIconMapping.getIcon(reviewData.topHabit.icon),
                                contentDescription = null,
                                tint = parseHabitColor(reviewData.topHabit.color),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = reviewData.topHabit.name,
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
                        text = tr(language, "Bester Monat:", "საუკეთესო თვე:", "最佳月份：", "Best Month:"),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = reviewData.bestMonthName,
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
                containerColor = Color(0xFFA855F7),
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
            text = tr(language, "Danke für deine Treue in $year! Auf ein fantastisches, produktives neues Jahr!", "გმადლობთ $year -ში თანმიმდევრულობისთვის! აქ არის ფანტასტიკური და ჩვევებით სავსე ახალი წელი!", "感谢你在 $year 年的持之以恒！祝愿你在新的一年里收获满满、精彩不断！", "Thank you for your consistency in $year! Here's to a fantastic and habit-filled new year!"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        if (showShareDialog) {
            com.example.ui.share.YearlyReviewShareDialog(
                year = year,
                reviewData = reviewData,
                language = language,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}
