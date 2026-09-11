package com.example.ui.dialogs

import com.example.tr
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Habit
import com.example.ui.*
import com.example.ui.screens.onboarding.*
import com.example.ui.HabitsViewModel
import com.example.ui.components.HabitItemRow
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AppOnboardingDialog(
    language: String,
    viewModel: HabitsViewModel,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { /* Nicht abbrechbar ohne Durchklicken */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = AppCard,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header mit Fortschritts-Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(language, "Einführung", "შესავალი", "新手指南", "Quick Tour"),
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { pageIndex ->
                            val isSelected = pagerState.currentPage == pageIndex
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 24.dp else 8.dp)
                                    .background(
                                        if (isSelected) PrimaryViolet else PrimaryViolet.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp, max = 500.dp)
                ) { page ->
                    when (page) {
                        0 -> OnboardingStep1(language)
                        1 -> OnboardingStep2(language)
                        2 -> OnboardingStep3(language)
                        3 -> OnboardingStep4(language)
                        4 -> OnboardingStep5(language, viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons (Next / Finish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text(
                                text = tr(language, "Zurück", "უკან", "上一步", "Back"),
                                color = TextSecondary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < 4) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage < 4) {
                                tr(language, "Weiter", "შემდეგი", "下一步", "Next")
                            } else {
                                tr(language, "Los geht's!", "დაწყება!", "开始使用！", "Get Started!")
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

