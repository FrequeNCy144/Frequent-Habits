package com.example.ui.screens.today

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.example.data.HabitFolder
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.theme.*

@Composable
fun TodayFolderDialog(
    folder: HabitFolder?,
    language: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var folderName by remember { mutableStateOf(folder?.name ?: "") }
    var selectedColor by remember { mutableStateOf(folder?.color ?: "purple") }
    var selectedIcon by remember { mutableStateOf(folder?.icon ?: "folder") }
    var selectedCategoryId by remember { mutableStateOf("all") }

    val activeColor = remember(selectedColor) { HabitIconMapping.getColor(selectedColor) }

    fun getIconLabel(key: String, lang: String): String {
        return when (key) {
            "dumbbell" -> tr(lang, "Fitness / Sport", "ფიტნესი / სპორტი", "健身 / 运动", "Fitness / Sport")
            "run" -> tr(lang, "Laufen / Joggen", "სირბილი", "跑步", "Running")
            "book" -> tr(lang, "Lesen / Buch", "კითხვა / წიგნი", "阅读 / 书籍", "Reading / Books")
            "code" -> tr(lang, "Programmieren", "დაპროგრამება", "编程", "Coding")
            "water" -> tr(lang, "Wasser trinken", "წყლის დალევა", "喝水", "Drinking Water")
            "meditation" -> tr(lang, "Meditation / Achtsamkeit", "მედიტაცია", "冥想 / 正念", "Meditation / Mindfulness")
            "work" -> tr(lang, "Arbeit / Produktivität", "სამუშაო", "工作 / 生产力", "Work / Productivity")
            "bed" -> tr(lang, "Schlaf", "ძილი", "睡眠", "Sleep")
            "music" -> tr(lang, "Musik / Instrument", "მუსიკა", "音乐 / 乐器", "Music / Instruments")
            "palette" -> tr(lang, "Kreativität / Malen", "კრეატიულობა / ხატვა", "创意 / 绘画", "Creativity / Art")
            "school" -> tr(lang, "Lernen / Studium", "სწავლა", "学习 / 进修", "Studying / Education")
            "money" -> tr(lang, "Finanzen / Sparen", "ფინანსები", "财务 / 存钱", "Finance / Savings")
            "home" -> tr(lang, "Zuhause / Haushalt", "სახლი", "家庭", "Home / Household")
            "star" -> tr(lang, "Priorität / Wichtig", "ვარსკვლავი", "重要 / 优先", "Important / Star")
            "heart" -> tr(lang, "Gesundheit / Liebe", "ჯანმრთელობა", "健康 / 关爱", "Health / Care")
            "clean" -> tr(lang, "Hygiene / Ordnung", "ჰიგიენა", "清洁 / 整理", "Cleanliness / Hygiene")
            else -> key.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    val displayIcons = remember(selectedCategoryId) {
        if (selectedCategoryId == "all") {
            HabitIconMapping.iconList
        } else {
            val cat = HabitIconMapping.categories.find { it.id == selectedCategoryId }
            val keys = cat?.keys ?: emptyList()
            HabitIconMapping.iconList.filter { keys.contains(it.first) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .border(1.dp, AppBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (folder == null) {
                            tr(language, "Neuer Ordner", "ახალი საქაღალდე", "新文件夹", "New Folder")
                        } else {
                            tr(language, "Ordner bearbeiten", "საქაღალდის რედაქტირება", "编辑文件夹", "Edit Folder")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    // Preview of folder icon + color
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.15f))
                            .border(1.dp, activeColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIconMapping.getIcon(selectedIcon),
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(tr(language, "Name", "სახელი", "名称", "Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = activeColor,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = AppBorder,
                        focusedContainerColor = ProgressTrack,
                        unfocusedContainerColor = ProgressTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips for Icons
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
                                    text = if (language == "de") cat.nameDe else cat.nameEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCatSelected) Color.White else TextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = activeColor,
                                selectedLabelColor = Color.White,
                                containerColor = AppBg,
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

                // Icon Grid Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(AppBg, RoundedCornerShape(16.dp))
                        .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 44.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
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
                                            onTap = { selectedIcon = key },
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
                                    val offsetInPx = with(density) { -48.dp.roundToPx() }
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
                                                .widthIn(max = 180.dp)
                                        ) {
                                            Text(
                                                text = getIconLabel(key, language),
                                                color = TextPrimary,
                                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Color Selection (Presets + Custom Rainbow Picker)
                Text(
                    text = tr(language, "Farbe auswählen", "ფერის არჩევა", "选择颜色", "Select Color"),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                ColorPaletteSelector(
                    selectedColorKey = selectedColor,
                    onColorSelected = { selectedColor = it },
                    language = language
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Delete / Cancel / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (folder != null && onDelete != null) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (folder != null && onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = FailedRed)
                        ) {
                            Text(
                                text = tr(language, "Löschen", "წაშლა", "删除", "Delete", "Supprimer")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel", "Annuler"),
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                if (folderName.isNotBlank()) {
                                    onSave(folderName, selectedIcon, selectedColor)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tr(language, "Speichern", "შენახვა", "保存", "Save", "Enregistrer"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
