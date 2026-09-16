package com.example

fun tr(lang: String, de: String, en: String): String {
    if (lang == "de") return de
    if (lang == "ka") {
        return GEORGIAN_TRANSLATIONS[en]
            ?: GEORGIAN_TRANSLATIONS[de]
            ?: translateDynamicGeorgian(en, de)
    }
    if (lang == "zh") {
        return CHINESE_TRANSLATIONS[en]
            ?: CHINESE_TRANSLATIONS[de]
            ?: translateDynamicChinese(en, de)
    }
    if (lang == "fr") {
        return FRENCH_TRANSLATIONS[en]
            ?: FRENCH_TRANSLATIONS[de]
            ?: translateDynamicFrench(en, de)
    }
    return en
}

fun tr(lang: String, de: String, ka: String, zh: String, en: String): String {
    return when (lang) {
        "de" -> de
        "ka" -> ka
        "zh" -> zh
        "fr" -> FRENCH_TRANSLATIONS[en] ?: FRENCH_TRANSLATIONS[de] ?: translateDynamicFrench(en, de)
        else -> en
    }
}

fun tr(lang: String, de: String, ka: String, zh: String, en: String, fr: String): String {
    return when (lang) {
        "de" -> de
        "ka" -> ka
        "zh" -> zh
        "fr" -> fr
        else -> en
    }
}

private fun translateDynamicFrench(en: String, de: String): String {
    var str = if (en.isNotBlank()) en else de
    if (str.isBlank()) return de
    str = str.replace("Habit updated!", "Habitude mise à jour !")
    str = str.replace("Habit added!", "Habitude ajoutée !")
    str = str.replace("Delete", "Supprimer")
    str = str.replace("Edit", "Modifier")
    str = str.replace("Save", "Enregistrer")
    str = str.replace("Cancel", "Annuler")
    str = str.replace("Done", "Terminé")
    str = str.replace("Add", "Ajouter")
    str = str.replace("Next", "Suivant")
    str = str.replace("Back", "Retour")
    str = str.replace("Today", "Aujourd'hui")
    str = str.replace("Yesterday", "Hier")
    str = str.replace("Tomorrow", "Demain")
    str = str.replace("Habits", "Habitudes")
    str = str.replace("Statistics", "Statistiques")
    str = str.replace("Achievements", "Succès")
    str = str.replace("Settings", "Paramètres")
    str = str.replace("Profile", "Profil")
    return str
}

private fun translateDynamicGeorgian(en: String, de: String): String {
    var str = en
    str = str.replace("Habit updated!", "ჩვევა განახლდა!")
    str = str.replace("Habit added!", "ჩვევა დაემატა!")
    str = str.replace("Delete", "წაშლა")
    str = str.replace("Edit", "რედაქტირება")
    str = str.replace("Save", "შენახვა")
    str = str.replace("Cancel", "გაუქმება")
    str = str.replace("Done", "მზადაა")
    str = str.replace("Add", "დამატება")
    str = str.replace("Next", "შემდეგი")
    str = str.replace("Back", "უკან")
    str = str.replace("Today", "დღეს")
    str = str.replace("Yesterday", "გუშინ")
    str = str.replace("Tomorrow", "ხვალ")
    str = str.replace("Total Completions", "სულ დასრულებები")
    str = str.replace("Total Check-ins", "სულ ჩექინები")
    str = str.replace("Total Check-Ins", "სულ ჩექინები")
    str = str.replace("Active Habits", "აქტიური ჩვევები")
    str = str.replace("Days Longest Streak", "დღეების ყველაზე გრძელი სერია")
    str = str.replace("Streak", "სერია")
    str = str.replace("Progress", "პროგრესი")
    str = str.replace("Language", "ენა")
    str = str.replace("Settings", "პარამეტრები")
    str = str.replace("Statistics", "სტატისტიკა")
    str = str.replace("Achievements", "მიღწევები")
    str = str.replace("Habits", "ჩვევები")
    str = str.replace("soundscapes & focus audio", "ხმოვანი გარემო და ფოკუსის აუდიო")
    str = str.replace("Soundscapes & Fokus-Audio", "ხმოვანი გარემო და ფოკუსის აუდიო")
    str = str.replace("Support", "მხარდაჭერა")
    str = str.replace("Email", "ელ-ფოსტა")
    str = str.replace("E-Mail", "ელ-ფოსტა")
    return str
}

private fun translateDynamicChinese(en: String, de: String): String {
    var str = en
    str = str.replace("Habit updated!", "习惯已更新！")
    str = str.replace("Habit added!", "习惯已添加！")
    str = str.replace("Delete", "删除")
    str = str.replace("Edit", "编辑")
    str = str.replace("Save", "保存")
    str = str.replace("Cancel", "取消")
    str = str.replace("Done", "完成")
    str = str.replace("Add", "添加")
    str = str.replace("Next", "下一步")
    str = str.replace("Back", "返回")
    str = str.replace("Today", "今天")
    str = str.replace("Yesterday", "昨天")
    str = str.replace("Tomorrow", "明天")
    str = str.replace("Total Completions", "累计完成次数")
    str = str.replace("Total Check-ins", "累计打卡次数")
    str = str.replace("Total Check-Ins", "累计打卡次数")
    str = str.replace("Active Habits", "活跃习惯")
    str = str.replace("Days Longest Streak", "历史最高连续天数")
    str = str.replace("Streak", "连续达成")
    str = str.replace("Progress", "进度")
    str = str.replace("Language", "语言")
    str = str.replace("Settings", "设置")
    str = str.replace("Statistics", "数据统计")
    str = str.replace("Achievements", "成就勋章")
    str = str.replace("Habits", "习惯")
    str = str.replace("soundscapes & focus audio", "专注白噪音与背景音频")
    str = str.replace("Soundscapes & Fokus-Audio", "专注白噪音与背景音频")
    str = str.replace("Support", "支持与帮助")
    str = str.replace("Email", "电子邮件")
    str = str.replace("E-Mail", "电子邮件")
    return str
}
