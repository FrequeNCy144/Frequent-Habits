package com.example.ui.screens.today

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.CalendarDayItem
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TodayWeekHeader(
    pagerState: PagerState,
    minMonday: LocalDate,
    selectedDate: String,
    todayDateString: String,
    minDateStr: String,
    earliestHabitDateStr: String,
    language: String,
    canSwipePrev: Boolean,
    canSwipeNext: Boolean,
    coroutineScope: CoroutineScope,
    onSelectDay: (String) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color = PrimaryViolet
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val prevInteractionSource = remember { MutableInteractionSource() }
        val prevIsPressed by prevInteractionSource.collectIsPressedAsState()
        val prevOffsetX by animateDpAsState(
            targetValue = if (prevIsPressed) (-6).dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "prev_week_offset"
        )
        IconButton(
            onClick = {
                if (canSwipePrev && pagerState.currentPage - 1 >= 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            },
            enabled = canSwipePrev,
            interactionSource = prevInteractionSource,
            modifier = Modifier.size(36.dp).testTag("prev_week_button")
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Week",
                tint = if (canSwipePrev) (if (prevIsPressed) accentColor else TextPrimary) else TextPrimary.copy(alpha = 0.3f),
                modifier = Modifier
                    .offset(x = prevOffsetX)
                    .size(24.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            key = { page -> page }
        ) { page ->
            val pageMonday = remember(page, minMonday) { minMonday.plusWeeks(page.toLong()) }
            val pageDays = remember(pageMonday, language) {
                val dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
                val numFmt = DateTimeFormatter.ofPattern("d", Locale.US)
                val loc = when (language) {
                    "de" -> Locale.GERMANY
                    "ka" -> Locale.forLanguageTag("ka")
                    else -> Locale.US
                }
                val nameFmt = DateTimeFormatter.ofPattern("E", loc)
                (0..6).map { i ->
                    val d = pageMonday.plusDays(i.toLong())
                    Triple(
                        d.format(dbFmt),
                        d.format(numFmt),
                        if (language == "ka") d.format(nameFmt).take(3) else d.format(nameFmt).uppercase(loc).take(2)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pageDays.forEach { (dayStr, dayNum, dayName) ->
                    val isSelected = dayStr == selectedDate
                    val isDayEnabled = dayStr >= minDateStr && dayStr >= earliestHabitDateStr
                    key(dayStr) {
                        CalendarDayItem(
                            dayStr = dayStr,
                            dayNum = dayNum,
                            dayName = dayName,
                            isSelected = isSelected,
                            onSelect = onSelectDay,
                            isToday = dayStr == todayDateString,
                            isFuture = dayStr > todayDateString,
                            modifier = Modifier.weight(1f),
                            isEnabled = isDayEnabled,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }

        val nextInteractionSource = remember { MutableInteractionSource() }
        val nextIsPressed by nextInteractionSource.collectIsPressedAsState()
        val nextOffsetX by animateDpAsState(
            targetValue = if (nextIsPressed) 6.dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "next_week_offset"
        )
        IconButton(
            onClick = {
                if (canSwipeNext && pagerState.currentPage + 1 < pagerState.pageCount) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            enabled = canSwipeNext,
            interactionSource = nextInteractionSource,
            modifier = Modifier.size(36.dp).testTag("next_week_button")
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Week",
                tint = if (canSwipeNext) (if (nextIsPressed) accentColor else TextPrimary) else TextPrimary.copy(alpha = 0.3f),
                modifier = Modifier
                    .offset(x = nextOffsetX)
                    .size(24.dp)
            )
        }
    }
}
