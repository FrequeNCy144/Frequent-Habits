package com.example.ui

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.DailyBackupWorker
import com.example.HabitWidgetProvider
import com.example.data.BackupManager
import com.example.data.CsvImporter
import kotlinx.coroutines.launch
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
    _darkModeEnabled.value = sharedPrefs.getBoolean("dark_mode_enabled", true)
    _vibrationEnabled.value = sharedPrefs.getBoolean("vibration_enabled", true)
    _infoCardsEnabled.value = sharedPrefs.getBoolean("info_cards_enabled", true)
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
