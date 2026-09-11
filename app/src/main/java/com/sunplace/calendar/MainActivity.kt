package com.sunplace.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sunplace.calendar.ui.CalendarScreen
import com.sunplace.calendar.ui.CalendarViewModel
import com.sunplace.calendar.ui.theme.MyApplicationTheme
import com.sunplace.calendar.widget.MonthCalendarAppWidgetProvider
import com.sunplace.calendar.widget.TodayAppWidgetProvider

class MainActivity : ComponentActivity() {

  private val calendarViewModel: CalendarViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    TodayAppWidgetProvider.updateAllWidgets(this)
    MonthCalendarAppWidgetProvider.updateAllWidgets(this)
    setContent {
      val themeMode by calendarViewModel.themeMode.collectAsState()
      val isElderlyMode by calendarViewModel.isElderlyMode.collectAsState()
      MyApplicationTheme(themeMode = themeMode, isElderlyMode = isElderlyMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
          CalendarScreen(viewModel = calendarViewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    TodayAppWidgetProvider.updateAllWidgets(this)
    MonthCalendarAppWidgetProvider.updateAllWidgets(this)
  }

  override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    TodayAppWidgetProvider.updateAllWidgets(this)
    MonthCalendarAppWidgetProvider.updateAllWidgets(this)
  }
}

