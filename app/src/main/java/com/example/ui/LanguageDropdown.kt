package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        "en" to "English 🇬🇧",
        "de" to "Deutsch 🇩🇪",
        "fr" to "Français 🇫🇷",
        "ka" to "ქართული 🇬🇪",
        "zh" to "简体中文 🇨🇳"
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = languages.find { it.first == currentLanguage }?.second ?: "English 🇬🇧"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = com.example.ui.theme.ProgressTrack,
                unfocusedContainerColor = com.example.ui.theme.ProgressTrack,
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = com.example.ui.theme.AppBorder,
                focusedTextColor = com.example.ui.theme.TextPrimary,
                unfocusedTextColor = com.example.ui.theme.TextPrimary
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(com.example.ui.theme.ProgressTrack)
        ) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontWeight = if (code == currentLanguage) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
