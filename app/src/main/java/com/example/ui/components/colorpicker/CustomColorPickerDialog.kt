package com.example.ui.components.colorpicker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

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
                // Intuitive Color Ring Picker
                ColorRingPicker(
                    hue = currentHue,
                    onHueChanged = { currentHue = it },
                    centerColor = currentColor,
                    hexText = currentHex,
                    modifier = Modifier.size(180.dp)
                )

                // 1. Saturation Slider
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

                // 2. Brightness / Value Slider
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

                // 3. Opacity Slider
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
                SavedColorSlotsRow(
                    savedSlots = savedSlots,
                    currentHex = currentHex,
                    selectedSlotIndex = selectedSlotIndex,
                    language = language,
                    onSelectSlot = { index, hex ->
                        selectedSlotIndex = index
                        try {
                            val c = Color(android.graphics.Color.parseColor(hex))
                            val arr = FloatArray(3)
                            android.graphics.Color.colorToHSV(c.toArgb(), arr)
                            currentHue = if (arr[0].isNaN()) 0f else arr[0]
                            currentSat = arr[1]
                            currentVal = arr[2]
                            currentAlpha = c.alpha
                        } catch (e: Exception) {}
                    },
                    onSaveSlot = { index, hex ->
                        saveSlotColor(index, hex)
                        selectedSlotIndex = index
                    },
                    onDeleteSlot = { index ->
                        saveSlotColor(index, "")
                        selectedSlotIndex = null
                    },
                    onShowInfo = { showSlotInfoDialog = true }
                )

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
                    labelText = "HEX-Code (#RRGGBB oder #AARRGGBB)",
                    placeholderText = "#8B5CF6",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentHex)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = tr(language, "Farbe wählen", "Select Color"),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(language, "Abbrechen", "Cancel"), color = TextSecondary)
            }
        },
        containerColor = AppCard,
        shape = RoundedCornerShape(20.dp)
    )

    if (showSlotInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSlotInfoDialog = false },
            title = {
                Text(
                    text = tr(language, "Farb-Slots Hilfe", "Color Slots Help"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(language, "• Speichern: Tippe auf einen leeren Slot (+), um die aktuelle Farbe zu sichern.", "• Save: Tap an empty slot (+) to store the current color."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(language, "• Verwenden: Tippe auf einen belegten Slot, um die Farbe in den Picker zu laden.", "• Use: Tap an occupied slot to load its color into the picker."),
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
