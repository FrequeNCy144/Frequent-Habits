package com.example.ui.screens.createhabit
import com.frequent.habits.R

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.AppTextField
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.theme.*

@Composable
fun HabitAppearanceCard(
    language: String,
    selectedColor: String,
    onColorSelect: (String) -> Unit,
    selectedIcon: String,
    onIconSelect: (String) -> Unit,
    iconSearchQuery: String,
    onIconSearchQueryChange: (String) -> Unit,
    activeColor: Color,
    filteredIcons: List<Pair<String, Int>>
) {
            // 3. CARD: Aussehen anpassen
            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_palette), contentDescription = null, tint = HabitOrange, modifier = Modifier.size(20.dp))
                        Text(
                            text = tr(language, "Aussehen anpassen", "გარეგნობის მორგება", "定制视觉外观", "Customize Visuals"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Icon selector
                    Text(
                        text = tr(language, "Icon auswählen", "აირჩიეთ ხატულა", "选择图标", "Select Icon"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    val activeColor = HabitIconMapping.getColor(selectedColor)
                    var selectedCategoryId by remember { mutableStateOf("all") }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(HabitIconMapping.categories) { cat ->
                            val isCatSelected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { selectedCategoryId = cat.id },
                                label = {
                                    Text(
                                        text = cat.getLocalizedName(language),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCatSelected) Color.White else TextSecondary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = activeColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = ProgressTrack,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isCatSelected,
                                    selectedBorderColor = activeColor,
                                    borderColor = AppBorder,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    val displayIcons = remember(selectedCategoryId, filteredIcons) {
                        if (selectedCategoryId == "all") {
                            filteredIcons
                        } else {
                            val cat = HabitIconMapping.categories.find { it.id == selectedCategoryId }
                            val keys = cat?.keys ?: emptyList()
                            filteredIcons.filter { keys.contains(it.first) }
                        }
                    }

                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                return available
                            }

                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity
                            ): Velocity {
                                return available
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(ProgressTrack, RoundedCornerShape(16.dp))
                            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                        ) {
                            items(displayIcons) { iconPair ->
                                val key = iconPair.first
                                val isSelected = selectedIcon == key
                                var showTooltip by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isSelected) activeColor.copy(alpha = 0.2f) else AppCard,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) activeColor else AppBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .pointerInput(key) {
                                            detectTapGestures(
                                                onTap = { onIconSelect(key) },
                                                onLongPress = { showTooltip = true }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HabitIconMapping.getIcon(key),
                                        contentDescription = key,
                                        tint = if (isSelected) activeColor else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    if (showTooltip) {
                                        val density = LocalDensity.current
                                        val offsetInPx = with(density) { -52.dp.roundToPx() }
                                        Popup(
                                            alignment = Alignment.TopCenter,
                                            offset = IntOffset(0, offsetInPx),
                                            onDismissRequest = { showTooltip = false }
                                        ) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = AppCard),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .border(1.dp, AppBorder, RoundedCornerShape(8.dp))
                                                    .widthIn(max = 200.dp)
                                            ) {
                                                Text(
                                                    text = HabitIconMapping.getIconLabel(key, language),
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = AppBorder, thickness = 1.dp)

                    // Color selector dots
                    Text(
                        text = tr(language, "Farbe auswählen", "აირჩიეთ ფერი", "选择颜色", "Select Color"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    ColorPaletteSelector(
                        selectedColorKey = selectedColor,
                        onColorSelected = { onColorSelect(it) },
                        language = language
                    )
                }
            }


}
