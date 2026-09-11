package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.components.ModernBackButton
import com.example.ui.deleteHabit
import com.example.ui.theme.*
import com.example.ui.unarchiveHabit

@Composable
fun ArchivedHabitsSubpage(
    language: String,
    viewModel: HabitsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(enabled = true) {
        onDismiss()
    }
    val archivedHabits by viewModel.archivedHabits.collectAsStateWithLifecycle()
    var showDeleteConfirmInArchive by remember { mutableStateOf<Habit?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Spacer for floating header
                item {
                    Spacer(modifier = Modifier.height(84.dp))
                }

                // Description
                item {
                    Text(
                        text = if (language == "de") "Archivierte Gewohnheiten sind pausiert und werden nicht im Dashboard oder in Statistiken angezeigt. Du kannst sie jederzeit wieder aktivieren." else "Archived habits are paused and do not appear in the dashboard or statistics. You can reactivate them at any time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                    )
                }

                if (archivedHabits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(language, "Keine archivierten Gewohnheiten.", "არქივირებული ჩვევები.", "暂无已归档习惯。", "No archived habits."),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(archivedHabits, key = { it.id }) { habit ->
                        val habitColor = remember(habit.color) { HabitIconMapping.getColor(habit.color) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(habitColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = HabitIconMapping.getIcon(habit.icon),
                                            contentDescription = null,
                                            tint = habitColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = habit.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reaktivieren Button
                                    IconButton(
                                        onClick = {
                                            viewModel.unarchiveHabit(habit)
                                            Toast.makeText(
                                                context,
                                                tr(language, "${habit.name} reaktiviert", "${habit.name} ხელახლა გააქტიურებულია", "已重新启用 ${habit.name}", "${habit.name} reactivated"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Restore",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Löschen Button
                                    IconButton(
                                        onClick = { showDeleteConfirmInArchive = habit },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ProgressTrack, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Header Overlay with vertical gradient fade-out
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppBg,
                                AppBg,
                                AppBg.copy(alpha = 0.9f),
                                AppBg.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernBackButton(
                        onClick = { onDismiss() },
                        testTag = "archive_back_button"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = tr(language, "Archiv", "არქივი", "归档", "Archive"),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showDeleteConfirmInArchive != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmInArchive = null },
                containerColor = AppCard,
                title = { Text(text = tr(language, "Gewohnheit endgültig löschen?", "სამუდამოდ წაშალოთ ჩვევა?", "永久删除习惯？", "Delete Habit Permanently?")) },
                text = { Text(text = if (language == "de") "Möchtest du '${showDeleteConfirmInArchive?.name}' wirklich unwiderruflich löschen? Alle Verlaufsdaten gehen verloren." else if (language == "ka") "დარწმუნებული ხართ, რომ გსურთ სამუდამოდ წაშალოთ \"${showDeleteConfirmInArchive?.name}\"? ყველა თვალთვალის ისტორია დაიკარგება." else if (language == "zh") "确定要永久删除“${showDeleteConfirmInArchive?.name}”吗？所有历史数据都将丢失。" else "Are you sure you want to delete '${showDeleteConfirmInArchive?.name}' permanently? All tracking history will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val h = showDeleteConfirmInArchive!!
                            viewModel.deleteHabit(h)
                            showDeleteConfirmInArchive = null
                            Toast.makeText(
                                context,
                                tr(language, "Gewohnheit gelöscht", "ჩვევა წაშლილია", "习惯已删除", "Habit deleted"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(tr(language, "Löschen", "წაშლა", "删除", "Delete"), color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmInArchive = null }) {
                        Text(tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"), color = TextSecondary)
                    }
                }
            )
        }
}

