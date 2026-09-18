package com.sunplace.calendar.data.repository

import android.content.Context
import android.util.Log
import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.LocaleFestivalItem
import com.sunplace.calendar.data.model.RestDayMonth
import com.sunplace.calendar.data.model.YearConfig
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
            get() = String(android.util.Base64.decode("aHR0cHM6Ly9ra2lpLm9yZy9hc3NldHMvanMvY2Fscy9hbmRyb2lkLWFwcC5qc29u", android.util.Base64.DEFAULT))

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

        private const val EMBEDDED_CONFIG_JSON = """{"curYear":2026,"localeFestivals":{"26":[{"m":4,"d":[17,20]}],"27":[{"m":4,"d":[16,19]}]},"restDays":{"26":[{"m":1,"d":[1,2,3,10,11,17,18,24,25,31]},{"m":2,"d":[1,7,8,15,16,17,18,19,20,21,22,23]},{"m":3,"d":[1,7,8,14,15,21,22,28,29]},{"m":4,"d":[4,5,6,11,12,18,19,25,26]},{"m":5,"d":[1,2,3,4,5,10,16,17,23,24,30,31]},{"m":6,"d":[6,7,13,14,19,20,21,27,28]},{"m":7,"d":[4,5,11,12,18,19,25,26]},{"m":8,"d":[1,2,8,9,15,16,22,23,29,30]},{"m":9,"d":[5,6,12,13,19,25,26,27]},{"m":10,"d":[1,2,3,4,5,6,7,11,17,18,24,25,31]},{"m":11,"d":[1,7,8,14,15,21,22,28,29]},{"m":12,"d":[5,6,12,13,19,20,26,27]}],"27":[{"m":1,"d":[1,2,3]},{"m":2,"d":[6,7,8,9,10,11]},{"m":3,"d":[]},{"m":4,"d":[]},{"m":5,"d":[1,2,3,4,5]},{"m":6,"d":[]},{"m":7,"d":[]},{"m":8,"d":[]},{"m":9,"d":[]},{"m":10,"d":[1,2,3,4,5,6,7]},{"m":11,"d":[]},{"m":12,"d":[]}]}}"""

        private const val EMBEDDED_NEXT_CONFIG_JSON = """{"curYear":2027,"localeFestivals":[{"m":4,"d":[16,19]}],"restDays":[{"m":1,"d":[1,2,3]},{"m":2,"d":[6,7,8,9,10,11]},{"m":3,"d":[]},{"m":4,"d":[]},{"m":5,"d":[1,2,3,4,5]},{"m":6,"d":[]},{"m":7,"d":[]},{"m":8,"d":[]},{"m":9,"d":[]},{"m":10,"d":[1,2,3,4,5,6,7]},{"m":11,"d":[]},{"m":12,"d":[]}]}"""
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
                val nextYear = config.curYear + 1
                val nextYearConfig = config.getYearConfig(nextYear)
                if (nextYearConfig != null) {
                    _nextYearConfigState.value = CalendarConfig(
                        curYear = nextYear,
                        restDays = nextYearConfig.restDays,
                        localeFestivals = nextYearConfig.localeFestivals,
                        yearlyConfigs = config.yearlyConfigs
                    )
                }
                _syncState.value = SyncState.Success("配置已同步", now)
                
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
     * Parses android-app.json or config.json structure into CalendarConfig with robust fault tolerance.
     */
    fun parseConfigJson(jsonStr: String?): CalendarConfig? {
        if (jsonStr.isNullOrBlank()) return null
        val trimmed = jsonStr.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }

        return try {
            val root = JSONObject(trimmed)
            var curYear = root.optInt("curYear", 0)
            if (curYear < 1900 || curYear > 2100) {
                curYear = 2026
            }

            val yearlyConfigs = mutableMapOf<Int, YearConfig>()
            val yearlyRestDays = mutableMapOf<Int, MutableList<RestDayMonth>>()
            val yearlyLocaleFestivals = mutableMapOf<Int, MutableList<LocaleFestivalItem>>()

            fun parseYearKey(key: String): Int? {
                val num = key.trim().toIntOrNull() ?: return null
                return if (num in 0..99) 2000 + num else if (num in 1900..2100) num else null
            }

            fun parseRestDaysArray(arr: JSONArray?): List<RestDayMonth> {
                if (arr == null) return emptyList()
                val list = mutableListOf<RestDayMonth>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val m = obj.optInt("m", 0)
                    if (m !in 1..12) continue
                    val dList = mutableListOf<Int>()
                    val dArr = obj.optJSONArray("d")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            val day = dArr.optInt(k, 0)
                            if (day in 1..31) dList.add(day)
                        }
                    }
                    if (dList.isNotEmpty()) {
                        list.add(RestDayMonth(m, dList))
                    }
                }
                return list
            }

            fun parseLocaleFestivalsArray(arr: JSONArray?): List<LocaleFestivalItem> {
                if (arr == null) return emptyList()
                val list = mutableListOf<LocaleFestivalItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val m = obj.optInt("m", 0)
                    if (m !in 1..12) continue
                    val dList = mutableListOf<Int>()
                    val dArr = obj.optJSONArray("d")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            val day = dArr.optInt(k, 0)
                            if (day in 1..31) dList.add(day)
                        }
                    }
                    if (dList.isNotEmpty()) {
                        list.add(LocaleFestivalItem(m, dList))
                    }
                }
                return list
            }

            val restDaysJsonObj = root.optJSONObject("restDays")
            val restDaysJsonArr = root.optJSONArray("restDays")
            val defaultRestDays = mutableListOf<RestDayMonth>()

            if (restDaysJsonObj != null) {
                val keys = restDaysJsonObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val y = parseYearKey(k) ?: continue
                    val arr = restDaysJsonObj.optJSONArray(k)
                    val rList = parseRestDaysArray(arr)
                    yearlyRestDays.getOrPut(y) { mutableListOf() }.addAll(rList)
                }
            } else if (restDaysJsonArr != null) {
                defaultRestDays.addAll(parseRestDaysArray(restDaysJsonArr))
                yearlyRestDays.getOrPut(curYear) { mutableListOf() }.addAll(defaultRestDays)
            }

            val localeFestivalsJsonObj = root.optJSONObject("localeFestivals")
            val localeFestivalsJsonArr = root.optJSONArray("localeFestivals")
            val defaultLocaleFestivals = mutableListOf<LocaleFestivalItem>()

            if (localeFestivalsJsonObj != null) {
                val keys = localeFestivalsJsonObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val y = parseYearKey(k) ?: continue
                    val arr = localeFestivalsJsonObj.optJSONArray(k)
                    val lList = parseLocaleFestivalsArray(arr)
                    yearlyLocaleFestivals.getOrPut(y) { mutableListOf() }.addAll(lList)
                }
            } else if (localeFestivalsJsonArr != null) {
                defaultLocaleFestivals.addAll(parseLocaleFestivalsArray(localeFestivalsJsonArr))
                yearlyLocaleFestivals.getOrPut(curYear) { mutableListOf() }.addAll(defaultLocaleFestivals)
            }

            val allYears = (yearlyRestDays.keys + yearlyLocaleFestivals.keys).toSet()
            for (y in allYears) {
                yearlyConfigs[y] = YearConfig(
                    year = y,
                    restDays = yearlyRestDays[y] ?: emptyList(),
                    localeFestivals = yearlyLocaleFestivals[y] ?: emptyList()
                )
            }

            val curYearRestDays = yearlyConfigs[curYear]?.restDays ?: defaultRestDays
            val curYearLocaleFestivals = yearlyConfigs[curYear]?.localeFestivals ?: defaultLocaleFestivals

            CalendarConfig(
                curYear = curYear,
                localeFestivals = curYearLocaleFestivals,
                restDays = curYearRestDays,
                yearlyConfigs = yearlyConfigs
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseConfigJson caught exception, silently returning null", e)
            null
        }
    }
}
