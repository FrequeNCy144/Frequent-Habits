package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryViolet
import com.example.tr

@Composable
fun CreateFirstHabitArrowHint(
    language: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_arrow")
    val animOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 70.dp, end = 20.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Arrow pointing straight UP directly at the '+' button above
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .offset(y = animOffsetY.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                color = PrimaryViolet,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 10.dp,
                border = BorderStroke(1.5.dp, SecondaryViolet),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = tr(language, "Erstelle deine erste Gewohnheit!", "შექმენი შენი პირველი ჩვევა!", "创建你的第一个习惯！", "Create your first habit!"),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (language == "de") "Tippe oben rechts auf das '+'-Symbol" else if (language == "ka") "შეეხეთ "+" ხატულას ზედა მარჯვენა კუთხეში" else if (language == "zh") "点击右上角的“+”图标" else "Tap the '+' icon at the top right",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
