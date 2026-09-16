package com.example.data

object AchievementEvaluator {
    fun calculateUnlockedAchievementsList(
        stats: com.example.data.ProfileStats,
        lang: String,
        rewards: List<MilestoneReward> = emptyList()
    ): List<com.example.data.UnlockedAchievementInfo> {
        val list = mutableListOf<com.example.data.UnlockedAchievementInfo>()

        // Helper to find reward text
        fun getRewardText(trophyId: String, habitId: Int? = null): String? {
            return rewards.find { it.conditionType == "TROPHY_COUPLED" && it.trophyId == trophyId && (habitId == null || it.habitId == habitId) }?.rewardText
        }

        // Global Completions
        val totalGlobalCompletions = stats.totalGlobalCompletions
        if (totalGlobalCompletions >= 10) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "COMP_10",
                type = "COMPLETIONS",
                tier = "COMP_10",
                title = if (lang == "de") "Erster Schritt" else if (lang == "ka") "პირველი ნაბიჯი" else if (lang == "zh") "第一步" else "First Step",
                description = if (lang == "de") "Trage insgesamt 10 Erledigungen ein." else if (lang == "ka") "ჩაწერეთ სულ 10 შესრულება." else if (lang == "zh") "记录累计 10 次完成。" else "Log a total of 10 completions across all habits."
            ))
        }
        if (totalGlobalCompletions >= 50) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "COMP_50",
                type = "COMPLETIONS",
                tier = "COMP_50",
                title = if (lang == "de") "Gewohnheits-Routine" else if (lang == "ka") "ჩვევის რუტინა" else if (lang == "zh") "习惯养成" else "Habit Routine",
                description = if (lang == "de") "Trage insgesamt 50 Erledigungen ein." else if (lang == "ka") "ჩაწერეთ სულ 50 შესრულება." else if (lang == "zh") "记录累计 50 次完成。" else "Log a total of 50 completions across all habits."
            ))
        }
        if (totalGlobalCompletions >= 200) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "COMP_200",
                type = "COMPLETIONS",
                tier = "COMP_200",
                title = if (lang == "de") "Eiserner Wille" else if (lang == "ka") "რკინის ნებისყოფა" else if (lang == "zh") "钢铁意志" else "Iron Will",
                description = if (lang == "de") "Trage insgesamt 200 Erledigungen ein." else if (lang == "ka") "ჩაწერეთ სულ 200 შესრულება." else if (lang == "zh") "记录累计 200 次完成。" else "Log a total of 200 completions across all habits."
            ))
        }
        if (totalGlobalCompletions >= 500) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "COMP_500",
                type = "COMPLETIONS",
                tier = "COMP_500",
                title = if (lang == "de") "Lebensstil-Transformation" else if (lang == "ka") "ცხოვრების ტრანსფორმაცია" else if (lang == "zh") "生活蜕变" else "Lifestyle Transformation",
                description = if (lang == "de") "Trage insgesamt 500 Erledigungen ein." else if (lang == "ka") "ჩაწერეთ სულ 500 შესრულება." else if (lang == "zh") "记录累计 500 次完成。" else "Log a total of 500 completions across all habits."
            ))
        }

        // Perfect Days
        val perfectDaysStreak = stats.perfectDaysStreak
        if (perfectDaysStreak >= 7) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "PERF_7",
                type = "PERFECT_DAYS",
                tier = "PERF_7",
                title = if (lang == "de") "Perfekte Woche" else if (lang == "ka") "სრულყოფილი კვირა" else if (lang == "zh") "完美周" else "Perfect Week",
                description = if (lang == "de") "Erreiche eine Serie von 7 perfekten Tagen am Stück." else if (lang == "ka") "მიაღწიეთ 7 სრულყოფილი დღის სერიას." else if (lang == "zh") "连续达成 7 个完美天。" else "Achieve a streak of 7 consecutive perfect days."
            ))
        }
        if (perfectDaysStreak >= 30) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "PERF_30",
                type = "PERFECT_DAYS",
                tier = "PERF_30",
                title = if (lang == "de") "Perfekter Monat" else if (lang == "ka") "სრულყოფილი თვე" else if (lang == "zh") "完美月" else "Perfect Month",
                description = if (lang == "de") "Erreiche eine Serie von 30 perfekten Tagen am Stück." else if (lang == "ka") "მიაღწიეთ 30 სრულყოფილი დღის სერიას." else if (lang == "zh") "连续达成 30 个完美天。" else "Achieve a streak of 30 consecutive perfect days."
            ))
        }
        if (perfectDaysStreak >= 100) {
            list.add(com.example.data.UnlockedAchievementInfo(
                id = "PERF_100",
                type = "PERFECT_DAYS",
                tier = "PERF_100",
                title = if (lang == "de") "Perfektion" else if (lang == "ka") "სრულყოფილება" else if (lang == "zh") "完美极致" else "Perfection",
                description = if (lang == "de") "Erreiche eine Serie von 100 perfekten Tagen am Stück." else if (lang == "ka") "მიაღწიეთ 100 სრულყოფილი დღის სერიას." else if (lang == "zh") "连续达成 100 个完美天。" else "Achieve a streak of 100 consecutive perfect days."
            ))
        }

        // Individual Habit Streaks
        stats.habitStreaks.forEach { streakInfo ->
            val habit = streakInfo.habit
            val streak = streakInfo.longestStreak
            if (streak >= 7) {
                list.add(com.example.data.UnlockedAchievementInfo(
                    id = "STREAK_${habit.id}_7",
                    type = "STREAK",
                    tier = "WOOD",
                    title = if (lang == "de") "${habit.name}: Holz-Streak" else if (lang == "ka") "${habit.name}: ხის სერია" else if (lang == "zh") "${habit.name}：木质连续" else "${habit.name}: Wood Streak",
                    description = if (lang == "de") "7 Tage Serie erreicht!" else if (lang == "ka") "7 დღის სერია მიღწეულია!" else if (lang == "zh") "达成 7 天连续！" else "Reached a 7-day streak!",
                    habitName = habit.name,
                    habitColor = habit.color,
                    habitIcon = habit.icon
                ))
            }
            if (streak >= 14) {
                list.add(com.example.data.UnlockedAchievementInfo(
                    id = "STREAK_${habit.id}_14",
                    type = "STREAK",
                    tier = "BRONZE",
                    title = if (lang == "de") "${habit.name}: Bronze-Streak" else if (lang == "ka") "${habit.name}: ბრინჯაოს სერია" else if (lang == "zh") "${habit.name}：青铜连续" else "${habit.name}: Bronze Streak",
                    description = if (lang == "de") "14 Tage Serie erreicht!" else if (lang == "ka") "14 დღის სერია მიღწეულია!" else if (lang == "zh") "达成 14 天连续！" else "Reached a 14-day streak!",
                    habitName = habit.name,
                    habitColor = habit.color,
                    habitIcon = habit.icon
                ))
            }
            if (streak >= 30) {
                list.add(com.example.data.UnlockedAchievementInfo(
                    id = "STREAK_${habit.id}_30",
                    type = "STREAK",
                    tier = "SILVER",
                    title = if (lang == "de") "${habit.name}: Silber-Streak" else if (lang == "ka") "${habit.name}: ვერცხლის სერია" else if (lang == "zh") "${habit.name}：白银连续" else "${habit.name}: Silver Streak",
                    description = if (lang == "de") "30 Tage Serie erreicht!" else if (lang == "ka") "30 დღის სერია მიღწეულია!" else if (lang == "zh") "达成 30 天连续！" else "Reached a 30-day streak!",
                    habitName = habit.name,
                    habitColor = habit.color,
                    habitIcon = habit.icon
                ))
            }
            if (streak >= 100) {
                list.add(com.example.data.UnlockedAchievementInfo(
                    id = "STREAK_${habit.id}_100",
                    type = "STREAK",
                    tier = "GOLD",
                    title = if (lang == "de") "${habit.name}: Gold-Streak" else if (lang == "ka") "${habit.name}: ოქროს სერია" else if (lang == "zh") "${habit.name}：黄金连续" else "${habit.name}: Gold Streak",
                    description = if (lang == "de") "100 Tage Serie erreicht!" else if (lang == "ka") "100 დღის სერია მიღწეულია!" else if (lang == "zh") "达成 100 天连续！" else "Reached a 100-day streak!",
                    habitName = habit.name,
                    habitColor = habit.color,
                    habitIcon = habit.icon
                ))
            }
        }

        // Apply reward text and milestoneRewardId to standard achievements
        val finalList = list.map { ach ->
            val habitId = if (ach.id.startsWith("STREAK_")) ach.id.split("_")[1].toIntOrNull() else null
            val matchingReward = rewards.find { it.conditionType == "TROPHY_COUPLED" && it.trophyId == ach.tier && (habitId == null || it.habitId == habitId) }
            ach.copy(
                rewardText = matchingReward?.rewardText,
                rewardDescription = matchingReward?.description,
                milestoneRewardId = matchingReward?.id
            )
        }.toMutableList()

        // Add custom milestone rewards as virtual achievements
        val customMilestones = rewards.filter { it.conditionType == "STREAK" || it.conditionType == "COMPLETIONS" }
        customMilestones.forEach { reward ->
            val habitStat = stats.habitStreaks.find { it.habit.id == reward.habitId }
            if (habitStat != null) {
                val isReached = when (reward.conditionType) {
                    "STREAK" -> habitStat.longestStreak >= reward.conditionValue
                    "COMPLETIONS" -> habitStat.totalCompletions >= reward.conditionValue
                    else -> false
                }
                
                if (isReached) {
                    finalList.add(com.example.data.UnlockedAchievementInfo(
                        id = "CUSTOM_${reward.id}",
                        type = "CUSTOM_MILESTONE",
                        tier = "CUSTOM",
                        title = if (lang == "de") "${habitStat.habit.name}: Meilenstein erreicht" else if (lang == "ka") "${habitStat.habit.name}: ეტაპი მიღწეულია" else if (lang == "zh") "${habitStat.habit.name}：达成里程碑" else "${habitStat.habit.name}: Milestone Reached",
                        description = if (lang == "de") "Belohnung freigeschaltet!" else if (lang == "ka") "ჯილდო განბლოკილია!" else if (lang == "zh") "奖励已解锁！" else "Reward unlocked!",
                        habitName = habitStat.habit.name,
                        habitColor = habitStat.habit.color,
                        habitIcon = habitStat.habit.icon,
                        rewardText = reward.rewardText,
                        rewardDescription = reward.description,
                        milestoneRewardId = reward.id
                    ))
                }
            }
        }

        return finalList
    }
}
