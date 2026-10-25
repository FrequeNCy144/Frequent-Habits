package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyBackupWorker
import com.example.HabitWidgetProvider
import com.example.data.BackupManager
import com.example.data.CsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

fun HabitsViewModel.saveBackupFolderUri(uri: String) {
    _backupFolderUri.value = uri
    sharedPrefs.edit()
        .putString("backup_folder_uri", uri)
        .apply()
    DailyBackupWorker.scheduleDailyBackup(getApplication())
    val lang = language.value
    _syncStatus.value = when (lang) {
        "de" -> "Sicherungsordner gespeichert! Täglicher Export ist aktiv."
        "ka" -> "სარეზერვო საქაღალდე შენახულია! ყოველდღიური ექსპორტი აქტიურია."
        "zh" -> "备份文件夹已保存！每日自动导出已启用。"
        "fr" -> "Dossier de sauvegarde enregistré ! L'export quotidien est actif."
        else -> "Backup folder saved! Daily export is active."
    }
}

fun HabitsViewModel.triggerManualBackup() {
    viewModelScope.launch {
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Sicherung wird erstellt..."
            "ka" -> "სარეზერვო ასლი იქმნება..."
            "zh" -> "正在创建备份..."
            "fr" -> "Création de la sauvegarde en cours..."
            else -> "Creating backup..."
        }
        val uriStr = _backupFolderUri.value
        if (uriStr.isEmpty()) {
            _syncStatus.value = when (lang) {
                "de" -> "Fehler: Kein Ordner ausgewählt!"
                "ka" -> "შეცდომა: საქაღალდე არ არის არჩეული!"
                "zh" -> "错误：未选择文件夹！"
                "fr" -> "Erreur : Aucun dossier sélectionné !"
                else -> "Error: No folder selected!"
            }
            return@launch
        }
        val success = BackupManager.performBackup(getApplication(), uriStr)
        _syncStatus.value = if (success) {
            when (lang) {
                "de" -> "Sicherung erfolgreich erstellt! 🎉"
                "ka" -> "სარეზერვო ასლი წარმატებით შეიქმნა! 🎉"
                "zh" -> "备份创建成功！🎉"
                "fr" -> "Sauvegarde créée avec succès ! 🎉"
                else -> "Backup created successfully! 🎉"
            }
        } else {
            when (lang) {
                "de" -> "Fehler beim Erstellen der Sicherung!"
                "ka" -> "შეცდომა სარეზერვო ასლის შექმნისას!"
                "zh" -> "创建备份时出错！"
                "fr" -> "Erreur lors de la création de la sauvegarde !"
                else -> "Error creating backup!"
            }
        }
    }
}

fun HabitsViewModel.triggerManualRestoreFromUri(uri: Uri) {
    viewModelScope.launch {
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Daten werden wiederhergestellt..."
            "ka" -> "მონაცემები აღდგება..."
            "zh" -> "正在恢复数据..."
            "fr" -> "Restauration des données en cours..."
            else -> "Restoring data..."
        }
        val context = getApplication<android.app.Application>()
        val (success, errorMsg) = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BackupManager.restoreDatabaseFromInputStreamDetailed(context, stream)
                } ?: Pair(false, "Datei konnte nicht geöffnet werden")
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, e.localizedMessage ?: "Unbekannter Fehler")
            }
        }

        if (success) {
            reloadSettingsFromPrefs()
            _syncStatus.value = when (lang) {
                "de" -> "Sicherung erfolgreich wiederhergestellt! 🎉"
                "ka" -> "სარეზერვო ასლი წარმატებით აღდგა! 🎉"
                "zh" -> "备份已成功恢复！🎉"
                "fr" -> "Sauvegarde restaurée avec succès ! 🎉"
                else -> "Backup successfully restored! 🎉"
            }
        } else {
            val detail = if (errorMsg.isNotEmpty()) " ($errorMsg)" else ""
            _syncStatus.value = when (lang) {
                "de" -> "Fehler bei der Wiederherstellung!$detail"
                "ka" -> "შეცდომა აღდგენისას!$detail"
                "zh" -> "恢复失败！$detail"
                "fr" -> "Échec de la restauration !$detail"
                else -> "Restore failed!$detail"
            }
        }
        HabitWidgetProvider.triggerUpdate(context)
    }
}

fun HabitsViewModel.exportBackupToUri(uri: Uri) {
    viewModelScope.launch {
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Backup-Datei wird exportiert..."
            "ka" -> "სარეზერვო ფაილი ექსპორტირდება..."
            "zh" -> "正在导出备份文件..."
            "fr" -> "Exportation du fichier de sauvegarde..."
            else -> "Exporting backup file..."
        }
        val context = getApplication<android.app.Application>()
        val success = BackupManager.exportBackupToUri(context, uri)
        _syncStatus.value = if (success) {
            when (lang) {
                "de" -> "Backup-Datei erfolgreich gespeichert! 🎉"
                "ka" -> "სარეზერვო ფაილი წარმატებით შეინახა! 🎉"
                "zh" -> "备份文件保存成功！🎉"
                "fr" -> "Fichier de sauvegarde enregistré avec succès ! 🎉"
                else -> "Backup file saved successfully! 🎉"
            }
        } else {
            when (lang) {
                "de" -> "Fehler beim Exportieren der Sicherung!"
                "ka" -> "შეცდომა ექსპორტისას!"
                "zh" -> "导出备份时出错！"
                "fr" -> "Erreur lors de l'exportation !"
                else -> "Error exporting backup!"
            }
        }
    }
}

fun HabitsViewModel.shareBackupFile(context: Context) {
    viewModelScope.launch {
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Sicherung wird für das Teilen vorbereitet..."
            "ka" -> "სარეზერვო ასლი მზადდება გასაზიარებლად..."
            "zh" -> "正在准备分享备份..."
            "fr" -> "Préparation de la sauvegarde pour le partage..."
            else -> "Preparing backup to share..."
        }
        try {
            val file = BackupManager.createBackupZipFile(context)
            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Backup teilen / versenden").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            _syncStatus.value = when (lang) {
                "de" -> "Backup bereit zum Teilen! 🚀"
                "ka" -> "სარეზერვო ასლი მზადაა! 🚀"
                "zh" -> "备份已准备好分享！🚀"
                "fr" -> "Sauvegarde prête à être partagée ! 🚀"
                else -> "Backup ready to share! 🚀"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatus.value = when (lang) {
                "de" -> "Fehler beim Teilen: ${e.localizedMessage}"
                else -> "Error sharing backup: ${e.localizedMessage}"
            }
        }
    }
}

fun HabitsViewModel.triggerManualRestoreFromStream(inputStream: InputStream) {
    viewModelScope.launch {
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Daten werden wiederhergestellt..."
            "ka" -> "მონაცემები აღდგება..."
            "zh" -> "正在恢复数据..."
            "fr" -> "Restauration des données en cours..."
            else -> "Restoring data..."
        }
        val success = BackupManager.restoreDatabaseFromInputStream(getApplication(), inputStream)
        if (success) {
            reloadSettingsFromPrefs()
        }
        _syncStatus.value = if (success) {
            when (lang) {
                "de" -> "Sicherung erfolgreich wiederhergestellt! 🎉"
                "ka" -> "სარეზერვო ასლი წარმატებით აღდგა! 🎉"
                "zh" -> "备份已成功恢复！🎉"
                "fr" -> "Sauvegarde restaurée avec succès ! 🎉"
                else -> "Backup successfully restored! 🎉"
            }
        } else {
            when (lang) {
                "de" -> "Fehler bei der Wiederherstellung! Ungültige Datei."
                "ka" -> "შეცდომა აღდგენისას! არასწორი ფაილი."
                "zh" -> "恢复失败！无效的文件。"
                "fr" -> "Échec de la restauration ! Fichier invalide."
                else -> "Restore failed! Invalid file."
            }
        }
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}

fun HabitsViewModel.triggerManualRestore(jsonString: String) {
    viewModelScope.launch {
        val lang = language.value
        _syncStatus.value = when (lang) {
            "de" -> "Daten werden wiederhergestellt..."
            "ka" -> "მონაცემები აღდგება..."
            "zh" -> "正在恢复数据..."
            "fr" -> "Restauration des données en cours..."
            else -> "Restoring data..."
        }
        val success = BackupManager.restoreDatabaseFromJson(getApplication(), jsonString)
        if (success) {
            reloadSettingsFromPrefs()
        }
        _syncStatus.value = if (success) {
            when (lang) {
                "de" -> "Sicherung erfolgreich wiederhergestellt! 🎉"
                "ka" -> "სარეზერვო ასლი წარმატებით აღდგა! 🎉"
                "zh" -> "备份已成功恢复！🎉"
                "fr" -> "Sauvegarde restaurée avec succès ! 🎉"
                else -> "Backup successfully restored! 🎉"
            }
        } else {
            when (lang) {
                "de" -> "Fehler bei der Wiederherstellung! Ungültige Datei."
                "ka" -> "შეცდომა აღდგენისას! არასწორი ფაილი."
                "zh" -> "恢复失败！无效的文件。"
                "fr" -> "Erreur lors de la restauration ! Fichier non valide."
                else -> "Error during restore! Invalid file."
            }
        }
    }
}

fun HabitsViewModel.reloadSettingsFromPrefs() {
    val uName = sharedPrefs.getString("user_name", "") ?: ""
    _userName.value = if (uName == "Inlitx") "" else uName

    val pUri = sharedPrefs.getString("profile_image_uri", "") ?: ""
    _profileImageUri.value = when {
        avatarFile.exists() && avatarFile.length() > 0 -> Uri.fromFile(avatarFile).toString()
        pUri.startsWith("file://") -> {
            val f = File(pUri.removePrefix("file://"))
            if (f.exists() && f.length() > 0) pUri else ""
        }
        else -> pUri
    }
    _smartInsightDismissedDate.value = sharedPrefs.getString("smart_insight_dismissed_date", "") ?: ""
    _language.value = sharedPrefs.getString("language", "en") ?: "en"
    _accentColorName.value = sharedPrefs.getString("accent_color_name", "PURPLE") ?: "PURPLE"
    _themeMode.value = sharedPrefs.getString("theme_mode", "DARK") ?: "DARK"
    _darkModeEnabled.value = sharedPrefs.getBoolean("dark_mode_enabled", true)
    _vibrationEnabled.value = sharedPrefs.getBoolean("vibration_enabled", true)
    _infoCardsEnabled.value = sharedPrefs.getBoolean("info_cards_enabled", true)
    _coloredFolderCardsEnabled.value = sharedPrefs.getBoolean("colored_folder_cards_enabled", true)
    _reviewNotificationsEnabled.value = sharedPrefs.getBoolean("notifications_enabled", true)
    _dismissedReviews.value = sharedPrefs.getStringSet("dismissed_reviews", emptySet()) ?: emptySet()
    _insightNotificationsEnabled.value = sharedPrefs.getBoolean("insight_notifications_enabled", true)
    _smartInsightsInAppEnabled.value = sharedPrefs.getBoolean("smart_insights_in_app_enabled", true)
    _monthlyReviewEnabled.value = sharedPrefs.getBoolean("monthly_review_enabled", true)
    _yearlyReviewEnabled.value = sharedPrefs.getBoolean("yearly_review_enabled", true)
    _hasOnboarded.value = sharedPrefs.getBoolean("has_onboarded", false)
    _backupFolderUri.value = sharedPrefs.getString("backup_folder_uri", "") ?: ""
    _notificationsEnabled.value = sharedPrefs.getBoolean("reminder_enabled", false)
    _notificationsHour.value = sharedPrefs.getInt("reminder_hour", 18)
    _notificationsMinute.value = sharedPrefs.getInt("reminder_minute", 0)
    _isSaskiaUnlocked.value = sharedPrefs.getBoolean("is_saskia_unlocked", false)
    _allFolders.value = com.example.data.FolderStorageHelper.loadFolders(getApplication())
}

fun HabitsViewModel.clearSyncStatus() {
    _syncStatus.value = null
}

fun HabitsViewModel.analyzeCsvImport(uri: Uri) {
    viewModelScope.launch {
        _isAnalyzingCsv.value = true
        val preview = CsvImporter.analyzeFile(getApplication(), uri)
        _csvPreviewState.value = preview
        _isAnalyzingCsv.value = false
    }
}

fun HabitsViewModel.confirmCsvImport(replaceExisting: Boolean) {
    val preview = _csvPreviewState.value ?: return
    viewModelScope.launch {
        _isAnalyzingCsv.value = true
        val success = CsvImporter.importDataToDatabase(getApplication(), preview, replaceExisting)
        _isAnalyzingCsv.value = false
        _csvPreviewState.value = null
        val lang = language.value
        _syncStatus.value = if (success) {
            when (lang) {
                "de" -> "CSV-Import erfolgreich! ${preview.habits.size} Gewohnheiten importiert. 🎉"
                "ka" -> "CSV იმპორტი წარმატებულია! ${preview.habits.size} ჩვევა იმპორტირებულია. 🎉"
                "zh" -> "CSV 导入成功！已导入 ${preview.habits.size} 个习惯。🎉"
                else -> "CSV Import successful! ${preview.habits.size} habits imported. 🎉"
            }
        } else {
            when (lang) {
                "de" -> "Fehler beim CSV-Import!"
                "ka" -> "შეცდომა CSV იმპორტისას!"
                "zh" -> "CSV 导入失败！"
                else -> "Error during CSV import!"
            }
        }
    }
}

fun HabitsViewModel.dismissCsvPreview() {
    _csvPreviewState.value = null
}

fun HabitsViewModel.wipeAllData() {
    viewModelScope.launch {
        repository.clearAllData()
        _allFolders.value = emptyList()
        com.example.data.FolderStorageHelper.saveFolders(getApplication(), emptyList())
        HabitWidgetProvider.triggerUpdate(getApplication())
    }
}
