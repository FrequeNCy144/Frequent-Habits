package com.example.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.components.HabitItemRow
import com.example.ui.theme.*

@Composable
fun OnboardingStep1(language: String) {
    val sampleHabit1 = remember(language) {
        Habit(
            id = 991,
            name = tr(language, "Wasser trinken", "დალიე წყალი", "喝水", "Drink Water"),
            category = "Gesundheit",
            icon = "water",
            color = "teal",
            type = "NUMBER",
            unit = "ml",
            targetValue = 3000f,
            reminderEnabled = true,
            reminderHour = 9,
            reminderMinute = 0
        )
    }
    val sampleHabit2 = remember(language) {
        Habit(
            id = 992,
            name = tr(language, "Morgenmeditation", "დილის მედიტაცია", "晨间冥想", "Morning Meditation"),
            category = "Achtsamkeit",
            icon = "meditation",
            color = "purple",
            type = "BINARY"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HabitItemRow(
                habit = sampleHabit1,
                currentValue = 2250f,
                isCompleted = false,
                isFailed = false,
                isPaused = false,
                hasLog = true,
                onToggle = { _, _ -> },
                onAddQuantity = { _, _, _ -> },
                onLongClick = {},
                language = language
            )

            HabitItemRow(
                habit = sampleHabit2,
                currentValue = 1f,
                isCompleted = true,
                isFailed = false,
                isPaused = false,
                hasLog = true,
                onToggle = { _, _ -> },
                onAddQuantity = { _, _, _ -> },
                onLongClick = {},
                language = language
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tr(language, "Gewohnheiten steuern & anpassen", "ჩვევების კონტროლი და მორგება", "管理与定制习惯", "Control & Customize Habits"),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text("👆 ", fontSize = 14.sp)
                Text(
                    text = tr(language, "Tippen auf Karte: Abhaken (Ja/Nein) oder Wert-Eingabedialog öffnen (Zahlen).", "ბარათზე შეხება: მონიშვნა (დიახ/არა) ან მნიშვნელობის დიალოგის გახსნა.", "点击卡片：打卡完成（开关型）或打开数值输入弹窗（计量型）。", "Tap card: Toggle completion (binary) or open exact value dialog (numeric)."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Text("➕ ", fontSize = 14.sp)
                Text(
                    text = if (language == "de") "Kreis-Symbol rechts: Erhöht bei Zahlen-Gewohnheiten sofort den Wert um +1." else if (language == "ka") "მარჯვენა წრის ხატულა: სწრაფად ზრდის ციფრულ მნიშვნელობას +1-ით ერთი შეხებით." else "Right circle icon: Quickly increments numeric value by +1 with one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Text("⏱️ ", fontSize = 14.sp)
                Text(
                    text = tr(language, "Gedrückt halten: Kontextmenü zum Pausieren, Fehlgeschlagen, Bearbeiten & Löschen.", "დიდხანს დააჭირეთ: კონტექსტური მენიუ პაუზისთვის, წარუმატებელი სტატუსისთვის, რედაქტირება და წაშლა.", "长按卡片：呼出快捷菜单进行暂停、标记未达成、编辑与删除。", "Long Press: Context menu for Pause, Failed status, Edit & Delete."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}

// STEP 2: Audio Soundscapes & Fokus-Timer
@Composable
fun OnboardingStep2(language: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(language, "FOKUS TIMER", "ფოკუსის ტაიმერი", "专注计时器", "FOCUS TIMER"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = SuccessBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SuccessGreen)
                    ) {
                        Text(
                            text = tr(language, "AKTIV", "აქტიური", "进行中", "ACTIVE"),
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(ProgressTrack, CircleShape)
                        .border(2.dp, PrimaryViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "24:35",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(language, "Minuten", "წუთები", "分钟", "Minutes"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ProgressTrack, CircleShape)
                            .border(1.dp, AppBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SuccessGreen.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, SuccessGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Finish",
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(language, "Hintergrund-Audio", "ფონის აუდიო", "背景音频", "Background Audio"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(AppBg, RoundedCornerShape(14.dp))
                                .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(language, "Sound suchen...", "ხმის ძებნა...", "搜索白噪音...", "Search sound..."),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PrimaryViolet,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitOrange),
                            border = BorderStroke(1.dp, HabitOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = tr(language, "Reset", "გადატვირთვა", "重置", "Reset"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FailedRed),
                            border = BorderStroke(1.dp, FailedRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = tr(language, "Fehlgeschlagen", "ვერ მოხერხდა", "未达成", "Failed"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = tr(language, "Speichern", "შენახვა", "保存", "Save"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tr(language, "Fokus mit Naturklängen", "ფოკუსირება ბუნების ხმებით", "伴随大自然白噪音专注", "Focus with Nature Soundscapes"),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (language == "de") "Integrierter Timer mit sanften Hintergrundgeräuschen für ungestörte Konzentration." else if (language == "ka") "ჩამონტაჟებული ფოკუსირების ტაიმერი დამამშვიდებელი ფონის ხმებით უწყვეტი კონცენტრაციისთვის." else "Built-in focus timer with soothing background sounds for uninterrupted concentration.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// STEP 3: Statistiken & Heatmap
@Composable
fun OnboardingStep3(language: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tr(language, "STATISTIK & HEATMAP", "სტატისტიკა და სითბოს რუკა", "统计与热力图", "STATISTICS & HEATMAP"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(7) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(
                                    if (it % 2 == 0) SuccessGreen else SuccessGreen.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = tr(language, "Behalte den Überblick", "თვალყური ადევნეთ პროგრესს", "时刻掌握进度", "Keep Track of Progress"),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr(language, "Visualisiere deine Erfolge in der Wochen- und Jahresübersicht.", "თქვენი მიღწევების ვიზუალიზაცია ყოველკვირეულ და წლიურ მიმოხილვებში.", "在每周和年度总览中直观呈现你的成就。", "Visualize your achievements in weekly and yearly overviews."),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// STEP 4: Home-Widgets & SAF Backup
@Composable
fun OnboardingStep4(language: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(language, "HOMESCREEN WIDGETS", "საწყისი ეკრანის ვიჯეტები", "桌面小组件", "HOMESCREEN WIDGETS"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = tr(language, "Schnellzugriff auf deinen Homescreen", "სწრაფი წვდომა მთავარ ეკრანზე", "主屏幕快捷访问", "Quick Access on Homescreen"),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr(language, "Füge Widgets hinzu, um deine Gewohnheiten direkt vom Homescreen aus abzuhaken.", "დაამატეთ ვიჯეტები ჩვევების დასასრულებლად პირდაპირ თქვენი საწყისი ეკრანიდან.", "添加小组件，直接在主屏幕上轻松打卡习惯。", "Add widgets to complete habits directly from your home screen."),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// STEP 5: Fertigstellen / Starten
@Composable
fun OnboardingStep5(language: String, viewModel: HabitsViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PrimaryViolet.copy(alpha = 0.2f), CircleShape)
                .border(2.dp, PrimaryViolet, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PrimaryViolet,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = tr(language, "Bereit für deine Reise!", "მოემზადეთ თქვენი მოგზაურობისთვის!", "准备开启你的习惯之旅！", "Ready for your journey!"),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tr(language, "Starte jetzt und baue deine Traumgewohnheiten auf.", "დაიწყე ახლა და ჩამოაყალიბე შენი საოცნებო ჩვევები.", "立即开始，打造你的理想习惯。", "Start now and build your dream habits."),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
