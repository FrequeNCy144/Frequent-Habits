package com.example.ui.screens.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LocalDarkMode
import com.example.LocalHapticsEnabled
import com.example.LocalInfoCardsEnabled
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.LanguageDropdown
import com.example.ui.components.AppSegmentedButtonWithIcons
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.theme.*

fun LazyListScope.appearanceSettingsSection(
    language: String,
    isDark: Boolean,
    isVibrationEnabled: Boolean,
    isInfoCardsEnabled: Boolean,
    widgetOpacity: Float,
    accentColorName: String,
    viewModel: HabitsViewModel,
    context: Context
) {
                    // SUBPAGE 2: ERSCHEINUNGSBILD & SPRACHE
                    // Card 1: Sprache & Region
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
                                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Sprache & Region", "Language & Region"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                com.example.ui.LanguageDropdown(
                                    currentLanguage = language,
                                    onLanguageSelected = { viewModel.setLanguage(it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Card 2: App Theme
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
                                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Erscheinungsbild", "App Theme"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(language, "Wechsle zwischen hellem und dunklem Design", "Switch between light and dark theme"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                AppSegmentedButtonWithIcons(
                                    options = listOf(
                                        tr(language, "Dunkel", "Dark") to Icons.Default.DarkMode,
                                        tr(language, "Hell", "Light") to Icons.Default.LightMode
                                    ),
                                    selectedIndex = if (isDark) 0 else 1,
                                    onOptionSelected = { index -> viewModel.setDarkModeEnabled(index == 0) },
                                    testTagPrefix = "app_theme_toggle"
                                )
                            }
                        }
                    }

                    // Card 3: Akzentfarbe
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
                                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Akzentfarbe", "Accent Color"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(language, "Passe die Primärfarbe der App an dein Design an", "Customize the primary accent color of the app"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                com.example.ui.components.ColorPaletteSelector(
                                    selectedColorKey = accentColorName,
                                    onColorSelected = { viewModel.setAccentColorName(it) },
                                    language = language
                                )
                            }
                        }
                    }

                    // Card 3.5: Widget-Transparenz
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
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(language, "Widget-Transparenz", "Widget Opacity"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${(widgetOpacity * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PrimaryViolet,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(
                                        language,
                                        "Passe die Transparenz des Widget-Hintergrunds an (Hell & Dunkel)",
                                        "Adjust the opacity of the widget background (Light & Dark mode)"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Slider(
                                    value = widgetOpacity,
                                    onValueChange = { newOpacity ->
                                        viewModel.setWidgetOpacity(newOpacity)
                                    },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryViolet,
                                        activeTrackColor = PrimaryViolet,
                                        inactiveTrackColor = ProgressTrack
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Card 4: Infokarten & Tipps
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
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info Cards",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tr(language, "Infokarten & Hinweise", "Info Cards & Tips"),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tr(language, "Blendet Erklärungen und Info-Buttons ein", "Display explanatory cards and info icons"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isInfoCardsEnabled,
                                        onCheckedChange = { viewModel.setInfoCardsEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PrimaryViolet,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = ProgressTrack
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Card 5: Haptisches Feedback
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
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Vibration,
                                            contentDescription = "Vibration",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tr(language, "Vibration beim Abhaken", "Haptic Feedback"),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tr(language, "Haptischer Impuls beim Erledigen von Aufgaben", "Short vibration pulse on task completion"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isVibrationEnabled,
                                        onCheckedChange = { viewModel.setVibrationEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PrimaryViolet,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = ProgressTrack
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Card 6: Einführung
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
                                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Einführung & Onboarding", "Introduction & Onboarding"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(language, "Starte die Einführung erneut, um alle App-Tipps zu sehen.", "Restart the introduction to review all app tips."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Button(
                                    onClick = { viewModel.resetOnboarding() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ProgressTrack),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_restart_onboarding")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tr(language, "Einführung neu starten", "Restart Introduction"),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Card 7: Spielmodus
                    item {
                        val gameMode by viewModel.gameMode.collectAsStateWithLifecycle()
                        val unlockedSlots by viewModel.unlockedStorySlots.collectAsStateWithLifecycle()
                        val allHabitsList by viewModel.allHabits.collectAsStateWithLifecycle()
                        val activeCount = remember(allHabitsList) { allHabitsList.filter { !it.isArchived }.size }

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
                                    Icon(imageVector = Icons.Default.VideogameAsset, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "App-Spielmodus", "Game Mode"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(
                                        language,
                                        "Wähle zwischen dem schrittweisen Story-Modus (30-Tage Challenge für Slot-Freischaltungen) und dem Freien Spiel (unbegrenzte Gewohnheiten).",
                                        "Choose between Story Mode (30-day challenges to unlock slots) and Free Play (unlimited habits)."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = gameMode == "STORY",
                                        onClick = { viewModel.setGameMode("STORY", activeCount) },
                                        label = {
                                            Text(text = tr(language, "🎮 Story-Modus", "🎮 Story Mode"))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryViolet.copy(alpha = 0.2f),
                                            selectedLabelColor = PrimaryViolet
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = gameMode == "FREE",
                                        onClick = { viewModel.setGameMode("FREE", activeCount) },
                                        label = {
                                            Text(text = tr(language, "🔓 Freies Spiel", "🔓 Free Play"))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryViolet.copy(alpha = 0.2f),
                                            selectedLabelColor = PrimaryViolet
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (gameMode == "STORY") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = PrimaryViolet.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = tr(language, "Status: $activeCount / $unlockedSlots Slots belegt", "Status: $activeCount / $unlockedSlots Slots occupied"),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = PrimaryViolet,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tr(
                                                    language,
                                                    "Bestehende Gewohnheiten bleiben vollständig erhalten. Deine Freischaltungen passen sich automatisch an deinen aktuellen Stand an.",
                                                    "Existing habits are fully preserved. Your unlocked slots automatically adjust to your current habit count."
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
}
