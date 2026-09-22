package com.sunplace.calendar.data.model

data class LocaleFestivalItem(
    val m: Int,
    val d: List<Int>
)

data class RestDayMonth(
    val m: Int,
    val d: List<Int>
)

data class YearConfig(
    val year: Int,
    val restDays: List<RestDayMonth> = emptyList(),
    val localeFestivals: List<LocaleFestivalItem> = emptyList()
) {
    fun isRestDay(month: Int, day: Int): Boolean {
        val monthRest = restDays.find { it.m == month }
        return monthRest?.d?.contains(day) ?: false
    }

    fun isWorkDay(month: Int, day: Int): Boolean {
        return !isRestDay(month, day)
    }

    fun getValidLocaleFestivalDates(): List<Pair<Int, Int>> {
        return localeFestivals
            .filter { it.m in 1..12 }
            .flatMap { item ->
                item.d.filter { it in 1..31 }.map { day -> Pair(item.m, day) }
            }
            .sortedWith(compareBy({ it.first }, { it.second }))
    }

    val localeFestivalFirstDay: Pair<Int, Int>?
        get() = getValidLocaleFestivalDates().firstOrNull()

    val localeFestivalLastDay: Pair<Int, Int>?
        get() = getValidLocaleFestivalDates().lastOrNull()

    fun isInLocaleFestivalRange(month: Int, day: Int): Boolean {
        val first = localeFestivalFirstDay ?: return false
        val last = localeFestivalLastDay ?: return false

        return try {
            val startDate = java.time.LocalDate.of(year, first.first, first.second)
            val endDate = java.time.LocalDate.of(year, last.first, last.second)
            val currDate = java.time.LocalDate.of(year, month, day)
            !currDate.isBefore(startDate) && !currDate.isAfter(endDate)
        } catch (e: Exception) {
            val current = month * 100 + day
            val start = first.first * 100 + first.second
            val end = last.first * 100 + last.second
            if (start <= end) {
                current in start..end
            } else {
                current >= start || current <= end
            }
        }
    }

    fun isExplicitLocaleHoliday(month: Int, day: Int): Boolean {
        val loc = localeFestivals.find { it.m == month }
        return loc?.d?.contains(day) ?: false
    }

    val isLocaleFestivalRangeContinuous: Boolean
        get() {
            val first = localeFestivalFirstDay ?: return false
            val last = localeFestivalLastDay ?: return false
            return try {
                val startDate = java.time.LocalDate.of(year, first.first, first.second)
                val endDate = java.time.LocalDate.of(year, last.first, last.second)
                if (startDate.isAfter(endDate)) return false
                var curr = startDate
                while (!curr.isAfter(endDate)) {
                    val m = curr.monthValue
                    val d = curr.dayOfMonth
                    val isExplicit = isExplicitLocaleHoliday(m, d)
                    val isRest = isRestDay(m, d)
                    if (!isExplicit && !isRest) {
                        return false
                    }
                    curr = curr.plusDays(1)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    fun isLocaleHoliday(month: Int, day: Int): Boolean {
        return if (isLocaleFestivalRangeContinuous) {
            isInLocaleFestivalRange(month, day)
        } else {
            isExplicitLocaleHoliday(month, day)
        }
    }
}

data class CalendarConfig(
    val curYear: Int = 2026,
    val localeFestivals: List<LocaleFestivalItem> = emptyList(),
    val restDays: List<RestDayMonth> = emptyList(),
    val yearlyConfigs: Map<Int, YearConfig> = emptyMap()
) {
    fun hasYearConfig(year: Int): Boolean {
        return yearlyConfigs.containsKey(year) || (curYear == year && restDays.isNotEmpty())
    }

    fun getYearConfig(year: Int): YearConfig? {
        if (yearlyConfigs.containsKey(year)) {
            return yearlyConfigs[year]
        }
        if (curYear == year && (restDays.isNotEmpty() || localeFestivals.isNotEmpty())) {
            return YearConfig(
                year = curYear,
                restDays = restDays,
                localeFestivals = localeFestivals
            )
        }
        return null
    }

    fun isRestDay(month: Int, day: Int): Boolean {
        val yCfg = getYearConfig(curYear)
        return yCfg?.isRestDay(month, day) ?: (restDays.find { it.m == month }?.d?.contains(day) ?: false)
    }

    fun isWorkDay(month: Int, day: Int): Boolean {
        return !isRestDay(month, day)
    }

    fun isExplicitLocaleHoliday(month: Int, day: Int): Boolean {
        val yCfg = getYearConfig(curYear)
        return yCfg?.isExplicitLocaleHoliday(month, day) ?: (localeFestivals.find { it.m == month }?.d?.contains(day) ?: false)
    }

    val localeFestivalFirstDay: Pair<Int, Int>?
        get() = getYearConfig(curYear)?.localeFestivalFirstDay

    val localeFestivalLastDay: Pair<Int, Int>?
        get() = getYearConfig(curYear)?.localeFestivalLastDay

    fun isInLocaleFestivalRange(month: Int, day: Int): Boolean {
        return getYearConfig(curYear)?.isInLocaleFestivalRange(month, day) ?: false
    }

    val isLocaleFestivalRangeContinuous: Boolean
        get() = getYearConfig(curYear)?.isLocaleFestivalRangeContinuous ?: false

    fun isLocaleHoliday(month: Int, day: Int): Boolean {
        val yCfg = getYearConfig(curYear)
        return yCfg?.isLocaleHoliday(month, day) ?: false
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
    val isLocaleHoliday: Boolean = false, // Whether the day is in localeFestivals range [a, b] (Popup card shows "三月三")
    val isExplicitLocaleHoliday: Boolean = false, // Whether the day is explicitly in json localeFestivals (Day number is Green)
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

/**
 * Lightweight date representation for reactive day updates.
 */
data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    companion object {
        fun today(): CalendarDate {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
            return CalendarDate(
                year = cal.get(java.util.Calendar.YEAR),
                month = cal.get(java.util.Calendar.MONTH) + 1,
                day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
    }
}

