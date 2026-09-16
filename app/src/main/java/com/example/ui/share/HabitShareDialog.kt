package com.example.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.ui.components.AppTextField
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitShareDialog(
    habit: Habit,
    habitLogs: List<HabitLog>,
    userName: String,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Calculate Habit Stats
    val habitSpecificLogs = remember(habit.id, habitLogs) { habitLogs.filter { it.habitId == habit.id } }
    val totalCompletions = remember(habitSpecificLogs) { habitSpecificLogs.size }
    
    // Calculate streak
    val streakDays = remember(habitSpecificLogs) {
        if (habitSpecificLogs.isEmpty()) 0
        else {
            val logDates = habitSpecificLogs.map { it.date }.toSet()
            var current = LocalDate.now()
            var streak = 0
            while (logDates.contains(current.toString())) {
                streak++
                current = current.minusDays(1)
            }
            if (streak == 0 && logDates.contains(LocalDate.now().minusDays(1).toString())) {
                current = LocalDate.now().minusDays(1)
                while (logDates.contains(current.toString())) {
                    streak++
                    current = current.minusDays(1)
                }
            }
            streak
        }
    }

    // Completion rate in last 30 days
    val last30DaysRate = remember(habitSpecificLogs) {
        val today = LocalDate.now()
        val last30Set = (0 until 30).map { today.minusDays(it.toLong()).toString() }.toSet()
        val count = habitSpecificLogs.count { last30Set.contains(it.date) }
        ((count / 30f) * 100).toInt().coerceIn(0, 100)
    }

    // Density / Option Toggles
    var showStreak by remember { mutableStateOf(true) }
    var showCompletionRate by remember { mutableStateOf(true) }
    var showTarget by remember { mutableStateOf(true) }
    var showTotalCount by remember { mutableStateOf(true) }
    var showUserName by remember { mutableStateOf(true) }
    var showCustomMotto by remember { mutableStateOf(true) }
    var customMottoText by remember {
        mutableStateOf(tr(language, "Fokus & Disziplin jeden Tag! 💪", "ფოკუსირება და თანმიმდევრულობა ყოველდღე! 💪", "每天专注与坚持！💪", "Focus & Consistency every day! 💪"))
    }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }

    val currentTheme = habitShareThemes[selectedThemeIndex % habitShareThemes.size]

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { StandardSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = tr(language, "Gewohnheit teilen", "გაზიარება ჩვევა", "分享习惯", "Share Habit"),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LIVE PREVIEW CARD
                Text(
                    text = tr(language, "VORSCHAU", "გადახედვა", "卡片预览", "PREVIEW"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(currentTheme.bgColors)
                        )
                        .border(1.5.dp, currentTheme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Category Tag
                        Surface(
                            color = currentTheme.accentColor.copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = habit.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = currentTheme.accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Habit Name
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Target / Unit info
                        if (showTarget && habit.type == "NUMBER" && habit.unit.isNotEmpty()) {
                            Text(
                                text = tr(language, "Ziel: ${habit.targetValue} ${habit.unit}", "სამიზნე: ${habit.targetValue} ${habit.unit}", "目标：${habit.targetValue} ${habit.unit}", "Target: ${habit.targetValue} ${habit.unit}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metrics Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showStreak) {
                                MetricChip(
                                    icon = "🔥",
                                    label = "$streakDays ${tr(language, "Tage Streak", "დღეების სერია", "天连续", "Days Streak")}",
                                    accent = currentTheme.accentColor
                                )
                            }
                            if (showCompletionRate) {
                                MetricChip(
                                    icon = "📈",
                                    label = "$last30DaysRate% ${tr(language, "30T Erfolge", "30d წარმატება", "30天成功率", "30d Success")}",
                                    accent = currentTheme.accentColor
                                )
                            }
                            if (showTotalCount) {
                                MetricChip(
                                    icon = "✅",
                                    label = "$totalCompletions ${tr(language, "mal absolviert", "ჯერ შესრულებული", "次完成", "times done")}",
                                    accent = currentTheme.accentColor
                                )
                            }
                        }

                        // Custom Motto Quote
                        if (showCustomMotto && customMottoText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "$customMottoText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // User Badge
                        if (showUserName && userName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "— $userName",
                                style = MaterialTheme.typography.labelMedium,
                                color = currentTheme.accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // INFORMATION DENSITY & CUSTOMIZATION CONTROLS
                Text(
                    text = tr(language, "INFORMATIONSDICHTE ANPASSEN", "ინფორმაციის სიმკვრივის მორგება", "自定义显示信息", "CUSTOMIZE INFORMATION DENSITY"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                // Theme selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    habitShareThemes.forEachIndexed { idx, theme ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(theme.bgColors))
                                .border(
                                    width = if (selectedThemeIndex == idx) 2.dp else 1.dp,
                                    color = if (selectedThemeIndex == idx) theme.accentColor else TextSecondary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedThemeIndex = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = if (selectedThemeIndex == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Toggles
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ProgressTrack)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DensityToggleRow(
                        label = tr(language, "🔥 Serie / Streak anzeigen", "🔥 სერიის ჩვენება", "🔥 显示连续天数", "🔥 Show Streak"),
                        checked = showStreak,
                        onCheckedChange = { showStreak = it }
                    )
                    DensityToggleRow(
                        label = tr(language, "📈 Erfolgsquote (30 Tage) anzeigen", "📈 წარმატების მაჩვენებლის ჩვენება", "📈 显示成功率 (30天)", "📈 Show Success Rate"),
                        checked = showCompletionRate,
                        onCheckedChange = { showCompletionRate = it }
                    )
                    DensityToggleRow(
                        label = tr(language, "✅ Gesamtzahl der Abschlüsse anzeigen", "✅ სრული დასრულებების ჩვენება", "✅ 显示累计完成次数", "✅ Show Total Completions"),
                        checked = showTotalCount,
                        onCheckedChange = { showTotalCount = it }
                    )
                    if (habit.type == "NUMBER" && habit.unit.isNotEmpty()) {
                        DensityToggleRow(
                            label = tr(language, "🎯 Tagesziel & Einheit anzeigen", "🎯 სამიზნისა და ერთეულის ჩვენება", "🎯 显示每日目标与单位", "🎯 Show Target & Unit"),
                            checked = showTarget,
                            onCheckedChange = { showTarget = it }
                        )
                    }
                    DensityToggleRow(
                        label = tr(language, "👤 Deinen Profilnamen anzeigen", "👤 პროფილის სახელის ჩვენება", "👤 显示用户名", "👤 Show Profile Name"),
                        checked = showUserName,
                        onCheckedChange = { showUserName = it }
                    )
                    DensityToggleRow(
                        label = tr(language, "💬 Eigenen Spruch / Motto anzeigen", "💬 მორგებული ციტატის ჩვენება", "💬 显示个人格言", "💬 Show Custom Quote"),
                        checked = showCustomMotto,
                        onCheckedChange = { showCustomMotto = it }
                    )
                }

                // Editable Quote / Motto input field
                if (showCustomMotto) {
                    AppTextField(
                        value = customMottoText,
                        onValueChange = { customMottoText = it },
                        labelText = tr(language, "Persönliche Notiz / Motto", "პირადი ციტატა", "个人格言 / 寄语", "Personal Quote"),
                        containerColor = Color.Black.copy(alpha = 0.2f),
                        accentColor = currentTheme.accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        singleLine = true,
                        testTag = "share_quote_input"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Share
            Button(
                onClick = {
                    val bitmap = renderHabitShareBitmap(
                        habit = habit,
                        streakDays = if (showStreak) streakDays else null,
                        completionRate = if (showCompletionRate) last30DaysRate else null,
                        totalCompletions = if (showTotalCount) totalCompletions else null,
                        showTarget = showTarget,
                        userName = if (showUserName) userName else null,
                        customMotto = if (showCustomMotto) customMottoText else null,
                        theme = currentTheme,
                        language = language,
                        context = context
                    )
                    val textSummary = buildString {
                        append("🔥 ")
                        append(habit.name)
                        if (showStreak) append(" • $streakDays Tage Streak")
                        if (showCompletionRate) append(" • $last30DaysRate% Erfolgsquote")
                        append("\nShared via Everyday Habits App")
                    }
                    shareBitmapImage(context, bitmap, habit.name, textSummary)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentTheme.accentColor,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "Gewohnheitskarte teilen", "გააზიარეთ ჩვევების ბარათი", "分享习惯成就卡", "Share Habit Card"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun renderHabitShareBitmap(
    habit: Habit,
    streakDays: Int?,
    completionRate: Int?,
    totalCompletions: Int?,
    showTarget: Boolean,
    userName: String?,
    customMotto: String?,
    theme: ShareThemePreset,
    language: String,
    context: Context? = null
): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background Gradient Fill
    val bgPaint = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            theme.bgColors.first().toArgb(),
            theme.bgColors.last().toArgb(),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Border Card
    val cardPaint = Paint().apply {
        isAntiAlias = true
        color = theme.accentColor.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }
    val cardRect = RectF(60f, 60f, width - 60f, height - 60f)
    canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

    var currentY = 220f

    // Category
    val catPaint = Paint().apply {
        isAntiAlias = true
        color = theme.accentColor.toArgb()
        textSize = 38f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(habit.category.uppercase(), width / 2f, currentY, catPaint)
    currentY += 120f

    // Habit Title
    val titlePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 72f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(habit.name, width / 2f, currentY, titlePaint)
    currentY += 90f

    if (showTarget && habit.type == "NUMBER" && habit.unit.isNotEmpty()) {
        val targetPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.LTGRAY
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        val targetText = tr(language, "Ziel: ${habit.targetValue} ${habit.unit}", "სამიზნე: ${habit.targetValue} ${habit.unit}", "目标：${habit.targetValue} ${habit.unit}", "Target: ${habit.targetValue} ${habit.unit}")
        canvas.drawText(targetText, width / 2f, currentY, targetPaint)
        currentY += 80f
    }

    currentY += 60f

    // Metrics
    val metricPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    if (streakDays != null) {
        val text = "🔥 $streakDays ${tr(language, "Tage Streak", "დღეების სერია", "天连续", "Days Streak")}"
        canvas.drawText(text, width / 2f, currentY, metricPaint)
        currentY += 80f
    }

    if (completionRate != null) {
        val text = "📈 $completionRate% ${tr(language, "Erfolgsquote", "წარმატების მაჩვენებელი", "成功率", "Success Rate")}"
        canvas.drawText(text, width / 2f, currentY, metricPaint)
        currentY += 80f
    }

    if (totalCompletions != null) {
        val text = "✅ $totalCompletions ${tr(language, "mal geschafft", "ჯერ დასრულებული", "次完成", "times completed")}"
        canvas.drawText(text, width / 2f, currentY, metricPaint)
        currentY += 80f
    }

    if (!customMotto.isNullOrBlank()) {
        currentY += 40f
        val mottoPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor.toArgb()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$customMotto", width / 2f, currentY, mottoPaint)
        currentY += 80f
    }

    if (!userName.isNullOrBlank()) {
        val userPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— $userName", width / 2f, height - 140f, userPaint)
    }

    // App Branding Footer
    drawFooterBranding(canvas, context, width, height)

    return bitmap
}
