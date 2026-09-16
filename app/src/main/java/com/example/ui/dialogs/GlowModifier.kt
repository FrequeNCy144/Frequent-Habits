package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp






fun Modifier.animatedGlowingBorder(
    glowColor: Color,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 2.dp,
    baseColor: Color? = null
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    this.drawBehind {
        val radiusPx = cornerRadius.toPx()
        
        // Layer 1: Broad soft ambient aura (outermost)
        drawRoundRect(
            color = glowColor.copy(alpha = 0.18f * pulseAlpha),
            size = androidx.compose.ui.geometry.Size(
                width = size.width + 24.dp.toPx(),
                height = size.height + 24.dp.toPx()
            ),
            topLeft = androidx.compose.ui.geometry.Offset(-12.dp.toPx(), -12.dp.toPx()),
            cornerRadius = CornerRadius(radiusPx + 12.dp.toPx(), radiusPx + 12.dp.toPx())
        )
        
        // Layer 2: Medium vibrant glow halo
        drawRoundRect(
            color = glowColor.copy(alpha = 0.35f * pulseAlpha),
            size = androidx.compose.ui.geometry.Size(
                width = size.width + 14.dp.toPx(),
                height = size.height + 14.dp.toPx()
            ),
            topLeft = androidx.compose.ui.geometry.Offset(-7.dp.toPx(), -7.dp.toPx()),
            cornerRadius = CornerRadius(radiusPx + 7.dp.toPx(), radiusPx + 7.dp.toPx())
        )

        // Layer 3: Intense inner glowing ring
        drawRoundRect(
            color = glowColor.copy(alpha = 0.55f * pulseAlpha),
            size = androidx.compose.ui.geometry.Size(
                width = size.width + 6.dp.toPx(),
                height = size.height + 6.dp.toPx()
            ),
            topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx(), -3.dp.toPx()),
            cornerRadius = CornerRadius(radiusPx + 3.dp.toPx(), radiusPx + 3.dp.toPx())
        )
    }.drawWithContent {
        drawContent()
        val radiusPx = cornerRadius.toPx()
        val widthPx = borderWidth.toPx()
        
        // Solid vibrant border outline
        drawRoundRect(
            color = baseColor?.copy(alpha = 0.85f) ?: glowColor.copy(alpha = 0.85f),
            size = size,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(widthPx)
        )
    }
}