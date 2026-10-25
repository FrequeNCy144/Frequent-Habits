package com.example.ui.screens.createhabit

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HabitWizardStepper(
    currentStep: Int,
    activeColor: Color,
    language: String,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated connector line fractions between icons
    val line1Progress by animateFloatAsState(
        targetValue = if (currentStep >= 2) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "step_line_1"
    )
    val line2Progress by animateFloatAsState(
        targetValue = if (currentStep >= 3) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "step_line_2"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        // Node 1: Basis (Icon in circle + Name underneath)
        val isNode1Active = currentStep == 1
        val isNode1Done = currentStep > 1
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onStepSelected(1) }
                .testTag("wizard_step_1")
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isNode1Done -> activeColor
                    isNode1Active -> activeColor.copy(alpha = 0.22f)
                    else -> AppCard
                },
                border = BorderStroke(
                    width = if (isNode1Active || isNode1Done) 2.dp else 1.dp,
                    color = if (isNode1Active || isNode1Done) activeColor else AppBorder
                ),
                shadowElevation = if (isNode1Active || isNode1Done) 4.dp else 0.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isNode1Done) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isNode1Done) Color.White else if (isNode1Active) activeColor else TextSecondary,
                        modifier = Modifier.size(if (isNode1Done) 20.dp else 18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(language, "Basis", "საფუძველი", "基础", "Basics"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isNode1Active || isNode1Done) FontWeight.Bold else FontWeight.Medium,
                color = if (isNode1Active) TextPrimary else if (isNode1Done) activeColor else TextSecondary
            )
        }

        // Connector Line 1 -> 2
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 18.dp, start = 8.dp, end = 8.dp)
                .height(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AppBorder.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = line1Progress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.8f),
                                activeColor
                            )
                        )
                    )
            )
        }

        // Node 2: Ziel (Icon in circle + Name underneath)
        val isNode2Active = currentStep == 2
        val isNode2Done = currentStep > 2
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onStepSelected(2) }
                .testTag("wizard_step_2")
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isNode2Done -> activeColor
                    isNode2Active -> activeColor.copy(alpha = 0.22f)
                    else -> AppCard
                },
                border = BorderStroke(
                    width = if (isNode2Active || isNode2Done) 2.dp else 1.dp,
                    color = if (isNode2Active || isNode2Done) activeColor else AppBorder
                ),
                shadowElevation = if (isNode2Active || isNode2Done) 4.dp else 0.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isNode2Done) Icons.Default.Check else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (isNode2Done) Color.White else if (isNode2Active) activeColor else TextSecondary,
                        modifier = Modifier.size(if (isNode2Done) 20.dp else 18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(language, "Ziel", "მიზანი", "目标", "Target"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isNode2Active || isNode2Done) FontWeight.Bold else FontWeight.Medium,
                color = if (isNode2Active) TextPrimary else if (isNode2Done) activeColor else TextSecondary
            )
        }

        // Connector Line 2 -> 3
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 18.dp, start = 8.dp, end = 8.dp)
                .height(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AppBorder.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = line2Progress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.8f),
                                activeColor
                            )
                        )
                    )
            )
        }

        // Node 3: Details (Icon in circle + Name underneath)
        val isNode3Active = currentStep == 3
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onStepSelected(3) }
                .testTag("wizard_step_3")
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isNode3Active -> activeColor.copy(alpha = 0.22f)
                    else -> AppCard
                },
                border = BorderStroke(
                    width = if (isNode3Active) 2.dp else 1.dp,
                    color = if (isNode3Active) activeColor else AppBorder
                ),
                shadowElevation = if (isNode3Active) 4.dp else 0.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isNode3Active) activeColor else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(language, "Details", "დეტალები", "详情", "Details"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isNode3Active) FontWeight.Bold else FontWeight.Medium,
                color = if (isNode3Active) TextPrimary else TextSecondary
            )
        }
    }
}
