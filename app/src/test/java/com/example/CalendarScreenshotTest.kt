package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.CalendarConfig
import com.example.ui.MinimalMonthSection
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LunarCalculator
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class CalendarScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun calendar_month_screenshot() {
    val config = CalendarConfig()
    val days = LunarCalculator.getMonthDays(2026, 5, config)

    composeTestRule.setContent {
      MyApplicationTheme {
        MinimalMonthSection(
          year = 2026,
          month = 5,
          days = days,
          selectedDay = null,
          onDayClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calendar_month.png")
  }
}
