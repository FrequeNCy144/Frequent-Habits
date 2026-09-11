package com.example.ui.screens.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BackupManager
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.theme.*

fun LazyListScope.dataBackupSettingsSection(
    language: String,
    autoBackupFolderUri: String?,
    lastBackupTime: String,
    onShowArchivedList: () -> Unit,
    onPickFolder: () -> Unit,
    onManualBackup: () -> Unit,
    onPickRestoreFile: () -> Unit,
    onPickCsvFile: () -> Unit,
    onShowWipeDialog: () -> Unit,
    viewModel: HabitsViewModel
) {
                    // SUBPAGE 5: DATEN, SICHERUNG & ARCHIV
                    // Card 1: Archivierte Gewohnheiten
                    item {
                        val archiveInteractionSource = remember { MutableInteractionSource() }
                        val archiveIsPressed by archiveInteractionSource.collectIsPressedAsState()
                        val archiveArrowOffsetX by animateDpAsState(
                            targetValue = if (archiveIsPressed) 8.dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "archive_arrow_offset"
                        )
                        val archiveArrowScale by animateFloatAsState(
                            targetValue = if (archiveIsPressed) 1.25f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "archive_arrow_scale"
                        )

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
                                    Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Archivierte Gewohnheiten", "Archived Habits"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = archiveInteractionSource,
                                            indication = ripple(),
                                            onClick = onShowArchivedList
                                        )
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Inbox,
                                            contentDescription = "Archive",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tr(language, "Archiv öffnen", "Open Archive"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tr(language, "Inaktive Gewohnheiten ansehen & reaktivieren", "View & reactivate suspended habits"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Go",
                                        tint = if (archiveIsPressed) PrimaryViolet else TextSecondary,
                                        modifier = Modifier
                                            .offset(x = archiveArrowOffsetX)
                                            .scale(archiveArrowScale)
                                            .size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Loop Habit Tracker Import (ZIP / CSV)
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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = tr(language, "Loop Habit Tracker Import", "Loop Habit Tracker Import"),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = PrimaryViolet.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Text(
                                            text = "ZIP / CSV",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryViolet,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(
                                        language,
                                        "Importiere deine gesamte Historie (ZIP) direkt aus dem Loop Habit Tracker, oder lade einzelne CSV-Dateien (Loop, Bull, Custom) hoch.",
                                        "Import your entire history (ZIP) directly from Loop Habit Tracker, or upload individual CSV files (Loop, Bull, Custom)."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = onPickCsvFile,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = "Import", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr(language, "CSV / ZIP Datei importieren", "Import CSV / ZIP File"))
                                }
                            }
                        }
                    }

                    // Card 3: Lokales SAF Backup & Wiederherstellung
                    item {
                        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
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
                                    Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Backup & Wiederherstellung", "Backup & Restore"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(
                                        language,
                                        "Wähle einen Ordner aus. Die App erstellt dort täglich automatisch ein Backup der letzten 3 Tage. Du kannst auch jederzeit manuell sichern oder wiederherstellen.",
                                        "Select a local folder. The app will automatically save daily JSON exports there (retaining only the 3 latest). You can also back up or restore manually."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = tr(language, "Ausgewählter Ordner:", "Selected Folder:"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ProgressTrack)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (autoBackupFolderUri.isNullOrEmpty()) {
                                            tr(language, "Kein Ordner ausgewählt", "No folder selected")
                                        } else {
                                            autoBackupFolderUri
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (autoBackupFolderUri.isNullOrEmpty()) ErrorRed else SuccessGreen,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = onPickFolder,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "Folder", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr(language, "Ordner auswählen", "Select Folder"))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onManualBackup,
                                        enabled = !autoBackupFolderUri.isNullOrEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Backup, contentDescription = "Backup", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr(language, "Sichern", "Backup"))
                                    }

                                    Button(
                                        onClick = onPickRestoreFile,
                                        colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr(language, "Einspielen", "Restore"))
                                    }
                                }

                                if (syncStatus != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = syncStatus ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { viewModel.clearSyncStatus() }
                                    )
                                }
                            }
                        }
                    }

                    // Card 4: Gefahrenbereich
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
                                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Gefahrenbereich", "Danger Zone"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(language, "Hiermit werden alle Gewohnheiten unwiderruflich gelöscht.", "This permanently deletes all habits and tracking history."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Button(
                                    onClick = { onShowWipeDialog() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Wipe", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr(language, "Alles löschen", "Wipe All Data"))
                                }
                            }
                        }
                    }
}
