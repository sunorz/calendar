package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.CalendarRepository
import com.example.util.LunarCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertTrue(appName == "调休日历" || appName == "CN ShiftCal")
  }

  @Test
  fun `verify rest days and work days in config`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CalendarRepository(context)
    val config = repository.configState.value

    // Since we removed the local fallback JSON for security, the initial config will be empty.
    // The test was assuming the full 2026 calendar was bundled.
    assertNotNull(config)
    
    // We can simulate parsing a JSON string directly to test the parsing logic instead
    val mockJson = """
    {
        "curYear" : 2026 ,
        "startWeek" : 4 ,
        "restDays" : [
            {"m" : 5,"d" : [1,2,3,4,5,10,16,17,23,24,30,31]}
        ],
        "localeFestivals" :[{"m" : 4,"d" : [17,20]}]
    }
    """.trimIndent()
    
    val parsedConfig = repository.parseConfigJson(mockJson)
    assertNotNull(parsedConfig)
    assertEquals(2026, parsedConfig!!.curYear)

    // May 1st (Labor day) should be Rest day (Red) based on mock JSON
    assertTrue("May 1 should be a rest day", parsedConfig.isRestDay(5, 1))
    assertFalse("May 1 should not be a work day", parsedConfig.isWorkDay(5, 1))

    // May 6th should be Work day (Black) based on mock JSON
    assertTrue("May 6 should be a work day", parsedConfig.isWorkDay(5, 6))

    // Verify day of week abbreviation
    val dayInfoMay1 = LunarCalculator.getDayInfoForDate(2026, 5, 1, parsedConfig)
    assertEquals("FRI", dayInfoMay1.dayOfWeekAbbr)
    assertTrue(dayInfoMay1.isRestDay)
  }
}

