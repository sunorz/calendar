package com.sunplace.calendar.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.data.repository.SyncState
import com.sunplace.calendar.data.repository.ThemePreferences
import com.sunplace.calendar.util.LunarCalculator
import com.sunplace.calendar.widget.MonthCalendarAppWidgetProvider
import com.sunplace.calendar.widget.TodayAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthItem(
    val index: Int,
    val year: Int,
    val month: Int,
    val days: List<DayInfo>
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val START_YEAR = 2020
        const val END_YEAR = 2040
        val TOTAL_MONTHS = (END_YEAR - START_YEAR + 1) * 12

        fun getMonthIndex(year: Int, month: Int): Int {
            val clampedYear = year.coerceIn(START_YEAR, END_YEAR)
            return (clampedYear - START_YEAR) * 12 + (month - 1)
        }

        fun getYearMonthFromIndex(index: Int): Pair<Int, Int> {
            val y = START_YEAR + (index / 12)
            val m = (index % 12) + 1
            return Pair(y, m)
        }
    }

    private val repository = CalendarRepository(application)
    private val themePreferences = ThemePreferences(application)

    val config: StateFlow<CalendarConfig> = repository.configState
    val nextYearConfig: StateFlow<CalendarConfig?> = repository.nextYearConfigState
    val syncState: StateFlow<SyncState> = repository.syncState
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
    val isElderlyMode: StateFlow<Boolean> = themePreferences.isElderlyMode

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _selectedDay = MutableStateFlow<DayInfo?>(null)
    val selectedDay: StateFlow<DayInfo?> = _selectedDay.asStateFlow()

    private val _remoteVersion = MutableStateFlow<String?>(null)
    val remoteVersion: StateFlow<String?> = _remoteVersion.asStateFlow()

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    val todayMonthIndex: Int
        get() {
            val cal = LunarCalculator.getChinaCalendar()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            return getMonthIndex(y, m)
        }

    init {
        syncAllConfigs()
        checkVersion()
    }

    fun checkVersion() {
        viewModelScope.launch {
            repository.checkAndSyncVersion().onSuccess { result ->
                _remoteVersion.value = result.remoteMain
                if (result.isNewer) {
                    _updateAvailable.value = true
                }
            }
        }
    }

    fun downloadAndUpdate() {
        val newVer = _remoteVersion.value ?: "0.7.12"
        val baseDownloadUrl = String(android.util.Base64.decode("aHR0cHM6Ly9ra2lpLm9yZy9hcHAvY2FsZW5kYXItdg==", android.util.Base64.DEFAULT))
        val endDownloadUrl = String(android.util.Base64.decode("LXJlbGVhc2UuYXBr", android.util.Base64.DEFAULT))
        val timestamp = System.currentTimeMillis() / 1000
        val downloadUrl = "$baseDownloadUrl$newVer$endDownloadUrl?d=$timestamp"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // fallback
        }
    }

    fun getAppVersion(): String {
        return repository.getAppVersion()
    }

    fun setThemeMode(mode: ThemeMode) {
        if (isElderlyMode.value) {
            // In elderly mode, theme switching is forbidden; light mode is forced
            return
        }
        themePreferences.setThemeMode(mode)
        TodayAppWidgetProvider.updateAllWidgets(getApplication())
        MonthCalendarAppWidgetProvider.updateAllWidgets(getApplication())
    }

    fun toggleElderlyMode() {
        themePreferences.toggleElderlyMode()
        TodayAppWidgetProvider.updateAllWidgets(getApplication())
        MonthCalendarAppWidgetProvider.updateAllWidgets(getApplication())
    }

    fun setElderlyMode(enabled: Boolean) {
        themePreferences.setElderlyMode(enabled)
        TodayAppWidgetProvider.updateAllWidgets(getApplication())
        MonthCalendarAppWidgetProvider.updateAllWidgets(getApplication())
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
        checkVersion()
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun selectDay(dayInfo: DayInfo?) {
        _selectedDay.value = dayInfo
    }

    fun selectToday() {
        val cal = LunarCalculator.getChinaCalendar()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val days = getDaysForMonth(y, m)
        val todayInfo = days.find { it.year == y && it.month == m && it.day == d }
        _selectedDay.value = todayInfo
    }

    fun getConfigUrl(): String = repository.getConfigUrl()

    fun updateConfigUrlAndRefresh(url: String) {
        repository.setConfigUrl(url)
        refreshConfig()
    }

    fun resetConfigUrlToDefault() {
        repository.setConfigUrl(CalendarRepository.DEFAULT_CONFIG_URL)
        refreshConfig()
    }

    fun syncAllConfigs() {
        viewModelScope.launch {
            repository.fetchRemoteConfig()
            repository.fetchNextYearConfig()
            // Update home screen widgets
            TodayAppWidgetProvider.updateAllWidgets(getApplication())
            MonthCalendarAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun refreshConfig() {
        syncAllConfigs()
    }

    /**
     * Helper to get days for any month index using current config
     */
    fun getDaysForMonth(year: Int, month: Int): List<DayInfo> {
        return LunarCalculator.getMonthDays(year, month, config.value, nextYearConfig.value)
    }
}
