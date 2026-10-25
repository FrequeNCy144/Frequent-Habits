package com.example.ui.screens.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.tr
import com.example.getLocalizedUnit
import com.example.ui.HabitIconMapping
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*
import com.frequent.habits.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitActionBottomSheet(
    habit: Habit,
    language: String,
    isPausedOnSelectedDate: Boolean,
    isMinimalViableOnSelectedDate: Boolean,
    onTogglePause: () -> Unit,
    onToggleMinimalViable: () -> Unit,
    onEditHabit: () -> Unit,
    onArchiveHabit: () -> Unit,
    onDeleteHabit: () -> Unit,
    onStartReorder: (() -> Unit)? = null,
    allFolders: List<HabitFolder> = emptyList(),
    onMoveToFolder: ((folderId: Int?) -> Unit)? = null,
    onCreateFolderAndMove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val habitColor = HabitIconMapping.getColor(habit.color)
    val hasMinimalViableConfig = (habit.minimalViableValue != null && habit.minimalViableValue > 0f) || habit.minimalViableText.isNotBlank()
    var showFolderPicker by remember { mutableStateOf(false) }
    val currentFolder = remember(allFolders, habit.folderId) {
        allFolders.find { it.id == habit.folderId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { StandardSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Habit Icon, Color badge & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(habitColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconMapping.getIcon(habit.icon),
                        contentDescription = habit.name,
                        tint = habitColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (habit.category.isNotEmpty()) habit.category else tr(language, "Gewohnheit", "ჩვევა", "习惯", "Habit"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = habitColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = AppBorder)

            // Minimal Viable Version (MVV) Action (if configured)
            if (hasMinimalViableConfig) {
                Surface(
                    onClick = onToggleMinimalViable,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isMinimalViableOnSelectedDate) PrimaryViolet.copy(alpha = 0.2f) else PrimaryViolet.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().testTag("btn_toggle_minimal_viable_habit")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isMinimalViableOnSelectedDate) PrimaryViolet.copy(alpha = 0.35f) else PrimaryViolet.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = PrimaryViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMinimalViableOnSelectedDate) {
                                    tr(language, "Minimal Viable Version deaktivieren", "მინიმალური ვერსიის გაუქმება", "停用最小可行版本", "Deactivate Minimal Viable Version")
                                } else {
                                    tr(language, "Minimal Viable Version aktivieren", "მინიმალური ვერსიის გააქტიურება", "启用最小可行版本", "Activate Minimal Viable Version")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isMinimalViableOnSelectedDate) {
                                    tr(language, "Vollen Zielwert für heute wiederherstellen", "სრული მიზნის აღდგენა დღეს", "今日恢复完整目标值", "Restore full target value for today")
                                } else {
                                    if (habit.type == "NUMBER" && habit.minimalViableValue != null) {
                                        val minValStr = if (habit.minimalViableValue % 1f == 0f) habit.minimalViableValue.toInt().toString() else habit.minimalViableValue.toString()
                                        val unitStr = if (habit.unit.isNotEmpty()) " ${getLocalizedUnit(habit.unit, language)}" else ""
                                        tr(language, "Zielwert für heute auf $minValStr$unitStr reduzieren", "მიზნის შემცირება: $minValStr$unitStr", "今日目标降低为 $minValStr$unitStr", "Reduce target for today to $minValStr$unitStr")
                                    } else if (habit.minimalViableText.isNotBlank()) {
                                        habit.minimalViableText
                                    } else {
                                        tr(language, "Einstiegshürde für heute auf das Minimum senken", "მინიმალური ვერსიის გამოყენება დღეს", "今日降低至最小可行版本", "Lower effort to minimum viable version for today")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Pausieren / Fortsetzen Action
            Surface(
                onClick = onTogglePause,
                shape = RoundedCornerShape(16.dp),
                color = (if (isPausedOnSelectedDate) SuccessGreen else HabitOrange).copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().testTag("btn_toggle_pause_habit")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isPausedOnSelectedDate) SuccessGreen.copy(alpha = 0.2f) else HabitOrange.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPausedOnSelectedDate) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = if (isPausedOnSelectedDate) SuccessGreen else HabitOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPausedOnSelectedDate) {
                                tr(language, "Fortsetzen (Heute)", "გაგრძელება (დღეს)", "恢复（今日）", "Resume (Today)")
                            } else {
                                tr(language, "Pausieren (Heute)", "დაპაუზება (დღეს)", "暂停（今日）", "Pause (Today)")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isPausedOnSelectedDate) {
                                tr(language, "Gewohnheit heute wieder als aktiv markieren", "ჩვევის ხელახლა გააქტიურება დღეს", "今日将该习惯恢复为正常打卡状态", "Mark habit as active for today")
                            } else {
                                tr(language, "Gewohnheit für heute entschuldigt überspringen", "ჩვევის გამოტოვება დღეს", "今日跳过此习惯打卡，不影响连续记录", "Excuse this habit for today without breaking streak")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Bearbeiten Action
            Surface(
                onClick = onEditHabit,
                shape = RoundedCornerShape(16.dp),
                color = ProgressTrack,
                modifier = Modifier.fillMaxWidth().testTag("btn_edit_habit")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryViolet.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Bearbeiten", "რედაქტირება", "编辑", "Edit"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(language, "Name, Zielwert, Farbe oder Rhythmus anpassen", "სახელის, მიზნის, ფერის შეცვლა", "调整名称、目标值、颜色或执行周期", "Edit name, target value, color or schedule"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // In Ordner verschieben / zuweisen (Move to Folder) Action
            if (onMoveToFolder != null) {
                Surface(
                    onClick = { showFolderPicker = !showFolderPicker },
                    shape = RoundedCornerShape(16.dp),
                    color = ProgressTrack,
                    modifier = Modifier.fillMaxWidth().testTag("btn_assign_folder")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(PrimaryViolet.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_folder),
                                    contentDescription = "Folder",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(language, "In Ordner verschieben", "საქაღალდეში გადატანა", "移至文件夹", "Move to Folder"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (currentFolder != null) {
                                        tr(language, "Aktuell: ${currentFolder.name}", "მიმდინარე: ${currentFolder.name}", "当前: ${currentFolder.name}", "Current: ${currentFolder.name}")
                                    } else {
                                        tr(language, "Keinem Ordner zugeordnet", "არ არის საქაღალდეში", "未分配文件夹", "No folder assigned")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = if (showFolderPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Expanded Folder List
                        if (showFolderPicker) {
                            HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Option: No Folder (Remove from folder)
                                Surface(
                                    onClick = {
                                        onMoveToFolder(null)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (habit.folderId == null) PrimaryViolet.copy(alpha = 0.15f) else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (habit.folderId == null) PrimaryViolet else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = tr(language, "Kein Ordner (Herausnehmen)", "საქაღალდის გარეშე", "无文件夹（移出）", "No Folder (Remove from folder)"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (habit.folderId == null) PrimaryViolet else TextPrimary,
                                            fontWeight = if (habit.folderId == null) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }

                                // List of existing folders
                                allFolders.forEach { folder ->
                                    val fColor = HabitIconMapping.getColor(folder.color)
                                    val isCurrent = folder.id == habit.folderId
                                    Surface(
                                        onClick = {
                                            onMoveToFolder(folder.id)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCurrent) fColor.copy(alpha = 0.15f) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(fColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = HabitIconMapping.getIcon(folder.icon),
                                                    contentDescription = null,
                                                    tint = fColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCurrent) fColor else TextPrimary,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = fColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Option: Create New Folder
                                if (onCreateFolderAndMove != null) {
                                    Surface(
                                        onClick = {
                                            onDismiss()
                                            onCreateFolderAndMove()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = PrimaryViolet,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = tr(language, "+ Neuen Ordner erstellen...", "+ ახალი საქაღალდე...", "+ 创建新文件夹...", "+ Create new folder..."),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = PrimaryViolet,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reihenfolge anpassen (Reorder) Action
            if (onStartReorder != null) {
                Surface(
                    onClick = {
                        onDismiss()
                        onStartReorder()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = ProgressTrack,
                    modifier = Modifier.fillMaxWidth().testTag("btn_reorder_habits_action")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(TextSecondary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(language, "Reihenfolge anpassen", "თანმიმდევრობის შეცვლა", "调整排序", "Reorder Habits"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = tr(language, "Gewohnheiten nach oben oder unten verschieben", "ჩვევების ზემოთ ან ქვემოთ გადაადგილება", "上下移动自定义习惯展示顺序", "Move habits up or down"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Archivieren Action
            Surface(
                onClick = onArchiveHabit,
                shape = RoundedCornerShape(16.dp),
                color = TextSecondary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth().testTag("btn_archive_habit")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(TextSecondary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Archivieren", "არქივაცია", "归档", "Archive"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(language, "Ausblenden, aber Historie und Logs behalten", "დამალვა ისტორიის შენარჩუნებით", "从主界面隐藏，但完整保留所有历史记录", "Hide from active list, preserve all logs"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Löschen Action
            Surface(
                onClick = onDeleteHabit,
                shape = RoundedCornerShape(16.dp),
                color = ErrorRed.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth().testTag("btn_delete_habit")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ErrorRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Löschen", "წაშლა", "删除", "Delete"),
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(language, "Gewohnheit und alle Logs unwiderruflich entfernen", "სამუდამოდ წაშლა", "永久删除该习惯及其所有历史打卡数据", "Permanently remove habit and all its history"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderActionBottomSheet(
    folder: com.example.data.HabitFolder,
    language: String,
    onEditFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val folderColor = HabitIconMapping.getColor(folder.color)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { StandardSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Folder Icon, Color badge & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(folderColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconMapping.getIcon(folder.icon),
                        contentDescription = folder.name,
                        tint = folderColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "Ordner", "საქაღალდე", "文件夹", "Folder"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = folderColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = AppBorder)

            // Bearbeiten Action
            Surface(
                onClick = {
                    onEditFolder()
                },
                shape = RoundedCornerShape(16.dp),
                color = folderColor.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth().testTag("btn_edit_folder")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(folderColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = folderColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Ordner bearbeiten", "საქაღალდის რედაქტირება", "编辑文件夹", "Edit Folder"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(language, "Name, Icon oder Farbe dieses Ordners anpassen", "სახელის, ხატულას ან ფერის შეცვლა", "更改此文件夹的名称、图标或颜色", "Change name, icon, or color of this folder"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Löschen Action
            Surface(
                onClick = {
                    onDeleteFolder()
                },
                shape = RoundedCornerShape(16.dp),
                color = ErrorRed.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth().testTag("btn_delete_folder")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ErrorRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Ordner löschen", "საქაღალდის წაშლა", "删除文件夹", "Delete Folder"),
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(language, "Ordner löschen (Gewohnheiten bleiben erhalten)", "მხოლოდ საქაღალდის წაშლა", "仅删除该文件夹 (其中的习惯将被移出文件夹)", "Delete folder only (habits inside will be kept outside)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
