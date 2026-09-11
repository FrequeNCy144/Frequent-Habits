package com.example.ui.screens.createhabit

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Habit
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*

@Composable
fun HabitAdvancedSettingsCards(
    language: String,
    startDateStr: String,
    onStartDateClick: () -> Unit,
    motivationWhy: String,
    onMotivationWhyChange: (String) -> Unit,
    minimalViableRule: String,
    onMinimalViableRuleChange: (String) -> Unit,
    isNumeric: Boolean = false,
    unit: String = "",
    selectedStackedHabit: Habit?,
    onSelectStackedHabit: (Habit?) -> Unit,
    activeHabits: List<Habit>,
    onShowInfoDialog: (title: String, explanation: String) -> Unit = { _, _ -> },
    onShowStackingInfoDialog: () -> Unit = {},
    onShowTwoMinuteInfoDialog: () -> Unit = {},
    activeColor: Color
) {
    val hasAdvancedConfig = motivationWhy.isNotBlank() || minimalViableRule.isNotBlank() || selectedStackedHabit != null
    var isExpanded by remember { mutableStateOf(hasAdvancedConfig) }
    var showHabitStackingDropdown by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Clickable row to toggle expansion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Advanced Options",
                        tint = if (isExpanded) PrimaryViolet else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr(language, "Erweiterte Optionen", "დამატებითი პარამეტრები", "高级选项", "Advanced Options"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (!isExpanded) {
                            Text(
                                text = tr(language, "Startdatum, Motivation, Miniversion, Habit Stacking", "დაწყების თარიღი, მოტივაცია, მინი-ვერსია, ჩვევების დაწყობა", "开始日期、动机、微习惯、习惯叠加", "Start Date, Motivation, Micro-version, Habit Stacking"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Advanced Section 1: Startdatum
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProgressTrack.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Startdatum",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tr(language, "Startdatum", "დაწყების თარიღი", "开始日期", "Start Date"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                InfoIconButton(
                                    title = tr(language, "Startdatum", "დაწყების თარიღი", "开始日期", "Start Date"),
                                    explanation = tr(
                                        language,
                                        "📅 Flexibler Start:\n\n• Startdatum: Ab welchem Tag die Gewohnheit aktiv getrackt wird (z. B. heute oder am kommenden Montag).",
                                        "📅 Flexible Start:\n\n• Start Date: Day tracking begins (e.g. today or next Monday)."
                                    ),
                                    onClick = { t, e -> onShowInfoDialog(t, e) }
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = AppCard),
                                border = BorderStroke(1.dp, AppBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onStartDateClick)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = "Calendar",
                                            tint = PrimaryViolet,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = startDateStr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.EditCalendar,
                                        contentDescription = "Edit Start Date",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }


                        }
                    }

                    // Advanced Section 2: Motivation / Warum
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProgressTrack.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Motivation",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tr(language, "Dein „Warum“ (Motivation)", "შენი „რატომ“ (მოტივაცია)", "你的“为什么”（动机）", "Your 'Why' (Motivation)"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                InfoIconButton(
                                    title = tr(language, "Dein „Warum“ (Motivation)", "შენი „რატომ“ (მოტივაცია)", "你的“为什么”（动机）", "Your 'Why' (Motivation)"),
                                    explanation = tr(
                                        language,
                                        "🧠 Psychologischer Anker:\n\nStudien zur Verhaltenspsychologie belegen: Wer den tieferen persönlichen Sinn einer Gewohnheit klar vor Augen hat, überwindet motivationale Tiefs deutlich leichter.\n\n📱 Verwendung in der App:\nDein „Warum“ wird dir nicht nur zur Erinnerung in der App angezeigt, sondern auch als motivierende Push-Benachrichtigung gesendet – insbesondere dann, wenn du eine Gewohnheit schleifen lässt oder einen zusätzlichen Motivationsschub brauchst.\n\nNotiere hier, warum diese Gewohnheit dein Leben positiv verändert – als kraftvolle Erinnerung an schwachen Tagen.",
                                        "🧠 ფსიქოლოგიური საყრდენი:\n\nქცევითი ფსიქოლოგიის კვლევები ადასტურებს: როცა ჩვევის პირად ღრმა აზრს ნათლად ხედავთ, მოტივაციის დაქვეითებას ბევრად მარტივად გადალახავთ.\n\n📱 გამოყენება აპლიკაციაში:\nთქვენი „რატომ“ გამოჩნდება არა მხოლოდ აპლიკაციაში, არამედ გამოგეგზავნებათ მოტივაციური შეტყობინების სახითაც, როდესაც შეხსენება ან დამატებითი სტიმული დაგჭირდებათ.\n\nჩაწერეთ აქ, რატომ ცვლის ეს ჩვევა თქვენს ცხოვრებას პოზიტიურად.",
                                        "🧠 心理学锚点：\n\n行为心理学研究表明：清楚了解习惯背后的深层个人意义，能极大帮助克服动力不足的低谷期。\n\n📱 在应用中的用途：\n你的“为什么”不仅会在应用内展示，还会在你可能遗漏习惯或需要额外动力时，作为激励性推送通知发送给你。\n\n在此记录下这个习惯为你生活带来的积极改变，作为低谷时的强力提醒。",
                                        "🧠 Psychological Anchor:\n\nBehavioral research shows: Having a clear intrinsic purpose drastically improves consistency and helps overcome motivational dips.\n\n📱 Usage in the App:\nYour 'Why' is not only displayed within the app, but is also sent to you as an encouraging push notification reminder when you miss a habit or need an extra boost of motivation.\n\nWrite down why this habit positively impacts your life – as a powerful reminder on challenging days."
                                    ),
                                    onClick = { t, e -> onShowInfoDialog(t, e) }
                                )
                            }

                            AppTextField(
                                value = motivationWhy,
                                onValueChange = onMotivationWhyChange,
                                placeholderText = tr(language, "z.B. Ich möchte mich fitter und ausgeglichener fühlen...", "მაგ. მინდა ვიყო ჯანმრთელი...", "例如：我想感觉更健康、精力更充沛……", "e.g., I want to feel more energetic and healthy..."),
                                singleLine = false,
                                maxLines = 3,
                                accentColor = activeColor
                            )
                        }
                    }

                    // Advanced Section 3: Minimal Viable Version (MVV)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProgressTrack.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Minimal Viable Version",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tr(language, "Minimal Viable Version (MVV)", "მინიმალური ვერსია (MVV)", "最小可行版本（MVV）", "Minimal Viable Version (MVV)"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                InfoIconButton(
                                    title = tr(language, "Minimal Viable Version", "მინიმალური ვერსია", "最小可行版本", "Minimal Viable Version"),
                                    explanation = tr(
                                        language,
                                        "⚡ Minimal Viable Version (MVV):\n\nDie kleinste, machbare Version deiner Gewohnheit für stressige Tage oder Notfälle (z.B. nur 1 Liegestütz, 1 Seite lesen oder 2 Minuten meditieren).\n\n🔬 Der wissenschaftliche Effekt:\nIndem du die Einstiegshürde an schwierigen Tagen senkst, schützt du deine neuronale Gewohnheitsschleife und deinen Streak. Besser eine minimale Version als gar keine!",
                                        "⚡ Minimal Viable Version (MVV):\n\nScale any habit down to its smallest doable version for busy days or emergencies (e.g. 1 pushup, 1 page or 2 minutes).\n\n🔬 The Science:\nLowering friction on hard days preserves your habit momentum and protects your streak. Consistency beats intensity!"
                                    ),
                                    onClick = { t, e -> onShowInfoDialog(t, e) }
                                )
                            }

                            val mvvPlaceholder = if (isNumeric) {
                                if (unit.isNotBlank()) "z. B. 1 $unit" else "z. B. 1"
                            } else {
                                tr(language, "z.B. Nur 1 Liegestütz / 1 Seite lesen / 2 Min", "მაგ. 1 აზიდვა", "例如：只做 1 个俯卧撑 / 读 1 页书", "e.g., Just 1 pushup / read 1 page")
                            }

                            AppTextField(
                                value = minimalViableRule,
                                onValueChange = onMinimalViableRuleChange,
                                placeholderText = mvvPlaceholder,
                                keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                                singleLine = true,
                                accentColor = activeColor
                            )
                        }
                    }

                    // Advanced Section 4: Habit Stacking
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProgressTrack.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Habit Stacking",
                                        tint = PrimaryViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tr(language, "Habit Stacking (Anker-Gewohnheit)", "ჩვევების დაწყობა", "习惯叠加（锚点习惯）", "Habit Stacking"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                InfoIconButton(
                                    title = tr(language, "Habit Stacking (Wissenschaftlich fundiert)", "ჩვევების დაწყობა", "习惯叠加（科学验证）", "Habit Stacking (Scientifically Proven)"),
                                    explanation = tr(
                                        language,
                                        "🔬 Wissenschaftlich belegt (Dr. BJ Fogg, Stanford University & James Clear):\n\nDein Gehirn besitzt für bereits feste Routinen (wie Zähneputzen, Kaffee kochen oder Schuhe anziehen) extrem stabile synaptische Nervenbahnen.\n\nBeim Habit Stacking hängst du eine neue Gewohnheit direkt an eine solche bestehende Anker-Gewohnheit an:\n➜ „NACHDEM ich [Anker-Gewohnheit] erledigt habe, werde ich sofort [neue Gewohnheit] ausführen.“\n\n💡 Warum das so effektiv ist:\nDu musst dich nicht mehr mühsam daran erinnern oder auf Willenskraft hoffen. Der Anker triggert das neue Verhalten im Gehirn ganz natürlich. Studien belegen, dass Gewohnheiten dadurch bis zu 2-3x schneller und stabiler automatisiert werden!",
                                        "🔬 Scientifically Proven (Dr. BJ Fogg, Stanford & James Clear):\n\nYour brain already has highly reinforced neural pathways for established daily routines.\n\nBy stacking a new behavior onto an existing anchor habit ('After I [Anchor], I will immediately [New Habit]'), you leverage automatic triggers. Studies show habits automate up to 2-3x faster and stay consistent!"
                                    ),
                                    onClick = { t, e -> onShowInfoDialog(t, e) }
                                )
                            }

                            // Dropdown selection for Anchor Habit
                            Box {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AppCard),
                                    border = BorderStroke(1.dp, AppBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showHabitStackingDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedStackedHabit?.name ?: tr(language, "Keine Anker-Gewohnheit (Eigenständig)", "ანკერი არ არის", "无锚点习惯（独立）", "No Anchor Habit (Standalone)"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedStackedHabit != null) TextPrimary else TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Anchor",
                                            tint = TextSecondary
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showHabitStackingDropdown,
                                    onDismissRequest = { showHabitStackingDropdown = false },
                                    modifier = Modifier.background(AppCard)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(tr(language, "Keine (Eigenständig)", "არცერთი", "无（独立）", "None (Standalone)"), color = TextPrimary) },
                                        onClick = {
                                            onSelectStackedHabit(null)
                                            showHabitStackingDropdown = false
                                        }
                                    )
                                    activeHabits.forEach { anchor ->
                                        DropdownMenuItem(
                                            text = { Text(anchor.name, color = TextPrimary) },
                                            onClick = {
                                                onSelectStackedHabit(anchor)
                                                showHabitStackingDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedStackedHabit != null) {
                                Surface(
                                    color = PrimaryViolet.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "„Nachdem ich „${selectedStackedHabit.name}“ erledigt habe, werde ich diese Gewohnheit ausführen.“",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = PrimaryViolet,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(12.dp)
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
