package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CalendarConfig
import com.example.data.model.FestivalItem
import com.example.data.model.LocaleFestivalItem
import com.example.data.model.RestDayMonth
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
        const val DEFAULT_CONFIG_URL = "https://kkii.org/assets/js/cals/config.json"
        const val VERSION_CHECK_URL = "https://kkii.org/app/calendar-version.json"
        private const val PREFS_NAME = "calendar_prefs"
        private const val KEY_CACHED_JSON = "cached_config_json"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_CUSTOM_URL = "custom_config_url"
        private const val KEY_APP_VERSION = "app_version_override"
        private const val TAG = "CalendarRepository"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadInitialConfig())
    val configState: StateFlow<CalendarConfig> = _configState.asStateFlow()

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
            pInfo.versionName ?: "0.7.3"
        } catch (e: Exception) {
            prefs.getString(KEY_APP_VERSION, "0.7.3") ?: "0.7.3"
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
        val isNewer: Boolean,
        val configUpdated: Boolean
    )

    suspend fun checkAndSyncVersion(): Result<VersionCheckResult> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(VERSION_CHECK_URL)
                    .header("User-Agent", "CalendarApp/1.0 (Android)")
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
                val remoteMain = root.optString("main", "0.6.1")
                val localVersion = getAppVersion()
                Log.d(TAG, "Version check: remoteMain=$remoteMain, localVersion=$localVersion")

                var configUpdated = false
                val configVal = root.opt("config")
                val remoteConfigStr = when (configVal) {
                    is JSONObject -> configVal.toString()
                    is String -> {
                        if (configVal.trim().startsWith("{")) {
                            configVal
                        } else {
                            null
                        }
                    }
                    else -> null
                }
                if (remoteConfigStr != null) {
                    val localCached = prefs.getString(KEY_CACHED_JSON, null)
                    if (remoteConfigStr != localCached) {
                        val parsed = parseConfigJson(remoteConfigStr)
                        if (parsed != null) {
                            prefs.edit().putString(KEY_CACHED_JSON, remoteConfigStr).apply()
                            _configState.value = parsed
                            configUpdated = true
                        }
                    }
                }

                val isNewer = isVersionNewer(remoteMain, localVersion)

                Result.success(VersionCheckResult(remoteMain, isNewer, configUpdated))
            } catch (e: Exception) {
                Log.e(TAG, "Version check error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Initial synchronous load from cache or assets
     */
    private fun loadInitialConfig(): CalendarConfig {
        val cached = prefs.getString(KEY_CACHED_JSON, null)
        if (!cached.isNullOrEmpty()) {
            try {
                val parsed = parseConfigJson(cached)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse cached config, falling back to assets", e)
            }
        }
        return loadFromAssets()
    }

    /**
     * Load bundled fallback config from APK assets
     */
    private fun loadFromAssets(): CalendarConfig {
        return try {
            val json = context.assets.open("config.json").bufferedReader().use { it.readText() }
            parseConfigJson(json) ?: CalendarConfig()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config from assets", e)
            CalendarConfig()
        }
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
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "CalendarApp/1.0 (Android)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    _syncState.value = SyncState.Error(errMsg, getLastSyncTime())
                    return@withContext Result.failure(Exception(errMsg))
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    val errMsg = "Empty response from config URL"
                    _syncState.value = SyncState.Error(errMsg, getLastSyncTime())
                    return@withContext Result.failure(Exception(errMsg))
                }

                val config = parseConfigJson(bodyString)
                if (config == null) {
                    val errMsg = "Failed to parse JSON config"
                    _syncState.value = SyncState.Error(errMsg, getLastSyncTime())
                    return@withContext Result.failure(Exception(errMsg))
                }

                // Save to local cache
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putString(KEY_CACHED_JSON, bodyString)
                    .putLong(KEY_LAST_SYNC, now)
                    .apply()

                _configState.value = config
                _syncState.value = SyncState.Success("配置已同步 (${config.curYear}年)", now)
                Result.success(config)
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching remote config", e)
                val errMsg = e.localizedMessage ?: "Network connection failed"
                _syncState.value = SyncState.Error("同步失败: $errMsg (已使用本地配置)", getLastSyncTime())
                Result.failure(e)
            }
        }
    }

    /**
     * Parses the custom config.json structure into CalendarConfig
     */
    fun parseConfigJson(jsonStr: String): CalendarConfig? {
        return try {
            val root = JSONObject(jsonStr)
            val curYear = root.optInt("curYear", 2026)
            val startWeek = root.optInt("startWeek", 4)

            // monthsLimit
            val monthsLimit = mutableListOf<Int>()
            val monthsArray = root.optJSONArray("monthsLimit")
            if (monthsArray != null) {
                for (i in 0 until monthsArray.length()) {
                    monthsLimit.add(monthsArray.getInt(i))
                }
            }

            // lunarStart
            val lunarStart = mutableListOf<Int>()
            val lunarStartArray = root.optJSONArray("lunarStart")
            if (lunarStartArray != null) {
                for (i in 0 until lunarStartArray.length()) {
                    lunarStart.add(lunarStartArray.getInt(i))
                }
            }

            // lunarMonth
            val lunarMonth = mutableListOf<Int>()
            val lunarMonthArray = root.optJSONArray("lunarMonth")
            if (lunarMonthArray != null) {
                for (i in 0 until lunarMonthArray.length()) {
                    lunarMonth.add(lunarMonthArray.getInt(i))
                }
            }

            // lunarMonthLimit
            val lunarMonthLimit = mutableListOf<Int>()
            val lunarMonthLimitArray = root.optJSONArray("lunarMonthLimit")
            if (lunarMonthLimitArray != null) {
                for (i in 0 until lunarMonthLimitArray.length()) {
                    lunarMonthLimit.add(lunarMonthLimitArray.getInt(i))
                }
            }

            // lunarFestivals
            val lunarFestivals = mutableListOf<FestivalItem>()
            val lunarFestivalsArray = root.optJSONArray("lunarFestivals")
            if (lunarFestivalsArray != null) {
                for (i in 0 until lunarFestivalsArray.length()) {
                    val obj = lunarFestivalsArray.getJSONObject(i)
                    lunarFestivals.add(
                        FestivalItem(
                            date = obj.optString("date"),
                            name = obj.optString("name"),
                            tag = obj.optInt("tag", 0)
                        )
                    )
                }
            }

            // Festivals (Solar)
            val festivals = mutableListOf<FestivalItem>()
            val festivalsArray = root.optJSONArray("Festivals")
            if (festivalsArray != null) {
                for (i in 0 until festivalsArray.length()) {
                    val obj = festivalsArray.getJSONObject(i)
                    festivals.add(
                        FestivalItem(
                            date = obj.optString("date"),
                            name = obj.optString("name"),
                            tag = obj.optInt("tag", 0)
                        )
                    )
                }
            }

            // localeFestivals
            val localeFestivals = mutableListOf<LocaleFestivalItem>()
            val localeFestivalsArray = root.optJSONArray("localeFestivals")
            if (localeFestivalsArray != null) {
                for (i in 0 until localeFestivalsArray.length()) {
                    val obj = localeFestivalsArray.getJSONObject(i)
                    val m = obj.optInt("m")
                    val dList = mutableListOf<Int>()
                    val dArr = obj.optJSONArray("d")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            dList.add(dArr.getInt(k))
                        }
                    }
                    localeFestivals.add(LocaleFestivalItem(m, dList))
                }
            }

            // restDays
            val restDays = mutableListOf<RestDayMonth>()
            val restDaysArray = root.optJSONArray("restDays")
            if (restDaysArray != null) {
                for (i in 0 until restDaysArray.length()) {
                    val obj = restDaysArray.getJSONObject(i)
                    val m = obj.optInt("m")
                    val dList = mutableListOf<Int>()
                    val dArr = obj.optJSONArray("d")
                    if (dArr != null) {
                        for (k in 0 until dArr.length()) {
                            dList.add(dArr.getInt(k))
                        }
                    }
                    restDays.add(RestDayMonth(m, dList))
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
            Log.e(TAG, "Error parsing JSON", e)
            null
        }
    }
}
