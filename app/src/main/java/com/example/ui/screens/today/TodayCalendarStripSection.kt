package com.example.ui.screens.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PrimaryViolet
import java.util.Calendar

@Composable
fun TodayCalendarStripSection(
    todayDateString: String,
    minWeekStartMillis: Long,
    weekStartCalendar: Calendar,
    selectedDate: String,
    minDateStr: String,
    earliestHabitDateStr: String,
    language: String,
    onSelectDay: (String) -> Unit,
    onSelectDateAndSyncWeek: (String) -> Unit,
    accentColor: Color = PrimaryViolet
) {
    val coroutineScope = rememberCoroutineScope()

    val todayDate = remember(todayDateString) {
        try { java.time.LocalDate.parse(todayDateString) } catch (e: Exception) { java.time.LocalDate.now() }
    }
    val todayMonday = remember(todayDate) {
        todayDate.with(java.time.DayOfWeek.MONDAY)
    }
    val minMonday = remember(minWeekStartMillis, todayMonday) {
        if (minWeekStartMillis > 0) {
            java.time.Instant.ofEpochMilli(minWeekStartMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .with(java.time.DayOfWeek.MONDAY)
                .coerceAtMost(todayMonday)
        } else {
            todayMonday.minusYears(1).with(java.time.DayOfWeek.MONDAY)
        }
    }

    val totalWeeks = remember(minMonday, todayMonday) {
        (java.time.temporal.ChronoUnit.WEEKS.between(minMonday, todayMonday).toInt() + 1).coerceAtLeast(1)
    }
    val maxPageIndex = totalWeeks - 1

    val currentWeekMonday = remember(weekStartCalendar) {
        java.time.Instant.ofEpochMilli(weekStartCalendar.timeInMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .with(java.time.DayOfWeek.MONDAY)
    }
    val currentWeekPageIndex = remember(minMonday, currentWeekMonday, maxPageIndex) {
        java.time.temporal.ChronoUnit.WEEKS.between(minMonday, currentWeekMonday).toInt().coerceIn(0, maxPageIndex)
    }

    val pagerState = key(minMonday, totalWeeks) {
        rememberPagerState(
            initialPage = currentWeekPageIndex,
            pageCount = { totalWeeks }
        )
    }

    var isUserSwiping by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isUserSwiping = true
        }
    }

    // Sync from viewModel to pagerState when currentWeekStart changes outside (e.g. date picker / Heute)
    LaunchedEffect(currentWeekMonday, totalWeeks) {
        val targetPage = java.time.temporal.ChronoUnit.WEEKS.between(minMonday, currentWeekMonday).toInt().coerceIn(0, maxPageIndex)
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress && !isUserSwiping) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync from pagerState to viewModel when settled page changes via swipe
    LaunchedEffect(pagerState.settledPage) {
        if (isUserSwiping) {
            val pageMonday = minMonday.plusWeeks(pagerState.settledPage.toLong())
            if (pageMonday != currentWeekMonday) {
                val selLocalDate = try { java.time.LocalDate.parse(selectedDate) } catch (e: Exception) { todayDate }
                val dayOffset = (selLocalDate.dayOfWeek.value - 1).coerceIn(0, 6)
                var targetDate = pageMonday.plusDays(dayOffset.toLong())
                if (targetDate > todayDate) targetDate = todayDate
                val minLocalDate = try { java.time.LocalDate.parse(minDateStr) } catch (e: Exception) { todayDate }
                if (targetDate < minLocalDate) targetDate = minLocalDate

                onSelectDateAndSyncWeek(targetDate.toString())
            }
            isUserSwiping = false
        }
    }

    val canSwipePrev = pagerState.currentPage > 0
    val canSwipeNext = pagerState.currentPage < maxPageIndex

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TodayWeekHeader(
            pagerState = pagerState,
            minMonday = minMonday,
            selectedDate = selectedDate,
            todayDateString = todayDateString,
            minDateStr = minDateStr,
            earliestHabitDateStr = earliestHabitDateStr,
            language = language,
            canSwipePrev = canSwipePrev,
            canSwipeNext = canSwipeNext,
            coroutineScope = coroutineScope,
            onSelectDay = onSelectDay,
            accentColor = accentColor
        )
    }
}
