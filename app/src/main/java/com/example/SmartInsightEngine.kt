package com.example

import android.content.Context
import com.example.data.Habit
import com.example.data.HabitLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SmartInsightIconType {
    REVIEW,
    COMPLETION,
    BOLT,
    TRENDING_UP,
    FIRE,
    TARGET,
    STAR,
    SPARKLE,
    LIGHTBULB,
    TRENDING_DOWN,
    DATE_RANGE,
    CHART,
    ARROW_UP
}

data class SmartInsight(
    val type: String,
    val text: String,
    val cleanText: String,
    val iconType: SmartInsightIconType,
    val isReviewPrompt: Boolean = false,
    val reviewYear: Int? = null,
    val reviewMonth: Int? = null
)

object SmartInsightEngine {

    const val REQUIRED_LOGGED_DAYS = 7

    fun getUniqueLoggedDaysCount(allLogs: List<HabitLog>): Int {
        return allLogs.filter { it.value > 0f && !it.isPaused }.map { it.date }.distinct().size
    }

    fun hasUnlockedSmartInsights(allLogs: List<HabitLog>): Boolean {
        return getUniqueLoggedDaysCount(allLogs) >= REQUIRED_LOGGED_DAYS
    }

    fun isInsightTypeInCooldown(context: Context, type: String, todayDateString: String): Boolean {
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val lastShown = prefs.getString("insight_cooldown_$type", null) ?: return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val d1 = sdf.parse(lastShown)
            val d2 = sdf.parse(todayDateString)
            if (d1 != null && d2 != null) {
                val diffMs = d2.time - d1.time
                val diffDays = Math.round(diffMs / (1000f * 60 * 60 * 24)).toInt()
                diffDays < 10 && diffDays > 0
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun recordInsightTypeShown(context: Context, type: String, todayDateString: String) {
        val prefs = context.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("insight_cooldown_$type", todayDateString).apply()
    }

    fun generateInsights(
        context: Context,
        allHabits: List<Habit>,
        allLogs: List<HabitLog>,
        language: String,
        userName: String = "",
        todayDateString: String,
        currentStreak: Int = 0,
        completionRate: Int = 0,
        dismissedReviews: Set<String> = emptySet(),
        checkCooldowns: Boolean = true
    ): List<SmartInsight> {
        if (!hasUnlockedSmartInsights(allLogs)) {
            return emptyList()
        }

        val list = mutableListOf<SmartInsight>()
        val greeting = if (userName.isNotBlank()) "Hey $userName, " else ""

        // Calculate habit strengths
        val habitsWithStrength = allHabits.map { habit ->
            val strength = com.example.data.calculateHabitStrength(habit, allLogs)
            Pair(habit, strength)
        }

        // 1. Action-Based Review Insight
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curDay = cal.get(Calendar.DAY_OF_MONTH)
        var prevYear = curYear
        var prevMonth = curMonth - 1
        if (prevMonth < 1) {
            prevMonth = 12
            prevYear -= 1
        }

        val monthlyKey = "monthly_${prevYear}_${prevMonth}"
        val yearlyKey = "yearly_${prevYear}"

        if (curMonth == 1 && curDay <= 15 && yearlyKey !in dismissedReviews && (!checkCooldowns || !isInsightTypeInCooldown(context, "REVIEW_PROMPT", todayDateString))) {
            val text = when (language) {
                "de" -> "${greeting}dein **Jahres-Review $prevYear** ist bereit! Öffne deine Zeitkapsel. ⏳"
                "ka" -> "${greeting}თქვენი **$prevYear წლის მიმოხილვა** მზად არის! გახსენით დროის კაფსულა. ⏳"
                "zh" -> "${greeting}你的 **$prevYear 年度回顾** 已生成！开启时光胶囊。⏳"
                "fr" -> "${greeting}ton **Bilan annuel $prevYear** est prêt ! Ouvre ta capsule temporelle. ⏳"
                else -> "${greeting}your **$prevYear yearly review** is ready! Open your time capsule. ⏳"
            }
            list.add(SmartInsight("REVIEW_PROMPT", text, text.replace("**", ""), SmartInsightIconType.REVIEW, true, prevYear, null))
        } else if (curDay <= 7 && monthlyKey !in dismissedReviews && (!checkCooldowns || !isInsightTypeInCooldown(context, "REVIEW_PROMPT", todayDateString))) {
            val text = when (language) {
                "de" -> "${greeting}dein **Monats-Review** ist bereit! Öffne deine Zeitkapsel. ⏳"
                "ka" -> "${greeting}თქვენი **თვიური მიმოხილვა** მზად არის! გახსენით დროის კაფსულა. ⏳"
                "zh" -> "${greeting}你的 **月度回顾** 已生成！开启时光胶囊。⏳"
                "fr" -> "${greeting}ton **Bilan mensuel** est prêt ! Ouvre ta capsule temporelle. ⏳"
                else -> "${greeting}your **monthly review** is ready! Open your time capsule. ⏳"
            }
            list.add(SmartInsight("REVIEW_PROMPT", text, text.replace("**", ""), SmartInsightIconType.REVIEW, true, prevYear, prevMonth))
        }

        // 2. Total completions
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "TOTAL_COMPLETIONS", todayDateString)) {
            val totalCompletions = allLogs.count { it.value > 0f && !it.isPaused }
            if (totalCompletions >= 10) {
                val text = when (language) {
                    "de" -> "${greeting}du hast insgesamt bereits **$totalCompletions** Gewohnheiten erfolgreich absolviert! 🚀"
                    "ka" -> "${greeting}თქვენ წარმატებით დაასრულეთ **$totalCompletions** ჩვევა სულ! 🚀"
                    "zh" -> "${greeting}你已累计成功完成 **$totalCompletions** 次习惯打卡！🚀"
                    "fr" -> "${greeting}tu as déjà accompli avec succès un total de **$totalCompletions** habitudes ! 🚀"
                    else -> "${greeting}you have successfully completed **$totalCompletions** habits in total! 🚀"
                }
                list.add(SmartInsight("TOTAL_COMPLETIONS", text, text.replace("**", ""), SmartInsightIconType.COMPLETION))
            }
        }

        // 3. Weekly volume
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "WEEKLY_VOLUME", todayDateString)) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDate = try { sdf.parse(todayDateString) } catch (e: Exception) { null }
            val weeklyCompletions = if (todayDate != null) {
                allLogs.count { log ->
                    if (log.value > 0f && !log.isPaused) {
                        val d = try { sdf.parse(log.date) } catch (e: Exception) { null }
                        if (d != null) {
                            val diffDays = ((todayDate.time - d.time) / (1000L * 60 * 60 * 24)).toInt()
                            diffDays in 0..6
                        } else false
                    } else false
                }
            } else 0

            if (weeklyCompletions >= 3) {
                val text = when (language) {
                    "de" -> "${greeting}diese Woche hast du bereits **$weeklyCompletions** Abschlüsse geschafft. Ein fantastischer Rhythmus! ⚡"
                    "ka" -> "${greeting}ამ კვირაში უკვე მიაღწიეთ **$weeklyCompletions** დასრულებას. შესანიშნავი რიტმია! ⚡"
                    "zh" -> "${greeting}本周你已经完成了 **$weeklyCompletions** 次打卡。绝佳的节奏！⚡"
                    "fr" -> "${greeting}cette semaine tu as déjà réalisé **$weeklyCompletions** réussites. Un rythme fantastique ! ⚡"
                    else -> "${greeting}you've already achieved **$weeklyCompletions** completions this week. A fantastic rhythm! ⚡"
                }
                list.add(SmartInsight("WEEKLY_VOLUME", text, text.replace("**", ""), SmartInsightIconType.BOLT))
            }
        }

        // 4. Most active weekday
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "MOST_ACTIVE_DAY", todayDateString)) {
            val weekdayCompletionsMap = IntArray(8)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            allLogs.forEach { log ->
                if (log.value > 0f && !log.isPaused) {
                    try {
                        val dateVal = sdf.parse(log.date)
                        if (dateVal != null) {
                            val calDay = Calendar.getInstance()
                            calDay.time = dateVal
                            val dayOfWeek = calDay.get(Calendar.DAY_OF_WEEK)
                            if (dayOfWeek in 1..7) {
                                weekdayCompletionsMap[dayOfWeek]++
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            var maxDayIndex = -1
            var maxCount = 0
            for (i in 1..7) {
                if (weekdayCompletionsMap[i] > maxCount) {
                    maxCount = weekdayCompletionsMap[i]
                    maxDayIndex = i
                }
            }
            if (maxDayIndex != -1 && maxCount >= 3) {
                val dayName = when (maxDayIndex) {
                    Calendar.MONDAY -> tr(language, "Montag", "ორშაბათი", "周一", "Monday", "Lundi")
                    Calendar.TUESDAY -> tr(language, "Dienstag", "სამშაბათი", "周二", "Tuesday", "Mardi")
                    Calendar.WEDNESDAY -> tr(language, "Mittwoch", "ოთხშაბათი", "周三", "Wednesday", "Mercredi")
                    Calendar.THURSDAY -> tr(language, "Donnerstag", "ხუთშაბათი", "周四", "Thursday", "Jeudi")
                    Calendar.FRIDAY -> tr(language, "Freitag", "პარასკევი", "周五", "Friday", "Vendredi")
                    Calendar.SATURDAY -> tr(language, "Samstag", "შაბათი", "周六", "Saturday", "Samedi")
                    Calendar.SUNDAY -> tr(language, "Sonntag", "კვირა", "周日", "Sunday", "Dimanche")
                    else -> ""
                }
                if (dayName.isNotEmpty()) {
                    val text = when (language) {
                        "de" -> "${greeting}dein aktivster Wochentag ist der **$dayName** mit insgesamt **$maxCount** erfolgreichen Abschlüssen! 📈"
                        "ka" -> "${greeting}კვირის თქვენი ყველაზე აქტიური დღეა **$dayName** სულ **$maxCount** წარმატებული შესრულებით! 📈"
                        "zh" -> "${greeting}你在一周中最活跃的一天是 **$dayName**，共完成了 **$maxCount** 次打卡！📈"
                        "fr" -> "${greeting}ton jour le plus actif est le **$dayName** avec un total de **$maxCount** réussites ! 📈"
                        else -> "${greeting}your most active day of the week is **$dayName** with a total of **$maxCount** successful completions! 📈"
                    }
                    list.add(SmartInsight("MOST_ACTIVE_DAY", text, text.replace("**", ""), SmartInsightIconType.TRENDING_UP))
                }
            }
        }

        // 5. Perfect days streak
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "PERFECT_DAYS_STREAK", todayDateString)) {
            if (currentStreak >= 2) {
                val text = when (language) {
                    "de" -> "${greeting}du bist on fire! 🔥 Schon **$currentStreak** perfekte Tage in Folge. Lass uns diese Serie heute fortsetzen!"
                    "ka" -> "${greeting}შენ ცეცხლი ხარ! 🔥 უკვე **$currentStreak** შესანიშნავი დღე ზედიზედ. გავაგრძელოთ ეს სერია!"
                    "zh" -> "${greeting}状态火热！🔥 已经连续 **$currentStreak** 个完美日。今天继续保持这个连胜！"
                    "fr" -> "${greeting}tu es en feu ! 🔥 Déjà **$currentStreak** jours parfaits consécutifs. Continuons sur cette lancée aujourd'hui !"
                    else -> "${greeting}you are on fire! 🔥 **$currentStreak** perfect days in a row. Let's keep this streak going today!"
                }
                list.add(SmartInsight("PERFECT_DAYS_STREAK", text, text.replace("**", ""), SmartInsightIconType.FIRE))
            }
        }

        // 6. Completion rate
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "COMPLETION_RATE", todayDateString)) {
            if (completionRate >= 50) {
                val text = when (language) {
                    "de" -> "${greeting}deine Gesamt-Erfolgsquote liegt bei extrem starken **$completionRate%**. Bleib weiter so fokussiert! 🎯"
                    "ka" -> "${greeting}თქვენი საერთო წარმატების მაჩვენებელი არის ძლიერი **$completionRate%**. დარჩით კონცენტრირებული! 🎯"
                    "zh" -> "${greeting}你的总完成率达到了极佳的 **$completionRate%**。继续保持专注！🎯"
                    "fr" -> "${greeting}ton taux de réussite global s'élève à un solide **$completionRate%**. Reste concentré(e) ! 🎯"
                    else -> "${greeting}your overall success rate is an extremely strong **$completionRate%**. Keep staying focused! 🎯"
                }
                list.add(SmartInsight("COMPLETION_RATE", text, text.replace("**", ""), SmartInsightIconType.TARGET))
            }
        }

        // 7. Top performer
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "TOP_PERFORMER", todayDateString)) {
            val habitCompletionsMap = mutableMapOf<Int, Int>()
            allLogs.forEach { log ->
                if (log.value > 0f && !log.isPaused) {
                    habitCompletionsMap[log.habitId] = (habitCompletionsMap[log.habitId] ?: 0) + 1
                }
            }
            val maxHabitEntry = habitCompletionsMap.maxByOrNull { it.value }
            if (maxHabitEntry != null && maxHabitEntry.value >= 3) {
                val matchingHabit = allHabits.find { it.id == maxHabitEntry.key }
                if (matchingHabit != null) {
                    val text = when (language) {
                        "de" -> "${greeting}dein absoluter Spitzenreiter ist **'${matchingHabit.name}'** mit bereits **${maxHabitEntry.value}** Abschlüssen! Tolle Leistung. 🟢"
                        "ka" -> "${greeting}თქვენი საუკეთესო ჩვევაა **'${matchingHabit.name}'** უკვე **${maxHabitEntry.value}** შესრულებით! ყოჩაღ. 🟢"
                        "zh" -> "${greeting}你的王牌习惯是 **“${matchingHabit.name}”**，已打卡 **${maxHabitEntry.value}** 次！太棒了。🟢"
                        "fr" -> "${greeting}ton habitude championne est **'${matchingHabit.name}'** avec déjà **${maxHabitEntry.value}** réalisations ! Bravo. 🟢"
                        else -> "${greeting}your absolute top performer is **'${matchingHabit.name}'** with already **${maxHabitEntry.value}** completions! Great job. 🟢"
                    }
                    list.add(SmartInsight("TOP_PERFORMER", text, text.replace("**", ""), SmartInsightIconType.STAR))
                }
            }
        }

        // 8. High momentum
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "HIGH_MOMENTUM", todayDateString)) {
            val highMomentum = habitsWithStrength.maxByOrNull { it.second }
            if (highMomentum != null && highMomentum.second >= 75) {
                val text = when (language) {
                    "de" -> "${greeting}dein Habit **'${highMomentum.first.name}'** hat eine fantastische Stärke von **${highMomentum.second}%** erreicht! 💎"
                    "ka" -> "${greeting}შენმა ჩვევამ **'${highMomentum.first.name}'** მიაღწია სიძლიერეს **${highMomentum.second}%**! 💎"
                    "zh" -> "${greeting}你的习惯 **“${highMomentum.first.name}”** 稳固度达到了 **${highMomentum.second}%**！💎"
                    "fr" -> "${greeting}ton habitude **'${highMomentum.first.name}'** a atteint une force remarquable de **${highMomentum.second}%** ! 💎"
                    else -> "${greeting}your habit **'${highMomentum.first.name}'** has reached a fantastic strength of **${highMomentum.second}%**! 💎"
                }
                list.add(SmartInsight("HIGH_MOMENTUM", text, text.replace("**", ""), SmartInsightIconType.SPARKLE))
            }
        }

        // 9. Least active weekday (Constructive)
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "LEAST_ACTIVE_DAY", todayDateString)) {
            val weekdayCompletionsMap = IntArray(8)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            allLogs.forEach { log ->
                if (log.value > 0f && !log.isPaused) {
                    try {
                        val dateVal = sdf.parse(log.date)
                        if (dateVal != null) {
                            val calDay = Calendar.getInstance()
                            calDay.time = dateVal
                            val dayOfWeek = calDay.get(Calendar.DAY_OF_WEEK)
                            if (dayOfWeek in 1..7) {
                                weekdayCompletionsMap[dayOfWeek]++
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            var minDayIndex = -1
            var minCount = Int.MAX_VALUE
            var maxCount = 0
            for (i in 1..7) {
                if (weekdayCompletionsMap[i] > maxCount) maxCount = weekdayCompletionsMap[i]
                if (weekdayCompletionsMap[i] < minCount) {
                    minCount = weekdayCompletionsMap[i]
                    minDayIndex = i
                }
            }
            if (minDayIndex != -1 && maxCount >= 4) {
                val dayName = when (minDayIndex) {
                    Calendar.MONDAY -> tr(language, "Montag", "ორშაბათი", "周一", "Monday", "Lundi")
                    Calendar.TUESDAY -> tr(language, "Dienstag", "სამშაბათი", "周二", "Tuesday", "Mardi")
                    Calendar.WEDNESDAY -> tr(language, "Mittwoch", "ოთხშაბათი", "周三", "Wednesday", "Mercredi")
                    Calendar.THURSDAY -> tr(language, "Donnerstag", "ხუთშაბათი", "周四", "Thursday", "Jeudi")
                    Calendar.FRIDAY -> tr(language, "Freitag", "პარასკევი", "周五", "Friday", "Vendredi")
                    Calendar.SATURDAY -> tr(language, "Samstag", "შაბათი", "周六", "Saturday", "Samedi")
                    Calendar.SUNDAY -> tr(language, "Sonntag", "კვირა", "周日", "Sunday", "Dimanche")
                    else -> ""
                }
                if (dayName.isNotEmpty()) {
                    val text = when (language) {
                        "de" -> "${greeting}am **$dayName** fällt dir das Abhaken noch am schwersten ($minCount Abschlüsse). Ein kleiner Schritt reicht schon aus, um deinen Rhythmus zu stärken! 💡"
                        "ka" -> "${greeting}დღეს **$dayName**-ს ჩვევების შესრულება შედარებით გიჭირთ ($minCount შესრულება). პატარა ნაბიჯიც კი დაგეხმარებათ! 💡"
                        "zh" -> "${greeting}在 **$dayName** 你的打卡相对较少（$minCount 次）。迈出一小步就能找回良好节奏！💡"
                        "fr" -> "${greeting}le **$dayName**, la régularité est un peu plus difficile ($minCount réussites). Un petit pas aujourd'hui suffit pour relancer l'élan ! 💡"
                        else -> "${greeting}on **$dayName** you find it a bit harder to complete habits ($minCount completions). A small step today can help boost your momentum! 💡"
                    }
                    list.add(SmartInsight("LEAST_ACTIVE_DAY", text, text.replace("**", ""), SmartInsightIconType.LIGHTBULB))
                }
            }
        }

        // 10. Lowest performing habit (Constructive)
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "LOWEST_PERFORMER", todayDateString)) {
            val activeHabits = habitsWithStrength.filter { !it.first.isArchived }
            if (activeHabits.size >= 2) {
                val lowestHabit = activeHabits.minByOrNull { it.second }
                if (lowestHabit != null && lowestHabit.second < 50) {
                    val text = when (language) {
                        "de" -> "${greeting}dein Habit **'${lowestHabit.first.name}'** könnte etwas zusätzliche Aufmerksamkeit gebrauchen (Stärke: **${lowestHabit.second}%**). Wie wäre es heute mit einem kleinen Neustart? 🎯"
                        "ka" -> "${greeting}თქვენს ჩვევას **'${lowestHabit.first.name}'** ცოტა მეტი ყურადღება სჭირდება (სიძლიერე: **${lowestHabit.second}%**). მზად ხართ დღეს დასაბრუნებლად? 🎯"
                        "zh" -> "${greeting}你的习惯 **“${lowestHabit.first.name}”** 可能需要更多关注（稳固度：**${lowestHabit.second}%**）。今天来一次全新的开始？🎯"
                        "fr" -> "${greeting}ton habitude **'${lowestHabit.first.name}'** pourrait bénéficier d'un peu d'attention (force : **${lowestHabit.second}%**). Prêt pour un nouveau départ aujourd'hui ? 🎯"
                        else -> "${greeting}your habit **'${lowestHabit.first.name}'** could use a little extra attention (strength: **${lowestHabit.second}%**). How about a fresh start today? 🎯"
                    }
                    list.add(SmartInsight("LOWEST_PERFORMER", text, text.replace("**", ""), SmartInsightIconType.TRENDING_DOWN))
                }
            }
        }

        // 11. Month-over-Month comparison
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "MONTH_OVER_MONTH", todayDateString)) {
            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH)
            val currentYear = now.get(Calendar.YEAR)
            
            val prevCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }
            val prevMonthNum = prevCal.get(Calendar.MONTH)
            val prevYearNum = prevCal.get(Calendar.YEAR)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            var currMonthCount = 0
            var prevMonthCount = 0
            
            allLogs.forEach { log ->
                if (log.value > 0f && !log.isPaused) {
                    try {
                        val dateVal = sdf.parse(log.date)
                        if (dateVal != null) {
                            val logCal = Calendar.getInstance()
                            logCal.time = dateVal
                            if (logCal.get(Calendar.YEAR) == currentYear && logCal.get(Calendar.MONTH) == currentMonth) {
                                currMonthCount++
                            } else if (logCal.get(Calendar.YEAR) == prevYearNum && logCal.get(Calendar.MONTH) == prevMonthNum) {
                                prevMonthCount++
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            
            if (prevMonthCount > 0 || currMonthCount > 0) {
                val text = when (language) {
                    "de" -> if (currMonthCount >= prevMonthCount) "${greeting}diesen Monat hast du bereits **$currMonthCount** Abschlüsse geschafft (im Vormonat waren es **$prevMonthCount**). Du bist auf einem richtig guten Weg! 📈" else "${greeting}diesen Monat stehst du aktuell bei **$currMonthCount** Abschlüssen im Vergleich zu **$prevMonthCount** im Vormonat. Noch ist genug Zeit, um aufzuholen! 💪"
                    "ka" -> if (currMonthCount >= prevMonthCount) "${greeting}ამ თვეში უკვე **$currMonthCount** შესრულება გაქვთ (გასულ თვეს: **$prevMonthCount**). შესანიშნავი პროგრესია! 📈" else "${greeting}ამ თვეში გაქვთ **$currMonthCount** შესრულება (გასულ თვეს: **$prevMonthCount**). კიდევ გაქვთ დრო წინსვლისთვის! 💪"
                    "zh" -> if (currMonthCount >= prevMonthCount) "${greeting}本月你已达成了 **$currMonthCount** 次打卡（上月同期：**$prevMonthCount** 次）。趋势非常向好！📈" else "${greeting}本月你目前有 **$currMonthCount** 次打卡（上月同期：**$prevMonthCount** 次）。还有充足时间迎头赶上！💪"
                    "fr" -> if (currMonthCount >= prevMonthCount) "${greeting}ce mois-ci tu as déjà cumulé **$currMonthCount** réussites (contre **$prevMonthCount** le mois précédent). Tu es sur une excellente lancée ! 📈" else "${greeting}ce mois-ci tu es actuellement à **$currMonthCount** réussites (contre **$prevMonthCount** le mois précédent). Il reste encore du temps pour progresser ! 💪"
                    else -> if (currMonthCount >= prevMonthCount) "${greeting}you've achieved **$currMonthCount** completions this month compared to **$prevMonthCount** last month. You're on a great trajectory! 📈" else "${greeting}you currently have **$currMonthCount** completions this month compared to **$prevMonthCount** last month. Plenty of time to catch up! 💪"
                }
                list.add(SmartInsight("MONTH_OVER_MONTH", text, text.replace("**", ""), SmartInsightIconType.DATE_RANGE))
            }
        }

        // 12. Week-over-Week comparison
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "WEEK_OVER_WEEK", todayDateString)) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDate = try { sdf.parse(todayDateString) } catch (e: Exception) { null }
            
            val (thisWeekCount, prevWeekCount) = if (todayDate != null) {
                var tw = 0
                var pw = 0
                allLogs.forEach { log ->
                    if (log.value > 0f && !log.isPaused) {
                        val d = try { sdf.parse(log.date) } catch (e: Exception) { null }
                        if (d != null) {
                            val diffDays = ((todayDate.time - d.time) / (1000L * 60 * 60 * 24)).toInt()
                            if (diffDays in 0..6) tw++
                            else if (diffDays in 7..13) pw++
                        }
                    }
                }
                Pair(tw, pw)
            } else Pair(0, 0)
            
            if (thisWeekCount > 0 || prevWeekCount > 0) {
                val text = when (language) {
                    "de" -> "${greeting}in den letzten 7 Tagen hast du **$thisWeekCount** Abschlüsse verzeichnet, verglichen mit **$prevWeekCount** in der Vorwoche. ${if (thisWeekCount >= prevWeekCount) "Klasse Steigerung! 🚀" else "Bleib weiter am Ball! ⚡"}"
                    "ka" -> "${greeting}ბოლო 7 დღეში გაქვთ **$thisWeekCount** შესრულება, წინა კვირის **$prevWeekCount**-თან შედარებით. ${if (thisWeekCount >= prevWeekCount) "შესანიშნავი ზრდაა! 🚀" else "არ გაჩერდეთ! ⚡"}"
                    "zh" -> "${greeting}最近 7 天内你记录了 **$thisWeekCount** 次完成（上周：**$prevWeekCount** 次）。${if (thisWeekCount >= prevWeekCount) "出色的进步！🚀" else "继续保持势头！⚡"}"
                    "fr" -> "${greeting}au cours des 7 derniers jours tu as enregistré **$thisWeekCount** réussites, contre **$prevWeekCount** la semaine précédente. ${if (thisWeekCount >= prevWeekCount) "Belle progression ! 🚀" else "Garde le cap ! ⚡"}"
                    else -> "${greeting}in the last 7 days you logged **$thisWeekCount** completions, compared to **$prevWeekCount** the previous week. ${if (thisWeekCount >= prevWeekCount) "Great increase! 🚀" else "Keep staying on track! ⚡"}"
                }
                list.add(SmartInsight("WEEK_OVER_WEEK", text, text.replace("**", ""), SmartInsightIconType.CHART))
            }
        }

        // 13. Habit-specific improvement
        if (!checkCooldowns || !isInsightTypeInCooldown(context, "HABIT_IMPROVEMENT", todayDateString)) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDate = try { sdf.parse(todayDateString) } catch (e: Exception) { null }
            
            var bestHabit: Habit? = null
            var maxDiff = 0
            var bestThisWeek = 0
            var bestLastWeek = 0
            
            if (todayDate != null) {
                allHabits.filter { !it.isArchived }.forEach { habit ->
                    var thisWeek = 0
                    var lastWeek = 0
                    allLogs.forEach { log ->
                        if (log.habitId == habit.id && log.value > 0f && !log.isPaused) {
                            val d = try { sdf.parse(log.date) } catch (e: Exception) { null }
                            if (d != null) {
                                val diffDays = ((todayDate.time - d.time) / (1000L * 60 * 60 * 24)).toInt()
                                if (diffDays in 0..6) thisWeek++
                                else if (diffDays in 7..13) lastWeek++
                            }
                        }
                    }
                    val diff = thisWeek - lastWeek
                    if (diff > maxDiff && thisWeek >= 2) {
                        maxDiff = diff
                        bestHabit = habit
                        bestThisWeek = thisWeek
                        bestLastWeek = lastWeek
                    }
                }
            }
            
            if (bestHabit != null) {
                val habitName = bestHabit.name
                val text = when (language) {
                    "de" -> "${greeting}dein Habit **'$habitName'** läuft diese Woche spürbar besser als in der Vorwoche (**$bestThisWeek** vs. **$bestLastWeek** Abschlüsse)! 🎉"
                    "ka" -> "${greeting}თქვენი ჩვევა **'$habitName'** ამ კვირაში უკეთესად მიდის, ვიდრე გასულ კვირას (**$bestThisWeek** vs **$bestLastWeek** შესრულება)! 🎉"
                    "zh" -> "${greeting}你的习惯 **“$habitName”** 本周表现明显优于上周（**$bestThisWeek** 对比 **$bestLastWeek** 次打卡）！🎉"
                    "fr" -> "${greeting}ton habitude **'$habitName'** progresse nettement cette semaine par rapport à la précédente (**$bestThisWeek** contre **$bestLastWeek** réalisations) ! 🎉"
                    else -> "${greeting}your habit **'$habitName'** is going noticeably better this week than last week (**$bestThisWeek** vs **$bestLastWeek** completions)! 🎉"
                }
                list.add(SmartInsight("HABIT_IMPROVEMENT", text, text.replace("**", ""), SmartInsightIconType.ARROW_UP))
            }
        }

        return list
    }

    fun selectBestInsight(insights: List<SmartInsight>, todayDateString: String): SmartInsight? {
        if (insights.isEmpty()) return null
        return insights.find { it.type == "REVIEW_PROMPT" }
            ?: insights[Math.abs(todayDateString.hashCode()) % insights.size]
    }

    private fun tr(language: String, de: String, ka: String, zh: String, en: String, fr: String): String {
        return when (language) {
            "de" -> de
            "ka" -> ka
            "zh" -> zh
            "fr" -> fr
            else -> en
        }
    }
}
