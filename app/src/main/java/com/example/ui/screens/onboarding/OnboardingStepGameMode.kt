package com.example.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.HabitIconMapping
import com.example.ui.theme.PrimaryViolet

@Composable
fun OnboardingStepGameMode(language: String, viewModel: HabitsViewModel) {
    val gameMode by viewModel.gameMode.collectAsStateWithLifecycle()
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = tr(language, "Wähle deinen Spielmodus", "Spielmodus", "选择游戏模式", "Choose Game Mode"),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("game_mode_info_icon")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = tr(language, "Informationen zum Spielmodus", "ინფორმაცია თამაშის რეჟიმზე", "关于游戏模式", "Game mode info"),
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(
                language,
                "Wie möchtest du deine Gewohnheiten aufbauen?",
                "Wie möchtest du deine Gewohnheiten aufbauen?",
                "你想如何建立习惯？",
                "How do you want to build your habits?"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Option 1: Story-Modus
        Surface(
            onClick = { viewModel.setGameMode("STORY") },
            color = if (gameMode == "STORY") PrimaryViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = if (gameMode == "STORY") 2.dp else 1.dp,
                color = if (gameMode == "STORY") PrimaryViolet else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_mode_story")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(PrimaryViolet.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideogameAsset,
                                contentDescription = null,
                                tint = PrimaryViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(language, "Story-Modus 🎮", "Story Mode 🎮"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                color = PrimaryViolet.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tr(language, "Fokus & Belohnung", "Focus & Reward"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryViolet,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    RadioButton(
                        selected = (gameMode == "STORY"),
                        onClick = { viewModel.setGameMode("STORY") },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr(
                        language,
                        "• Starte mit 1 Gewohnheits-Slot\n• Meistere 30 Tage mit ≥ 85% Erfolgsquote\n• Schalte Schritt für Schritt neue Slots frei!",
                        "• Start with 1 habit slot\n• Master 30 days with ≥ 85% success rate\n• Step by step unlock new habit slots!"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option 2: Freies Spiel
        Surface(
            onClick = { viewModel.setGameMode("FREE") },
            color = if (gameMode == "FREE") PrimaryViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = if (gameMode == "FREE") 2.dp else 1.dp,
                color = if (gameMode == "FREE") PrimaryViolet else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_mode_free")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(language, "Freies Spiel 🔓", "Free Play 🔓"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tr(language, "Maximale Freiheit", "Maximum Freedom"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    RadioButton(
                        selected = (gameMode == "FREE"),
                        onClick = { viewModel.setGameMode("FREE") },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr(
                        language,
                        "• Unbegrenzte Gewohnheiten sofort freigeschaltet\n• Keine Vorbedingungen oder Slot-Sperren\n• Ideal für erfahrene Gewohnheits-Tracker",
                        "• Unlimited habit slots unlocked immediately\n• No prerequisites or slot restrictions\n• Ideal for experienced habit trackers"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = tr(
                        language,
                        "Warum mit 1 Gewohnheit starten?",
                        "რატომ 1 ჩვევა?",
                        "为什么从1个习惯开始？",
                        "Why Start with 1 Habit?"
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = tr(
                            language,
                            "Studien der Verhaltenspsychologie belegen: Wer mit nur einer Gewohnheit beginnt, hat eine 3-mal höhere Erfolgsquote. Zu viele Routinen gleichzeitig überfordern das Gehirn und führen schnell zum Abbruch.",
                            "კვლევები ადასტურებს: მხოლოდ 1 ჩვევით დაწყება 3-ჯერ ზრდის წარმატებას. ერთდროულად ბევრი რუტინა იწვევს გადაღლას.",
                            "行为心理学研究表明：从仅仅一个习惯开始的人，成功率高出3倍。一次尝试建立过多习惯容易导致大脑认知超载并迅速放弃。",
                            "Studies in behavioral psychology show: People who start with just one habit are 3x more likely to succeed. Attempting too many changes at once quickly leads to cognitive overload and quitting."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tr(
                            language,
                            "• Willenskraft ist endlich: Jede Entscheidung verbraucht mentale Energie. Wer 5 neue Gewohnheiten gleichzeitig erzwingen will, erleidet rasch Decision Fatigue und bricht ab.\n\n• Neuronale Bahnen: Das menschliche Gehirn benötigt 30 bis 66 Tage fokussierte Wiederholung, um neue Verhaltensmuster tief im Unterbewusstsein zu verankern.\n\n• Der Domino-Effekt: Wenn du eine Schlüsselgewohnheit (z.B. Sport, Meditation oder Lesen) gemeistert hast, stärkt dieser Erfolg deine Selbstwirksamkeit – nachfolgende Gewohnheiten fallen danach um ein Vielfaches leichter.\n\n• Story-Modus: Genau deshalb führt dich der Story-Modus Schritt für Schritt von 1 Gewohnheit zu stabilen, lebenslangen Routinen!",
                            "• ნებისყოფა სასრულია: ერთდროულად 5 ახალი ჩვევა იწვევს ენერგიის ამოწურვას.\n\n• ნეირონული გზები: ტვინს სჭირდება 30-დან 66 დღემდე ფოკუსირებული გამეორება ავტომატიზაციისთვის.\n\n• დომინოს ეფექტი: ერთი ჩვევის დაუფლების შემდეგ, შემდეგი ბევრად მარტივი ხდება!\n\n• Story Mode ზუსტად ამ პრინციპით მიგიყვანთ წარმატებამდე!",
                            "• 意志力是有限资源：试图同时建立5个新习惯通常会在2周内因决策疲劳而放弃。\n\n• 神经回路建立：大脑需要平均30至66天的专注重复，才能将新习惯内化为无意识的自动行为。\n\n• 多米诺骨牌效应：当你攻克了一个核心习惯，后续的新习惯建立将变得容易数倍。\n\n• Story 模式正是基于该科学原理，带你稳步迈向持久成功！",
                            "• Willpower is finite: Trying to force 5 new habits at once quickly results in mental exhaustion and abandonment.\n\n• Neural pathways: The brain requires 30 to 66 days of continuous repetition to wire a new behavior into the subconscious.\n\n• The Domino Effect: Mastering one keystone habit drastically boosts self-efficacy, making subsequent habits much easier to build.\n\n• Story Mode: This is why Story Mode guides you step by step from 1 habit to sustainable routines!"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text(
                        text = tr(language, "Verstanden", "გასაგებია", "明白了", "Got it"),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}
