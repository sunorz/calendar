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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.sunplace.calendar.data.repository.SyncState
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

    /**
     * Fallback verification:
     * When receiving malformed JSON (HTML, truncated, missing braces, garbage):
     * parseConfigJson must return null safely without throwing uncaught exceptions.
     */
    @Test
    fun testMalformedJsonFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)

        // 1. HTML error response (e.g. 403 Forbidden / Wi-Fi portal)
        val htmlResponse = "<html><head><title>403 Forbidden</title></head><body>Forbidden</body></html>"
        val parsedHtml = repository.parseConfigJson(htmlResponse)
        org.junit.Assert.assertNull("HTML content must return null", parsedHtml)

        // 2. Truncated JSON
        val truncatedJson = "{\"curYear\": 2026, \"restDays\": ["
        val parsedTruncated = repository.parseConfigJson(truncatedJson)
        org.junit.Assert.assertNull("Truncated JSON must return null", parsedTruncated)

        // 3. Non-JSON string or empty/blank
        org.junit.Assert.assertNull(repository.parseConfigJson(""))
        org.junit.Assert.assertNull(repository.parseConfigJson("   "))
        org.junit.Assert.assertNull(repository.parseConfigJson(null))
        org.junit.Assert.assertNull(repository.parseConfigJson("Not a json at all"))

        // 4. Invalid year range silently fallbacks to 2026 without throwing error or failing
        val invalidYearJson = "{\"curYear\": 1200}"
        val parsedInvalidYear = repository.parseConfigJson(invalidYearJson)
        assertNotNull(parsedInvalidYear)
        assertEquals(2026, parsedInvalidYear?.curYear)
    }

    /**
     * Requirement verification:
     * If JSON contains month 14 or day 32 (or any invalid month/day),
     * the code MUST silently ignore them without throwing any error or showing errors to the user.
     */
    @Test
    fun testIgnoreMonth14AndDay32Silently() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)

        val jsonWithMonth14AndDay32 = """
        {
            "curYear": 2026,
            "startWeek": 4,
            "restDays": [
                {"m": 14, "d": [1, 2, 3]},
                {"m": 5, "d": [1, 2, 32, 99, 3]}
            ],
            "localeFestivals": [
                {"m": 14, "d": [17, 20]},
                {"m": 4, "d": [17, 32, 18]}
            ],
            "Festivals": [
                {"date": "14-5", "name": "无效14月节"},
                {"date": "5-32", "name": "无效32日节"},
                {"date": "5-1", "name": "劳动节", "tag": 1}
            ],
            "lunarFestivals": [
                {"date": "14-1", "name": "无效14月农历节"},
                {"date": "1-35", "name": "无效35日农历节"},
                {"date": "1-29", "name": "除夕", "tag": 0}
            ]
        }
        """.trimIndent()

        val config = repository.parseConfigJson(jsonWithMonth14AndDay32)
        assertNotNull("Config must be parsed successfully, never returning null on month 14 or day 32", config)

        // 1. restDays: Month 14 must be completely ignored; Day 32 and 99 in month 5 must be ignored
        assertEquals(1, config?.restDays?.size)
        val mayRestDays = config?.restDays?.find { it.m == 5 }
        assertNotNull("May (month 5) rest days should exist", mayRestDays)
        assertEquals(listOf(1, 2, 3), mayRestDays?.d)
        assertFalse("Day 32 must be ignored", mayRestDays?.d?.contains(32) == true)
        assertFalse("Day 99 must be ignored", mayRestDays?.d?.contains(99) == true)
        assertNull("Month 14 must be ignored", config?.restDays?.find { it.m == 14 })

        // 2. localeFestivals: Month 14 must be completely ignored; Day 32 in month 4 must be ignored
        assertEquals(1, config?.localeFestivals?.size)
        val aprLocale = config?.localeFestivals?.find { it.m == 4 }
        assertNotNull("April locale festivals should exist", aprLocale)
        assertEquals(listOf(17, 18), aprLocale?.d)
        assertFalse("Day 32 must be ignored", aprLocale?.d?.contains(32) == true)
        assertNull("Month 14 must be ignored", config?.localeFestivals?.find { it.m == 14 })

        // 3. Festivals: 14-5 and 5-32 must be silently ignored, only 5-1 remains
        assertEquals(1, config?.festivals?.size)
        assertEquals("5-1", config?.festivals?.get(0)?.date)
        assertEquals("劳动节", config?.festivals?.get(0)?.name)

        // 4. lunarFestivals: 14-1 and 1-35 must be silently ignored, only 1-29 remains
        assertEquals(1, config?.lunarFestivals?.size)
        assertEquals("1-29", config?.lunarFestivals?.get(0)?.date)
        assertEquals("除夕", config?.lunarFestivals?.get(0)?.name)
    }

    /**
     * Requirement verification:
     * Never show any error to the user under any failure condition.
     */
    @Test
    fun testNetworkFailureSilentlyUsesFallbackWithoutErrorState() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)

        // Force request to an unreachable domain
        val result = repository.fetchRemoteConfig("https://127.0.0.1:9999/non_existent_config.json")
        assertTrue("Repository fetch must return success with fallback config", result.isSuccess)
        assertNotNull(result.getOrNull())

        // SyncState must NOT be SyncState.Error under any circumstance
        val state = repository.syncState.value
        assertFalse("SyncState must NEVER be Error for user", state is SyncState.Error)
    }

    /**
     * Fallback verification:
     * When device has no internet and no prior cache,
     * repository must safely initialize with embedded 2026 and 2027 fallback configurations.
     */
    @Test
    fun testInitialOfflineEmbeddedFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Ensure shared preferences are clear
        context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val repository = CalendarRepository(context)
        val mainConfig = repository.configState.value
        val nextConfig = repository.nextYearConfigState.value

        assertNotNull("Main config must not be null even without network/cache", mainConfig)
        assertEquals(2026, mainConfig.curYear)
        assertTrue("Embedded 2026 config must have restDays", mainConfig.restDays.isNotEmpty())
        assertTrue("Embedded 2026 config must have festivals", mainConfig.festivals.isNotEmpty())

        assertNotNull("Next config must not be null even without network/cache", nextConfig)
        assertEquals(2027, nextConfig?.curYear)
        assertTrue("Embedded 2027 config must have restDays", nextConfig?.restDays?.isNotEmpty() == true)
    }

    /**
     * Fallback verification:
     * Partially corrupted items inside arrays should be gracefully skipped without failing the whole valid JSON.
     */
    @Test
    fun testJsonTolerantParsingWithDirtyArrayItems() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)

        val dirtyJson = """
        {
            "curYear": 2026,
            "startWeek": 4,
            "monthsLimit": [31, 28, 31],
            "restDays": [
                {"m": 1, "d": [1, 2, 3]},
                "corrupted_element",
                null,
                {"m": 0, "d": [10]},
                {"m": 2, "d": [15, 16]}
            ]
        }
        """.trimIndent()

        val config = repository.parseConfigJson(dirtyJson)
        assertNotNull(config)
        assertEquals(2026, config?.curYear)
        // Corrupted elements skipped, month 1 and month 2 parsed safely
        assertEquals(2, config?.restDays?.size)
        assertEquals(1, config?.restDays?.get(0)?.m)
        assertEquals(2, config?.restDays?.get(1)?.m)
    }
}
