package com.example.ui.screens.createhabit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.components.AppSegmentedButton
import com.example.ui.components.AppSegmentedButtonWithIcons
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

@Composable
fun HabitTargetGoalCard(
    language: String,
    isNumeric: Boolean,
    onNumericChange: (Boolean) -> Unit,
    targetValue: String,
    onTargetValueChange: (String) -> Unit,
    targetValueError: Boolean,
    clickIncrement: String,
    onClickIncrementChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    selectedChip: String,
    onChipSelect: (String) -> Unit,
    activeColor: Color,
    isFinishable: Boolean = false,
    onFinishableChange: (Boolean) -> Unit = {},
    totalTargetValue: String = "",
    onTotalTargetValueChange: (String) -> Unit = {},
    endDateStr: String? = null,
    onEndDateClick: () -> Unit = {},
    onClearEndDate: () -> Unit = {},
    isNegative: Boolean = false,
    onNegativeChange: (Boolean) -> Unit = {},
    difficulty: String = "MEDIUM",
    onDifficultyChange: (String) -> Unit = {},
    isEditMode: Boolean = false,
    onShowInfoDialog: (String, String) -> Unit = { _, _ -> }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = activeColor, modifier = Modifier.size(20.dp))
                Text(
                    text = tr(language, "Ziel & Messung", "მიზანი და გაზომვა", "目标与计量", "Goal & Measurement"),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. Goal Nature: Build vs Quit
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            text = tr(language, "Gewohnheits-Richtung", "მიზანი", "目标方向", "Habit Direction"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                onShowInfoDialog(
                                    tr(language, "Gewohnheits-Richtung", "მიზნის მიმართულება", "目标方向", "Habit Direction"),
                                    tr(
                                        language,
                                        "• Aufbauen: Positive Gewohnheiten fördern (z. B. Lesen, Sport, Wasser trinken).\n\n• Abgewöhnen: Ungeplante Laster reduzieren oder stoppen (z. B. Rauchen, Zucker, Social Media). Bei Abgewöhnen-Challenges zählst du rauchfreie Tage!",
                                        "• Aufbauen: Encourage positive habits.\n\n• Abgewöhnen: Break unwanted habits. Track clean days!",
                                        "• 养成：培养积极良好的好习惯。\n\n• 戒除：减少或停止坏习惯。戒除模式下记录保持无破戒的天数！",
                                        "• Build: Develop positive habits (e.g. reading, exercise, drinking water).\n\n• Quit: Reduce or break unwanted habits (e.g. smoking, sugar). Track clean/sober days!"
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
                    if (isEditMode) {
                        Text(
                            text = tr(language, "🔒 Grundtyp fixiert", "🔒 Type locked", "🔒 类型已固定", "🔒 Type locked"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                AppSegmentedButtonWithIcons(
                    options = listOf(
                        (tr(language, "Aufbauen", "აშენება", "养成", "Build")) to Icons.Default.ArrowUpward,
                        (tr(language, "Abgewöhnen", "თავი დაანებე", "戒除", "Quit")) to Icons.Default.ArrowDownward
                    ),
                    selectedIndex = if (isNegative) 1 else 0,
                    onOptionSelected = { index ->
                        if (!isEditMode) {
                            val newIsNegative = (index == 1)
                            onNegativeChange(newIsNegative)
                            if (newIsNegative) {
                                onNumericChange(false)
                            }
                        }
                    },
                    testTagPrefix = "goal_segment"
                )
            }

            // 2. Art der Messung (Measurement Type): Ja/Nein vs Zahlenbasiert (Only for Positive habits)
            if (!isNegative) {
                HorizontalDivider(color = AppBorder, thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                text = tr(language, "Art der Messung", "გაზომვის ტიპი", "计量方式", "Measurement Type"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = {
                                    onShowInfoDialog(
                                        tr(language, "Art der Messung", "გაზომვის ტიპი", "计量方式说明", "Measurement Type"),
                                        tr(
                                            language,
                                            "• Ja / Nein: Einfaches Abhaken jeden Tag (z. B. Betten machen, Vitamin einnehmen).\n\n• Zahlenbasiert: Täglicher Messwert mit eigener Einheit & Zielwert (z. B. 20 Minuten lesen, 2 Liter trinken, 5 km laufen).",
                                            "• Ja/Nein: Simple daily checkmark.\n• Zahlenbasiert: Daily numeric goal.",
                                            "• 是/否：每日简单打卡。\n• 数值计量：指定具体单位与每日目标。",
                                            "• Yes / No: Simple daily checkmark (e.g. make bed).\n\n• Numeric: Daily tracking with custom unit and daily target (e.g. read 20 minutes, drink 2 liters, run 5 km)."
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
                        if (isEditMode) {
                            Text(
                                text = tr(language, "🔒 Messart fixiert", "🔒 Unit locked", "🔒 计量已固定", "🔒 Unit locked"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    AppSegmentedButton(
                        options = listOf(
                            tr(language, "Ja / Nein", "დიახ / არა", "是 / 否", "Yes / No"),
                            tr(language, "Zahlenbasiert", "რიცხვითი", "数值计量", "Numeric")
                        ),
                        selectedIndex = if (isNumeric) 1 else 0,
                        onOptionSelected = { index ->
                            if (!isEditMode) {
                                onNumericChange(index == 1)
                            }
                        },
                        testTagPrefix = "measurement_type"
                    )
                }

                // 3. Units & Daily targets configuration (when numeric mode is active)
                if (isNumeric) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr(language, "Einheit & Tagesziel", "აირჩიეთ ერთეული", "单位与每日目标", "Unit & Daily Target"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        val standardUnits = if (language == "de") {
                            listOf("Minuten", "km", "Liter", "Stunden", "Mal", "Anderes...")
                        } else {
                            listOf("Minutes", "km", "Liters", "Hours", "Times", "Custom...")
                        }

                        val row1 = standardUnits.take(3)
                        val row2 = standardUnits.drop(3)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row1.forEach { standardUnit ->
                                    val isSelected = selectedChip == standardUnit || (unit.equals(standardUnit, ignoreCase = true) && standardUnit != "Anderes..." && standardUnit != "Custom...")
                                    Button(
                                        onClick = {
                                            onChipSelect(standardUnit)
                                            if (standardUnit != "Anderes..." && standardUnit != "Custom...") {
                                                onUnitChange(standardUnit)
                                            } else {
                                                onUnitChange("")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) activeColor else ProgressTrack,
                                            contentColor = if (isSelected) Color.White else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else AppBorder),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = standardUnit,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row2.forEach { standardUnit ->
                                    val isSelected = selectedChip == standardUnit || (unit.equals(standardUnit, ignoreCase = true) && standardUnit != "Anderes..." && standardUnit != "Custom...")
                                    Button(
                                        onClick = {
                                            onChipSelect(standardUnit)
                                            if (standardUnit != "Anderes..." && standardUnit != "Custom...") {
                                                onUnitChange(standardUnit)
                                            } else {
                                                onUnitChange("")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) activeColor else ProgressTrack,
                                            contentColor = if (isSelected) Color.White else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else AppBorder),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = standardUnit,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedChip == "Anderes..." || selectedChip == "Custom..." || unit !in listOf("Minuten", "Minutes", "km", "Liter", "Liters", "Stunden", "Hours", "Mal", "Times")) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = tr(language, "Eigene Einheit", "Custom Unit"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    AppTextField(
                                        value = unit,
                                        onValueChange = onUnitChange,
                                        placeholderText = tr(language, "z. B. Tassen", "e.g. cups"),
                                        singleLine = true
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tr(language, "Tagesziel", "Daily Target"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AppTextField(
                                    value = targetValue,
                                    onValueChange = onTargetValueChange,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tr(language, "Erhöhung (+)", "Increment (+)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AppTextField(
                                    value = clickIncrement,
                                    onValueChange = onClickIncrementChange,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    placeholderText = "1",
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = AppBorder, thickness = 1.dp)

            // 4. Horizon: Ongoing Routine vs Finishable Goal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tr(language, "Ziel-Horizont", "მიზნის ტიპი", "目标周期", "Goal Horizon"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                onShowInfoDialog(
                                    tr(language, "Ziel-Horizont & Abschließbare Ziele", "მიზნის ჰორიზონტი", "目标周期与可达成目标", "Goal Horizon & Finishable Goals"),
                                    tr(
                                        language,
                                        "• Dauerhafte Routine: Für fortlaufende Gewohnheiten ohne festes Enddatum (z. B. tägliche Meditation, Betten machen).\n\n• Abschließbares Ziel / Challenge: Für Ziele mit konkretem Gesamtziel (z. B. 30 erfolgreiche Tage oder 300 Seiten insgesamt). Sobald erreicht, wandert es als Erfolg in deine Ruhmeshalle!",
                                        "• Dauerhafte Routine: Ongoing habits.\n\n• Abschließbares Ziel: Target goals placed in Hall of Fame!",
                                        "• 持续习惯：日常长期保持的习惯。\n\n• 可达成目标：具有明确终点总量的目标（如 30 天、300 页）。一旦达成即荣耀封存至成就馆！",
                                        "• Ongoing Routine: For indefinite habits without an end date (e.g. daily meditation).\n\n• Finishable Goal / Challenge: For goals with a set milestone target (e.g. 30 successful days or 300 total pages). Once reached, it is celebrated as mastered in your Hall of Fame!"
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
                        tr(language, "Dauerhafte Routine", "მუდმივი ჩვევა", "持续习惯", "Ongoing Routine"),
                        if (isNegative) {
                            tr(language, "Tage-Challenge", "დღეების გამოწვევა", "天数挑战", "Days Challenge")
                        } else {
                            tr(language, "Abschließbares Ziel", "დასრულებადი მიზანი", "可达成目标", "Finishable Goal")
                        }
                    ),
                    selectedIndex = if (isFinishable) 1 else 0,
                    onOptionSelected = { index ->
                        onFinishableChange(index == 1)
                    },
                    testTagPrefix = "goal_horizon"
                )

                // When Finishable Goal is active
                if (isFinishable) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProgressTrack.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Target Amount
                            Text(
                                text = when {
                                    isNegative -> tr(language, "Challenge-Dauer (Tage abstinent/clean)", "გამოწვევის დღეები", "挑战天数（保持戒断）", "Challenge Duration (Days Clean)")
                                    !isNumeric -> tr(language, "Ziel-Anzahl (Erfolgreiche Tage)", "სამიზნე დღეები", "目标达成天数", "Target Days")
                                    else -> {
                                        val u = if (unit.isNotBlank()) " ($unit)" else ""
                                        tr(language, "Gesamtziel zum Abschließen$u", "საერთო მიზანი$u", "达成总目标$u", "Total Target to Complete$u")
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            AppTextField(
                                value = totalTargetValue,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d*)?$"""))) {
                                        onTotalTargetValueChange(input)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholderText = when {
                                    isNegative -> "30"
                                    !isNumeric -> "30"
                                    unit.lowercase() in listOf("km", "kilometer") -> "100"
                                    else -> "300"
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = when {
                                    isNegative -> tr(
                                        language,
                                        "Halte dieses Laster für $totalTargetValue Tage erfolgreich durch, um die Challenge siegreich zu meistern!",
                                        "გაუძელით $totalTargetValue დღეს წარმატებით!",
                                        "坚持 $totalTargetValue 天不破戒，即可成功达成挑战！",
                                        "Stay clean for $totalTargetValue days to successfully conquer this challenge!"
                                    )
                                    !isNumeric -> tr(
                                        language,
                                        "Schließe diese Gewohnheit an insgesamt $totalTargetValue Tagen ab, um sie als gemeistertes Ziel in deine Ruhmeshalle zu überführen.",
                                        "შეასრულეთ $totalTargetValue დღის განმავლობაში.",
                                        "累计完成 $totalTargetValue 天即可解锁专属成就并荣耀封存。",
                                        "Complete this habit on $totalTargetValue total days to master it and add it to your Hall of Fame."
                                    )
                                    else -> {
                                        val u = if (unit.isNotBlank()) " $unit" else ""
                                        tr(
                                            language,
                                            "Sobald du insgesamt $totalTargetValue$u erreicht hast, gilt das Ziel als gemeistert und wandert in deine Ruhmeshalle.",
                                            "როდესაც მიაღწევთ $totalTargetValue$u, მიზანი ჩაითვლება დასრულებულად.",
                                            "一旦你累计完成 $totalTargetValue$u，该目标即被视作圆满达成！",
                                            "Once you reach $totalTargetValue$u in total, this goal is celebrated as mastered!"
                                        )
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )


                        }
                    }
                }
            }

            HorizontalDivider(color = AppBorder, thickness = 1.dp)

            // 5. Schwierigkeitsgrad (Difficulty)
            if (isFinishable) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                text = tr(language, "Schwierigkeitsgrad", "სირთულის დონე", "难度级别", "Difficulty Level"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
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
        }
    }
}

