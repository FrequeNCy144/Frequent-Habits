package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryViolet
import com.example.tr

@Composable
fun CreateFirstHabitArrowHint(
    language: String,
    onDismiss: () -> Unit
) {
    // 1. Slow, premium floating animation for the entire card & arrow container
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_card")
    val cardOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, end = 16.dp)
            .offset(y = cardOffsetY.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // 2. Beautiful animated dashed curved arrow pointing directly at the '+' button
            CurvedDashedArrow(
                color = PrimaryViolet,
                modifier = Modifier
                    .width(80.dp)
                    .height(44.dp)
                    .padding(end = 6.dp)
            )

            // 3. Premium Glassmorphic Card
            Surface(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = PrimaryViolet.copy(alpha = 0.4f),
                        spotColor = PrimaryViolet
                    ),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF783CFA), // Primary Purple
                                    Color(0xFF5B20DF)  // Deep Royal Violet
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFC084FC).copy(alpha = 0.8f), // Soft neon purple
                                    Color(0xFF783CFA).copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Top row with Tip badge & close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Tip" Capsule
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = tr(language, "TIPP", "რჩევა", "提示", "TIP"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Minimalistic round Close button
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { onDismiss() }
                                    .testTag("dismiss_first_habit_hint"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = tr(language, "Erstelle deine erste Gewohnheit!", "შექმენი შენი პირველი ჩვევა!", "创建你的第一个习惯！", "Create your first habit!"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 19.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (language == "de") "Tippe oben rechts auf das '+'-Symbol" else if (language == "ka") "შეეხეთ \"+\" ხატულას ზედა მარჯვენა კუთხეში" else if (language == "zh") "点击右上角的“+”图标" else "Tap the '+' icon at the top right",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurvedDashedArrow(
    color: Color,
    modifier: Modifier = Modifier
) {
    // Elegant flow animation phase for the dashes
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_dash_offset")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Path starting from top of card (bottom-left of canvas)
        // curving gracefully up and right to point directly to the '+' button (top-right)
        val path = Path().apply {
            moveTo(width * 0.15f, height * 0.95f)
            quadraticTo(
                width * 0.40f, height * 0.25f, // Smooth upward curve anchor
                width * 0.92f, height * 0.04f  // Arrowhead pointing right up into button
            )
        }

        // Measure path to extract accurate arrowhead position and tangent
        val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
        val position = floatArrayOf(0f, 0f)
        val tangent = floatArrayOf(0f, 0f)
        pathMeasure.getPosTan(pathMeasure.length, position, tangent)

        // Draw the curved dashed stream line
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), phase)
            )
        )

        // Draw accurate arrowhead pointing right/up-right dynamically oriented with tangent
        if (position[0] > 0) {
            val arrowPath = Path().apply {
                moveTo(position[0], position[1])
                val angle = Math.atan2(tangent[1].toDouble(), tangent[0].toDouble())
                val arrowLength = 7.5.dp.toPx()
                // Arrow head wings angles
                val angle1 = angle - Math.PI * 5 / 6
                val angle2 = angle + Math.PI * 5 / 6
                lineTo(
                    (position[0] + arrowLength * Math.cos(angle1)).toFloat(),
                    (position[1] + arrowLength * Math.sin(angle1)).toFloat()
                )
                moveTo(position[0], position[1])
                lineTo(
                    (position[0] + arrowLength * Math.cos(angle2)).toFloat(),
                    (position[1] + arrowLength * Math.sin(angle2)).toFloat()
                )
            }
            drawPath(
                path = arrowPath,
                color = color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
