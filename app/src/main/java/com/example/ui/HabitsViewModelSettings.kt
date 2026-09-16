package com.example.ui

import android.content.Context
import com.example.HabitWidgetProvider
import com.example.NotificationHelper
import com.example.ui.theme.updateAccentColors
import com.example.ui.theme.updateThemeColors

fun HabitsViewModel.setLanguage(lang: String) {
    _language.value = lang
    sharedPrefs.edit().putString("language", lang).apply()
}

fun HabitsViewModel.setAccentColorName(name: String) {
    _accentColorName.value = name
    sharedPrefs.edit().putString("accent_color_name", name).apply()
    try {
        getApplication<android.app.Application>()
            .getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("accent_color_name", name)
            .apply()
    } catch (e: Exception) {}
    updateAccentColors(name)
    HabitWidgetProvider.triggerUpdate(getApplication())
}

fun HabitsViewModel.setDarkModeEnabled(enabled: Boolean) {
    _darkModeEnabled.value = enabled
    sharedPrefs.edit().putBoolean("dark_mode_enabled", enabled).commit()
    try {
        getApplication<android.app.Application>()
            .getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode_enabled", enabled)
            .commit()
    } catch (e: Exception) {}
    updateThemeColors(enabled)
    HabitWidgetProvider.triggerUpdate(getApplication())
}

fun HabitsViewModel.setWidgetOpacity(opacity: Float) {
    val clamped = opacity.coerceIn(0f, 1f)
    _widgetOpacity.value = clamped
    sharedPrefs.edit().putFloat("widget_opacity", clamped).commit()
    HabitWidgetProvider.triggerUpdate(getApplication())
}

fun HabitsViewModel.setVibrationEnabled(enabled: Boolean) {
    _vibrationEnabled.value = enabled
    sharedPrefs.edit().putBoolean("vibration_enabled", enabled).apply()
}

fun HabitsViewModel.setInfoCardsEnabled(enabled: Boolean) {
    _infoCardsEnabled.value = enabled
    sharedPrefs.edit().putBoolean("info_cards_enabled", enabled).apply()
}

fun HabitsViewModel.setReviewNotificationsEnabled(enabled: Boolean) {
    _reviewNotificationsEnabled.value = enabled
    sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
    NotificationHelper.scheduleReviewNotifications(getApplication())
}

fun HabitsViewModel.setMonthlyReviewEnabled(enabled: Boolean) {
    _monthlyReviewEnabled.value = enabled
    sharedPrefs.edit().putBoolean("monthly_review_enabled", enabled).apply()
    NotificationHelper.scheduleReviewNotifications(getApplication())
}

fun HabitsViewModel.setInsightNotificationsEnabled(enabled: Boolean) {
    _insightNotificationsEnabled.value = enabled
    sharedPrefs.edit().putBoolean("insight_notifications_enabled", enabled).apply()
    if (enabled) {
        NotificationHelper.scheduleSmartInsightNotifications(getApplication())
    } else {
        NotificationHelper.cancelSmartInsightNotifications(getApplication())
    }
}

fun HabitsViewModel.setSmartInsightsInAppEnabled(enabled: Boolean) {
    _smartInsightsInAppEnabled.value = enabled
    sharedPrefs.edit().putBoolean("smart_insights_in_app_enabled", enabled).apply()
}

fun HabitsViewModel.setYearlyReviewEnabled(enabled: Boolean) {
    _yearlyReviewEnabled.value = enabled
    sharedPrefs.edit().putBoolean("yearly_review_enabled", enabled).apply()
    NotificationHelper.scheduleReviewNotifications(getApplication())
}

fun HabitsViewModel.setOnboarded(completed: Boolean) {
    _hasOnboarded.value = completed
    sharedPrefs.edit().putBoolean("has_onboarded", completed).apply()
}

fun HabitsViewModel.resetOnboarding() {
    setOnboarded(false)
}

fun HabitsViewModel.setGameMode(mode: String, activeHabitCount: Int = 0) {
    _gameMode.value = mode
    sharedPrefs.edit().putString("game_mode", mode).apply()
    if (mode == "STORY") {
        val currentUnlocked = _unlockedStorySlots.value
        if (activeHabitCount > currentUnlocked) {
            setUnlockedStorySlots(activeHabitCount)
        }
    }
}

fun HabitsViewModel.setUnlockedStorySlots(slots: Int) {
    val count = slots.coerceAtLeast(1)
    _unlockedStorySlots.value = count
    sharedPrefs.edit().putInt("unlocked_story_slots", count).apply()
}

fun HabitsViewModel.canCreateNewHabit(currentCount: Int): Boolean {
    if (_gameMode.value == "FREE") return true
    return currentCount < _unlockedStorySlots.value
}

fun HabitsViewModel.setNotificationsEnabled(enabled: Boolean) {
    _notificationsEnabled.value = enabled
    sharedPrefs.edit().putBoolean("reminder_enabled", enabled).apply()
    if (enabled) {
        NotificationHelper.scheduleReminder(getApplication(), _notificationsHour.value, _notificationsMinute.value)
    } else {
        NotificationHelper.cancelReminder(getApplication())
    }
}

fun HabitsViewModel.unlockSaskia() {
    _isSaskiaUnlocked.value = true
    sharedPrefs.edit().putBoolean("is_saskia_unlocked", true).apply()
}

fun HabitsViewModel.updateNotificationTime(hour: Int, minute: Int) {
    _notificationsHour.value = hour
    _notificationsMinute.value = minute
    sharedPrefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
    if (_notificationsEnabled.value) {
        NotificationHelper.scheduleReminder(getApplication(), hour, minute)
    }
}
