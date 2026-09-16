package com.example.ui.screens.createhabit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppSegmentedButton
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HabitDifficultySection(
    language: String,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    onShowInfoDialog: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = tr(language, "Schwierigkeitsgrad (Badges)", "სირთულე (ბეჯები)", "难度（勋章）", "Difficulty (Badges)"),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        onShowInfoDialog(
                            tr(language, "Schwierigkeitsgrad & Badges", "სირთულე და ბეჯები", "难度与勋章", "Difficulty & Badges"),
                            tr(
                                language,
                                "Bestimmt das Aussehen deines Erfolgs-Badges beim Meistern der Gewohnheit:\n\n• Leicht: Bronze-Akzent 🥉\n• Mittel: Silber-Klassiker 🥈\n• Schwer: Goldener Glanz 🥇\n• Extrem: Diamant-Glow & Corona 👑!",
                                "Determines your achievement badge style when completed:\n\n• Easy: Bronze 🥉\n• Medium: Silver 🥈\n• Hard: Gold 🥇\n• Ultra: Diamond Glow 👑!",
                                "决定达成目标时的专属勋章外观：\n\n• 简单：青铜质感 🥉\n• 中等：经典白银 🥈\n• 困难：璀璨黄金 🥇\n• 极难：钻石神级发光勋章 👑！",
                                "Determines your achievement badge style upon mastering this habit:\n\n• Easy: Bronze accent 🥉\n• Medium: Silver classic 🥈\n• Hard: Golden shine 🥇\n• Ultra: Diamond glow & crown 👑!"
                            )
                        )
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
                tr(language, "Leicht", "იოლი", "简单", "Easy"),
                tr(language, "Mittel", "საშუალო", "中等", "Medium"),
                tr(language, "Schwer", "რთული", "困难", "Hard"),
                tr(language, "Extrem 👑", "ულტრა 👑", "极难 👑", "Ultra 👑")
            ),
            selectedIndex = when (difficulty) {
                "EASY" -> 0
                "HARD" -> 2
                "ULTRA" -> 3
                else -> 1
            },
            onOptionSelected = { index ->
                val diff = when (index) {
                    0 -> "EASY"
                    2 -> "HARD"
                    3 -> "ULTRA"
                    else -> "MEDIUM"
                }
                onDifficultyChange(diff)
            },
            testTagPrefix = "habit_difficulty"
        )
    }
}
