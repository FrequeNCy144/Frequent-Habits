package com.example.ui.screens.createhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.components.InfoIconButton
import com.example.ui.theme.*
import com.frequent.habits.R

@Composable
fun HabitMotivationCard(
    language: String,
    motivationWhy: String,
    onMotivationWhyChange: (String) -> Unit,
    accentColor: Color = PrimaryViolet,
    onShowInfoDialog: (title: String, explanation: String) -> Unit = { _, _ -> }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_brain),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(language, "Dein „Warum“ (Motivation)", "შენი „რატომ“ (მოტივაცია)", "你的“为什么”（动机）", "Your 'Why' (Motivation)"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                language,
                                "Psychologischer Anker für schwierige Tage",
                                "ფსიქოლოგიური საყრდენი",
                                "克服动力不足的心理锚点",
                                "Psychological anchor for challenging days"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
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

            // Motivation text input
            AppTextField(
                value = motivationWhy,
                onValueChange = onMotivationWhyChange,
                placeholderText = tr(
                    language,
                    "z.B. Ich möchte mehr Energie haben und mich ausgeglichener fühlen...",
                    "მაგ. მინდა მეტი ენერგია მქონდეს...",
                    "例如：我想拥有更多精力，感觉更平静充实……",
                    "e.g. I want to have more energy and feel balanced..."
                ),
                singleLine = false,
                maxLines = 3,
                accentColor = accentColor
            )
        }
    }
}
