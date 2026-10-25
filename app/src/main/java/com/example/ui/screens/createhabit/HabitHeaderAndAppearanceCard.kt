package com.example.ui.screens.createhabit

import com.frequent.habits.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
fun HabitHeaderAndAppearanceCard(
    language: String,
    name: String,
    onNameChange: (String) -> Unit,
    nameError: Boolean,
    onNameErrorChange: (Boolean) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    nameFocusRequester: FocusRequester,
    selectedColor: String,
    onColorSelect: (String) -> Unit,
    selectedIcon: String,
    onIconSelect: (String) -> Unit,
    iconSearchQuery: String,
    onIconSearchQueryChange: (String) -> Unit,
    activeColor: Color,
    filteredIcons: List<Pair<String, Int>>
) {
    var isSelectorExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (nameError) FailedRed else AppBorder, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Interactive Icon Circle + Text Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left side: Large circular icon button to toggle visuals selection
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(activeColor.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, activeColor, CircleShape)
                            .clickable { isSelectorExpanded = !isSelectorExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIconMapping.getIcon(selectedIcon),
                            contentDescription = "Selected Icon",
                            tint = activeColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clickable { isSelectorExpanded = !isSelectorExpanded }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = tr(language, "Aussehen", "გარეგნობა", "视觉外观", "Visuals"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Right side: Habit Name & Optional Notes
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Habit Name
                    AppTextField(
                        value = name,
                        onValueChange = { 
                            onNameChange(it)
                            if (it.isNotBlank()) onNameErrorChange(false)
                        },
                        placeholderText = tr(language, "z.B. Meditieren, Laufen...", "მაგ., მედიტაცია, სირბილი...", "例如：冥想、跑步...", "e.g., Meditate, Running..."),
                        isError = nameError,
                        supportingText = {
                            if (nameError) {
                                Text(
                                    text = tr(language, "Bitte gib einen Namen ein", "გთხოვთ შეიყვანოთ სახელი", "请输入名称", "Please enter a name"),
                                    color = FailedRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        modifier = Modifier.focusRequester(nameFocusRequester),
                        testTag = "habit_name_input",
                        singleLine = true
                    )

                    // Optional notes / description
                    AppTextField(
                        value = description,
                        onValueChange = { onDescriptionChange(it) },
                        placeholderText = tr(language, "Notizen oder Regeln (optional)...", "აღწერა (სურვილისამებრ)...", "备忘或规则（可选）...", "Notes or rules (optional)..."),
                        testTag = "habit_description_input",
                        singleLine = false,
                        minLines = 1,
                        maxLines = 3
                    )
                }
            }

            // Expanded Area: Full custom icon grid & color palette selector
            AnimatedVisibility(visible = isSelectorExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = AppBorder, thickness = 1.dp)

                    // Colors Palette Selector
                    Text(
                        text = tr(language, "Farbe auswählen", "აირჩიეთ ფერი", "选择颜色", "Select Color"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    ColorPaletteSelector(
                        selectedColorKey = selectedColor,
                        onColorSelected = onColorSelect,
                        language = language
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Icons Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(language, "Icon auswählen", "აირჩიეთ ხატულა", "选择图标", "Select Icon"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Search input
                    AppTextField(
                        value = iconSearchQuery,
                        onValueChange = onIconSearchQueryChange,
                        placeholderText = tr(language, "Icon suchen...", "ძებნა...", "搜索图标...", "Search icon..."),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    var selectedCategoryId by remember { mutableStateOf("all") }

                    // Category chips
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
                            .height(180.dp)
                            .background(ProgressTrack, RoundedCornerShape(16.dp))
                            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 44.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                        .size(44.dp)
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
                                        modifier = Modifier.size(22.dp)
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
                }
            }
        }
    }
}
