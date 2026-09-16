package com.example.ui.components.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.theme.*

@Composable
fun SavedColorSlotsRow(
    savedSlots: List<String>,
    currentHex: String,
    selectedSlotIndex: Int?,
    language: String,
    onSelectSlot: (Int, String) -> Unit,
    onSaveSlot: (Int, String) -> Unit,
    onDeleteSlot: (Int) -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
                    text = tr(language, "Gespeicherte Farben", "Saved Colors"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onShowInfo,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = tr(language, "Tippen: Laden • Halten: Speichern", "Tap: Load • Hold: Save"),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }

        // 6 Slots Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            savedSlots.forEachIndexed { index, slotHex ->
                val slotColor = if (slotHex.isNotBlank()) {
                    try {
                        Color(android.graphics.Color.parseColor(slotHex))
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val isSlotActive = selectedSlotIndex == index

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(slotColor ?: Color.White.copy(alpha = 0.05f))
                        .border(
                            width = if (isSlotActive) 2.5.dp else 1.dp,
                            color = if (isSlotActive) Color.White else if (slotColor != null) AppBorder else AppBorder.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (slotColor != null) {
                                        onSelectSlot(index, slotHex)
                                    } else {
                                        onSaveSlot(index, currentHex)
                                    }
                                },
                                onLongPress = {
                                    onSaveSlot(index, currentHex)
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
            val activeIndex = selectedSlotIndex
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
                                onSaveSlot(activeIndex, currentHex)
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
                                    onDeleteSlot(activeIndex)
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
}
