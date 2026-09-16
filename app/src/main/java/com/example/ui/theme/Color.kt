package com.example.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// Application Theme Palette
var AppBg by mutableStateOf(Color(0xFF0C0C0E))
var AppCard by mutableStateOf(Color(0xFF1E1E24))
var AppBorder by mutableStateOf(Color(0xFF2E2E37))
var PrimaryViolet by mutableStateOf(Color(0xFF783CFA))   // Exact vibrant purple accent (+ New button, checked items, active day)
var SecondaryViolet by mutableStateOf(Color(0xFFA78BFA)) // Soft purple highlight/text

fun updateThemeColors(isDark: Boolean) {
    if (isDark) {
        AppBg = Color(0xFF0C0C0E)
        AppCard = Color(0xFF1E1E24)
        AppBorder = Color(0xFF2E2E37)
        TextPrimary = Color(0xFFFFFFFF)
        TextSecondary = Color(0xFF9E9EA8)
        ProgressTrack = Color(0xFF2A2A32)
        ProgressEndText = Color(0xFFDDD6FE)
        SuccessBg = Color(0xFF133227)
        FailedBg = Color(0xFF311721)
        PausedBg = Color(0xFF321E18)
    } else {
        AppBg = Color(0xFFF9F9FB)
        AppCard = Color(0xFFFFFFFF)
        AppBorder = Color(0xFFE5E5ED)
        TextPrimary = Color(0xFF111115)
        TextSecondary = Color(0xFF6B6B76)
        ProgressTrack = Color(0xFFF1F1F5)
        ProgressEndText = Color(0xFF6366F1)
        SuccessBg = Color(0xFFD1FAE5)
        FailedBg = Color(0xFFFEE2E2)
        PausedBg = Color(0xFFFEF3C7)
    }
}

fun updateAccentColors(colorHex: String) {
    if (colorHex.startsWith("#")) {
        try {
            val parsedPrimary = Color(android.graphics.Color.parseColor(colorHex))
            PrimaryViolet = parsedPrimary
            SecondaryViolet = parsedPrimary.copy(alpha = 0.7f)
            return
        } catch(e: Exception) {}
    }
    if (colorHex.startsWith("0x") || colorHex.startsWith("0X")) {
        try {
            val parsedPrimary = Color(colorHex.substring(2).toLong(16) or 0xFF000000)
            PrimaryViolet = parsedPrimary
            SecondaryViolet = parsedPrimary.copy(alpha = 0.7f)
            return
        } catch(e: Exception) {}
    }
    val (primary, secondary) = when (colorHex.uppercase()) {
        "ELECTRIC_BLUE", "BLUE" -> Color(0xFF3B82F6) to Color(0xFF93C5FD)
        "EMERALD_GREEN", "GREEN" -> Color(0xFF10B981) to Color(0xFF6EE7B7)
        "SUNSET_ORANGE", "ORANGE" -> Color(0xFFF97316) to Color(0xFFFDBA74)
        "CRIMSON_RED", "RED" -> Color(0xFFEF4444) to Color(0xFFFCA5A5)
        "NEON_TEAL", "TEAL" -> Color(0xFF14B8A6) to Color(0xFF5EEAD4)
        "ROSE_PINK", "PINK", "ROSE" -> Color(0xFFF43F5E) to Color(0xFFFDA4AF)
        "CYAN" -> Color(0xFF06B6D4) to Color(0xFF67E8F9)
        "YELLOW" -> Color(0xFFF59E0B) to Color(0xFFFDE047)
        "INDIGO" -> Color(0xFF6366F1) to Color(0xFFA5B4FC)
        "PURPLE" -> Color(0xFF783CFA) to Color(0xFFA78BFA)
        else -> {
            try {
                val hex = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
                val parsedPrimary = Color(android.graphics.Color.parseColor(hex))
                parsedPrimary to parsedPrimary.copy(alpha = 0.7f)
            } catch (e: Exception) {
                Color(0xFF783CFA) to Color(0xFFA78BFA) // Default
            }
        }
    }
    PrimaryViolet = primary
    SecondaryViolet = secondary
}
var TextPrimary by mutableStateOf(Color(0xFFFFFFFF))     // Pure white text
var TextSecondary by mutableStateOf(Color(0xFF9E9EA8))   // Muted slate gray text
var ProgressTrack by mutableStateOf(Color(0xFF2A2A32))   // Dark neutral track for empty day squares & progress bars
var ProgressEndText by mutableStateOf(Color(0xFFDDD6FE))  // Soft light purple

// Feedback colors
val SuccessGreen = Color(0xFF10B981)
var SuccessBg by mutableStateOf(Color(0xFF133227))
val ErrorRed = Color(0xFFEF4444)
val FailedRed = Color(0xFFEF4444)
var FailedBg by mutableStateOf(Color(0xFF311721))
var PausedBg by mutableStateOf(Color(0xFF321E18))

// Habit customized dot colors
val HabitBlue = Color(0xFF3B82F6)
val HabitPurple = Color(0xFF783CFA)
val HabitCyan = Color(0xFF06B6D4)
val HabitGreen = Color(0xFF10B981)
val HabitYellow = Color(0xFFF59E0B)
val HabitOrange = Color(0xFFF97316)
val HabitRed = Color(0xFFEF4444)
val HabitPink = Color(0xFFEC4899)
val HabitSlate = Color(0xFF64748B)
val HabitTeal = Color(0xFF14B8A6)
val HabitRose = Color(0xFFF43F5E)
val HabitIndigo = Color(0xFF6366F1)

// Streak flame colors (independent dedicated colors decoupled from accent color)
// Normal Streak: Richtig leuchtendes heißes Orange
val NormalStreakFlame = Color(0xFFFF5E00) // Vibrant glowing hot fire orange
val HabitStreakFlame = NormalStreakFlame // Habit streaks flame
val StreakFlame = NormalStreakFlame // Legacy alias
val DailyStreakFlame = NormalStreakFlame

// Perfect Streak: Cooles leuchtendes Eisblau
val PerfectStreakFlame = Color(0xFF00E5FF) // Cool vibrant electric ice blue flame
val PerfectStreakIceBlue = PerfectStreakFlame // Alias
val PerfectStreakCyan = Color(0xFF38BDF8) // Ice cyan accent

val AccentRed = Color(0xFFEF4444)
