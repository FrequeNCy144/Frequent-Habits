package com.example.ui.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.ui.screens.profile.AchievementBadge
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.tr
import com.example.ui.components.*
import com.example.ui.theme.*




data class StandardTrophyOption(
    val id: String,
    val type: String,
    val tier: String,
    val titleDe: String,
    val titleEn: String,
    val descDe: String,
    val descEn: String,
    val titleKa: String = titleEn,
    val descKa: String = descEn,
    val titleZh: String = titleEn,
    val descZh: String = descEn
) {
    fun getLocalizedTitle(language: String): String = when(language) {
        "de" -> titleDe
        "ka" -> titleKa
        "zh" -> titleZh
        else -> titleEn
    }
    fun getLocalizedDesc(language: String): String = when(language) {
        "de" -> descDe
        "ka" -> descKa
        "zh" -> descZh
        else -> descEn
    }
}

val STANDARD_TROPHIES = listOf(
    StandardTrophyOption("COMP_10", "COMPLETIONS", "COMP_10", "10 Abschlüsse", "10 Completions", "Insgesamt 10 Gewohnheits-Abschlüsse erreichen", "Achieve 10 total completions", "10 დასრულება", "მიაღწიეთ 10 დასრულებას", "10 次完成", "累计达成 10 次习惯完成"),
    StandardTrophyOption("COMP_50", "COMPLETIONS", "COMP_50", "50 Abschlüsse", "50 Completions", "Insgesamt 50 Gewohnheits-Abschlüsse erreichen", "Achieve 50 total completions", "50 დასრულება", "მიაღწიეთ 50 დასრულებას", "50 次完成", "累计达成 50 次习惯完成"),
    StandardTrophyOption("COMP_200", "COMPLETIONS", "COMP_200", "200 Abschlüsse", "200 Completions", "Insgesamt 200 Gewohnheits-Abschlüsse erreichen", "Achieve 200 total completions", "200 დასრულება", "მიაღწიეთ 200 დასრულებას", "200 次完成", "累计达成 200 次习惯完成"),
    StandardTrophyOption("COMP_500", "COMPLETIONS", "COMP_500", "500 Abschlüsse", "500 Completions", "Insgesamt 500 Gewohnheits-Abschlüsse erreichen", "Achieve 500 total completions", "500 დასრულება", "მიაღწიეთ 500 დასრულებას", "500 次完成", "累计达成 500 次习惯完成"),
    StandardTrophyOption("COMP_1000", "COMPLETIONS", "COMP_1000", "1000 Abschlüsse", "1000 Completions", "Insgesamt 1000 Gewohnheits-Abschlüsse erreichen", "Achieve 1000 total completions", "1000 დასრულება", "მიაღწიეთ 1000 დასრულებას", "1000 次完成", "累计达成 1000 次习惯完成"),
    StandardTrophyOption("WOOD", "STREAK", "WOOD", "7 Tage Serie (Holz)", "7-Day Streak (Wood)", "Eine 7-Tage-Serie erreichen", "Achieve a 7-day streak", "7-დღიანი სერია (ხე)", "მიაღწიეთ 7-დღიან სერიას", "7 天连续（木质）", "达成 7 天连续打卡"),
    StandardTrophyOption("BRONZE", "STREAK", "BRONZE", "14 Tage Serie (Bronze)", "14-Day Streak (Bronze)", "Eine 14-Tage-Serie erreichen", "Achieve a 14-day streak", "14-დღიანი სერია (ბრინჯაო)", "მიაღწიეთ 14-დღიან სერიას", "14 天连续（青铜）", "达成 14 天连续打卡"),
    StandardTrophyOption("SILVER", "STREAK", "SILVER", "30 Tage Serie (Silber)", "30-Day Streak (Silver)", "Eine 30-Tage-Serie erreichen", "Achieve a 30-day streak", "30-დღიანი სერია (ვერცხლი)", "მიაღწიეთ 30-დღიან სერიას", "30 天连续（白银）", "达成 30 天连续打卡"),
    StandardTrophyOption("GOLD", "STREAK", "GOLD", "100 Tage Serie (Gold)", "100-Day Streak (Gold)", "Eine 100-Tage-Serie erreichen", "Achieve a 100-day streak", "100-დღიანი სერია (ოქრო)", "მიაღწიეთ 100-დღიან სერიას", "100 天连续（黄金）", "达成 100 天连续打卡"),
    StandardTrophyOption("DIAMOND", "STREAK", "DIAMOND", "365 Tage Serie (Diamant)", "365-Day Streak (Diamond)", "Eine 365-Tage-Serie erreichen", "Achieve a 365-day streak", "365-დღიანი სერია (ბრილიანტი)", "მიაღწიეთ 365-დღიან სერიას", "365 天连续（钻石）", "达成 365 天连续打卡"),
    StandardTrophyOption("PERF_7", "PERFECT_DAYS", "PERF_7", "7 perfekte Tage", "7 Perfect Days", "7 perfekte Tage am Stück", "7 consecutive perfect days", "7 სრულყოფილი დღე", "7 სრულყოფილი დღე ზედიზედ", "7 个完美天", "连续达成 7 个完美天"),
    StandardTrophyOption("PERF_30", "PERFECT_DAYS", "PERF_30", "30 perfekte Tage", "30 Perfect Days", "30 perfekte Tage am Stück", "30 consecutive perfect days", "30 სრულყოფილი დღე", "30 სრულყოფილი დღე ზედიზედ", "30 个完美天", "连续达成 30 个完美天"),
    StandardTrophyOption("PERF_365", "PERFECT_DAYS", "PERF_365", "365 perfekte Tage", "365 Perfect Days", "365 perfekte Tage am Stück", "365 consecutive perfect days", "365 სრულყოფილი დღე", "365 სრულყოფილი დღე ზედიზედ", "365 个完美天", "连续达成 365 个完美天"),
    StandardTrophyOption("PERF_1000", "PERFECT_DAYS", "PERF_1000", "1000 perfekte Tage", "1000 Perfect Days", "1000 perfekte Tage am Stück", "1000 consecutive perfect days", "1000 სრულყოფილი დღე", "1000 სრულყოფილი დღე ზედიზედ", "1000 个完美天", "连续达成 1000 个完美天")
)

@Composable
fun AddMilestoneRewardDialog(
    language: String,
    onDismiss: () -> Unit,
    onAdd: (com.example.data.MilestoneReward) -> Unit
) {
    var rewardText by remember { mutableStateOf("") }
    var rewardDesc by remember { mutableStateOf("") }
    var conditionType by remember { mutableStateOf("STREAK") }
    var conditionValueStr by remember { mutableStateOf("") }
    
    var mode by remember { mutableStateOf("TROPHY") } // "TROPHY" or "CUSTOM"
    var selectedTrophyId by remember { mutableStateOf("WOOD") }
    
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppCard,
        title = {
            Text(
                text = tr(language, "Belohnung definieren", "Define Reward"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(scrollState)) {
                
                AppTextField(
                    value = rewardText,
                    onValueChange = { rewardText = it },
                    labelText = tr(language, "Name der Belohnung (z.B. Kino Abend)", "Reward Name (e.g. Cinema Night)"),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                AppTextField(
                    value = rewardDesc,
                    onValueChange = { rewardDesc = it },
                    labelText = tr(language, "Details / Link (optional, z.B. https://...)", "Details / Link (optional, e.g. https://...)"),
                    placeholderText = "https://...",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    singleLine = false
                )

                AppSegmentedButton(
                    options = listOf(
                        tr(language, "Trophäe", "Trophy"),
                        tr(language, "Manuell", "Manual")
                    ),
                    selectedIndex = if (mode == "TROPHY") 0 else 1,
                    onOptionSelected = { index -> mode = if (index == 0) "TROPHY" else "CUSTOM" },
                    testTagPrefix = "reward_mode_switch"
                )
                
                if (mode == "CUSTOM") {
                    Text(
                        text = tr(language, "Bedingung:", "Condition:"),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    AppSegmentedButton(
                        options = listOf(
                            tr(language, "Streak (Tage)", "Streak (Days)"),
                            tr(language, "Gesamt (Mal)", "Total (Times)")
                        ),
                        selectedIndex = if (conditionType == "STREAK") 0 else 1,
                        onOptionSelected = { index -> conditionType = if (index == 0) "STREAK" else "COMPLETIONS" },
                        testTagPrefix = "reward_condition_switch"
                    )
                    
                    AppTextField(
                        value = conditionValueStr,
                        onValueChange = { conditionValueStr = it.filter { ch -> ch.isDigit() } },
                        labelText = tr(language, "Ziel-Wert", "Target Value"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = tr(language, "Gewohnheitsspezifische Trophäe:", "Habit Trophy:"),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        STANDARD_TROPHIES.filter { it.type == "STREAK" }.forEach { trophy ->
                            val isSelected = selectedTrophyId == trophy.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryViolet.copy(alpha = 0.2f) else ProgressTrack
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryViolet else AppBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTrophyId = trophy.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AchievementBadge(
                                        type = trophy.type,
                                        tier = trophy.tier,
                                        isUnlocked = true,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = trophy.getLocalizedTitle(language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = trophy.getLocalizedDesc(language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rewardText.isNotBlank()) {
                        if (mode == "CUSTOM" && conditionValueStr.isNotBlank()) {
                            val condVal = conditionValueStr.toIntOrNull() ?: 1
                            onAdd(com.example.data.MilestoneReward(
                                habitId = 0,
                                rewardText = rewardText.trim(),
                                description = rewardDesc.trim(),
                                conditionType = conditionType,
                                conditionValue = condVal
                            ))
                        } else if (mode == "TROPHY") {
                            onAdd(com.example.data.MilestoneReward(
                                habitId = 0,
                                rewardText = rewardText.trim(),
                                description = rewardDesc.trim(),
                                conditionType = "TROPHY_COUPLED",
                                conditionValue = 0,
                                trophyId = selectedTrophyId
                            ))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                enabled = rewardText.isNotBlank() && (mode == "TROPHY" || conditionValueStr.isNotBlank())
            ) {
                Text(tr(language, "Hinzufügen", "დამატება", "添加", "Add"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddCustomReminderDialog(
    language: String,
    initialTime: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember {
        val parsedHour = initialTime?.split(":")?.getOrNull(0)?.toIntOrNull()
        mutableStateOf(parsedHour ?: 12)
    }
    var minute by remember {
        val parsedMin = initialTime?.split(":")?.getOrNull(1)?.toIntOrNull()
        mutableStateOf(parsedMin ?: 0)
    }
    
    var hourStr by remember(hour) { mutableStateOf(hour.toString().padStart(2, '0')) }
    var minuteStr by remember(minute) { mutableStateOf(minute.toString().padStart(2, '0')) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        containerColor = AppCard,
        title = {
            Text(
                text = tr(language, "Erinnerung hinzufügen", "დაამატეთ შეხსენება", "添加提醒", "Add Reminder"),
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour TextField
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tr(language, "Stunde (0-23)", "საათი (0-23)", "小时 (0-23)", "Hour (0-23)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        AppTextField(
                            value = hourStr,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(2)
                                hourStr = filtered
                                val parsed = filtered.toIntOrNull()
                                if (parsed != null && parsed in 0..23) {
                                    hour = parsed
                                }
                            },
                            placeholderText = "12",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = ":",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 20.dp)
                    )

                    // Minute TextField
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tr(language, "Minute (0-59)", "წუთი (0-59)", "分钟 (0-59)", "Minute (0-59)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        AppTextField(
                            value = minuteStr,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(2)
                                minuteStr = filtered
                                val parsed = filtered.toIntOrNull()
                                if (parsed != null && parsed in 0..59) {
                                    minute = parsed
                                }
                            },
                            placeholderText = "00",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Quick steppers (+1h, -1h, +15m, -15m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val stepButtons = listOf(
                        "-1h" to {
                            val h = ((hourStr.toIntOrNull() ?: hour) - 1 + 24) % 24
                            hour = h
                            hourStr = h.toString().padStart(2, '0')
                        },
                        "+1h" to {
                            val h = ((hourStr.toIntOrNull() ?: hour) + 1) % 24
                            hour = h
                            hourStr = h.toString().padStart(2, '0')
                        },
                        "-15m" to {
                            val m = ((minuteStr.toIntOrNull() ?: minute) - 15 + 60) % 60
                            minute = m
                            minuteStr = m.toString().padStart(2, '0')
                        },
                        "+15m" to {
                            val m = ((minuteStr.toIntOrNull() ?: minute) + 15) % 60
                            minute = m
                            minuteStr = m.toString().padStart(2, '0')
                        }
                    )
                    stepButtons.forEach { (label, action) ->
                        Surface(
                            color = ProgressTrack,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AppBorder),
                            modifier = Modifier
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    action()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("07:00", "08:30", "12:00", "18:00", "21:00").forEach { preset ->
                        val parts = preset.split(":")
                        val ph = parts[0].toInt()
                        val pm = parts[1].toInt()
                        val isSelected = (hourStr.toIntOrNull() ?: hour) == ph && (minuteStr.toIntOrNull() ?: minute) == pm
                        Surface(
                            color = if (isSelected) PrimaryViolet else ProgressTrack,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) PrimaryViolet else AppBorder),
                            modifier = Modifier
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    hour = ph
                                    minute = pm
                                    hourStr = parts[0]
                                    minuteStr = parts[1]
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                onClick = {
                    val finalHour = (hourStr.toIntOrNull() ?: hour).coerceIn(0, 23)
                    val finalMin = (minuteStr.toIntOrNull() ?: minute).coerceIn(0, 59)
                    onConfirm(finalHour, finalMin)
                }
            ) {
                Text(tr(language, "Speichern", "შენახვა", "保存", "Save"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"), color = TextSecondary)
            }
        }
    )
}

// COMPACT CUSTOM DATE PICKER

@Composable
fun SimpleDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val dpd = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(year, month, dayOfMonth)
            },
            initialYear,
            initialMonth,
            initialDay
        )
        dpd.setOnDismissListener { onDismiss() }
        dpd.show()
        onDispose {
            dpd.dismiss()
        }
    }
}

@Composable
fun ExplanationDialog(
    title: String,
    explanation: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppCard,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryViolet
                )
            }
        }
    )
}
