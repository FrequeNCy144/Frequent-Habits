package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearFocusOnKeyboardDismiss() {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible) {
            focusManager.clearFocus(force = true)
            wasImeVisible = false
        }
    }

    if (wasImeVisible || isImeVisible) {
        BackHandler {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            wasImeVisible = false
        }
    }
}
