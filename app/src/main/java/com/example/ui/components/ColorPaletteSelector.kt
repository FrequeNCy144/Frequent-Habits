package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ui.HabitIconMapping
import com.example.ui.components.colorpicker.ColorCircleItem
import com.example.ui.components.colorpicker.CustomColorPickerDialog
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun ColorPaletteSelector(
    selectedColorKey: String,
    onColorSelected: (String) -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    val presetColors = remember {
        listOf(
            "blue" to HabitBlue,
            "purple" to HabitPurple,
            "cyan" to HabitCyan,
            "green" to HabitGreen,
            "yellow" to HabitYellow,
            "orange" to HabitOrange,
            "red" to HabitRed,
            "pink" to HabitPink,
            "teal" to HabitTeal,
            "rose" to HabitRose,
            "indigo" to HabitIndigo
        )
    }

    val row1 = remember { presetColors.take(6) } // 6 items
    val row2 = remember { presetColors.drop(6) } // 5 items + 1 custom item

    val isCustomSelected = remember(selectedColorKey) {
        val norm = selectedColorKey.lowercase()
        selectedColorKey.startsWith("#") || selectedColorKey.startsWith("0x") ||
            (norm != "purple" && norm != "electric_blue" && norm != "emerald_green" &&
             norm != "sunset_orange" && norm != "crimson_red" && norm != "neon_teal" &&
             norm != "rose_pink" && presetColors.none { it.first.equals(norm, ignoreCase = true) })
    }

    val currentCustomColor = remember(selectedColorKey) {
        HabitIconMapping.getColor(selectedColorKey)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: 6 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            row1.forEach { (key, colorObj) ->
                val isSelected = !isCustomSelected && (
                    selectedColorKey.equals(key, ignoreCase = true) ||
                    (key == "purple" && (selectedColorKey.equals("PURPLE", ignoreCase = true) || selectedColorKey.isBlank())) ||
                    (key == "blue" && selectedColorKey.equals("ELECTRIC_BLUE", ignoreCase = true)) ||
                    (key == "green" && selectedColorKey.equals("EMERALD_GREEN", ignoreCase = true)) ||
                    (key == "orange" && selectedColorKey.equals("SUNSET_ORANGE", ignoreCase = true)) ||
                    (key == "red" && selectedColorKey.equals("CRIMSON_RED", ignoreCase = true)) ||
                    (key == "teal" && selectedColorKey.equals("NEON_TEAL", ignoreCase = true)) ||
                    (key == "pink" && selectedColorKey.equals("ROSE_PINK", ignoreCase = true))
                )

                ColorCircleItem(
                    colorObj = colorObj,
                    isSelected = isSelected,
                    onClick = { onColorSelected(key) }
                )
            }
        }

        // Row 2: 5 colors + Custom Rainbow Circle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            row2.forEach { (key, colorObj) ->
                val isSelected = !isCustomSelected && (
                    selectedColorKey.equals(key, ignoreCase = true) ||
                    (key == "red" && selectedColorKey.equals("CRIMSON_RED", ignoreCase = true)) ||
                    (key == "pink" && selectedColorKey.equals("ROSE_PINK", ignoreCase = true)) ||
                    (key == "teal" && selectedColorKey.equals("NEON_TEAL", ignoreCase = true))
                )

                ColorCircleItem(
                    colorObj = colorObj,
                    isSelected = isSelected,
                    onClick = { onColorSelected(key) }
                )
            }

            // Item 12: Custom Rainbow Color Picker Circle
            val rainbowBrush = remember {
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFEF4444),
                        Color(0xFFF59E0B),
                        Color(0xFF10B981),
                        Color(0xFF06B6D4),
                        Color(0xFF3B82F6),
                        Color(0xFF8B5CF6),
                        Color(0xFFEC4899),
                        Color(0xFFEF4444)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(rainbowBrush)
                    .border(
                        width = if (isCustomSelected) 2.5.dp else 1.dp,
                        color = if (isCustomSelected) Color.White else AppBorder,
                        shape = CircleShape
                    )
                    .clickable { showCustomPicker = true },
                contentAlignment = Alignment.Center
            ) {
                if (isCustomSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(currentCustomColor, CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Custom Selected",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(AppBg.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_palette),
                            contentDescription = "Custom Color Picker",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = currentCustomColor,
            language = language,
            onDismiss = { showCustomPicker = false },
            onColorSelected = onColorSelected
        )
    }
}
