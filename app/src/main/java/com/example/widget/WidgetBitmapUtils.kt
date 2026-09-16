package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.ui.HabitIconMapping

object WidgetBitmapUtils {

    fun getColorInt(colorName: String): Int {
        return com.example.ui.HabitIconMapping.getColorInt(colorName)
    }

    fun drawProgressBarBitmap(progressPercent: Int, isCompleted: Boolean, accentColorInt: Int, isDark: Boolean): Bitmap {
        val width = 400
        val height = 24
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cornerRadius = 12f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // 1. Background Track
        val trackPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isDark) Color.parseColor("#20FFFFFF") else Color.parseColor("#EAEAEF")
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackPaint)

        // 2. Progress Fill
        if (progressPercent > 0) {
            val fillColor = if (isCompleted) Color.parseColor("#10B981") else accentColorInt
            val fillPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = fillColor
            }
            val fillWidth = width.toFloat() * (progressPercent.coerceIn(0, 100) / 100f)

            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRect(0f, 0f, fillWidth, height.toFloat(), fillPaint)
            canvas.restore()
        }

        // 3. Subtle Border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = if (isDark) Color.parseColor("#2C2C38") else Color.parseColor("#E5E5ED")
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        return bitmap
    }

    fun drawIconToBitmap(context: Context, iconName: String, colorInt: Int): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val resId = HabitIconMapping.getIconDrawableId(iconName)
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val wrapped = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(wrapped, colorInt)
            wrapped.setBounds(4, 4, size - 4, size - 4)
            wrapped.draw(canvas)
        }
        return bitmap
    }

    fun drawWidgetBackgroundBitmapStatic(isDark: Boolean, opacity: Float): Bitmap {
        val width = 400
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cornerRadius = 32f
        val rect = RectF(1.5f, 1.5f, width.toFloat() - 1.5f, height.toFloat() - 1.5f)

        val alphaInt = (opacity.coerceIn(0f, 1f) * 255).toInt()

        if (alphaInt > 0) {
            val r = if (isDark) 22 else 255
            val g = if (isDark) 22 else 255
            val b = if (isDark) 30 else 255
            val fillColor = Color.argb(alphaInt, r, g, b)

            val fillPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = fillColor
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)

            if (opacity > 0.1f) {
                val sr = if (isDark) 44 else 229
                val sg = if (isDark) 44 else 229
                val sb = if (isDark) 56 else 237
                val strokeAlpha = (alphaInt * 0.85f).toInt().coerceIn(0, 255)
                val strokeColor = Color.argb(strokeAlpha, sr, sg, sb)

                val borderPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = strokeColor
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            }
        }

        return bitmap
    }
}
