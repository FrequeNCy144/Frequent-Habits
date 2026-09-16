package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryViolet

@Composable
fun SimpleDatePickerView(initialTimeMs: Long, onDateSelected: (Long) -> Unit) {
    val initialDate = remember(initialTimeMs) {
        java.time.Instant.ofEpochMilli(initialTimeMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    }
    var currentMonth by remember { mutableStateOf(initialDate.monthValue) } // 1..12
    var currentYear by remember { mutableStateOf(initialDate.year) }

    val monthNames = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val yearMonth = remember(currentMonth, currentYear) {
        java.time.YearMonth.of(currentYear, currentMonth)
    }
    val firstDayOfMonth = remember(yearMonth) {
        yearMonth.atDay(1)
    }
    val offset = remember(firstDayOfMonth) {
        (firstDayOfMonth.dayOfWeek.value - 1) // 0 is Monday, ..., 6 is Sunday
    }
    val maxDays = remember(yearMonth) {
        yearMonth.lengthOfMonth()
    }

    val gridItems = remember(offset, maxDays) {
        val list = mutableListOf<Int?>()
        for (i in 0 until offset) {
            list.add(null)
        }
        for (i in 1..maxDays) {
            list.add(i)
        }
        list
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth == 1) {
                        currentMonth = 12
                        currentYear--
                    } else {
                        currentMonth--
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.onSurface)
            }

            Text(
                text = "${monthNames[currentMonth]} $currentYear",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    if (currentMonth == 12) {
                        currentMonth = 1
                        currentYear++
                    } else {
                        currentMonth++
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val rowItems = remember(gridItems) { gridItems.chunked(7) }
        rowItems.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = if (c < week.size) week[c] else null
                    if (day != null) {
                        val isSelected = initialDate.dayOfMonth == day &&
                                initialDate.monthValue == currentMonth &&
                                initialDate.year == currentYear

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryViolet else Color.Transparent)
                                .clickable {
                                    val localDate = java.time.LocalDate.of(currentYear, currentMonth, day)
                                    val epochMs = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    onDateSelected(epochMs)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
