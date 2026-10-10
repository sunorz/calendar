package com.sunplace.calendar.util

import android.content.Context
import com.sunplace.calendar.data.model.DayInfo
import java.util.Locale

/**
 * Provides bilingual translations for traditional Chinese festivals, solar terms,
 * and lunar calendar concepts.
 * English translations strictly follow official standards established by Chinese government authorities
 * (GB/T 33694-2017 & Standard Translation of Public Terms).
 */
object TranslationHelper {
    /**
     * Checks if a locale represents any form of Chinese language.
     */
    fun isChineseLanguage(locale: Locale?): Boolean {
        if (locale == null) return false
        val lang = locale.language.lowercase()
        return lang.startsWith("zh")
    }

    /**
     * Retrieves the primary Locale based on context or system default.
     * Accurately corrects Android 7.0+ multi-locale fallback inversion (e.g., [en, zh-CN]).
     */
    fun getPrimaryLocale(context: Context? = null): Locale {
        return try {
            val appLocales = if (context != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.resources.configuration.locales
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList.getDefault()
            } else null

            if (appLocales != null && !appLocales.isEmpty) {
                appLocales.get(0)
            } else if (context != null) {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale ?: getSystemDefaultLocale()
            } else {
                getSystemDefaultLocale()
            }
        } catch (_: Throwable) {
            getSystemDefaultLocale()
        }
    }

    /**
     * Checks if the primary display language is English (or non-Chinese).
     * Follows standard 1.1.8 logic: when primary language is Chinese (zh), returns false (Chinese).
     * Otherwise returns true (English).
     */
    fun isEnglish(context: Context? = null): Boolean {
        val primaryLocale = getPrimaryLocale(context)
        return !isChineseLanguage(primaryLocale)
    }

    /**
     * Provides comprehensive diagnostic locale information for debugging.
     */
    fun getDebugLocaleInfo(context: Context? = null): String {
        val primaryLocale = getPrimaryLocale(context)
        val lang = primaryLocale.language.lowercase()
        val isChinese = lang.startsWith("zh")
        val isEn = !isChinese

        val ctxLocalesStr = try {
            if (context != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.resources.configuration.locales.toLanguageTags()
            } else if (context != null) {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale?.toString() ?: "null"
            } else "context is null"
        } catch (e: Throwable) {
            "error: ${e.message}"
        }

        val sysLocaleListStr = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList.getDefault().toLanguageTags()
            } else "N/A (< API 24)"
        } catch (e: Throwable) {
            "error: ${e.message}"
        }

        val jvmLocaleDefaultStr = try {
            Locale.getDefault().toString()
        } catch (e: Throwable) {
            "error: ${e.message}"
        }

        val systemResourcesLocalesStr = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.content.res.Resources.getSystem().configuration.locales.toLanguageTags()
            } else {
                @Suppress("DEPRECATION")
                android.content.res.Resources.getSystem().configuration.locale?.toString() ?: "null"
            }
        } catch (e: Throwable) {
            "error: ${e.message}"
        }

        return buildString {
            appendLine("【核心语言判断】")
            appendLine("primaryLocale.language.lowercase(): \"$lang\"")
            appendLine("isChinese: $isChinese")
            appendLine("isEnglish (显示英文): $isEn")
            appendLine()
            appendLine("【详细 Locale 对象】")
            appendLine("primaryLocale: $primaryLocale")
            appendLine("primaryLocale.toLanguageTag(): ${primaryLocale.toLanguageTag()}")
            appendLine("primaryLocale.language: \"${primaryLocale.language}\"")
            appendLine("primaryLocale.script: \"${primaryLocale.script}\"")
            appendLine("primaryLocale.country: \"${primaryLocale.country}\"")
            appendLine()
            appendLine("【系统与应用环境】")
            appendLine("context.resources.locales: $ctxLocalesStr")
            appendLine("LocaleList.getDefault(): $sysLocaleListStr")
            appendLine("Locale.getDefault(): $jvmLocaleDefaultStr")
            appendLine("Resources.getSystem().locales: $systemResourcesLocalesStr")
        }
    }

    private fun getSystemDefaultLocale(): Locale {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val sysLocales = android.os.LocaleList.getDefault()
                if (!sysLocales.isEmpty) {
                    sysLocales.get(0)
                } else {
                    Locale.getDefault()
                }
            } else {
                Locale.getDefault()
            }
        } catch (_: Throwable) {
            Locale.getDefault()
        } ?: Locale.SIMPLIFIED_CHINESE
    }

    // Official and authoritative translations for 24 Solar Terms (二十四节气 - GB/T 33694-2017)
    private val SOLAR_TERMS_EN = mapOf(
        "立春" to "Beginning of Spring",
        "雨水" to "Rain Water",
        "惊蛰" to "Awakening of Insects",
        "春分" to "Spring Equinox",
        "清明" to "Pure Brightness",
        "谷雨" to "Grain Rain",
        "立夏" to "Beginning of Summer",
        "小满" to "Lesser Fullness of Grain",
        "芒种" to "Grain in Ear",
        "夏至" to "Summer Solstice",
        "小暑" to "Lesser Heat",
        "大暑" to "Greater Heat",
        "立秋" to "Beginning of Autumn",
        "处暑" to "End of Heat",
        "白露" to "White Dew",
        "秋分" to "Autumn Equinox",
        "寒露" to "Cold Dew",
        "霜降" to "Frost's Descent",
        "立冬" to "Beginning of Winter",
        "小雪" to "Lesser Snow",
        "大雪" to "Greater Snow",
        "冬至" to "Winter Solstice",
        "小寒" to "Lesser Cold",
        "大寒" to "Greater Cold"
    )

    // Official and authoritative translations for Traditional Festivals & Statutory Holidays
    private val FESTIVALS_EN = mapOf(
        // Statutory & Traditional Festivals
        "元旦" to "New Year's Day",
        "除夕" to "Chinese New Year's Eve",
        "春节" to "Spring Festival",
        "破五" to "Po Wu (5th Day of CNY)",
        "元宵节" to "Lantern Festival",
        "龙抬头" to "Dragon-Head-Raising Day",
        "三月三" to "Sanyuesan Festival",
        "三月三假日" to "Sanyuesan Festival",
        "壮族三月三" to "Sanyuesan Festival",
        "清明节" to "Tomb-Sweeping Day",
        "端午" to "Dragon Boat Festival",
        "端午节" to "Dragon Boat Festival",
        "七夕节" to "Qixi Festival (Double Seventh)",
        "中元节" to "Zhongyuan Festival (Ghost Festival)",
        "中秋" to "Mid-Autumn Festival",
        "中秋节" to "Mid-Autumn Festival",
        "重阳节" to "Chongyang Festival (Double Ninth)",
        "寒衣节" to "Hanyi Festival",
        "下元节" to "Xiayuan Festival",
        "腊八节" to "Laba Festival",
        "北方小年" to "Little New Year (North)",
        "南方小年" to "Little New Year (South)",
        "小年" to "Little New Year",

        // Solar / Modern Commemorative & Public Holidays
        "警察节" to "Police Day",
        "情人节" to "Valentine's Day",
        "学雷锋日" to "Lei Feng Day",
        "妇女节" to "Women's Day",
        "植树节" to "Tree-Planting Day",
        "消费者日" to "Consumer Rights Day",
        "愚人节" to "April Fools' Day",
        "地球日" to "Earth Day",
        "劳动节" to "International Workers' Day",
        "五一" to "Labor Day",
        "青年节" to "Youth Day",
        "母亲节" to "Mother's Day",
        "护士节" to "Nurses Day",
        "儿童节" to "Children's Day",
        "父亲节" to "Father's Day",
        "建党节" to "CPC Founding Day",
        "建军节" to "Army Day",
        "抗战胜利" to "Victory Day",
        "教师节" to "Teachers' Day",
        "九一八" to "Sept 18 Incident Memorial Day",
        "烈士日" to "Martyrs' Day",
        "国庆" to "National Day",
        "国庆节" to "National Day",
        "十一" to "National Day",
        "程序员节" to "Programmers' Day",
        "万圣夜" to "Halloween Eve",
        "万圣节" to "Halloween",
        "感恩节" to "Thanksgiving Day",
        "双十一" to "Double 11 Shopping Day",
        "艾滋病日" to "World AIDS Day",
        "国家公祭日" to "National Memorial Day",
        "澳门回归" to "Macao Return Day",
        "平安夜" to "Christmas Eve",
        "圣诞节" to "Christmas Day"
    )

    private val HEAVENLY_STEMS_PINYIN = listOf("Jia", "Yi", "Bing", "Ding", "Wu", "Ji", "Geng", "Xin", "Ren", "Gui")

    private val EARTHLY_BRANCHES_EN = listOf(
        "Zi (Rat)", "Chou (Ox)", "Yin (Tiger)", "Mao (Rabbit)",
        "Chen (Dragon)", "Si (Snake)", "Wu (Horse)", "Wei (Goat)",
        "Shen (Monkey)", "You (Rooster)", "Xu (Dog)", "Hai (Pig)"
    )

    private val LUNAR_MONTH_NAMES_EN = listOf(
        "1st Lunar Month", "2nd Lunar Month", "3rd Lunar Month", "4th Lunar Month",
        "5th Lunar Month", "6th Lunar Month", "7th Lunar Month", "8th Lunar Month",
        "9th Lunar Month", "10th Lunar Month", "11th Lunar Month", "12th Lunar Month"
    )

    private val GREGORIAN_MONTHS_EN = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val GREGORIAN_MONTHS_SHORT_EN = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun getMonthName(month: Int): String {
        if (!isEnglish()) return "${month}月"
        return GREGORIAN_MONTHS_EN.getOrElse(month - 1) { "Month $month" }
    }

    fun getShortMonthName(month: Int): String {
        if (!isEnglish()) return "${month}月"
        return GREGORIAN_MONTHS_SHORT_EN.getOrElse(month - 1) { "$month" }
    }

    fun formatYearMonth(year: Int, month: Int): String {
        if (!isEnglish()) return "${year}年${month}月"
        return "${getMonthName(month)} $year"
    }

    fun formatShortYearMonth(year: Int, month: Int): String {
        if (!isEnglish()) return "${year}年${month}月"
        return "${getShortMonthName(month)} $year"
    }

    fun translateFestival(chineseName: String?): String? {
        if (chineseName.isNullOrBlank()) return null
        if (!isEnglish()) return chineseName

        val parts = chineseName.split("·")
        val translatedParts = parts.map { part ->
            FESTIVALS_EN[part] ?: SOLAR_TERMS_EN[part] ?: getPartialMatch(part)
        }
        return translatedParts.joinToString(" · ")
    }

    private fun getPartialMatch(part: String): String {
        for ((key, value) in FESTIVALS_EN) {
            if (part.contains(key) || key.contains(part)) {
                return value
            }
        }
        return part
    }

    fun translateSolarTerm(chineseTerm: String?): String? {
        if (chineseTerm.isNullOrBlank()) return null
        if (!isEnglish()) return chineseTerm
        return SOLAR_TERMS_EN[chineseTerm] ?: chineseTerm
    }

    private fun getOrdinal(n: Int): String {
        val remainder100 = n % 100
        if (remainder100 in 11..13) return "${n}th"
        return when (n % 10) {
            1 -> "${n}st"
            2 -> "${n}nd"
            3 -> "${n}rd"
            else -> "${n}th"
        }
    }

    fun getStandardLunarDayText(lunarMonth: Int, lunarDay: Int, isLeap: Boolean = false): String {
        if (!isEnglish()) {
            return LunarCalculator.getStandardLunarDayText(lunarMonth, lunarDay, isLeap)
        }
        val leapPrefix = if (isLeap) "Leap " else ""
        return if (lunarDay == 1) "${leapPrefix}M${lunarMonth}" else getOrdinal(lunarDay)
    }

    fun getLunarMonthAndDayText(lunarMonth: Int, lunarDay: Int, isLeap: Boolean = false): String {
        if (!isEnglish()) {
            return LunarCalculator.getLunarMonthAndDayText(lunarMonth, lunarDay, isLeap)
        }
        val leapPrefix = if (isLeap) "Leap " else ""
        val monthStr = LUNAR_MONTH_NAMES_EN.getOrElse(lunarMonth - 1) { "Month $lunarMonth" }
        return "${leapPrefix}${monthStr}, ${getOrdinal(lunarDay)}"
    }

    fun getFullLunarDateString(year: Int, month: Int, day: Int): String {
        if (!isEnglish()) {
            return LunarCalculator.getFullLunarDateString(year, month, day)
        }
        val lunar = LunarCalculator.getLunarDate(year, month, day)
        val offset = (lunar.lunarYear - 4) % 60
        val stemIndex = if (offset < 0) (offset + 60) % 10 else offset % 10
        val branchIndex = if (offset < 0) (offset + 60) % 12 else offset % 12

        val stem = HEAVENLY_STEMS_PINYIN[stemIndex]
        val branchAndZodiac = EARTHLY_BRANCHES_EN[branchIndex]

        val leapPrefix = if (lunar.isLeap) "Leap " else ""
        val monthName = LUNAR_MONTH_NAMES_EN.getOrElse(lunar.lunarMonth - 1) { "Month ${lunar.lunarMonth}" }
        val dayOrdinal = getOrdinal(lunar.lunarDay)

        return "Year of $stem-$branchAndZodiac, $leapPrefix$monthName, $dayOrdinal"
    }

    private val LEGAL_HOLIDAYS = setOf("元旦", "春节", "清明节", "端午节", "劳动节", "中秋节", "国庆节")
    private val TRADITIONS_AND_TERMS = setOf(
        "龙抬头", "中元节",
        "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
        "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
        "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
        "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"
    )

    /**
     * Optimized display note based on user priority and length constraints.
     */
    fun getFilteredDisplayNote(dayInfo: DayInfo): String {
        val festival = dayInfo.festivalName
        val term = dayInfo.solarTermName
        val festivalParts = if (!festival.isNullOrBlank()) festival.split("·") else emptyList()

        val allPotential = mutableListOf<String>()

        // 1. Legal Holidays
        festivalParts.forEach { part ->
            if (LEGAL_HOLIDAYS.contains(part)) allPotential.add(part)
        }

        // 2. Other Festivals
        festivalParts.forEach { part ->
            if (!LEGAL_HOLIDAYS.contains(part) && !TRADITIONS_AND_TERMS.contains(part)) {
                allPotential.add(part)
            }
        }

        // 3. Traditions/Terms
        if (!term.isNullOrBlank()) allPotential.add(term)
        festivalParts.forEach { part ->
            if (TRADITIONS_AND_TERMS.contains(part)) allPotential.add(part)
        }

        val isEn = isEnglish()
        for (note in allPotential) {
            if (note.length <= 4) {
                return if (isEn) translateFestival(note) ?: note else note
            }
        }

        // Default: Standard lunar day (reuse precomputed lunarText in Chinese mode)
        if (!isEn && dayInfo.lunarText.isNotBlank()) {
            return dayInfo.lunarText
        }
        val lunar = LunarCalculator.getLunarDate(dayInfo.year, dayInfo.month, dayInfo.day)
        return getStandardLunarDayText(lunar.lunarMonth, lunar.lunarDay, lunar.isLeap)
    }

    /**
     * Localizes full lunar text for cards and detail views.
     */
    fun getLocalizedFullLunarText(dayInfo: DayInfo): String {
        if (!isEnglish()) return dayInfo.fullLunarText
        return getFullLunarDateString(dayInfo.year, dayInfo.month, dayInfo.day)
    }

    data class CardLunarPriorityData(
        val hasFestival: Boolean,
        val festivalText: String?,
        val hasSolarTerm: Boolean,
        val solarTermText: String?,
        val ordinaryLunarText: String,
        val isHolidayWithRest: Boolean,
        val isLunarFestival: Boolean,
        val combinedText: String
    )

    /**
     * Card Lunar Priority Rule (Chinese & English):
     * Completely combines lunar date, solar term, and festivals.
     */
    fun getCardLunarPriorityData(dayInfo: DayInfo): CardLunarPriorityData {
        val isEn = isEnglish()
        val festival = translateFestival(dayInfo.festivalName)
        val solarTerm = translateSolarTerm(dayInfo.solarTermName)
        val ordinaryLunar = if (!isEn && dayInfo.lunarMonthAndDayText.isNotBlank()) {
            dayInfo.lunarMonthAndDayText
        } else {
            val lunarDate = LunarCalculator.getLunarDate(dayInfo.year, dayInfo.month, dayInfo.day)
            getLunarMonthAndDayText(lunarDate.lunarMonth, lunarDate.lunarDay, lunarDate.isLeap)
        }

        val localeText = if (dayInfo.isLocaleHoliday) {
            translateFestival(dayInfo.localeHolidayName ?: "三月三假日") ?: "三月三假日"
        } else null

        val festList = mutableListOf<String>()
        if (localeText != null) {
            festList.add(localeText)
        }
        if (!festival.isNullOrBlank()) {
            festival.split(" · ", "·").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty() && trimmed != localeText && !festList.contains(trimmed)) {
                    festList.add(trimmed)
                }
            }
        }

        val separator = if (isEn) " · " else "·"
        val hasFestival = festList.isNotEmpty()
        val festivalText = if (hasFestival) festList.joinToString(separator) else null
        val hasSolarTerm = !solarTerm.isNullOrBlank()

        val parts = mutableListOf<String>()
        if (ordinaryLunar.isNotBlank()) parts.add(ordinaryLunar)
        if (hasSolarTerm) parts.add(solarTerm!!)
        if (hasFestival) parts.addAll(festList)

        return CardLunarPriorityData(
            hasFestival = hasFestival,
            festivalText = festivalText,
            hasSolarTerm = hasSolarTerm,
            solarTermText = if (hasSolarTerm) solarTerm else null,
            ordinaryLunarText = ordinaryLunar,
            isHolidayWithRest = dayInfo.isHolidayWithRest,
            isLunarFestival = dayInfo.isLunarFestival,
            combinedText = parts.joinToString(separator)
        )
    }
}
