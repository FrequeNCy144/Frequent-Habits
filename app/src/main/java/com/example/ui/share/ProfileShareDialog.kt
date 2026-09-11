package com.example.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileShareDialog(
    userName: String,
    profileImageUri: String,
    habits: List<Habit>,
    logs: List<HabitLog>,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = PrimaryViolet

    // Calculate metrics
    val totalCompletions = remember(habits, logs) {
        logs.count { log ->
            val habit = habits.find { it.id == log.habitId }
            habit != null && com.example.data.isLogCompleted(habit, log)
        }
    }
    val totalCheckIns = remember(logs) {
        logs.count { it.value != 0f }
    }
    val activeHabitsCount = remember(habits) { habits.count { !it.isArchived } }
    val longestStreak = remember(habits, logs) {
        habits.maxOfOrNull { h ->
            val hLogs = logs.filter { it.habitId == h.id }.map { it.date }.toSet()
            var streak = 0
            var curr = LocalDate.now()
            while (hLogs.contains(curr.toString())) {
                streak++
                curr = curr.minusDays(1)
            }
            streak
        } ?: 0
    }

    // Render bitmap
    val bitmap = remember(userName, profileImageUri, totalCompletions, totalCheckIns, activeHabitsCount, longestStreak, accentColor, language) {
        renderProfileShareBitmap(
            context = context,
            userName = userName,
            profileImageUri = profileImageUri,
            totalCompletions = totalCompletions,
            totalCheckIns = totalCheckIns,
            activeHabits = activeHabitsCount,
            longestStreak = longestStreak,
            accentColor = accentColor,
            language = language
        )
    }

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
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = tr(language, "Profilkarte teilen", "პროფილის გაზიარება", "分享个人主页", "Share Profile"),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Preview (using the exact rendered Bitmap!)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1080f / 1350f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profile Share Card Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Share
            Button(
                onClick = {
                    val title = tr(language, "Frequent Habits Profil", "ხშირი ჩვევების პროფილი", "Frequent Habits 习惯主页", "Frequent Habits Profile")
                    val summaryText = getSocialShareText(language)
                    shareBitmapImage(context, bitmap, title, summaryText)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(language, "Profilkarte teilen", "პროფილის ბარათის გაზიარება", "分享主页卡片", "Share Profile Card"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun renderProfileShareBitmap(
    context: Context,
    userName: String,
    profileImageUri: String,
    totalCompletions: Int,
    totalCheckIns: Int,
    activeHabits: Int,
    longestStreak: Int,
    accentColor: Color,
    language: String
): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val primaryInt = accentColor.toArgb()
    val bgStart = mixColorWithBlack(primaryInt, 0.15f)
    val bgEnd = mixColorWithBlack(primaryInt, 0.04f)

    val bgPaint = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            bgStart,
            bgEnd,
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Rounded card border
    val borderPaint = Paint().apply {
        isAntiAlias = true
        color = primaryInt
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }
    val cardRect = RectF(60f, 60f, width - 60f, height - 60f)
    canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

    var currentY = 160f

    // Header Title
    val headerPaint = Paint().apply {
        isAntiAlias = true
        color = primaryInt
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.15f
    }
    canvas.drawText(tr(language, "MEIN PROFIL", "ჩემი პროფილი", "我的个人主页", "MY PROFILE"), width / 2f, currentY, headerPaint)
    currentY += 150f

    // Profile Pic / Avatar
    val displayName = userName.ifBlank { tr(language, "Gewohnheiten Held", "ჩვევების გმირი", "习惯达人", "Habit Hero") }
    val avatarRadius = 110f
    val avatarPaint = Paint().apply {
        isAntiAlias = true
        color = primaryInt
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val avatarBitmap = loadBitmapFromUri(context, profileImageUri)
    if (avatarBitmap != null) {
        drawCircularAvatar(canvas, avatarBitmap, width / 2f, currentY, avatarRadius, avatarPaint)
    } else {
        // Placeholder
        val placeholderPaint = Paint().apply {
            isAntiAlias = true
            color = mixColorWithBlack(primaryInt, 0.35f)
        }
        canvas.drawCircle(width / 2f, currentY, avatarRadius, placeholderPaint)
        canvas.drawCircle(width / 2f, currentY, avatarRadius, avatarPaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 100f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val letter = if (displayName.isNotEmpty()) displayName.substring(0, 1).uppercase() else "H"
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(letter, 0, 1, textBounds)
        val yOffset = textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(letter, width / 2f, currentY + yOffset, textPaint)
    }
    currentY += 190f

    // User Name
    val namePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 72f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(displayName, width / 2f, currentY, namePaint)
    currentY += 110f

    // Horizontal Separator
    val sepPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(64, 255, 255, 255)
        strokeWidth = 3f
    }
    canvas.drawLine(150f, currentY, width - 150f, currentY, sepPaint)
    currentY += 110f

    // Stats
    val statPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val compText = "🏆  $totalCompletions  ${tr(language, "Abschlüsse insgesamt", "სულ შესრულებული", "累计完成", "Total Completions")}"
    canvas.drawText(compText, width / 2f, currentY, statPaint)
    currentY += 75f

    val checkInsText = "✅  $totalCheckIns  ${tr(language, "Check-ins insgesamt", "სულ შემოწმება", "累计打卡", "Total Check-ins")}"
    canvas.drawText(checkInsText, width / 2f, currentY, statPaint)
    currentY += 75f

    val habitsText = "📌  $activeHabits  ${tr(language, "Aktive Gewohnheiten", "აქტიური ჩვევები", "进行中习惯", "Active Habits")}"
    canvas.drawText(habitsText, width / 2f, currentY, statPaint)
    currentY += 75f

    val streakText = "🔥  $longestStreak  ${tr(language, "Tage beste Serie", "დღის საუკეთესო სერია", "最长连续天数", "Days Longest Streak")}"
    canvas.drawText(streakText, width / 2f, currentY, statPaint)

    // Footer Branding
    drawFooterBranding(canvas, context, width, height)

    return bitmap
}
