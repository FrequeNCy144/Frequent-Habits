package com.example.ui.screens

import com.example.ui.dialogs.*

import com.example.ui.components.*

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.screens.onboarding.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: HabitsViewModel,
    language: String,
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val totalSteps = 8
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalSteps })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with Centered Step Dots & Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Centered Dot indicators
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { idx ->
                        val isCurrent = pagerState.currentPage == idx
                        val isPassed = pagerState.currentPage > idx
                        val dotColor = when {
                            isCurrent -> PrimaryViolet
                            isPassed -> PrimaryViolet.copy(alpha = 0.5f)
                            else -> TextSecondary.copy(alpha = 0.25f)
                        }
                        val dotWidth = if (isCurrent) 22.dp else 8.dp
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                                .animateContentSize()
                        )
                    }
                }

                if (pagerState.currentPage < totalSteps - 1) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = tr(language, "Überspringen", "გამოტოვება", "跳过", "Skip"),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> OnboardingStepWelcome(language = language)
                    1 -> OnboardingStepPersonalize(language = language, viewModel = viewModel)
                    2 -> OnboardingStepGameMode(language = language, viewModel = viewModel)
                    3 -> OnboardingStepTransition(language = language)
                    4 -> OnboardingStepSmartInsights(language = language)
                    5 -> OnboardingStepReviewTimeCapsule(language = language)
                    6 -> OnboardingStepMilestoneRewards(language = language)
                    7 -> OnboardingStepAndMuchMore(language = language)
                }
            }

            // Bottom Navigation Controls (Next / Start) - Full Width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (pagerState.currentPage < totalSteps - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_next_button")
                    ) {
                        Text(
                            text = tr(language, "Weiter", "შემდეგი", "下一步", "Next"),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_finish_button")
                    ) {
                        Text(
                            text = tr(language, "Loslegen! 🚀", "დაწყება! 🚀", "开始使用！🚀", "Get Started! 🚀"),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
