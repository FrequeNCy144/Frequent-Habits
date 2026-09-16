package com.example.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.ui.theme.*

@Composable
fun TargetWithArrowsIcon(
    arrowCount: Int,
    modifier: Modifier = Modifier
) {
    val baseAngles = remember { listOf(-45f, 135f, 15f, 75f, -105f, -165f) }

    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
            val outerRadius = width * 0.42f
            
            val stroke1 = Stroke(width = 1.5f * density)
            val stroke2 = Stroke(width = 2f * density)
            
            val shaftLength = width * 0.65f
            val startX = center.x - shaftLength * 0.6f
            val endX = center.x + shaftLength * 0.35f
            val y = center.y
            val arrowHeadSize = width * 0.08f
            
            val shaftStart = androidx.compose.ui.geometry.Offset(startX, y)
            val shaftEnd = androidx.compose.ui.geometry.Offset(endX, y)
            
            val fletch1Start = androidx.compose.ui.geometry.Offset(startX, y)
            val fletch1End = androidx.compose.ui.geometry.Offset(startX - arrowHeadSize * 0.8f, y - arrowHeadSize * 0.5f)
            val fletch2Start = androidx.compose.ui.geometry.Offset(startX, y)
            val fletch2End = androidx.compose.ui.geometry.Offset(startX - arrowHeadSize * 0.8f, y + arrowHeadSize * 0.5f)
            
            val arrowHeadPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(endX, y)
                lineTo(endX - arrowHeadSize, y - arrowHeadSize * 0.5f)
                lineTo(endX - arrowHeadSize * 0.7f, y)
                lineTo(endX - arrowHeadSize, y + arrowHeadSize * 0.5f)
                close()
            }
            
            onDrawBehind {
                // Outer ring
                drawCircle(
                    color = Color(0xFF673AB7).copy(alpha = 0.15f),
                    radius = outerRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF673AB7),
                    radius = outerRadius,
                    center = center,
                    style = stroke1
                )
                // Red / Crimson ring
                drawCircle(
                    color = Color(0xFFE91E63),
                    radius = outerRadius * 0.7f,
                    center = center,
                    style = stroke2
                )
                // Golden Center Bullseye
                drawCircle(
                    color = Color(0xFFFFC107),
                    radius = outerRadius * 0.3f,
                    center = center
                )
                
                // Draw arrows piercing the bullseye at different angles
                val limit = arrowCount.coerceAtMost(6)
                for (i in 0 until limit) {
                    val angleDeg = baseAngles[i]
                    withTransform({
                        rotate(angleDeg, center)
                    }) {
                        // 1. Arrow Shaft
                        drawLine(
                            color = Color(0xFF8D6E63),
                            start = shaftStart,
                            end = shaftEnd,
                            strokeWidth = 2f * density,
                            cap = StrokeCap.Round
                        )
                        
                        // 2. Arrow Head
                        drawPath(
                            path = arrowHeadPath,
                            color = Color(0xFFB0BEC5)
                        )
                        
                        // 3. Fletching
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = fletch1Start,
                            end = fletch1End,
                            strokeWidth = 1.5f * density,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = fletch2Start,
                            end = fletch2End,
                            strokeWidth = 1.5f * density,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    )
}

private class RingOrbitData(
    val radius: Float,
    val stroke: Stroke,
    val dotCenter: androidx.compose.ui.geometry.Offset,
    val rotationAngle: Float
)

@Composable
fun CalendarWithRingsIcon(
    ringCount: Int,
    modifier: Modifier = Modifier
) {
    val ringColors = remember { 
        listOf(
            Color(0xFF4CAF50), // Green for 3 days
            Color(0xFFFFC107), // Yellow/Bronze for 10 days
            Color(0xFF9C27B0), // Purple for 30 days
            Color(0xFFFF9800), // Gold/Amber for 100 days
            Color(0xFF00BCD4)  // Cyan/Cosmic for 365 days
        ) 
    }
    
    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
            
            val orbits = (0 until ringCount.coerceAtMost(5)).map { i ->
                val radius = width * (0.35f + (i * 0.045f))
                val stroke = Stroke(
                    width = 1.2f * density,
                    cap = StrokeCap.Round
                )
                val angleRad = Math.toRadians((i * 72f).toDouble())
                val dotX = center.x + radius * Math.cos(angleRad).toFloat()
                val dotY = center.y + radius * Math.sin(angleRad).toFloat()
                RingOrbitData(
                    radius = radius,
                    stroke = stroke,
                    dotCenter = androidx.compose.ui.geometry.Offset(dotX, dotY),
                    rotationAngle = i * 35f
                )
            }
            
            // Calendar Body Geometry
            val calWidth = width * 0.44f
            val calHeight = height * 0.48f
            val calTopLeft = androidx.compose.ui.geometry.Offset(center.x - calWidth / 2f, center.y - calHeight / 2f + height * 0.02f)
            val calCornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * density, 6f * density)
            val calSizeObj = androidx.compose.ui.geometry.Size(calWidth, calHeight)
            
            val headerHeight = calHeight * 0.32f
            val headerSize = androidx.compose.ui.geometry.Size(calWidth, headerHeight)
            val headerRectSize = androidx.compose.ui.geometry.Size(calWidth, headerHeight * 0.5f)
            val headerRectTopLeft = androidx.compose.ui.geometry.Offset(calTopLeft.x, calTopLeft.y + headerHeight * 0.5f)
            
            // Binder Rings (small vertical rounded rects)
            val ringWidth = calWidth * 0.12f
            val ringH = calHeight * 0.22f
            val ringSize = androidx.compose.ui.geometry.Size(ringWidth, ringH)
            val ringCornerRadius = androidx.compose.ui.geometry.CornerRadius(ringWidth / 2f, ringWidth / 2f)
            val ring1TopLeft = androidx.compose.ui.geometry.Offset(calTopLeft.x + calWidth * 0.22f, calTopLeft.y - ringH * 0.35f)
            val ring2TopLeft = androidx.compose.ui.geometry.Offset(calTopLeft.x + calWidth * 0.66f, calTopLeft.y - ringH * 0.35f)
            
            // Calendar grid dots
            val gridStartX = calTopLeft.x + calWidth * 0.25f
            val gridEndX = calTopLeft.x + calWidth * 0.75f
            val gridStartY = calTopLeft.y + headerHeight + calHeight * 0.18f
            val gridEndY = calTopLeft.y + calHeight * 0.82f
            val dotRadius = 1.6f * density
            
            val gridDots = listOf(
                androidx.compose.ui.geometry.Offset(gridStartX, gridStartY),
                androidx.compose.ui.geometry.Offset((gridStartX + gridEndX) / 2f, gridStartY),
                androidx.compose.ui.geometry.Offset(gridEndX, gridStartY),
                androidx.compose.ui.geometry.Offset(gridStartX, (gridStartY + gridEndY) / 2f),
                androidx.compose.ui.geometry.Offset((gridStartX + gridEndX) / 2f, (gridStartY + gridEndY) / 2f),
                androidx.compose.ui.geometry.Offset(gridEndX, (gridStartY + gridEndY) / 2f),
                androidx.compose.ui.geometry.Offset(gridStartX, gridEndY),
                androidx.compose.ui.geometry.Offset((gridStartX + gridEndX) / 2f, gridEndY),
                androidx.compose.ui.geometry.Offset(gridEndX, gridEndY)
            )
            
            // Checkmark Path
            val checkStroke = Stroke(width = 2.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val checkPath = androidx.compose.ui.graphics.Path().apply {
                val cx = center.x
                val cy = center.y + calHeight * 0.12f
                val s = calWidth * 0.22f
                moveTo(cx - s * 0.8f, cy)
                lineTo(cx - s * 0.2f, cy + s * 0.6f)
                lineTo(cx + s * 0.9f, cy - s * 0.6f)
            }
            
            onDrawBehind {
                // Draw Cosmic Energy Orbit Rings around the Calendar
                orbits.forEachIndexed { i, rData ->
                    withTransform({
                        rotate(rData.rotationAngle, center)
                    }) {
                        drawCircle(
                            color = ringColors[i],
                            radius = rData.radius,
                            center = center,
                            style = rData.stroke
                        )
                        drawCircle(
                            color = ringColors[i],
                            radius = 3f * density,
                            center = rData.dotCenter
                        )
                    }
                }
                
                // Calendar Background Card
                drawRoundRect(
                    color = AppCard,
                    size = calSizeObj,
                    topLeft = calTopLeft,
                    cornerRadius = calCornerRadius
                )
                
                // Calendar Header Banner (Red/Pinkish)
                drawRoundRect(
                    color = Color(0xFFE91E63),
                    size = headerSize,
                    topLeft = calTopLeft,
                    cornerRadius = calCornerRadius
                )
                // Fill lower rounded corners back to square for calendar card divider
                drawRect(
                    color = Color(0xFFE91E63),
                    size = headerRectSize,
                    topLeft = headerRectTopLeft
                )
                
                // Calendar Binder Rings
                drawRoundRect(
                    color = Color(0xFFB0BEC5),
                    size = ringSize,
                    topLeft = ring1TopLeft,
                    cornerRadius = ringCornerRadius
                )
                drawRoundRect(
                    color = Color(0xFFB0BEC5),
                    size = ringSize,
                    topLeft = ring2TopLeft,
                    cornerRadius = ringCornerRadius
                )
                
                // Calendar Grid/Dots in body
                gridDots.forEach { dotOffset ->
                    drawCircle(
                        color = Color(0xFF94A3B8),
                        radius = dotRadius,
                        center = dotOffset
                    )
                }
                
                // Checkmark overlay
                drawPath(
                    path = checkPath,
                    color = Color(0xFF4CAF50),
                    style = checkStroke
                )
            }
        }
    )
}

@Composable
fun ZettelMitStiftIcon(
    modifier: Modifier = Modifier,
    color: Color = TextPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.13f

        // 1. Bold rounded note page outline - well proportioned
        val padLeft = w * 0.12f
        val padTop = h * 0.10f
        val padWidth = w * 0.76f
        val padHeight = h * 0.80f
        val padRadius = w * 0.18f

        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(padLeft, padTop),
            size = androidx.compose.ui.geometry.Size(padWidth, padHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(padRadius, padRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // 2. Bold horizontal note lines inside
        val lineStartX = padLeft + padWidth * 0.24f
        val line1EndX = padLeft + padWidth * 0.76f
        val line2EndX = padLeft + padWidth * 0.54f
        val line1Y = padTop + padHeight * 0.38f
        val line2Y = padTop + padHeight * 0.64f

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(lineStartX, line1Y),
            end = androidx.compose.ui.geometry.Offset(line1EndX, line1Y),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(lineStartX, line2Y),
            end = androidx.compose.ui.geometry.Offset(line2EndX, line2Y),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun BoldPlusIcon(
    modifier: Modifier = Modifier,
    color: Color = TextPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val halfLen = w * 0.32f
        val stroke = w * 0.16f

        // Horizontal bar
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx - halfLen, cy),
            end = androidx.compose.ui.geometry.Offset(cx + halfLen, cy),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // Vertical bar
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx, cy - halfLen),
            end = androidx.compose.ui.geometry.Offset(cx, cy + halfLen),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
