package com.example.ui.dialogs

import com.example.tr
import com.example.ui.components.*

import com.example.ui.dialogs.review.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyReviewDialog(
    year: Int,
    month: Int,
    allHabits: List<Habit>,
    allLogs: List<HabitLog>,
    language: String,
    viewModel: HabitsViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val reviewData = remember(year, month, allHabits, allLogs, language) {
            calculateMonthlyReviewData(year, month, allHabits, allLogs, language)
        }
        val pageCount = 6
        val pagerState = rememberPagerState(pageCount = { pageCount })
        val coroutineScope = rememberCoroutineScope()

        val gradientColors = when (pagerState.currentPage) {
            0 -> listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), AppBg)
            1 -> listOf(Color(0xFF064E3B), Color(0xFF0F172A), AppBg)
            2 -> listOf(Color(0xFF4C1D95), Color(0xFF1E1B4B), AppBg)
            3 -> listOf(Color(0xFF78350F), Color(0xFF18181B), AppBg)
            4 -> listOf(Color(0xFF312E81), Color(0xFF1E1B4B), AppBg)
            else -> listOf(Color(0xFF1E3A8A), Color(0xFF0F172A), AppBg)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradientColors))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress indicators & close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until pageCount) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.25f)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> MonthlyReviewCoverSlide(year, month, language, reviewData)
                            1 -> MonthlyReviewVolumeSlide(year, month, language, reviewData)
                            2 -> MonthlyReviewMVPSlide(year, month, language, reviewData)
                            3 -> MonthlyReviewFocusSlide(year, month, language, reviewData)
                            4 -> TimeCapsuleSlide("MONTHLY", year, month, language, viewModel)
                            else -> MonthlyReviewSummarySlide(year, month, language, reviewData, onDismiss)
                        }
                    }

                    // Bottom Navigation Buttons: Only a single Next button!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 72.dp, top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val isLastPage = pagerState.currentPage == pageCount - 1
                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastPage) SuccessGreen else PrimaryViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (isLastPage) {
                                    tr(language, "Fertig", "დასრულება", "完成", "Finish")
                                } else {
                                    tr(language, "Weiter", "შემდეგი", "下一步", "Next")
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (isLastPage) "Finish" else "Next",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


