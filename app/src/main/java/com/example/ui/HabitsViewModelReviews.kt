package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.data.TimeCapsuleNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun HabitsViewModel.setDeepLinkReviewMonth(value: Pair<Int, Int>?) {
    _deepLinkReviewMonth.value = value
}

fun HabitsViewModel.setDeepLinkReviewYear(value: Int?) {
    _deepLinkReviewYear.value = value
}

fun HabitsViewModel.dismissReview(reviewKey: String) {
    val updated = _dismissedReviews.value.toMutableSet()
    updated.add(reviewKey)
    _dismissedReviews.value = updated
    sharedPrefs.edit().putStringSet("dismissed_reviews", updated).apply()
}

fun HabitsViewModel.saveDailyNote(date: String, content: String) {
    viewModelScope.launch {
        repository.saveDailyNote(date, content)
    }
}

fun HabitsViewModel.getTimeCapsuleNote(type: String, targetPeriod: String): Flow<TimeCapsuleNote?> {
    return repository.getTimeCapsuleNote(type, targetPeriod)
}

fun HabitsViewModel.saveTimeCapsuleNote(type: String, targetPeriod: String, content: String) {
    viewModelScope.launch {
        repository.saveTimeCapsuleNote(type, targetPeriod, content)
    }
}

fun HabitsViewModel.setAnalyticsFilter(filter: String) {
    _analyticsFilter.value = filter
    _analyticsVerlaufOffset.value = 0
}

fun HabitsViewModel.navigateAnalyticsVerlauf(delta: Int) {
    _analyticsVerlaufOffset.update { it + delta }
}
