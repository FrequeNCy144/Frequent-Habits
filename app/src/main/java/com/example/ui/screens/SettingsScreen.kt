package com.example.ui.screens

import com.example.ui.HabitIconMapping
import com.example.ui.dialogs.*

import com.example.ui.components.*

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AudioSoundscapeManager
import com.example.LocalDarkMode
import com.example.LocalHapticsEnabled
import com.example.LocalInfoCardsEnabled
import com.example.data.BackupManager
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.components.AudioSoundscapeDialog
import com.example.ui.dialogs.CsvImportPreviewDialog
import com.example.ui.components.ModernBackButton
import com.example.ui.screens.settings.*
import com.example.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: HabitsViewModel,
    language: String,
    initialSubpage: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val backupFolderUri by viewModel.backupFolderUri.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val widgetOpacity by viewModel.widgetOpacity.collectAsStateWithLifecycle()
    val infoCardsEnabled by viewModel.infoCardsEnabled.collectAsStateWithLifecycle()
    val reviewNotificationsEnabled by viewModel.reviewNotificationsEnabled.collectAsStateWithLifecycle()
    val insightNotificationsEnabled by viewModel.insightNotificationsEnabled.collectAsStateWithLifecycle()
    val smartInsightsInAppEnabled by viewModel.smartInsightsInAppEnabled.collectAsStateWithLifecycle()
    val monthlyReviewEnabled by viewModel.monthlyReviewEnabled.collectAsStateWithLifecycle()
    val yearlyReviewEnabled by viewModel.yearlyReviewEnabled.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.profileImageUri.collectAsStateWithLifecycle()

    var selectedSubpage by rememberSaveable { mutableStateOf<String?>(initialSubpage) }
    BackHandler(enabled = selectedSubpage != null) {
        selectedSubpage = null
    }

    var showWipeConfirm by remember { mutableStateOf(false) }
    var showArchivedList by remember { mutableStateOf(false) }
    var importedAudios by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedAudioFile by remember { mutableStateOf<File?>(null) }
    var showAudioDialog by remember { mutableStateOf(false) }

    val profileImagePickerLauncherSettings = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val file = File(context.filesDir, "profile_avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateProfileImageUri(Uri.fromFile(file).toString())
                Toast.makeText(
                    context,
                    tr(language, "Profilbild aktualisiert", "პროფილის სურათი განახლდა", "头像已更新", "Profile picture updated"),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasNotificationPermission = isGranted
        viewModel.setReviewNotificationsEnabled(isGranted)
    }

    val refreshAudios = remember(context) {
        {
            val dir = File(context.filesDir, "audios")
            if (!dir.exists()) dir.mkdirs()
            importedAudios = dir.listFiles()?.filter {
                it.isFile && (it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "aac"))
            }?.sortedBy { it.name } ?: emptyList()

            val savedName = AudioSoundscapeManager.getLastSelectedAudio(context)
            selectedAudioFile = if (savedName.isNotEmpty()) {
                importedAudios.find { it.name == savedName }
            } else {
                null
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAudios()
    }

    val audioPickerLauncherSettings = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "imported_audio_${System.currentTimeMillis()}.mp3"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val dispName = cursor.getString(nameIndex)
                        if (!dispName.isNullOrEmpty()) {
                            fileName = dispName
                        }
                    }
                }
                val destDir = File(context.filesDir, "audios")
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = File(destDir, fileName)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                refreshAudios()
                selectedAudioFile = destFile
                AudioSoundscapeManager.setLastSelectedAudio(context, destFile.name)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showArchivedList) {
        ArchivedHabitsSubpage(
            language = language,
            viewModel = viewModel,
            onDismiss = { showArchivedList = false }
        )
    } else {
        val folderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                viewModel.saveBackupFolderUri(uri.toString())
            }
        }

        val fileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        viewModel.triggerManualRestoreFromStream(inputStream)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading backup file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val csvPreview by viewModel.csvPreviewState.collectAsStateWithLifecycle()
        val isAnalyzingCsv by viewModel.isAnalyzingCsv.collectAsStateWithLifecycle()

        val csvPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.analyzeCsvImport(uri)
            }
        }

        if (csvPreview != null || isAnalyzingCsv) {
            CsvImportPreviewDialog(
                preview = csvPreview,
                isAnalyzing = isAnalyzingCsv,
                language = language,
                onConfirm = { replace -> viewModel.confirmCsvImport(replace) },
                onDismiss = { viewModel.dismissCsvPreview() }
            )
        }

        AnimatedContent(
            targetState = selectedSubpage,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                } else if (targetState == null && initialState != null) {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                } else {
                    fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
                }
            },
            label = "settings_page_transition",
            modifier = Modifier.fillMaxSize()
        ) { currentSubpage ->
            Column(modifier = Modifier.fillMaxSize().background(AppBg)) {
                // Top Header
                Surface(
                    color = AppBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernBackButton(
                            onClick = {
                                if (currentSubpage != null) {
                                    selectedSubpage = null
                                } else {
                                    onBack()
                                }
                            },
                            testTag = "settings_back_button"
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        val headerTitle = when (currentSubpage) {
                            "profile" -> tr(language, "Profil & Konto", "Profile & Account")
                            "appearance" -> tr(language, "Erscheinungsbild & Sprache", "Appearance & Language")
                            "notifications" -> tr(language, "Push-Benachrichtigungen", "Push Notifications")
                            "audio" -> tr(language, "Töne & Entspannung", "Audio & Soundscapes")
                            "data" -> tr(language, "Daten, Sicherung & Archiv", "Data, Backup & Archive")
                            "about" -> tr(language, "Support & Über die App", "Support & About")
                            else -> tr(language, "Einstellungen", "Settings")
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
                ) {
                    if (currentSubpage == null) {
                        // MAIN SETTINGS CATEGORY LIST (Android System Settings Style)
                        item {
                            val settingsCategories = listOf(
                                Triple("profile", tr(language, "Profil & Konto", "Profile & Account"), if (userName.isNotBlank()) userName else tr(language, "Profilbild & Nutzername bearbeiten", "Edit profile picture & name")) to Pair(Icons.Default.Person, Color(0xFF22C55E)),
                                Triple("appearance", tr(language, "Erscheinungsbild & Sprache", "Appearance & Language"), tr(language, "Sprache, Theme, Widget-Transparenz & Haptik", "Language, Theme, Widget Opacity & Haptics")) to Pair(Icons.Default.Palette, HabitOrange),
                                Triple("notifications", tr(language, "Push-Benachrichtigungen", "Push Notifications"), tr(language, "Erinnerungen, Smart Insights & Rückblicke", "Reminders, Smart Insights & Reviews")) to Pair(Icons.Default.NotificationsActive, Color(0xFF10B981)),
                                Triple("audio", tr(language, "Töne & Entspannung", "Audio & Soundscapes"), tr(language, "${importedAudios.size} Soundscapes & Fokus-Audio", "${importedAudios.size} soundscapes & focus audio")) to Pair(Icons.Default.LibraryMusic, Color(0xFF00B4D8)),
                                Triple("data", tr(language, "Daten, Sicherung & Archiv", "Data, Backup & Archive"), tr(language, "Sicherung, Wiederherstellung & Archivierte Gewohnheiten", "Backup, Restore & Archived habits")) to Pair(Icons.Default.Backup, ErrorRed)
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AppCard),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column {
                                    settingsCategories.forEachIndexed { index, (catInfo, iconPair) ->
                                        val (key, title, subtitle) = catInfo
                                        val (icon, tintColor) = iconPair
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedSubpage = key }
                                                .padding(horizontal = 16.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(tintColor.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = title,
                                                        tint = tintColor,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Open",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        if (index < settingsCategories.size - 1) {
                                            HorizontalDivider(
                                                color = AppBorder,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Card 6: Support & Feedback
                        item {
                            SupportFeedbackCard(
                                language = language,
                                context = context,
                                uriHandler = uriHandler,
                                accentColor = accentColor
                            )
                        }
                    } else when (currentSubpage) {
                        "profile" -> {
                            profileSettingsSection(
                                language = language,
                                userName = userName,
                                profileImageUri = profileImageUri,
                                viewModel = viewModel,
                                photoPickerLauncher = profileImagePickerLauncherSettings,
                                onNameChange = { newName -> viewModel.updateUserName(newName) }
                            )
                        }
                        "appearance" -> {
                            appearanceSettingsSection(
                                language = language,
                                isDark = darkModeEnabled,
                                isVibrationEnabled = vibrationEnabled,
                                isInfoCardsEnabled = infoCardsEnabled,
                                widgetOpacity = widgetOpacity,
                                accentColorName = accentColorName,
                                viewModel = viewModel,
                                context = context
                            )
                        }
                        "audio" -> {
                            audioSettingsSection(
                                language = language,
                                importedAudios = importedAudios,
                                selectedAudioFile = selectedAudioFile,
                                onShowAudioDialog = { showAudioDialog = true },
                                onImportAudioClick = {
                                    try {
                                        audioPickerLauncherSettings.launch("audio/*")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }
                        "notifications" -> {
                            notificationsSettingsSection(
                                language = language,
                                reviewNotificationsEnabled = reviewNotificationsEnabled,
                                insightNotificationsEnabled = insightNotificationsEnabled,
                                monthlyReviewEnabled = monthlyReviewEnabled,
                                yearlyReviewEnabled = yearlyReviewEnabled,
                                viewModel = viewModel,
                                permissionLauncher = permissionLauncher,
                                hasNotificationPermission = hasNotificationPermission
                            )
                        }
                        "data" -> {
                            dataBackupSettingsSection(
                                language = language,
                                autoBackupFolderUri = backupFolderUri,
                                lastBackupTime = BackupManager.getLastBackupTime(context),
                                onShowArchivedList = { showArchivedList = true },
                                onPickFolder = {
                                    try {
                                        folderLauncher.launch(null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onManualBackup = { viewModel.triggerManualBackup() },
                                onPickRestoreFile = {
                                    try {
                                        fileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onPickCsvFile = {
                                    try {
                                        csvPickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onShowWipeDialog = { showWipeConfirm = true },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        if (showWipeConfirm) {
            AlertDialog(
                onDismissRequest = { showWipeConfirm = false },
                title = { Text(text = tr(language, "ALLE DATEN LÖSCHEN?", "წაშალოთ ყველა მონაცემი?", "确定清空所有数据？", "WIPE ALL DATA?")) },
                text = { Text(text = tr(language, "Möchtest du wirklich alle angelegten Gewohnheiten und Log-Einträge restlos entfernen? Das kann nicht rückgängig gemacht werden!", "Are you sure you want to clear all habits and historic progress permanently? This action cannot be undone!")) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.wipeAllData()
                            showWipeConfirm = false
                            Toast.makeText(
                                context,
                                tr(language, "Alle Daten gelöscht", "ყველა მონაცემი წაშლილია", "所有数据已清空", "All data wiped"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text(tr(language, "Ja, Löschen", "Yes, Wipe"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirm = false }) {
                        Text(tr(language, "Abbrechen", "Cancel"))
                    }
                }
            )
        }

        if (showAudioDialog) {
            AudioSoundscapeDialog(
                language = language,
                importedAudios = importedAudios,
                selectedAudioFile = selectedAudioFile,
                onSelectAudio = { file ->
                    selectedAudioFile = file
                    AudioSoundscapeManager.setLastSelectedAudio(context, file?.name ?: "")
                    showAudioDialog = false
                },
                onImportAudioClick = {
                    try {
                        audioPickerLauncherSettings.launch("audio/*")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onDeleteAudioClick = { file ->
                    try {
                        if (file.exists()) {
                            file.delete()
                            refreshAudios()
                            if (selectedAudioFile?.absolutePath == file.absolutePath) {
                                selectedAudioFile = null
                                AudioSoundscapeManager.setLastSelectedAudio(context, "")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onDismiss = { showAudioDialog = false }
            )
        }
    }
}
