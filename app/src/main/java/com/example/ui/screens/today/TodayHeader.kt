package com.example.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.screens.profile.BoldPlusIcon
import com.example.ui.screens.profile.ZettelMitStiftIcon
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppCard
import com.example.ui.theme.TextPrimary
import com.frequent.habits.R

@Composable
fun TodayHeader(
    formattedDisplayDate: String,
    hasDailyNote: Boolean,
    accentColor: Color,
    language: String,
    onOpenDailyNote: () -> Unit,
    onTitleClick: () -> Unit,
    onAddHabit: () -> Unit,
    onAddFolder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .defaultMinSize(minHeight = 44.dp)
    ) {
        // Top Left: Daily Note Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { onOpenDailyNote() }
                .align(Alignment.CenterStart)
                .testTag("btn_daily_note"),
            contentAlignment = Alignment.Center
        ) {
            ZettelMitStiftIcon(
                modifier = Modifier.size(22.dp),
                color = accentColor
            )
            if (hasDailyNote) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }

        // Center Title: Date Header
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onTitleClick()
                },
            text = formattedDisplayDate,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Top Right: Add Habit Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { onAddHabit() }
                .testTag("btn_add_habit"),
            contentAlignment = Alignment.Center
        ) {
            BoldPlusIcon(
                modifier = Modifier.size(18.dp),
                color = accentColor
            )
        }
    }
}
