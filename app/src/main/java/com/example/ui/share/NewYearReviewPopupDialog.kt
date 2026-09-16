package com.example.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tr

@Composable
fun NewYearReviewPopupDialog(
    reviewYear: Int,
    language: String,
    onStartReview: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4C1D95),
                            Color(0xFF1E1B4B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(2.dp, Color(0xFFA855F7), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Festive Header Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎆", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = tr(language, "Dein $reviewYear Rückblick ist da! 🎉", "თქვენი $reviewYear მიმოხილვა მზად არის! 🎉", "你的 $reviewYear 年度回顾已就绪！🎉", "Your $reviewYear Review is Ready! 🎉"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Teaser Text
                Text(
                    text = if (language == "de")
                        "Entdecke deine beeindruckenden Erfolge, Strakes und Top-Gewohnheiten des vergangenen Jahres in deiner persönlichen Story!"
                    else if (language == "ka") "აღმოაჩინეთ გასული წლის თქვენი მაჩვენებლები, ზოლები და მთავარი ჩვევები თქვენს პირად ინტერაქტიულ ისტორიაში!" else if (language == "zh") "在个性化互动回顾中，重温你过去一年的高光时刻、坚持连续与最佳习惯！" else "Discover your highlights, streaks and top habits from the past year in your personal interactive story!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onDismiss()
                        onStartReview()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA855F7),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Rückblick jetzt ansehen", "იხილეთ ამბავი ახლა", "立即查看回顾", "View Story Now"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(language, "Später", "მოგვიანებით", "稍后", "Later"),
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
