package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*

@Composable
fun HabitAnalyticsSection(
    viewModel: HabitsViewModel,
    language: String,
    onInfoClick: (String, String) -> Unit
) {
    val activeFilter by viewModel.analyticsFilter.collectAsState()
    val analyticsState by viewModel.habitAnalyticsState.collectAsState()
    val canPrevVerlauf = analyticsState?.canPrevVerlauf ?: false
    val canNextVerlauf = analyticsState?.canNextVerlauf ?: false
    val barData = analyticsState?.barData ?: emptyList()
    val verlaufTitle = analyticsState?.verlaufTitle ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tr(language, "VERLAUF", "ისტორია", "历史趋势", "HISTORY"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    InfoIconButton(
                        title = tr(language, "Habit-Verlauf", "ჩვევების ისტორია", "习惯历史", "Habit History"),
                        explanation = if (language == "de") "Zeigt an, wie viel du an jedem Tag eingetragen hast. Bei numerischen Gewohnheiten wird das Erfüllungsverhältnis (z.B. 50% deines Ziels) dargestellt. Wechsle zwischen Woche, Monat und Jahr, um langfristige Muster zu erkennen." else if (language == "ka") "აჩვენებს, თუ რამდენს შედიხართ ყოველ დღე. რიცხვითი ჩვევებისთვის, ის აჩვენებს დასრულების კოეფიციენტს (მაგ. თქვენი მიზნის 50%). გადართეთ კვირის, თვის და წლის ხედებს შორის გრძელვადიანი შაბლონების დასანახად." else "Shows how much you logged on each day. For numeric habits, it displays the completion ratio (e.g. 50% of your target). Switch between Week, Month, and Year views to spot long-term patterns.",
                        onClick = onInfoClick
                    )
                }
                Text(
                    text = tr(language, "Mal pro Tag", "ჯერ დღეში", "次 / 每天", "Times per day"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (canPrevVerlauf) viewModel.navigateAnalyticsVerlauf(-1) },
                    enabled = canPrevVerlauf
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Prev",
                        tint = if (canPrevVerlauf) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                    )
                }
                Text(
                    text = verlaufTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { if (canNextVerlauf) viewModel.navigateAnalyticsVerlauf(1) },
                    enabled = canNextVerlauf
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = if (canNextVerlauf) TextSecondary else TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                barData.forEach { (label, ratio) ->
                    val barWidth = when (activeFilter) {
                        "WEEK" -> 16.dp
                        "MONTH" -> 4.dp
                        else -> 16.dp
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .height(100.dp)
                                .width(barWidth)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(ProgressTrack),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(ratio.coerceIn(0f, 1f))
                                    .fillMaxWidth()
                                    .background(SuccessGreen)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 7.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "WEEK" to (tr(language, "Woche", "კვირა", "周", "Week")),
                    "MONTH" to (tr(language, "Monat", "თვე", "月", "Month")),
                    "YEAR" to (tr(language, "Jahr", "წელიწადი", "年", "Year"))
                ).forEach { (key, display) ->
                    val isSelected = activeFilter == key
                    Button(
                        onClick = {
                            viewModel.setAnalyticsFilter(key)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) PrimaryViolet else ProgressTrack,
                            contentColor = if (isSelected) Color.White else TextSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = display, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
