package com.example.ui

import androidx.compose.runtime.Immutable

@Immutable
data class HabitAnalyticsUiState(
    val minVerlaufOffset: Int = 0,
    val verlaufTitle: String = "",
    val canPrevVerlauf: Boolean = false,
    val canNextVerlauf: Boolean = false,
    val barData: List<Pair<String, Float>> = emptyList()
)
