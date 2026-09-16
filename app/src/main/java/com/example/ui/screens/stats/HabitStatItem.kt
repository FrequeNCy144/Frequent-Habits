package com.example.ui.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.HabitStatModel
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.getEncouragementText
import com.example.ui.components.getStrengthLabel
import com.example.ui.theme.*

@Composable
fun HabitStatItem(
    model: HabitStatModel,
    shortDayNames: List<String>,
    dayNumbers: List<String>,
    language: String,
    accentColor: Color = PrimaryViolet,
    onClick: (Int) -> Unit,
    onToggleDay: ((Int, String) -> Unit)? = null
) {
    val habit = model.habit
    val strength = model.strength
    val habitColor = HabitIconMapping.getColor(habit.color)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val arrowOffsetX by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "habit_stat_arrow_offset"
    )
    val arrowScale by animateFloatAsState(
        targetValue = if (isPressed) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "habit_stat_arrow_scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isPressed) accentColor.copy(alpha = 0.5f) else AppBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = { onClick(habit.id) }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(habitColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, habitColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIconMapping.getIcon(habit.icon),
                            contentDescription = habit.name,
                            tint = habitColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val startDateStr = remember(habit.startDate, language) {
                            try {
                                val date = java.util.Date(habit.startDate)
                                val format = java.text.SimpleDateFormat(
                                    when (language) {
                                        "de" -> "dd.MM.yyyy"
                                        "ka" -> "dd.MM.yyyy"
                                        "zh" -> "yyyy/MM/dd"
                                        else -> "MMM d, yyyy"
                                    },
                                    when (language) {
                                        "de" -> java.util.Locale.GERMAN
                                        "ka" -> java.util.Locale("ka")
                                        "zh" -> java.util.Locale.CHINESE
                                        else -> java.util.Locale.ENGLISH
                                    }
                                )
                                format.format(date)
                            } catch (e: Exception) { "" }
                        }

                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(language, "Stärke: $strength", "სიძლიერე: $strength", "稳固度：$strength", "Strength: $strength"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = if (isPressed) accentColor else TextSecondary,
                    modifier = Modifier
                        .offset(x = arrowOffsetX)
                        .scale(arrowScale)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 7) {
                    val dayName = shortDayNames.getOrElse(i) { "" }
                    val dayNum = dayNumbers.getOrElse(i) { "" }
                    val status = model.past7DaysStatuses.getOrElse(i) { "INACTIVE" }
                    val dateStr = model.past7DaysDates.getOrNull(i)
                    val cellColor = when (status) {
                        "SUCCESS" -> SuccessGreen
                        "FAILED" -> ErrorRed
                        "PENDING" -> HabitYellow
                        "PAUSED" -> accentColor.copy(alpha = 0.15f)
                        else -> ProgressTrack
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.then(
                            if (dateStr != null && onToggleDay != null) {
                                Modifier.clickable {
                                    onToggleDay(habit.id, dateStr)
                                }
                            } else Modifier
                        )
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status == "SUCCESS" || status == "FAILED" || status == "PENDING") AppBg else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

