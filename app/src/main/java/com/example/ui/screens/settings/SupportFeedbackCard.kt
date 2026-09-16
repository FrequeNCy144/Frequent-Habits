package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.dialogs.animatedGlowingBorder
import com.example.ui.theme.*
import com.frequent.habits.BuildConfig

@Composable
fun SupportFeedbackCard(
    language: String,
    context: Context,
    uriHandler: UriHandler,
    accentColor: Color = PrimaryViolet
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animatedGlowingBorder(
                glowColor = accentColor,
                cornerRadius = 20.dp,
                borderWidth = 2.dp,
                baseColor = accentColor
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tr(language, "Projekt & Feedback", "Project & Feedback"),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tr(language, "Frequent Habits ist zu 100% werbefrei, open-source und offline-first. Unterstütze das Projekt gerne oder gib uns Feedback!", "Frequent Habits is 100% ad-free, open-source and offline-first. Support the project or give us feedback!"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Grid of Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Rate & GitHub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Rate", modifier = Modifier.size(18.dp), tint = PrimaryViolet)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Bewerten", "შეფასება", "评价应用", "Rate App"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/FrequeNCy144/Frequent-Habits") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "GitHub", modifier = Modifier.size(18.dp), tint = HabitPurple)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "GitHub Repo", "GitHub რეპო", "开源代码", "GitHub Repo"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 2: Bug & Feature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/FrequeNCy144/Frequent-Habits/issues/new?template=bug_report.md") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Bug", modifier = Modifier.size(18.dp), tint = ErrorRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Bug melden", "შეცდომის შეტყობინება", "反馈问题", "Report Bug"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/FrequeNCy144/Frequent-Habits/issues/new?template=feature_request.md") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Feature", modifier = Modifier.size(18.dp), tint = HabitYellow)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Feature anfordern", "ფუნქციის მოთხოვნა", "提出新功能建议", "Request Feature"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 3: Share & Website
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, tr(language, "Empfehlung: Frequent Habits", "Recommendation: Frequent Habits"))
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey! I'm using 'Frequent Habits' to track my daily habits and improve my routine. Fully ad-free, open-source, and offline-first! Check it out: https://github.com/FrequeNCy144/Frequent-Habits"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, tr(language, "Teilen mit...", "Share with...")))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp), tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "App teilen", "გაზიარება", "分享应用", "Share App"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://www.frequency-apps.com/") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Website", modifier = Modifier.size(18.dp), tint = HabitOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Website", "ვებსაიტი", "官网", "Website"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { uriHandler.openUri("https://ko-fi.com/frequency_144") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5E5B)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "Unterstützen via Ko-fi", "Support via Ko-fi"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequent Habits v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
