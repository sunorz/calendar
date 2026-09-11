package com.sunplace.calendar.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.sunplace.calendar.MainActivity
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.data.repository.ThemePreferences
import com.sunplace.calendar.util.LunarCalculator
import com.sunplace.calendar.util.TranslationHelper
import java.util.Calendar

class MonthCalendarAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleMidnightUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
        scheduleMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_CONFIGURATION_CHANGED ||
            action == ACTION_MIDNIGHT_UPDATE
        ) {
            updateAllWidgets(context)
            scheduleMidnightUpdate(context)
        }
    }

    companion object {
        private const val TAG = "MonthWidget"
        const val ACTION_MIDNIGHT_UPDATE = "com.sunplace.calendar.ACTION_MONTH_MIDNIGHT_UPDATE"

        private val DAY_VIEW_IDS = intArrayOf(
            R.id.widget_day_0, R.id.widget_day_1, R.id.widget_day_2, R.id.widget_day_3, R.id.widget_day_4, R.id.widget_day_5, R.id.widget_day_6,
            R.id.widget_day_7, R.id.widget_day_8, R.id.widget_day_9, R.id.widget_day_10, R.id.widget_day_11, R.id.widget_day_12, R.id.widget_day_13,
            R.id.widget_day_14, R.id.widget_day_15, R.id.widget_day_16, R.id.widget_day_17, R.id.widget_day_18, R.id.widget_day_19, R.id.widget_day_20,
            R.id.widget_day_21, R.id.widget_day_22, R.id.widget_day_23, R.id.widget_day_24, R.id.widget_day_25, R.id.widget_day_26, R.id.widget_day_27,
            R.id.widget_day_28, R.id.widget_day_29, R.id.widget_day_30, R.id.widget_day_31, R.id.widget_day_32, R.id.widget_day_33, R.id.widget_day_34,
            R.id.widget_day_35, R.id.widget_day_36, R.id.widget_day_37, R.id.widget_day_38, R.id.widget_day_39, R.id.widget_day_40, R.id.widget_day_41
        )

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val repository = CalendarRepository(context)
                val themePrefs = ThemePreferences(context)
                val isElderlyMode = themePrefs.isElderlyMode.value
                val themeMode = if (isElderlyMode) ThemeMode.LIGHT else themePrefs.themeMode.value
                val isSystemNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val isNight = when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemNight
                }

                val views = buildRemoteViews(context, themeMode, isNight, repository)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating month widget", e)
            }
        }

        private fun applyTextColor(
            views: RemoteViews,
            viewId: Int,
            themeMode: ThemeMode,
            colorDarkInt: Int,
            colorLightInt: Int,
            context: Context
        ) {
            when (themeMode) {
                ThemeMode.SYSTEM -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        views.setColorInt(viewId, "setTextColor", colorLightInt, colorDarkInt)
                    } else {
                        val isSystemNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        views.setTextColor(viewId, if (isSystemNight) colorDarkInt else colorLightInt)
                    }
                }
                ThemeMode.DARK -> {
                    views.setTextColor(viewId, colorDarkInt)
                }
                ThemeMode.LIGHT -> {
                    views.setTextColor(viewId, colorLightInt)
                }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            themeMode: ThemeMode,
            isNight: Boolean,
            repository: CalendarRepository
        ): RemoteViews {
            val config = repository.configState.value
            val nextConfig = repository.nextYearConfigState.value

            val cal = LunarCalculator.getChinaCalendar()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1

            val daysList = LunarCalculator.getMonthDays(year, month, config, nextConfig)
            val daysInMonth = daysList.size
            val firstDayOfWeek = if (daysList.isNotEmpty()) daysList[0].dayOfWeekNumber else 1
            val startOffset = firstDayOfWeek - 1

            val views = RemoteViews(context.packageName, R.layout.widget_month)

            // 0. Ensure Card Background matches theme mode
            val bgCardRes = when (themeMode) {
                ThemeMode.DARK -> R.drawable.bg_widget_card_dark
                ThemeMode.LIGHT -> R.drawable.bg_widget_card_light
                ThemeMode.SYSTEM -> R.drawable.bg_widget_card
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", bgCardRes)

            // 1. Year & Month Title
            val monthTitle = if (TranslationHelper.isEnglish()) {
                "${TranslationHelper.getMonthName(month)} $year"
            } else {
                "${year}年${month}月"
            }
            views.setTextViewText(R.id.widget_month_title, monthTitle)
            applyTextColor(
                views = views,
                viewId = R.id.widget_month_title,
                themeMode = themeMode,
                colorDarkInt = 0xFFE8EAED.toInt(),
                colorLightInt = 0xFF202124.toInt(),
                context = context
            )

            // 2. Days Grid
            val totalCellsNeeded = startOffset + daysInMonth
            if (totalCellsNeeded <= 35) {
                views.setViewVisibility(R.id.widget_week_row_5, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_week_row_5, View.VISIBLE)
            }

            // Populate 42 cells
            for (cell in 0 until 42) {
                val viewId = DAY_VIEW_IDS[cell]
                if (cell < startOffset || cell >= startOffset + daysInMonth) {
                    views.setTextViewText(viewId, "")
                    views.setInt(viewId, "setBackgroundResource", R.drawable.bg_widget_transparent)
                } else {
                    val dayNumber = cell - startOffset + 1
                    val dayInfo = daysList[dayNumber - 1]
                    views.setTextViewText(viewId, dayNumber.toString())

                    // Highlight Today
                    if (dayInfo.isToday) {
                        when {
                            dayInfo.isLocaleHoliday -> {
                                val bgRes = when (themeMode) {
                                    ThemeMode.DARK -> R.drawable.bg_widget_today_dark_locale
                                    ThemeMode.LIGHT -> R.drawable.bg_widget_today_light_locale
                                    ThemeMode.SYSTEM -> R.drawable.bg_widget_today_locale
                                }
                                views.setInt(viewId, "setBackgroundResource", bgRes)
                                applyTextColor(
                                    views = views,
                                    viewId = viewId,
                                    themeMode = themeMode,
                                    colorDarkInt = 0xFF81C995.toInt(),
                                    colorLightInt = 0xFF188038.toInt(),
                                    context = context
                                )
                            }
                            dayInfo.isRestDay -> {
                                val bgRes = when (themeMode) {
                                    ThemeMode.DARK -> R.drawable.bg_widget_today_dark_restday
                                    ThemeMode.LIGHT -> R.drawable.bg_widget_today_light_restday
                                    ThemeMode.SYSTEM -> R.drawable.bg_widget_today_red
                                }
                                views.setInt(viewId, "setBackgroundResource", bgRes)
                                applyTextColor(
                                    views = views,
                                    viewId = viewId,
                                    themeMode = themeMode,
                                    colorDarkInt = 0xFFF28B82.toInt(),
                                    colorLightInt = 0xFFD93025.toInt(),
                                    context = context
                                )
                            }
                            else -> {
                                val bgRes = when (themeMode) {
                                    ThemeMode.DARK -> R.drawable.bg_widget_today_dark_workday
                                    ThemeMode.LIGHT -> R.drawable.bg_widget_today_light_workday
                                    ThemeMode.SYSTEM -> R.drawable.bg_widget_today
                                }
                                views.setInt(viewId, "setBackgroundResource", bgRes)
                                applyTextColor(
                                    views = views,
                                    viewId = viewId,
                                    themeMode = themeMode,
                                    colorDarkInt = 0xFFE8EAED.toInt(),
                                    colorLightInt = 0xFF202124.toInt(),
                                    context = context
                                )
                            }
                        }
                    } else {
                        views.setInt(viewId, "setBackgroundResource", R.drawable.bg_widget_transparent)
                        when {
                            dayInfo.isLocaleHoliday -> {
                                applyTextColor(
                                    views = views,
                                    viewId = viewId,
                                    themeMode = themeMode,
                                    colorDarkInt = 0xFF81C995.toInt(),
                                    colorLightInt = 0xFF188038.toInt(),
                                    context = context
                                )
                            }
                            dayInfo.isRestDay -> {
                                applyTextColor(
                                    views = views,
                                    viewId = viewId,
                                    themeMode = themeMode,
                                    colorDarkInt = 0xFFF28B82.toInt(),
                                    colorLightInt = 0xFFD93025.toInt(),
                                    context = context
                                )
                            }
                            else -> {
                                applyTextColor(
                                    views = views,
                                    viewId = viewId,
                                    themeMode = themeMode,
                                    colorDarkInt = 0xFFE8EAED.toInt(),
                                    colorLightInt = 0xFF202124.toInt(),
                                    context = context
                                )
                            }
                        }
                    }
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            return views
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MonthCalendarAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun scheduleMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, MonthCalendarAppWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1003,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cal = LunarCalculator.getChinaCalendar().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}
