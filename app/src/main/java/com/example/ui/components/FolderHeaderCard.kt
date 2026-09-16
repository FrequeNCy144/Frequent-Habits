package com.example.ui.components
import com.frequent.habits.R

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.HabitFolder
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.theme.*

@Composable
fun FolderHeaderCard(
    folder: HabitFolder,
    containedCount: Int,
    completedCount: Int,
    progressPercent: Float,
    language: String,
    onToggleCollapse: () -> Unit,
    onToggleFolderCompletion: () -> Unit,
    onMarkAllCompleted: () -> Unit,
    onMarkAllFailed: () -> Unit,
    onResetFolder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isReorderMode: Boolean
) {
    val themeColor = HabitIconMapping.getColor(folder.color)
    val isCollapsed = folder.isCollapsed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .clickable { onToggleCollapse() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon & Name Left side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isReorderMode && containedCount > 0) {
                        val isAllDone = completedCount == containedCount
                        val isPartiallyDone = completedCount > 0 && !isAllDone

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isAllDone -> themeColor
                                        isPartiallyDone -> themeColor.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isAllDone || isPartiallyDone) themeColor else TextSecondary.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { onToggleFolderCompletion() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAllDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Complete Folder",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isPartiallyDone) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(themeColor)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIconMapping.getIcon(folder.icon),
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (containedCount == 1) {
                                tr(language, "1 Gewohnheit", "1 ჩვევა", "1 个习惯", "1 Habit")
                            } else {
                                tr(language, "$containedCount Gewohnheiten", "$containedCount ჩვევა", "$containedCount 个习惯", "$containedCount Habits")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Control / Action Right side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isReorderMode) {
                        IconButton(onClick = onMoveUp) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = TextSecondary
                            )
                        }
                        IconButton(onClick = onMoveDown) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = TextSecondary
                            )
                        }
                    } else {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .background(AppCard)
                                    .border(1.dp, AppBorder, RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(tr(language, "Alle erledigt markieren", "ყველას შესრულებულად მონიშვნა", "全部标记为已完成", "Mark all completed"), color = TextPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onMarkAllCompleted()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check_circle),
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tr(language, "Alle als fehlgeschlagen markieren", "ყველას ჩაშლილად მონიშვნა", "全部标记为失败", "Mark all failed"), color = TextPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onMarkAllFailed()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tr(language, "Zurücksetzen", "ჩამოყრა", "重置", "Reset all"), color = TextPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onResetFolder()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                HorizontalDivider(color = AppBorder)
                                DropdownMenuItem(
                                    text = { Text(tr(language, "Bearbeiten", "რედაქტირება", "编辑", "Edit"), color = TextPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tr(language, "Löschen", "წაშლა", "删除", "Delete"), color = TextSecondary) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        IconButton(onClick = onToggleCollapse) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Progress Bar / Status Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = progressPercent,
                    color = themeColor,
                    trackColor = themeColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$completedCount/$containedCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
            }
        }
    }
}
