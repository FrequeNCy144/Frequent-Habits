package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.tr
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter




@Composable
fun HabitScoreTrendCard(
    habit: Habit,
    logs: List<HabitLog>,
    language: String,
    onInfoClick: (String, String) -> Unit
) {
    var selectedTimeframeIndex by rememberSaveable(habit.id) { mutableIntStateOf(0) }

    val trendPoints = remember(habit, logs, selectedTimeframeIndex) {
        calculateHabitTrendPoints(habit, logs, selectedTimeframeIndex)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(PrimaryViolet.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Score Trend",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "HABIT SCORE TREND", "ჩვევების ქულის ტენდენცია", "习惯得分趋势", "HABIT SCORE TREND"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Habit Score Trend", "ჩვევების ქულის ტენდენცია", "习惯得分趋势", "Habit Score Trend"),
                    explanation = if (language == "de") "Zeigt die historische Entwicklung deines Gewohnheits-Scores (0–100) im gewählten Zeitraum." else "Shows the historical progression of your habit strength score (0–100) over time.",
                    onClick = onInfoClick
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(language, "Interaktiv", "ინტერაქტიული", "交互式", "Interactive"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TimeframeSelectorPills(
                selectedTimeframeIndex = selectedTimeframeIndex,
                onTimeframeSelected = { selectedTimeframeIndex = it },
                language = language,
                customLabels = if (language == "de") {
                    listOf("Wöchentlich", "Monatlich", "Jährlich")
                } else {
                    listOf("Weekly", "Monthly", "Yearly")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ScoreTrendLineChart(points = trendPoints)
        }
    }
}


@Composable
fun ScoreTrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryViolet
) {
    if (points.isEmpty()) return

    var selectedPointIndex by remember(points) { mutableStateOf<Int?>(null) }
    val isDark = isSystemInDarkTheme()
    val textSecArgb = TextSecondary.toArgb()
    val borderColor = AppBorder.copy(alpha = if (isDark) 0.35f else 0.5f)
    val appBgColor = AppBg

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val paddingLeft = 32.dp.toPx()
                        val paddingRight = 14.dp.toPx()
                        val paddingTop = 22.dp.toPx()
                        val paddingBottom = 26.dp.toPx()
                        val graphWidth = size.width - paddingLeft - paddingRight
                        val graphHeight = size.height - paddingTop - paddingBottom
                        val stepX = if (points.size > 1) graphWidth / (points.size - 1) else graphWidth

                        val clickedIndex = points.mapIndexed { idx, pt ->
                            val x = paddingLeft + (idx * stepX)
                            val y = paddingTop + (1f - (pt.score.coerceIn(0f, 100f) / 100f)) * graphHeight
                            val distance = Math.hypot((offset.x - x).toDouble(), (offset.y - y).toDouble())
                            idx to distance
                        }.minByOrNull { it.second }

                        if (clickedIndex != null && clickedIndex.second < 44.dp.toPx()) {
                            selectedPointIndex = clickedIndex.first
                        } else {
                            selectedPointIndex = null
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 32.dp.toPx()
            val paddingRight = 14.dp.toPx()
            val paddingTop = 22.dp.toPx()
            val paddingBottom = 26.dp.toPx()
            val graphWidth = width - paddingLeft - paddingRight
            val graphHeight = height - paddingTop - paddingBottom
            val baselineY = paddingTop + graphHeight

            val yAxisPaint = android.graphics.Paint().apply {
                color = textSecArgb
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }

            val xAxisPaint = android.graphics.Paint().apply {
                color = textSecArgb
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }

            // 1. Grid Lines & Y-Axis Labels (100, 75, 50, 25, 0)
            val yLabels = listOf("100", "75", "50", "25", "0")
            for (i in 0..4) {
                val ratio = i / 4f
                val y = paddingTop + (ratio * graphHeight)

                // Dashed horizontal line
                drawLine(
                    color = borderColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Y label text vertically centered on grid line
                val label = yLabels[i]
                val textBounds = android.graphics.Rect()
                yAxisPaint.getTextBounds(label, 0, label.length, textBounds)
                val textY = y + (textBounds.height() / 2f)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    paddingLeft - 6.dp.toPx(),
                    textY,
                    yAxisPaint
                )
            }

            // 2. Data Points Coordinates
            val stepX = if (points.size > 1) graphWidth / (points.size - 1) else graphWidth
            val pathPoints = points.mapIndexed { idx, pt ->
                val x = paddingLeft + (idx * stepX)
                val y = paddingTop + (1f - (pt.score.coerceIn(0f, 100f) / 100f)) * graphHeight
                Offset(x, y)
            }

            // 3. Smooth Line & Gradient Fill
            if (pathPoints.size > 1) {
                val linePath = Path().apply {
                    moveTo(pathPoints[0].x, pathPoints[0].y)
                    for (i in 1 until pathPoints.size) {
                        val prev = pathPoints[i - 1]
                        val curr = pathPoints[i]
                        val controlX1 = prev.x + (curr.x - prev.x) / 2f
                        val controlY1 = prev.y
                        val controlX2 = prev.x + (curr.x - prev.x) / 2f
                        val controlY2 = curr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(pathPoints.last().x, baselineY)
                    lineTo(pathPoints.first().x, baselineY)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.02f)
                        ),
                        startY = paddingTop,
                        endY = baselineY
                    )
                )

                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Point Circles & X-Axis Labels
            pathPoints.forEachIndexed { idx, pt ->
                // Outer glow / background border circle
                drawCircle(
                    color = appBgColor,
                    radius = 5.5.dp.toPx(),
                    center = pt
                )
                // Accent dot
                drawCircle(
                    color = accentColor,
                    radius = 3.5.dp.toPx(),
                    center = pt
                )

                // X-Axis label centered under data point
                if (idx in points.indices) {
                    val label = points[idx].label
                    val labelY = height - 4.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        pt.x,
                        labelY,
                        xAxisPaint
                    )
                }
            }

            // 5. Interactive Tooltip on Tap
            selectedPointIndex?.let { idx ->
                if (idx in pathPoints.indices && idx in points.indices) {
                    val pt = pathPoints[idx]
                    val trendPoint = points[idx]
                    val text = "${trendPoint.score.toInt()}"

                    val tooltipTextPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    val tooltipBgPaint = android.graphics.Paint().apply {
                        color = accentColor.toArgb()
                        isAntiAlias = true
                    }

                    val textBounds = android.graphics.Rect()
                    tooltipTextPaint.getTextBounds(text, 0, text.length, textBounds)

                    val tooltipWidth = textBounds.width() + 32f
                    val tooltipHeight = textBounds.height() + 20f

                    val rect = android.graphics.RectF(
                        pt.x - tooltipWidth / 2f,
                        pt.y - tooltipHeight - 24f,
                        pt.x + tooltipWidth / 2f,
                        pt.y - 8f
                    )

                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        rect, 14f, 14f, tooltipBgPaint
                    )

                    val arrowPath = android.graphics.Path().apply {
                        moveTo(pt.x - 8f, pt.y - 9f)
                        lineTo(pt.x + 8f, pt.y - 9f)
                        lineTo(pt.x, pt.y - 2f)
                        close()
                    }
                    drawContext.canvas.nativeCanvas.drawPath(arrowPath, tooltipBgPaint)

                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        pt.x,
                        pt.y - (tooltipHeight / 2f) - 18f + (textBounds.height() / 2f),
                        tooltipTextPaint
                    )
                }
            }
        }
    }
}


@Composable
fun OverallScoreTrendCard(
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    language: String,
    accentColor: Color = PrimaryViolet,
    onInfoClick: (String, String) -> Unit
) {
    var selectedTimeframeIndex by rememberSaveable { mutableIntStateOf(0) }

    val trendPoints = remember(allHabits, allLogs, selectedTimeframeIndex) {
        calculateOverallTrendPoints(allHabits, allLogs, selectedTimeframeIndex)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Score Trend",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "GESAMT SCORE TREND", "საერთო ქულის ტენდენცია", "综合得分趋势", "OVERALL SCORE TREND"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoIconButton(
                    title = tr(language, "Gesamt Score Trend", "საერთო ქულის ტენდენცია", "综合得分趋势", "Overall Score Trend"),
                    explanation = if (language == "de") "Zeigt die historische Entwicklung deines Gesamt-Konto-Scores (0–100) über alle Gewohnheiten." else "Shows the historical progression of your overall account strength score (0–100) across all habits.",
                    onClick = onInfoClick
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(language, "Interaktiv", "ინტერაქტიული", "交互式", "Interactive"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TimeframeSelectorPills(
                selectedTimeframeIndex = selectedTimeframeIndex,
                onTimeframeSelected = { selectedTimeframeIndex = it },
                language = language,
                customLabels = if (language == "de") {
                    listOf("Wöchentlich", "Monatlich", "Jährlich")
                } else {
                    listOf("Weekly", "Monthly", "Yearly")
                },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            ScoreTrendLineChart(points = trendPoints, accentColor = accentColor)
        }
    }
}
