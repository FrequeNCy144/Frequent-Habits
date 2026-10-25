package com.example.ui.screens.createhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun HabitMinimalViableCard(
    language: String,
    minimalViableRule: String,
    onMinimalViableRuleChange: (String) -> Unit,
    isNumeric: Boolean = false,
    unit: String = "",
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
                            painter = painterResource(id = R.drawable.ic_bolt),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(language, "Minimalversion (MVV)", "მინიმალური ვერსია (MVV)", "最小可行版本（MVV）", "Minimal Viable Version (MVV)"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                language,
                                "Notfall-Version für stressige Tage",
                                "გადაუდებელი ვერსია რთული დღეებისთვის",
                                "忙碌或艰难日子的应急微习惯",
                                "Emergency micro-habit for busy days"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                InfoIconButton(
                    title = tr(language, "Minimal Viable Version (MVV)", "მინიმალური ვერსია", "最小可行版本", "Minimal Viable Version"),
                    explanation = tr(
                        language,
                        "⚡ Minimal Viable Version (MVV / 2-Minuten-Regel):\n\nDie kleinste, machbare Version deiner Gewohnheit für stressige Tage oder Notfälle (z.B. nur 1 Liegestütz, 1 Seite lesen oder 2 Minuten meditieren).\n\n🔬 Der wissenschaftliche Effekt:\nIndem du die Einstiegshürde an schwierigen Tagen senkst, schützt du deine neuronale Gewohnheitsschleife und deinen Streak. Besser eine minimale Version als gar keine!",
                        "⚡ მინიმალური ვერსია (MVV):\n\nყველაზე მცირე, ადვილად შესასრულებელი ვერსია დატვირთული დღეებისთვის (მაგ. მხოლოდ 1 აზიდვა, 1 გვერდის წაკითხვა ან 2 წუთი მედიტაცია).\n\n🔬 ეფექტი:\nიცავს თქვენს ჩვევის რიტმსა და სთრიქს.",
                        "⚡ 最小可行版本（MVV / 两分钟定律）：\n\n为极度忙碌或疲惫日子设定的极简微习惯（例如：只做 1 个俯卧撑、读 1 页书或冥想 2 分钟）。\n\n🔬 科学原理：\n在艰难的日子降低门槛，能守护你的神经习惯回路，保住连续打卡记录。做极小的一点点，也远好过完全放弃！",
                        "⚡ Minimal Viable Version (MVV):\n\nScale any habit down to its smallest doable version for busy days or emergencies (e.g. 1 pushup, 1 page or 2 minutes).\n\n🔬 The Science:\nLowering friction on hard days preserves your habit momentum and protects your streak. Consistency beats intensity!"
                    ),
                    onClick = { t, e -> onShowInfoDialog(t, e) }
                )
            }

            val mvvPlaceholder = if (isNumeric) {
                if (unit.isNotBlank()) {
                    tr(language, "z. B. 1 $unit", "მაგ. 1 $unit", "例如: 1 $unit", "e.g. 1 $unit")
                } else {
                    tr(language, "z. B. 1", "მაგ. 1", "例如: 1", "e.g. 1")
                }
            } else {
                tr(
                    language,
                    "z.B. Nur 1 Liegestütz / 1 Seite lesen / 2 Min",
                    "მაგ. 1 აზიდვა / 1 გვერდი",
                    "例如：只做 1 个俯卧撑 / 读 1 页书 / 冥想 2 分钟",
                    "e.g. Just 1 pushup / read 1 page / 2 min"
                )
            }

            AppTextField(
                value = minimalViableRule,
                onValueChange = onMinimalViableRuleChange,
                placeholderText = mvvPlaceholder,
                keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                singleLine = true,
                accentColor = accentColor
            )
        }
    }
}
