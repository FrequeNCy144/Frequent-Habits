package com.example.ui.screens.createhabit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.theme.*

@Composable
fun CreateHabitBottomBar(
    isEditing: Boolean,
    language: String,
    onCancel: () -> Unit,
    onSaveClick: () -> Unit
) {
    val buttonText = if (isEditing) {
        tr(language, "Änderungen speichern", "ცვლილებების შენახვა", "保存修改", "Save Changes")
    } else {
        tr(language, "Gewohnheit erstellen", "ჩვევის შექმნა", "创建习惯", "Create Habit")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("cancel_habit_button"),
            border = BorderStroke(1.dp, AppBorder),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Text(
                text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Button(
            onClick = onSaveClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
            modifier = Modifier
                .weight(1.2f)
                .height(50.dp)
                .testTag("save_habit_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
