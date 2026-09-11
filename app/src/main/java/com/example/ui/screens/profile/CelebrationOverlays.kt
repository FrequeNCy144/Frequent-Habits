package com.example.ui.screens.profile

import android.content.Context
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UnlockedAchievementInfo
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.FeedbackHelper
import com.example.ui.theme.*

fun Modifier.saturationFilter(saturation: Float): Modifier = this.drawWithContent {
    if (saturation >= 0.99f) {
        drawContent()
    } else {
        val matrix = ColorMatrix().apply {
            setToSaturation(saturation)
        }
        val filter = ColorFilter.colorMatrix(matrix)
        val paint = androidx.compose.ui.graphics.Paint().apply {
            colorFilter = filter
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
    }
}

private data class ConfettiParticle(
    val cosVal: Float,
    val sinVal: Float,
    val speed: Float,
    val color: Color
)

@Composable
fun ConfettiAnimation(onFinished: () -> Unit) {
    val particles = remember {
        List(24) {
            val angle = (0..360).random().toFloat()
            val radian = Math.toRadians(angle.toDouble())
            ConfettiParticle(
                cosVal = Math.cos(radian).toFloat(),
                sinVal = Math.sin(radian).toFloat(),
                speed = (15..45).random().toFloat(),
                color = listOf(SuccessGreen, HabitYellow, HabitRed, PrimaryViolet).random()
            )
        }
    }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            progress = value
        }
        onFinished()
    }

    Canvas(modifier = Modifier.size(80.dp)) {
        val radiusMultiplier = 6f * (1f - progress)
        val alphaVal = (1f - progress).coerceIn(0f, 1f)
        val progressFactor = progress * 1.5f

        particles.forEach { particle ->
            val distance = particle.speed * progressFactor
            val x = center.x + particle.cosVal * distance
            val y = center.y + particle.sinVal * distance
            
            drawCircle(
                color = particle.color,
                radius = radiusMultiplier,
                center = androidx.compose.ui.geometry.Offset(x, y),
                alpha = alphaVal
            )
        }
    }
}

private data class ScreenConfettiParticle(
    val startXRatio: Float,
    val startYRatio: Float,
    val vx: Float,
    val vy: Float,
    val pSize: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float,
    val shapeType: Int
)

@Composable
fun FullScreensCelebrationConfetti(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FeedbackHelper.playCompletionFeedback(
            context = context,
            vibrationEnabled = true
        )
    }

    val particles = remember {
        val colors = listOf(
            Color(0xFFFFD54F), // Gold
            Color(0xFFAB47BC), // Violet
            Color(0xFF66BB6A), // Green
            Color(0xFF29B6F6), // Cyan
            Color(0xFFFF7043), // Coral
            Color(0xFFEC407A), // Pink
            Color(0xFF7E57C2), // Deep Purple
            Color(0xFF26A69A), // Teal
            Color(0xFFFFFFFF)  // White
        )
        val list = mutableListOf<ScreenConfettiParticle>()
        val random = java.util.Random(42)
        for (i in 0 until 180) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 200f + random.nextFloat() * 750f
            list.add(
                ScreenConfettiParticle(
                    startXRatio = random.nextFloat(),
                    startYRatio = -0.2f + random.nextFloat() * 0.5f,
                    vx = (kotlin.math.cos(angle) * speed).toFloat(),
                    vy = (kotlin.math.sin(angle) * speed - 200f).toFloat(),
                    pSize = (12f + random.nextFloat() * 22f),
                    color = colors[random.nextInt(colors.size)],
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = (random.nextFloat() - 0.5f) * 720f,
                    shapeType = random.nextInt(3)
                )
            )
        }
        list
    }

    val timeAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        onFinished()
    }

    val progress = timeAnim.value
    val alpha = (1f - progress).coerceIn(0f, 1f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val t = progress * 2.0f
        val gravity = 500f

        particles.forEach { p ->
            val startX = p.startXRatio * canvasWidth
            val startY = p.startYRatio * canvasHeight
            val curX = startX + p.vx * t
            val curY = startY + p.vy * t + 0.5f * gravity * t * t
            val curRot = p.rotation + p.rotationSpeed * t

            if (curX in -100f..(canvasWidth + 100f) && curY in -100f..(canvasHeight + 100f)) {
                withTransform({
                    translate(left = curX, top = curY)
                    rotate(degrees = curRot)
                }) {
                    val pColor = p.color.copy(alpha = alpha)
                    when (p.shapeType) {
                        0 -> drawCircle(color = pColor, radius = p.pSize / 2f)
                        1 -> drawRect(
                            color = pColor,
                            topLeft = androidx.compose.ui.geometry.Offset(-p.pSize / 2f, -p.pSize / 2f),
                            size = androidx.compose.ui.geometry.Size(p.pSize, p.pSize * 0.7f)
                        )
                        else -> drawOval(
                            color = pColor,
                            topLeft = androidx.compose.ui.geometry.Offset(-p.pSize / 2f, -p.pSize / 2f),
                            size = androidx.compose.ui.geometry.Size(p.pSize * 1.4f, p.pSize * 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private data class AchievementConfettiParticle(
    val startXRatio: Float,
    val startYRatio: Float,
    val vx: Float,
    val vy: Float,
    val pSize: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float,
    val shapeType: Int
)

@Composable
fun ConfettiCanvas(
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFFD54F), // Gold
            Color(0xFFAB47BC), // Violet
            Color(0xFF66BB6A), // Green
            Color(0xFF29B6F6), // Cyan
            Color(0xFFFF7043), // Coral
            Color(0xFFEC407A), // Pink
            Color(0xFF7E57C2), // Deep Purple
            Color(0xFF26A69A), // Teal
            Color(0xFFFFFFFF)  // White
        )
        val list = mutableListOf<AchievementConfettiParticle>()
        val random = java.util.Random(1337)
        for (i in 0 until 180) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 200f + random.nextFloat() * 750f
            list.add(
                AchievementConfettiParticle(
                    startXRatio = random.nextFloat(),
                    startYRatio = -0.2f + random.nextFloat() * 0.5f,
                    vx = (kotlin.math.cos(angle) * speed).toFloat(),
                    vy = (kotlin.math.sin(angle) * speed - 200f).toFloat(),
                    pSize = (12f + random.nextFloat() * 22f),
                    color = colors[random.nextInt(colors.size)],
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = (random.nextFloat() - 0.5f) * 720f,
                    shapeType = random.nextInt(3)
                )
            )
        }
        list
    }

    val timeAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
        )
    }

    val progress = timeAnim.value
    val alpha = (1f - (progress * 0.75f)).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val t = progress * 3.5f
        val gravity = 500f

        particles.forEach { p ->
            val startX = p.startXRatio * canvasWidth
            val startY = p.startYRatio * canvasHeight
            val curX = startX + p.vx * t
            val curY = startY + p.vy * t + 0.5f * gravity * t * t
            val curRot = p.rotation + p.rotationSpeed * t

            if (curX in -100f..(canvasWidth + 100f) && curY in -100f..(canvasHeight + 100f)) {
                withTransform({
                    translate(left = curX, top = curY)
                    rotate(degrees = curRot)
                }) {
                    val pColor = p.color.copy(alpha = alpha)
                    when (p.shapeType) {
                        0 -> drawCircle(color = pColor, radius = p.pSize / 2f)
                        1 -> drawRect(
                            color = pColor,
                            topLeft = androidx.compose.ui.geometry.Offset(-p.pSize / 2f, -p.pSize / 2f),
                            size = androidx.compose.ui.geometry.Size(p.pSize, p.pSize * 0.7f)
                        )
                        else -> drawOval(
                            color = pColor,
                            topLeft = androidx.compose.ui.geometry.Offset(-p.pSize / 2f, -p.pSize / 2f),
                            size = androidx.compose.ui.geometry.Size(p.pSize * 1.4f, p.pSize * 0.6f)
                        )
                    }
                }
            }
        }
    }
}
