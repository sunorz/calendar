package com.sunplace.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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

  private val dateChangeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      if (action == Intent.ACTION_DATE_CHANGED ||
          action == Intent.ACTION_TIME_TICK ||
          action == Intent.ACTION_TIME_CHANGED ||
          action == Intent.ACTION_TIMEZONE_CHANGED) {
        val changed = calendarViewModel.checkAndRefreshDate()
        if (changed && context != null) {
          TodayAppWidgetProvider.updateAllWidgets(context)
          MonthCalendarAppWidgetProvider.updateAllWidgets(context)
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    calendarViewModel.checkAndRefreshDate()
    TodayAppWidgetProvider.updateAllWidgets(this)
    MonthCalendarAppWidgetProvider.updateAllWidgets(this)

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_DATE_CHANGED)
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(dateChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(dateChangeReceiver, filter)
    }

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
    calendarViewModel.checkAndRefreshDate()
    TodayAppWidgetProvider.updateAllWidgets(this)
    MonthCalendarAppWidgetProvider.updateAllWidgets(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(dateChangeReceiver)
    } catch (_: Exception) {}
  }

  override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    calendarViewModel.checkAndRefreshDate()
    TodayAppWidgetProvider.updateAllWidgets(this)
    MonthCalendarAppWidgetProvider.updateAllWidgets(this)
  }
}

