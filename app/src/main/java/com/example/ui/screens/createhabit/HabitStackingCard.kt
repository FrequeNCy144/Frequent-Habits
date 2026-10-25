package com.example.ui.screens.createhabit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.AppTextField
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun HabitStackingCard(
    language: String,
    selectedStackedHabit: Habit?,
    onSelectStackedHabit: (Habit?) -> Unit,
    activeHabits: List<Habit>,
    allFolders: List<HabitFolder> = emptyList(),
    accentColor: Color = PrimaryViolet,
    onShowInfoDialog: (title: String, explanation: String) -> Unit = { _, _ -> }
) {
    var showSelectDialog by remember { mutableStateOf(false) }

    // Dialog when clicking the selector
    if (showSelectDialog) {
        HabitStackingSelectionDialog(
            language = language,
            activeHabits = activeHabits,
            allFolders = allFolders,
            selectedStackedHabit = selectedStackedHabit,
            accentColor = accentColor,
            onSelectStackedHabit = { habit ->
                onSelectStackedHabit(habit)
                showSelectDialog = false
            },
            onDismiss = { showSelectDialog = false }
        )
    }

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
            // Header Row with Icon, Title & Info Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(language, "Habit Stacking", "ჩვევების დაწყობა", "习惯叠加", "Habit Stacking"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                language,
                                "An eine bestehende Routine knüpfen",
                                "მიაბით არსებულ რუტინას",
                                "依附于已有日常习惯",
                                "Anchor to an existing routine"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                InfoIconButton(
                    title = tr(language, "Habit Stacking (Wissenschaftlich fundiert)", "ჩვევების დაწყობა", "习惯叠加（科学验证）", "Habit Stacking (Scientifically Proven)"),
                    explanation = tr(
                        language,
                        "🔬 Wissenschaftlich belegt (Dr. BJ Fogg, Stanford & James Clear):\n\nDein Gehirn besitzt für bereits feste Routinen (wie Zähneputzen, Kaffee kochen oder Schuhe anziehen) extrem stabile neuronale Bahnen.\n\nBeim Habit Stacking hängst du eine neue Gewohnheit direkt an eine bestehende Anker-Gewohnheit an:\n➜ „NACHDEM ich [Anker-Gewohnheit] erledigt habe, werde ich sofort [neue Gewohnheit] ausführen.“\n\n💡 Vorteil:\nKein Vertrauen auf Willenskraft nötig – der Anker erinnert dich ganz automatisch. Studien zeigen, dass neue Routinen dadurch bis zu 2-3x schneller automatisiert werden!",
                        "🔬 Scientifically Proven (Dr. BJ Fogg, Stanford & James Clear):\n\nYour brain already has highly reinforced neural pathways for established routines.\n\nBy stacking a new behavior onto an existing anchor habit ('After I [Anchor], I will immediately [New Habit]'), you leverage automatic triggers. Habits automate up to 2-3x faster!"
                    ),
                    onClick = { t, e -> onShowInfoDialog(t, e) }
                )
            }

            // If an anchor habit is selected: Show informative anchor card
            if (selectedStackedHabit != null) {
                val anchorColor = HabitIconMapping.getColor(selectedStackedHabit.color)
                val anchorFolder = allFolders.find { it.id == selectedStackedHabit.folderId }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = anchorColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, anchorColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(anchorColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HabitIconMapping.getIcon(selectedStackedHabit.icon),
                                    contentDescription = null,
                                    tint = anchorColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tr(language, "Anker-Gewohnheit", "ანკერი", "锚点习惯", "Anchor Habit"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = anchorColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (anchorFolder != null) {
                                        Text(
                                            text = "• ${anchorFolder.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = selectedStackedHabit.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { onSelectStackedHabit(null) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove stack",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Psychological commitment phrase
                Surface(
                    color = ProgressTrack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "„Nachdem ich „${selectedStackedHabit.name}“ erledigt habe, werde ich sofort diese neue Gewohnheit ausführen.“",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Selector Button that opens the organized Dialog
            Surface(
                onClick = { showSelectDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = ProgressTrack,
                border = BorderStroke(1.dp, AppBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = selectedStackedHabit?.let {
                                tr(language, "Anker wechseln: ${it.name}", "ანკერის შეცვლა: ${it.name}", "更改锚点: ${it.name}", "Change anchor: ${it.name}")
                            } ?: tr(
                                language,
                                "Bestehende Gewohnheit als Anker wählen...",
                                "აირჩიეთ ანკერი ჩვევა...",
                                "选择现有习惯作为锚点...",
                                "Choose existing habit as anchor..."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedStackedHabit != null) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitStackingSelectionDialog(
    language: String,
    activeHabits: List<Habit>,
    allFolders: List<HabitFolder>,
    selectedStackedHabit: Habit?,
    accentColor: Color,
    onSelectStackedHabit: (Habit?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Alphabetically sorted & filtered habits
    val filteredHabits = remember(activeHabits, searchQuery) {
        val sorted = activeHabits.sortedBy { it.name.lowercase() }
        if (searchQuery.isBlank()) {
            sorted
        } else {
            sorted.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppCard,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Anker-Gewohnheit wählen", "ანკერის არჩევა", "选择锚点习惯", "Choose Anchor Habit"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr(
                                language,
                                "Alphabetisch geordnet (${activeHabits.size} verfügbar)",
                                "ანბანის მიხედვით (${activeHabits.size})",
                                "按字母排序（共 ${activeHabits.size} 个）",
                                "Sorted alphabetically (${activeHabits.size} available)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholderText = tr(language, "Gewohnheit suchen...", "ძებნა...", "搜索习惯...", "Search habit..."),
                    singleLine = true,
                    containerColor = ProgressTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option: Standalone / No Anchor
                Surface(
                    onClick = { onSelectStackedHabit(null) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedStackedHabit == null) accentColor.copy(alpha = 0.12f) else ProgressTrack,
                    border = BorderStroke(1.dp, if (selectedStackedHabit == null) accentColor.copy(alpha = 0.4f) else AppBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TextSecondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = tr(
                                    language,
                                    "Keine Anker-Gewohnheit (Eigenständig)",
                                    "ანკერის გარეშე (დამოუკიდებელი)",
                                    "无锚点习惯（独立）",
                                    "No Anchor Habit (Standalone)"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedStackedHabit == null) accentColor else TextPrimary,
                                fontWeight = if (selectedStackedHabit == null) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        if (selectedStackedHabit == null) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AppBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // List of habits with 1px divider between each
                if (filteredHabits.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr(language, "Keine Gewohnheiten gefunden", "ვერ მოიძებნა", "未找到习惯", "No habits found"),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(filteredHabits, key = { _, habit -> habit.id }) { index, habit ->
                            val isSelected = habit.id == selectedStackedHabit?.id
                            val hColor = HabitIconMapping.getColor(habit.color)
                            val folder = allFolders.find { it.id == habit.folderId }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { onSelectStackedHabit(habit) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Habit Icon + Habit Name (+ Folder subtitle if present)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(hColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = HabitIconMapping.getIcon(habit.icon),
                                                contentDescription = null,
                                                tint = hColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = habit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) accentColor else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (folder != null) {
                                                Text(
                                                    text = folder.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Right: Folder badge / icon on the right side + checkmark if selected
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (folder != null) {
                                            val fColor = HabitIconMapping.getColor(folder.color)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = fColor.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, fColor.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = HabitIconMapping.getIcon(folder.icon),
                                                        contentDescription = null,
                                                        tint = fColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = folder.name,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        color = fColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // 1px separator line between rows (as requested by user)
                                if (index < filteredHabits.lastIndex) {
                                    HorizontalDivider(
                                        color = AppBorder.copy(alpha = 0.65f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
