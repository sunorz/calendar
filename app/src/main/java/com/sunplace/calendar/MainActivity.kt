package com.sunplace.calendar

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.sunplace.calendar.ui.CalendarScreen
import com.sunplace.calendar.ui.CalendarViewModel
import com.sunplace.calendar.ui.theme.MyApplicationTheme
import com.sunplace.calendar.widget.BaseCalendarWidgetProvider
import java.util.Locale

class MainActivity : ComponentActivity() {

  private val calendarViewModel: CalendarViewModel by viewModels()

  private val dateChangeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      when (action) {
        Intent.ACTION_SCREEN_OFF -> {
          calendarViewModel.onScreenLocked()
        }
        Intent.ACTION_SCREEN_ON,
        Intent.ACTION_USER_PRESENT -> {
          calendarViewModel.onUserUnlocked()
          if (context != null) {
            BaseCalendarWidgetProvider.updateAllWidgets(context)
          }
        }
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_TICK,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED -> {
          val changed = calendarViewModel.checkAndRefreshDate()
          if (changed && context != null) {
            BaseCalendarWidgetProvider.updateAllWidgets(context)
          }
        }
      }
    }
  }

  private fun isDeviceLockedOrScreenOff(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val screenOff = powerManager != null && !powerManager.isInteractive
    val keyguardLocked = keyguardManager != null &&
        (keyguardManager.isKeyguardLocked || keyguardManager.isDeviceLocked)
    return screenOff || keyguardLocked
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    calendarViewModel.checkAndRefreshDate()
    BaseCalendarWidgetProvider.updateAllWidgets(this)

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_DATE_CHANGED)
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_USER_PRESENT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(dateChangeReceiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      registerReceiver(dateChangeReceiver, filter)
    }

    setContent {
      val themeMode by calendarViewModel.themeMode.collectAsState()
      val isElderlyMode by calendarViewModel.isElderlyMode.collectAsState()
      val appLanguage by calendarViewModel.appLanguage.collectAsState()

      val configuration = LocalConfiguration.current
      val localizedConfig = remember(appLanguage, configuration) {
        Configuration(configuration).apply {
          val locale = if (appLanguage.equals("en", ignoreCase = true)) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
          setLocale(locale)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setLocales(LocaleList(locale))
          }
        }
      }

      CompositionLocalProvider(LocalConfiguration provides localizedConfig) {
        MyApplicationTheme(themeMode = themeMode, isElderlyMode = isElderlyMode) {
          Surface(modifier = Modifier.fillMaxSize()) {
            CalendarScreen(viewModel = calendarViewModel)
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (isDeviceLockedOrScreenOff()) {
      calendarViewModel.onScreenLocked()
    } else {
      calendarViewModel.onAppResumed()
    }
    BaseCalendarWidgetProvider.updateAllWidgets(this)
  }

  override fun onPause() {
    super.onPause()
    calendarViewModel.onAppBackgrounded(isScreenLocked = isDeviceLockedOrScreenOff())
  }

  override fun onStop() {
    super.onStop()
    if (isDeviceLockedOrScreenOff()) {
      calendarViewModel.onScreenLocked()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(dateChangeReceiver)
    } catch (_: Exception) {}
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    calendarViewModel.checkAndRefreshDate()
    calendarViewModel.checkSystemLanguageChange()
    BaseCalendarWidgetProvider.updateAllWidgets(this)
  }
}
