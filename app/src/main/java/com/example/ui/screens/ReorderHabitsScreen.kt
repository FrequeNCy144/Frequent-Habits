package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.data.HabitFolder
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.saveNewHabitOrder
import com.example.ui.components.ModernBackButton
import com.example.ui.theme.*
import com.frequent.habits.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderHabitsScreen(
    viewModel: HabitsViewModel,
    language: String,
    onBack: () -> Unit,
    onEditHabit: ((Habit) -> Unit)? = null
) {
    val context = LocalContext.current
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()

    val initialActiveHabits = remember(allHabits) {
        allHabits.filter { !it.isArchived && !(it.isFinishable && it.isCompletedGoal) }.sortedBy { it.sortOrder }
    }

    val initialFolders = remember(allFolders) {
        allFolders.sortedBy { it.sortOrder }
    }

    // Local mutable state for reordering so user can cancel or save
    var localFolders by remember(initialFolders) { mutableStateOf(initialFolders) }
    var localHabits by remember(initialActiveHabits) { mutableStateOf(initialActiveHabits) }
    var hasChanges by remember { mutableStateOf(false) }

    val onSaveAndExit = {
        if (hasChanges) {
            viewModel.saveFoldersOrder(localFolders)
            viewModel.saveNewHabitOrder(localHabits.map { it.id })
            Toast.makeText(
                context,
                tr(language, "Reihenfolge gespeichert!", "თანმიმდევრობა შენახულია!", "排序已保存！", "Order saved!"),
                Toast.LENGTH_SHORT
            ).show()
        }
        onBack()
    }

    val onCancelAndExit = {
        onBack()
    }

    BackHandler {
        onCancelAndExit()
    }

    val looseHabits = remember(localHabits) {
        localHabits.filter { it.folderId == null }
    }

    val looseRoots = remember(looseHabits) {
        val roots = looseHabits.filter { it.stackedOnHabitId == null }
        val orphans = looseHabits.filter { child ->
            child.stackedOnHabitId != null && looseHabits.none { it.id == child.stackedOnHabitId }
        }
        roots + orphans
    }

    val listState = rememberLazyListState()

    // Helper reorder functions on local state
    fun moveFolderLocally(folderId: Int, up: Boolean) {
        val list = localFolders.toMutableList()
        val index = list.indexOfFirst { it.id == folderId }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in list.indices) {
            val item = list.removeAt(index)
            list.add(targetIndex, item)
            localFolders = list
            hasChanges = true
        }
    }

    fun moveNestedHabitLocally(folderId: Int, habitId: Int, up: Boolean) {
        val folderItems = localHabits.filter { it.folderId == folderId }.toMutableList()
        val index = folderItems.indexOfFirst { it.id == habitId }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in folderItems.indices) {
            val item = folderItems.removeAt(index)
            folderItems.add(targetIndex, item)

            // Reconstruct localHabits keeping relative orders
            val otherHabits = localHabits.filter { it.folderId != folderId }
            localHabits = otherHabits + folderItems
            hasChanges = true
        }
    }

    fun moveLooseHabitLocally(habitId: Int, up: Boolean) {
        val rootItems = looseRoots.toMutableList()
        val index = rootItems.indexOfFirst { it.id == habitId }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in rootItems.indices) {
            val item = rootItems.removeAt(index)
            rootItems.add(targetIndex, item)

            // Flatten loose roots and their children in order
            val newLooseList = mutableListOf<Habit>()
            for (root in rootItems) {
                newLooseList.add(root)
                val children = looseHabits.filter { it.stackedOnHabitId == root.id }
                newLooseList.addAll(children)
            }

            val folderHabits = localHabits.filter { it.folderId != null }
            localHabits = folderHabits + newLooseList
            hasChanges = true
        }
    }

    fun moveChildHabitLocally(childId: Int, parentId: Int, up: Boolean) {
        val children = localHabits.filter { it.stackedOnHabitId == parentId }.toMutableList()
        val index = children.indexOfFirst { it.id == childId }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in children.indices) {
            val item = children.removeAt(index)
            children.add(targetIndex, item)

            val otherHabits = localHabits.filter { it.stackedOnHabitId != parentId }
            val parentIndex = otherHabits.indexOfFirst { it.id == parentId }
            val result = otherHabits.toMutableList()
            if (parentIndex != -1) {
                result.addAll(parentIndex + 1, children)
            } else {
                result.addAll(children)
            }
            localHabits = result
            hasChanges = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr(language, "Reihenfolge anpassen", "თანმიმდევრობის შეცვლა", "调整排序", "Reorder Habits"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                        ModernBackButton(
                            onClick = onCancelAndExit,
                            testTag = "btn_cancel_reorder_top"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onSaveAndExit,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("btn_save_reorder_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Speichern", "შენახვა", "保存", "Save"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBg,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = AppBg
    ) { innerPadding ->
        if (localHabits.isEmpty() && localFolders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AppBorder))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = tr(language, "Keine Gewohnheiten vorhanden", "ჩვევები არ მოიძებნა", "暂无可调整的习惯", "No habits available"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr(
                                language,
                                "Erstelle zuerst Gewohnheiten, um deren Reihenfolge anzupassen.",
                                "ჯერ შექმენით ჩვევები, რათა შეცვალოთ მათი თანმიმდევრობა.",
                                "请先创建习惯，然后再调整排序。",
                                "Create habits first to customize their order."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
            ) {
                // Folders Section
                if (localFolders.isNotEmpty()) {
                    itemsIndexed(
                        items = localFolders,
                        key = { _, folder -> "folder_${folder.id}" }
                    ) { folderIndex, folder ->
                        val habitsInFolder = localHabits.filter { it.folderId == folder.id }
                        ReorderFolderCard(
                            folder = folder,
                            habits = habitsInFolder,
                            isFirst = folderIndex == 0,
                            isLast = folderIndex == localFolders.size - 1,
                            accentColor = accentColor,
                            language = language,
                            onMoveFolderUp = { moveFolderLocally(folder.id, up = true) },
                            onMoveFolderDown = { moveFolderLocally(folder.id, up = false) },
                            onMoveHabitUp = { habitId -> moveNestedHabitLocally(folder.id, habitId, up = true) },
                            onMoveHabitDown = { habitId -> moveNestedHabitLocally(folder.id, habitId, up = false) },
                            onEditHabit = onEditHabit
                        )
                    }
                }

                // Loose Habits Section
                if (looseRoots.isNotEmpty()) {
                    itemsIndexed(
                        items = looseRoots,
                        key = { _, habit -> "loose_root_${habit.id}" }
                    ) { index, rootHabit ->
                        val stackedChildren = looseHabits.filter { it.stackedOnHabitId == rootHabit.id }
                        val isFirst = index == 0
                        val isLast = index == looseRoots.size - 1

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AppCard),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AppBorder))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReorderHabitRow(
                                    habit = rootHabit,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    language = language,
                                    accentColor = accentColor,
                                    isNested = false,
                                    onMoveUp = { moveLooseHabitLocally(rootHabit.id, up = true) },
                                    onMoveDown = { moveLooseHabitLocally(rootHabit.id, up = false) },
                                    onEditHabit = onEditHabit
                                )

                                if (stackedChildren.isNotEmpty()) {
                                    stackedChildren.forEachIndexed { childIndex, childHabit ->
                                        val isChildFirst = childIndex == 0
                                        val isChildLast = childIndex == stackedChildren.size - 1
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(modifier = Modifier.width(20.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(28.dp)
                                                    .background(accentColor.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(modifier = Modifier.weight(1f)) {
                                                ReorderHabitRow(
                                                    habit = childHabit,
                                                    isFirst = isChildFirst,
                                                    isLast = isChildLast,
                                                    language = language,
                                                    accentColor = accentColor,
                                                    isNested = true,
                                                    onMoveUp = { moveChildHabitLocally(childHabit.id, rootHabit.id, up = true) },
                                                    onMoveDown = { moveChildHabitLocally(childHabit.id, rootHabit.id, up = false) },
                                                    onEditHabit = onEditHabit
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderFolderCard(
    folder: HabitFolder,
    habits: List<Habit>,
    isFirst: Boolean,
    isLast: Boolean,
    accentColor: Color,
    language: String,
    onMoveFolderUp: () -> Unit,
    onMoveFolderDown: () -> Unit,
    onMoveHabitUp: (Int) -> Unit,
    onMoveHabitDown: (Int) -> Unit,
    onEditHabit: ((Habit) -> Unit)?
) {
    val folderColor = remember(folder.color) { HabitIconMapping.getColor(folder.color) }
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(folderColor.copy(alpha = 0.35f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Folder Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(folderColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder),
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${habits.size} " + if (habits.size == 1) {
                            tr(language, "Gewohnheit", "ჩვევა", "习惯", "habit")
                        } else {
                            tr(language, "Gewohnheiten", "ჩვევები", "习惯", "habits")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Folder Move Buttons
                IconButton(
                    onClick = onMoveFolderUp,
                    enabled = !isFirst,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_move_folder_up_${folder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Folder Up",
                        tint = if (!isFirst) folderColor else TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onMoveFolderDown,
                    enabled = !isLast,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_move_folder_down_${folder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Folder Down",
                        tint = if (!isLast) folderColor else TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 1.dp)

                    if (habits.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(language, "Keine Gewohnheiten in diesem Ordner", "ამ საქაღალდეში ჩვევები არ არის", "此文件夹内无习惯", "No habits in this folder"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    } else {
                        habits.forEachIndexed { hIndex, habit ->
                            val isHabitFirst = hIndex == 0
                            val isHabitLast = hIndex == habits.size - 1
                            ReorderHabitRow(
                                habit = habit,
                                isFirst = isHabitFirst,
                                isLast = isHabitLast,
                                language = language,
                                accentColor = accentColor,
                                isNested = true,
                                onMoveUp = { onMoveHabitUp(habit.id) },
                                onMoveDown = { onMoveHabitDown(habit.id) },
                                onEditHabit = onEditHabit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderHabitRow(
    habit: Habit,
    isFirst: Boolean,
    isLast: Boolean,
    language: String,
    accentColor: Color,
    isNested: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditHabit: ((Habit) -> Unit)?
) {
    val habitColor = remember(habit.color) { HabitIconMapping.getColor(habit.color) }
    val iconRes = remember(habit.icon) { HabitIconMapping.getIconDrawableId(habit.icon) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isNested) ProgressTrack.copy(alpha = 0.6f) else ProgressTrack,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reorder_row_${habit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(habitColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = habitColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Habit Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditHabit?.invoke(habit) }
            ) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (habit.stackedOnHabitId != null) {
                        Text(
                            text = "🔗 " + tr(language, "Gestapelt", "დაჯაჭვული", "堆叠", "Stacked"),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }

                    val freqText = when (habit.frequency) {
                        "DAILY", "EVERYDAY" -> tr(language, "Täglich", "ყოველდღე", "每日", "Daily")
                        "SPECIFIC", "SPECIFIC_DAYS" -> tr(language, "Bestimmte Tage", "კონკრეტული დღეები", "指定日期", "Specific Days")
                        "TIMES_WEEKLY" -> tr(language, "Wöchentlich", "კვირაში", "每周", "Weekly")
                        "TIMES_MONTHLY" -> tr(language, "Monatlich", "თვეში", "每月", "Monthly")
                        "INTERVAL" -> {
                            val days = habit.specificDays.toIntOrNull() ?: 2
                            tr(language, "Alle $days Tage", "ყოველ $days დღეში", "每 $days 天", "Every $days days")
                        }
                        else -> habit.frequency
                    }
                    Text(
                        text = freqText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Move Up Button
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_move_up_${habit.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (!isFirst) TextPrimary else TextSecondary.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Move Down Button
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_move_down_${habit.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (!isLast) TextPrimary else TextSecondary.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
