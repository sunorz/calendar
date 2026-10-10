package com.sunplace.calendar.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object ServerTimeManager {
    private const val TAG = "ServerTimeManager"
    private const val PREFS_NAME = "server_time_prefs"
    private const val KEY_SERVER_EPOCH = "server_epoch_ms"
    private const val KEY_ELAPSED_REALTIME = "elapsed_realtime_ms"
    private const val KEY_SYSTEM_TIME = "system_time_ms"
    private const val KEY_SERVER_OFFSET = "server_offset_ms"

    val CHINA_TIME_ZONE: TimeZone = TimeZone.getTimeZone("GMT+8")

    /**
     * NTP server candidate list for selecting the best/fastest source.
     */
    val NTP_SERVERS = listOf(
        "ntp.aliyun.com",
        "time.cloudflare.com",
        "pool.ntp.org"
    )

    private val HTTP_FALLBACK_URLS = listOf(
        "https://time.cloudflare.com",
        "https://www.aliyun.com"
    )

    private val fallbackHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var serverEpochAnchor: Long = 0L

    @Volatile
    private var elapsedRealtimeAnchor: Long = 0L

    @Volatile
    private var fallbackServerOffset: Long = 0L

    @Volatile
    private var isCalibrated: Boolean = false

    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm?.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                cm?.activeNetworkInfo?.isConnected == true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedServerEpoch = prefs.getLong(KEY_SERVER_EPOCH, 0L)
            val savedElapsed = prefs.getLong(KEY_ELAPSED_REALTIME, 0L)
            val currentElapsed = SystemClock.elapsedRealtime()

            if (savedServerEpoch > 0L && savedElapsed > 0L && currentElapsed >= savedElapsed) {
                serverEpochAnchor = savedServerEpoch
                elapsedRealtimeAnchor = savedElapsed
                fallbackServerOffset = prefs.getLong(KEY_SERVER_OFFSET, 0L)
                isCalibrated = true
                Log.d(TAG, "Restored active server time calibration from prefs")
            } else {
                markDegradedToSystemTime()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init ServerTimeManager, falling back to system time", e)
            markDegradedToSystemTime()
        }
    }

    /**
     * Degrades strictly to system time. Converts to UTC+8 on demand.
     */
    fun markDegradedToSystemTime(context: Context? = null) {
        isCalibrated = false
        serverEpochAnchor = 0L
        elapsedRealtimeAnchor = 0L
        fallbackServerOffset = 0L
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (_: Exception) {}
        }
        Log.i(TAG, "Degraded to system time; will convert to UTC+8")
    }

    /**
     * Called when system time or timezone changes, immediately invalidating any old offset.
     */
    fun onSystemTimeChanged(context: Context? = null) {
        markDegradedToSystemTime(context)
    }

    /**
     * Calibrate the reference clock with server epoch milliseconds.
     */
    fun calibrate(serverEpochMs: Long, context: Context? = null) {
        if (serverEpochMs <= 0L) return
        val currentElapsed = SystemClock.elapsedRealtime()
        val currentSystemTime = System.currentTimeMillis()
        val offset = serverEpochMs - currentSystemTime

        serverEpochAnchor = serverEpochMs
        elapsedRealtimeAnchor = currentElapsed
        fallbackServerOffset = offset
        isCalibrated = true
        Log.d(TAG, "Server time calibrated: epoch=$serverEpochMs, elapsed=$currentElapsed, offset=$offset")

        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong(KEY_SERVER_EPOCH, serverEpochMs)
                    .putLong(KEY_ELAPSED_REALTIME, currentElapsed)
                    .putLong(KEY_SYSTEM_TIME, currentSystemTime)
                    .putLong(KEY_SERVER_OFFSET, offset)
                    .apply()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist server time anchor", e)
            }
        }
    }

    data class NtpQueryResult(
        val host: String,
        val epochMs: Long,
        val roundTripMs: Long
    )

    /**
     * Query a single NTP server via UDP port 123.
     */
    fun queryNtpServer(host: String, timeoutMs: Int = 3000): NtpQueryResult? {
        var socket: DatagramSocket? = null
        return try {
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(48)
            buffer[0] = 0x1B.toByte() // LI=0, VN=3, Mode=3 (client)

            val request = DatagramPacket(buffer, buffer.size, address, 123)
            socket = DatagramSocket().apply {
                soTimeout = timeoutMs
            }

            val t1 = System.currentTimeMillis()
            socket.send(request)

            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val t4 = System.currentTimeMillis()

            val transmitSeconds = read32(buffer, 40)
            val transmitFraction = read32(buffer, 44)
            if (transmitSeconds == 0L) return null

            val transmitMs = (transmitSeconds - 2208988800L) * 1000L + (transmitFraction * 1000L) / 0x100000000L
            val roundTrip = (t4 - t1).coerceAtLeast(0L)
            val ntpTime = transmitMs + roundTrip / 2

            Log.d(TAG, "NTP success from $host: time=$ntpTime, rtt=${roundTrip}ms")
            NtpQueryResult(host, ntpTime, roundTrip)
        } catch (e: Exception) {
            Log.d(TAG, "NTP query failed for $host: ${e.message}")
            null
        } finally {
            socket?.close()
        }
    }

    private fun read32(buffer: ByteArray, offset: Int): Long {
        val b0 = buffer[offset].toLong() and 0xFF
        val b1 = buffer[offset + 1].toLong() and 0xFF
        val b2 = buffer[offset + 2].toLong() and 0xFF
        val b3 = buffer[offset + 3].toLong() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    /**
     * Queries time servers. If offline or unreachable, cleanly degrades to system time in UTC+8.
     */
    suspend fun syncOptimalTime(context: Context? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (context != null && !isNetworkAvailable(context)) {
                    Log.d(TAG, "No network available for time sync, degrading to system time in UTC+8")
                    markDegradedToSystemTime(context)
                    return@withContext false
                }

                val results = withTimeoutOrNull(4000L) {
                    coroutineScope {
                        NTP_SERVERS.map { host ->
                            async { queryNtpServer(host, 3000) }
                        }.awaitAll().filterNotNull()
                    }
                }

                if (!results.isNullOrEmpty()) {
                    val best = results.minByOrNull { it.roundTripMs } ?: results.first()
                    Log.i(TAG, "Optimal NTP source selected: ${best.host} (RTT: ${best.roundTripMs}ms)")
                    calibrate(best.epochMs, context)
                    return@withContext true
                }

                // HTTP fallback if UDP port 123 is blocked
                for (url in HTTP_FALLBACK_URLS) {
                    try {
                        val req = Request.Builder()
                            .url(url)
                            .head()
                            .header("User-Agent", "Mozilla/5.0")
                            .build()
                        val resp = fallbackHttpClient.newCall(req).execute()
                        val dateHeader = resp.header("Date")
                        resp.close()
                        if (!dateHeader.isNullOrBlank()) {
                            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("GMT")
                            }
                            val date = sdf.parse(dateHeader)
                            if (date != null) {
                                Log.i(TAG, "Calibrated from HTTP fallback: $url")
                                calibrate(date.time, context)
                                return@withContext true
                            }
                        }
                    } catch (_: Exception) {}
                }

                // All servers unreachable: degrade to system time
                Log.w(TAG, "Time servers unreachable, degrading to system time in UTC+8")
                markDegradedToSystemTime(context)
                false
            } catch (e: Exception) {
                Log.w(TAG, "syncOptimalTime error, degrading to system time", e)
                markDegradedToSystemTime(context)
                false
            }
        }
    }

    fun isServerTimeCalibrated(): Boolean = isCalibrated

    fun nowEpochMillis(): Long {
        return if (isCalibrated && elapsedRealtimeAnchor > 0L) {
            val delta = SystemClock.elapsedRealtime() - elapsedRealtimeAnchor
            serverEpochAnchor + delta
        } else {
            System.currentTimeMillis()
        }
    }

    /**
     * Returns a Calendar instance strictly converted to China Standard Time (GMT+8 / CST).
     * When server time is unavailable or degraded, system time is converted to UTC+8.
     */
    fun getChinaCalendar(): Calendar {
        val cal = Calendar.getInstance(CHINA_TIME_ZONE)
        cal.timeInMillis = nowEpochMillis()
        return cal
    }

    fun resetForTesting() {
        serverEpochAnchor = 0L
        elapsedRealtimeAnchor = 0L
        fallbackServerOffset = 0L
        isCalibrated = false
    }
}
