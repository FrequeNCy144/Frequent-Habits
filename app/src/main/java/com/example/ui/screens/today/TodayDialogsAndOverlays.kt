package com.example.ui.screens.today

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Habit
import com.example.tr
import com.example.ui.*
import com.example.ui.HabitsViewModel
import com.example.ui.theme.*

@Composable
fun TodayDeleteConfirmDialog(
    habit: Habit,
    viewModel: HabitsViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppCard,
        title = { Text(text = tr(language, "Habit löschen?", "წაშალოთ ჩვევა?", "删除习惯？", "Delete Habit?")) },
        text = { Text(text = tr(language, "Möchtest du diese Gewohnheit wirklich unwiderruflich löschen?", "დარწმუნებული ხართ, რომ გსურთ სამუდამოდ წაშალოთ ეს ჩვევა?", "确定要永久删除该习惯吗？", "Are you sure you want to delete this habit permanently?")) },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.deleteHabit(habit)
                    onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text(tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
fun TodayArchiveConfirmDialog(
    habit: Habit,
    viewModel: HabitsViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppCard,
        title = { Text(text = tr(language, "Gewohnheit archivieren?", "არქივის ჩვევა?", "归档习惯？", "Archive Habit?")) },
        text = { Text(text = tr(language, "Möchtest du diese Gewohnheit archivieren? Sie wird vom Dashboard und den Statistiken ausgeblendet, kann aber in den Einstellungen jederzeit wieder reaktiviert werden.", "გსურთ დაარქივოთ ეს ჩვევა? ის დამალული იქნება საინფორმაციო დაფისა და სტატისტიკისგან, მაგრამ მისი ხელახალი გააქტიურება ნებისმიერ დროს შესაძლებელია პარამეტრებში.", "确定要归档此习惯吗？归档后将从总览和统计中隐藏，但随时可在设置中重新启用。", "Do you want to archive this habit? It will be hidden from the dashboard and stats, but can be reactivated at any time in settings.")) },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.archiveHabit(habit)
                    onDismiss()
                    Toast.makeText(
                        context,
                        tr(language, "Gewohnheit archiviert", "ჩვევა დაარქივებულია", "习惯已归档", "Habit archived"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text(tr(language, "Archivieren", "არქივი", "归档", "Archive"), color = HabitOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
fun TodaySaskiaUnlockDialog(
    language: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = AppCard,
            border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFE91E63).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "„An Saskia – danke, dass du an meiner Seite bist, heute und an jedem weiteren Tag.“",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(language, "Schließen ❤️", "დახურეთ ❤️", "关闭 ❤️", "Close ❤️"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
