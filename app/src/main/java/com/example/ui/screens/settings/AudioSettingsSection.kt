package com.example.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.theme.*
import java.io.File

fun LazyListScope.audioSettingsSection(
    language: String,
    importedAudios: List<File>,
    selectedAudioFile: File?,
    onShowAudioDialog: () -> Unit,
    onImportAudioClick: () -> Unit
) {
                    // SUBPAGE 3: TÖNE & ENTSPANNUNG
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCard),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Fokus-Sounds & Hintergründe", "Focus Sounds & Soundscapes"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tr(language, "Klangwelten verwalten", "Manage Soundscapes"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = when (language) {
                                                "de" -> {
                                                    val count = importedAudios.size
                                                    val currentName = selectedAudioFile?.nameWithoutExtension ?: "Kein Sound"
                                                    "$count Sounds • Standard: $currentName"
                                                }
                                                "ka" -> {
                                                    val count = importedAudios.size
                                                    val currentName = selectedAudioFile?.nameWithoutExtension ?: "ხმის გარეშე"
                                                    "$count ხმოვანი გარემო • ნაგულისხმევი: $currentName"
                                                }
                                                "zh" -> {
                                                    val count = importedAudios.size
                                                    val currentName = selectedAudioFile?.nameWithoutExtension ?: "无默认音效"
                                                    "$count 个音效 • 默认：$currentName"
                                                }
                                                "fr" -> {
                                                    val count = importedAudios.size
                                                    val currentName = selectedAudioFile?.nameWithoutExtension ?: "Aucun son"
                                                    "$count sons • Par défaut : $currentName"
                                                }
                                                else -> {
                                                    val count = importedAudios.size
                                                    val currentName = selectedAudioFile?.nameWithoutExtension ?: "No Sound"
                                                    "$count sounds • Default: $currentName"
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onShowAudioDialog() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr(language, "Verwalten", "Manage"), fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onImportAudioClick() },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SuccessGreen)
                                    ) {
                                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr(language, "Import", "იმპორტი", "导入", "Import"), color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
}