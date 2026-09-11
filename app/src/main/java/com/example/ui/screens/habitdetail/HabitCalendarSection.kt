package com.example.ui.screens.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarCellState
import com.example.data.Habit
import com.example.tr
import com.example.ui.components.InfoIconButton
import com.example.ui.screens.stats.CalendarDayCell
import com.example.ui.theme.*

@Composable
fun HabitCalendarCard(
    habit: Habit,
    gridRows: List<List<CalendarCellState?>>,
    monthName: String,
    habitColor: Color,
    accentColor: Color,
    language: String,
    canPrevMonth: Boolean,
    canNextMonth: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (String) -> Unit,
    onInfoClick: (String, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(accentColor.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Kalender",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "KALENDER-VERLAUF", "კალენდრის ისტორია", "日历历史记录", "CALENDAR HISTORY"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Monatskalender", "თვის კალენდარი", "月历", "Monthly Calendar"),
                    explanation = if (language == "de") {
                        if (habit.isNegative) {
                            "Zeigt deinen Monatsverlauf. Tippe auf einen Tag, um zwischen Erfolgreich (Clean) und Gescheitert (Rückfall) zu wechseln."
                        } else {
                            "Zeigt deinen Monatsverlauf. Tippe auf einen Tag, um den Status direkt zu ändern (Ausstehend -> Erreicht -> Gescheitert)."
                        }
                    } else if (language == "ka") {
                        "აჩვენებს თქვენი თვის ისტორიას. შეეხეთ დღეს სტატუსის შესაცვლელად."
                    } else if (language == "zh") {
                        "显示您的月度记录。轻触某一天可直接切换状态。"
                    } else {
                        if (habit.isNegative) {
                            "Shows your monthly history. Tap any day to toggle between Clean (Success) and Relapse (Failed)."
                        } else {
                            "Shows your monthly history. Tap any day to cycle status (Pending -> Completed -> Failed)."
                        }
                    },
                    onClick = onInfoClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    enabled = canPrevMonth
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = if (canPrevMonth) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                    )
                }
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onNextMonth,
                    enabled = canNextMonth
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = if (canNextMonth) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }

            // Weekday Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shortDayNames = if (language == "de") listOf("MO", "DI", "MI", "DO", "FR", "SA", "SO") else listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
                shortDayNames.forEach {
                    key(it) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid Rows
            gridRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until 7) {
                        val cell = if (c < row.size) row[c] else null
                        if (cell != null) {
                            key(cell.id) {
                                CalendarDayCell(
                                    dayNum = cell.dayNum,
                                    status = cell.status,
                                    habitColor = habitColor,
                                    onCellClick = if (cell.id.isNotBlank()) { { onDayClick(cell.id) } } else null,
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legende (Erreicht, Gescheitert, Ausstehend, Pausiert)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (habit.isNegative) tr(language, "Erfolgreich (Clean)", "წარმატებული", "成功戒除", "Clean") else tr(language, "Erreicht", "დასრულებული", "已完成", "Completed"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (habit.isNegative) tr(language, "Rückfall", "ჩავარდნა", "破戒", "Relapse") else tr(language, "Gescheitert", "ვერ მოხერხდა", "未达成", "Failed"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (!habit.isNegative) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(HabitYellow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(language, "Ausstehend", "მომლოდინე", "待完成", "Pending"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(HabitOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(language, "Pausiert / Skipped", "გამოტოვებული", "已暂停/跳过", "Paused / Skipped"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(HabitOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Pausiert / Skipped", "გამოტოვებული", "已暂停/跳过", "Paused / Skipped"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
