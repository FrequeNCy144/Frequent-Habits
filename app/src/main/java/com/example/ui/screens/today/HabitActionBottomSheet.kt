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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*

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
    onDismiss: () -> Unit
) {
    val habitColor = HabitIconMapping.getColor(habit.color)
    val hasMinimalViableConfig = (habit.minimalViableValue != null && habit.minimalViableValue > 0f) || habit.minimalViableText.isNotBlank()

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
                                        val unitStr = if (habit.unit.isNotEmpty()) " ${habit.unit}" else ""
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
