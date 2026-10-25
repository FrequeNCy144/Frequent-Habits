package com.example.ui.screens.today

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.example.data.HabitFolder
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.components.AppTextField
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.screens.createhabit.CreateHabitHeaderOverlay
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun TodayFolderDialog(
    folder: HabitFolder?,
    language: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var folderName by remember { mutableStateOf(folder?.name ?: "") }
    var folderNameError by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(folder?.color ?: "purple") }
    var selectedIcon by remember { mutableStateOf(folder?.icon ?: "folder") }
    var iconSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("all") }

    val activeColor = remember(selectedColor) { HabitIconMapping.getColor(selectedColor) }
    val scrollState = rememberScrollState()

    val filteredIcons = remember(iconSearchQuery) {
        val q = iconSearchQuery.trim().lowercase()
        if (q.isBlank()) {
            HabitIconMapping.iconList
        } else {
            HabitIconMapping.iconList.filter { (key, _) ->
                key.lowercase().contains(q) || HabitIconMapping.getIconLabel(key, language).lowercase().contains(q)
            }
        }
    }

    val displayIcons = remember(selectedCategoryId, filteredIcons) {
        if (selectedCategoryId == "all") {
            filteredIcons
        } else {
            val cat = HabitIconMapping.categories.find { it.id == selectedCategoryId }
            val keys = cat?.keys ?: emptyList()
            filteredIcons.filter { keys.contains(it.first) }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                return available
            }
        }
    }

    val performSave = {
        if (folderName.isNotBlank()) {
            onSave(folderName.trim(), selectedIcon, selectedColor)
        } else {
            folderNameError = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 88.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Sub-Header & Quick-Save Chip (matching CreateHabitScreen style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (folder != null) {
                            tr(language, "Ordner anpassen & verwalten", "საქაღალდის მართვა", "管理文件夹设置", "Manage folder settings")
                        } else {
                            tr(language, "Neuen Bereich für Gewohnheiten erstellen", "ახალი საქაღალდის შექმნა", "创建新的习惯分组", "Create new area for habits")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (folderName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = activeColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable { performSave() }
                                .testTag("quick_save_folder_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = tr(language, "Sofort fertig", "შენახვა", "直接完成", "Quick Save"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = AppBorder, thickness = 1.dp)

                // 1. CARD: Basis-Informationen (identical structure to HabitBasicInfoCard)
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (folderNameError) FailedRed else AppBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = activeColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = tr(language, "Basis-Informationen", "ძირითადი ინფორმაცია", "基本信息", "Basic Information"),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (folderNameError) FailedRed else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Folder Name field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tr(language, "Name des Ordners", "საქაღალდის სახელი", "文件夹名称", "Folder Name"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (folderNameError) FailedRed else TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            AppTextField(
                                value = folderName,
                                onValueChange = {
                                    folderName = it
                                    if (it.isNotBlank()) folderNameError = false
                                },
                                placeholderText = tr(
                                    language,
                                    "z.B. Morgenroutine, Gesundheit, Finanzen, Sport...",
                                    "მაგ. სპორტი, ჯანმრთელობა",
                                    "例如: 晨间日常、健康、运动...",
                                    "e.g. Morning Routine, Health, Sports..."
                                ),
                                isError = folderNameError,
                                accentColor = activeColor,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = if (folderNameError) FailedRed else if (folderName.isNotBlank()) activeColor else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                supportingText = {
                                    if (folderNameError) {
                                        Text(
                                            text = tr(language, "Bitte gib einen Ordnernamen ein", "გთხოვთ შეიყვანოთ სახელი", "请输入文件夹名称", "Please enter a folder name"),
                                            color = FailedRed,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                testTag = "input_folder_name"
                            )
                        }
                    }
                }

                // 2. CARD: Aussehen anpassen (identical structure to HabitAppearanceCard)
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HabitOrange.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_palette),
                                        contentDescription = null,
                                        tint = HabitOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = tr(language, "Aussehen anpassen", "გარეგნობის მორგება", "定制视觉外观", "Customize Visuals"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Icon selector header & search
                        Text(
                            text = tr(language, "Icon auswählen", "ხატულას არჩევა", "选择图标", "Select Icon"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        AppTextField(
                            value = iconSearchQuery,
                            onValueChange = { iconSearchQuery = it },
                            placeholderText = tr(language, "Icon durchsuchen...", "ძიება...", "搜索图标...", "Search icon..."),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (iconSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { iconSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            accentColor = activeColor,
                            testTag = "input_folder_icon_search"
                        )

                        // Category Chips for Icons
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(HabitIconMapping.categories) { cat ->
                                val isCatSelected = selectedCategoryId == cat.id
                                FilterChip(
                                    selected = isCatSelected,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = {
                                        Text(
                                            text = cat.getLocalizedName(language),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCatSelected) Color.White else TextSecondary
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = activeColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = ProgressTrack,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isCatSelected,
                                        selectedBorderColor = activeColor,
                                        borderColor = AppBorder,
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.dp
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Icon Grid Box with nested scroll
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(ProgressTrack, RoundedCornerShape(16.dp))
                                .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(nestedScrollConnection)
                            ) {
                                items(displayIcons) { iconPair ->
                                    val key = iconPair.first
                                    val isSelected = selectedIcon == key
                                    var showTooltip by remember { mutableStateOf(false) }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (isSelected) activeColor.copy(alpha = 0.2f) else AppCard,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) activeColor else AppBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .pointerInput(key) {
                                                detectTapGestures(
                                                    onTap = { selectedIcon = key },
                                                    onLongPress = { showTooltip = true }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = HabitIconMapping.getIcon(key),
                                            contentDescription = key,
                                            tint = if (isSelected) activeColor else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        if (showTooltip) {
                                            val density = LocalDensity.current
                                            val offsetInPx = with(density) { -52.dp.roundToPx() }
                                            Popup(
                                                alignment = Alignment.TopCenter,
                                                offset = IntOffset(0, offsetInPx),
                                                onDismissRequest = { showTooltip = false }
                                            ) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = AppCard),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .border(1.dp, AppBorder, RoundedCornerShape(8.dp))
                                                        .widthIn(max = 200.dp)
                                                ) {
                                                    Text(
                                                        text = HabitIconMapping.getIconLabel(key, language),
                                                        color = TextPrimary,
                                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = AppBorder, thickness = 1.dp)

                        // Color Selection
                        Text(
                            text = tr(language, "Farbe auswählen", "ფერის არჩევა", "选择颜色", "Select Color"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        ColorPaletteSelector(
                            selectedColorKey = selectedColor,
                            onColorSelected = { selectedColor = it },
                            language = language
                        )
                    }
                }

                // 3. CARD: Vorschau (Live Preview Card)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = activeColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check_circle),
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = tr(language, "Vorschau", "გადახედვა", "预览", "Preview"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = activeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(activeColor.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HabitIconMapping.getIcon(selectedIcon),
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (folderName.isNotBlank()) folderName else tr(language, "Neuer Ordner", "ახალი საქაღალდე", "新文件夹", "New Folder"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(language, "Gruppiert deine Gewohnheiten übersichtlich", "აჯგუფებს ჩვევებს", "习惯分组卡片", "Groups your habits"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. CARD: Gefahrzone (Delete Action if editing existing folder)
                if (folder != null && onDelete != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCard),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FailedRed.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tr(language, "Gefahrzone", "საფრთხის ზონა", "危险区域", "Danger Zone"),
                                style = MaterialTheme.typography.titleMedium,
                                color = FailedRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Der Ordner wird gelöscht. Enthaltene Gewohnheiten bleiben vollständig erhalten und werden nicht gelöscht.",
                                    "საქაღალდე წაიშლება. მასში არსებული ჩვევები შენარჩუნდება.",
                                    "删除文件夹不会删除其中的习惯，习惯将移出文件夹。",
                                    "Deleting the folder will keep all its habits intact."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FailedRed),
                                border = BorderStroke(1.dp, FailedRed.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_delete_folder_sheet")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = FailedRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(language, "Ordner löschen", "საქაღალდის წაშლა", "删除文件夹", "Delete Folder"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Bottom Bar (Abbrechen / Speichern - matching CreateHabitScreen style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_cancel_folder"),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text(
                            text = tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Button(
                        onClick = { performSave() },
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("btn_save_folder"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (folder == null) {
                                tr(language, "Ordner erstellen", "შექმნა", "创建文件夹", "Create Folder")
                            } else {
                                tr(language, "Änderungen speichern", "შენახვა", "保存修改", "Save Changes")
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // Floating Header Overlay with vertical gradient fade-out (using CreateHabitHeaderOverlay)
            CreateHabitHeaderOverlay(
                isEditing = folder != null,
                language = language,
                onDismiss = onDismiss,
                accentColor = activeColor,
                onSaveClick = { performSave() },
                customTitle = if (folder != null) {
                    tr(language, "Ordner bearbeiten", "საქაღალდის რედაქტირება", "编辑文件夹", "Edit Folder")
                } else {
                    tr(language, "Neuer Ordner", "ახალი საქაღალდე", "新建文件夹", "New Folder")
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
