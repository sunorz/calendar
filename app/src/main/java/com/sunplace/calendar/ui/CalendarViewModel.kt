package com.sunplace.calendar.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunplace.calendar.data.model.AppLanguage
import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.CalendarDate
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.model.TodayPattern
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.data.repository.SyncState
import com.sunplace.calendar.data.repository.ThemePreferences
import com.sunplace.calendar.util.LunarCalculator
import com.sunplace.calendar.util.TranslationHelper
import com.sunplace.calendar.widget.BaseCalendarWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

object TodayPatternHolder {
    var angle: Float = Random.nextFloat() * 180f
        private set

    var normCenterX: Float = 0.22f + Random.nextFloat() * 0.56f
        private set

    var normCenterY: Float = 0.22f + Random.nextFloat() * 0.56f
        private set

    fun randomize() {
        angle = Random.nextFloat() * 180f
        normCenterX = 0.22f + Random.nextFloat() * 0.56f
        normCenterY = 0.22f + Random.nextFloat() * 0.56f
    }
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val START_YEAR = 2020
        const val END_YEAR = 2040
        val TOTAL_MONTHS = (END_YEAR - START_YEAR + 1) * 12
        const val SELECTION_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes timeout reset

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
    val showGuangxiSanyuesan: StateFlow<Boolean> = themePreferences.showGuangxiSanyuesan
    val todayPattern: StateFlow<TodayPattern> = themePreferences.todayPattern


    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _selectedDay = MutableStateFlow<DayInfo?>(null)
    val selectedDay: StateFlow<DayInfo?> = _selectedDay.asStateFlow()

    private val _resetScrollEvent = MutableStateFlow(0L)
    val resetScrollEvent: StateFlow<Long> = _resetScrollEvent.asStateFlow()

    private var lastSelectionTimeMillis: Long = 0L
    private var lastBackgroundTimeMillis: Long = 0L
    private var screenLockedWhileAway: Boolean = false
    private var selectionTimeoutJob: Job? = null

    private val _remoteVersion = MutableStateFlow<String?>(null)
    val remoteVersion: StateFlow<String?> = _remoteVersion.asStateFlow()

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _todayDate = MutableStateFlow(CalendarDate.today())
    val todayDate: StateFlow<CalendarDate> = _todayDate.asStateFlow()

    val todayMonthIndex: Int
        get() {
            val d = _todayDate.value
            return getMonthIndex(d.year, d.month)
        }

    init {
        TodayPatternHolder.randomize()
        syncAllConfigs()
        checkVersion()
        viewModelScope.launch {
            repository.syncServerTime()
            checkAndRefreshDate()
        }
        viewModelScope.launch {
            combine(config, nextYearConfig) { c, nc -> Pair(c, nc) }
                .collect { (c, nc) ->
                    refreshSelectedDay(c, nc, showGuangxiSanyuesan.value)
                }
        }
    }

    private fun refreshSelectedDay(
        mainCfg: CalendarConfig = config.value,
        nextCfg: CalendarConfig? = nextYearConfig.value,
        sanyuesanEnabled: Boolean = showGuangxiSanyuesan.value
    ) {
        _selectedDay.value?.let { current ->
            _selectedDay.value = LunarCalculator.getDayInfoForDate(
                year = current.year,
                month = current.month,
                day = current.day,
                mainConfig = mainCfg,
                nextConfig = nextCfg,
                showGuangxiSanyuesan = sanyuesanEnabled,
                todayDate = _todayDate.value
            )
        }
    }

    private fun notifyWidgetsUpdated() {
        try {
            BaseCalendarWidgetProvider.updateAllWidgets(getApplication())
        } catch (_: Throwable) {
            // Silently ignore
        }
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
        if (_isDownloading.value) return
        val newVer = _remoteVersion.value ?: return
        val baseDownloadUrl = String(Base64.decode("aHR0cHM6Ly9ra2lpLm9yZy9hcHAvY2FsZW5kYXItdg==", Base64.DEFAULT))
        val endDownloadUrl = String(Base64.decode("LXJlbGVhc2UuYXBr", Base64.DEFAULT))
        val timestamp = System.currentTimeMillis() / 1000
        val downloadUrl = "$baseDownloadUrl$newVer$endDownloadUrl?d=$timestamp"

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            val context = getApplication<Application>()
            val downloadedFile = withContext(Dispatchers.IO) {
                try {
                    val url = URL(downloadUrl)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                    }
                    connection.connect()
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext null
                    }
                    val fileLength = connection.contentLength
                    val targetDir = context.externalCacheDir ?: context.cacheDir
                    targetDir.listFiles { _, name -> name.startsWith("update_") && name.endsWith(".apk") }
                        ?.forEach { it.delete() }
                    val file = File(targetDir, "update_v${newVer}.apk")

                    connection.inputStream.use { input ->
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8192)
                            var total: Long = 0
                            var count: Int
                            while (input.read(buffer).also { count = it } != -1) {
                                total += count
                                if (fileLength > 0) {
                                    _downloadProgress.value = total.toFloat() / fileLength
                                }
                                output.write(buffer, 0, count)
                            }
                            output.flush()
                        }
                    }
                    file
                } catch (_: Exception) {
                    null
                }
            }

            _downloadProgress.value = 1f
            _isDownloading.value = false
            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0) {
                installApk(context, downloadedFile)
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) { }
            }
        }
    }

    private fun installApk(context: Application, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback
        }
    }

    fun getAppVersion(): String = repository.getAppVersion()

    fun setThemeMode(mode: ThemeMode) {
        if (isElderlyMode.value) return
        themePreferences.setThemeMode(mode)
        notifyWidgetsUpdated()
    }

    fun toggleElderlyMode() {
        themePreferences.toggleElderlyMode()
        notifyWidgetsUpdated()
    }



    fun setElderlyMode(enabled: Boolean) {
        themePreferences.setElderlyMode(enabled)
        notifyWidgetsUpdated()
    }

    fun setShowGuangxiSanyuesan(enabled: Boolean) {
        themePreferences.setShowGuangxiSanyuesan(enabled)
        notifyWidgetsUpdated()
        refreshSelectedDay(sanyuesanEnabled = enabled)
    }

    fun toggleShowGuangxiSanyuesan() {
        setShowGuangxiSanyuesan(!showGuangxiSanyuesan.value)
    }

    fun setTodayPattern(pattern: TodayPattern) {
        themePreferences.setTodayPattern(pattern)
    }



    fun openSettingsDialog() {
        _showSettingsDialog.value = true
        checkVersion()
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun selectDay(dayInfo: DayInfo?, nowMillis: Long = System.currentTimeMillis()) {
        _selectedDay.value = dayInfo
        selectionTimeoutJob?.cancel()
        if (dayInfo != null) {
            lastSelectionTimeMillis = nowMillis
            selectionTimeoutJob = viewModelScope.launch {
                delay(SELECTION_TIMEOUT_MS)
                _selectedDay.value = null
                lastSelectionTimeMillis = 0L
            }
        } else {
            lastSelectionTimeMillis = 0L
        }
    }

    fun onAppBackgrounded(nowMillis: Long = System.currentTimeMillis(), isScreenLocked: Boolean = false) {
        lastBackgroundTimeMillis = nowMillis
        if (isScreenLocked) {
            onScreenLocked(nowMillis)
        }
    }

    fun onScreenLocked(nowMillis: Long = System.currentTimeMillis()) {
        screenLockedWhileAway = true
        selectDay(null)
        _resetScrollEvent.value = nowMillis
    }

    fun onUserUnlocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
        checkAndRefreshDate()
        viewModelScope.launch {
            repository.syncServerTime()
            checkAndRefreshDate()
        }
        val hadSelection = _selectedDay.value != null
        val wasLocked = screenLockedWhileAway
        screenLockedWhileAway = false
        selectDay(null)
        _resetScrollEvent.value = nowMillis
        return hadSelection || wasLocked
    }

    fun onAppResumed(nowMillis: Long = System.currentTimeMillis()): Boolean {
        viewModelScope.launch {
            repository.syncServerTime()
            checkAndRefreshDate()
        }
        val dateChanged = checkAndRefreshDate()
        val selectionTimedOut = _selectedDay.value != null &&
            lastSelectionTimeMillis > 0L &&
            (nowMillis - lastSelectionTimeMillis) >= SELECTION_TIMEOUT_MS
        val backgroundTimedOut = lastBackgroundTimeMillis > 0L &&
            (nowMillis - lastBackgroundTimeMillis) >= SELECTION_TIMEOUT_MS
        val lockedReset = screenLockedWhileAway

        lastBackgroundTimeMillis = 0L
        screenLockedWhileAway = false

        if (selectionTimedOut || backgroundTimedOut || dateChanged || lockedReset) {
            selectDay(null)
            if (backgroundTimedOut || dateChanged || lockedReset) {
                _resetScrollEvent.value = nowMillis
            }
            return true
        }
        return false
    }

    fun checkAndRefreshDate(): Boolean {
        val current = CalendarDate.today()
        val previous = _todayDate.value
        if (current != previous) {
            _todayDate.value = current
            TodayPatternHolder.randomize()
            selectDay(null)
            _resetScrollEvent.value = System.currentTimeMillis()
            syncAllConfigs()
            return true
        }
        return false
    }

    fun selectToday() {
        checkAndRefreshDate()
        selectDay(getTodayDayInfo())
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
            try {
                coroutineScope {
                    val currentDeferred = async { repository.fetchRemoteConfig() }
                    val nextDeferred = async { repository.fetchNextYearConfig() }
                    currentDeferred.await()
                    nextDeferred.await()
                }
            } catch (_: Throwable) {
                // Silently ignore, never expose any error
            }
            notifyWidgetsUpdated()
        }
    }

    fun refreshConfig() {
        syncAllConfigs()
    }

    fun getTodayDayInfo(): DayInfo {
        val d = _todayDate.value
        return LunarCalculator.getDayInfoForDate(
            year = d.year,
            month = d.month,
            day = d.day,
            mainConfig = config.value,
            nextConfig = nextYearConfig.value,
            showGuangxiSanyuesan = showGuangxiSanyuesan.value,
            todayDate = d
        )
    }

    fun getTodayColorModeIndex(): Int {
        val todayInfo = getTodayDayInfo()
        return when {
            todayInfo.isExplicitLocaleHoliday -> 2
            todayInfo.isRestDay -> 1
            else -> 0
        }
    }

    fun getDaysForMonth(year: Int, month: Int): List<DayInfo> {
        return LunarCalculator.getMonthDays(
            year = year,
            month = month,
            mainConfig = config.value,
            nextConfig = nextYearConfig.value,
            showGuangxiSanyuesan = showGuangxiSanyuesan.value,
            todayDate = _todayDate.value
        )
    }
}
