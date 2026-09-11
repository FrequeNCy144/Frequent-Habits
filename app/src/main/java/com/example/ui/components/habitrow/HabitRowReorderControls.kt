package com.example.ui.components.habitrow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryViolet

@Composable
fun HabitRowReorderControls(
    habitId: Int,
    onMoveUp: ((Int) -> Unit)?,
    onMoveDown: ((Int) -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // Move Up Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryViolet.copy(alpha = 0.12f))
                .border(1.dp, PrimaryViolet.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .clickable { onMoveUp?.invoke(habitId) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Move Up",
                tint = PrimaryViolet,
                modifier = Modifier.size(22.dp)
            )
        }

        // Move Down Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryViolet.copy(alpha = 0.12f))
                .border(1.dp, PrimaryViolet.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .clickable { onMoveDown?.invoke(habitId) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Move Down",
                tint = PrimaryViolet,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

