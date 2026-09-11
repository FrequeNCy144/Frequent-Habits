package com.example.ui.screens.settings

import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.theme.*

fun LazyListScope.notificationsSettingsSection(
    language: String,
    reviewNotificationsEnabled: Boolean,
    insightNotificationsEnabled: Boolean,
    monthlyReviewEnabled: Boolean,
    yearlyReviewEnabled: Boolean,
    viewModel: HabitsViewModel,
    permissionLauncher: ActivityResultLauncher<String>,
    hasNotificationPermission: Boolean
) {
    // Card 1: Master Benachrichtigungen
    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifications",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(language, "Gewohnheits-Erinnerungen", "ჩვევების შეხსენებები", "习惯提醒推送", "Habit Reminders Push"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tr(language, "Tägliche Push-Erinnerungen für deine aktiven Gewohnheiten", "ყოველდღიური push შეხსენებები", "日常习惯每日推送提醒", "Daily push reminders for your habits"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = reviewNotificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setReviewNotificationsEnabled(true)
                                }
                            } else {
                                viewModel.setReviewNotificationsEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryViolet,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ProgressTrack
                        )
                    )
                }
            }
        }
    }

    // Card 2: Smart Insights Push
    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Insights",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(language, "Smart Insights Push", "ჭკვიანი ანალიტიკის Push", "智能洞察推送", "Smart Insights Push"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tr(language, "Regelmäßige Push-Benachrichtigungen mit deinen besten Verhaltens-Erkenntnissen", "რეგულარული push შეტყობინებები მთავარი ანალიტიკით", "定期推送关键行为分析与洞察", "Regular push notifications with key behavioral insights"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = insightNotificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setInsightNotificationsEnabled(true)
                                }
                            } else {
                                viewModel.setInsightNotificationsEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryViolet,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ProgressTrack
                        )
                    )
                }
            }
        }
    }

    // Card 3: Monatsrückblick
    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarViewMonth,
                            contentDescription = "Monthly",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(language, "Monatsrückblick (In-App & Push)", "თვის მიმოხილვა", "月度回顾", "Monthly Review (In-App & Push)"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tr(language, "Aktiviert den Monatsrückblick in der App sowie Push-Erinnerung", "ყოველთვიური სტატისტიკა და შეხსენება", "启用应用内月度回顾与推送提醒", "Enables monthly review in app & push reminder"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = monthlyReviewEnabled,
                        onCheckedChange = { viewModel.setMonthlyReviewEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryViolet,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ProgressTrack
                        )
                    )
                }
            }
        }
    }

    // Card 4: Jahresrückblick
    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Yearly",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(language, "Jahresrückblick (In-App & Push)", "წლის მიმოხილვა", "年度回顾", "Yearly Review (In-App & Push)"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tr(language, "Aktiviert den großen Jahresrückblick in der App sowie Push-Erinnerung", "წლის შეჯამება და შეხსენება", "启用应用内年度回顾与推送提醒", "Enables year in review in app & push reminder"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = yearlyReviewEnabled,
                        onCheckedChange = { viewModel.setYearlyReviewEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryViolet,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ProgressTrack
                        )
                    )
                }
            }
        }
    }
}
