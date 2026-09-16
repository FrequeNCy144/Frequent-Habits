package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StandardSheetDragHandle(color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) {
    Box(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 12.dp)
            .width(32.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}
