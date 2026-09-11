package com.example.ui.screens.stats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarCellState
import com.example.tr
import com.example.ui.theme.*

@Composable
fun CalendarDayCell(
    dayNum: String,
    status: String,
    habitColor: Color,
    modifier: Modifier = Modifier,
    onCellClick: (() -> Unit)? = null
) {
    val bgColor = when (status) {
        "SUCCESS" -> SuccessGreen
        "FAILED" -> ErrorRed
        "PENDING" -> Color(0xFFFACC15)
        "PAUSED" -> Color(0xFFF97316)
        else -> Color.White.copy(alpha = 0.03f) // Completely neutral dark grey background without any purple/lila tint
    }
    val textColor = when (status) {
        "SUCCESS", "FAILED", "PENDING", "PAUSED" -> AppBg
        else -> if (status == "INACTIVE") TextSecondary.copy(alpha = 0.4f) else TextPrimary
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (onCellClick != null) Modifier.clickable { onCellClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNum,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CalendarMonthGrid(
    gridRows: List<List<CalendarCellState?>>,
    monthName: String,
    habitColor: Color,
    language: String,
    canPrevMonth: Boolean,
    canNextMonth: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: ((String) -> Unit)? = null
) {
    Column {
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
                                onCellClick = if (onDayClick != null && cell.id.isNotBlank()) { { onDayClick(cell.id) } } else null,
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

        // Legende (Erreicht, Gescheitert, Ausstehend, Pausiert) - Screen-resilient 2x2 centered grid
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                    Text(text = tr(language, "Erreicht", "დასრულებული", "已完成", "Completed"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                    Text(text = tr(language, "Gescheitert", "ვერ მოხერხდა", "未达成", "Failed"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
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
                    Text(text = tr(language, "Ausstehend", "მომლოდინე", "待完成", "Pending"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                    Text(text = tr(language, "Skipped", "გამოტოვებული", "已跳过", "Skipped"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}
