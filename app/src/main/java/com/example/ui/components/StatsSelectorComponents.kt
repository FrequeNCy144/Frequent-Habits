package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.tr
import com.example.ui.theme.*

import com.example.LocalInfoCardsEnabled

@Composable
fun InfoIconButton(
    title: String,
    explanation: String,
    onClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable { onClick(title, explanation) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// Data model and helpers for Trends & Progression
data class TrendPoint(
    val label: String,
    val score: Float,
    val completions: Int
)

@Composable
fun TimeframeSelectorPills(
    selectedTimeframeIndex: Int,
    onTimeframeSelected: (Int) -> Unit,
    language: String,
    modifier: Modifier = Modifier,
    customLabels: List<String>? = null,
    badges: List<Int>? = null,
    accentColor: Color = PrimaryViolet
) {
    val labels = customLabels ?: when (language) {
        "de" -> listOf("Diese Woche", "Diesen Monat", "Dieses Jahr", "Gesamt")
        "ka" -> listOf("ამ კვირაში", "ამ თვეში", "ამ წელს", "სულ")
        else -> listOf("This Week", "This Month", "This Year", "Total")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProgressTrack)
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = selectedTimeframeIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onTimeframeSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val badgeCount = badges?.getOrNull(index) ?: 0
                    if (badgeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .background(if (isSelected) Color.White else PrimaryViolet, CircleShape)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) accentColor else Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
