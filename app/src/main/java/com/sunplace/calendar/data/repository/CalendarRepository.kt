package com.sunplace.calendar.data.repository

import android.content.Context
import android.util.Log
import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.FestivalItem
import com.sunplace.calendar.data.model.LocaleFestivalItem
import com.sunplace.calendar.data.model.RestDayMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String, val lastSyncTime: Long) : SyncState()
    data class Error(val error: String, val lastSyncTime: Long? = null) : SyncState()
}

class CalendarRepository(private val context: Context) {

    companion object {
        val DEFAULT_CONFIG_URL: String
            get() = String(android.util.Base64.decode("aHR0cHM6Ly9ra2lpLm9yZy9hc3NldHMvanMvY2Fscy9jb25maWcuanNvbg==", android.util.Base64.DEFAULT))

        val NEXT_CONFIG_URL: String
            get() = String(android.util.Base64.decode("aHR0cHM6Ly9ra2lpLm9yZy9hc3NldHMvanMvY2Fscy9jb25maWctbmV4dC5qc29u", android.util.Base64.DEFAULT))
            
        val VERSION_CHECK_URL: String
            get() = String(android.util.Base64.decode("aHR0cHM6Ly9ra2lpLm9yZy9hcHAvY2FsZW5kYXItdmVyc2lvbi5qc29u", android.util.Base64.DEFAULT))
            
        private const val PREFS_NAME = "calendar_prefs"
        private const val KEY_CACHED_JSON = "cached_config_json"
        private const val KEY_CACHED_NEXT_JSON = "cached_next_config_json"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_CUSTOM_URL = "custom_config_url"
        private const val KEY_APP_VERSION = "app_version_override"
        private const val TAG = "CalendarRepository"

        // Embedded initial fallback for current year (2026) so it is instantly available offline or before first network call
        private const val EMBEDDED_CONFIG_JSON = """{"curYear":2026,"startWeek":4,"monthsLimit":[31,28,31,30,31,30,31,31,30,31,30,31],"lunarStart":[0,13],"lunarMonth":[11,12,1,2,3,4,5,6,7,8,9,10,11],"lunarMonthLimit":[30,29,30,29,30,29,29,30,29,29,30,30,30],"lunarFestivals":[{"date":"0-17","name":"小寒","tag":0},{"date":"1-2","name":"大寒","tag":0},{"date":"1-8","name":"腊八节","tag":0},{"date":"1-17","name":"立春","tag":0},{"date":"1-23","name":"北方小年","tag":0},{"date":"1-24","name":"南方小年","tag":0},{"date":"1-29","name":"除夕","tag":0},{"date":"2-2","name":"雨水","tag":0},{"date":"2-15","name":"元宵节","tag":0},{"date":"3-2","name":"龙抬头","tag":0},{"date":"3-18","name":"清明节","tag":1},{"date":"4-4","name":"谷雨","tag":0},{"date":"4-19","name":"立夏","tag":0},{"date":"5-5","name":"小满","tag":0},{"date":"5-20","name":"芒种","tag":0},{"date":"6-5","name":"端午节","tag":1},{"date":"6-7","name":"夏至","tag":0},{"date":"6-23","name":"小暑","tag":0},{"date":"7-10","name":"大暑","tag":0},{"date":"7-25","name":"立秋","tag":0},{"date":"8-7","name":"七夕节","tag":0},{"date":"8-11","name":"处暑","tag":0},{"date":"8-15","name":"中元节","tag":0},{"date":"8-26","name":"白露","tag":0},{"date":"9-13","name":"秋分","tag":0},{"date":"9-15","name":"中秋节","tag":1},{"date":"9-28","name":"寒露","tag":0},{"date":"10-9","name":"重阳节","tag":0},{"date":"10-14","name":"霜降","tag":0},{"date":"10-29","name":"立冬","tag":0},{"date":"11-14","name":"小雪","tag":0},{"date":"11-29","name":"大雪","tag":0},{"date":"12-14","name":"冬至","tag":0}],"Festivals":[{"date":"1-10","name":"警察节","tag":0},{"date":"2-14","name":"情人节","tag":0},{"date":"3-5","name":"学雷锋日","tag":0},{"date":"3-8","name":"妇女节","tag":0},{"date":"3-12","name":"植树节","tag":0},{"date":"3-15","name":"消费者日","tag":0},{"date":"4-1","name":"愚人节","tag":0},{"date":"4-20","name":"复活节","tag":0},{"date":"5-1","name":"劳动节","tag":1},{"date":"5-4","name":"青年节","tag":0},{"date":"5-10","name":"母亲节","tag":0},{"date":"6-1","name":"儿童节","tag":0},{"date":"6-16","name":"父亲节","tag":0},{"date":"7-1","name":"建党节","tag":0},{"date":"8-1","name":"建军节","tag":0},{"date":"9-10","name":"教师节","tag":0},{"date":"10-1","name":"国庆节","tag":1},{"date":"10-31","name":"万圣夜","tag":0},{"date":"11-1","name":"万圣节","tag":0},{"date":"11-11","name":"购物节","tag":0},{"date":"11-26","name":"感恩节","tag":0},{"date":"12-1","name":"艾滋病日","tag":0},{"date":"12-20","name":"澳门回归","tag":0},{"date":"12-24","name":"平安夜","tag":0},{"date":"12-25","name":"圣诞节","tag":0}],"localeFestivals":[{"m":4,"d":[17,20]}],"restDays":[{"m":1,"d":[1,2,3,10,11,17,18,24,25,31]},{"m":2,"d":[1,7,8,15,16,17,18,19,20,21,22,23]},{"m":3,"d":[1,7,8,14,15,21,22,28,29]},{"m":4,"d":[4,5,6,11,12,18,19,25,26]},{"m":5,"d":[1,2,3,4,5,10,16,17,23,24,30,31]},{"m":6,"d":[6,7,13,14,19,20,21,27,28]},{"m":7,"d":[4,5,11,12,18,19,25,26]},{"m":8,"d":[1,2,8,9,15,16,22,23,29,30]},{"m":9,"d":[5,6,12,13,19,25,26,27]},{"m":10,"d":[1,2,3,4,5,6,7,11,17,18,24,25,31]},{"m":11,"d":[1,7,8,14,15,21,22,28,29]},{"m":12,"d":[5,6,12,13,19,20,26,27]}]}"""

        // Embedded initial fallback for next year (2027) so it is instantly available offline or before first network call
        private const val EMBEDDED_NEXT_CONFIG_JSON = """{"curYear":2027,"startWeek":5,"monthsLimit":[31,28,31,30,31,30,31,31,30,31,30,31],"lunarStart":[0,24],"lunarMonth":[11,12,1,2,3,4,5,6,7,8,9,10,11],"lunarMonthLimit":[30,29,30,29,30,29,29,30,29,29,30,30,30],"lunarFestivals":[{"date":"0-28","name":"小寒","tag":0},{"date":"1-2","name":"大寒","tag":0},{"date":"1-8","name":"腊八节","tag":0},{"date":"1-17","name":"立春","tag":0},{"date":"1-23","name":"北方小年","tag":0},{"date":"1-24","name":"南方小年","tag":0},{"date":"1-29","name":"除夕","tag":0},{"date":"2-2","name":"雨水","tag":0},{"date":"2-15","name":"元宵节","tag":0},{"date":"3-2","name":"龙抬头","tag":0},{"date":"3-18","name":"清明节","tag":1},{"date":"4-4","name":"谷雨","tag":0},{"date":"4-19","name":"立夏","tag":0},{"date":"5-5","name":"小满","tag":0},{"date":"5-20","name":"芒种","tag":0},{"date":"6-5","name":"端午节","tag":1},{"date":"6-7","name":"夏至","tag":0},{"date":"6-23","name":"小暑","tag":0},{"date":"7-10","name":"大暑","tag":0},{"date":"7-25","name":"立秋","tag":0},{"date":"8-7","name":"七夕节","tag":0},{"date":"8-11","name":"处暑","tag":0},{"date":"8-15","name":"中元节","tag":0},{"date":"8-26","name":"白露","tag":0},{"date":"9-13","name":"秋分","tag":0},{"date":"9-15","name":"中秋节","tag":1},{"date":"9-28","name":"寒露","tag":0},{"date":"10-9","name":"重阳节","tag":0},{"date":"10-14","name":"霜降","tag":0},{"date":"10-29","name":"立冬","tag":0},{"date":"11-14","name":"小雪","tag":0},{"date":"11-29","name":"大雪","tag":0},{"date":"12-14","name":"冬至","tag":0}],"Festivals":[{"date":"1-10","name":"警察节","tag":0},{"date":"2-14","name":"情人节","tag":0},{"date":"3-5","name":"学雷锋日","tag":0},{"date":"3-8","name":"妇女节","tag":0},{"date":"3-12","name":"植树节","tag":0},{"date":"3-15","name":"消费者日","tag":0},{"date":"4-1","name":"愚人节","tag":0},{"date":"4-20","name":"复活节","tag":0},{"date":"5-1","name":"劳动节","tag":1},{"date":"5-4","name":"青年节","tag":0},{"date":"5-10","name":"母亲节","tag":0},{"date":"6-1","name":"儿童节","tag":0},{"date":"6-16","name":"父亲节","tag":0},{"date":"7-1","name":"建党节","tag":0},{"date":"8-1","name":"建军节","tag":0},{"date":"9-10","name":"教师节","tag":0},{"date":"10-1","name":"国庆节","tag":1},{"date":"10-31","name":"万圣夜","tag":0},{"date":"11-1","name":"万圣节","tag":0},{"date":"11-11","name":"购物节","tag":0},{"date":"11-26","name":"感恩节","tag":0},{"date":"12-1","name":"艾滋病日","tag":0},{"date":"12-20","name":"澳门回归","tag":0},{"date":"12-24","name":"平安夜","tag":0},{"date":"12-25","name":"圣诞节","tag":0}],"localeFestivals":[{"m":4,"d":[9]}],"restDays":[{"m":1,"d":[1]},{"m":2,"d":[6]},{"m":3,"d":[]},{"m":4,"d":[5]},{"m":5,"d":[1,2,3]},{"m":6,"d":[]},{"m":7,"d":[]},{"m":8,"d":[]},{"m":9,"d":[15]},{"m":10,"d":[1,2,3,4,5,6,7]},{"m":11,"d":[]},{"m":12,"d":[]}]}"""
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadInitialConfig())
    val configState: StateFlow<CalendarConfig> = _configState.asStateFlow()

    private val _nextYearConfigState = MutableStateFlow(loadInitialNextConfig())
    val nextYearConfigState: StateFlow<CalendarConfig?> = _nextYearConfigState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(
        if (getLastSyncTime() > 0L) SyncState.Success("配置已同步", getLastSyncTime()) else SyncState.Idle
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun getConfigUrl(): String {
        return prefs.getString(KEY_CUSTOM_URL, DEFAULT_CONFIG_URL) ?: DEFAULT_CONFIG_URL
    }

    fun setConfigUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_URL, url).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.7.5"
        } catch (e: Exception) {
            prefs.getString(KEY_APP_VERSION, "0.7.5") ?: "0.7.5"
        }
    }

    fun setAppVersion(version: String) {
        prefs.edit().putString(KEY_APP_VERSION, version).apply()
    }

    fun isVersionNewer(remoteVersion: String, localVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            val localParts = localVersion.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            // fallback
        }
        return remoteVersion != localVersion
    }

    data class VersionCheckResult(
        val remoteMain: String,
        val isNewer: Boolean
    )

    suspend fun checkAndSyncVersion(): Result<VersionCheckResult> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(VERSION_CHECK_URL)
                    .header("User-Agent", "CN-ShiftCal/1.0 (Android)")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val bodyStr = response.body?.string()
                if (bodyStr.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Empty body"))
                }

                val root = JSONObject(bodyStr)
                val remoteMain = root.optString("main", "0.7.12")
                val localVersion = getAppVersion()
                Log.d(TAG, "Version check: remoteMain=$remoteMain, localVersion=$localVersion")

                val isNewer = isVersionNewer(remoteMain, localVersion)

                Result.success(VersionCheckResult(remoteMain, isNewer))
            } catch (e: Exception) {
                Log.e(TAG, "Version check error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Initial synchronous load from cache or embedded assets
     */
    private fun loadInitialConfig(): CalendarConfig {
        val cached = prefs.getString(KEY_CACHED_JSON, null)
        if (!cached.isNullOrEmpty()) {
            try {
                val parsed = parseConfigJson(cached)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse cached config, falling back to embedded config", e)
            }
        }
        return parseConfigJson(EMBEDDED_CONFIG_JSON) ?: CalendarConfig(curYear = 2026)
    }

    private fun loadInitialNextConfig(): CalendarConfig? {
        val cached = prefs.getString(KEY_CACHED_NEXT_JSON, null)
        if (!cached.isNullOrEmpty()) {
            try {
                val parsed = parseConfigJson(cached)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse cached next config, falling back to embedded next config", e)
            }
        }
        return parseConfigJson(EMBEDDED_NEXT_CONFIG_JSON)
    }

    /**
     * Fetch the config from the remote URL asynchronously
     */
    suspend fun fetchRemoteConfig(customUrl: String? = null): Result<CalendarConfig> {
        return withContext(Dispatchers.IO) {
            _syncState.value = SyncState.Loading
            val rawUrl = customUrl ?: getConfigUrl()
            val targetUrl = when {
                rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                rawUrl.startsWith("/") -> "https://kkii.org$rawUrl"
                else -> "https://$rawUrl"
            }

            try {
                val timestamp = System.currentTimeMillis() / 1000
                val separator = if (targetUrl.contains("?")) "&" else "?"
                val finalUrl = "$targetUrl${separator}d=$timestamp"

                val request = Request.Builder()
                    .url(finalUrl)
                    .header("User-Agent", "CN-ShiftCal/1.0 (Android)")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    Log.w(TAG, "fetchRemoteConfig failed: $errMsg. Silently falling back to local configuration.")
                    _syncState.value = if (getLastSyncTime() > 0L) SyncState.Success("配置已同步", getLastSyncTime()) else SyncState.Idle
                    return@withContext Result.success(configState.value)
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    Log.w(TAG, "fetchRemoteConfig: empty body. Silently falling back to local configuration.")
                    _syncState.value = if (getLastSyncTime() > 0L) SyncState.Success("配置已同步", getLastSyncTime()) else SyncState.Idle
                    return@withContext Result.success(configState.value)
                }

                val config = parseConfigJson(bodyString)
                if (config == null) {
                    Log.w(TAG, "fetchRemoteConfig: parseConfigJson returned null. Silently falling back to safe local configuration.")
                    _syncState.value = if (getLastSyncTime() > 0L) SyncState.Success("配置已同步", getLastSyncTime()) else SyncState.Idle
                    return@withContext Result.success(configState.value)
                }

                // Save to local cache ONLY after successful parse
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putString(KEY_CACHED_JSON, bodyString)
                    .putLong(KEY_LAST_SYNC, now)
                    .apply()

                _configState.value = config
                _syncState.value = SyncState.Success("配置已同步 (${config.curYear}年)", now)
                
                Result.success(config)
            } catch (e: Exception) {
                Log.w(TAG, "Network error fetching remote config. Silently falling back to local configuration.", e)
                _syncState.value = if (getLastSyncTime() > 0L) SyncState.Success("配置已同步", getLastSyncTime()) else SyncState.Idle
                Result.success(configState.value)
            }
        }
    }

    suspend fun fetchNextYearConfig(customUrl: String? = null): Result<CalendarConfig> {
        return withContext(Dispatchers.IO) {
            val fallbackConfig = nextYearConfigState.value ?: loadInitialNextConfig() ?: CalendarConfig(curYear = 2027)
            try {
                val rawUrl = customUrl ?: deriveNextConfigUrl(getConfigUrl())
                val targetUrl = when {
                    rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                    rawUrl.startsWith("/") -> "https://kkii.org$rawUrl"
                    else -> "https://$rawUrl"
                }

                val timestamp = System.currentTimeMillis() / 1000
                val separator = if (targetUrl.contains("?")) "&" else "?"
                val finalUrl = "$targetUrl${separator}d=$timestamp"

                val request = Request.Builder()
                    .url(finalUrl)
                    .header("User-Agent", "CN-ShiftCal/1.0 (Android)")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    Log.w(TAG, "fetchNextYearConfig failed: $errMsg. Silently using fallback next config.")
                    return@withContext Result.success(fallbackConfig)
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    Log.w(TAG, "fetchNextYearConfig received empty body. Silently using fallback next config.")
                    return@withContext Result.success(fallbackConfig)
                }

                val config = parseConfigJson(bodyString)
                if (config != null) {
                    prefs.edit().putString(KEY_CACHED_NEXT_JSON, bodyString).apply()
                    _nextYearConfigState.value = config
                    Log.d(TAG, "Successfully updated nextYearConfig: year=${config.curYear}")
                    Result.success(config)
                } else {
                    Log.w(TAG, "fetchNextYearConfig failed to parse JSON. Silently using fallback next config.")
                    Result.success(fallbackConfig)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error fetching next year config. Silently using fallback: ${e.localizedMessage}")
                Result.success(fallbackConfig)
            }
        }
    }

    private fun deriveNextConfigUrl(mainUrl: String): String {
        return if (mainUrl.contains("config.json")) {
            mainUrl.replace("config.json", "config-next.json")
        } else {
            NEXT_CONFIG_URL
        }
    }

    /**
     * Parses the custom config.json structure into CalendarConfig with robust fault tolerance.
     * Automatically filters and silently ignores out-of-range dates (e.g. month > 12 like month 14,
     * day > 31 like day 32, or malformed items), without throwing errors or showing errors to the user.
     */
    fun parseConfigJson(jsonStr: String?): CalendarConfig? {
        if (jsonStr.isNullOrBlank()) return null
        val trimmed = jsonStr.trim()
        // Defensive check: must start and end with curly braces (prevent HTML/error responses from throwing unexpected crashes)
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            Log.w(TAG, "parseConfigJson: Content is not a valid JSON object, silently ignoring. Length=${trimmed.length}")
            return null
        }

        return try {
            val root = JSONObject(trimmed)
            var curYear = root.optInt("curYear", 0)
            if (curYear < 1900 || curYear > 2100) {
                // Silently fallback to 2026 without throwing errors
                curYear = 2026
            }
            val startWeek = root.optInt("startWeek", 4)

            // monthsLimit
            val monthsLimit = mutableListOf<Int>()
            val monthsArray = root.optJSONArray("monthsLimit")
            if (monthsArray != null) {
                for (i in 0 until monthsArray.length()) {
                    val limit = monthsArray.optInt(i, 0)
                    if (limit in 28..31 && monthsLimit.size < 12) {
                        monthsLimit.add(limit)
                    }
                }
            }

            // lunarStart
            val lunarStart = mutableListOf<Int>()
            val lunarStartArray = root.optJSONArray("lunarStart")
            if (lunarStartArray != null) {
                for (i in 0 until lunarStartArray.length()) {
                    lunarStart.add(lunarStartArray.optInt(i, 0))
                }
            }

            // lunarMonth
            val lunarMonth = mutableListOf<Int>()
            val lunarMonthArray = root.optJSONArray("lunarMonth")
            if (lunarMonthArray != null) {
                for (i in 0 until lunarMonthArray.length()) {
                    val m = lunarMonthArray.optInt(i, 0)
                    if (m in 1..12) lunarMonth.add(m)
                }
            }

            // lunarMonthLimit
            val lunarMonthLimit = mutableListOf<Int>()
            val lunarMonthLimitArray = root.optJSONArray("lunarMonthLimit")
            if (lunarMonthLimitArray != null) {
                for (i in 0 until lunarMonthLimitArray.length()) {
                    val limit = lunarMonthLimitArray.optInt(i, 0)
                    if (limit in 28..31) lunarMonthLimit.add(limit)
                }
            }

            // lunarFestivals (silently ignores month > 12 or day > 31)
            val lunarFestivals = mutableListOf<FestivalItem>()
            val lunarFestivalsArray = root.optJSONArray("lunarFestivals")
            if (lunarFestivalsArray != null) {
                for (i in 0 until lunarFestivalsArray.length()) {
                    val obj = lunarFestivalsArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    val date = obj.optString("date", "").trim()
                    if (name.isNotBlank() && date.isNotBlank()) {
                        val parts = date.split("-")
                        if (parts.size == 2) {
                            val m = parts[0].toIntOrNull()
                            val d = parts[1].toIntOrNull()
                            // Silently ignore invalid lunar month (0..12) or invalid day (1..31)
                            if (m != null && m in 0..12 && d != null && d in 1..31) {
                                lunarFestivals.add(
                                    FestivalItem(
                                        date = "$m-$d",
                                        name = name,
                                        tag = obj.optInt("tag", 0)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Festivals (Solar: silently ignores month > 12 like month 14, or day > 31 like day 32)
            val festivals = mutableListOf<FestivalItem>()
            val festivalsArray = root.optJSONArray("Festivals")
            if (festivalsArray != null) {
                for (i in 0 until festivalsArray.length()) {
                    val obj = festivalsArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    val date = obj.optString("date", "").trim()
                    if (name.isNotBlank() && date.isNotBlank()) {
                        val parts = date.split("-")
                        if (parts.size == 2) {
                            val m = parts[0].toIntOrNull()
                            val d = parts[1].toIntOrNull()
                            // Silently ignore out-of-bounds solar month (1..12) or day (1..31)
                            if (m != null && m in 1..12 && d != null && d in 1..31) {
                                festivals.add(
                                    FestivalItem(
                                        date = "$m-$d",
                                        name = name,
                                        tag = obj.optInt("tag", 0)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // localeFestivals (silently ignores month > 12 like month 14, or day > 31 like day 32)
            val localeFestivals = mutableListOf<LocaleFestivalItem>()
            val localeFestivalsArray = root.optJSONArray("localeFestivals")
            if (localeFestivalsArray != null) {
                for (i in 0 until localeFestivalsArray.length()) {
                    val obj = localeFestivalsArray.optJSONObject(i) ?: continue
                    val m = obj.optInt("m", 0)
                    // If month is not 1..12 (e.g. month 14), silently ignore and skip
                    if (m !in 1..12) continue
                    val dList = mutableListOf<Int>()
                    val dArr = obj.optJSONArray("d")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            val day = dArr.optInt(k, 0)
                            // If day is not 1..31 (e.g. day 32), silently ignore and skip
                            if (day in 1..31) dList.add(day)
                        }
                    }
                    if (dList.isNotEmpty()) {
                        localeFestivals.add(LocaleFestivalItem(m, dList))
                    }
                }
            }

            // restDays (silently ignores month > 12 like month 14, or day > 31 like day 32)
            val restDays = mutableListOf<RestDayMonth>()
            val restDaysArray = root.optJSONArray("restDays")
            if (restDaysArray != null) {
                for (i in 0 until restDaysArray.length()) {
                    val obj = restDaysArray.optJSONObject(i) ?: continue
                    val m = obj.optInt("m", 0)
                    // If month is not 1..12 (e.g. month 14), silently ignore and skip
                    if (m !in 1..12) continue
                    val dList = mutableListOf<Int>()
                    val dArr = obj.optJSONArray("d")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            val day = dArr.optInt(k, 0)
                            // If day is not 1..31 (e.g. day 32), silently ignore and skip
                            if (day in 1..31) dList.add(day)
                        }
                    }
                    if (dList.isNotEmpty()) {
                        restDays.add(RestDayMonth(m, dList))
                    }
                }
            }

            CalendarConfig(
                curYear = curYear,
                startWeek = startWeek,
                monthsLimit = if (monthsLimit.isNotEmpty()) monthsLimit else listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31),
                lunarStart = if (lunarStart.isNotEmpty()) lunarStart else listOf(0, 13),
                lunarMonth = if (lunarMonth.isNotEmpty()) lunarMonth else listOf(11, 12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                lunarMonthLimit = if (lunarMonthLimit.isNotEmpty()) lunarMonthLimit else listOf(30, 29, 30, 29, 30, 29, 29, 30, 29, 29, 30, 30, 30),
                lunarFestivals = lunarFestivals,
                festivals = festivals,
                localeFestivals = localeFestivals,
                restDays = restDays
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseConfigJson caught exception, silently returning null", e)
            null
        }
    }
}
