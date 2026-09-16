package com.example.ui.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextPrimary

// Theme presets for share cards
data class ShareThemePreset(
    val name: String,
    val bgColors: List<Color>,
    val accentColor: Color,
    val textColor: Color
)

val habitShareThemes = listOf(
    ShareThemePreset(
        name = "Violet Glow",
        bgColors = listOf(Color(0xFF2E1065), Color(0xFF0F172A)),
        accentColor = Color(0xFFA855F7),
        textColor = Color.White
    ),
    ShareThemePreset(
        name = "Emerald Mint",
        bgColors = listOf(Color(0xFF064E3B), Color(0xFF022C22)),
        accentColor = Color(0xFF10B981),
        textColor = Color.White
    ),
    ShareThemePreset(
        name = "Sunset Orange",
        bgColors = listOf(Color(0xFF7C2D12), Color(0xFF18181B)),
        accentColor = Color(0xFFF97316),
        textColor = Color.White
    ),
    ShareThemePreset(
        name = "Electric Cyber",
        bgColors = listOf(Color(0xFF1E1B4B), Color(0xFF311042)),
        accentColor = Color(0xFF06B6D4),
        textColor = Color.White
    )
)

val profileShareThemes = listOf(
    ShareThemePreset(
        name = "Midnight Cyber",
        bgColors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)),
        accentColor = Color(0xFFA855F7),
        textColor = Color.White
    ),
    ShareThemePreset(
        name = "Emerald Gold",
        bgColors = listOf(Color(0xFF064E3B), Color(0xFF14532D)),
        accentColor = Color(0xFFF59E0B),
        textColor = Color.White
    ),
    ShareThemePreset(
        name = "Sunset Royal",
        bgColors = listOf(Color(0xFF831843), Color(0xFF4C1D95)),
        accentColor = Color(0xFFF43F5E),
        textColor = Color.White
    ),
    ShareThemePreset(
        name = "Obsidian Dark",
        bgColors = listOf(Color(0xFF18181B), Color(0xFF09090B)),
        accentColor = Color(0xFF38BDF8),
        textColor = Color.White
    )
)

@Composable
fun MetricChip(icon: String, label: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DensityToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFA855F7)
            )
        )
    }
}
