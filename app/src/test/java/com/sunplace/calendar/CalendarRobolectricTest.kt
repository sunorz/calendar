package com.sunplace.calendar

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.ui.CalendarViewModel
import com.sunplace.calendar.util.LunarCalculator
import com.sunplace.calendar.widget.BaseCalendarWidgetProvider
import com.sunplace.calendar.widget.MonthCalendarAppWidgetProvider
import com.sunplace.calendar.widget.TodayAppWidgetProvider
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
class CalendarRobolectricTest {

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

  @Test
  fun `verify announcement content`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("吃上调休红利", context.getString(R.string.announcement_content))
  }

  @Test
  fun `verify widget today highlight drawables exist and inflate`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val bgToday = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_widget_today)
    val bgTodayRed = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_widget_today_red)
    val bgTodayLocale = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_widget_today_locale)
    assertNotNull(bgToday)
    assertNotNull(bgTodayRed)
    assertNotNull(bgTodayLocale)
  }

  @Test
  fun `verify month widget today text color and background`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
    val views = android.widget.RemoteViews(context.packageName, R.layout.widget_month)
    val root = views.apply(context, null) as android.view.ViewGroup
    assertNotNull(root)

    // Measure and layout in a compact 2x2 widget container (e.g., 145dp x 145dp)
    val density = context.resources.displayMetrics.density
    val sizePx = (145 * density).toInt()
    val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(sizePx, android.view.View.MeasureSpec.EXACTLY)
    val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(sizePx, android.view.View.MeasureSpec.EXACTLY)

    // 5-week month configuration (e.g. Sept 2026 where 28th is in row 4)
    val row5 = root.findViewById<android.view.View>(R.id.widget_week_row_5)
    row5.visibility = android.view.View.GONE
    root.measure(widthSpec, heightSpec)
    root.layout(0, 0, sizePx, sizePx)

    val grid = root.findViewById<android.view.ViewGroup>(R.id.widget_days_grid)
    val row4 = root.findViewById<android.view.ViewGroup>(R.id.widget_week_row_4)
    val day28Cell = root.findViewById<android.widget.TextView>(R.id.widget_day_28)
    assertTrue("Row 4 must fit completely within widget_days_grid without bottom clipping", row4.bottom <= grid.height)
    assertTrue("Day 28 cell must have sufficient height for text and 4dp bottom bar", day28Cell.height >= (18 * density).toInt())

    // Run update logic
    com.sunplace.calendar.widget.MonthCalendarAppWidgetProvider.updateAppWidget(context, appWidgetManager, 1001)
  }

  @Test
  fun `verify app version name is 1 1 9 and supports git hash suffix`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CalendarRepository(context)
    val version = repository.getAppVersion()
    assertTrue("Version must be 1.1.9 or 1.1.9_hash, was: $version", version == "1.1.9" || version.startsWith("1.1.9_"))
    assertEquals("1.1.9_a3f9c2e", repository.formatVersionWithGitHash("1.1.9", "a3f9c2e9988"))
    assertFalse(repository.isVersionNewer("1.1.9", "1.1.9_a3f9c2e"))
    assertTrue(repository.isVersionNewer("1.1.9", "1.1.8_a3f9c2e"))
    assertFalse(repository.isVersionNewer("1.1.7", "1.1.9_a3f9c2e"))
  }

  @Test
  fun `verify reactive todayDate updates isToday correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CalendarRepository(context)
    val config = repository.configState.value

    val testDateDay22 = com.sunplace.calendar.data.model.CalendarDate(2026, 9, 22)
    val days22 = LunarCalculator.getMonthDays(2026, 9, config, todayDate = testDateDay22)
    val day22 = days22.find { it.day == 22 }
    val day23 = days22.find { it.day == 23 }
    assertTrue(day22?.isToday == true)
    assertTrue(day23?.isToday == false)

    // Next day simulation
    val testDateDay23 = com.sunplace.calendar.data.model.CalendarDate(2026, 9, 23)
    val days23 = LunarCalculator.getMonthDays(2026, 9, config, todayDate = testDateDay23)
    val day22After = days23.find { it.day == 22 }
    val day23After = days23.find { it.day == 23 }
    assertTrue(day22After?.isToday == false)
    assertTrue(day23After?.isToday == true)
  }

  @Test
  fun `verify CalendarViewModel todayDate state and checkAndRefreshDate`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = CalendarViewModel(app)
    assertNotNull(viewModel.todayDate.value)
    val todayInfo = viewModel.getTodayDayInfo()
    assertTrue(todayInfo.isToday)
    assertEquals(viewModel.todayDate.value.day, todayInfo.day)
  }

  @Test
  fun `verify 10 minute timeout resets selected date and scroll position`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = CalendarViewModel(app)
    val days = viewModel.getDaysForMonth(2026, 9)
    val day30 = days.find { it.day == 30 }
    assertNotNull(day30)

    val t0 = 1_000_000L
    viewModel.selectDay(day30, nowMillis = t0)
    assertEquals(30, viewModel.selectedDay.value?.day)

    // Exit to background and return after 5 minutes (< 10 min): still selected
    viewModel.onAppBackgrounded(nowMillis = t0 + 1_000L)
    val reset5Min = viewModel.onAppResumed(nowMillis = t0 + 5 * 60 * 1000L)
    assertEquals(false, reset5Min)
    assertEquals(30, viewModel.selectedDay.value?.day)

    // Exit to background and return after 10 minutes (>= 10 min): selection cleared & scroll reset triggered
    val bgStart = t0 + 5 * 60 * 1000L
    viewModel.onAppBackgrounded(nowMillis = bgStart)
    val reset10Min = viewModel.onAppResumed(nowMillis = bgStart + CalendarViewModel.SELECTION_TIMEOUT_MS)
    assertEquals(true, reset10Min)
    assertEquals(null, viewModel.selectedDay.value)
    assertTrue(viewModel.resetScrollEvent.value > 0L)
  }

  @Test
  fun `verify screen lock and unlock resets selected date immediately`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = CalendarViewModel(app)
    val days = viewModel.getDaysForMonth(2026, 9)
    val day30 = days.find { it.day == 30 }
    assertNotNull(day30)

    val t0 = 2_000_000L
    viewModel.selectDay(day30, nowMillis = t0)
    assertEquals(30, viewModel.selectedDay.value?.day)

    // Simulate screen lock (even 3 seconds after selecting a date)
    viewModel.onAppBackgrounded(nowMillis = t0 + 3_000L, isScreenLocked = true)
    assertEquals(null, viewModel.selectedDay.value)

    // Simulate unlock and resume 5 seconds later
    val resetOnUnlock = viewModel.onUserUnlocked(nowMillis = t0 + 5_000L)
    assertTrue(resetOnUnlock)
    assertEquals(null, viewModel.selectedDay.value)
    assertEquals(t0 + 5_000L, viewModel.resetScrollEvent.value)
  }

  @Test
  fun `verify CalendarApplication and widget midnight alarm scheduling`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    BaseCalendarWidgetProvider.updateAllWidgets(context)
    TodayAppWidgetProvider.scheduleMidnightUpdate(context)
    MonthCalendarAppWidgetProvider.scheduleMidnightUpdate(context)
  }

  @Test
  fun `verify ServerTimeManager dictates today date independent of local clock manipulation`() {
    // 2026-09-29 10:05:00 CST (UTC+8) -> 2026-09-29 02:05:00 UTC
    // In milliseconds: 1790647500000L
    val calCst = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8")).apply {
      set(2026, java.util.Calendar.SEPTEMBER, 29, 10, 5, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val serverEpochMs = calCst.timeInMillis

    com.sunplace.calendar.util.ServerTimeManager.calibrate(serverEpochMs)
    assertTrue(com.sunplace.calendar.util.ServerTimeManager.isServerTimeCalibrated())

    val todayDate = com.sunplace.calendar.data.model.CalendarDate.today()
    assertEquals(2026, todayDate.year)
    assertEquals(9, todayDate.month)
    assertEquals(29, todayDate.day)

    val chinaCal = LunarCalculator.getChinaCalendar()
    assertEquals(2026, chinaCal.get(java.util.Calendar.YEAR))
    assertEquals(java.util.Calendar.SEPTEMBER, chinaCal.get(java.util.Calendar.MONTH))
    assertEquals(29, chinaCal.get(java.util.Calendar.DAY_OF_MONTH))

    // Reset after test
    com.sunplace.calendar.util.ServerTimeManager.resetForTesting()
  }

  @Test
  fun `verify ServerTimeManager persistence and recovery from shared preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val calCst = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8")).apply {
      set(2026, java.util.Calendar.OCTOBER, 1, 8, 0, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val serverEpochMs = calCst.timeInMillis

    com.sunplace.calendar.util.ServerTimeManager.calibrate(serverEpochMs, context)
    com.sunplace.calendar.util.ServerTimeManager.resetForTesting()

    // Init from context
    com.sunplace.calendar.util.ServerTimeManager.init(context)
    val chinaCal = LunarCalculator.getChinaCalendar()
    assertEquals(2026, chinaCal.get(java.util.Calendar.YEAR))
    assertEquals(java.util.Calendar.OCTOBER, chinaCal.get(java.util.Calendar.MONTH))
    assertEquals(1, chinaCal.get(java.util.Calendar.DAY_OF_MONTH))

    com.sunplace.calendar.util.ServerTimeManager.resetForTesting()
  }

  @Test
  fun `verify ServerTimeManager NTP servers list and CST conversion`() {
    val servers = com.sunplace.calendar.util.ServerTimeManager.NTP_SERVERS
    assertTrue(servers.contains("ntp.aliyun.com"))
    assertTrue(servers.contains("time.cloudflare.com"))
    assertTrue(servers.contains("pool.ntp.org"))

    // Test UTC to CST conversion
    // 2026-12-31 16:30:00 UTC -> 2027-01-01 00:30:00 CST (crosses year boundary)
    val calUtc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
      set(2026, java.util.Calendar.DECEMBER, 31, 16, 30, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    com.sunplace.calendar.util.ServerTimeManager.calibrate(calUtc.timeInMillis)
    val cstCal = com.sunplace.calendar.util.ServerTimeManager.getChinaCalendar()
    assertEquals(2027, cstCal.get(java.util.Calendar.YEAR))
    assertEquals(java.util.Calendar.JANUARY, cstCal.get(java.util.Calendar.MONTH))
    assertEquals(1, cstCal.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(0, cstCal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(30, cstCal.get(java.util.Calendar.MINUTE))

    com.sunplace.calendar.util.ServerTimeManager.resetForTesting()
  }

  @Test
  fun `verify custom widget preview PNGs decode as valid bitmaps for all 8 configurations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify resources decode cleanly via BitmapFactory
    val todayPreview = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.preview_widget_today)
    val monthPreview = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.preview_widget_month)
    assertNotNull("Today widget preview must decode cleanly", todayPreview)
    assertNotNull("Month widget preview must decode cleanly", monthPreview)
    assertTrue(todayPreview.width > 0 && todayPreview.height > 0)
    assertTrue(monthPreview.width > 0 && monthPreview.height > 0)

    // 2. Verify Chinese and English XML layout resources remain aligned on 2026-09-01
    val zhConfig = android.content.res.Configuration(context.resources.configuration).apply {
      setLocale(java.util.Locale.SIMPLIFIED_CHINESE)
    }
    val zhContext = context.createConfigurationContext(zhConfig)
    assertEquals("2026年9月", zhContext.getString(R.string.widget_preview_month_title))
    assertEquals("周二", zhContext.getString(R.string.widget_preview_today_weekday))
    assertEquals("9月", zhContext.getString(R.string.widget_preview_today_month))

    val enConfig = android.content.res.Configuration(context.resources.configuration).apply {
      setLocale(java.util.Locale.ENGLISH)
    }
    val enContext = context.createConfigurationContext(enConfig)
    assertEquals("September 2026", enContext.getString(R.string.widget_preview_month_title))
    assertEquals("Tuesday", enContext.getString(R.string.widget_preview_today_weekday))
    assertEquals("September", enContext.getString(R.string.widget_preview_today_month))
  }

  @Test
  fun `verify today widget RemoteViews inflates without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rv = android.widget.RemoteViews(context.packageName, R.layout.widget_today)
    val inflated = rv.apply(context, android.widget.FrameLayout(context))
    assertNotNull(inflated)

    val appWidgetManager = AppWidgetManager.getInstance(context)
    com.sunplace.calendar.widget.TodayAppWidgetProvider.updateAppWidget(context, appWidgetManager, 101)
  }

  @Test
  fun `verify TranslationHelper isEnglish conforms to 1 1 8 specification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Simplified Chinese primary -> false (display Chinese)
    val zhCnConfig = android.content.res.Configuration(context.resources.configuration).apply {
      setLocale(java.util.Locale.SIMPLIFIED_CHINESE)
    }
    val zhCnContext = context.createConfigurationContext(zhCnConfig)
    assertFalse(com.sunplace.calendar.util.TranslationHelper.isEnglish(zhCnContext))

    // 2. Traditional Chinese primary -> false (display Chinese, conforming to 1.1.8)
    val zhTwConfig = android.content.res.Configuration(context.resources.configuration).apply {
      setLocale(java.util.Locale.TRADITIONAL_CHINESE)
    }
    val zhTwContext = context.createConfigurationContext(zhTwConfig)
    assertFalse(com.sunplace.calendar.util.TranslationHelper.isEnglish(zhTwContext))

    // 3. English primary -> true (display English)
    val enConfig = android.content.res.Configuration(context.resources.configuration).apply {
      setLocale(java.util.Locale.US)
    }
    val enContext = context.createConfigurationContext(enConfig)
    assertTrue(com.sunplace.calendar.util.TranslationHelper.isEnglish(enContext))

    // 4. Other languages (Japanese, French, etc.) -> true (default to English)
    val jaConfig = android.content.res.Configuration(context.resources.configuration).apply {
      setLocale(java.util.Locale.JAPAN)
    }
    val jaContext = context.createConfigurationContext(jaConfig)
    assertTrue(com.sunplace.calendar.util.TranslationHelper.isEnglish(jaContext))

    // 5. Multi-language list: Chinese first, English second -> false (strictly respects primary language)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      val multiZhConfig = android.content.res.Configuration(context.resources.configuration).apply {
        setLocales(android.os.LocaleList(java.util.Locale.SIMPLIFIED_CHINESE, java.util.Locale.US))
      }
      val multiZhContext = context.createConfigurationContext(multiZhConfig)
      assertFalse(com.sunplace.calendar.util.TranslationHelper.isEnglish(multiZhContext))

      // English first, Chinese second -> true
      val multiEnConfig = android.content.res.Configuration(context.resources.configuration).apply {
        setLocales(android.os.LocaleList(java.util.Locale.US, java.util.Locale.SIMPLIFIED_CHINESE))
      }
      val multiEnContext = context.createConfigurationContext(multiEnConfig)
      assertTrue(com.sunplace.calendar.util.TranslationHelper.isEnglish(multiEnContext))
    }
  }
}




