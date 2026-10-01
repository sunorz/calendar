package com.sunplace.calendar.data.repository

import android.content.Context
import android.util.Base64
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
import java.util.concurrent.TimeUnit

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String, val lastSyncTime: Long) : SyncState()
    data class Error(val error: String, val lastSyncTime: Long? = null) : SyncState()
}

class CalendarRepository(private val context: Context) {

    companion object {
        private fun decodeBase64Url(encoded: String): String =
            String(Base64.decode(encoded, Base64.DEFAULT))

        val DEFAULT_CONFIG_URL: String
            get() = decodeBase64Url("aHR0cHM6Ly9ra2lpLm9yZy9hc3NldHMvanMvY2Fscy9hbmRyb2lkLWFwcC5qc29u")

        val NEXT_CONFIG_URL: String
            get() = decodeBase64Url("aHR0cHM6Ly9ra2lpLm9yZy9hc3NldHMvanMvY2Fscy9jb25maWctbmV4dC5qc29u")

        val VERSION_CHECK_URL: String
            get() = decodeBase64Url("aHR0cHM6Ly9ra2lpLm9yZy9hcHAvY2FsZW5kYXItdmVyc2lvbi5qc29u")

        private const val PREFS_NAME = "calendar_prefs"
        private const val KEY_CACHED_JSON = "cached_config_json"
        private const val KEY_CACHED_NEXT_JSON = "cached_next_config_json"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_CUSTOM_URL = "custom_config_url"
        private const val KEY_APP_VERSION = "app_version_override"
        private const val USER_AGENT = "CN-ShiftCal/1.0 (Android)"
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

    private val _syncState = MutableStateFlow(resolveRestoredSyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private fun resolveRestoredSyncState(): SyncState {
        val lastSync = getLastSyncTime()
        return if (lastSync > 0L) SyncState.Success("配置已同步", lastSync) else SyncState.Idle
    }

    fun getConfigUrl(): String =
        prefs.getString(KEY_CUSTOM_URL, DEFAULT_CONFIG_URL) ?: DEFAULT_CONFIG_URL

    fun setConfigUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_URL, url).apply()
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.7.5"
        } catch (_: Exception) {
            prefs.getString(KEY_APP_VERSION, "0.7.5") ?: "0.7.5"
        }
    }

    fun setAppVersion(version: String) {
        prefs.edit().putString(KEY_APP_VERSION, version).apply()
    }

    fun formatVersionWithGitHash(baseVersion: String, gitHash: String?): String {
        val cleanBase = baseVersion.substringBefore("(").trim()
        val cleanHash = gitHash?.trim()?.take(7).orEmpty()
        return if (cleanHash.isNotEmpty()) "$cleanBase($cleanHash)" else cleanBase
    }

    fun isVersionNewer(remoteVersion: String, localVersion: String): Boolean {
        val cleanRemote = remoteVersion.substringBefore("(").trim()
        val cleanLocal = localVersion.substringBefore("(").trim()
        try {
            val remoteParts = cleanRemote.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            val localParts = cleanLocal.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (_: Exception) {
            // fallback
        }
        return cleanRemote != cleanLocal
    }

    data class VersionCheckResult(
        val remoteMain: String,
        val isNewer: Boolean
    )

    private fun normalizeAndCacheBustUrl(rawUrl: String): String {
        val targetUrl = when {
            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
            rawUrl.startsWith("/") -> "https://kkii.org$rawUrl"
            else -> "https://$rawUrl"
        }
        val timestamp = System.currentTimeMillis() / 1000
        val separator = if (targetUrl.contains("?")) "&" else "?"
        return "$targetUrl${separator}d=$timestamp"
    }

    private fun buildNoCacheGetRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()
    }

    suspend fun syncServerTime(): Boolean {
        return com.sunplace.calendar.util.ServerTimeManager.syncOptimalTime(context)
    }

    suspend fun checkAndSyncVersion(): Result<VersionCheckResult> {
        return withContext(Dispatchers.IO) {
            try {
                val request = buildNoCacheGetRequest(VERSION_CHECK_URL)
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

                Result.success(VersionCheckResult(remoteMain, isVersionNewer(remoteMain, localVersion)))
            } catch (e: Exception) {
                Log.e(TAG, "Version check error", e)
                Result.failure(e)
            }
        }
    }

    private fun loadCachedOrEmbeddedConfig(cacheKey: String, embeddedJson: String): CalendarConfig? {
        val cached = prefs.getString(cacheKey, null)
        if (!cached.isNullOrEmpty()) {
            try {
                parseConfigJson(cached)?.let { return it }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse cached config ($cacheKey), falling back to embedded config", e)
            }
        }
        return parseConfigJson(embeddedJson)
    }

    private fun loadInitialConfig(): CalendarConfig =
        loadCachedOrEmbeddedConfig(KEY_CACHED_JSON, EMBEDDED_CONFIG_JSON) ?: CalendarConfig(curYear = 2026)

    private fun loadInitialNextConfig(): CalendarConfig? =
        loadCachedOrEmbeddedConfig(KEY_CACHED_NEXT_JSON, EMBEDDED_NEXT_CONFIG_JSON)

    /**
     * Fetch the config from the remote URL asynchronously
     */
    suspend fun fetchRemoteConfig(customUrl: String? = null): Result<CalendarConfig> {
        return withContext(Dispatchers.IO) {
            _syncState.value = SyncState.Loading
            val finalUrl = normalizeAndCacheBustUrl(customUrl ?: getConfigUrl())

            try {
                val response = okHttpClient.newCall(buildNoCacheGetRequest(finalUrl)).execute()
                val bodyString = if (response.isSuccessful) response.body?.string() else null
                val config = if (!bodyString.isNullOrBlank()) parseConfigJson(bodyString) else null

                if (config == null) {
                    Log.w(TAG, "fetchRemoteConfig failed or returned invalid JSON. Silently falling back to local configuration.")
                    _syncState.value = resolveRestoredSyncState()
                    return@withContext Result.success(configState.value)
                }

                val now = System.currentTimeMillis()
                prefs.edit()
                    .putString(KEY_CACHED_JSON, bodyString)
                    .putLong(KEY_LAST_SYNC, now)
                    .apply()

                _configState.value = config
                val nextYear = config.curYear + 1
                config.getYearConfig(nextYear)?.let { nextYearConfig ->
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
                _syncState.value = resolveRestoredSyncState()
                Result.success(configState.value)
            }
        }
    }

    suspend fun fetchNextYearConfig(customUrl: String? = null): Result<CalendarConfig> {
        return withContext(Dispatchers.IO) {
            val fallbackConfig = nextYearConfigState.value ?: loadInitialNextConfig() ?: CalendarConfig(curYear = 2027)
            try {
                val finalUrl = normalizeAndCacheBustUrl(customUrl ?: deriveNextConfigUrl(getConfigUrl()))
                val response = okHttpClient.newCall(buildNoCacheGetRequest(finalUrl)).execute()
                val bodyString = if (response.isSuccessful) response.body?.string() else null
                val config = if (!bodyString.isNullOrBlank()) parseConfigJson(bodyString) else null

                if (config != null) {
                    prefs.edit().putString(KEY_CACHED_NEXT_JSON, bodyString).apply()
                    _nextYearConfigState.value = config
                    Result.success(config)
                } else {
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

            fun parseYearKey(key: String): Int? {
                val num = key.trim().toIntOrNull() ?: return null
                return when (num) {
                    in 0..99 -> 2000 + num
                    in 1900..2100 -> num
                    else -> null
                }
            }

            fun <T> parseMonthDaysArray(arr: JSONArray?, factory: (Int, List<Int>) -> T): List<T> {
                if (arr == null) return emptyList()
                val list = mutableListOf<T>()
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
                        list.add(factory(m, dList))
                    }
                }
                return list
            }

            fun <T> parseYearlyOrFlatSection(
                fieldName: String,
                yearlyMap: MutableMap<Int, MutableList<T>>,
                defaultList: MutableList<T>,
                factory: (Int, List<Int>) -> T
            ) {
                val jsonObj = root.optJSONObject(fieldName)
                val jsonArr = root.optJSONArray(fieldName)
                if (jsonObj != null) {
                    val keys = jsonObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val y = parseYearKey(k) ?: continue
                        val parsed = parseMonthDaysArray(jsonObj.optJSONArray(k), factory)
                        yearlyMap.getOrPut(y) { mutableListOf() }.addAll(parsed)
                    }
                } else if (jsonArr != null) {
                    defaultList.addAll(parseMonthDaysArray(jsonArr, factory))
                    yearlyMap.getOrPut(curYear) { mutableListOf() }.addAll(defaultList)
                }
            }

            val yearlyRestDays = mutableMapOf<Int, MutableList<RestDayMonth>>()
            val defaultRestDays = mutableListOf<RestDayMonth>()
            parseYearlyOrFlatSection("restDays", yearlyRestDays, defaultRestDays, ::RestDayMonth)

            val yearlyLocaleFestivals = mutableMapOf<Int, MutableList<LocaleFestivalItem>>()
            val defaultLocaleFestivals = mutableListOf<LocaleFestivalItem>()
            parseYearlyOrFlatSection("localeFestivals", yearlyLocaleFestivals, defaultLocaleFestivals, ::LocaleFestivalItem)

            val yearlyConfigs = mutableMapOf<Int, YearConfig>()
            val allYears = yearlyRestDays.keys + yearlyLocaleFestivals.keys
            for (y in allYears) {
                yearlyConfigs[y] = YearConfig(
                    year = y,
                    restDays = yearlyRestDays[y] ?: emptyList(),
                    localeFestivals = yearlyLocaleFestivals[y] ?: emptyList()
                )
            }

            CalendarConfig(
                curYear = curYear,
                localeFestivals = yearlyConfigs[curYear]?.localeFestivals ?: defaultLocaleFestivals,
                restDays = yearlyConfigs[curYear]?.restDays ?: defaultRestDays,
                yearlyConfigs = yearlyConfigs
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseConfigJson caught exception, silently returning null", e)
            null
        }
    }
}
