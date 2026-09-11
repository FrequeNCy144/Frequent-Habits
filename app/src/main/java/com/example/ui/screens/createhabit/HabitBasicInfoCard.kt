package com.example.ui.screens.createhabit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

@Composable
fun HabitBasicInfoCard(
    language: String,
    name: String,
    onNameChange: (String) -> Unit,
    nameError: Boolean,
    onNameErrorChange: (Boolean) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    nameFocusRequester: FocusRequester
) {
            // 1. CARD: Basis-Informationen
            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (nameError) FailedRed else AppBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (nameError) FailedRed else PrimaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tr(language, "Basis-Informationen", "ძირითადი ინფორმაცია", "基本信息", "Basic Information"),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (nameError) FailedRed else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Name field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr(language, "Name der Gewohnheit", "ჩვევის სახელი", "习惯名称", "Habit Name"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (nameError) FailedRed else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        AppTextField(
                            value = name,
                            onValueChange = { 
                                onNameChange(it)
                                if (it.isNotBlank()) onNameErrorChange(false)
                            },
                            placeholderText = tr(language, "z.B. Meditieren, Laufen, Lesen...", "მაგ., მედიტაცია, სირბილი, კითხვა...", "例如：冥想、跑步、阅读...", "e.g., Meditate, Running, Reading..."),
                            isError = nameError,
                            supportingText = {
                                if (nameError) {
                                    Text(
                                        text = tr(language, "Bitte gib einen Namen ein", "გთხოვთ შეიყვანოთ სახელი", "请输入名称", "Please enter a name"),
                                        color = FailedRed,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            modifier = Modifier.focusRequester(nameFocusRequester),
                            testTag = "habit_name_input",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (nameError) FailedRed else if (name.isNotBlank()) PrimaryViolet else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    // Description field (optional)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr(language, "Beschreibung (optional)", "აღწერა (სურვილისამებრ)", "描述说明（可选）", "Description (optional)"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        AppTextField(
                            value = description,
                            onValueChange = { onDescriptionChange(it) },
                            placeholderText = tr(language, "z.B. Notizen, Motivation oder Regeln...", "მაგ. შენიშვნები, მოტივაცია თუ წესები...", "例如：备忘、动力寄语或规则...", "e.g. Notes, motivation or rules..."),
                            testTag = "habit_description_input",
                            singleLine = false,
                            minLines = 1,
                            maxLines = 4,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (description.isNotBlank()) PrimaryViolet else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
}
