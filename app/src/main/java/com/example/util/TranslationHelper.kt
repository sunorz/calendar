package com.example.util

import java.util.Locale

/**
 * Provides bilingual translations for traditional Chinese festivals, solar terms,
 * and lunar calendar concepts.
 * English translations strictly follow official standards established by Chinese government authorities
 * (GB/T 33694-2017 & Standard Translation of Public Terms).
 */
object TranslationHelper {

    fun isEnglish(): Boolean {
        return Locale.getDefault().language.equals("en", ignoreCase = true)
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
        "三月三" to "Shangsi Festival (Double Third)",
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

    // Heavenly Stems in Pinyin
    private val HEAVENLY_STEMS_PINYIN = listOf("Jia", "Yi", "Bing", "Ding", "Wu", "Ji", "Geng", "Xin", "Ren", "Gui")

    // Earthly Branches with Zodiac animal in English
    private val EARTHLY_BRANCHES_EN = listOf(
        "Zi (Rat)", "Chou (Ox)", "Yin (Tiger)", "Mao (Rabbit)",
        "Chen (Dragon)", "Si (Snake)", "Wu (Horse)", "Wei (Goat)",
        "Shen (Monkey)", "You (Rooster)", "Xu (Dog)", "Hai (Pig)"
    )

    // English Lunar Month Names
    private val LUNAR_MONTH_NAMES_EN = listOf(
        "1st Lunar Month",
        "2nd Lunar Month",
        "3rd Lunar Month",
        "4th Lunar Month",
        "5th Lunar Month",
        "6th Lunar Month",
        "7th Lunar Month",
        "8th Lunar Month",
        "9th Lunar Month",
        "10th Lunar Month",
        "11th Lunar Month",
        "12th Lunar Month"
    )

    // Standard Gregorian Month Names in English
    private val GREGORIAN_MONTHS_EN = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    /**
     * Gets the month name according to language setting:
     * - In English: January, February, ..., September, etc.
     * - In Chinese: 1月, 2月, ..., 9月, etc.
     */
    fun getMonthName(month: Int): String {
        if (!isEnglish()) return "${month}月"
        return GREGORIAN_MONTHS_EN.getOrElse(month - 1) { "Month $month" }
    }

    /**
     * Formats Year and Month:
     * - In English: September 2026
     * - In Chinese: 2026年9月
     */
    fun formatYearMonth(year: Int, month: Int): String {
        if (!isEnglish()) return "${year}年${month}月"
        return "${getMonthName(month)} $year"
    }

    /**
     * Translates a festival or special day name.
     */
    fun translateFestival(chineseName: String?): String? {
        if (chineseName.isNullOrBlank()) return null
        if (!isEnglish()) return chineseName

        // Direct match
        FESTIVALS_EN[chineseName]?.let { return it }

        // Also check solar terms in case a festival is named after one (e.g. 清明)
        SOLAR_TERMS_EN[chineseName]?.let { return it }

        // Partial or trimmed match
        for ((key, value) in FESTIVALS_EN) {
            if (chineseName.contains(key) || key.contains(chineseName)) {
                return value
            }
        }
        return chineseName
    }

    /**
     * Translates a 24 solar term name.
     */
    fun translateSolarTerm(chineseTerm: String?): String? {
        if (chineseTerm.isNullOrBlank()) return null
        if (!isEnglish()) return chineseTerm
        return SOLAR_TERMS_EN[chineseTerm] ?: chineseTerm
    }

    /**
     * Returns the ordinal string for a number (e.g., 1st, 2nd, 3rd, 21st, 30th).
     */
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

    /**
     * Formats the standard short lunar day text:
     * - In Chinese: 正月, 初一, 初二, 十五, 廿三, etc.
     * - In English: 1st Day, 2nd Day, 15th Day, or "1st Month" if day 1.
     */
    fun getStandardLunarDayText(lunarMonth: Int, lunarDay: Int, isLeap: Boolean = false): String {
        if (!isEnglish()) {
            return LunarCalculator.getStandardLunarDayText(lunarMonth, lunarDay, isLeap)
        }
        val leapPrefix = if (isLeap) "Leap " else ""
        return if (lunarDay == 1) {
            "${leapPrefix}M${lunarMonth}"
        } else {
            getOrdinal(lunarDay)
        }
    }

    /**
     * Formats Lunar Month and Day (e.g., "七月初一", "三月廿二").
     * - In Chinese: 七月初一
     * - In English: 7th Month 1st Day (or Leap 7th Month 1st Day)
     */
    fun getLunarMonthAndDayText(lunarMonth: Int, lunarDay: Int, isLeap: Boolean = false): String {
        if (!isEnglish()) {
            return LunarCalculator.getLunarMonthAndDayText(lunarMonth, lunarDay, isLeap)
        }
        val leapPrefix = if (isLeap) "Leap " else ""
        val monthStr = LUNAR_MONTH_NAMES_EN.getOrElse(lunarMonth - 1) { "Month $lunarMonth" }
        return "${leapPrefix}${monthStr}, ${getOrdinal(lunarDay)}"
    }

    /**
     * Formats the complete full lunar date string:
     * - In Chinese: 丙午年七月十九
     * - In English: Year of Bing-Wu (Horse), 7th Month 19th
     */
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

    // Compact / Fitted translations for small Calendar Grid cells in English (prevents ugly '...' truncation)
    private val FESTIVALS_CELL_SHORT_EN = mapOf(
        "元旦" to "New Year",
        "除夕" to "CNY Eve",
        "春节" to "Spring",
        "破五" to "Po Wu",
        "元宵节" to "Lantern",
        "龙抬头" to "Dragon Head",
        "三月三" to "Shangsi",
        "清明节" to "Qingming",
        "端午" to "Dragon",
        "端午节" to "Dragon",
        "七夕节" to "Qixi",
        "中元节" to "Ghost Day",
        "中秋" to "Mid-Aut",
        "中秋节" to "Mid-Aut",
        "重阳节" to "Double 9",
        "寒衣节" to "Hanyi",
        "下元节" to "Xiayuan",
        "腊八节" to "Laba",
        "小年" to "Little CNY",
        "北方小年" to "Little CNY",
        "南方小年" to "Little CNY",
        "劳动节" to "Labor",
        "五一" to "Labor",
        "国庆" to "National",
        "国庆节" to "National",
        "十一" to "National",
        "情人节" to "Valentine",
        "妇女节" to "Women",
        "青年节" to "Youth",
        "儿童节" to "Children",
        "建党节" to "CPC Day",
        "建军节" to "Army Day",
        "教师节" to "Teachers",
        "九一八" to "Sept 18",
        "程序员节" to "Programmer",
        "万圣节" to "Halloween",
        "感恩节" to "Thanksgiving",
        "平安夜" to "Xmas Eve",
        "圣诞节" to "Christmas"
    )

    private val SOLAR_TERMS_CELL_SHORT_EN = mapOf(
        "立春" to "Beg Spring",
        "雨水" to "Rain Water",
        "惊蛰" to "Insects",
        "春分" to "Spring Eq",
        "清明" to "Qingming",
        "谷雨" to "Grain Rain",
        "立夏" to "Beg Summer",
        "小满" to "Lesser Full",
        "芒种" to "Grain Ear",
        "夏至" to "Summer Sol",
        "小暑" to "Lesser Heat",
        "大暑" to "Great Heat",
        "立秋" to "Beg Autumn",
        "处暑" to "End Heat",
        "白露" to "White Dew",
        "秋分" to "Autumn Eq",
        "寒露" to "Cold Dew",
        "霜降" to "Frost",
        "立冬" to "Beg Winter",
        "小雪" to "Lesser Snow",
        "大雪" to "Great Snow",
        "冬至" to "Winter Sol",
        "小寒" to "Lesser Cold",
        "大寒" to "Great Cold"
    )

    /**
     * Localizes display note for calendar grid.
     * Uses compact terms in English so text does not overflow tiny calendar cells.
     */
    fun getLocalizedDisplayNote(dayInfo: com.example.data.model.DayInfo): String {
        if (!isEnglish()) return dayInfo.displayNote

        // Check short festival name for grid cells
        dayInfo.festivalName?.let { fest ->
            FESTIVALS_CELL_SHORT_EN[fest]?.let { return it }
            for ((key, value) in FESTIVALS_CELL_SHORT_EN) {
                if (fest.contains(key) || key.contains(fest)) return value
            }
        }

        // Check short solar term name
        dayInfo.solarTermName?.let { term ->
            SOLAR_TERMS_CELL_SHORT_EN[term]?.let { return it }
        }

        // Standard lunar day
        val lunar = LunarCalculator.getLunarDate(dayInfo.year, dayInfo.month, dayInfo.day)
        return getStandardLunarDayText(lunar.lunarMonth, lunar.lunarDay, lunar.isLeap)
    }

    /**
     * Localizes full lunar text for cards and detail views.
     */
    fun getLocalizedFullLunarText(dayInfo: com.example.data.model.DayInfo): String {
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
        val isLunarFestival: Boolean
    )

    /**
     * Card Lunar Priority Rule (Chinese & English):
     * 1. 农历节日时，同时显示农历节日和节气（如果有），不显示普通农历
     * 2. 如果是公历节日（如国庆节），显示公历节日，并显示普通农历（和节气，如果有）
     * 3. 如果没有节日但有节气，显示节气，（可以不显示普通农历或者按需，这里原逻辑是如果没有节日也没有节气才显示普通农历，
     *    但如果是节气，原逻辑是不显示普通农历的。我们暂保持：无节日有节气时不显示普通农历，或者...等下，节气本身就是农历体系的，所以原逻辑是不显示普通农历）
     * 4. 如果没有节日也没有节气，显示普通农历
     */
    fun getCardLunarPriorityData(dayInfo: com.example.data.model.DayInfo): CardLunarPriorityData {
        val festival = translateFestival(dayInfo.festivalName)
        val solarTerm = translateSolarTerm(dayInfo.solarTermName)
        val lunarDate = LunarCalculator.getLunarDate(dayInfo.year, dayInfo.month, dayInfo.day)
        val ordinaryLunar = getLunarMonthAndDayText(lunarDate.lunarMonth, lunarDate.lunarDay, lunarDate.isLeap)

        val hasFestival = !festival.isNullOrBlank() && !dayInfo.isLocaleHoliday
        val hasSolarTerm = !solarTerm.isNullOrBlank()

        return CardLunarPriorityData(
            hasFestival = hasFestival,
            festivalText = if (hasFestival) festival else null,
            hasSolarTerm = hasSolarTerm,
            solarTermText = if (hasSolarTerm) solarTerm else null,
            ordinaryLunarText = ordinaryLunar,
            isHolidayWithRest = dayInfo.isHolidayWithRest,
            isLunarFestival = dayInfo.isLunarFestival
        )
    }
}
