package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.ClearFocusOnKeyboardDismiss
import com.example.ui.theme.*
import com.example.tr
import java.io.File

object AudioSoundscapeManager {
    private const val PREFS_NAME = "audio_soundscape_prefs"
    private const val KEY_LAST_SELECTED_AUDIO = "last_selected_audio_filename"
    private const val KEY_RECENT_AUDIOS = "recent_audio_filenames"
    private const val PREFIX_CATEGORY = "sound_category_"

    val CATEGORIES = listOf(
        "Alle",
        "Natur & Wetter",
        "Fokus & Noise",
        "Ambiente & Ruhe",
        "Eigene Sounds"
    )

    fun getLastSelectedAudio(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SELECTED_AUDIO, "") ?: ""
    }

    fun getRecentAudios(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT_AUDIOS, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").filter { it.isNotEmpty() }
    }

    fun setLastSelectedAudio(context: Context, filename: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SELECTED_AUDIO, filename ?: "").apply()
        if (!filename.isNullOrEmpty()) {
            val recents = getRecentAudios(context).toMutableList()
            recents.remove(filename)
            recents.add(0, filename)
            val trimmed = recents.take(3)
            prefs.edit().putString(KEY_RECENT_AUDIOS, trimmed.joinToString(";")).apply()
        }
    }

    fun getSoundCategory(context: Context, filename: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREFIX_CATEGORY + filename, null)
        if (!stored.isNullOrEmpty()) return stored

        val lower = filename.lowercase()
        return when {
            lower.contains("rain") || lower.contains("regen") || lower.contains("forest") || lower.contains("wald") ||
            lower.contains("ocean") || lower.contains("meer") || lower.contains("wind") || lower.contains("stream") || lower.contains("fluss") ||
            lower.contains("water") || lower.contains("wasser") || lower.contains("storm") -> "Natur & Wetter"

            lower.contains("binaural") || lower.contains("focus") || lower.contains("fokus") || lower.contains("white") ||
            lower.contains("noise") || lower.contains("alpha") || lower.contains("wave") || lower.contains("brown") || lower.contains("pink") -> "Fokus & Noise"

            lower.contains("meditation") || lower.contains("zen") || lower.contains("piano") || lower.contains("ambient") ||
            lower.contains("ruhe") || lower.contains("calm") || lower.contains("sleep") || lower.contains("relax") -> "Ambiente & Ruhe"

            else -> "Eigene Sounds"
        }
    }

    fun setSoundCategory(context: Context, filename: String, category: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFIX_CATEGORY + filename, category).apply()
    }

    fun renameAudioFile(context: Context, file: File, newNameWithoutExtension: String): File? {
        val trimmed = newNameWithoutExtension.trim()
        if (trimmed.isEmpty()) return null
        val ext = file.extension
        val newFileName = if (ext.isNotEmpty()) "$trimmed.$ext" else trimmed
        val newFile = File(file.parentFile, newFileName)
        if (newFile.exists() && newFile.absolutePath != file.absolutePath) return null

        val success = file.renameTo(newFile)
        if (success) {
            val lastSelected = getLastSelectedAudio(context)
            if (lastSelected == file.name) {
                setLastSelectedAudio(context, newFile.name)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val oldCat = prefs.getString(PREFIX_CATEGORY + file.name, null)
            if (oldCat != null) {
                prefs.edit().remove(PREFIX_CATEGORY + file.name).putString(PREFIX_CATEGORY + newFile.name, oldCat).apply()
            }
            return newFile
        }
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSoundscapeDialog(
    language: String,
    importedAudios: List<File>,
    selectedAudioFile: File?,
    onSelectAudio: (File?) -> Unit,
    onImportAudioClick: () -> Unit,
    onDeleteAudioClick: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    ClearFocusOnKeyboardDismiss()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (isSearchFocused) {
        BackHandler {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filteredAudios = remember(importedAudios, searchQuery) {
        importedAudios.filter { file ->
            searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onDismiss()
        },
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(language, "Fokus-Audio Bibliothek", "აუდიო ბგერების ფოკუსირება", "专注背景白噪音", "Focus Audio Soundscapes"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(language, "Sound auswählen & verwalten", "აირჩიეთ და მართეთ ხმები", "选择与管理声音", "Select & manage sounds"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ClearFocusOnKeyboardDismiss()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Field
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholderText = tr(language, "Sound suchen...", "ხმის ძებნა...", "搜索白噪音...", "Search sound..."),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(force = true); keyboardController?.hide() }),
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    singleLine = true,
                    testTag = "sound_search_query"
                )

                // Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBg, RoundedCornerShape(12.dp))
                        .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Eigene Sounds laufen während des Timers im Loop.", "მორგებული ხმები მეორდება ტაიმერის დროს.", "自定义白噪音将在计时期间循环播放。", "Custom audio will loop continuously during focus timers."),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Audio list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mute / None option
                    item {
                        Surface(
                            onClick = {
                                onSelectAudio(null)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedAudioFile == null) PrimaryViolet.copy(alpha = 0.15f) else AppBg,
                            border = BorderStroke(1.dp, if (selectedAudioFile == null) PrimaryViolet else AppBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ProgressTrack, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.VolumeOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tr(language, "Kein Sound (Stumm)", "ხმა არ არის (დადუმებული)", "静音（已静音）", "No Sound (Muted)"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = if (selectedAudioFile == null) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = tr(language, "Fokus-Timer läuft lautlos", "ტაიმერი მუშაობს ჩუმად", "专注计时静音运行", "Focus timer runs silently"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = selectedAudioFile == null,
                                    onClick = { onSelectAudio(null) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet)
                                )
                            }
                        }
                    }

                    if (filteredAudios.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MusicOff, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                                Text(
                                    text = tr(language, "Noch keine eigenen Sounds importiert.", "მორგებული ხმები ჯერ არ არის იმპორტირებული.", "尚未导入任何自定义声音。", "No custom sounds imported yet."),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(filteredAudios, key = { it.absolutePath }) { file ->
                            val isSelected = selectedAudioFile?.absolutePath == file.absolutePath
                            val isPreviewing = previewFile?.absolutePath == file.absolutePath

                            Surface(
                                onClick = {
                                    onSelectAudio(file)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) PrimaryViolet.copy(alpha = 0.15f) else AppBg,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryViolet else AppBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Preview Play/Pause button
                                        IconButton(
                                            onClick = {
                                                if (isPreviewing) {
                                                    try {
                                                        previewPlayer?.stop()
                                                        previewPlayer?.release()
                                                        previewPlayer = null
                                                        previewFile = null
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    try {
                                                        previewPlayer?.stop()
                                                        previewPlayer?.release()
                                                        val mp = MediaPlayer().apply {
                                                            setDataSource(file.absolutePath)
                                                            isLooping = true
                                                            prepare()
                                                            start()
                                                        }
                                                        previewPlayer = mp
                                                        previewFile = file
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(if (isPreviewing) SuccessGreen else PrimaryViolet, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Preview",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = file.nameWithoutExtension,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Rename File Button
                                        IconButton(
                                            onClick = {
                                                fileToRename = file
                                                renameText = file.nameWithoutExtension
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename audio",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete File Button
                                        IconButton(
                                            onClick = {
                                                if (previewFile?.absolutePath == file.absolutePath) {
                                                    try {
                                                        previewPlayer?.stop()
                                                        previewPlayer?.release()
                                                        previewPlayer = null
                                                        previewFile = null
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                onDeleteAudioClick(file)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete audio",
                                                tint = ErrorRed.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectAudio(file) },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Import Button
                Button(
                    onClick = { onImportAudioClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Neues Audio importieren (.mp3, .wav, .m4a)", "ახალი აუდიოს იმპორტი (.mp3, .wav, .m4a)", "导入新音频 (.mp3, .wav, .m4a)", "Import new audio (.mp3, .wav, .m4a)"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        try {
                            previewPlayer?.stop()
                            previewPlayer?.release()
                            previewPlayer = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr(language, "Fertig", "შესრულებულია", "完成", "Done"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (fileToRename != null) {
        val targetFile = fileToRename!!
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            containerColor = AppCard,
            title = {
                Text(
                    text = tr(language, "Audio umbenennen", "აუდიოს გადარქმევა", "重命名音频", "Rename Audio"),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(language, "Gib einen neuen Namen für die Audiodatei ein:", "შეიყვანეთ აუდიო ფაილის ახალი სახელი:", "输入音频文件的新名称：", "Enter a new name for the audio file:"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    AppTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        placeholderText = tr(language, "Neuer Name", "ახალი სახელი", "新名称", "New Name"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newFile = AudioSoundscapeManager.renameAudioFile(context, targetFile, renameText)
                        if (newFile != null && selectedAudioFile?.absolutePath == targetFile.absolutePath) {
                            onSelectAudio(newFile)
                        }
                        fileToRename = null
                    }
                ) {
                    Text(
                        text = tr(language, "Speichern", "შენახვა", "保存", "Save"),
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text(
                        text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
