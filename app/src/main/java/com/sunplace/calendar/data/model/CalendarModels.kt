package com.sunplace.calendar.data.model

import com.sunplace.calendar.util.LunarCalculator
import java.time.LocalDate
import java.util.Calendar

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
    fun isRestDay(month: Int, day: Int): Boolean =
        restDays.find { it.m == month }?.d?.contains(day) ?: false

    fun isWorkDay(month: Int, day: Int): Boolean = !isRestDay(month, day)

    fun getValidLocaleFestivalDates(): List<Pair<Int, Int>> =
        localeFestivals
            .filter { it.m in 1..12 }
            .flatMap { item -> item.d.filter { it in 1..31 }.map { day -> Pair(item.m, day) } }
            .sortedWith(compareBy({ it.first }, { it.second }))

    val localeFestivalFirstDay: Pair<Int, Int>?
        get() = getValidLocaleFestivalDates().firstOrNull()

    val localeFestivalLastDay: Pair<Int, Int>?
        get() = getValidLocaleFestivalDates().lastOrNull()

    fun isInLocaleFestivalRange(month: Int, day: Int): Boolean {
        val first = localeFestivalFirstDay ?: return false
        val last = localeFestivalLastDay ?: return false

        return try {
            val startDate = LocalDate.of(year, first.first, first.second)
            val endDate = LocalDate.of(year, last.first, last.second)
            val currDate = LocalDate.of(year, month, day)
            !currDate.isBefore(startDate) && !currDate.isAfter(endDate)
        } catch (_: Exception) {
            val current = month * 100 + day
            val start = first.first * 100 + first.second
            val end = last.first * 100 + last.second
            if (start <= end) current in start..end else (current >= start || current <= end)
        }
    }

    fun isExplicitLocaleHoliday(month: Int, day: Int): Boolean =
        localeFestivals.find { it.m == month }?.d?.contains(day) ?: false

    val isLocaleFestivalRangeContinuous: Boolean
        get() {
            val first = localeFestivalFirstDay ?: return false
            val last = localeFestivalLastDay ?: return false
            return try {
                val startDate = LocalDate.of(year, first.first, first.second)
                val endDate = LocalDate.of(year, last.first, last.second)
                if (startDate.isAfter(endDate)) return false
                var curr = startDate
                while (!curr.isAfter(endDate)) {
                    val m = curr.monthValue
                    val d = curr.dayOfMonth
                    if (!isExplicitLocaleHoliday(m, d) && !isRestDay(m, d)) {
                        return false
                    }
                    curr = curr.plusDays(1)
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    fun isLocaleHoliday(month: Int, day: Int): Boolean =
        if (isLocaleFestivalRangeContinuous) isInLocaleFestivalRange(month, day)
        else isExplicitLocaleHoliday(month, day)
}

data class CalendarConfig(
    val curYear: Int = 2026,
    val localeFestivals: List<LocaleFestivalItem> = emptyList(),
    val restDays: List<RestDayMonth> = emptyList(),
    val yearlyConfigs: Map<Int, YearConfig> = emptyMap()
) {
    fun hasYearConfig(year: Int): Boolean =
        yearlyConfigs.containsKey(year) || (curYear == year && restDays.isNotEmpty())

    fun getYearConfig(year: Int): YearConfig? {
        yearlyConfigs[year]?.let { return it }
        if (curYear == year && (restDays.isNotEmpty() || localeFestivals.isNotEmpty())) {
            return YearConfig(year = curYear, restDays = restDays, localeFestivals = localeFestivals)
        }
        return null
    }

    fun isRestDay(month: Int, day: Int): Boolean =
        getYearConfig(curYear)?.isRestDay(month, day)
            ?: (restDays.find { it.m == month }?.d?.contains(day) ?: false)

    fun isWorkDay(month: Int, day: Int): Boolean = !isRestDay(month, day)

    fun isExplicitLocaleHoliday(month: Int, day: Int): Boolean =
        getYearConfig(curYear)?.isExplicitLocaleHoliday(month, day)
            ?: (localeFestivals.find { it.m == month }?.d?.contains(day) ?: false)

    val localeFestivalFirstDay: Pair<Int, Int>?
        get() = getYearConfig(curYear)?.localeFestivalFirstDay

    val localeFestivalLastDay: Pair<Int, Int>?
        get() = getYearConfig(curYear)?.localeFestivalLastDay

    fun isInLocaleFestivalRange(month: Int, day: Int): Boolean =
        getYearConfig(curYear)?.isInLocaleFestivalRange(month, day) ?: false

    val isLocaleFestivalRangeContinuous: Boolean
        get() = getYearConfig(curYear)?.isLocaleFestivalRangeContinuous ?: false

    fun isLocaleHoliday(month: Int, day: Int): Boolean =
        getYearConfig(curYear)?.isLocaleHoliday(month, day) ?: false
}

/**
 * Information for rendering a single day in the calendar.
 */
data class DayInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val dayOfWeekNumber: Int, // 1=Mon..7=Sun
    val dayOfWeekAbbr: String, // MON..SUN
    val dayOfWeekChinese: String, // 周一..周日
    val isRestDay: Boolean,
    val isWorkDay: Boolean,
    val lunarText: String,
    val festivalName: String? = null,
    val isLunarFestival: Boolean = false,
    val solarTermName: String? = null,
    val lunarMonthAndDayText: String = "",
    val hasFestival: Boolean = false,
    val isHolidayWithRest: Boolean = false,
    val isLocaleHoliday: Boolean = false,
    val isExplicitLocaleHoliday: Boolean = false,
    val localeHolidayName: String? = null,
    val isToday: Boolean = false,
    val fullLunarText: String = ""
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
        get() = when {
            !festivalName.isNullOrBlank() -> festivalName
            !solarTermName.isNullOrBlank() -> solarTermName
            else -> lunarMonthAndDayText.ifEmpty { lunarText }
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
            val cal = LunarCalculator.getChinaCalendar()
            return CalendarDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}
