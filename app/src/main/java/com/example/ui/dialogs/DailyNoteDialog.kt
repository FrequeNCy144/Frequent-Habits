package com.example.ui.dialogs

import com.example.ui.components.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.components.ClearFocusOnKeyboardDismiss
import com.example.ui.components.StandardSheetDragHandle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyNoteDialog(
    currentNote: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember(currentNote) { mutableStateOf(currentNote) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { StandardSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = tr(language, "Tägliche Notiz", "ყოველდღიური შენიშვნა", "每日随笔", "Daily Note"),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            ClearFocusOnKeyboardDismiss()
            AppTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholderText = tr(language, "Gedanken, Erfolge oder Notizen für diesen Tag...", "აზრები, მიღწევები თუ ნოტები ამ დღისთვის...", "这一天的心得体会、小成就 or 备忘...", "Thoughts, achievements, or notes for this day..."),
                modifier = Modifier
                    .heightIn(min = 140.dp, max = 240.dp)
                    .focusRequester(focusRequester),
                singleLine = false,
                testTag = "daily_note_input"
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onSave(noteText) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = tr(language, "Speichern", "შენახვა", "保存", "Save"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
