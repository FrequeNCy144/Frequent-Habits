package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*
import com.frequent.habits.R

object HabitIconMapping {

    val iconList = listOf(
        "sparkle" to Icons.Default.AutoAwesome,
        "moon" to Icons.Default.NightsStay,
        "sun" to Icons.Default.WbSunny,
        "water" to Icons.Default.WaterDrop,
        "heart" to Icons.Default.Favorite,
        "dumbbell" to Icons.Default.FitnessCenter,
        "book" to Icons.Default.MenuBook,
        "coffee" to Icons.Default.Coffee,
        "run" to Icons.Default.DirectionsRun,
        "code" to Icons.Default.Code,
        "music" to Icons.Default.MusicNote,
        "phone" to Icons.Default.Phone,
        "meditation" to Icons.Default.SelfImprovement,
        "clock" to Icons.Default.Alarm,
        "food" to Icons.Default.Restaurant,
        "money" to Icons.Default.AttachMoney,
        "work" to Icons.Default.Work,
        "clean" to Icons.Default.CleaningServices
    )

    val colorList = listOf(
        "blue" to HabitBlue,
        "purple" to HabitPurple,
        "cyan" to HabitCyan,
        "green" to HabitGreen,
        "yellow" to HabitYellow,
        "orange" to HabitOrange,
        "red" to HabitRed,
        "pink" to HabitPink,
        "slate" to HabitSlate
    )

    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "sparkle" -> Icons.Default.AutoAwesome
            "moon" -> Icons.Default.NightsStay
            "sun" -> Icons.Default.WbSunny
            "water" -> Icons.Default.WaterDrop
            "heart" -> Icons.Default.Favorite
            "dumbbell" -> Icons.Default.FitnessCenter
            "book" -> Icons.Default.MenuBook
            "coffee" -> Icons.Default.Coffee
            "run" -> Icons.Default.DirectionsRun
            "code" -> Icons.Default.Code
            "music" -> Icons.Default.MusicNote
            "phone" -> Icons.Default.Phone
            "meditation" -> Icons.Default.SelfImprovement
            "clock" -> Icons.Default.Alarm
            "food" -> Icons.Default.Restaurant
            "money" -> Icons.Default.AttachMoney
            "work" -> Icons.Default.Work
            "clean" -> Icons.Default.CleaningServices
            else -> Icons.Default.AutoAwesome
        }
    }

    fun getColor(name: String): Color {
        return when (name.lowercase()) {
            "blue" -> HabitBlue
            "purple" -> HabitPurple
            "cyan" -> HabitCyan
            "green" -> HabitGreen
            "yellow" -> HabitYellow
            "orange" -> HabitOrange
            "red" -> HabitRed
            "pink" -> HabitPink
            "slate", "grey", "gray" -> HabitSlate
            else -> PrimaryViolet
        }
    }

    fun getIconDrawableId(name: String): Int {
        return when (name.lowercase()) {
            "sparkle" -> R.drawable.ic_sparkle
            "moon" -> R.drawable.ic_moon
            "sun" -> R.drawable.ic_sun
            "water" -> R.drawable.ic_water
            "heart" -> R.drawable.ic_heart
            "dumbbell" -> R.drawable.ic_dumbbell
            "book" -> R.drawable.ic_book
            "coffee" -> R.drawable.ic_coffee
            "run" -> R.drawable.ic_run
            "code" -> R.drawable.ic_code
            "music" -> R.drawable.ic_music
            "phone" -> R.drawable.ic_phone
            "meditation" -> R.drawable.ic_meditation
            "clock" -> R.drawable.ic_clock
            "food" -> R.drawable.ic_food
            "money" -> R.drawable.ic_money
            "work" -> R.drawable.ic_work
            "clean" -> R.drawable.ic_clean
            else -> R.drawable.ic_sparkle
        }
    }
}
