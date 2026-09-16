package com.example.ui.screens.habitdetail
import com.frequent.habits.R

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChallengeCalculationEngine
import com.example.data.Habit
import com.example.data.HabitCalculationEngine
import com.example.data.HabitLog
import com.example.tr
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

@Composable
fun FinishableHabitProgressCard(
    habit: Habit,
    totalCount: Int,
    currentStreak: Int,
    logs: List<HabitLog>,
    language: String,
    onInfoClick: (String, String) -> Unit = { _, _ -> }
) {
    val totalTarget: Float = habit.totalTargetValue ?: 0f
    if (totalTarget <= 0f) return

    val isNumeric = habit.type == "NUMBER" || habit.type == "NUMERICAL"
    val isNegative = habit.isNegative

    // Fetch challenge stats
    val challengeStats = ChallengeCalculationEngine.getChallengeStats(habit, logs)

    val currentProgress: Float = when (habit.challengeMode) {
        "STREAK" -> challengeStats.currentStreak.toFloat()
        "SUCCESS_RATE" -> challengeStats.cleanDays.toFloat()
        else -> {
            if (isNegative) challengeStats.cleanDays.toFloat()
            else if (isNumeric) logs.filter { it.habitId == habit.id && it.value > 0f }.sumOf { it.value.toDouble() }.toFloat()
            else totalCount.toFloat()
        }
    }

    val progressPercent = ((currentProgress / totalTarget) * 100f).coerceIn(0f, 100f).toInt()
    val remaining = (totalTarget - currentProgress).coerceAtLeast(0f)

    // Forecast and pace
    val now = LocalDate.now()
    val validStartMillis = if (habit.startDate > 946684800000L) habit.startDate else habit.createdAt
    val startDate = Instant.ofEpochMilli(validStartMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val totalDaysActive = (ChronoUnit.DAYS.between(startDate, now) + 1).coerceAtLeast(1)
    val avgDailyProgress = if (totalDaysActive > 0) currentProgress / totalDaysActive.toFloat() else 0f

    val daysToFinish: Long? = if (remaining <= 0f) {
        0L
    } else if (habit.challengeMode in listOf("STREAK", "SUCCESS_RATE") || isNegative) {
        remaining.toLong()
    } else if (avgDailyProgress > 0.001f) {
        ceil(remaining / avgDailyProgress).toLong()
    } else {
        null
    }

    val estimatedCompletionDateStr: String = if (remaining <= 0f) {
        tr(language, "Bereits erreicht! 🎉", "უკვე მიღწეულია! 🎉", "已达成！🎉", "Already completed! 🎉")
    } else if (daysToFinish != null && daysToFinish in 1..3650) {
        val estimatedDate = now.plusDays(daysToFinish)
        val formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", when (language) {
            "de" -> Locale.GERMANY
            "zh" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.US
        })
        val tagWord = if (daysToFinish == 1L) {
            tr(language, "Tag", "დღე", "天", "day")
        } else {
            tr(language, "Tage", "დღე", "天", "days")
        }
        estimatedDate.format(formatter) + " (~$daysToFinish $tagWord)"
    } else {
        tr(language, "Nicht genügend Aktivität für Prognose", "არასაკმარისი მონაცემები", "数据不足无法预测", "Not enough data for forecast")
    }

    val formattedCurrent = if (currentProgress % 1f == 0f) currentProgress.toInt().toString() else String.format(Locale.US, "%.1f", currentProgress)
    val formattedTarget = if (totalTarget % 1f == 0f) totalTarget.toInt().toString() else String.format(Locale.US, "%.1f", totalTarget)
    val unitSuffix = if (habit.challengeMode in listOf("STREAK", "SUCCESS_RATE")) {
        " " + tr(language, "Tage", "დღე", "天", "days")
    } else if (isNumeric && !isNegative && habit.unit.isNotBlank()) {
        " ${habit.unit}"
    } else {
        " " + tr(language, "Tage", "დღე", "天", "days")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HabitYellow.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(HabitYellow.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trophy),
                        contentDescription = "Milestone Goal",
                        tint = HabitYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isNegative) {
                                tr(language, "CHALLENGE-FORTSCHRITT", "გამოწვევის პროგრესი", "挑战进度", "CHALLENGE PROGRESS")
                            } else {
                                tr(language, "GESAMTZIELE-FORTSCHRITT", "საერთო მიზნის პროგრესი", "总目标进度", "MILESTONE PROGRESS")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = HabitYellow,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        val explanationText = when (habit.challengeMode) {
                            "STREAK" -> tr(
                                language,
                                "Harter Streak: Jeder verpasste Tag setzt deinen Zähler auf 0 zurück. Du benötigst $formattedTarget fehlerfreie Tage am Stück.",
                                "მკაცრი სტრიკი: ნებისმიერი ჩავარდნა ანულებს მრიცხველს. გჭირდებათ $formattedTarget უწყვეტი დღე.",
                                "连续天数：一旦中断计数器归零。你需要连续达成 $formattedTarget 天。",
                                "Hard Streak: Any missed day resets your counter to 0. You need $formattedTarget consecutive successful days."
                            )
                            "SUCCESS_RATE" -> tr(
                                language,
                                "Erfolgsquote: Du musst im Zeitraum von $formattedTarget Tagen mindestens ${habit.challengeSuccessRate}% Erfolgsquote halten.",
                                "წარმატების %: $formattedTarget დღის განმავლობაში უნდა შეინარჩუნოთ მინიმუმ ${habit.challengeSuccessRate}%.",
                                "成功率：在 $formattedTarget 天的周期内，你需要保持至少 ${habit.challengeSuccessRate}% 的成功率。",
                                "Success Rate: Over the span of $formattedTarget days, you must maintain at least ${habit.challengeSuccessRate}% successful days."
                            )
                            else -> {
                                if (isNegative) {
                                    tr(
                                        language,
                                        "Tage sammeln: Jeder abstinente Tag zählt als Meilenstein. Rückfälle stoppen den Tag, setzen das Gesamtergebnis aber nicht auf 0.",
                                        "დღეების შეგროვება: ყოველი სუფთა დღე ემატება საერთო მიზანს. ჩავარდნები არ ანულებს შედეგს.",
                                        "累计天数：每个清爽天数都会累计。破戒不会清空既往成果。",
                                        "Clean Days: Each clean day adds towards your goal. Slip-ups don't reset your total progress."
                                    )
                                } else {
                                    tr(
                                        language,
                                        "Zeigt deinen aktuellen Fortschritt bis zum Erreichen deines Gesamtziels. Die Prognose berechnet anhand deines bisherigen Tagesdurchschnitts das voraussichtliche Zieldatum.",
                                        "აჩვენებს თქვენს პროგრესს საბოლოო მიზნამდე. პროგნოზი ეფუძნება თქვენს საშუალო ტემპს.",
                                        "展示你达成总目标的当前进度。系统根据你以往的日常节奏预测达成日期。",
                                        "Shows your progress towards your total target. The forecast estimates your completion date based on your average daily pace."
                                    )
                                }
                            }
                        }
                        InfoIconButton(
                            title = tr(language, "Ziel-Fortschritt & Prognose", "მიზნის პროგრესი და პროგნოზი", "目标进度与预计完成", "Goal Progress & Forecast"),
                            explanation = explanationText,
                            onClick = onInfoClick
                        )
                    }
                    Text(
                        text = if (isNegative) {
                            tr(language, "Abstinenz-Ziel: $formattedTarget$unitSuffix", "სამიზნე: $formattedTarget$unitSuffix", "目标戒除：$formattedTarget$unitSuffix", "Target Clean: $formattedTarget$unitSuffix")
                        } else {
                            tr(language, "Abschlussziel: $formattedTarget$unitSuffix", "სამიზნე: $formattedTarget$unitSuffix", "达成总目标：$formattedTarget$unitSuffix", "Total Target: $formattedTarget$unitSuffix")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Challenge mode pill tag
                val modeLabel = when (habit.challengeMode) {
                    "STREAK" -> tr(language, "Harter Streak", "მკაცრი სტრიკი", "连续天数", "Hard Streak")
                    "SUCCESS_RATE" -> tr(language, "Quote (${habit.challengeSuccessRate}%)", "წარმატების %", "成功率 (${habit.challengeSuccessRate}%)", "Rate (${habit.challengeSuccessRate}%)")
                    else -> {
                        if (isNegative) tr(language, "Tage sammeln", "დღეების შეგროვება", "累计天数", "Clean Days")
                        else tr(language, "Fortschritt", "პროგრესი", "累计进度", "Accumulate")
                    }
                }
                Surface(
                    color = PrimaryViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryViolet,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Big Metric & Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formattedCurrent,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = " / $formattedTarget$unitSuffix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (progressPercent >= 100) SuccessGreen else HabitYellow
                )
            }

            // Big Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((progressPercent / 100f).coerceIn(0.01f, 1f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(HabitYellow, PrimaryViolet)
                            )
                        )
                )
            }

            // For SUCCESS_RATE mode: extra breakdown card
            if (habit.challengeMode == "SUCCESS_RATE") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isNegative) tr(language, "Clean Days", "სუფთა დღეები", "清爽天数", "Clean Days")
                                       else tr(language, "Erfolgreich", "წარმატებული", "达成天数", "Successful"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${challengeStats.cleanDays} / ${challengeStats.totalActiveDays}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = AppBorder
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tr(language, "Aktuelle Quote", "მიმდინარე %", "当前成功率", "Current Rate"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${challengeStats.currentRatePercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (challengeStats.currentRatePercent >= habit.challengeSuccessRate) SuccessGreen else HabitRed
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = AppBorder
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tr(language, "Gefordert", "სამიზნე %", "目标要求", "Required"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${habit.challengeSuccessRate}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryViolet
                            )
                        }
                    }
                }
            }

            // Forecast info box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = if (isNegative) painterResource(id = R.drawable.ic_calendar) else painterResource(id = R.drawable.ic_trending_up),
                            contentDescription = if (isNegative) "Zieltag" else "Prognose",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isNegative) {
                                tr(language, "Geplante Zielerreichung (Challenge-Ende)", "მიზნის მიღწევის თარიღი", "预计达成日期", "Target Completion Date")
                            } else {
                                tr(language, "Voraussichtliche Zielerreichung", "სავარაუდო დასრულების თარიღი", "预计达成日期", "Estimated Completion")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = estimatedCompletionDateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (remaining > 0f && habit.challengeMode in listOf("STREAK", "SUCCESS_RATE")) {
                        val remainingDaysInt = remaining.toInt()
                        val explanationText = when (habit.challengeMode) {
                            "STREAK" -> if (isNegative) {
                                tr(
                                    language,
                                    "Noch $remainingDaysInt Tage fehlerfreier Streak nötig. Ein Ausrutscher setzt auf 0 zurück!",
                                    "დარჩენილია $remainingDaysInt დღე უწყვეტი სტრიკი.",
                                    "还需坚持连续 $remainingDaysInt 天。一旦破戒将清零！",
                                    "$remainingDaysInt unbroken streak days needed. A slip-up resets to 0!"
                                )
                            } else {
                                tr(
                                    language,
                                    "Noch $remainingDaysInt Tage fehlerfreier Streak nötig. Ein verpasster Tag setzt auf 0 zurück!",
                                    "დარჩენილია $remainingDaysInt დღე უწყვეტი სტრიკი.",
                                    "还需连续达成 $remainingDaysInt 天。一旦漏打卡将清零！",
                                    "$remainingDaysInt unbroken streak days needed. A missed day resets to 0!"
                                )
                            }
                            "SUCCESS_RATE" -> tr(
                                language,
                                "Noch $remainingDaysInt Tage im Challenge-Zeitraum. Halte deine Quote über ${habit.challengeSuccessRate}%.",
                                "დარჩენილია $remainingDaysInt დღე გამოწვევის პერიოდში.",
                                "挑战期还剩 $remainingDaysInt 天。请保持成功率在 ${habit.challengeSuccessRate}% 以上。",
                                "$remainingDaysInt days left in challenge. Keep your rate above ${habit.challengeSuccessRate}%."
                            )
                            else -> ""
                        }
                        Text(
                            text = explanationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    } else if (isNegative && remaining > 0f) {
                        val remainingDaysInt = remaining.toInt()
                        Text(
                            text = tr(
                                language,
                                "Noch $remainingDaysInt clean-Tage bis zum Meilenstein-Ziel",
                                "დარჩენილია $remainingDaysInt სუფთა დღე",
                                "距目标还需坚持 $remainingDaysInt 天",
                                "$remainingDaysInt clean days remaining to reach goal"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    } else if (!isNegative && avgDailyProgress > 0f && remaining > 0f) {
                        val formattedPace = if (avgDailyProgress >= 10f) avgDailyProgress.toInt().toString() else String.format(Locale.US, "%.1f", avgDailyProgress)
                        Text(
                            text = tr(language, "Aktuelles Tempo: ~$formattedPace pro Tag", "მიმდინარე ტემპი: ~$formattedPace დღეში", "当前节奏：每天 ~$formattedPace", "Current pace: ~$formattedPace / day"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
