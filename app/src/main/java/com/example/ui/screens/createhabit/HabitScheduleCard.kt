package com.example.ui.screens.createhabit
import com.frequent.habits.R

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.components.AppSegmentedButton
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

private data class FrequencyOptionItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int
)

@Composable
fun HabitScheduleCard(
    language: String,
    frequencyType: String,
    onFrequencyTypeChange: (String) -> Unit,
    isNumeric: Boolean = false,
    targetDaysPerWeek: Int,
    onTargetDaysPerWeekChange: (Int) -> Unit,
    targetDaysPerMonth: Int,
    onTargetDaysPerMonthChange: (Int) -> Unit,
    intervalDays: Int,
    onIntervalDaysChange: (Int) -> Unit,
    selectedWeekdays: Set<Int>,
    onToggleWeekday: (Int) -> Unit,
    activeColor: Color,
    onShowInfoDialog: (String, String) -> Unit = { _, _ -> }
) {
            // 4. CARD: Häufigkeit & Startdatum
            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(activeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calendar),
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr(language, "Häufigkeit & Rhythmus", "სიხშირე და რიტმი", "频率与节奏", "Frequency & Rhythm"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(language, "Wie oft möchtest du aktiv werden?", "რამდენად ხშირად?", "你希望执行此习惯的频率？", "How often do you want to do this?"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onShowInfoDialog(
                                    tr(language, "Häufigkeit & Zeitfenster", "სიხშირე და დროის ფანჯარა", "频率与时间窗口", "Frequency & Time Windows"),
                                    tr(
                                        language,
                                        "• Täglich: Du möchtest jeden einzelnen Tag aktiv werden.\n\n• X mal/Woche (Rollierendes 7-Tage-Fenster): Schaut immer auf die jeweils letzten 7 Tage. Verhindert den 'Montags-Schock', sodass deine Erfolge vom Wochenende nicht montags verpuffen.\n\n• X mal/Monat (Rollierendes 30-Tage-Fenster): Schaut kontinuierlich auf deine letzten 30 Tage. Dadurch wirst du durch kurze Monate (z. B. Februar mit 28 Tagen) niemals benachteiligt und dein Fortschritt bleibt auch am 1. eines Monats voll erhalten!\n\n• Intervall: Alle N Tage aktiv (z. B. alle 3 Tage).\n\n• Feste Tage: Ausgewählte Wochentage (z. B. jeden Dienstag & Donnerstag).",
                                        "• Daily: Active every single day.\n\n• Weekly: Rolling 7-day window.\n\n• Monthly: Rolling 30-day window (Loop style).\n\n• Interval: Every N days.\n\n• Exact Days: Selected weekdays.",
                                        "• 每日：每天执行。\n\n• 每周 X 次：滚动 7 天窗口。\n\n• 每月 X 次：滚动 30 天窗口，避免二月等天数差异与月初归零。\n\n• 间隔周期：每 N 天一次。\n\n• 指定星期：固定星期几执行。",
                                        "• Daily: Active every single day.\n\n• X times/Week: Rolling 7-day window. Evaluates your completion across the last 7 days continuously without resetting on Monday morning.\n\n• X times/Month: Rolling 30-day window. Evaluates your consistency across the last 30 days. No penalties for short months (like February) and no abrupt resets on the 1st of the month!\n\n• Interval: Repeats every N days (e.g. every 3 days).\n\n• Exact Days: Scheduled on specific days of the week (e.g. Mon, Wed, Fri)."
                                    )
                                )
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Frequency Selector (untereinander gestapelt mit vollem Namen)
                    val freqOptions = listOf(
                        FrequencyOptionItem(
                            key = "DAILY",
                            title = tr(language, "Täglich", "ყოველდღიური", "每天", "Daily"),
                            subtitle = tr(language, "Jeden Tag aktiv", "აქტიურია ყოველდღე", "每日执行打卡", "Active every single day"),
                            iconRes = R.drawable.ic_calendar
                        ),
                        FrequencyOptionItem(
                            key = "TIMES_WEEKLY",
                            title = tr(language, "X mal pro Woche", "X ჯერ კვირაში", "每周 X 次", "X times per week"),
                            subtitle = tr(language, "Rollierendes 7-Tage-Fenster (ohne Wochenstart-Reset)", "7-დღიანი მოძრავი ფანჯარა", "滚动 7 天窗口，周一不重置", "Rolling 7-day window without Monday reset"),
                            iconRes = R.drawable.ic_target
                        ),
                        FrequencyOptionItem(
                            key = "TIMES_MONTHLY",
                            title = tr(language, "X mal pro Monat", "X ჯერ თვეში", "每月 X 次", "X times per month"),
                            subtitle = tr(language, "Rollierendes 30-Tage-Fenster (fair bei kurzen Monaten)", "30-დღიანი მოძრავი ფანჯარა", "滚动 30 天窗口，避免月份天数差异", "Rolling 30-day window across past 30 days"),
                            iconRes = R.drawable.ic_calendar
                        ),
                        FrequencyOptionItem(
                            key = "INTERVAL",
                            title = tr(language, "Festes Intervall", "ინტერვალი", "固定间隔周期", "Fixed Interval"),
                            subtitle = tr(language, "Alle N Tage wiederholen (z. B. alle 3 Tage)", "განმეორება ყოველ N დღეში", "每 N 天重复一次", "Repeat every N days (e.g. every 3 days)"),
                            iconRes = R.drawable.ic_timer
                        ),
                        FrequencyOptionItem(
                            key = "SPECIFIC",
                            title = tr(language, "Feste Wochentage", "კონკრეტული დღეები", "指定星期", "Specific Weekdays"),
                            subtitle = tr(language, "Nur an ausgewählten Wochentagen (z. B. Mo, Fr)", "მხოლოდ შერჩეულ დღეებში", "仅在选定星期几执行", "Scheduled on specific days of the week"),
                            iconRes = R.drawable.ic_task
                        )
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        freqOptions.forEach { opt ->
                            val isSelected = frequencyType == opt.key
                            Surface(
                                onClick = { onFrequencyTypeChange(opt.key) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) activeColor.copy(alpha = 0.12f) else ProgressTrack,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) activeColor else AppBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("frequency_option_${opt.key.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Leading Icon in circle
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                if (isSelected) activeColor.copy(alpha = 0.22f) else AppCard,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = opt.iconRes),
                                            contentDescription = null,
                                            tint = if (isSelected) activeColor else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Full Title + Subtitle
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = opt.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = opt.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) activeColor else TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Selection Radio / Check Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .border(
                                                width = if (isSelected) 0.dp else 1.5.dp,
                                                color = if (isSelected) Color.Transparent else TextSecondary.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .background(
                                                if (isSelected) activeColor else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (frequencyType == "INTERVAL") {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Text(
                                text = tr(language, "Alle ", "ყოველ ", "每 ", "Every ") + "$intervalDays " + tr(language, "Tage wiederholen", "დღეში ერთხელ", "天执行一次", "days"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = intervalDays.toFloat(),
                                onValueChange = { onIntervalDaysChange(it.toInt()) },
                                valueRange = 2f..30f,
                                steps = 27,
                                colors = SliderDefaults.colors(
                                    thumbColor = SuccessGreen,
                                    activeTrackColor = SuccessGreen,
                                    inactiveTrackColor = ProgressTrack
                                )
                            )
                        }
                    }

                    if (frequencyType == "TIMES_MONTHLY") {
                        if (isNumeric) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_target),
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(
                                            language,
                                            "Monatliches Summenziel: Alle Einträge der letzten 30 Tage werden zu deinem Ziel summiert.",
                                            "ყველა ჩანაწერი ბოლო 30 დღის განმავლობაში შეჯამდება.",
                                            "30 天汇总目标：最近 30 天内的所有记录将累加到你的总目标中。",
                                            "30-day aggregate goal: All entries across the past 30 days are summed towards your goal."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Text(
                                    text = tr(language, "Ziel im 30-Tage-Fenster: ", "მიზანი 30 დღეში: ", "30 天内目标: ", "Target in 30 days: ") + "$targetDaysPerMonth " + tr(language, "Tage", "დღე", "天", "days"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = targetDaysPerMonth.toFloat(),
                                    onValueChange = { onTargetDaysPerMonthChange(it.toInt()) },
                                    valueRange = 1f..30f,
                                    steps = 28,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SuccessGreen,
                                        activeTrackColor = SuccessGreen,
                                        inactiveTrackColor = ProgressTrack
                                    )
                                )
                            }
                        }
                    }

                    if (frequencyType == "TIMES_WEEKLY") {

                        if (isNumeric) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_target),
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(
                                            language,
                                            "Wöchentliches Summenziel: Alle Einträge der letzten 7 Tage werden zu deinem Wochenziel summiert.",
                                            "ყველა ჩანაწერი ბოლო 7 დღის განმავლობაში შეჯამდება.",
                                            "周度汇总目标：最近 7 天内的所有记录将累加到你的总目标中。",
                                            "Weekly aggregate goal: All entries from the last 7 days are summed towards your weekly goal."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..7).forEach { num ->
                                    val isSelected = targetDaysPerWeek == num
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                color = if (isSelected) SuccessGreen else ProgressTrack,
                                                shape = CircleShape
                                            )
                                            .clickable { onTargetDaysPerWeekChange(num) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = num.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (frequencyType == "SPECIFIC") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val weekdays = when (language) {
                                "de" -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                                "ka" -> listOf("ორშ", "სამ", "ოთხ", "ხუთ", "პარ", "შაბ", "კვი")
                                "zh" -> listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                                else -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            }
                            weekdays.forEachIndexed { index, dayName ->
                                val dayNum = index + 1
                                val isSelected = selectedWeekdays.contains(dayNum)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            color = if (isSelected) SuccessGreen else ProgressTrack,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onToggleWeekday(dayNum)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (language == "ka" || language == "zh") dayName else dayName.take(2),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
}
