package com.example.ui.screens.profile

import com.example.ui.components.Achievement
import com.example.tr

object AchievementExplanations {
    fun habitStreaksText(language: String): String {
        return if (language == "de") {
            "Gewohnheiten-Serien belohnen dich für aufeinanderfolgende Tage, an denen du eine bestimmte Gewohnheit erfolgreich abgeschlossen hast!\n\n" +
                    "Für jede einzelne Gewohnheit erhältst du Abzeichen basierend auf deiner längsten Serie aller Zeiten:\n\n" +
                    "• 🥉 Bronze-Serie: 14 Tage Serie\n" +
                    "• 🥈 Silber-Serie: 30 Tage Serie\n" +
                    "• 🥇 Gold-Serie: 100 Tage Serie\n\n" +
                    "Tippe auf die Abzeichen in der jeweiligen Gewohnheitskarte, um weitere Details zu sehen!"
        } else if (language == "ka") {
            "ჩვევების სერიები დაგაჯილდოებთ ზედიზედ დღეებისთვის, როდესაც წარმატებით დაასრულეთ კონკრეტული ჩვევა!\n\n" +
                    "თითოეული ინდივიდუალური ჩვევისთვის, თქვენ იღებთ სამკერდე ნიშნებს თქვენი ყველაზე გრძელი სერიის მიხედვით:\n\n" +
                    "• 🥉 ბრინჯაოს სერია: 14 დღიანი სერია\n" +
                    "• 🥈 ვერცხლის სერია: 30 დღიანი სერია\n" +
                    "• 🥇 ოქროს სერია: 100 დღიანი სერია\n\n" +
                    "შეეხეთ სამკერდე ნიშნებს თითოეული ჩვევის ბარათში დამატებითი დეტალების სანახავად!"
        } else {
            "Habit Streaks reward you for consecutive days where you successfully completed a specific habit!\n\n" +
                    "For each individual habit, you earn badges based on your longest streak of all time:\n\n" +
                    "• 🥉 Bronze Streak: 14 days streak\n" +
                    "• 🥈 Silver Streak: 30 days streak\n" +
                    "• 🥇 Gold Streak: 100 days streak\n\n" +
                    "Tap on the badges in each habit card to see more details!"
        }
    }

    fun completionsText(language: String): String {
        return if (language == "de") {
            "Gesamt-Abschlüsse zählen, wie oft du deine Gewohnheitsziele insgesamt erfolgreich erreicht hast!\n\n" +
                    "Hierbei werden all deine absolvierten Ziele über alle Gewohnheiten hinweg addiert. Erreiche folgende Meilensteine:\n\n" +
                    "• 🌱 Erster Schritt: 10 Abschlüsse insgesamt\n" +
                    "• 🔄 Gewohnheits-Routine: 50 Abschlüsse insgesamt\n" +
                    "• 🧠 Eiserner Wille: 200 Abschlüsse insgesamt\n" +
                    "• 🏆 Lebensstil-Transformation: 500 Abschlüsse insgesamt"
        } else if (language == "ka") {
            "მთლიანი დასრულებები ითვლის რამდენჯერ დაასრულეთ ჩვევების მიზნები!\n\n" +
                    "ყველა თქვენი დასრულებული მიზანი ყველა ჩვევაში ემატება ერთად. მიაღწიეთ ამ ეტაპებს:\n\n" +
                    "• 🌱 პირველი ნაბიჯი: სულ 10 დასრულება\n" +
                    "• 🔄 ჩვევა: სულ 50 დასრულება\n" +
                    "• 🧠 რკინის ნება: სულ 200 დასრულება\n" +
                    "• 🏆 ცხოვრების წესის ტრანსფორმაცია: ჯამში 500 დასრულება"
        } else {
            "Total Completions count how many times you completed habit goals in total!\n\n" +
                    "All your completed goals across all habits are added together. Achieve these milestones:\n\n" +
                    "• 🌱 First Step: 10 completions in total\n" +
                    "• 🔄 Habit Routine: 50 completions in total\n" +
                    "• 🧠 Iron Will: 200 completions in total\n" +
                    "• 🏆 Lifestyle Transformation: 500 completions in total"
        }
    }

    fun perfectDaysText(language: String): String {
        return tr(
            language,
            "Perfekte Tage belohnen Tage, an denen du deine Disziplin zu 100% gehalten hast!\n\n" +
                    "Ein perfekter Tag ist ein Tag, an dem du alle deine für diesen Tag geplanten bzw. aktiven Gewohnheiten vollständig erledigt hast. Wenn du diese perfekten Tage hintereinander schaffst, erreichst du:\n\n" +
                    "• 📅 Perfekte Woche: 7 perfekte Tage am Stück\n" +
                    "• 🗓️ Perfekter Monat: 30 perfekte Tage am Stück\n" +
                    "• 💯 100 perfekte Tage: 100 perfekte Tage am Stück\n" +
                    "• 📆 Perfektes Jahr: 365 perfekte Tage am Stück\n" +
                    "• 👑 Perfektion: 1000 perfekte Tage am Stück",
            "Perfect Days აჯილდოვებს ზედიზედ დღეებს, სადაც თქვენ შეინარჩუნეთ 100%-იანი დისციპლინა!\n\nსრულყოფილი დღე არის დღე, როდესაც წარმატებით ასრულებთ თქვენს ყველა დაგეგმილ/აქტიურ ჩვევას. სრულყოფილი დღეების ერთად შეკრებით თქვენ მიაღწევთ:\n\n• 📅 იდეალური კვირა: ზედიზედ 7 სრულყოფილი დღე\n• 🗓️ იდეალური თვე: ზედიზედ 30 სრულყოფილი დღე\n• 💯 100 იდეალური დღე: ზედიზედ 100 სრულყოფილი დღე\n• 📆 იდეალური წელი: ზედიზედ 365 სრულყოფილი დღე\n• 👑 სრულყოფილება: ზედიზედ 1000 სრულყოფილი დღე",
            "完美日奖励连续保持 100% 纪律的天数！\n\n完美日是指您成功完成所有计划/活动习惯的一天。通过连续的完美天数，您将获得：\n\n• 📅 完美周：连续 7 个完美天\n• 🗓️ 完美月：连续 30 个完美天\n• 💯 100完美天：连续 100 个完美天\n• 📆 完美年：连续 365 个完美天\n• 👑 完美极致：连续 1000 个完美天",
            "Perfect Days reward consecutive days where you maintained 100% discipline!\n\nA perfect day is a day where you successfully complete all of your scheduled/active habits. By stringing perfect days together, you achieve:\n\n• 📅 Perfect Week: 7 consecutive perfect days\n• 🗓️ Perfect Month: 30 consecutive perfect days\n• 💯 100 Perfect Days: 100 consecutive perfect days\n• 📆 Perfect Year: 365 consecutive perfect days\n• 👑 Perfection: 1000 consecutive perfect days"
        )
    }

    fun getCompletionsAchievements(totalGlobalCompletions: Int, language: String): List<Achievement> {
        return listOf(
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_10",
                title = tr(language, "Erster Schritt", "პირველი ნაბიჯი", "第一步", "First Step"),
                description = tr(language, "Trage insgesamt 10 Erledigungen ein.", "დაარეგისტრირეთ სულ 10 დასრულება ყველა ჩვევაში.", "记录累计 10 次完成。", "Log a total of 10 completions across all habits."),
                targetValue = 10,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 10
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_50",
                title = tr(language, "Gewohnheits-Routine", "ჩვევების რუტინა", "习惯养成", "Habit Routine"),
                description = tr(language, "Trage insgesamt 50 Erledigungen ein.", "დაარეგისტრირეთ სულ 50 დასრულება ყველა ჩვევაში.", "记录累计 50 次完成。", "Log a total of 50 completions across all habits."),
                targetValue = 50,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 50
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_200",
                title = tr(language, "Eiserner Wille", "რკინის ნება", "钢铁意志", "Iron Will"),
                description = tr(language, "Trage insgesamt 200 Erledigungen ein.", "დაარეგისტრირეთ სულ 200 დასრულება ყველა ჩვევაში.", "记录累计 200 次完成。", "Log a total of 200 completions across all habits."),
                targetValue = 200,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 200
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_500",
                title = tr(language, "Lebensstil-Transformation", "ცხოვრების წესის ტრანსფორმაცია", "生活蜕变", "Lifestyle Transformation"),
                description = tr(language, "Trage insgesamt 500 Erledigungen ein.", "დაარეგისტრირეთ სულ 500 დასრულება ყველა ჩვევაში.", "记录累计 500 次完成。", "Log a total of 500 completions across all habits."),
                targetValue = 500,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 500
            ),
            Achievement(
                type = "COMPLETIONS",
                tier = "COMP_1000",
                title = tr(language, "Meister der Beständigkeit", "მიმდევრულობის ოსტატი", "坚持大师", "Master of Consistency"),
                description = tr(language, "Trage insgesamt 1000 Erledigungen ein.", "დაარეგისტრირეთ სულ 1000 დასრულება ყველა ჩვევაში.", "记录累计 1000 次完成。", "Log a total of 1000 completions across all habits."),
                targetValue = 1000,
                currentValue = totalGlobalCompletions,
                isUnlocked = totalGlobalCompletions >= 1000
            )
        )
    }

    fun getPerfectDaysAchievements(perfectDaysStreak: Int, currentPerfectStreak: Int, language: String): List<Achievement> {
        return listOf(
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_7",
                title = tr(language, "Perfekte Woche", "იდეალური კვირა", "完美周", "Perfect Week"),
                description = tr(language, "Erreiche eine Serie von 7 perfekten Tagen am Stück.", "მიაღწიეთ ზედიზედ 7 სრულყოფილი დღის სერიას.", "连续达成 7 个完美天。", "Achieve a streak of 7 consecutive perfect days."),
                targetValue = 7,
                currentValue = if (perfectDaysStreak >= 7) 7 else currentPerfectStreak,
                isUnlocked = perfectDaysStreak >= 7
            ),
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_30",
                title = tr(language, "Perfekter Monat", "იდეალური თვე", "完美月", "Perfect Month"),
                description = tr(language, "Erreiche eine Serie von 30 perfekten Tagen am Stück.", "მიაღწიეთ ზედიზედ 30 სრულყოფილი დღის სერიას.", "连续达成 30 个完美天。", "Achieve a streak of 30 consecutive perfect days."),
                targetValue = 30,
                currentValue = if (perfectDaysStreak >= 30) 30 else currentPerfectStreak,
                isUnlocked = perfectDaysStreak >= 30
            ),
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_100",
                title = tr(language, "100 perfekte Tage", "100 იდეალური დღე", "100完美天", "100 Perfect Days"),
                description = tr(language, "Erreiche eine Serie von 100 perfekten Tagen am Stück.", "მიაღწიეთ ზედიზედ 100 სრულყოფილი დღის სერიას.", "连续达成 100 个完美天。", "Achieve a streak of 100 consecutive perfect days."),
                targetValue = 100,
                currentValue = if (perfectDaysStreak >= 100) 100 else currentPerfectStreak,
                isUnlocked = perfectDaysStreak >= 100
            ),
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_365",
                title = tr(language, "Perfektes Jahr", "იდეალური წელი", "完美年", "Perfect Year"),
                description = tr(language, "Erreiche eine Serie von 365 perfekten Tagen am Stück.", "მიაღწიეთ ზედიზედ 365 სრულყოფილი დღის სერიებს.", "连续达成 365 个完美天。", "Achieve a streak of 365 consecutive perfect days."),
                targetValue = 365,
                currentValue = if (perfectDaysStreak >= 365) 365 else currentPerfectStreak,
                isUnlocked = perfectDaysStreak >= 365
            ),
            Achievement(
                type = "PERFECT_DAYS",
                tier = "PERF_1000",
                title = tr(language, "Perfektion", "სრულყოფილება", "完美极致", "Perfection"),
                description = tr(language, "Erreiche eine Serie von 1000 perfekten Tagen am Stück.", "მიაღწიეთ ზედიზედ 1000 სრულყოფილი დღის სერიებს.", "连续达成 1000 个完美天。", "Achieve a streak of 1000 consecutive perfect days."),
                targetValue = 1000,
                currentValue = if (perfectDaysStreak >= 1000) 1000 else currentPerfectStreak,
                isUnlocked = perfectDaysStreak >= 1000
            )
        )
    }
}
