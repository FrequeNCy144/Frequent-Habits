package com.example.ui.screens.createhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.ModernBackButton
import com.example.ui.theme.AppBg
import com.example.ui.theme.TextPrimary

@Composable
fun CreateHabitHeaderOverlay(
    isEditing: Boolean,
    language: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = if (isEditing) {
                    tr(language, "Gewohnheit bearbeiten", "ჩვევის რედაქტირება", "编辑习惯", "Edit Habit")
                } else {
                    tr(language, "Neue Gewohnheit", "ახალი ჩვევა", "新建习惯", "New Habit")
                },
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
