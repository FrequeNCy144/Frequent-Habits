package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.tr

fun getEncouragementText(completed: Int, total: Int, language: String): String {
    if (total == 0) {
        val german = listOf(
            "Heute ist ein wunderbarer Tag, um eine neue Gewohnheit zu starten! 🌟",
            "Jeder Tag ist eine neue Chance, über dich hinauszuwachsen.",
            "Fang heute an und danke dir selbst morgen.",
            "Der beste Zeitpunkt zu starten ist genau jetzt.",
            "Kleine Schritte führen zu großen Veränderungen. Erstelle deine erste Gewohnheit!",
            "Träume nicht dein Leben, sondern gestalte deine Routinen.",
            "Ein neues Kapitel beginnt mit einer einzigen Entscheidung.",
            "Deine Zukunft wird durch deine heutigen Gewohnheiten geformt."
        )
        val english = listOf(
            "Today is a wonderful day to start a new habit! 🌟",
            "Every day is a new chance to grow beyond yourself.",
            "Start today and thank yourself tomorrow.",
            "The best time to start is right now.",
            "Small steps lead to big changes. Create your first habit!",
            "Don't dream your life, design your routines.",
            "A new chapter starts with a single decision.",
            "Your future is shaped by your habits today."
        )
        val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
        return if (language == "de") german[index] else english[index]
    }
    
    val fraction = completed.toFloat() / total
    return when {
        fraction == 0f -> {
            val german = listOf(
                "Fang klein an – heute ist der perfekte Tag für den ersten Schritt! 🚀",
                "Der erste Schritt ist immer der schwerste. Du schaffst das!",
                "Ein kleiner Schritt heute ist der Anfang eines großen Weges.",
                "Zögere nicht – mach einfach den ersten kleinen Haken für heute.",
                "Motivation bringt dich in Gang. Gewohnheit hält dich am Laufen.",
                "Die geheime Zutat des Erfolgs ist das Anfangen.",
                "Jeder Weg von tausend Meilen beginnt mit einem einzigen Schritt.",
                "Du musst nicht perfekt sein, du musst nur anfangen."
            )
            val english = listOf(
                "Start small - today is the perfect day for the first step! 🚀",
                "The first step is always the hardest. You can do this!",
                "A small step today is the beginning of a great journey.",
                "Don't hesitate - just check off that first tiny step for today.",
                "Motivation is what gets you started. Habit is what keeps you going.",
                "The secret of getting ahead is getting started.",
                "The journey of a thousand miles begins with a single step.",
                "You don't have to be perfect, you just have to start."
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
        fraction < 0.5f -> {
            val german = listOf(
                "Schritt für Schritt vorwärts! 🚀 Jeder Fortschritt zählt.",
                "Du bist gestartet! Bleib dran, es lohnt sich.",
                "Jede erledigte Gewohnheit bringt dich deinem Ziel näher.",
                "Konsequenz ist der Schlüssel zum Erfolg. Mach weiter so!",
                "Kleine Erfolge summieren sich. Jeder Haken ist ein Sieg!",
                "Du bist auf dem Weg. Lass dich nicht aufhalten.",
                "Besser 1% Fortschritt als 0%. Dranbleiben!",
                "Dein zukünftiges Ich klatscht bereits Beifall."
            )
            val english = listOf(
                "Step by step forward! 🚀 Every progress counts.",
                "You have started! Keep going, it's worth it.",
                "Every completed habit brings you closer to your goal.",
                "Consistency is the key to success. Keep it up!",
                "Small wins add up. Every checkmark is a victory!",
                "You are on your way. Don't let anything stop you.",
                "Better 1% progress than 0%. Keep pushing!",
                "Your future self is already applauding."
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
        fraction < 1.0f -> {
            val german = listOf(
                "Du bist auf einem fantastischen Weg! Fast geschafft für heute! 💪",
                "Großartige Arbeit! Nur noch ein kleines Stück.",
                "Spürst du die Energie? Du machst das hervorragend heute!",
                "Du ziehst es heute wirklich durch. Dranbleiben für den perfekten Tag!",
                "Die Ziellinie ist in Sicht. Gib jetzt noch mal alles!",
                "Unglaubliche Disziplin heute! Mach den Tag perfekt.",
                "Du bist fast am Ziel. Lass uns den Tag gemeinsam krönen!",
                "Deine Routine wird immer stäker. Fast vollendet!"
            )
            val english = listOf(
                "You are on a fantastic path! Almost done for today! 💪",
                "Great work! Just a little bit more.",
                "Do you feel the energy? You are doing outstandingly today!",
                "You are really pulling through today. Keep going for a perfect day!",
                "The finish line is in sight. Give it your all!",
                "Incredible discipline today! Make today perfect.",
                "You are almost there. Let's crown the day together!",
                "Your routine is growing stronger. Almost complete!"
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
        else -> {
            val german = listOf(
                "Tag perfekt gemeistert! ✨ Du bist unaufhaltsam!",
                "Alle Gewohnheiten erledigt! Du kannst stolz auf dich sein. 🎉",
                "100% geschafft! Ein perfekter Tag für deine persönliche Entwicklung.",
                "Hervorragend! Du hast heute alles gegeben und gewonnen!",
                "Du hast die Messlatte hoch gelegt. Atemberaubende Leistung! 🏆",
                "Perfekter Tag! Deine Disziplin ist deine Superkraft.",
                "Alles abgehakt! Genieße deinen wohlverdienten Feierabend.",
                "Meisterhaft! Du beweist dir selbst jeden Tag, was in dir steckt."
            )
            val english = listOf(
                "Day crushed! ✨ You are unstoppable!",
                "All habits completed! You can be proud of yourself. 🎉",
                "100% completed! A perfect day for your personal growth.",
                "Outstanding! You gave it your all today and won!",
                "You've set the bar high. Breathtaking performance! 🏆",
                "Perfect day! Your discipline is your superpower.",
                "Everything checked! Enjoy your well-deserved evening.",
                "Masterful! You prove to yourself every day what you are capable of."
            )
            val index = (completed + total + (System.currentTimeMillis() / 86400000L).toInt()) % german.size
            if (language == "de") german[index] else english[index]
        }
    }
}

fun getStrengthLabel(strength: Int, language: String): String {
    return when {
        strength == 0 -> tr(language, "Ausstehend - Auf geht's!", "მომლოდინე - წავიდეთ!", "未完成 - 开始行动！", "Pending - Let's go!")
        strength < 30 -> tr(language, "Anfang gemacht - Weiter so! 🌱", "დაიწყო - გააგრძელე! 🌱", "已起步 - 继续保持！🌱", "Started - Keep going! 🌱")
        strength < 60 -> tr(language, "Mittelmäßig - Dranbleiben! ✨", "სამართლიანი - ასე გააგრძელე! ✨", "良好 - 继续加油！✨", "Fair - Keep it up! ✨")
        strength < 85 -> tr(language, "Solide - Starker Einsatz! 💪", "მყარი - ძლიერად მიდის! 💪", "扎实 - 势头强劲！💪", "Solid - Going strong! 💪")
        else -> tr(language, "Exzellent - Unaufhaltsam! 🔥", "შესანიშნავი - შეუჩერებელი! 🔥", "卓越 - 势不可挡！🔥", "Excellent - Unstoppable! 🔥")
    }
}

data class Achievement(
    val type: String, // "STREAK", "COMPLETIONS", "PERFECT_DAYS"
    val tier: String, // e.g. "WOOD", "COMP_10", "PERF_7" etc.
    val title: String,
    val description: String,
    val habitName: String? = null,
    val habitColor: Color? = null,
    val targetValue: Int,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false
)
