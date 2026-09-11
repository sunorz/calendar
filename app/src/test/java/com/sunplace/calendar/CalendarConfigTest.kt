package com.sunplace.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.RestDayMonth
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.util.LunarCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CalendarConfigTest {

    @Test
    fun testParseNextYearConfig() {
        val json = """
        {
            "curYear": 2027,
            "restDays": [
                {"m": 1, "d": [1]},
                {"m": 10, "d": [1, 2, 3, 4, 5, 6, 7]}
            ]
        }
        """.trimIndent()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)
        val config = repository.parseConfigJson(json)

        assertNotNull(config)
        assertEquals(2027, config?.curYear)
        assertEquals(2, config?.restDays?.size)
        assertEquals(1, config?.restDays?.find { it.m == 1 }?.d?.get(0))
        assertEquals(7, config?.restDays?.find { it.m == 10 }?.d?.size)
    }

    /**
     * User requirement verification:
     * For May 2027 (next year):
     * 1. Next config has restDays for month 5: [1, 2, 3]
     * 2. All weekends in 2027 must be cleared from default rest rules (turned into work days/black).
     * 3. Only 1~3 are rest days (red), 4~31 are all work days (black), including weekends (8, 9, 15, 16, 22, 23, 29, 30).
     */
    @Test
    fun testMay2027RestDaysOnly1To3And4To31AllBlack() {
        val mainConfig = CalendarConfig(curYear = 2026)
        val nextConfig = CalendarConfig(
            curYear = 2027,
            restDays = listOf(
                RestDayMonth(m = 1, d = listOf(1)),
                RestDayMonth(m = 5, d = listOf(1, 2, 3)),
                RestDayMonth(m = 10, d = listOf(1, 2, 3, 4, 5, 6, 7))
            )
        )

        val days = LunarCalculator.getMonthDays(2027, 5, mainConfig, nextConfig)
        assertEquals(31, days.size)

        // Days 1, 2, 3 must be rest days (isRestDay = true, red)
        assertTrue("May 1 should be rest day", days[0].isRestDay)
        assertTrue("May 2 should be rest day", days[1].isRestDay)
        assertTrue("May 3 should be rest day", days[2].isRestDay)

        // Days 4 to 31 must ALL be work days (isRestDay = false, black)
        for (i in 3 until 31) {
            val dayNumber = i + 1
            val dayInfo = days[i]
            assertFalse(
                "May $dayNumber (DOW=${dayInfo.dayOfWeekNumber}) MUST be a work day (black), but was marked rest day",
                dayInfo.isRestDay
            )
        }

        // Specifically assert the weekends in May 2027:
        // 2027-05-08 (Saturday), 2027-05-09 (Sunday)
        // 2027-05-15 (Saturday), 2027-05-16 (Sunday)
        // 2027-05-22 (Saturday), 2027-05-23 (Sunday)
        // 2027-05-29 (Saturday), 2027-05-30 (Sunday)
        val weekendDays = listOf(8, 9, 15, 16, 22, 23, 29, 30)
        for (w in weekendDays) {
            val dayInfo = days[w - 1]
            assertFalse("May $w is weekend but must be normal work day (black)", dayInfo.isRestDay)
        }
    }

    /**
     * Test when nextConfig is not yet loaded for 2027 (currentYear + 1):
     * All weekends should still be reset to work days (all black).
     */
    @Test
    fun testMay2027WithoutNextConfigDefaultsAllWorkDays() {
        val mainConfig = CalendarConfig(curYear = 2026)
        val days = LunarCalculator.getMonthDays(2027, 5, mainConfig, nextConfig = null)

        for (dayInfo in days) {
            assertFalse(
                "In next year without config, all days must be cleared to work days, but day ${dayInfo.day} was rest",
                dayInfo.isRestDay
            )
        }
    }

    /**
     * User requirement verification:
     * For 2027 localeFestivals: [{"m": 4, "d": [9]}]
     * 2027-04-09 must be marked as locale holiday ("三月三"), subtitle displayed as "三月三".
     */
    @Test
    fun test2027LocaleFestivalApril9IsGreenAndShowsSanyuesan() {
        val json = """
        {
            "curYear": 2027,
            "localeFestivals": [{"m": 4, "d": [9]}],
            "restDays": [{"m": 4, "d": [5]}]
        }
        """.trimIndent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)
        val nextConfig = repository.parseConfigJson(json)
        assertNotNull(nextConfig)
        assertEquals(1, nextConfig!!.localeFestivals.size)
        assertEquals(4, nextConfig.localeFestivals[0].m)
        assertEquals(listOf(9), nextConfig.localeFestivals[0].d)

        val mainConfig = CalendarConfig(curYear = 2026)
        val days = LunarCalculator.getMonthDays(2027, 4, mainConfig, nextConfig)
        val apr9 = days.find { it.day == 9 }
        assertNotNull(apr9)
        assertTrue("2027-04-09 must be isLocaleHoliday", apr9!!.isLocaleHoliday)
        assertEquals("三月三", apr9.localeHolidayName)
        val note = com.sunplace.calendar.util.TranslationHelper.getFilteredDisplayNote(apr9)
        assertTrue("Note should be '三月三' or 'Shangsi', but was '$note'", note == "三月三" || note == "Shangsi")
    }
}
