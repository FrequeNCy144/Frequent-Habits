package com.example.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.ui.theme.*

@Composable
fun AchievementBadge(
    type: String, // "STREAK", "COMPLETIONS", "PERFECT_DAYS"
    tier: String, // e.g. "WOOD", "BRONZE", "SILVER", "GOLD", "COMP_10", "PERF_7" etc.
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = if (isUnlocked) 1f else 0.35f
    val modifierWithAlpha = modifier.graphicsLayer { this.alpha = alpha }

    when (type) {
        "COMPLETIONS" -> {
            val arrowCount = when (tier) {
                "COMP_10" -> 1
                "COMP_50" -> 2
                "COMP_200" -> 3
                "COMP_500" -> 4
                "COMP_1000" -> 6
                else -> 1
            }
            TargetWithArrowsIcon(
                arrowCount = arrowCount,
                modifier = modifierWithAlpha
            )
        }
        "PERFECT_DAYS" -> {
            val ringCount = when (tier) {
                "PERF_7" -> 1
                "PERF_30" -> 2
                "PERF_365" -> 3
                "PERF_1000" -> 5
                else -> 1
            }
            CalendarWithRingsIcon(
                ringCount = ringCount,
                modifier = modifierWithAlpha
            )
        }
        "GOAL_MASTERED" -> {
            MasteredGoalTrophyIcon(modifier = modifierWithAlpha)
        }
        else -> {
            // STREAK
            val tierColor = when (tier) {
                "WOOD" -> Color(0xFF8D6E63) // Brown Wood
                "BRONZE" -> Color(0xFFCD7F32) // Bronze
                "SILVER" -> Color(0xFFB0BEC5) // Silver
                "GOLD" -> Color(0xFFFFD700) // Gold
                "PLATINUM" -> Color(0xFF80DEEA) // Platinum (Aqua)
                "DIAMOND" -> Color(0xFF00E5FF) // Diamond (Vibrant Cyan)
                "RUBY" -> Color(0xFFFF1744) // Ruby (Deep Red)
                "MASTER" -> Color(0xFFAA00FF) // Master (Deep Violet)
                "LEGEND" -> Color(0xFFFF6D00) // Legend (Blazing Orange)
                "UNREAL" -> Color(0xFF76FF03) // Unreal (Neon Lime)
                else -> Color(0xFFFFD700)
            }
            val tierSecondary = when (tier) {
                "WOOD" -> Color(0xFF4E342E)
                "BRONZE" -> Color(0xFF8D5524)
                "SILVER" -> Color(0xFF78909C)
                "GOLD" -> Color(0xFFB59000)
                "PLATINUM" -> Color(0xFF00ACC1)
                "DIAMOND" -> Color(0xFF0097A7)
                "RUBY" -> Color(0xFFB71C1C)
                "MASTER" -> Color(0xFF4A148C)
                "LEGEND" -> Color(0xFFE65100)
                "UNREAL" -> Color(0xFF33691E)
                else -> Color(0xFFB59000)
            }

            Spacer(
                modifier = modifier.drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                    val outerRadius = width * 0.45f

                    val borderStroke = Stroke(width = 3f * density)
                    val woodStroke1 = Stroke(width = 1.5f * density)
                    val woodStroke2 = Stroke(width = 1f * density)

                    val flameWidth = width * 0.35f
                    val flameHeight = height * 0.5f

                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x, center.y - flameHeight * 0.5f)
                        cubicTo(
                            center.x + flameWidth * 0.4f, center.y - flameHeight * 0.1f,
                            center.x + flameWidth * 0.6f, center.y + flameHeight * 0.2f,
                            center.x, center.y + flameHeight * 0.5f
                        )
                        cubicTo(
                            center.x - flameWidth * 0.6f, center.y + flameHeight * 0.2f,
                            center.x - flameWidth * 0.4f, center.y - flameHeight * 0.1f,
                            center.x, center.y - flameHeight * 0.5f
                        )
                    }

                    val innerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x, center.y - flameHeight * 0.2f)
                        cubicTo(
                            center.x + flameWidth * 0.2f, center.y + flameHeight * 0.05f,
                            center.x + flameWidth * 0.3f, center.y + flameHeight * 0.2f,
                            center.x, center.y + flameHeight * 0.4f
                        )
                        cubicTo(
                            center.x - flameWidth * 0.3f, center.y + flameHeight * 0.2f,
                            center.x - flameWidth * 0.2f, center.y + flameHeight * 0.05f,
                            center.x, center.y - flameHeight * 0.2f
                        )
                    }

                    val innerFlameColor = when (tier) {
                        "WOOD" -> Color(0xFFFFA726)
                        "BRONZE" -> Color(0xFFFFD180)
                        "SILVER" -> Color(0xFFE0F7FA)
                        "GOLD" -> Color(0xFFFFF9C4)
                        "PLATINUM" -> Color(0xFFE0F2F1)
                        "DIAMOND" -> Color(0xFFE0F7FA)
                        "RUBY" -> Color(0xFFFFEBEE)
                        "MASTER" -> Color(0xFFF3E5F5)
                        "LEGEND" -> Color(0xFFFFF3E0)
                        "UNREAL" -> Color(0xFFF1F8E9)
                        else -> Color.White
                    }

                    onDrawBehind {
                        // Draw background base circle
                        drawCircle(
                            color = if (isUnlocked) AppCard else AppCard.copy(alpha = 0.5f),
                            radius = outerRadius,
                            center = center
                        )

                        // Draw Tier Border
                        drawCircle(
                            color = tierColor.copy(alpha = alpha),
                            radius = outerRadius,
                            center = center,
                            style = borderStroke
                        )

                        // Grain/texture for streak tiers
                        when (tier) {
                            "WOOD" -> {
                                drawCircle(
                                    color = tierSecondary.copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.75f,
                                    center = center,
                                    style = woodStroke1
                                )
                                drawCircle(
                                    color = tierSecondary.copy(alpha = 0.15f * alpha),
                                    radius = outerRadius * 0.5f,
                                    center = center,
                                    style = woodStroke2
                                )
                            }
                            "BRONZE" -> {
                                drawCircle(
                                    color = tierColor.copy(alpha = 0.1f * alpha),
                                    radius = outerRadius * 0.8f,
                                    center = center
                                )
                            }
                            "SILVER" -> {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.15f * alpha),
                                    radius = outerRadius * 0.8f,
                                    center = center
                                )
                            }
                            "GOLD" -> {
                                drawCircle(
                                    color = Color(0xFFFFE082).copy(alpha = 0.2f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "PLATINUM" -> {
                                drawCircle(
                                    color = Color(0xFFE0F7FA).copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "DIAMOND" -> {
                                drawCircle(
                                    color = Color(0xFFE0F7FA).copy(alpha = 0.35f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "RUBY" -> {
                                drawCircle(
                                    color = Color(0xFFFFCDD2).copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "MASTER" -> {
                                drawCircle(
                                    color = Color(0xFFE1BEE7).copy(alpha = 0.25f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "LEGEND" -> {
                                drawCircle(
                                    color = Color(0xFFFFE0B2).copy(alpha = 0.3f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                            "UNREAL" -> {
                                drawCircle(
                                    color = Color(0xFFDCEDC8).copy(alpha = 0.35f * alpha),
                                    radius = outerRadius * 0.85f,
                                    center = center
                                )
                            }
                        }

                        // Draw Streak Flame
                        drawPath(
                            path = path,
                            color = tierColor.copy(alpha = alpha)
                        )

                        // Inner flame core
                        drawPath(
                            path = innerPath,
                            color = innerFlameColor.copy(alpha = 0.8f * alpha)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun MasteredGoalTrophyIcon(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
            val strokeWidth = 2.5f * density

            val goldColor = Color(0xFFFFD700)
            val deepGold = Color(0xFFC69200)
            val brightGold = Color(0xFFFFF176)
            val laurelGreen = Color(0xFF81C784)

            onDrawBehind {
                // Background golden glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(goldColor.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = width * 0.48f
                    ),
                    radius = width * 0.48f,
                    center = center
                )

                // Outer decorative ring
                drawCircle(
                    color = goldColor.copy(alpha = 0.4f),
                    radius = width * 0.42f,
                    center = center,
                    style = Stroke(width = 1.5f * density)
                )

                // Trophy Cup Base
                val baseRect = androidx.compose.ui.geometry.Rect(
                    center.x - width * 0.18f,
                    center.y + height * 0.28f,
                    center.x + width * 0.18f,
                    center.y + height * 0.36f
                )
                drawRoundRect(
                    color = deepGold,
                    topLeft = baseRect.topLeft,
                    size = baseRect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * density, 4f * density)
                )

                // Trophy Stem
                val stemRect = androidx.compose.ui.geometry.Rect(
                    center.x - width * 0.05f,
                    center.y + height * 0.15f,
                    center.x + width * 0.05f,
                    center.y + height * 0.29f
                )
                drawRect(
                    color = goldColor,
                    topLeft = stemRect.topLeft,
                    size = stemRect.size
                )

                // Trophy Cup Body
                val cupPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - width * 0.24f, center.y - height * 0.22f)
                    lineTo(center.x + width * 0.24f, center.y - height * 0.22f)
                    cubicTo(
                        center.x + width * 0.22f, center.y + height * 0.08f,
                        center.x + width * 0.12f, center.y + height * 0.16f,
                        center.x, center.y + height * 0.16f
                    )
                    cubicTo(
                        center.x - width * 0.12f, center.y + height * 0.16f,
                        center.x - width * 0.22f, center.y + height * 0.08f,
                        center.x - width * 0.24f, center.y - height * 0.22f
                    )
                    close()
                }
                drawPath(
                    path = cupPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(brightGold, goldColor, deepGold)
                    )
                )

                // Cup handles (left and right)
                val handleStroke = Stroke(width = strokeWidth)
                val leftHandle = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - width * 0.22f, center.y - height * 0.16f)
                    cubicTo(
                        center.x - width * 0.38f, center.y - height * 0.16f,
                        center.x - width * 0.38f, center.y + height * 0.04f,
                        center.x - width * 0.16f, center.y + height * 0.06f
                    )
                }
                drawPath(leftHandle, color = goldColor, style = handleStroke)

                val rightHandle = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x + width * 0.22f, center.y - height * 0.16f)
                    cubicTo(
                        center.x + width * 0.38f, center.y - height * 0.16f,
                        center.x + width * 0.38f, center.y + height * 0.04f,
                        center.x + width * 0.16f, center.y + height * 0.06f
                    )
                }
                drawPath(rightHandle, color = goldColor, style = handleStroke)

                // Star on Cup
                val starPath = androidx.compose.ui.graphics.Path().apply {
                    val starCenter = androidx.compose.ui.geometry.Offset(center.x, center.y - height * 0.04f)
                    val outerR = width * 0.08f
                    val innerR = outerR * 0.45f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val angle = (i * 36 - 90) * (Math.PI / 180.0)
                        val x = (starCenter.x + r * Math.cos(angle)).toFloat()
                        val y = (starCenter.y + r * Math.sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(starPath, color = Color.White.copy(alpha = 0.95f))
            }
        }
    )
}

