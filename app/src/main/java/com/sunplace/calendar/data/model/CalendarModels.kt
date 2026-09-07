package com.sunplace.calendar.data.model

data class FestivalItem(
    val date: String, // "M-D" or "lunarMonth-lunarDay"
    val name: String,
    val tag: Int = 0 // 1 means highlight
)

data class LocaleFestivalItem(
    val m: Int,
    val d: List<Int>
)

data class RestDayMonth(
    val m: Int,
    val d: List<Int>
)

data class CalendarConfig(
    val curYear: Int = 2026,
    val startWeek: Int = 4,
    val monthsLimit: List<Int> = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31),
    val lunarStart: List<Int> = listOf(0, 13),
    val lunarMonth: List<Int> = listOf(11, 12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
    val lunarMonthLimit: List<Int> = listOf(30, 29, 30, 29, 30, 29, 29, 30, 29, 29, 30, 30, 30),
    val lunarFestivals: List<FestivalItem> = emptyList(),
    val festivals: List<FestivalItem> = emptyList(),
    val localeFestivals: List<LocaleFestivalItem> = emptyList(),
    val restDays: List<RestDayMonth> = emptyList()
) {
    /**
     * Checks if a given month (1-12) and day (1-31) is a rest day (休息日).
     * If true -> Rest day (Red number)
     * If false -> Work day (Normal text color)
     */
    fun isRestDay(month: Int, day: Int): Boolean {
        val monthRest = restDays.find { it.m == month }
        return monthRest?.d?.contains(day) ?: false
    }

    /**
     * Checks if a given month (1-12) and day is a workday (工作日).
     */
    fun isWorkDay(month: Int, day: Int): Boolean {
        return !isRestDay(month, day)
    }

    /**
     * Checks if a day is in localeFestivals (e.g. 广西三月三).
     */
    fun isLocaleHoliday(month: Int, day: Int): Boolean {
        val loc = localeFestivals.find { it.m == month }
        return loc?.d?.contains(day) ?: false
    }
}

/**
 * Information for rendering a single day in the calendar.
 */
data class DayInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val dayOfWeekNumber: Int, // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val dayOfWeekAbbr: String, // MON, TUE, WED, THU, FRI, SAT, SUN
    val dayOfWeekChinese: String, // 周一, 周二, ...
    val isRestDay: Boolean, // true -> 休息日 (Day number is Red), false -> 工作日 (Normal text color)
    val isWorkDay: Boolean,
    val lunarText: String, // e.g. "初一", "正月"
    val festivalName: String? = null, // e.g. "春节", "元旦", "国庆节"
    val isLunarFestival: Boolean = false, // true if the festival is a Lunar festival (e.g. "春节", "中秋节")
    val solarTermName: String? = null, // e.g. "立春", "清明"
    val lunarMonthAndDayText: String = "", // e.g. "三月廿二", "七月初一"
    val hasFestival: Boolean = false, // Whether the day has a solar/lunar festival or 24 solar term
    val isHolidayWithRest: Boolean = false, // Whether the day is a festival AND it is a rest day (Subtitle is Red)
    val isLocaleHoliday: Boolean = false, // Whether the day is in localeFestivals (Subtitle is Green, Card shows "三月三")
    val localeHolidayName: String? = null, // e.g. "三月三"
    val isToday: Boolean = false,
    val fullLunarText: String = "" // e.g. "丙午年七月十九"
) {
    /**
     * Primary display note under the day number in calendar grid.
     */
    val displayNote: String
        get() = when {
            !festivalName.isNullOrBlank() -> festivalName
            !solarTermName.isNullOrBlank() -> solarTermName
            else -> lunarText
        }

    /**
     * Note for widget line 3: Priority -> Festival > 24 Solar Term > Lunar Date with Month (e.g. "三月廿二")
     */
    val widgetNote: String
        get() {
            if (!festivalName.isNullOrBlank()) return festivalName
            if (!solarTermName.isNullOrBlank()) return solarTermName
            return lunarMonthAndDayText.ifEmpty { lunarText }
        }
}

