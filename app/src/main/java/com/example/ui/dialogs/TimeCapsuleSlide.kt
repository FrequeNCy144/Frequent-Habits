package com.example.ui.dialogs

import com.example.ui.components.*

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.HabitsViewModel
import com.example.ui.components.AppTextField
import com.example.tr
import java.util.*

@Composable
fun TimeCapsuleSlide(
    type: String, // "MONTHLY" or "YEARLY"
    year: Int,
    month: Int = 1, // Only used for Monthly
    language: String,
    viewModel: HabitsViewModel
) {
    // 1. Calculate current period string & upcoming period string
    val currentPeriod = if (type == "MONTHLY") {
        "$year-%02d".format(month)
    } else "$year"

    val upcomingPeriod = if (type == "MONTHLY") {
        val currentYearMonth = java.time.YearMonth.of(year, month)
        currentYearMonth.plusMonths(1).toString() // "yyyy-MM"
    } else "${year + 1}"

    // 2. Fetch Past Note (targeting currentPeriod)
    val pastNoteFlow = remember(type, currentPeriod) { viewModel.getTimeCapsuleNote(type, currentPeriod) }
    val pastNote by pastNoteFlow.collectAsStateWithLifecycle(initialValue = null)

    // 3. Fetch Future Note draft (targeting upcomingPeriod)
    val futureNoteFlow = remember(type, upcomingPeriod) { viewModel.getTimeCapsuleNote(type, upcomingPeriod) }
    val futureNote by futureNoteFlow.collectAsStateWithLifecycle(initialValue = null)

    // Local state for the future note text input
    var futureText by remember { mutableStateOf("") }

    // Synchronize the input field text when the database loads the draft
    LaunchedEffect(futureNote) {
        if (futureNote != null && futureText.isEmpty()) {
            futureText = futureNote!!.content
        }
    }

    val titleText = if (type == "MONTHLY") {
        tr(language, "Zeitkapsel", "დროის კაფსულა", "时间胶囊", "Time Capsule")
    } else {
        tr(language, "Jahres-Zeitkapsel", "წლიური დროის კაფსულა", "年度时间胶囊", "Yearly Time Capsule")
    }

    val subtitleText = if (type == "MONTHLY") {
        tr(language, "Botschaften zwischen deinem vergangenen und zukünftigen Ich.", "შეტყობინებები თქვენს წარსულსა და მომავალს შორის.", "过去与未来的你之间的跨时空对话。", "Messages between your past and future self.")
    } else {
        tr(language, "Wirf einen Blick zurück und hinterlassen Wünsche für das nächste Jahr.", "გადახედეთ თქვენს მიზნებს და დატოვეთ შეტყობინებები მომავალი წლისთვის.", "回顾你当下的目标，并为来年留下期许寄语。", "Look back at your goals and leave messages for next year.")
    }

    val pastPeriodLabel = if (type == "MONTHLY") {
        val yearMonth = java.time.YearMonth.parse(currentPeriod)
        val loc = when (language) {
            "de" -> Locale.GERMAN
            "ka" -> Locale.forLanguageTag("ka")
            else -> Locale.ENGLISH
        }
        val monthName = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, loc)
        "$monthName $year"
    } else "$year"

    val futurePeriodLabel = if (type == "MONTHLY") {
        val yearMonth = java.time.YearMonth.parse(upcomingPeriod)
        val loc = when (language) {
            "de" -> Locale.GERMAN
            "ka" -> Locale.forLanguageTag("ka")
            else -> Locale.ENGLISH
        }
        val monthName = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, loc)
        "$monthName ${yearMonth.year}"
    } else "${year + 1}"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Text(
                text = "⏳ $titleText",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )
        }

        // PAST MESSAGE CARD
        item {
            Text(
                text = "📜 " + tr(language, "Nachricht aus der Vergangenheit ($pastPeriodLabel)", "შეტყობინება წარსულიდან ($pastPeriodLabel)", "来自过去的信件 ($pastPeriodLabel)", "Message from the past ($pastPeriodLabel)"),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (pastNote != null && pastNote!!.content.isNotBlank()) {
                        Text(
                            text = "\"${pastNote!!.content}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = tr(language, "Für diesen Zeitraum wurde keine Nachricht hinterlegt.", "ამ პერიოდისთვის შეტყობინება არ დარჩენილა.", "此阶段未留下时间胶囊寄语。", "No message was left for this period."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // FUTURE MESSAGE CARD (Input)
        item {
            Text(
                text = "✍️ " + tr(language, "Nachricht an dein zukünftiges Ich ($futurePeriodLabel)", "წერილი მომავალს ($futurePeriodLabel)", "写给未来的自己 ($futurePeriodLabel)", "Message to your future self ($futurePeriodLabel)"),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (type == "MONTHLY") {
                            tr(language, "Was möchtest du im nächsten Monat erreichen? Welche Gewohnheiten willst du meistern? Schreib es hier nieder:", "რისი მიღწევა გსურთ შემდეგ თვეში? რომელი ჩვევების დაუფლება გსურთ? დაწერე აქ:", "下个月你想达成什么目标？想养成哪些好习惯？写在这里：", "What do you want to achieve next month? Which habits do you want to master? Write it here:")
                        } else {
                            tr(language, "Hinterlasse eine Nachricht oder ein großes Lebensziel an dein zukünftiges Ich für das kommende Jahr:", "დატოვეთ მესიჯი ან მთავარი ცხოვრებისეული მიზანი მომავალი წლისთვის:", "为来年写下一句寄语或核心人生目标，留给未来的自己：", "Leave a message or a major life goal for your future self for the upcoming year:")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AppTextField(
                        value = futureText,
                        onValueChange = { newValue ->
                            futureText = newValue
                            viewModel.saveTimeCapsuleNote(type, upcomingPeriod, newValue)
                        },
                        placeholderText = tr(language, "Liebes zukünftiges Ich...", "ძვირფასო მომავალო...", "亲爱的未来的我...", "Dear future self..."),
                        modifier = Modifier.height(130.dp),
                        containerColor = Color.Black.copy(alpha = 0.15f),
                        accentColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        singleLine = false,
                        maxLines = 5,
                        testTag = "time_capsule_future_input"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tr(language, "Diese Kapsel bleibt bis zum Beginn von $futurePeriodLabel versiegelt.", "ეს კაფსულა რჩება დალუქული $futurePeriodLabel -ის დაწყებამდე.", "此胶囊将在 $futurePeriodLabel 开始前保持密封。", "This capsule remains sealed until the start of $futurePeriodLabel."),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
