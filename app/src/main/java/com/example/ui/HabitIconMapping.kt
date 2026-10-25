package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import com.example.ui.theme.*
import com.example.tr
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

    var systemAccentColor: Color? = null

    fun initSystemAccentColor(context: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                val colorInt = context.getColor(android.R.color.system_accent1_500)
                systemAccentColor = Color(colorInt)
            } catch (e: Exception) {
                try {
                    val colorInt = context.getColor(android.R.color.system_accent1_600)
                    systemAccentColor = Color(colorInt)
                } catch (_: Exception) {
                    systemAccentColor = null
                }
            }
        }
    }

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
            keys = listOf(
                "dumbbell", "run", "bicycle", "pool", "heart", "meditation", "bed", "spa", "scale", "fire", "pill", 
                "sport", "soccer", "trophy", "target", "yoga", "teeth", "footsteps", "shower", "no_food", "pray", 
                "mountain", "vibration", "medical", "beer", "fastfood", "walk", "water_drop",
                "cold_shower", "supplements", "breathwork", "posture"
            )
        ),
        IconCategory(
            id = "daily_nature",
            nameDe = "Alltag & Natur",
            nameEn = "Daily & Nature",
            nameKa = "ყოველდღიურობა და ბუნება",
            nameZh = "日常与自然",
            keys = listOf(
                "sparkle", "sun", "moon", "water", "coffee", "tea", "food", "apple", "smile", "smoke_free", 
                "no_cell", "plant", "pet", "leaf", "drink", "cake", "fruit", "eco", "eco_leaf", "recycle", 
                "touch_app", "all_inclusive", "no_sugar", "cookie", "sad", "forest", "restaurant", "alarm",
                "sleep_early", "morning_sun", "salad", "water_plants"
            )
        ),
        IconCategory(
            id = "learning_work",
            nameDe = "Lernen & Arbeit",
            nameEn = "Learning & Work",
            nameKa = "სწავლა და სამუშაო",
            nameZh = "学习与工作",
            keys = listOf(
                "book", "school", "code", "work", "laptop", "pencil", "brush", "palette", "brain", "language", 
                "lightbulb", "camera", "music", "headphones", "graduation", "chess", "description", "analytics", 
                "bug_report", "task", "hourglass", "key", "guitar", "piano", "mic",
                "journal", "audiobook", "digital_detox"
            )
        ),
        IconCategory(
            id = "home_money",
            nameDe = "Haushalt & Finanzen",
            nameEn = "Home & Finance",
            nameKa = "საოჯახო და ფინანსები",
            nameZh = "居家与财务",
            keys = listOf(
                "clean", "home", "money", "piggy", "shopping", "clock", "timer", "calendar", "phone", "car", 
                "travel", "bell", "party", "star", "shield", "wallet", "cleaning", "chat", "gamepad", "gift", 
                "diamond", "trending_up", "trending_down", "backup", "bar_chart", "check_circle", "person", 
                "archive", "add_circle", "info", "lock", "lock_open", "notification", "open_in_new", "settings",
                "laundry", "flight", "movie", "people"
            )
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
        "target" to R.drawable.ic_target,
        "yoga" to R.drawable.ic_yoga,
        "teeth" to R.drawable.ic_teeth,
        "footsteps" to R.drawable.ic_footsteps,
        "shower" to R.drawable.ic_shower,
        "no_food" to R.drawable.ic_no_food,
        "pray" to R.drawable.ic_pray,
        "mountain" to R.drawable.ic_mountain,
        "vibration" to R.drawable.ic_vibration,
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
        "fruit" to R.drawable.ic_fruit,
        "eco" to R.drawable.ic_eco,
        "eco_leaf" to R.drawable.ic_eco_leaf,
        "recycle" to R.drawable.ic_recycle,
        "touch_app" to R.drawable.ic_touch_app,
        "all_inclusive" to R.drawable.ic_all_inclusive,
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
        "graduation" to R.drawable.ic_graduation,
        "chess" to R.drawable.ic_chess,
        "description" to R.drawable.ic_description,
        "analytics" to R.drawable.ic_analytics,
        "bug_report" to R.drawable.ic_bug_report,
        "task" to R.drawable.ic_task,
        "clean" to R.drawable.ic_clean,
        "home" to R.drawable.ic_home,
        "money" to R.drawable.ic_money,
        "piggy" to R.drawable.ic_piggy,
        "shopping" to R.drawable.ic_shopping,
        "clock" to R.drawable.ic_clock,
        "timer" to R.drawable.ic_timer,
        "calendar" to R.drawable.ic_calendar,
        "phone" to R.drawable.ic_phone,
        "car" to R.drawable.ic_car,
        "travel" to R.drawable.ic_travel,
        "bell" to R.drawable.ic_bell,
        "party" to R.drawable.ic_party,
        "star" to R.drawable.ic_star,
        "shield" to R.drawable.ic_shield,
        "wallet" to R.drawable.ic_wallet,
        "cleaning" to R.drawable.ic_cleaning,
        "chat" to R.drawable.ic_chat,
        "gamepad" to R.drawable.ic_gamepad,
        "gift" to R.drawable.ic_gift,
        "diamond" to R.drawable.ic_diamond,
        "trending_up" to R.drawable.ic_trending_up,
        "trending_down" to R.drawable.ic_trending_down,
        "backup" to R.drawable.ic_backup,
        "bar_chart" to R.drawable.ic_bar_chart,
        "check_circle" to R.drawable.ic_check_circle,
        "person" to R.drawable.ic_person,
        "archive" to R.drawable.ic_archive,
        "add_circle" to R.drawable.ic_add_circle,
        "info" to R.drawable.ic_info,
        "lock" to R.drawable.ic_lock,
        "lock_open" to R.drawable.ic_lock_open,
        "notification" to R.drawable.ic_notification,
        "open_in_new" to R.drawable.ic_open_in_new,
        "settings" to R.drawable.ic_settings,
        "laundry" to R.drawable.ic_laundry,
        "hourglass" to R.drawable.ic_hourglass,
        "no_sugar" to R.drawable.ic_no_sugar,
        "medical" to R.drawable.ic_medical,
        "cookie" to R.drawable.ic_cookie,
        "beer" to R.drawable.ic_beer,
        "fastfood" to R.drawable.ic_fastfood,
        "sad" to R.drawable.ic_sad,
        "forest" to R.drawable.ic_forest,
        "key" to R.drawable.ic_key,
        "flight" to R.drawable.ic_flight,
        "movie" to R.drawable.ic_movie,
        "guitar" to R.drawable.ic_guitar,
        "piano" to R.drawable.ic_piano,
        "walk" to R.drawable.ic_walk,
        "water_drop" to R.drawable.ic_water_drop,
        "restaurant" to R.drawable.ic_restaurant,
        "alarm" to R.drawable.ic_alarm,
        "people" to R.drawable.ic_people,
        "mic" to R.drawable.ic_mic,
        "cold_shower" to R.drawable.ic_cold_shower,
        "journal" to R.drawable.ic_journal,
        "supplements" to R.drawable.ic_supplements,
        "digital_detox" to R.drawable.ic_digital_detox,
        "sleep_early" to R.drawable.ic_sleep_early,
        "morning_sun" to R.drawable.ic_morning_sun,
        "salad" to R.drawable.ic_salad,
        "water_plants" to R.drawable.ic_water_plants,
        "breathwork" to R.drawable.ic_breathwork,
        "posture" to R.drawable.ic_posture,
        "audiobook" to R.drawable.ic_audiobook
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
        if (norm == "system" || norm == "dynamic" || norm == "auto") {
            return systemAccentColor ?: HabitPurple
        }
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
            "target" -> R.drawable.ic_target
            "yoga" -> R.drawable.ic_yoga
            "teeth" -> R.drawable.ic_teeth
            "footsteps" -> R.drawable.ic_footsteps
            "shower" -> R.drawable.ic_shower
            "no_food" -> R.drawable.ic_no_food
            "pray" -> R.drawable.ic_pray
            "mountain" -> R.drawable.ic_mountain
            "vibration" -> R.drawable.ic_vibration
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
            "fruit" -> R.drawable.ic_fruit
            "eco" -> R.drawable.ic_eco
            "eco_leaf" -> R.drawable.ic_eco_leaf
            "recycle" -> R.drawable.ic_recycle
            "touch_app" -> R.drawable.ic_touch_app
            "all_inclusive" -> R.drawable.ic_all_inclusive
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
            "graduation" -> R.drawable.ic_graduation
            "chess" -> R.drawable.ic_chess
            "description" -> R.drawable.ic_description
            "analytics" -> R.drawable.ic_analytics
            "bug_report" -> R.drawable.ic_bug_report
            "task" -> R.drawable.ic_task
            "clean" -> R.drawable.ic_clean
            "home" -> R.drawable.ic_home
            "money" -> R.drawable.ic_money
            "piggy" -> R.drawable.ic_piggy
            "shopping" -> R.drawable.ic_shopping
            "clock" -> R.drawable.ic_clock
            "timer" -> R.drawable.ic_timer
            "calendar" -> R.drawable.ic_calendar
            "phone" -> R.drawable.ic_phone
            "car" -> R.drawable.ic_car
            "travel" -> R.drawable.ic_travel
            "bell" -> R.drawable.ic_bell
            "party" -> R.drawable.ic_party
            "star" -> R.drawable.ic_star
            "shield" -> R.drawable.ic_shield
            "wallet" -> R.drawable.ic_wallet
            "cleaning" -> R.drawable.ic_cleaning
            "chat" -> R.drawable.ic_chat
            "gamepad" -> R.drawable.ic_gamepad
            "gift" -> R.drawable.ic_gift
            "diamond" -> R.drawable.ic_diamond
            "trending_up" -> R.drawable.ic_trending_up
            "trending_down" -> R.drawable.ic_trending_down
            "backup" -> R.drawable.ic_backup
            "bar_chart" -> R.drawable.ic_bar_chart
            "check_circle" -> R.drawable.ic_check_circle
            "person" -> R.drawable.ic_person
            "archive" -> R.drawable.ic_archive
            "add_circle" -> R.drawable.ic_add_circle
            "info" -> R.drawable.ic_info
            "lock" -> R.drawable.ic_lock
            "lock_open" -> R.drawable.ic_lock_open
            "notification" -> R.drawable.ic_notification
            "open_in_new" -> R.drawable.ic_open_in_new
            "settings" -> R.drawable.ic_settings
            "laundry" -> R.drawable.ic_laundry
            "hourglass" -> R.drawable.ic_hourglass
            "no_sugar" -> R.drawable.ic_no_sugar
            "medical" -> R.drawable.ic_medical
            "cookie" -> R.drawable.ic_cookie
            "folder" -> R.drawable.ic_folder
            "beer" -> R.drawable.ic_beer
            "fastfood" -> R.drawable.ic_fastfood
            "sad" -> R.drawable.ic_sad
            "forest" -> R.drawable.ic_forest
            "key" -> R.drawable.ic_key
            "flight" -> R.drawable.ic_flight
            "movie" -> R.drawable.ic_movie
            "guitar" -> R.drawable.ic_guitar
            "piano" -> R.drawable.ic_piano
            "walk" -> R.drawable.ic_walk
            "water_drop" -> R.drawable.ic_water_drop
            "restaurant" -> R.drawable.ic_restaurant
            "alarm" -> R.drawable.ic_alarm
            "people" -> R.drawable.ic_people
            "mic" -> R.drawable.ic_mic
            "cold_shower" -> R.drawable.ic_cold_shower
            "journal" -> R.drawable.ic_journal
            "supplements" -> R.drawable.ic_supplements
            "digital_detox" -> R.drawable.ic_digital_detox
            "sleep_early" -> R.drawable.ic_sleep_early
            "morning_sun" -> R.drawable.ic_morning_sun
            "salad" -> R.drawable.ic_salad
            "water_plants" -> R.drawable.ic_water_plants
            "breathwork" -> R.drawable.ic_breathwork
            "posture" -> R.drawable.ic_posture
            "audiobook" -> R.drawable.ic_audiobook
            else -> R.drawable.ic_sparkle
        }
    }

    fun getIconLabel(key: String, language: String): String {
        return HabitIconLabels.getLabel(key, language)
    }
}
