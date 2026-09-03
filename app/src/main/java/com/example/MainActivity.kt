package com.example

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
import com.example.ui.CalendarScreen
import com.example.ui.CalendarViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.CalendarAppWidgetProvider

class MainActivity : ComponentActivity() {

  private val calendarViewModel: CalendarViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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
    CalendarAppWidgetProvider.updateAllWidgets(this)
  }
}

