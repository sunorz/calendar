package com.sunplace.calendar

import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.LocaleFestivalItem
import com.sunplace.calendar.data.model.RestDayMonth
import com.sunplace.calendar.util.LunarCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LunarCalculatorTest {

    private lateinit var sampleConfig: CalendarConfig

    @Before
    fun setUp() {
        // Streamlined config containing curYear, localeFestivals, and restDays
        sampleConfig = CalendarConfig(
            curYear = 2026,
            localeFestivals = listOf(LocaleFestivalItem(4, listOf(17, 20))),
            restDays = listOf(
                RestDayMonth(1, listOf(1, 2, 3, 10, 11, 17, 18, 24, 25, 31)),
                RestDayMonth(2, listOf(1, 7, 8, 15, 16, 17, 18, 19, 20, 21, 22, 23)),
                RestDayMonth(4, listOf(4, 5, 6, 11, 12, 18, 19, 25, 26))
            )
        )
    }

    @Test
    fun testAugust31_2026_LunarDate() {
        val lunar = LunarCalculator.getLunarDate(2026, 8, 31)
        assertEquals(7, lunar.lunarMonth)
        assertEquals(19, lunar.lunarDay)
    }

    // ==================== Config Year (2026) Tests ====================

    @Test
    fun testConfigYearRestDaysAndNumberColor() {
        val janDays = LunarCalculator.getMonthDays(2026, 1, sampleConfig)
        val jan1 = janDays.find { it.day == 1 }!!
        // 2026-01-01 is New Year's Day (元旦) and in restDays -> isRestDay=true, isHolidayWithRest=true
        assertTrue(jan1.isRestDay)
        assertFalse(jan1.isWorkDay)
        assertEquals("元旦", jan1.displayNote)
        assertTrue(jan1.isHolidayWithRest)

        // Jan 4, 2026 is Sunday, but NOT in restDays -> workday (number is black, workDay=true)
        val jan4 = janDays.find { it.day == 4 }!!
        assertFalse(jan4.isRestDay)
        assertTrue(jan4.isWorkDay)
        assertFalse(jan4.isHolidayWithRest)

        // Jan 10, 2026 is Saturday and in restDays, but NO holiday -> isRestDay=true, isHolidayWithRest=false (subtitle NOT red)
        val jan10 = janDays.find { it.day == 10 }!!
        assertTrue(jan10.isRestDay)
        // 1-10 is 警察节 (built-in solar festival)
        assertEquals("警察节", jan10.displayNote)
    }

    @Test
    fun testConfigYearLunarFestivals() {
        val febDays = LunarCalculator.getMonthDays(2026, 2, sampleConfig)
        val feb16 = febDays.find { it.day == 16 }!!
        val feb17 = febDays.find { it.day == 17 }!!
        assertEquals("除夕", feb16.displayNote)
        assertTrue(feb16.isRestDay)
        assertTrue(feb16.isHolidayWithRest)

        assertEquals("春节", feb17.displayNote)
        assertTrue(feb17.isRestDay)
        assertTrue(feb17.isHolidayWithRest)
    }

    @Test
    fun testLocaleFestivalsGreenBadge() {
        // In 2026 config, 4-17 and 4-20 are in localeFestivals ("三月三")
        val aprDays = LunarCalculator.getMonthDays(2026, 4, sampleConfig)
        val apr17 = aprDays.find { it.day == 17 }!!
        assertTrue(apr17.isLocaleHoliday)
        assertEquals("三月三假日", apr17.localeHolidayName)
        assertFalse(apr17.displayNote.contains("三月三"))

        // Calendar grid subtitle MUST NOT display 三月三
        val gridNote17 = com.sunplace.calendar.util.TranslationHelper.getFilteredDisplayNote(apr17)
        assertFalse("Calendar grid note must NOT be 三月三", gridNote17.contains("三月三"))

        // Detailed card MUST include 三月三假日 (or Sanyuesan Festival in English)
        val cardData17 = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(apr17)
        assertTrue("Card details must include 三月三假日 or Sanyuesan", cardData17.combinedText.contains("三月三假日") || cardData17.combinedText.contains("Sanyuesan"))

        val apr20 = aprDays.find { it.day == 20 }!!
        assertTrue(apr20.isLocaleHoliday)
        assertEquals("三月三假日", apr20.localeHolidayName)
        assertFalse(apr20.displayNote.contains("三月三"))

        val gridNote20 = com.sunplace.calendar.util.TranslationHelper.getFilteredDisplayNote(apr20)
        assertFalse("Calendar grid note must NOT be 三月三", gridNote20.contains("三月三"))

        val cardData20 = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(apr20)
        assertTrue("Card details must include 三月三假日 or Sanyuesan", cardData20.combinedText.contains("三月三假日") || cardData20.combinedText.contains("Sanyuesan"))
    }

    // ==================== Non-Hit Years (e.g. 2025, 2024, 2027) Default Rules ====================

    @Test
    fun testNonHitYearDefaultsToWeekendRestDays() {
        // In 2025, sampleConfig is for 2026, so 2025 is a non-hit year.
        // It must follow system defaults: Saturday & Sunday are rest days, weekdays are workdays.
        val jan2025Days = LunarCalculator.getMonthDays(2025, 1, sampleConfig)

        // 2025-01-01 is Wednesday -> Workday
        val jan1 = jan2025Days.find { it.day == 1 }!!
        assertFalse(jan1.isRestDay)
        assertTrue(jan1.isWorkDay)
        assertEquals("元旦", jan1.displayNote)
        // Since it is a workday (not in weekend), subtitle is NOT red (isHolidayWithRest is false)
        assertFalse(jan1.isHolidayWithRest)

        // 2025-01-04 is Saturday -> Rest day (Weekend)
        val jan4 = jan2025Days.find { it.day == 4 }!!
        assertTrue(jan4.isRestDay)
        assertFalse(jan4.isWorkDay)
        // No festival on this day -> subtitle is NOT red
        assertFalse(jan4.isHolidayWithRest)

        // 2025-01-05 is Sunday -> Rest day (Weekend)
        val jan5 = jan2025Days.find { it.day == 5 }!!
        assertTrue(jan5.isRestDay)
        assertFalse(jan5.isWorkDay)
        // Note: 2025-01-05 is 小寒 (24 solar term) and it is on a Sunday, but NOT a statutory holiday, so isHolidayWithRest is false
        assertFalse(jan5.isHolidayWithRest)
    }

    @Test
    fun testNonHitYear2025LunarFestivals() {
        val jan2025Days = LunarCalculator.getMonthDays(2025, 1, sampleConfig)
        val jan28 = jan2025Days.find { it.day == 28 }!!
        val jan29 = jan2025Days.find { it.day == 29 }!!
        val l28 = LunarCalculator.getLunarDate(2025, 1, 28)
        val l29 = LunarCalculator.getLunarDate(2025, 1, 29)
        // Check whichever day is 春节 / 除夕
        if (l29.lunarMonth == 1 && l29.lunarDay == 1) {
            assertEquals("除夕", jan28.displayNote)
            assertEquals("春节", jan29.displayNote)
        } else {
            assertEquals("春节", jan28.displayNote)
        }

        // 2025 Dragon Boat Festival
        val may2025Days = LunarCalculator.getMonthDays(2025, 5, sampleConfig)
        val may31 = may2025Days.find { it.day == 31 }!!
        assertTrue(may31.isRestDay) // Saturday

        // 2025 Mid-Autumn Festival (中秋节: 八月十五) is 2025-10-05 (Sunday)
        val oct2025Days = LunarCalculator.getMonthDays(2025, 10, sampleConfig)
        val oct5 = oct2025Days.find { it.day == 5 }!!
        assertEquals("中秋节", oct5.displayNote)
        assertTrue(oct5.isRestDay) // Sunday
        assertTrue(oct5.isHolidayWithRest) // Mid-Autumn is statutory holiday
    }

    @Test
    fun testNonHitYear24SolarTerms() {
        // 2025 Qingming (清明) is 2025-04-04
        val apr2025Days = LunarCalculator.getMonthDays(2025, 4, sampleConfig)
        val apr4 = apr2025Days.find { it.day == 4 }!!
        assertEquals("清明", apr4.displayNote)

        // 2025 Winter Solstice (冬至) is 2025-12-21 (Sunday)
        val dec2025Days = LunarCalculator.getMonthDays(2025, 12, sampleConfig)
        val dec21 = dec2025Days.find { it.day == 21 }!!
        assertEquals("冬至", dec21.displayNote)
        assertTrue(dec21.isRestDay) // Sunday
        assertFalse(dec21.isHolidayWithRest) // Winter Solstice is not statutory holiday
    }

    @Test
    fun testNonHitYearDynamicSolarFestivals() {
        // 2025 Mother's Day: 2nd Sunday of May -> 2025-05-11
        val may2025Days = LunarCalculator.getMonthDays(2025, 5, sampleConfig)
        val may11 = may2025Days.find { it.day == 11 }!!
        assertEquals("母亲节", may11.displayNote)
        assertTrue(may11.isRestDay) // Sunday
        assertFalse(may11.isHolidayWithRest)

        // 2025 Father's Day: 3rd Sunday of June -> 2025-06-15
        val jun2025Days = LunarCalculator.getMonthDays(2025, 6, sampleConfig)
        val jun15 = jun2025Days.find { it.day == 15 }!!
        assertEquals("父亲节", jun15.displayNote)
        assertTrue(jun15.isRestDay) // Sunday
        assertFalse(jun15.isHolidayWithRest)
    }

    @Test
    fun testWidgetNotePriority() {
        // 1. Festival priority (e.g. 2026-10-01 National Day / 国庆节)
        val oct1 = LunarCalculator.getDayInfoForDate(2026, 10, 1, sampleConfig)
        assertEquals("国庆节", oct1.widgetNote)

        // 2. 24 Solar Term priority when no festival (e.g. 2026-02-04 Lichun / 立春)
        val feb4 = LunarCalculator.getDayInfoForDate(2026, 2, 4, sampleConfig)
        assertEquals("立春", feb4.widgetNote)

        // 3. Lunar Date with Month when no festival and no solar term (e.g. 2026-08-31 is Lunar 7-19 -> 七月十九)
        val aug31 = LunarCalculator.getDayInfoForDate(2026, 8, 31, sampleConfig)
        assertEquals("七月十九", aug31.widgetNote)

        // Verify lunar month + day format (e.g. 三月廿二)
        val text3_22 = LunarCalculator.getLunarMonthAndDayText(3, 22, false)
        assertEquals("三月廿二", text3_22)
    }

    @Test
    fun testCardLunarPriorityData() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            // Chinese Mode test
            java.util.Locale.setDefault(java.util.Locale.SIMPLIFIED_CHINESE)
            val bailuDayCn = LunarCalculator.getDayInfoForDate(2026, 9, 7, sampleConfig)
            val bailuCardCn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(bailuDayCn)
            assertFalse(bailuCardCn.hasFestival)
            assertTrue(bailuCardCn.hasSolarTerm)
            assertEquals("白露", bailuCardCn.solarTermText)

            val midAutumnDayCn = LunarCalculator.getDayInfoForDate(2026, 9, 25, sampleConfig)
            val midAutumnCardCn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(midAutumnDayCn)
            assertTrue(midAutumnCardCn.hasFestival)
            assertEquals("中秋节", midAutumnCardCn.festivalText)

            val normalDayCn = LunarCalculator.getDayInfoForDate(2026, 9, 4, sampleConfig)
            val normalCardCn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(normalDayCn)
            assertFalse(normalCardCn.hasFestival)
            assertFalse(normalCardCn.hasSolarTerm)
            assertTrue(normalCardCn.ordinaryLunarText.isNotBlank())

            val nationalDayCn = LunarCalculator.getDayInfoForDate(2026, 10, 1, sampleConfig)
            val nationalCardCn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(nationalDayCn)
            assertTrue(nationalCardCn.hasFestival)
            assertFalse(nationalCardCn.isLunarFestival)
            assertEquals("国庆节", nationalCardCn.festivalText)

            // English Mode test
            java.util.Locale.setDefault(java.util.Locale.US)
            val bailuDayEn = LunarCalculator.getDayInfoForDate(2026, 9, 7, sampleConfig)
            val bailuCardEn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(bailuDayEn)
            assertFalse(bailuCardEn.hasFestival)
            assertTrue(bailuCardEn.hasSolarTerm)
            assertEquals("White Dew", bailuCardEn.solarTermText)

            val midAutumnDayEn = LunarCalculator.getDayInfoForDate(2026, 9, 25, sampleConfig)
            val midAutumnCardEn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(midAutumnDayEn)
            assertTrue(midAutumnCardEn.hasFestival)
            assertEquals("Mid-Autumn Festival", midAutumnCardEn.festivalText)

            val normalDayEn = LunarCalculator.getDayInfoForDate(2026, 9, 4, sampleConfig)
            val normalCardEn = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(normalDayEn)
            assertFalse(normalCardEn.hasFestival)
            assertFalse(normalCardEn.hasSolarTerm)
            assertTrue(normalCardEn.ordinaryLunarText.isNotBlank())
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }
}
