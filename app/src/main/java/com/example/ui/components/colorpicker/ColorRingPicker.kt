package com.example.ui.components.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TextPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ColorRingPicker(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    centerColor: Color,
    hexText: String,
    modifier: Modifier = Modifier
) {
    var sizePx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Center Preview & Hex Text inside the ring
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(centerColor)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hexText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = sizePx / 2f
                        val dx = offset.x - center
                        val dy = offset.y - center
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        onHueChanged(angle)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = sizePx / 2f
                        val dx = change.position.x - center
                        val dy = change.position.y - center
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        onHueChanged(angle)
                    }
                }
                .onSizeChanged { sizePx = it.width.toFloat() }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 24.dp.toPx()
            val radius = (size.width - strokeWidth) / 2f

            // 1. Draw Rainbow Sweep Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFEF4444), // Red 0 deg
                        Color(0xFFF59E0B), // Yellow 60 deg
                        Color(0xFF10B981), // Green 120 deg
                        Color(0xFF06B6D4), // Cyan 180 deg
                        Color(0xFF3B82F6), // Blue 240 deg
                        Color(0xFFEC4899), // Magenta 300 deg
                        Color(0xFFEF4444)  // Red 360 deg
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Draw Selector Thumb
            val angleRad = Math.toRadians(hue.toDouble())
            val thumbX = center.x + radius * cos(angleRad).toFloat()
            val thumbY = center.y + radius * sin(angleRad).toFloat()
            val thumbCenter = Offset(thumbX, thumbY)

            // Outer drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 15.dp.toPx(),
                center = thumbCenter
            )
            // White border ring
            drawCircle(
                color = Color.White,
                radius = 13.dp.toPx(),
                center = thumbCenter
            )
            // Inner color
            val thumbHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            drawCircle(
                color = thumbHueColor,
                radius = 9.dp.toPx(),
                center = thumbCenter
            )
        }
    }
}
