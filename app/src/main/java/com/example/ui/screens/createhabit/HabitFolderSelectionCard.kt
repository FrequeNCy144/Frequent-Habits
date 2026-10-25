package com.example.ui.screens.createhabit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitFolder
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.AppTextField
import com.example.ui.screens.today.TodayFolderDialog
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun HabitFolderSelectionCard(
    allFolders: List<HabitFolder>,
    selectedFolderId: Int?,
    onFolderSelected: (Int?) -> Unit,
    language: String,
    accentColor: Color = PrimaryViolet,
    onCreateFolder: ((name: String, icon: String, color: String) -> Int)? = null
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var isDropdownOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showNewFolderDialog && onCreateFolder != null) {
        TodayFolderDialog(
            folder = null,
            language = language,
            accentColor = accentColor,
            onDismiss = { showNewFolderDialog = false },
            onSave = { name, icon, color ->
                val newId = onCreateFolder(name, icon, color)
                onFolderSelected(newId)
                showNewFolderDialog = false
            }
        )
    }

    val selectedFolder = remember(allFolders, selectedFolderId) {
        allFolders.find { it.id == selectedFolderId }
    }

    val filteredFolders = remember(allFolders, searchQuery) {
        if (searchQuery.isBlank()) {
            allFolders
        } else {
            allFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
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
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tr(language, "Ordner", "საქაღალდე", "文件夹", "Folder"),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Active Selected Folder Card
            if (selectedFolder != null) {
                val folderColor = HabitIconMapping.getColor(selectedFolder.color)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = folderColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, folderColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(folderColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HabitIconMapping.getIcon(selectedFolder.icon),
                                contentDescription = null,
                                tint = folderColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFolder.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(language, "Ausgewählter Ordner", "არჩეული საქაღალდე", "已选文件夹", "Selected Folder"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { onFolderSelected(null) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove folder",
                                tint = FailedRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Search & Add Folder Row (identical layout to Audio Timer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Dropdown trigger box
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { isDropdownOpen = true },
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
                                    text = selectedFolder?.name ?: tr(language, "Ordner suchen...", "საქაღალდის ძებნა...", "搜索文件夹...", "Search folder..."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedFolder != null) TextPrimary else TextSecondary,
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

                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = {
                            isDropdownOpen = false
                            searchQuery = ""
                        },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = AppCard,
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                    ) {
                        // Search inside dropdown if there are folders
                        if (allFolders.size > 4) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                AppTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholderText = tr(language, "Suchen...", "ძებნა...", "搜索...", "Search..."),
                                    singleLine = true,
                                    containerColor = ProgressTrack,
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                )
                            }
                            HorizontalDivider(color = AppBorder, thickness = 1.dp)
                        }

                        // Option: No folder
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = tr(language, "Kein Ordner", "საქაღალდის გარეშე", "无文件夹", "No Folder"),
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                onFolderSelected(null)
                                isDropdownOpen = false
                                searchQuery = ""
                            }
                        )

                        if (filteredFolders.isEmpty() && allFolders.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = tr(language, "Keine Treffer", "შედეგი არ მოიძებნა", "无匹配结果", "No matches"),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = { }
                            )
                        } else {
                            filteredFolders.forEach { folder ->
                                val folderColor = HabitIconMapping.getColor(folder.color)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = folder.name,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (folder.id == selectedFolderId) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = HabitIconMapping.getIcon(folder.icon),
                                            contentDescription = null,
                                            tint = folderColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        onFolderSelected(folder.id)
                                        isDropdownOpen = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }

                // Add Button (+) next to search bar
                if (onCreateFolder != null) {
                    Surface(
                        onClick = { showNewFolderDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = tr(language, "Neuer Ordner", "ახალი საქაღალდე", "新文件夹", "New Folder"),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
