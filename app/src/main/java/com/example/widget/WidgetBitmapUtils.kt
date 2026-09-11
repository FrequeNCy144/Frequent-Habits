package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object WidgetBitmapUtils {

    fun getColorInt(colorName: String): Int {
        return when (colorName.lowercase()) {
            "blue" -> 0xFF3B82F6.toInt()
            "purple" -> 0xFF9333EA.toInt()
            "cyan" -> 0xFF06B6D4.toInt()
            "green" -> 0xFF10B981.toInt()
            "yellow" -> 0xFFF59E0B.toInt()
            "orange" -> 0xFFF97316.toInt()
            "red" -> 0xFFEF4444.toInt()
            "pink" -> 0xFFEC4899.toInt()
            "slate", "grey", "gray" -> 0xFF64748B.toInt()
            else -> 0xFF7356FF.toInt() // Default to PrimaryViolet
        }
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
        val paint = Paint().apply {
            color = colorInt
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 3f
        }
        
        val half = size / 2f
        
        when (iconName.lowercase()) {
            "sparkle" -> {
                val path = Path().apply {
                    moveTo(half, 4f)
                    quadTo(half, half, size - 4f, half)
                    quadTo(half, half, half, size - 4f)
                    quadTo(half, half, 4f, half)
                    quadTo(half, half, half, 4f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "moon" -> {
                val path = Path().apply {
                    addCircle(half + 4f, half, half - 6f, Path.Direction.CW)
                    val subtraction = Path().apply {
                        addCircle(half, half, half - 6f, Path.Direction.CW)
                    }
                    op(subtraction, Path.Op.DIFFERENCE)
                }
                canvas.drawPath(path, paint)
            }
            "sun" -> {
                canvas.drawCircle(half, half, size / 5f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4
                    val startX = half + (size / 3.5f) * Math.cos(angle).toFloat()
                    val startY = half + (size / 3.5f) * Math.sin(angle).toFloat()
                    val endX = half + (size / 2.2f) * Math.cos(angle).toFloat()
                    val endY = half + (size / 2.2f) * Math.sin(angle).toFloat()
                    canvas.drawLine(startX, startY, endX, endY, paint)
                }
            }
            "water" -> {
                val path = Path().apply {
                    moveTo(half, 6f)
                    cubicTo(size - 8f, half + 4f, size - 8f, size - 6f, half, size - 6f)
                    cubicTo(8f, size - 6f, 8f, half + 4f, half, 6f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "heart" -> {
                val heartPath = Path().apply {
                    moveTo(half, size * 0.3f)
                    cubicTo(size * 0.2f, size * 0.05f, size * 0.02f, size * 0.25f, size * 0.05f, size * 0.5f)
                    cubicTo(size * 0.08f, size * 0.75f, size * 0.35f, size * 0.9f, half, size * 0.95f)
                    cubicTo(size * 0.65f, size * 0.9f, size * 0.92f, size * 0.75f, size * 0.95f, size * 0.5f)
                    cubicTo(size * 0.98f, size * 0.25f, size * 0.8f, size * 0.05f, half, size * 0.3f)
                    close()
                }
                canvas.drawPath(heartPath, paint)
            }
            "dumbbell" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                canvas.drawLine(8f, half, size - 8f, half, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(10f, half, 8f, paint)
                canvas.drawCircle(size - 10f, half, 8f, paint)
            }
            "book" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val path = Path().apply {
                    moveTo(half, size - 10f)
                    lineTo(half, 10f)
                }
                canvas.drawPath(path, paint)
                val leftPage = Path().apply {
                    moveTo(half, 12f)
                    cubicTo(half - 8f, 8f, 8f, 8f, 8f, 12f)
                    lineTo(8f, size - 12f)
                    cubicTo(8f, size - 16f, half - 8f, size - 16f, half, size - 12f)
                }
                canvas.drawPath(leftPage, paint)
                val rightPage = Path().apply {
                    moveTo(half, 12f)
                    cubicTo(half + 8f, 8f, size - 8f, 8f, size - 8f, 12f)
                    lineTo(size - 8f, size - 12f)
                    cubicTo(size - 8f, size - 16f, half + 8f, size - 16f, half, size - 12f)
                }
                canvas.drawPath(rightPage, paint)
            }
            "coffee" -> {
                val cup = Path().apply {
                    moveTo(10f, 16f)
                    lineTo(size - 14f, 16f)
                    cubicTo(size - 14f, size - 10f, 14f, size - 10f, 10f, 16f)
                    close()
                }
                canvas.drawPath(cup, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                val handle = Path().apply {
                    moveTo(size - 14f, 20f)
                    cubicTo(size - 6f, 20f, size - 6f, size - 16f, size - 14f, size - 16f)
                }
                canvas.drawPath(handle, paint)
                val steam1 = Path().apply {
                    moveTo(16f, 12f)
                    quadTo(18f, 9f, 16f, 6f)
                }
                val steam2 = Path().apply {
                    moveTo(half - 2f, 12f)
                    quadTo(half, 9f, half - 2f, 6f)
                }
                canvas.drawPath(steam1, paint)
                canvas.drawPath(steam2, paint)
            }
            "run" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.style = Paint.Style.FILL
                canvas.drawCircle(half + 4f, 10f, 4f, paint)
                paint.style = Paint.Style.STROKE
                val torso = Path().apply {
                    moveTo(half + 2f, 14f)
                    lineTo(half - 2f, size * 0.6f)
                    moveTo(half - 2f, size * 0.6f)
                    lineTo(half - 8f, size * 0.75f)
                    lineTo(half - 4f, size * 0.9f)
                    moveTo(half - 2f, size * 0.6f)
                    lineTo(half + 6f, size * 0.75f)
                    lineTo(half + 2f, size * 0.9f)
                    moveTo(half + 2f, 16f)
                    lineTo(half + 8f, 20f)
                    lineTo(half + 12f, 16f)
                    moveTo(half + 2f, 16f)
                    lineTo(half - 6f, 20f)
                    lineTo(half - 10f, 24f)
                }
                canvas.drawPath(torso, paint)
            }
            "code" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val left = Path().apply {
                    moveTo(12f, 10f)
                    lineTo(6f, half)
                    lineTo(12f, size - 10f)
                }
                val right = Path().apply {
                    moveTo(size - 12f, 10f)
                    lineTo(size - 6f, half)
                    lineTo(size - 12f, size - 10f)
                }
                val slash = Path().apply {
                    moveTo(size - 10f, 6f)
                    lineTo(10f, size - 6f)
                }
                canvas.drawPath(left, paint)
                canvas.drawPath(right, paint)
                canvas.drawPath(slash, paint)
            }
            "music" -> {
                canvas.drawCircle(12f, size - 12f, 6f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val stem = Path().apply {
                    moveTo(16f, size - 12f)
                    lineTo(16f, 8f)
                    lineTo(size - 10f, 12f)
                }
                canvas.drawPath(stem, paint)
            }
            "phone" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                val phone = Path().apply {
                    moveTo(10f, 10f)
                    cubicTo(6f, 14f, 14f, size - 6f, size - 10f, size - 10f)
                }
                canvas.drawPath(phone, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(10f, 10f, 4f, paint)
                canvas.drawCircle(size - 10f, size - 10f, 4f, paint)
            }
            "meditation" -> {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(half, 10f, 4f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                val body = Path().apply {
                    moveTo(half, 18f)
                    lineTo(half, size * 0.65f)
                    moveTo(half, size * 0.65f)
                    cubicTo(8f, size * 0.65f, 10f, size * 0.9f, half, size * 0.85f)
                    moveTo(half, size * 0.65f)
                    cubicTo(size - 8f, size * 0.65f, size - 10f, size * 0.9f, half, size * 0.85f)
                    moveTo(half, 23f)
                    cubicTo(8f, 23f, 8f, size * 0.6f, 12f, size * 0.65f)
                    moveTo(half, 23f)
                    cubicTo(size - 8f, 23f, size - 8f, size * 0.6f, size - 12f, size * 0.65f)
                }
                canvas.drawPath(body, paint)
            }
            "clock" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(half, half, size / 2f - 6f, paint)
                canvas.drawLine(half, half, half, 12f, paint)
                canvas.drawLine(half, half, half + 8f, half, paint)
            }
            "food" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawCircle(half, half, 12f, paint)
                canvas.drawLine(10f, 16f, 10f, size - 16f, paint)
                canvas.drawLine(size - 10f, 16f, size - 10f, size - 16f, paint)
            }
            "money" -> {
                paint.style = Paint.Style.FILL
                paint.textSize = 34f
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                canvas.drawText("$", half, half + 12f, paint)
            }
            "work" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                val rect = Path().apply {
                    addRoundRect(10f, 16f, size - 10f, size - 10f, 4f, 4f, Path.Direction.CW)
                }
                canvas.drawPath(rect, paint)
                val handle = Path().apply {
                    moveTo(half - 6f, 16f)
                    lineTo(half - 6f, 10f)
                    lineTo(half + 6f, 10f)
                    lineTo(half + 6f, 16f)
                }
                canvas.drawPath(handle, paint)
            }
            "clean" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawLine(10f, size - 10f, size - 16f, 16f, paint)
                canvas.drawLine(10f, size - 10f, 6f, size - 6f, paint)
                canvas.drawLine(10f, size - 10f, 14f, size - 6f, paint)
                canvas.drawLine(10f, size - 10f, 6f, size - 14f, paint)
            }
            else -> {
                val path = Path().apply {
                    moveTo(half, 4f)
                    quadTo(half, half, size - 4f, half)
                    quadTo(half, half, half, size - 4f)
                    quadTo(half, half, 4f, half)
                    quadTo(half, half, half, 4f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
        
        return bitmap
    }

    fun drawWidgetBackgroundBitmapStatic(isDark: Boolean, opacity: Float): Bitmap {
        val width = 120
        val height = 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cornerRadius = 16f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

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

            val sr = if (isDark) 44 else 229
            val sg = if (isDark) 44 else 229
            val sb = if (isDark) 56 else 237
            val strokeAlpha = (alphaInt * 0.85f).toInt().coerceIn(0, 255)
            val strokeColor = Color.argb(strokeAlpha, sr, sg, sb)

            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = strokeColor
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        }

        return bitmap
    }
}
