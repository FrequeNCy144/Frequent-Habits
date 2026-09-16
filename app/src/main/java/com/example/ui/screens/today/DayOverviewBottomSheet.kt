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
import com.example.tr
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOverviewBottomSheet(
    language: String,
    selectedDate: String,
    allArePaused: Boolean,
    hasMinimalViableHabits: Boolean,
    allAreMinimalViable: Boolean,
    onTogglePauseAll: () -> Unit,
    onToggleMinimalViableDay: () -> Unit,
    onStartReorder: () -> Unit,
    onDismiss: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Calendar/Overview badge & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PrimaryViolet.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Day Options",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = tr(language, "Tagesübersicht & Aktionen", "დღის მიმოხილვა და მოქმედებები", "当日概览与操作", "Day Overview & Actions"),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = AppBorder)

            // Action 1: Minimal Viable Day (MVD)
            Surface(
                onClick = onToggleMinimalViableDay,
                enabled = hasMinimalViableHabits,
                shape = RoundedCornerShape(16.dp),
                color = if (!hasMinimalViableHabits) ProgressTrack.copy(alpha = 0.5f)
                    else if (allAreMinimalViable) PrimaryViolet.copy(alpha = 0.2f)
                    else PrimaryViolet.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().testTag("btn_toggle_minimal_viable_day")
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
                                if (!hasMinimalViableHabits) TextSecondary.copy(alpha = 0.1f)
                                else if (allAreMinimalViable) PrimaryViolet.copy(alpha = 0.3f)
                                else PrimaryViolet.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (!hasMinimalViableHabits) TextSecondary.copy(alpha = 0.5f) else PrimaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!hasMinimalViableHabits) {
                                tr(language, "Minimal Viable Day", "მინიმალური დღე", "最小可行日", "Minimal Viable Day")
                            } else if (allAreMinimalViable) {
                                tr(language, "Minimal Viable Day deaktivieren", "მინიმალური დღის გაუქმება", "停用最小可行日", "Deactivate Minimal Viable Day")
                            } else {
                                tr(language, "Minimal Viable Day aktivieren", "მინიმალური დღის გააქტიურება", "启用最小可行日", "Activate Minimal Viable Day")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (!hasMinimalViableHabits) TextSecondary.copy(alpha = 0.6f) else TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (!hasMinimalViableHabits) {
                                tr(language, "Keine Gewohnheiten mit Minimal Viable Version hinterlegt", "მინიმალური ვერსია არ არის კონფიგურირებული", "暂无可用的最小可行版本习惯", "No habits with a minimal viable version configured")
                            } else if (allAreMinimalViable) {
                                tr(language, "Zurück zu vollen Zielwerten für alle Gewohnheiten", "სრულ მიზნებზე დაბრუნება", "恢复为所有习惯的完整目标", "Return to full target values for all habits")
                            } else {
                                tr(language, "Aktiviert die Minimal Viable Version für alle hinterlegten Gewohnheiten", "ააქტიურებს მინიმალურ ვერსიას ყველა კონფიგურირებული ჩვევისთვის", "为所有已配置的习惯启用最小可行版本", "Activates minimal viable version for all configured habits")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Action 2: Alle pausieren / fortsetzen
            Surface(
                onClick = onTogglePauseAll,
                shape = RoundedCornerShape(16.dp),
                color = (if (allArePaused) SuccessGreen else HabitOrange).copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().testTag("btn_toggle_pause_all")
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
                            .background(if (allArePaused) SuccessGreen.copy(alpha = 0.2f) else HabitOrange.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allArePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = if (allArePaused) SuccessGreen else HabitOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (allArePaused) {
                                tr(language, "Alle Gewohnheiten fortsetzen", "ყველა ჩვევის გაგრძელება", "恢复所有习惯", "Resume all habits")
                            } else {
                                tr(language, "Alle Gewohnheiten pausieren", "ყველა ჩვევის დაპაუზება", "暂停所有习惯", "Pause all habits")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (allArePaused) {
                                tr(language, "Pausierung für diesen Tag aufheben", "ამ დღისთვის პაუზის გაუქმება", "解除今日的暂停状态", "Unpause habits for this day")
                            } else {
                                tr(language, "Alle Gewohnheiten für diesen Tag überspringen", "ამ დღისთვის ყველა ჩვევის გამოტოვება", "今日跳过所有习惯打卡", "Skip all habits for this specific day")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Action 3: Reihenfolge anpassen (Reorder habits)
            Surface(
                onClick = onStartReorder,
                shape = RoundedCornerShape(16.dp),
                color = ProgressTrack,
                modifier = Modifier.fillMaxWidth().testTag("btn_reorder_habits")
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
    }
}
