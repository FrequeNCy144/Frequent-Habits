package com.example.ui.screens.createhabit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppSegmentedButton
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

@Composable
fun HabitScheduleCard(
    language: String,
    frequencyType: String,
    onFrequencyTypeChange: (String) -> Unit,
    targetDaysPerWeek: Int,
    onTargetDaysPerWeekChange: (Int) -> Unit,
    targetDaysPerMonth: Int,
    onTargetDaysPerMonthChange: (Int) -> Unit,
    intervalDays: Int,
    onIntervalDaysChange: (Int) -> Unit,
    selectedWeekdays: Set<Int>,
    onToggleWeekday: (Int) -> Unit,
    activeColor: Color
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                        Text(
                            text = tr(language, "Häufigkeit & Startdatum", "სიხშირე და თარიღი", "频率与开始日期", "Frequency & Start Date"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Frequency Selector
                    val freqOptions = listOf(
                        "DAILY" to (tr(language, "Täglich", "ყოველდღიური", "每天", "Daily")),
                        "TIMES_WEEKLY" to (tr(language, "X mal/Wo", "X ჯერ / კვირა", "每周 X 次", "X times/Wk")),
                        "SPECIFIC" to (tr(language, "Feste Tage", "დღეების არჩევა", "指定星期", "Exact Days"))
                    )
                    val selectedFreqIndex = freqOptions.indexOfFirst { it.first == frequencyType }.coerceAtLeast(0)
                    AppSegmentedButton(
                        options = freqOptions.map { it.second },
                        selectedIndex = selectedFreqIndex,
                        onOptionSelected = { index ->
                            val key = freqOptions[index].first
                            onFrequencyTypeChange(key)
                        },
                        testTagPrefix = "frequency_selection"
                    )

                    if (frequencyType == "TIMES_WEEKLY") {
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
