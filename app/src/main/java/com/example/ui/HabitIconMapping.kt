package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import com.example.ui.theme.*
import com.frequent.habits.R

data class IconCategory(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val nameKa: String = nameEn,
    val nameZh: String = nameEn,
    val keys: List<String>
) {
    fun getLocalizedName(language: String): String = when (language) {
        "de" -> nameDe
        "ka" -> nameKa
        "zh" -> nameZh
        else -> nameEn
    }
}

object HabitIconMapping {

    val categories = listOf(
        IconCategory(
            id = "all",
            nameDe = "Alle",
            nameEn = "All",
            nameKa = "ყველა",
            nameZh = "全部",
            keys = emptyList()
        ),
        IconCategory(
            id = "health_sport",
            nameDe = "Sport & Gesundheit",
            nameEn = "Sport & Health",
            nameKa = "სპორტი და ჯანმრთელობა",
            nameZh = "运动与健康",
            keys = listOf("dumbbell", "run", "bicycle", "pool", "heart", "meditation", "bed", "spa", "scale", "fire", "pill", "sport", "soccer", "trophy")
        ),
        IconCategory(
            id = "daily_nature",
            nameDe = "Alltag & Natur",
            nameEn = "Daily & Nature",
            nameKa = "ყოველდღიურობა და ბუნება",
            nameZh = "日常与自然",
            keys = listOf("sparkle", "sun", "moon", "water", "coffee", "tea", "food", "apple", "smile", "smoke_free", "no_cell", "plant", "pet", "leaf", "drink", "cake")
        ),
        IconCategory(
            id = "learning_work",
            nameDe = "Lernen & Arbeit",
            nameEn = "Learning & Work",
            nameKa = "სწავლა და სამუშაო",
            nameZh = "学习与工作",
            keys = listOf("book", "school", "code", "work", "laptop", "pencil", "brush", "palette", "brain", "language", "lightbulb", "camera", "music", "headphones")
        ),
        IconCategory(
            id = "home_money",
            nameDe = "Haushalt & Finanzen",
            nameEn = "Home & Finance",
            nameKa = "საოჯახო და ფინანსები",
            nameZh = "居家与财务",
            keys = listOf("clean", "home", "money", "piggy", "shopping", "clock", "timer", "calendar", "task", "phone", "car", "travel", "bell", "party", "star", "shield")
        )
    )

    val iconList = listOf(
        "dumbbell" to R.drawable.ic_dumbbell,
        "run" to R.drawable.ic_run,
        "bicycle" to R.drawable.ic_bicycle,
        "pool" to R.drawable.ic_pool,
        "heart" to R.drawable.ic_heart,
        "meditation" to R.drawable.ic_meditation,
        "bed" to R.drawable.ic_bed,
        "spa" to R.drawable.ic_spa,
        "scale" to R.drawable.ic_scale,
        "fire" to R.drawable.ic_fire,
        "pill" to R.drawable.ic_pill,
        "sport" to R.drawable.ic_sport,
        "soccer" to R.drawable.ic_soccer,
        "trophy" to R.drawable.ic_trophy,
        "sparkle" to R.drawable.ic_sparkle,
        "sun" to R.drawable.ic_sun,
        "moon" to R.drawable.ic_moon,
        "water" to R.drawable.ic_water,
        "coffee" to R.drawable.ic_coffee,
        "tea" to R.drawable.ic_tea,
        "food" to R.drawable.ic_food,
        "apple" to R.drawable.ic_apple,
        "smile" to R.drawable.ic_smile,
        "smoke_free" to R.drawable.ic_smoke_free,
        "no_cell" to R.drawable.ic_no_cell,
        "plant" to R.drawable.ic_plant,
        "pet" to R.drawable.ic_pet,
        "leaf" to R.drawable.ic_leaf,
        "drink" to R.drawable.ic_drink,
        "cake" to R.drawable.ic_cake,
        "book" to R.drawable.ic_book,
        "school" to R.drawable.ic_school,
        "code" to R.drawable.ic_code,
        "work" to R.drawable.ic_work,
        "laptop" to R.drawable.ic_laptop,
        "pencil" to R.drawable.ic_pencil,
        "brush" to R.drawable.ic_brush,
        "palette" to R.drawable.ic_palette,
        "brain" to R.drawable.ic_brain,
        "language" to R.drawable.ic_language,
        "lightbulb" to R.drawable.ic_lightbulb,
        "camera" to R.drawable.ic_camera,
        "music" to R.drawable.ic_music,
        "headphones" to R.drawable.ic_headphones,
        "clean" to R.drawable.ic_clean,
        "home" to R.drawable.ic_home,
        "money" to R.drawable.ic_money,
        "piggy" to R.drawable.ic_piggy,
        "shopping" to R.drawable.ic_shopping,
        "clock" to R.drawable.ic_clock,
        "timer" to R.drawable.ic_timer,
        "calendar" to R.drawable.ic_calendar,
        "task" to R.drawable.ic_task,
        "phone" to R.drawable.ic_phone,
        "car" to R.drawable.ic_car,
        "travel" to R.drawable.ic_travel,
        "bell" to R.drawable.ic_bell,
        "party" to R.drawable.ic_party,
        "star" to R.drawable.ic_star,
        "shield" to R.drawable.ic_shield
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
        "teal" to HabitTeal,
        "rose" to HabitRose,
        "indigo" to HabitIndigo
    )

    val DEFAULT_ICON_RES = R.drawable.ic_sparkle

    @Composable
    fun getIcon(name: String): ImageVector {
        return ImageVector.vectorResource(id = getIconDrawableId(name))
    }

    @Composable
    fun getPainter(name: String): Painter {
        return painterResource(id = getIconDrawableId(name))
    }

    fun getDrawableId(name: String): Int = getIconDrawableId(name)

    fun getColor(name: String): Color {
        val raw = name.trim()
        val norm = raw.lowercase()
        if (norm.startsWith("#")) {
            try { return Color(android.graphics.Color.parseColor(norm)) } catch (e: Exception) {}
        }
        if (norm.startsWith("0x")) {
            try { return Color(norm.substring(2).toLong(16) or 0xFF000000) } catch (e: Exception) {}
        }
        if (norm.matches(Regex("^[0-9a-f]{6}$")) || norm.matches(Regex("^[0-9a-f]{8}$"))) {
            try { return Color(android.graphics.Color.parseColor("#$norm")) } catch (e: Exception) {}
        }
        return when {
            norm == "blue" || norm.contains("electric_blue") || norm == "blau" -> HabitBlue
            norm == "purple" || norm == "lila" || norm == "violet" -> HabitPurple
            norm == "cyan" || norm.contains("neon_teal") || norm == "türkis" -> HabitCyan
            norm == "green" || norm.contains("emerald_green") || norm == "grün" -> HabitGreen
            norm == "yellow" || norm == "gelb" -> HabitYellow
            norm == "orange" || norm.contains("sunset_orange") -> HabitOrange
            norm == "red" || norm.contains("crimson") || norm.contains("rot") || norm.contains("red_accent") -> HabitRed
            norm == "pink" || norm.contains("rose_pink") || norm == "rosa" -> HabitPink
            norm == "slate" || norm == "grey" || norm == "gray" || norm == "grau" -> HabitSlate
            norm == "teal" -> HabitTeal
            norm == "rose" -> HabitRose
            norm == "indigo" -> HabitIndigo
            else -> {
                try {
                    val hex = if (raw.startsWith("#")) raw else "#$raw"
                    Color(android.graphics.Color.parseColor(hex))
                } catch (e: Exception) {
                    HabitRed
                }
            }
        }
    }

    fun getColorInt(name: String): Int {
        return getColor(name).toArgb()
    }

    fun getIconDrawableId(name: String): Int {
        return when (name.lowercase()) {
            "dumbbell" -> R.drawable.ic_dumbbell
            "run" -> R.drawable.ic_run
            "bicycle" -> R.drawable.ic_bicycle
            "pool" -> R.drawable.ic_pool
            "heart" -> R.drawable.ic_heart
            "meditation" -> R.drawable.ic_meditation
            "bed" -> R.drawable.ic_bed
            "spa" -> R.drawable.ic_spa
            "scale" -> R.drawable.ic_scale
            "fire" -> R.drawable.ic_fire
            "pill" -> R.drawable.ic_pill
            "sport" -> R.drawable.ic_sport
            "soccer" -> R.drawable.ic_soccer
            "trophy" -> R.drawable.ic_trophy
            "sparkle" -> R.drawable.ic_sparkle
            "sun" -> R.drawable.ic_sun
            "moon" -> R.drawable.ic_moon
            "water" -> R.drawable.ic_water
            "coffee" -> R.drawable.ic_coffee
            "tea" -> R.drawable.ic_tea
            "food" -> R.drawable.ic_food
            "apple" -> R.drawable.ic_apple
            "smile" -> R.drawable.ic_smile
            "smoke_free" -> R.drawable.ic_smoke_free
            "no_cell" -> R.drawable.ic_no_cell
            "plant" -> R.drawable.ic_plant
            "pet" -> R.drawable.ic_pet
            "leaf" -> R.drawable.ic_leaf
            "drink" -> R.drawable.ic_drink
            "cake" -> R.drawable.ic_cake
            "book" -> R.drawable.ic_book
            "school" -> R.drawable.ic_school
            "code" -> R.drawable.ic_code
            "work" -> R.drawable.ic_work
            "laptop" -> R.drawable.ic_laptop
            "pencil" -> R.drawable.ic_pencil
            "brush" -> R.drawable.ic_brush
            "palette" -> R.drawable.ic_palette
            "brain" -> R.drawable.ic_brain
            "language" -> R.drawable.ic_language
            "lightbulb" -> R.drawable.ic_lightbulb
            "camera" -> R.drawable.ic_camera
            "music" -> R.drawable.ic_music
            "headphones" -> R.drawable.ic_headphones
            "clean" -> R.drawable.ic_clean
            "home" -> R.drawable.ic_home
            "money" -> R.drawable.ic_money
            "piggy" -> R.drawable.ic_piggy
            "shopping" -> R.drawable.ic_shopping
            "clock" -> R.drawable.ic_clock
            "timer" -> R.drawable.ic_timer
            "calendar" -> R.drawable.ic_calendar
            "task" -> R.drawable.ic_task
            "phone" -> R.drawable.ic_phone
            "car" -> R.drawable.ic_car
            "travel" -> R.drawable.ic_travel
            "bell" -> R.drawable.ic_bell
            "party" -> R.drawable.ic_party
            "star" -> R.drawable.ic_star
            "shield" -> R.drawable.ic_shield
            "folder" -> R.drawable.ic_folder
            else -> R.drawable.ic_sparkle
        }
    }
}
