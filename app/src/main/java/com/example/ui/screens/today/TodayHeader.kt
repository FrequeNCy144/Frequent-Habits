package com.example.ui.screens.today

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.screens.profile.BoldPlusIcon
import com.example.ui.screens.profile.ZettelMitStiftIcon
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppCard
import com.example.ui.theme.TextPrimary
import com.frequent.habits.R

@Composable
fun TodayHeader(
    formattedDisplayDate: String,
    hasDailyNote: Boolean,
    accentColor: Color,
    language: String,
    onOpenDailyNote: () -> Unit,
    onTitleClick: () -> Unit,
    onAddHabit: () -> Unit,
    onAddFolder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isExpanded) {
        isExpanded = false
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "plus_rotation"
    )

    val crossIconColor by animateColorAsState(
        targetValue = if (isExpanded) Color(0xFFEF4444) else accentColor,
        animationSpec = tween(220),
        label = "cross_icon_color"
    )

    val centerTitleAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 1f,
        animationSpec = tween(180),
        label = "center_title_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .defaultMinSize(minHeight = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isExpanded) isExpanded = false
            }
    ) {
        // Top Left: Daily Note Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { onOpenDailyNote() }
                .align(Alignment.CenterStart)
                .testTag("btn_daily_note"),
            contentAlignment = Alignment.Center
        ) {
            ZettelMitStiftIcon(
                modifier = Modifier.size(22.dp),
                color = accentColor
            )
            if (hasDailyNote) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }

        // Center Title: Date Header (fades out when expanded so expanding pill has full room)
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
                .graphicsLayer { alpha = centerTitleAlpha }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onTitleClick()
                },
            text = formattedDisplayDate,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Top Right: Single expanding container with unified outer border pinned firmly to the right
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isExpanded) AppCard else accentColor.copy(alpha = 0.12f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isExpanded) accentColor.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.35f)
            ),
            shadowElevation = if (isExpanded) 6.dp else 0.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(44.dp)
                .animateContentSize(
                    alignment = Alignment.CenterEnd,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (isExpanded) {
                    // Option 1: Neuer Ordner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isExpanded = false
                                onAddFolder()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("menu_item_add_folder")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = tr(language, "Ordner", "საქაღალდე", "文件夹", "Folder", "Dossier"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Subtle Divider between Ordner and Gewohnheit
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(AppBorder)
                    )

                    // Option 2: Neue Gewohnheit
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isExpanded = false
                                onAddHabit()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("menu_item_add_habit")
                    ) {
                        BoldPlusIcon(
                            modifier = Modifier.size(13.dp),
                            color = accentColor
                        )
                        Text(
                            text = tr(language, "Gewohnheit", "ჩვევა", "新习惯", "Habit", "Habitude"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Subtle Divider before close cross
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(AppBorder)
                    )
                }

                // Morphing Action: The Plus Icon rotating into red Cross '✕' (NO separate border, fixed position!)
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isExpanded = !isExpanded }
                        .testTag("btn_add_habit"),
                    contentAlignment = Alignment.Center
                ) {
                    BoldPlusIcon(
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = rotationAngle },
                        color = crossIconColor
                    )
                }
            }
        }
    }
}
