package com.example.ui.screens.createhabit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun HabitStartDateCard(
    language: String,
    startDateStr: String,
    onStartDateClick: () -> Unit,
    accentColor: Color = PrimaryViolet,
    onShowInfoDialog: (title: String, explanation: String) -> Unit = { _, _ -> }
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(language, "Startdatum", "დაწყების თარიღი", "开始日期", "Start Date"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                language,
                                "Ab welchem Tag das Tracking beginnt",
                                "როდის იწყება თრექინგი",
                                "何时开始追踪此习惯",
                                "When habit tracking begins"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                InfoIconButton(
                    title = tr(language, "Startdatum", "დაწყების თარიღი", "开始日期", "Start Date"),
                    explanation = tr(
                        language,
                        "📅 Flexibler Start:\n\n• Startdatum: Ab welchem Tag die Gewohnheit aktiv getrackt wird (z. B. heute oder am kommenden Montag).\n\n• Vergangene Tage vor dem Startdatum werden in deinen Statistiken und Berechnungen nicht als unerledigt gewertet.",
                        "📅 მოქნილი დაწყება:\n\n• დაწყების თარიღი: როდიდან დაიწყება ჩვევის აღრიცხვა (მაგ. დღეს ან ორშაბათიდან).",
                        "📅 灵活开始：\n\n• 开始日期：从哪一天开始正式追踪此习惯（如今天或下周一）。在此日期之前的日子不会被视作遗漏。",
                        "📅 Flexible Start:\n\n• Start Date: The day tracking officially begins (e.g. today or next Monday).\n\n• Days prior to the start date are not counted as missed."
                    ),
                    onClick = { t, e -> onShowInfoDialog(t, e) }
                )
            }

            // Clickable Date Row
            Surface(
                onClick = onStartDateClick,
                shape = RoundedCornerShape(12.dp),
                color = ProgressTrack,
                border = BorderStroke(1.dp, AppBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = startDateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = tr(language, "Ändern", "შეცვლა", "更改", "Change"),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
