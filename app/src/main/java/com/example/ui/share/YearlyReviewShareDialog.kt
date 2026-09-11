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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.YearlyReviewData
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReviewShareDialog(
    year: Int,
    reviewData: YearlyReviewData,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = PrimaryViolet

    // Render bitmap
    val bitmap = remember(year, reviewData, accentColor, language) {
        renderYearlyReviewShareBitmap(
            context = context,
            year = year,
            reviewData = reviewData,
            language = language,
            accentColor = accentColor
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
                    text = tr(language, "Jahresrückblick teilen", "წლიური მიმოხილვის გაზიარება", "分享年度回顾", "Share Yearly Review"),
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
                    contentDescription = "Yearly Review Card Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Share
            Button(
                onClick = {
                    val title = tr(language, "Jahresrückblick $year", "$year მიმოხილვა", "${year}年度回顾", "$year Review")
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
                    text = tr(language, "Rückblick jetzt teilen", "გააზიარეთ მიმოხილვა ახლა", "立即分享回顾", "Share Review Now"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun renderYearlyReviewShareBitmap(
    context: Context,
    year: Int,
    reviewData: YearlyReviewData,
    language: String,
    accentColor: Color
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

    val borderPaint = Paint().apply {
        isAntiAlias = true
        color = primaryInt
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }
    val cardRect = RectF(60f, 60f, width - 60f, height - 60f)
    canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

    var currentY = 160f

    // Header
    val headerPaint = Paint().apply {
        isAntiAlias = true
        color = primaryInt
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.15f
    }
    canvas.drawText(tr(language, "JAHRESRÜCKBLICK", "წლიური მიმოხილვა", "年度回顾", "YEAR IN REVIEW"), width / 2f, currentY, headerPaint)
    currentY += 150f

    // Giant Display Date
    val datePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 80f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("✨ $year ✨", width / 2f, currentY, datePaint)
    currentY += 150f

    // Stats Grid Divider
    val sepPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(64, 255, 255, 255)
        strokeWidth = 3f
    }
    canvas.drawLine(150f, currentY, width - 150f, currentY, sepPaint)
    currentY += 110f

    // Stats rows
    val statPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val totalText = "🏆  ${reviewData.totalCompletions}  ${tr(language, "Abschlüsse", "დასრულებები", "完成次数", "Completions")}"
    canvas.drawText(totalText, width / 2f, currentY, statPaint)
    currentY += 80f

    val checkInsText = "✅  ${reviewData.totalCheckIns}  ${tr(language, "Check-ins insgesamt", "სულ შემოწმება", "累计打卡", "Total Check-ins")}"
    canvas.drawText(checkInsText, width / 2f, currentY, statPaint)
    currentY += 80f

    val activeText = "📅  ${reviewData.activeDaysCount} (${reviewData.activeDaysPercentage}%)  ${tr(language, "Aktive Tage", "აქტიური დღეები", "活跃天数", "Active Days")}"
    canvas.drawText(activeText, width / 2f, currentY, statPaint)
    currentY += 80f

    if (reviewData.topHabit != null) {
        val topText = "👑  ${reviewData.topHabit.icon} ${reviewData.topHabit.name}  ${tr(language, "Top Gewohnheit", "მთავარი ჩვევა", "最佳习惯", "Top Habit")}"
        canvas.drawText(topText, width / 2f, currentY, statPaint)
        currentY += 90f
    }

    val monthText = "🗓️  ${reviewData.bestMonthName}  ${tr(language, "Bester Monat", "საუკეთესო თვე", "最佳月份", "Best Month")}"
    canvas.drawText(monthText, width / 2f, currentY, statPaint)
    currentY += 90f

    val streakText = "🔥  ${reviewData.longestStreak} ${tr(language, "Tage Serie", "დღეების სერია", "天连续", "Days Streak")}  ${tr(language, "Beste Serie", "საუკეთესო სერია", "最高连续", "Best Streak")}"
    canvas.drawText(streakText, width / 2f, currentY, statPaint)

    // Footer Branding
    drawFooterBranding(canvas, context, width, height)

    return bitmap
}
