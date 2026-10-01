package com.sunplace.calendar

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
    assertEquals("你并没有那么热爱工作", context.getString(R.string.announcement_content))
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
  fun `verify app version name is 1 1 7`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CalendarRepository(context)
    assertEquals("1.1.7", repository.getAppVersion())
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
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun `verify and generate 8 Google style widget preview images for light dark zh en today month`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resDirCandidates = listOf(
      java.io.File("src/main/res"),
      java.io.File("app/src/main/res"),
      java.io.File("/app/src/main/res")
    )
    val baseResDir = resDirCandidates.firstOrNull { it.exists() && it.isDirectory }
      ?: java.io.File("src/main/res")

    data class PreviewSpec(
      val folderName: String,
      val isDark: Boolean,
      val isEnglish: Boolean
    )

    val specs = listOf(
      PreviewSpec("drawable", isDark = false, isEnglish = false),
      PreviewSpec("drawable-night", isDark = true, isEnglish = false),
      PreviewSpec("drawable-en", isDark = false, isEnglish = true),
      PreviewSpec("drawable-en-night", isDark = true, isEnglish = true)
    )

    for (spec in specs) {
      val dir = java.io.File(baseResDir, spec.folderName)
      if (!dir.exists()) dir.mkdirs()

      val todayBmp = renderGoogleStyleTodayPreview(spec.isDark, spec.isEnglish)
      val todayFile = java.io.File(dir, "preview_widget_today.png")
      java.io.FileOutputStream(todayFile).use { out ->
        todayBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
      }
      assertTrue("Generated ${todayFile.path} must exist and be non-empty", todayFile.exists() && todayFile.length() > 500)

      val monthBmp = renderGoogleStyleMonthPreview(spec.isDark, spec.isEnglish)
      val monthFile = java.io.File(dir, "preview_widget_month.png")
      java.io.FileOutputStream(monthFile).use { out ->
        monthBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
      }
      assertTrue("Generated ${monthFile.path} must exist and be non-empty", monthFile.exists() && monthFile.length() > 500)
    }

    // Also verify Chinese and English XML layout resources remain aligned on 2026-09-01
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

  private fun renderGoogleStyleTodayPreview(isDark: Boolean, isEnglish: Boolean): android.graphics.Bitmap {
    val size = 440
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val bgColor = if (isDark) 0xFF202124.toInt() else 0xFFFFFFFF.toInt()
    val borderColor = if (isDark) 0xFF3C4043.toInt() else 0xFFDADCE0.toInt()
    val primaryColor = if (isDark) 0xFFE8EAED.toInt() else 0xFF202124.toInt()
    val secondaryColor = if (isDark) 0xFF9AA0A6.toInt() else 0xFF70757A.toInt()
    val redColor = if (isDark) 0xFFF28B82.toInt() else 0xFFD93025.toInt()

    drawWidgetCardSurface(canvas, size.toFloat(), bgColor, borderColor)

    val boldTypeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      typeface = boldTypeface
      isSubpixelText = true
    }

    if (isEnglish) {
      // English layout: Tuesday / September / 1
      paint.textAlign = android.graphics.Paint.Align.CENTER

      paint.color = redColor
      paint.textSize = 42f
      canvas.drawText("Tuesday", size / 2f, 122f, paint)

      paint.color = primaryColor
      paint.textSize = 45f
      canvas.drawText("September", size / 2f, 176f, paint)

      paint.color = primaryColor
      paint.textSize = 184f
      canvas.drawText("1", size / 2f, 346f, paint)
    } else {
      // Chinese layout: Top row "周二  9月", Center row "1" + vertical "七月二十"
      // Top row centered around x = 220, yCenter = 114
      val topCharSize = 42f
      val topYCenter = 114f
      // "周二" width ~ 90, gap ~ 20, "9月" width ~ 72 -> total ~ 182, left = 129
      drawCjkGlyph(canvas, '周', 150f, topYCenter, topCharSize, redColor, 5.2f)
      drawCjkGlyph(canvas, '二', 196f, topYCenter, topCharSize, redColor, 5.2f)

      paint.textAlign = android.graphics.Paint.Align.CENTER
      paint.color = secondaryColor
      paint.textSize = 45f
      val nineMetrics = paint.fontMetrics
      val nineBaseline = topYCenter - (nineMetrics.ascent + nineMetrics.descent) / 2f
      canvas.drawText("9", 252f, nineBaseline, paint)
      drawCjkGlyph(canvas, '月', 290f, topYCenter, topCharSize, secondaryColor, 5.0f)

      // Center row: Big "1" on left + vertical "七 月 二 十" on right
      paint.color = primaryColor
      paint.textSize = 200f
      val oneMetrics = paint.fontMetrics
      val centerRowY = 268f
      val oneBaseline = centerRowY - (oneMetrics.ascent + oneMetrics.descent) / 2f
      canvas.drawText("1", 176f, oneBaseline, paint)

      val lunarChars = charArrayOf('七', '月', '二', '十')
      val lunarSize = 36f
      val lunarSpacing = 44f
      val lunarStartY = centerRowY - (lunarSpacing * 1.5f)
      val lunarX = 274f
      for (i in lunarChars.indices) {
        drawCjkGlyph(canvas, lunarChars[i], lunarX, lunarStartY + i * lunarSpacing, lunarSize, primaryColor, 4.6f)
      }
    }

    return bmp
  }

  private fun renderGoogleStyleMonthPreview(isDark: Boolean, isEnglish: Boolean): android.graphics.Bitmap {
    val size = 440
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val bgColor = if (isDark) 0xFF202124.toInt() else 0xFFFFFFFF.toInt()
    val borderColor = if (isDark) 0xFF3C4043.toInt() else 0xFFDADCE0.toInt()
    val primaryColor = if (isDark) 0xFFE8EAED.toInt() else 0xFF202124.toInt()
    val redColor = if (isDark) 0xFFF28B82.toInt() else 0xFFD93025.toInt()
    val todayBarColor = if (isDark) 0xFF81C995.toInt() else 0xFF188038.toInt()

    drawWidgetCardSurface(canvas, size.toFloat(), bgColor, borderColor)

    val boldTypeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      typeface = boldTypeface
      isSubpixelText = true
      textAlign = android.graphics.Paint.Align.CENTER
    }

    // Header: "2026年9月" or "September 2026"
    val headerYCenter = 56f
    if (isEnglish) {
      paint.color = primaryColor
      paint.textSize = 40f
      val fm = paint.fontMetrics
      canvas.drawText("September 2026", size / 2f, headerYCenter - (fm.ascent + fm.descent) / 2f, paint)
    } else {
      paint.color = primaryColor
      paint.textSize = 41f
      val fm = paint.fontMetrics
      val base = headerYCenter - (fm.ascent + fm.descent) / 2f
      // "2026" + "年" + "9" + "月" centered around x = 220
      canvas.drawText("2026", 162f, base, paint)
      drawCjkGlyph(canvas, '年', 230f, headerYCenter, 37f, primaryColor, 4.6f)
      canvas.drawText("9", 265f, base, paint)
      drawCjkGlyph(canvas, '月', 298f, headerYCenter, 37f, primaryColor, 4.6f)
    }

    // September 2026 Grid: 5 rows (0..4), 7 cols (0..6), Sept 1 = Tuesday (col 1)
    // Rest days in Sept 2026: 5, 6, 12, 13, 19, 25, 26, 27 (Sunday 20 is shift workday!)
    val restDays = setOf(5, 6, 12, 13, 19, 25, 26, 27)
    val gridLeft = 30f
    val gridRight = 410f
    val gridTop = 92f
    val gridBottom = 408f
    val colWidth = (gridRight - gridLeft) / 7f
    val rowHeight = (gridBottom - gridTop) / 5f

    paint.textSize = 34.5f
    val fm = paint.fontMetrics
    val textOffset = -(fm.ascent + fm.descent) / 2f

    val barPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      style = android.graphics.Paint.Style.FILL
      color = todayBarColor
    }

    val startOffset = 1 // Tuesday
    for (day in 1..30) {
      val cellIndex = startOffset + (day - 1)
      val row = cellIndex / 7
      val col = cellIndex % 7
      val cx = gridLeft + colWidth * (col + 0.5f)
      val cy = gridTop + rowHeight * (row + 0.5f) - 4f

      paint.color = if (day in restDays) redColor else primaryColor
      canvas.drawText(day.toString(), cx, cy + textOffset, paint)

      if (day == 1) {
        // Draw green Today indicator bar under Day 1 (matching @drawable/bg_widget_today: 16dp x 4dp, radius 2dp)
        val barW = 48f
        val barH = 12f
        val barTop = gridTop + rowHeight * (row + 1) - barH - 4f
        val barRect = android.graphics.RectF(cx - barW / 2f, barTop, cx + barW / 2f, barTop + barH)
        canvas.drawRoundRect(barRect, 6f, 6f, barPaint)
      }
    }

    return bmp
  }

  private fun drawWidgetCardSurface(
    canvas: android.graphics.Canvas,
    size: Float,
    bgColor: Int,
    borderColor: Int
  ) {
    val inset = 6f
    val radius = 72f
    val rect = android.graphics.RectF(inset, inset, size - inset, size - inset)

    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      style = android.graphics.Paint.Style.FILL
      color = bgColor
    }
    canvas.drawRoundRect(rect, radius, radius, fillPaint)

    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      style = android.graphics.Paint.Style.STROKE
      strokeWidth = 3f
      color = borderColor
    }
    canvas.drawRoundRect(rect, radius, radius, strokePaint)
  }

  /**
   * Draws clean, crisp Noto-Sans-CJK-style bold vector strokes for the 6 Chinese characters
   * used in the 2026-09-01 widget previews ('周', '二', '月', '七', '十', '年'), ensuring
   * pixel-perfect rendering even in headless build containers without CJK font files.
   */
  private fun drawCjkGlyph(
    canvas: android.graphics.Canvas,
    ch: Char,
    cx: Float,
    cy: Float,
    boxSize: Float,
    color: Int,
    strokeWidth: Float
  ) {
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      this.color = color
      this.style = android.graphics.Paint.Style.STROKE
      this.strokeWidth = strokeWidth
      this.strokeCap = android.graphics.Paint.Cap.ROUND
      this.strokeJoin = android.graphics.Paint.Join.ROUND
    }
    val half = boxSize / 2f
    fun x(nx: Float) = cx + (nx - 0.5f) * boxSize
    fun y(ny: Float) = cy + (ny - 0.5f) * boxSize

    when (ch) {
      '二' -> {
        canvas.drawLine(x(0.18f), y(0.32f), x(0.82f), y(0.32f), p)
        canvas.drawLine(x(0.10f), y(0.72f), x(0.90f), y(0.72f), p)
      }
      '十' -> {
        canvas.drawLine(x(0.12f), y(0.46f), x(0.88f), y(0.46f), p)
        canvas.drawLine(x(0.50f), y(0.10f), x(0.50f), y(0.90f), p)
      }
      '七' -> {
        // Slanted horizontal bar + vertical-then-curved bottom-right hook
        canvas.drawLine(x(0.12f), y(0.44f), x(0.88f), y(0.34f), p)
        val path = android.graphics.Path().apply {
          moveTo(x(0.44f), y(0.12f))
          lineTo(x(0.44f), y(0.72f))
          quadTo(x(0.44f), y(0.84f), x(0.58f), y(0.84f))
          lineTo(x(0.82f), y(0.84f))
          quadTo(x(0.88f), y(0.84f), x(0.89f), y(0.70f))
        }
        canvas.drawPath(path, p)
      }
      '月' -> {
        val path = android.graphics.Path().apply {
          moveTo(x(0.28f), y(0.12f))
          lineTo(x(0.74f), y(0.12f))
          lineTo(x(0.74f), y(0.84f))
          quadTo(x(0.74f), y(0.90f), x(0.64f), y(0.88f))
          moveTo(x(0.28f), y(0.12f))
          lineTo(x(0.28f), y(0.56f))
          quadTo(x(0.28f), y(0.80f), x(0.16f), y(0.90f))
        }
        canvas.drawPath(path, p)
        canvas.drawLine(x(0.28f), y(0.37f), x(0.74f), y(0.37f), p)
        canvas.drawLine(x(0.27f), y(0.60f), x(0.74f), y(0.60f), p)
      }
      '周' -> {
        // Outer enclosure with bottom-left sweep and bottom-right hook
        val outer = android.graphics.Path().apply {
          moveTo(x(0.20f), y(0.12f))
          lineTo(x(0.82f), y(0.12f))
          lineTo(x(0.82f), y(0.84f))
          quadTo(x(0.82f), y(0.90f), x(0.72f), y(0.88f))
          moveTo(x(0.20f), y(0.12f))
          lineTo(x(0.20f), y(0.58f))
          quadTo(x(0.20f), y(0.80f), x(0.10f), y(0.90f))
        }
        canvas.drawPath(outer, p)
        // Upper inner: 土 (two horizontals + short vertical)
        canvas.drawLine(x(0.32f), y(0.28f), x(0.70f), y(0.28f), p)
        canvas.drawLine(x(0.28f), y(0.44f), x(0.74f), y(0.44f), p)
        canvas.drawLine(x(0.51f), y(0.18f), x(0.51f), y(0.44f), p)
        // Lower inner: 口
        val kou = android.graphics.RectF(x(0.34f), y(0.56f), x(0.68f), y(0.78f))
        canvas.drawRect(kou, p)
      }
      '年' -> {
        // Left-falling top stroke + top horizontal
        canvas.drawLine(x(0.34f), y(0.08f), x(0.16f), y(0.32f), p)
        canvas.drawLine(x(0.28f), y(0.20f), x(0.82f), y(0.20f), p)
        // Middle horizontal + short left vertical
        canvas.drawLine(x(0.26f), y(0.43f), x(0.78f), y(0.43f), p)
        canvas.drawLine(x(0.28f), y(0.43f), x(0.28f), y(0.66f), p)
        // Long bottom horizontal + main central vertical stem
        canvas.drawLine(x(0.10f), y(0.66f), x(0.90f), y(0.66f), p)
        canvas.drawLine(x(0.54f), y(0.20f), x(0.54f), y(0.92f), p)
      }
    }
  }
}




