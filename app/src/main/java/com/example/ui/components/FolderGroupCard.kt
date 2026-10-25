package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.data.HabitUiItem
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.screens.today.TodayHabitRowItem
import com.example.ui.theme.*
import com.frequent.habits.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGroupCard(
    folder: HabitFolder,
    containedHabits: List<HabitUiItem>,
    completedCount: Int,
    totalCount: Int,
    progressPercent: Float,
    language: String,
    displayedHabits: List<HabitUiItem>,
    justCompletedEvent: Pair<Int, Long>?,
    selectedDate: String,
    vibrationEnabled: Boolean,
    isReorderMode: Boolean,
    listState: LazyListState,
    coloredFolderCards: Boolean = true,
    onToggleCollapse: () -> Unit,
    onToggleFolderCompletion: () -> Unit,
    onEditFolder: () -> Unit,
    onMoveFolderUp: () -> Unit,
    onMoveFolderDown: () -> Unit,
    onToggleHabit: (habitId: Int, hasLog: Boolean) -> Unit,
    onManualAddValue: (habitId: Int) -> Unit,
    onAddQuantity: (habitId: Int, currentVal: Float, diff: Float) -> Unit,
    onHabitLongClick: (Habit) -> Unit,
    onCompleteGoal: (Habit) -> Unit,
    onMoveNestedHabitUp: (habitId: Int) -> Unit,
    onMoveNestedHabitDown: (habitId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = HabitIconMapping.getColor(folder.color)
    val isCollapsed = folder.isCollapsed

    val isAllDone = completedCount == totalCount && totalCount > 0
    val isAllFailed = containedHabits.isNotEmpty() && containedHabits.all { it.isFailed }
    val isPartiallyDone = completedCount > 0 && !isAllDone
    val maxStreak = containedHabits.maxOfOrNull { it.streak } ?: 0

    val targetBgColor = if (coloredFolderCards) {
        when {
            isAllDone -> SuccessBg
            isAllFailed -> FailedBg
            !isCollapsed -> themeColor.copy(alpha = 0.20f)
            else -> themeColor.copy(alpha = 0.15f)
        }
    } else {
        when {
            isAllDone -> SuccessBg
            isAllFailed -> FailedBg
            !isCollapsed -> AppCard
            else -> AppCard
        }
    }

    val targetBorderColor = if (coloredFolderCards) {
        when {
            isAllDone -> SuccessGreen
            isAllFailed -> FailedRed
            !isCollapsed -> themeColor.copy(alpha = 0.85f)
            isPartiallyDone -> themeColor.copy(alpha = 0.70f)
            else -> themeColor.copy(alpha = 0.50f)
        }
    } else {
        when {
            isAllDone -> SuccessGreen
            isAllFailed -> FailedRed
            !isCollapsed -> themeColor.copy(alpha = 0.6f)
            isPartiallyDone -> themeColor.copy(alpha = 0.4f)
            else -> AppBorder
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(120),
        label = "folderBgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(120),
        label = "folderBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        border = BorderStroke(if (!isCollapsed) 1.5.dp else 1.dp, animatedBorderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Folder Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onToggleCollapse,
                        onLongClick = onEditFolder
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Icon (48dp circle matching habit icon container)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (coloredFolderCards) themeColor.copy(alpha = 0.28f) else themeColor.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .border(
                            if (coloredFolderCards) 1.5.dp else 1.dp,
                            if (coloredFolderCards) themeColor else themeColor.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconMapping.getIcon(folder.icon),
                        contentDescription = folder.name,
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Middle Info Column (strictly numerical info: Flame + 0/1, 2/3 + Mini Bar)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tr(language, "ORDNER", "საქაღალდე", "文件夹", "FOLDER"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = themeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (totalCount == 0) {
                            Text(
                                text = tr(language, "Leer (Keine Gewohnheiten)", "ცარიელი (ჩვევების გარეშე)", "空（暂无习惯）", "Empty (No habits)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "$completedCount/$totalCount " + tr(language, "erledigt", "შესრულებული", "已完成", "done"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isAllDone -> SuccessGreen
                                    isAllFailed -> FailedRed
                                    isPartiallyDone -> themeColor
                                    else -> TextSecondary
                                }
                            )

                            LinearProgressIndicator(
                                progress = { progressPercent.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = when {
                                    isAllDone -> SuccessGreen
                                    isAllFailed -> FailedRed
                                    else -> themeColor
                                },
                                trackColor = if (coloredFolderCards) Color.Black.copy(alpha = 0.25f) else ProgressTrack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: Expand/Collapse Arrow + Status Checkbox / Reorder Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isReorderMode) {
                        // Move Up Button matching HabitRowReorderControls
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryViolet.copy(alpha = 0.12f))
                                .border(1.dp, PrimaryViolet.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .clickable { onMoveFolderUp() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Move Down Button matching HabitRowReorderControls
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryViolet.copy(alpha = 0.12f))
                                .border(1.dp, PrimaryViolet.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .clickable { onMoveFolderDown() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Chevron Expand/Collapse button
                        IconButton(
                            onClick = onToggleCollapse,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Checkbox matching habit row checkbox
                        val isEmptyFolder = totalCount == 0
                        val checkboxScale by animateFloatAsState(
                            targetValue = if (!isEmptyFolder && (isAllDone || isAllFailed)) 1.05f else 1.0f,
                            animationSpec = tween(100),
                            label = "checkboxScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = checkboxScale
                                    scaleY = checkboxScale
                                }
                                .drawBehind {
                                    if (!isEmptyFolder && !isAllDone && !isAllFailed && progressPercent > 0f) {
                                        val strokeWidthPx = 3.dp.toPx()
                                        val inset = strokeWidthPx / 2f
                                        val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                                        // Track ring
                                        drawArc(
                                            color = themeColor.copy(alpha = 0.25f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = arcSize,
                                            style = Stroke(width = strokeWidthPx)
                                        )

                                        // Progress arc
                                        drawArc(
                                            color = themeColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f * progressPercent.coerceIn(0f, 1f),
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = arcSize,
                                            style = Stroke(
                                                width = strokeWidthPx,
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }
                                }
                                .background(
                                    when {
                                        isEmptyFolder -> Color.Transparent
                                        isAllDone -> SuccessGreen
                                        isAllFailed -> FailedRed
                                        isPartiallyDone -> themeColor.copy(alpha = 0.12f)
                                        else -> Color.Transparent
                                    },
                                    CircleShape
                                )
                                .border(
                                    width = if (isEmptyFolder) 1.5.dp else 2.dp,
                                    color = when {
                                        isEmptyFolder -> TextSecondary.copy(alpha = 0.22f)
                                        isAllDone -> SuccessGreen
                                        isAllFailed -> FailedRed
                                        else -> TextSecondary.copy(alpha = 0.45f)
                                    },
                                    shape = CircleShape
                                )
                                .clickable(
                                    enabled = !isEmptyFolder,
                                    onClick = { onToggleFolderCompletion() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEmptyFolder) {
                                // Subtly grayed-out dash to visually clarify that this empty folder is inactive
                                Text(
                                    text = "—",
                                    color = TextSecondary.copy(alpha = 0.35f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (isAllDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "All habits completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isAllFailed) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "All habits failed",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Contained Nested Habits (when expanded)
            if (!isCollapsed) {
                if (containedHabits.isNotEmpty()) {
                    // Subtle nesting container inside the folder card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .background(
                                color = if (coloredFolderCards) Color.Black.copy(alpha = 0.20f) else themeColor.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (coloredFolderCards) 1.dp else 0.dp,
                                color = if (coloredFolderCards) themeColor.copy(alpha = 0.30f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            containedHabits.forEach { habitUiItem ->
                                TodayHabitRowItem(
                                    uiItem = habitUiItem,
                                    displayedHabits = displayedHabits,
                                    justCompletedEvent = justCompletedEvent,
                                    isNested = false,
                                    isReorderMode = isReorderMode,
                                    isBeingDragged = false,
                                    dragTranslationY = 0f,
                                    listState = listState,
                                    selectedDate = selectedDate,
                                    language = language,
                                    vibrationEnabled = vibrationEnabled,
                                    onToggle = onToggleHabit,
                                    onManualAddValue = onManualAddValue,
                                    onAddQuantity = onAddQuantity,
                                    onLongClick = onHabitLongClick,
                                    onCompleteGoal = onCompleteGoal,
                                    onMoveUp = { onMoveNestedHabitUp(habitUiItem.habit.id) },
                                    onMoveDown = { onMoveNestedHabitDown(habitUiItem.habit.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Friendly Empty State when folder is expanded but has no habits
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .background(
                                color = if (coloredFolderCards) Color.Black.copy(alpha = 0.15f) else themeColor.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = themeColor.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = HabitIconMapping.getIcon("folder"),
                                contentDescription = null,
                                tint = themeColor.copy(alpha = 0.65f),
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = tr(language, "Dieser Ordner ist noch leer", "ეს საქაღალდე ჯერ ცარიელია", "此文件夹仍为空", "This folder is empty"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Weise Gewohnheiten beim Erstellen oder Bearbeiten diesem Ordner zu.",
                                    "მიაკუთვნეთ ჩვევები ამ საქაღალდეს მათი შექმნისას ან რედაქტირებისას.",
                                    "在创建或编辑习惯时将其分配到此文件夹。",
                                    "Assign habits to this folder when creating or editing them."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
