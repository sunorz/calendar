package com.sunplace.calendar.util

import android.content.Context
import android.os.SystemClock
import android.util.Log
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ServerTimeManager {
    private const val TAG = "ServerTimeManager"
    private const val PREFS_NAME = "server_time_prefs"
    private const val KEY_SERVER_EPOCH = "server_epoch_ms"
    private const val KEY_ELAPSED_REALTIME = "elapsed_realtime_ms"
    private const val KEY_SYSTEM_TIME = "system_time_ms"

    val CHINA_TIME_ZONE: TimeZone = TimeZone.getTimeZone("GMT+8")

    @Volatile
    private var serverEpochAnchor: Long = 0L

    @Volatile
    private var elapsedRealtimeAnchor: Long = 0L

    @Volatile
    private var isCalibrated: Boolean = false

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedServerEpoch = prefs.getLong(KEY_SERVER_EPOCH, 0L)
            val savedElapsed = prefs.getLong(KEY_ELAPSED_REALTIME, 0L)
            val currentElapsed = SystemClock.elapsedRealtime()

            // If saved within current boot cycle (current elapsed >= saved elapsed)
            if (savedServerEpoch > 0L && savedElapsed > 0L && currentElapsed >= savedElapsed) {
                serverEpochAnchor = savedServerEpoch
                elapsedRealtimeAnchor = savedElapsed
                isCalibrated = true
                Log.d(TAG, "Restored server time calibration from prefs")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init ServerTimeManager from prefs", e)
        }
    }

    /**
     * Calibrate time using HTTP Date header from OkHttp response.
     */
    fun calibrateFromResponse(response: Response, context: Context? = null) {
        try {
            var serverDate: Date? = response.headers.getDate("Date")
            if (serverDate == null) {
                val dateStr = response.header("Date")
                if (!dateStr.isNullOrBlank()) {
                    val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("GMT")
                    }
                    serverDate = sdf.parse(dateStr)
                }
            }

            if (serverDate != null) {
                calibrate(serverDate.time, context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse server Date header", e)
        }
    }

    /**
     * Calibrate the reference clock with server epoch milliseconds.
     */
    fun calibrate(serverEpochMs: Long, context: Context? = null) {
        if (serverEpochMs <= 0L) return
        val currentElapsed = SystemClock.elapsedRealtime()
        serverEpochAnchor = serverEpochMs
        elapsedRealtimeAnchor = currentElapsed
        isCalibrated = true
        Log.d(TAG, "Server time calibrated: epoch=$serverEpochMs, elapsed=$currentElapsed")

        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong(KEY_SERVER_EPOCH, serverEpochMs)
                    .putLong(KEY_ELAPSED_REALTIME, currentElapsed)
                    .putLong(KEY_SYSTEM_TIME, System.currentTimeMillis())
                    .apply()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist server time anchor", e)
            }
        }
    }

    /**
     * Check whether server time calibration is active.
     */
    fun isServerTimeCalibrated(): Boolean = isCalibrated

    /**
     * Current accurate epoch millis based on server anchor + elapsed monotonic time.
     * Immunized against manual device clock tampering.
     */
    fun nowEpochMillis(): Long {
        return if (isCalibrated && elapsedRealtimeAnchor > 0L) {
            val delta = SystemClock.elapsedRealtime() - elapsedRealtimeAnchor
            serverEpochAnchor + delta
        } else {
            System.currentTimeMillis()
        }
    }

    /**
     * Returns a Calendar instance set to China Standard Time (GMT+8 / CST) based on server time.
     */
    fun getChinaCalendar(): Calendar {
        val cal = Calendar.getInstance(CHINA_TIME_ZONE)
        cal.timeInMillis = nowEpochMillis()
        return cal
    }

    /**
     * Clear calibration (used mainly for tests).
     */
    fun resetForTesting() {
        serverEpochAnchor = 0L
        elapsedRealtimeAnchor = 0L
        isCalibrated = false
    }
}
