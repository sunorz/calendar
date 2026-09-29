package com.sunplace.calendar.util

import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.CalendarDate
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.data.model.YearConfig
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone

data class LunarDate(
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeap: Boolean
)

object LunarCalculator {

    /**
     * Fixed TimeZone for China Standard Time (UTC+8 / Asia/Shanghai).
     * Ensures all calendar and date calculations strictly reflect UTC+8 regardless of system/device timezone.
     */
    val CHINA_TIME_ZONE: TimeZone get() = ServerTimeManager.CHINA_TIME_ZONE

    /**
     * Get a Calendar instance initialized to the fixed UTC+8 TimeZone based on calibrated Server Time.
     */
    fun getChinaCalendar(): Calendar = ServerTimeManager.getChinaCalendar()

    private val CHINESE_DIGITS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    private val HEAVENLY_STEMS = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val EARTHLY_BRANCHES = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

    private val STATUTORY_HOLIDAY_NAMES = setOf(
        "元旦", "春节", "除夕", "清明", "清明节", "劳动节", "五一", "端午", "端午节", "中秋", "中秋节", "国庆", "国庆节", "十一"
    )

    fun getYearStemBranch(lunarYear: Int): String {
        val offset = (lunarYear - 4) % 60
        val stemIndex = if (offset < 0) (offset + 60) % 10 else offset % 10
        val branchIndex = if (offset < 0) (offset + 60) % 12 else offset % 12
        return HEAVENLY_STEMS[stemIndex] + EARTHLY_BRANCHES[branchIndex] + "年"
    }

    private fun formatLunarMonthName(lunarMonth: Int, isLeap: Boolean): String {
        val prefix = if (isLeap) "闰" else ""
        return when (lunarMonth) {
            1 -> "${prefix}正月"
            11 -> "${prefix}冬月"
            12 -> "${prefix}腊月"
            else -> prefix + (CHINESE_DIGITS.getOrNull(lunarMonth - 1) ?: lunarMonth.toString()) + "月"
        }
    }

    private fun formatLunarDayName(lunarDay: Int): String {
        return when {
            lunarDay < 11 -> "初" + CHINESE_DIGITS[lunarDay - 1]
            lunarDay in 11..19 -> "十" + (if (lunarDay == 11) "一" else CHINESE_DIGITS[lunarDay - 11])
            lunarDay == 20 -> "二十"
            lunarDay in 21..29 -> "廿" + CHINESE_DIGITS[lunarDay - 21]
            lunarDay == 30 -> "三十"
            else -> lunarDay.toString()
        }
    }

    fun getFullLunarDateString(lunar: LunarDate): String {
        return getYearStemBranch(lunar.lunarYear) +
            formatLunarMonthName(lunar.lunarMonth, lunar.isLeap) +
            formatLunarDayName(lunar.lunarDay)
    }

    fun getFullLunarDateString(year: Int, month: Int, day: Int): String {
        return getFullLunarDateString(getLunarDate(year, month, day))
    }

    private val WEEK_ABBRS = mapOf(
        1 to "MON", 2 to "TUE", 3 to "WED", 4 to "THU", 5 to "FRI", 6 to "SAT", 7 to "SUN"
    )

    private val WEEK_CHINESE = mapOf(
        1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日"
    )

    private val DEFAULT_SOLAR_FESTIVALS = mapOf(
        "1-1" to "元旦",
        "1-10" to "警察节",
        "2-14" to "情人节",
        "3-5" to "学雷锋日",
        "3-8" to "妇女节",
        "3-12" to "植树节",
        "3-15" to "消费者日",
        "4-1" to "愚人节",
        "4-22" to "地球日",
        "5-1" to "劳动节",
        "5-4" to "青年节",
        "5-12" to "护士节",
        "6-1" to "儿童节",
        "7-1" to "建党节",
        "8-1" to "建军节",
        "9-3" to "抗战胜利",
        "9-10" to "教师节",
        "9-18" to "九一八",
        "9-30" to "烈士日",
        "10-1" to "国庆节",
        "10-24" to "程序员节",
        "10-31" to "万圣夜",
        "11-1" to "万圣节",
        "11-11" to "双十一",
        "12-1" to "艾滋病日",
        "12-13" to "国家公祭日",
        "12-20" to "澳门回归",
        "12-24" to "平安夜",
        "12-25" to "圣诞节"
    )

    private val SOLAR_TERM_NAMES = listOf(
        "小寒", "大寒", // 1月
        "立春", "雨水", // 2月
        "惊蛰", "春分", // 3月
        "清明", "谷雨", // 4月
        "立夏", "小满", // 5月
        "芒种", "夏至", // 6月
        "小暑", "大暑", // 7月
        "立秋", "处暑", // 8月
        "白露", "秋分", // 9月
        "寒露", "霜降", // 10月
        "立冬", "小雪", // 11月
        "大雪", "冬至"  // 12月
    )

    private val C_21ST = doubleArrayOf(
        5.4055, 20.12,   // 1月
        3.87, 18.73,     // 2月
        5.63, 20.646,    // 3月
        4.81, 20.1,      // 4月
        5.52, 21.04,     // 5月
        5.678, 21.37,    // 6月
        7.108, 22.83,    // 7月
        7.5, 23.13,      // 8月
        7.646, 23.042,   // 9月
        8.318, 23.438,   // 10月
        7.438, 22.36,    // 11月
        7.18, 21.94      // 12月
    )

    // Standard Lunar Info database covering 1900 to 2099 (200 years)
    private val LUNAR_INFO = intArrayOf(
        // 1900-1909
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        // 1910-1919
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        // 1920-1929
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        // 1930-1939
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        // 1940-1949
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        // 1950-1959
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        // 1960-1969
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        // 1970-1979
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        // 1980-1989
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        // 1990-1999
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        // 2000-2009
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        // 2010-2019
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        // 2020-2029
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        // 2030-2039
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        // 2040-2049
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        // 2050-2059
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06aa0, 0x1a6c4, 0x0aae0,
        // 2060-2069
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        // 2070-2079
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        // 2080-2089
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        // 2090-2099
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252
    )

    private val BASE_DATE = LocalDate.of(1900, 1, 31)

    fun getDayOfWeekAbbr(dayOfWeek: Int): String = WEEK_ABBRS[dayOfWeek] ?: "DAY"
    fun getDayOfWeekChinese(dayOfWeek: Int): String = WEEK_CHINESE[dayOfWeek] ?: ""

    /**
     * Converts Solar date (year, month, day) to LunarDate (lunarYear, lunarMonth, lunarDay, isLeap)
     */
    fun getLunarDate(year: Int, month: Int, day: Int): LunarDate {
        val targetDate = LocalDate.of(year, month, day)
        var offset = ChronoUnit.DAYS.between(BASE_DATE, targetDate).toInt()
        if (offset < 0 || year < 1900 || year > 2099) {
            return LunarDate(year, month, day, false)
        }

        var lunarYear = 1900
        while (lunarYear <= 2099 && offset > 0) {
            val daysInYear = getLunarYearDays(lunarYear)
            if (offset < daysInYear) break
            offset -= daysInYear
            lunarYear++
        }

        val leapMonth = getLeapMonth(lunarYear)
        var isLeap = false
        var lunarMonth = 1

        for (m in 1..12) {
            val mask = 0x8000 shr (m - 1)
            val daysInMonth = if ((LUNAR_INFO[lunarYear - 1900] and mask) != 0) 30 else 29

            if (offset < daysInMonth) {
                lunarMonth = m
                isLeap = false
                break
            }
            offset -= daysInMonth

            if (m == leapMonth) {
                val leapDays = getLeapDays(lunarYear)
                if (offset < leapDays) {
                    lunarMonth = m
                    isLeap = true
                    break
                }
                offset -= leapDays
            }
        }

        return LunarDate(lunarYear, lunarMonth, offset + 1, isLeap)
    }

    private fun getLunarYearDays(year: Int): Int {
        var sum = 29 * 12
        var mask = 0x8000
        while (mask >= 0x0010) {
            if ((LUNAR_INFO[year - 1900] and mask) != 0) sum++
            mask = mask shr 1
        }
        return sum + getLeapDays(year)
    }

    private fun getLeapDays(year: Int): Int {
        if (getLeapMonth(year) != 0) {
            return if ((LUNAR_INFO[year - 1900] and 0x10000) != 0) 30 else 29
        }
        return 0
    }

    private fun getLeapMonth(year: Int): Int = LUNAR_INFO[year - 1900] and 0xf

    /**
     * Standard lunar date label (e.g. 正月, 初一, 初二, 十五, 二十, 廿三, 腊月, 闰四月)
     */
    fun getStandardLunarDayText(lunarMonth: Int, lunarDay: Int, isLeap: Boolean = false): String {
        return if (lunarDay == 1) formatLunarMonthName(lunarMonth, isLeap)
        else formatLunarDayName(lunarDay)
    }

    /**
     * Lunar month and day text (e.g. 三月廿二, 七月初一, 腊月三十)
     */
    fun getLunarMonthAndDayText(lunarMonth: Int, lunarDay: Int, isLeap: Boolean = false): String {
        return formatLunarMonthName(lunarMonth, isLeap) + formatLunarDayName(lunarDay)
    }

    /**
     * Calculates the exact solar day for a 24 Solar Term in the given year (2000-2099)
     */
    fun getSolarTermDay(year: Int, termIndex: Int): Int {
        if (year !in 2000..2099 || termIndex !in 0..23) return -1
        val y = year % 100
        val d = 0.2422
        val c = C_21ST[termIndex]
        val leapOffset = if (termIndex in 0..3) ((y - 1) / 4) else (y / 4)
        var day = (y * d + c).toInt() - leapOffset

        when (termIndex) {
            0 -> if (year == 2019) day -= 1 // 小寒
            1 -> if (year == 2082) day += 1 // 大寒
            2 -> if (year == 2017) day += 1 // 立春
            3 -> if (year == 2026) day -= 1 // 雨水
            5 -> if (year == 2084) day += 1 // 春分
            6 -> if (year == 2008) day += 1 // 清明
            9 -> if (year == 2008) day += 1 // 小满
            12 -> if (year == 2016) day += 1 // 小暑
            14 -> if (year == 2002) day += 1 // 立秋
            19 -> if (year == 2089) day += 1 // 霜降
            20 -> if (year == 2089) day += 1 // 立冬
            23 -> if (year == 2021) day -= 1 // 冬至
        }
        return day
    }

    /**
     * Checks if a given solar date is a 24 Solar Term
     */
    fun getSolarTerm(year: Int, month: Int, day: Int): String? {
        if (year in 2000..2099) {
            val term1 = (month - 1) * 2
            val term2 = term1 + 1
            if (day == getSolarTermDay(year, term1)) return SOLAR_TERM_NAMES[term1]
            if (day == getSolarTermDay(year, term2)) return SOLAR_TERM_NAMES[term2]
        }
        return null
    }

    /**
     * Checks if a date is 除夕 (the day before Chinese New Year 正月初一)
     */
    fun isNewYearsEve(year: Int, month: Int, day: Int): Boolean {
        val nextDate = LocalDate.of(year, month, day).plusDays(1)
        val nextLunar = getLunarDate(nextDate.year, nextDate.monthValue, nextDate.dayOfMonth)
        return nextLunar.lunarMonth == 1 && nextLunar.lunarDay == 1 && !nextLunar.isLeap
    }

    /**
     * Standard Traditional Chinese Lunar Festival for any year
     */
    fun getDefaultLunarFestival(
        year: Int,
        month: Int,
        day: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeap: Boolean,
        showGuangxiSanyuesan: Boolean = true
    ): String? {
        if (isNewYearsEve(year, month, day)) {
            return "除夕"
        }
        if (isLeap) return null

        return when {
            lunarMonth == 1 && lunarDay == 1 -> "春节"
            lunarMonth == 1 && lunarDay == 5 -> "破五"
            lunarMonth == 1 && lunarDay == 15 -> "元宵节"
            lunarMonth == 2 && lunarDay == 2 -> "龙抬头"
            lunarMonth == 3 && lunarDay == 3 -> if (showGuangxiSanyuesan) "三月三" else null
            lunarMonth == 5 && lunarDay == 5 -> "端午节"
            lunarMonth == 7 && lunarDay == 7 -> "七夕节"
            lunarMonth == 7 && lunarDay == 15 -> "中元节"
            lunarMonth == 8 && lunarDay == 15 -> "中秋节"
            lunarMonth == 9 && lunarDay == 9 -> "重阳节"
            lunarMonth == 10 && lunarDay == 1 -> "寒衣节"
            lunarMonth == 10 && lunarDay == 15 -> "下元节"
            lunarMonth == 12 && lunarDay == 8 -> "腊八节"
            lunarMonth == 12 && lunarDay == 23 -> "北方小年"
            lunarMonth == 12 && lunarDay == 24 -> "南方小年"
            else -> null
        }
    }

    /**
     * Standard Solar Festival (including fixed dates and floating dates like Mother's Day, Father's Day, Thanksgiving)
     */
    fun getDefaultSolarFestival(year: Int, month: Int, day: Int, dow: Int): String? {
        DEFAULT_SOLAR_FESTIVALS["$month-$day"]?.let { return it }
        if (month == 5 && dow == 7 && day in 8..14) return "母亲节"
        if (month == 6 && dow == 7 && day in 15..21) return "父亲节"
        if (month == 11 && dow == 4 && day in 22..28) return "感恩节"
        return null
    }

    /**
     * Get days count for a given year and month (1..12)
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            else -> 30
        }
    }

    private fun buildDayInfo(
        year: Int,
        month: Int,
        day: Int,
        dow: Int,
        yearConfig: YearConfig?,
        showGuangxiSanyuesan: Boolean,
        isToday: Boolean
    ): DayInfo {
        val lunarDate = getLunarDate(year, month, day)
        val standardLunarText = getStandardLunarDayText(lunarDate.lunarMonth, lunarDate.lunarDay, lunarDate.isLeap)
        val lunarMonthAndDayText = getLunarMonthAndDayText(lunarDate.lunarMonth, lunarDate.lunarDay, lunarDate.isLeap)
        val defaultLunarFestival = getDefaultLunarFestival(
            year, month, day, lunarDate.lunarMonth, lunarDate.lunarDay, lunarDate.isLeap, showGuangxiSanyuesan
        )
        val solarTermName = getSolarTerm(year, month, day)
        val defaultSolarFestival = getDefaultSolarFestival(year, month, day, dow)

        val isRest: Boolean
        var isLocale = false
        var isExplicitLocale = false
        var localeName: String? = null

        if (yearConfig != null) {
            isRest = yearConfig.isRestDay(month, day)
            if (showGuangxiSanyuesan) {
                isLocale = yearConfig.isLocaleHoliday(month, day)
                isExplicitLocale = yearConfig.isExplicitLocaleHoliday(month, day)
                if (isLocale) {
                    localeName = "三月三假日"
                }
            }
        } else {
            isRest = (dow == 6 || dow == 7)
        }

        val festivalName = when {
            defaultLunarFestival != null && defaultSolarFestival != null -> "$defaultLunarFestival·$defaultSolarFestival"
            defaultLunarFestival != null -> defaultLunarFestival
            defaultSolarFestival != null -> defaultSolarFestival
            else -> null
        }
        val resolvedFestival = festivalName ?: solarTermName
        val isHolidayWithRest = isRest && resolvedFestival != null &&
            resolvedFestival.split("·").any { STATUTORY_HOLIDAY_NAMES.contains(it) }

        return DayInfo(
            year = year,
            month = month,
            day = day,
            dayOfWeekNumber = dow,
            dayOfWeekAbbr = getDayOfWeekAbbr(dow),
            dayOfWeekChinese = getDayOfWeekChinese(dow),
            isRestDay = isRest,
            isWorkDay = !isRest,
            lunarText = standardLunarText,
            festivalName = festivalName,
            isLunarFestival = defaultLunarFestival != null,
            solarTermName = solarTermName,
            lunarMonthAndDayText = lunarMonthAndDayText,
            hasFestival = resolvedFestival != null,
            isHolidayWithRest = isHolidayWithRest,
            isLocaleHoliday = isLocale,
            isExplicitLocaleHoliday = isExplicitLocale,
            localeHolidayName = localeName,
            isToday = isToday,
            fullLunarText = getFullLunarDateString(lunarDate)
        )
    }

    /**
     * Generates DayInfo list for ANY given (year, month).
     */
    fun getMonthDays(
        year: Int,
        month: Int,
        mainConfig: CalendarConfig,
        nextConfig: CalendarConfig? = null,
        showGuangxiSanyuesan: Boolean = true,
        todayDate: CalendarDate? = null
    ): List<DayInfo> {
        val daysInMonth = getDaysInMonth(year, month)
        val resolvedToday = todayDate ?: CalendarDate.today()
        val yearConfig = mainConfig.getYearConfig(year) ?: nextConfig?.getYearConfig(year)

        val firstDow = LocalDate.of(year, month, 1).dayOfWeek.value // 1=Mon..7=Sun
        val result = ArrayList<DayInfo>(daysInMonth)

        for (d in 1..daysInMonth) {
            val dow = (firstDow + d - 2) % 7 + 1
            val isToday = (year == resolvedToday.year && month == resolvedToday.month && d == resolvedToday.day)
            result.add(buildDayInfo(year, month, d, dow, yearConfig, showGuangxiSanyuesan, isToday))
        }

        return result
    }

    /**
     *Directly computes DayInfo for a single date without generating the entire month.
     */
    fun getDayInfoForDate(
        year: Int,
        month: Int,
        day: Int,
        mainConfig: CalendarConfig,
        nextConfig: CalendarConfig? = null,
        showGuangxiSanyuesan: Boolean = true,
        todayDate: CalendarDate? = null
    ): DayInfo {
        val resolvedToday = todayDate ?: CalendarDate.today()
        val yearConfig = mainConfig.getYearConfig(year) ?: nextConfig?.getYearConfig(year)
        val safeDay = day.coerceIn(1, getDaysInMonth(year, month))
        val dow = LocalDate.of(year, month, safeDay).dayOfWeek.value // 1=Mon..7=Sun
        val isToday = (year == resolvedToday.year && month == resolvedToday.month && safeDay == resolvedToday.day)
        return buildDayInfo(year, month, safeDay, dow, yearConfig, showGuangxiSanyuesan, isToday)
    }
}
