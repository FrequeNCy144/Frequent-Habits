package com.example.ui.components.dialogs.addvalue

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.example.ui.components.AudioSoundscapeManager
import com.example.FocusTimerManager
import com.example.tr
import com.example.ui.theme.AppBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.ProgressTrack
import com.example.ui.theme.TextSecondary
import com.frequent.habits.R
import java.io.File

@Composable
fun WidgetTimerAudioPicker(
    context: Context,
    language: String,
    importedAudios: List<File>,
    selectedAudioFile: File?,
    onSelectAudio: (File?) -> Unit,
    onLaunchAudioPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAudioDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = tr(language, "Hintergrund-Audio", "ფონური აუდიო", "背景白噪音", "Background Audio"),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = TextSecondary
        )

        // Active Selected Audio Card
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
                            text = tr(language, "Aktiver Timer-Sound", "აქტიური ტაიმერის ხმა", "当前计时器背景音", "Active Timer Sound"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp
                            ),
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            onSelectAudio(null)
                            AudioSoundscapeManager.setLastSelectedAudio(context, "")
                            FocusTimerManager.updateAudio(context, null)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove audio",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Search & Add Sound Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                                color = TextSecondary,
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
                        .background(ProgressTrack)
                        .border(1.dp, AppBorder, RoundedCornerShape(8.dp))
                ) {
                    if (importedAudios.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = tr(language, "Keine Sounds importiert (+ tippen)", "No sounds imported (tap +)"),
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                isAudioDropdownOpen = false
                                onLaunchAudioPicker()
                            }
                        )
                    } else {
                        importedAudios.forEach { file ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = file.nameWithoutExtension,
                                        color = Color.White,
                                        fontSize = 13.sp,
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
                                    onSelectAudio(file)
                                    AudioSoundscapeManager.setLastSelectedAudio(context, file.name)
                                    FocusTimerManager.updateAudio(context, file)
                                    isAudioDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // Add Button (+)
            Surface(
                onClick = onLaunchAudioPicker,
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
    }
}
