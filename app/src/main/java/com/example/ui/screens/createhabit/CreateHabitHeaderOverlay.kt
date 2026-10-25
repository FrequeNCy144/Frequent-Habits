package com.example.ui.screens.createhabit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.ModernBackButton
import com.example.ui.theme.AppBg
import com.example.ui.theme.AppBorder
import com.example.ui.theme.TextPrimary

@Composable
fun CreateHabitHeaderOverlay(
    isEditing: Boolean,
    language: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = com.example.ui.theme.PrimaryViolet,
    onSaveClick: (() -> Unit)? = null,
    customTitle: String? = null,
    stepperContent: (@Composable () -> Unit)? = null
) {
    if (stepperContent != null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(AppBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernBackButton(
                    onClick = onDismiss,
                    testTag = "create_back_button"
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = customTitle ?: if (isEditing) {
                        tr(language, "Gewohnheit bearbeiten", "ჩვევის რედაქტირება", "编辑习惯", "Edit Habit")
                    } else {
                        tr(language, "Neue Gewohnheit", "ახალი ჩვევა", "新建习惯", "New Habit")
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp)
                )
                if (onSaveClick != null) {
                    Surface(
                        onClick = onSaveClick,
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, accentColor),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("header_save_habit_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            stepperContent()

            HorizontalDivider(
                color = AppBorder.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppBg,
                            AppBg,
                            AppBg.copy(alpha = 0.9f),
                            AppBg.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernBackButton(
                    onClick = onDismiss,
                    testTag = "create_back_button"
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = customTitle ?: if (isEditing) {
                        tr(language, "Gewohnheit bearbeiten", "ჩვევის რედაქტირება", "编辑习惯", "Edit Habit")
                    } else {
                        tr(language, "Neue Gewohnheit", "ახალი ჩვევა", "新建习惯", "New Habit")
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp)
                )
                if (onSaveClick != null) {
                    Surface(
                        onClick = onSaveClick,
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, accentColor),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("header_save_habit_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
