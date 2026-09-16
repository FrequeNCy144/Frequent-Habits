package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.theme.*
import com.frequent.habits.R
import java.io.File

fun LazyListScope.audioSettingsSection(
    language: String,
    importedAudios: List<File>,
    selectedAudioFile: File?,
    onShowAudioDialog: () -> Unit,
    onImportAudioClick: () -> Unit,
    onSelectAudio: ((File?) -> Unit)? = null
) {
    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tr(language, "Fokus-Sounds & Hintergründe", "ფოკუსირების ხმები", "专注白噪音与背景音", "Focus Sounds & Soundscapes"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Selected Sound Card (if one is selected)
                if (selectedAudioFile != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1A33),
                        border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_volume_up),
                                contentDescription = null,
                                tint = PrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedAudioFile.nameWithoutExtension,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tr(language, "Standard Fokus-Sound", "ნაგულისხმევი ხმა", "默认专注白噪音", "Default Focus Sound"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                            if (onSelectAudio != null) {
                                IconButton(
                                    onClick = { onSelectAudio(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove audio",
                                        tint = FailedRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search & Add Sound Row (matching Audio Timer layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isAudioDropdownOpen by remember { mutableStateOf(false) }

                    // Search Dropdown trigger box
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { isAudioDropdownOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            color = ProgressTrack,
                            border = BorderStroke(1.dp, AppBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    val audioDisplayName = selectedAudioFile?.nameWithoutExtension
                                    Text(
                                        text = audioDisplayName ?: tr(language, "Sound suchen...", "ხმის ძებნა...", "搜索背景音...", "Search sound..."),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (audioDisplayName != null) TextPrimary else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isAudioDropdownOpen,
                            onDismissRequest = { isAudioDropdownOpen = false },
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .background(ProgressTrack)
                                .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = tr(language, "Kein Sound (Stumm)", "ხმის გარეშე", "无声音 (静音)", "No Sound (Mute)"),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onSelectAudio?.invoke(null)
                                    isAudioDropdownOpen = false
                                }
                            )

                            if (importedAudios.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tr(language, "Keine Sounds importiert (+ tippen)", "ხმები არ არის (+ შეეხეთ)", "未导入任何音效 (点击 +)", "No sounds imported (tap +)"),
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        isAudioDropdownOpen = false
                                        onImportAudioClick()
                                    }
                                )
                            } else {
                                importedAudios.forEach { file ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = file.nameWithoutExtension,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (selectedAudioFile?.absolutePath == file.absolutePath) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_music),
                                                contentDescription = null,
                                                tint = PrimaryViolet,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            onSelectAudio?.invoke(file)
                                            isAudioDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add Button (+)
                    Surface(
                        onClick = onImportAudioClick,
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryViolet,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add audio",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Manage Sounds button
                Button(
                    onClick = onShowAudioDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Klangwelten verwalten", "ხმების მართვა", "管理白噪音", "Manage Soundscapes"),
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
