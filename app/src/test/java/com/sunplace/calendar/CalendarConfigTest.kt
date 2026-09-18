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
     * 1. Configured year (e.g. 2027 in config):
     *    Next config has restDays for month 5: [1, 2, 3]
     *    Only 1~3 are rest days (red), 4~31 are all work days (black), including regular weekends (8, 9, 15, 16, etc).
     */
    @Test
    fun testMay2027RestDaysOnly1To3And4To31AllBlack() {
        val mainConfig = CalendarConfig(
            curYear = 2026,
            yearlyConfigs = mapOf(
                2027 to com.sunplace.calendar.data.model.YearConfig(
                    year = 2027,
                    restDays = listOf(
                        RestDayMonth(m = 1, d = listOf(1)),
                        RestDayMonth(m = 5, d = listOf(1, 2, 3)),
                        RestDayMonth(m = 10, d = listOf(1, 2, 3, 4, 5, 6, 7))
                    )
                )
            )
        )

        val days = LunarCalculator.getMonthDays(2027, 5, mainConfig)
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
        val weekendDays = listOf(8, 9, 15, 16, 22, 23, 29, 30)
        for (w in weekendDays) {
            val dayInfo = days[w - 1]
            assertFalse("May $w is weekend but must be normal work day (black)", dayInfo.isRestDay)
        }
    }

    /**
     * User requirement verification:
     * Unconfigured year (未命中年份):
     * "未命中年份，保持周六和周日红色，其他日期黑色"
     * Weekends (Saturday=6, Sunday=7) must be Rest days (Red).
     * Weekdays (Monday..Friday) must be Work days (Black).
     */
    @Test
    fun testUnconfiguredYearDefaultsWeekendsRedAndWeekdaysBlack() {
        val mainConfig = CalendarConfig(curYear = 2026)
        // 2028 is unconfigured in mainConfig
        val daysMay2028 = LunarCalculator.getMonthDays(2028, 5, mainConfig)
        assertEquals(31, daysMay2028.size)

        for (dayInfo in daysMay2028) {
            val isWeekend = (dayInfo.dayOfWeekNumber == 6 || dayInfo.dayOfWeekNumber == 7)
            if (isWeekend) {
                assertTrue("Unconfigured year weekend (day ${dayInfo.day}) must be RED (rest day)", dayInfo.isRestDay)
            } else {
                assertFalse("Unconfigured year weekday (day ${dayInfo.day}) must be BLACK (work day)", dayInfo.isRestDay)
            }
        }
    }

    /**
     * Test single-file multi-year JSON format (android-app.json):
     * e.g. {"localeFestivals": {"26": [...], "27": [...]}, "restDays": {"26": [...], "27": [...]}}
     */
    @Test
    fun testSingleFileMultiYearJsonParsing() {
        val multiYearJson = """
        {
            "localeFestivals": {
                "26": [{"m": 4, "d": [17, 20]}],
                "27": [{"m": 4, "d": [16, 19]}]
            },
            "restDays": {
                "26": [
                    {"m": 1, "d": [1, 2, 3]},
                    {"m": 5, "d": [1, 2, 3, 4, 5]}
                ],
                "27": [
                    {"m": 1, "d": [1]},
                    {"m": 5, "d": [1, 2, 3]}
                ]
            }
        }
        """.trimIndent()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)
        val config = repository.parseConfigJson(multiYearJson)

        assertNotNull(config)
        assertTrue(config!!.hasYearConfig(2026))
        assertTrue(config.hasYearConfig(2027))
        assertFalse(config.hasYearConfig(2028))

        // 2026 config verification
        val cfg2026 = config.getYearConfig(2026)
        assertNotNull(cfg2026)
        assertTrue(cfg2026!!.isRestDay(5, 1))
        assertTrue(cfg2026.isRestDay(5, 5))
        assertFalse(cfg2026.isRestDay(5, 6))

        // 2027 config verification
        val cfg2027 = config.getYearConfig(2027)
        assertNotNull(cfg2027)
        assertTrue(cfg2027!!.isRestDay(5, 1))
        assertTrue(cfg2027.isRestDay(5, 3))
        assertFalse(cfg2027.isRestDay(5, 4))
        assertTrue(cfg2027.isExplicitLocaleHoliday(4, 16))
        assertTrue(cfg2027.isExplicitLocaleHoliday(4, 19))

        // In 2027 calendar rendering:
        val days2027May = LunarCalculator.getMonthDays(2027, 5, config)
        assertTrue("2027-05-01 is rest", days2027May[0].isRestDay)
        assertTrue("2027-05-03 is rest", days2027May[2].isRestDay)
        assertFalse("2027-05-04 is workday", days2027May[3].isRestDay)

        // In 2028 unconfigured calendar rendering:
        val days2028May = LunarCalculator.getMonthDays(2028, 5, config)
        val may6_2028 = days2028May.find { it.day == 6 }!! // Saturday in 2028-05
        val may5_2028 = days2028May.find { it.day == 5 }!! // Friday in 2028-05
        assertTrue("2028 Saturday must be Red (rest)", may6_2028.isRestDay)
        assertFalse("2028 Friday must be Black (workday)", may5_2028.isRestDay)
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
        assertEquals("三月三假日", apr9.localeHolidayName)
        val cardData = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(apr9)
        assertTrue("Card details must include '三月三假日' or 'Sanyuesan Festival'", cardData.combinedText.contains("三月三假日") || cardData.combinedText.contains("Sanyuesan"))
    }

    @Test
    fun testLocaleFestivalRangeFromFirstDayAToLastDayB() {
        // Range configured with day 17 and day 20 in localeFestivals, 18 and 19 in restDays
        val config = CalendarConfig(
            curYear = 2026,
            localeFestivals = listOf(
                com.sunplace.calendar.data.model.LocaleFestivalItem(m = 4, d = listOf(17, 20))
            ),
            restDays = listOf(
                com.sunplace.calendar.data.model.RestDayMonth(m = 4, d = listOf(18, 19))
            )
        )

        assertEquals(Pair(4, 17), config.localeFestivalFirstDay)
        assertEquals(Pair(4, 20), config.localeFestivalLastDay)

        // All days from 17 to 20 should be in locale holiday range
        for (day in 17..20) {
            assertTrue("Day $day must be in locale range", config.isInLocaleFestivalRange(4, day))
        }
        // Day 16 and 21 should NOT be in range
        assertFalse("Day 16 should not be in locale range", config.isInLocaleFestivalRange(4, 16))
        assertFalse("Day 21 should not be in locale range", config.isInLocaleFestivalRange(4, 21))

        // LunarCalculator getMonthDays verification
        val days = LunarCalculator.getMonthDays(2026, 4, config)

        // Day 17: Explicit in json localeFestivals -> Green (isExplicitLocaleHoliday=true)
        val d17 = days.find { it.day == 17 }!!
        assertTrue("Day 17 is explicitly locale holiday (Green)", d17.isExplicitLocaleHoliday)
        assertTrue("Day 17 is in locale holiday range (Card shows Sanyuesan)", d17.isLocaleHoliday)

        // Day 18: NOT in json localeFestivals, but is restDay -> Red (isRestDay=true, isExplicitLocaleHoliday=false)
        val d18 = days.find { it.day == 18 }!!
        assertFalse("Day 18 is not explicitly in localeFestivals (Not green)", d18.isExplicitLocaleHoliday)
        assertTrue("Day 18 is restDay (Red)", d18.isRestDay)
        assertTrue("Day 18 is in locale holiday range (Card shows Sanyuesan)", d18.isLocaleHoliday)

        // Day 19: NOT in json localeFestivals, but is restDay -> Red (isRestDay=true, isExplicitLocaleHoliday=false)
        val d19 = days.find { it.day == 19 }!!
        assertFalse("Day 19 is not explicitly in localeFestivals (Not green)", d19.isExplicitLocaleHoliday)
        assertTrue("Day 19 is restDay (Red)", d19.isRestDay)
        assertTrue("Day 19 is in locale holiday range (Card shows Sanyuesan)", d19.isLocaleHoliday)

        // Day 20: Explicit in json localeFestivals -> Green (isExplicitLocaleHoliday=true)
        val d20 = days.find { it.day == 20 }!!
        assertTrue("Day 20 is explicitly locale holiday (Green)", d20.isExplicitLocaleHoliday)
        assertTrue("Day 20 is in locale holiday range (Card shows Sanyuesan)", d20.isLocaleHoliday)

        for (d in 17..20) {
            val dayInfo = days.find { it.day == d }!!
            // Grid note should not be forced to 三月三
            val note = com.sunplace.calendar.util.TranslationHelper.getFilteredDisplayNote(dayInfo)
            assertFalse("Grid note must not be 三月三", note.contains("三月三"))

            // Card details MUST display 三月三假日 regardless of workday/restday
            val cardData = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(dayInfo)
            assertTrue("Card details for day $d must include 三月三假日", cardData.combinedText.contains("三月三假日") || cardData.combinedText.contains("Sanyuesan"))
        }
    }

    @Test
    fun testLocaleFestivalInterveningWorkdayDisablesRangeLogic() {
        // 17 and 20 are in localeFestivals, but 18 is a workday (NOT in restDays and NOT in localeFestivals)
        val configWithWorkday = CalendarConfig(
            curYear = 2026,
            localeFestivals = listOf(
                com.sunplace.calendar.data.model.LocaleFestivalItem(m = 4, d = listOf(17, 20))
            ),
            restDays = listOf(
                // Only 19 is rest day, 18 is a workday!
                com.sunplace.calendar.data.model.RestDayMonth(m = 4, d = listOf(19))
            )
        )

        // Continuous range check MUST return false
        assertFalse("Continuous range logic must be disabled due to workday on April 18", configWithWorkday.isLocaleFestivalRangeContinuous)

        // Day 17: Explicit in localeFestivals -> isLocaleHoliday = true
        assertTrue(configWithWorkday.isLocaleHoliday(4, 17))
        // Day 18: Workday -> isLocaleHoliday = false (disabled!)
        assertFalse("Day 18 is a workday, cannot be locale holiday", configWithWorkday.isLocaleHoliday(4, 18))
        // Day 19: Not explicitly in localeFestivals -> isLocaleHoliday = false (because range logic disabled)
        assertFalse("Day 19 is rest day but range logic disabled, cannot be locale holiday", configWithWorkday.isLocaleHoliday(4, 19))
        // Day 20: Explicit in localeFestivals -> isLocaleHoliday = true
        assertTrue(configWithWorkday.isLocaleHoliday(4, 20))

        val days = LunarCalculator.getMonthDays(2026, 4, configWithWorkday)
        val d17 = days.find { it.day == 17 }!!
        val d18 = days.find { it.day == 18 }!!
        val d19 = days.find { it.day == 19 }!!
        val d20 = days.find { it.day == 20 }!!

        assertTrue("Day 17 is locale holiday", d17.isLocaleHoliday)
        assertFalse("Day 18 must NOT be locale holiday", d18.isLocaleHoliday)
        assertFalse("Day 19 must NOT be locale holiday", d19.isLocaleHoliday)
        assertTrue("Day 20 is locale holiday", d20.isLocaleHoliday)
    }

    @Test
    fun testLocaleFestivalCrossMonthRange() {
        // Cross-month configuration: e.g. from March 30 (3.30) to April 2 (4.02)
        val crossMonthConfig = CalendarConfig(
            curYear = 2026,
            localeFestivals = listOf(
                com.sunplace.calendar.data.model.LocaleFestivalItem(m = 3, d = listOf(30, 31)),
                com.sunplace.calendar.data.model.LocaleFestivalItem(m = 4, d = listOf(1, 2))
            )
        )

        assertEquals(Pair(3, 30), crossMonthConfig.localeFestivalFirstDay)
        assertEquals(Pair(4, 2), crossMonthConfig.localeFestivalLastDay)

        // March checks: 3.29 (false), 3.30 (true), 3.31 (true)
        assertFalse(crossMonthConfig.isInLocaleFestivalRange(3, 29))
        assertTrue(crossMonthConfig.isInLocaleFestivalRange(3, 30))
        assertTrue(crossMonthConfig.isInLocaleFestivalRange(3, 31))

        // April checks: 4.01 (true), 4.02 (true), 4.03 (false)
        assertTrue(crossMonthConfig.isInLocaleFestivalRange(4, 1))
        assertTrue(crossMonthConfig.isInLocaleFestivalRange(4, 2))
        assertFalse(crossMonthConfig.isInLocaleFestivalRange(4, 3))

        // Check LunarCalculator generation for March
        val marchDays = LunarCalculator.getMonthDays(2026, 3, crossMonthConfig)
        val mar30 = marchDays.find { it.day == 30 }!!
        val mar31 = marchDays.find { it.day == 31 }!!
        assertTrue("March 30 must be locale holiday", mar30.isLocaleHoliday)
        assertTrue("March 31 must be locale holiday", mar31.isLocaleHoliday)
        val cardMar30 = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(mar30).combinedText
        assertTrue("Card for March 30 includes 三月三假日 or Sanyuesan", cardMar30.contains("三月三假日") || cardMar30.contains("Sanyuesan"))

        // Check LunarCalculator generation for April
        val aprilDays = LunarCalculator.getMonthDays(2026, 4, crossMonthConfig)
        val apr1 = aprilDays.find { it.day == 1 }!!
        val apr2 = aprilDays.find { it.day == 2 }!!
        val apr3 = aprilDays.find { it.day == 3 }!!
        assertTrue("April 1 must be locale holiday", apr1.isLocaleHoliday)
        assertTrue("April 2 must be locale holiday", apr2.isLocaleHoliday)
        assertFalse("April 3 must NOT be locale holiday", apr3.isLocaleHoliday)
        val cardApr1 = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(apr1).combinedText
        assertTrue("Card for April 1 includes 三月三假日 or Sanyuesan", cardApr1.contains("三月三假日") || cardApr1.contains("Sanyuesan"))
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
            "restDays": [
                {"m": 14, "d": [1, 2, 3]},
                {"m": 5, "d": [1, 2, 32, 99, 3]}
            ],
            "localeFestivals": [
                {"m": 14, "d": [17, 20]},
                {"m": 4, "d": [17, 32, 18]}
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

    /**
     * Test Guangxi Sanyuesan toggle behavior:
     * When showGuangxiSanyuesan is true (default):
     *  - Day 17 is green (explicitLocaleHoliday)
     *  - Day 18 is red (restDay)
     *  - Day 19 is red (restDay)
     *  - Day 20 is green (explicitLocaleHoliday)
     *  - Days 17-20 show "三月三假日" in card
     * When showGuangxiSanyuesan is false (disabled):
     *  - Day 17 and 20 are black workdays (isExplicitLocaleHoliday = false, isLocaleHoliday = false, isRestDay = false)
     *  - Day 18 and 19 remain standard red rest days (isRestDay = true)
     *  - None of 17-20 show "三月三假日"
     */
    @Test
    fun testGuangxiSanyuesanToggleEnabledAndDisabled() {
        val config = CalendarConfig(
            curYear = 2026,
            localeFestivals = listOf(
                com.sunplace.calendar.data.model.LocaleFestivalItem(m = 4, d = listOf(17, 20))
            ),
            restDays = listOf(
                com.sunplace.calendar.data.model.RestDayMonth(m = 4, d = listOf(18, 19))
            )
        )

        // 1. When enabled (true)
        val enabledDays = LunarCalculator.getMonthDays(2026, 4, config, null, showGuangxiSanyuesan = true)
        val day17E = enabledDays.find { it.day == 17 }!!
        val day18E = enabledDays.find { it.day == 18 }!!
        val day19E = enabledDays.find { it.day == 19 }!!
        val day20E = enabledDays.find { it.day == 20 }!!

        assertTrue("17 must be explicitLocaleHoliday", day17E.isExplicitLocaleHoliday)
        assertFalse("18 is restDay not explicitLocaleHoliday", day18E.isExplicitLocaleHoliday)
        assertTrue("18 is restDay", day18E.isRestDay)
        assertFalse("19 is restDay not explicitLocaleHoliday", day19E.isExplicitLocaleHoliday)
        assertTrue("19 is restDay", day19E.isRestDay)
        assertTrue("20 must be explicitLocaleHoliday", day20E.isExplicitLocaleHoliday)

        // Card should display "三月三假日" for all 17~20
        assertTrue(day17E.isLocaleHoliday)
        assertTrue(day18E.isLocaleHoliday)
        assertTrue(day19E.isLocaleHoliday)
        assertTrue(day20E.isLocaleHoliday)

        // Day 19 is lunar 3-3 (2026-04-19 is Lunar Year 2026, Month 3, Day 3)
        // When enabled, festivalName contains 三月三
        assertEquals("三月三", day19E.festivalName)

        // 2. When disabled (false)
        val disabledDays = LunarCalculator.getMonthDays(2026, 4, config, null, showGuangxiSanyuesan = false)
        val day17D = disabledDays.find { it.day == 17 }!!
        val day18D = disabledDays.find { it.day == 18 }!!
        val day19D = disabledDays.find { it.day == 19 }!!
        val day20D = disabledDays.find { it.day == 20 }!!

        // Green highlights removed completely
        assertFalse("17 must NOT be explicitLocaleHoliday", day17D.isExplicitLocaleHoliday)
        assertFalse("17 must NOT be localeHoliday", day17D.isLocaleHoliday)
        assertFalse("17 must revert to work day (black)", day17D.isRestDay)

        assertFalse("18 must NOT be explicitLocaleHoliday", day18D.isExplicitLocaleHoliday)
        assertFalse("18 must NOT be localeHoliday", day18D.isLocaleHoliday)
        assertTrue("18 must remain standard rest day (red)", day18D.isRestDay)

        assertFalse("19 must NOT be explicitLocaleHoliday", day19D.isExplicitLocaleHoliday)
        assertFalse("19 must NOT be localeHoliday", day19D.isLocaleHoliday)
        assertTrue("19 must remain standard rest day (red)", day19D.isRestDay)

        assertFalse("20 must NOT be explicitLocaleHoliday", day20D.isExplicitLocaleHoliday)
        assertFalse("20 must NOT be localeHoliday", day20D.isLocaleHoliday)
        assertFalse("20 must revert to work day (black)", day20D.isRestDay)

        // Check Lunar 3-3 date (Day 19) when disabled:
        // 1. festivalName should NOT contain 三月三
        assertNull("Day 19 festivalName must be null when disabled", day19D.festivalName)
        // 2. Grid note subtitle must NOT contain 三月三 / Sanyuesan, must show standard lunar day (初三 in CN / 3rd in EN)
        val gridNote19 = com.sunplace.calendar.util.TranslationHelper.getFilteredDisplayNote(day19D)
        assertTrue("Grid note must be 初三 or 3rd, but was: $gridNote19", gridNote19 == "初三" || gridNote19 == "3rd")
        assertFalse("Grid note must NOT contain 三月三", gridNote19.contains("三月三"))
        assertFalse("Grid note must NOT contain Sanyuesan", gridNote19.contains("Sanyuesan"))
        // 3. Card lunar priority data must NOT contain 三月三 or Sanyuesan
        val cardData19 = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(day19D)
        assertNull("Card festivalText must be null", cardData19.festivalText)
        assertFalse("Card combinedText must NOT contain 三月三", cardData19.combinedText.contains("三月三"))
        assertFalse("Card combinedText must NOT contain Sanyuesan", cardData19.combinedText.contains("Sanyuesan"))
    }
}
