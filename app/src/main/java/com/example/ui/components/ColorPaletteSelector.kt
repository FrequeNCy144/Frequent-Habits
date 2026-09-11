package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
                            imageVector = Icons.Default.Palette,
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
            initialColor = if (isCustomSelected) currentCustomColor else PrimaryViolet,
            language = language,
            onDismiss = { showCustomPicker = false },
            onColorSelected = { hex ->
                onColorSelected(hex)
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun ColorCircleItem(
    colorObj: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(colorObj.copy(alpha = 0.15f), CircleShape)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) colorObj else colorObj.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 22.dp else 26.dp)
                .background(colorObj, CircleShape)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    language: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val hsv = remember(initialColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), array)
        array
    }

    var currentHue by remember { mutableFloatStateOf(if (hsv[0].isNaN()) 0f else hsv[0]) }
    var currentSat by remember { mutableFloatStateOf(if (hsv[1] < 0.02f) 0.85f else hsv[1]) }
    var currentVal by remember { mutableFloatStateOf(if (hsv[2] < 0.02f) 0.95f else hsv[2]) }
    var currentAlpha by remember { mutableFloatStateOf(initialColor.alpha) }

    val baseColor = remember(currentHue, currentSat, currentVal) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(currentHue, currentSat, currentVal)))
    }
    val currentColor = remember(baseColor, currentAlpha) {
        baseColor.copy(alpha = currentAlpha)
    }

    val currentHex = remember(currentColor, currentAlpha) {
        if (currentAlpha >= 0.995f) {
            String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))
        } else {
            String.format("#%08X", currentColor.toArgb())
        }
    }

    var hexText by remember(currentHex) { mutableStateOf(currentHex) }
    var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showSlotInfoDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("habit_prefs", android.content.Context.MODE_PRIVATE) }

    var savedSlots by remember {
        val savedStr = sharedPrefs.getString("user_saved_color_slots", "") ?: ""
        val list = savedStr.split(";").filter { it.isNotBlank() }
        val initial = List(6) { i -> if (i < list.size) list[i] else "" }
        mutableStateOf(initial)
    }

    fun saveSlotColor(index: Int, hex: String) {
        val newList = savedSlots.toMutableList()
        newList[index] = hex
        savedSlots = newList
        sharedPrefs.edit().putString("user_saved_color_slots", newList.joinToString(";")).apply()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(language, "Individuelle Farbe", "Custom Color"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Intuitive Color Ring Picker with center preview & hex label
                ColorRingPicker(
                    hue = currentHue,
                    onHueChanged = { currentHue = it },
                    centerColor = currentColor,
                    hexText = currentHex,
                    modifier = Modifier.size(180.dp)
                )

                // 1. Saturation Slider (Sättigung: 0% bis 100%)
                val satGradient = remember(currentHue, currentVal) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 0f, currentVal))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 1f, currentVal)))
                        )
                    )
                }
                ColorSlider(
                    label = tr(language, "Sättigung", "Saturation"),
                    value = currentSat,
                    onValueChange = { currentSat = it },
                    valueRange = 0f..1f,
                    gradient = satGradient,
                    valueText = "${(currentSat * 100).toInt()}%"
                )

                // 2. Brightness / Value Slider (Helligkeit: Dunkel bis Hell)
                val valGradient = remember(currentHue, currentSat) {
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(currentHue, currentSat, 1f)))
                        )
                    )
                }
                ColorSlider(
                    label = tr(language, "Helligkeit", "Brightness"),
                    value = currentVal,
                    onValueChange = { currentVal = it },
                    valueRange = 0f..1f,
                    gradient = valGradient,
                    valueText = "${(currentVal * 100).toInt()}%"
                )

                // 3. Opacity / Transparency Slider (Deckkraft / Transparenz: 10% bis 100%)
                val alphaGradient = remember(baseColor) {
                    Brush.horizontalGradient(
                        listOf(
                            baseColor.copy(alpha = 0.1f),
                            baseColor.copy(alpha = 1f)
                        )
                    )
                }
                ColorSlider(
                    label = tr(language, "Deckkraft", "Opacity"),
                    value = currentAlpha,
                    onValueChange = { currentAlpha = it },
                    valueRange = 0.1f..1f,
                    gradient = alphaGradient,
                    valueText = "${(currentAlpha * 100).toInt()}%"
                )

                // Saved Slots Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tr(language, "Gemerkte Farben", "Saved Slots"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showSlotInfoDialog = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = tr(language, "Tippen: Laden • Halten: Speichern", "Tap: Load • Hold: Save"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        savedSlots.forEachIndexed { index, slotHex ->
                            val hasColor = slotHex.isNotBlank()
                            val slotColor = remember(slotHex) {
                                if (hasColor) {
                                    try { Color(android.graphics.Color.parseColor(slotHex)) } catch (e: Exception) { null }
                                } else null
                            }
                            val isSlotActive = selectedSlotIndex == index

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(slotColor ?: AppBg)
                                    .border(
                                        width = if (isSlotActive) 2.5.dp else 1.dp,
                                        color = if (isSlotActive) Color.White else AppBorder,
                                        shape = CircleShape
                                    )
                                    .pointerInput(slotHex, currentHex) {
                                        detectTapGestures(
                                            onTap = {
                                                if (slotColor != null) {
                                                    val arr = FloatArray(3)
                                                    android.graphics.Color.colorToHSV(slotColor.toArgb(), arr)
                                                    currentHue = if (arr[0].isNaN()) 0f else arr[0]
                                                    currentSat = arr[1]
                                                    currentVal = arr[2]
                                                    currentAlpha = slotColor.alpha
                                                    selectedSlotIndex = index
                                                } else {
                                                    saveSlotColor(index, currentHex)
                                                    selectedSlotIndex = index
                                                }
                                            },
                                            onLongPress = {
                                                saveSlotColor(index, currentHex)
                                                selectedSlotIndex = index
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (slotColor == null) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Save Color Slot",
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isSlotActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons for selected slot (Replace & Delete)
                    if (selectedSlotIndex != null) {
                        val activeIndex = selectedSlotIndex!!
                        val activeSlotHex = savedSlots.getOrNull(activeIndex) ?: ""
                        val activeHasColor = activeSlotHex.isNotBlank()

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppBg.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Slot ${activeIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Replace / Speichern Button
                                    FilledTonalButton(
                                        onClick = {
                                            saveSlotColor(activeIndex, currentHex)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = PrimaryViolet.copy(alpha = 0.2f),
                                            contentColor = PrimaryViolet
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (activeHasColor) tr(language, "Ersetzen", "Replace") else tr(language, "Speichern", "Save"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Delete / Löschen Button (only if slot has a color)
                                    if (activeHasColor) {
                                        OutlinedButton(
                                            onClick = {
                                                saveSlotColor(activeIndex, "")
                                                selectedSlotIndex = null
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = tr(language, "Löschen", "Delete"),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // HEX Input field
                AppTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val clean = if (input.startsWith("#")) input else "#$input"
                        if (clean.length == 7) {
                            try {
                                val c = Color(android.graphics.Color.parseColor(clean))
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(c.toArgb(), arr)
                                currentHue = if (arr[0].isNaN()) 0f else arr[0]
                                currentSat = arr[1]
                                currentVal = arr[2]
                                currentAlpha = 1f
                            } catch (e: Exception) {}
                        } else if (clean.length == 9) {
                            try {
                                val c = Color(android.graphics.Color.parseColor(clean))
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(c.toArgb(), arr)
                                currentHue = if (arr[0].isNaN()) 0f else arr[0]
                                currentSat = arr[1]
                                currentVal = arr[2]
                                currentAlpha = c.alpha
                            } catch (e: Exception) {}
                        }
                    },
                    labelText = "HEX Code (#RRGGBB / #AARRGGBB)",
                    singleLine = true,
                    testTag = "hex_color_input"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentHex) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = tr(language, "Übernehmen", "Apply"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = null,
        containerColor = AppCard,
        shape = RoundedCornerShape(20.dp)
    )

    if (showSlotInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSlotInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryViolet)
                    Text(
                        text = tr(language, "Farben speichern & verwalten", "Manage Saved Colors"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(language, "• Tippen: Gespeicherte Farbe in den Regler laden.", "• Tap: Load the saved color into the picker."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(language, "• Ersetzen: Tippe auf einen Slot und nutze den 'Ersetzen'-Button (oder halte den Slot gedrückt), um ihn mit der aktuellen Farbe zu überschreiben.", "• Replace: Tap a slot and use the 'Replace' button (or long-press the slot) to overwrite it with the current color."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(language, "• Löschen: Wähle einen belegten Slot aus und tippe auf 'Löschen', um ihn wieder freizugeben.", "• Delete: Select an occupied slot and tap 'Delete' to clear it."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSlotInfoDialog = false }) {
                    Text(tr(language, "Verstanden", "Got it"), color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AppCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    gradient: Brush,
    valueText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(gradient)
                    .border(1.dp, AppBorder, RoundedCornerShape(7.dp))
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ColorRingPicker(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    centerColor: Color,
    hexText: String,
    modifier: Modifier = Modifier
) {
    var sizePx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Center Preview & Hex Text inside the ring
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(centerColor)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hexText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = sizePx / 2f
                        val dx = offset.x - center
                        val dy = offset.y - center
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        onHueChanged(angle)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = sizePx / 2f
                        val dx = change.position.x - center
                        val dy = change.position.y - center
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        onHueChanged(angle)
                    }
                }
                .onSizeChanged { sizePx = it.width.toFloat() }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 24.dp.toPx()
            val radius = (size.width - strokeWidth) / 2f

            // 1. Draw Rainbow Sweep Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFEF4444), // Red 0 deg
                        Color(0xFFF59E0B), // Yellow 60 deg
                        Color(0xFF10B981), // Green 120 deg
                        Color(0xFF06B6D4), // Cyan 180 deg
                        Color(0xFF3B82F6), // Blue 240 deg
                        Color(0xFFEC4899), // Magenta 300 deg
                        Color(0xFFEF4444)  // Red 360 deg
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Draw Selector Thumb
            val angleRad = Math.toRadians(hue.toDouble())
            val thumbX = center.x + radius * cos(angleRad).toFloat()
            val thumbY = center.y + radius * sin(angleRad).toFloat()
            val thumbCenter = Offset(thumbX, thumbY)

            // Outer drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 15.dp.toPx(),
                center = thumbCenter
            )
            // White border ring
            drawCircle(
                color = Color.White,
                radius = 13.dp.toPx(),
                center = thumbCenter
            )
            // Inner color
            val thumbHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            drawCircle(
                color = thumbHueColor,
                radius = 9.dp.toPx(),
                center = thumbCenter
            )
        }
    }
}
