package com.example.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.tr
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

fun isNewYearReviewPeriod(): Boolean {
    val today = LocalDate.now()
    val month = today.monthValue
    return month == 1
}

fun isNewYearPopupPeriod(): Boolean {
    val today = LocalDate.now()
    val month = today.monthValue
    val day = today.dayOfMonth
    // Popup appears a few days before New Year until Jan 5: Dec 27 to Jan 5
    return (month == 12 && day >= 27) || (month == 1 && day <= 5)
}

/**
 * Helper to share a bitmap image card via Android Native Share Sheet
 */
fun shareBitmapImage(context: Context, bitmap: Bitmap, title: String, textSummary: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "share_card_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, textSummary)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Log.e("ShareDialogs", "Error sharing image", e)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\n$textSummary")
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

fun drawFooterBranding(
    canvas: Canvas,
    context: Context?,
    width: Int,
    height: Int
) {
    val logoDrawable = if (context != null) {
        try {
            val resId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
            if (resId != 0) androidx.core.content.ContextCompat.getDrawable(context, resId) else null
        } catch (e: Exception) {
            null
        } ?: try {
            androidx.core.content.ContextCompat.getDrawable(context, com.frequent.habits.R.mipmap.ic_launcher)
                ?: androidx.core.content.ContextCompat.getDrawable(context, com.frequent.habits.R.mipmap.ic_launcher_round)
        } catch (e: Exception) {
            null
        }
    } else null

    val footerY = height - 130f
    val logoSize = 72
    val appTitle = "Frequent Habits"

    val footerPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(230, 255, 255, 255)
        textSize = 38f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    if (logoDrawable != null) {
        val logoBitmap = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(logoBitmap)
        logoDrawable.setBounds(0, 0, logoSize, logoSize)
        logoDrawable.draw(logoCanvas)

        val gap = 20f
        val textWidth = footerPaint.measureText(appTitle)
        val totalWidth = logoSize + gap + textWidth
        val startX = (width - totalWidth) / 2f

        val logoY = footerY - logoSize / 2f
        canvas.drawBitmap(logoBitmap, startX, logoY, null)

        val textX = startX + logoSize + gap
        val textBounds = android.graphics.Rect()
        footerPaint.getTextBounds(appTitle, 0, appTitle.length, textBounds)
        val textY = footerY + (textBounds.height() / 2f) - textBounds.bottom
        canvas.drawText(appTitle, textX, textY, footerPaint)
    } else {
        footerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(appTitle, width / 2f, footerY, footerPaint)
    }
}

fun getSocialShareText(language: String): String {
    return tr(language, "Ich baue bessere Routinen auf und verfolge meine täglichen Ziele mit Frequent Habits! 🚀 Werde auch du produktiver und gestalte deine perfekte Routine. Lade die App hier herunter: https://github.com/FrequeNCy144/Frequent-Habits", "მე ვაშენებ უკეთეს რუტინას და თვალყურს ვადევნებ ჩემს ყოველდღიურ მიზნებს ხშირი ჩვევებით! 🚀 შემომიერთდით პოზიტიური ჩვევების ჩამოყალიბებაში ყოველდღე. ჩამოტვირთეთ აპლიკაცია აქ: https://github.com/FrequeNCy144/Frequent-Habits", "我正在使用「Frequent Habits」建立更好的习惯并追踪每日目标！🚀 一起开启高效自律每一天。下载应用：https://github.com/FrequeNCy144/Frequent-Habits", "I'm building better routines and tracking my daily goals with Frequent Habits! 🚀 Join me in shaping positive habits every day. Download the app here: https://github.com/FrequeNCy144/Frequent-Habits")
}

fun mixColorWithBlack(color: Int, ratio: Float): Int {
    val a = android.graphics.Color.alpha(color)
    val r = (android.graphics.Color.red(color) * ratio).toInt()
    val g = (android.graphics.Color.green(color) * ratio).toInt()
    val b = (android.graphics.Color.blue(color) * ratio).toInt()
    return android.graphics.Color.argb(a, r, g, b)
}

fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    if (uriString.isBlank()) return null
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        Log.e("ShareDialogs", "Error loading bitmap from URI: $uriString", e)
        null
    }
}

fun drawCircularAvatar(canvas: Canvas, bitmap: Bitmap, centerX: Float, centerY: Float, radius: Float, borderPaint: Paint?) {
    try {
        val size = (radius * 2).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }
        outputCanvas.drawARGB(0, 0, 0, 0)
        paint.color = 0xff424242.toInt()
        outputCanvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        val rect = android.graphics.Rect(0, 0, size, size)
        outputCanvas.drawBitmap(scaled, rect, rect, paint)
        
        canvas.drawBitmap(output, centerX - radius, centerY - radius, null)
        if (borderPaint != null) {
            canvas.drawCircle(centerX, centerY, radius, borderPaint)
        }
    } catch (e: Exception) {
        Log.e("ShareDialogs", "Failed to draw circular avatar", e)
    }
}
