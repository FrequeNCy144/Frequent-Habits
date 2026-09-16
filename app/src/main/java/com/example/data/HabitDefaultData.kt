package com.example.data

/**
 * HabitDefaultData contains standard templates, preset configurations,
 * and default data models used across the application.
 */
object HabitDefaultData {

    val PRESET_CATEGORIES = listOf(
        "HEALTH",
        "FITNESS",
        "MINDFULNESS",
        "PRODUCTIVITY",
        "LEARNING",
        "NUTRITION",
        "FINANCE",
        "RELATIONSHIPS",
        "CREATIVITY",
        "OTHER"
    )

    val DEFAULT_AUDIO_PRESETS = listOf(
        "DEFAULT",
        "GENTLE_BELL",
        "DIGITAL_CHIME",
        "NATURE_POP",
        "SUBTLE_CLICK",
        "RETRO_WIN"
    )

    val DEFAULT_APP_LANGUAGES = listOf("en", "de", "ka", "zh", "fr")

    val DEFAULT_GAME_MODES = listOf("FREE", "STORY")
}
