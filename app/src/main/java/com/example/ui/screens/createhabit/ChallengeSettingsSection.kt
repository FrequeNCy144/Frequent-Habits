package com.example.ui.screens.createhabit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppSegmentedButton
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

@Composable
fun ChallengeSettingsSection(
    language: String,
    challengeMode: String,
    onChallengeModeChange: (String) -> Unit,
    challengeSuccessRate: Int,
    onChallengeSuccessRateChange: (Int) -> Unit,
    isNegative: Boolean = true,
    onShowInfoDialog: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tr(language, "Challenge-Regelmodus", "გამოწვევის რეჟიმი", "挑战规则模式", "Challenge Mode"),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        val title = tr(language, "Challenge-Regelmodi", "გამოწვევის რეჟიმები", "挑战规则模式", "Challenge Modes")
                        val explanation = if (isNegative) {
                            tr(
                                language,
                                "Wähle, wie Rückfälle gewertet werden:\n\n" +
                                "• Clean Days (Tage sammeln): Jeder abstinente Tag zählt als Meilenstein (+1). Ein Rückfall stoppt nur den Tag, setzt den Gesamtzähler aber NICHT auf 0 zurück.\n\n" +
                                "• Harter Streak: Erfordert ununterbrochene Tage am Stück. Bei einem Rückfall wird der Zähler sofort auf 0 zurückgesetzt!\n\n" +
                                "• Erfolgsquote (%): Ermöglicht das Bestehen der Challenge, wenn z. B. 90% aller Tage im Zeitraum abstinent geblieben werden.",
                                "• Clean Days: Relapses don't reset to 0.\n• Streak: Relapses reset counter to 0!\n• Success Rate (%): Achieve a target success rate.",
                                "• 累计清爽天数：仅累计无破戒天数，破戒不重置总进度。\n• 连续天数（硬核）：一旦破戒，天数立即清零！\n• 成功率达标（%）：允许偶尔失误，只要整体戒断成功率达到指定百分比即可通过。",
                                "Choose how relapses affect your progress:\n\n" +
                                "• Clean Days (Accumulating): Every clean day counts (+1). A slip-up only pauses progress without resetting total days to 0.\n\n" +
                                "• Hard Streak: Requires unbroken consecutive days. A single slip-up instantly resets your counter to 0!\n\n" +
                                "• Success Rate (%): Pass the challenge if your overall clean rate meets a threshold (e.g. 90%) over the duration."
                            )
                        } else {
                            tr(
                                language,
                                "Wähle das Regelwerk für deine Challenge:\n\n" +
                                "• Fortschritt sammeln: Sammle Einheiten oder Tage (z. B. 50 Workouts oder 1.000 Seiten). Jeder erfolgreiche Tag zählt dazu, Pausen stoppen nur ohne Reset.\n\n" +
                                "• Harter Streak: Erfordert ununterbrochene Tage am Stück (z. B. 30 Tage tägliche Meditation). Ein verpasster Tag setzt den Zähler auf 0 zurück!\n\n" +
                                "• Erfolgsquote (%): Bestehe die Challenge, wenn du über den Zeitraum eine Mindestquote (z. B. 85%) erreichst. Erlaubt Ruhetage oder Notfälle.",
                                "• Accumulate: Collect total days or units.\n• Streak: Unbroken consecutive days; missed days reset to 0!\n• Success Rate: Achieve a minimum completion percentage.",
                                "• 累计进度：累计完成天数或数值（如 50 次健身或 1000 页阅读），休息天不重置总进度。\n• 连续打卡（硬核）：要求每日不间断。一旦漏打卡，天数立即归零！\n• 成功率达标（%）：在整个周期内达到指定完成率（如 85%），允许合理安排休息日。",
                                "Choose the rules for your challenge:\n\n" +
                                "• Accumulate: Gather total days or units (e.g. 50 workouts or 1,000 pages). Missed days pause without resetting.\n\n" +
                                "• Hard Streak: Requires consecutive unbroken days. Missing a single day resets your counter to 0!\n\n" +
                                "• Success Rate (%): Complete the challenge if you reach a minimum target percentage (e.g. 85%) over the time horizon."
                            )
                        }
                        onShowInfoDialog(title, explanation)
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        AppSegmentedButton(
            options = listOf(
                if (isNegative) tr(language, "Tage sammeln", "დღეების შეგროვება", "累计天数", "Clean Days")
                else tr(language, "Fortschritt", "პროგრესი", "累计进度", "Accumulate"),
                tr(language, "Harter Streak", "მკაცრი სტრიკი", "连续天数", "Hard Streak"),
                tr(language, "Quote (%)", "წარმატების %", "成功率(%)", "Rate (%)")
            ),
            selectedIndex = when (challengeMode) {
                "STREAK" -> 1
                "SUCCESS_RATE" -> 2
                else -> 0
            },
            onOptionSelected = { index ->
                val newMode = when (index) {
                    1 -> "STREAK"
                    2 -> "SUCCESS_RATE"
                    else -> "ACCUMULATE"
                }
                onChallengeModeChange(newMode)
            },
            testTagPrefix = "challenge_mode"
        )

        AnimatedVisibility(visible = challengeMode == "SUCCESS_RATE") {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(language, "Geforderte Erfolgsquote (%)", "სამიზნე %", "目标成功率 (%)", "Required Success Rate (%)"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppTextField(
                            value = challengeSuccessRate.toString(),
                            onValueChange = { input ->
                                val num = input.filter { it.isDigit() }.toIntOrNull()
                                if (num != null) {
                                    onChallengeSuccessRateChange(num.coerceIn(50, 100))
                                } else if (input.isEmpty()) {
                                    onChallengeSuccessRateChange(0)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholderText = "90",
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryViolet
                        )
                    }
                    Text(
                        text = if (isNegative) {
                            tr(
                                language,
                                "Beispiel: Bei 30 Tagen und $challengeSuccessRate% Erfolgsquote darfst du dir maximal ${(30 * (100 - challengeSuccessRate) / 100.0).toInt()} Ausrutscher erlauben.",
                                "მაგალითი: 30 დღეში დასაშვებია შეცდომა.",
                                "例如：在 30 天周期内，达到 $challengeSuccessRate% 成功率最多允许 ${(30 * (100 - challengeSuccessRate) / 100.0).toInt()} 次破戒。",
                                "Example: For 30 days at $challengeSuccessRate%, you can have up to ${(30 * (100 - challengeSuccessRate) / 100.0).toInt()} slip-ups."
                            )
                        } else {
                            tr(
                                language,
                                "Beispiel: Bei 30 Tagen und $challengeSuccessRate% Erfolgsquote musst du an mindestens ${(30 * challengeSuccessRate / 100.0).toInt()} Tagen dein Ziel erreichen.",
                                "მაგალითი: 30 დღეში უნდა შეასრულოთ მიზანი.",
                                "例如：在 30 天周期内，达到 $challengeSuccessRate% 成功率需要至少打卡达成 ${(30 * challengeSuccessRate / 100.0).toInt()} 天。",
                                "Example: For 30 days at $challengeSuccessRate%, you need to complete your goal on at least ${(30 * challengeSuccessRate / 100.0).toInt()} days."
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
