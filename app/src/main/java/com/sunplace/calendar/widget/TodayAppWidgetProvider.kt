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

class TodayAppWidgetProvider : AppWidgetProvider() {

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
        private const val TAG = "TodayWidget"
        const val ACTION_MIDNIGHT_UPDATE = "com.sunplace.calendar.ACTION_TODAY_MIDNIGHT_UPDATE"

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
                Log.e(TAG, "Error updating today widget", e)
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

            val cal = LunarCalculator.getChinaCalendar()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val dayInfo = LunarCalculator.getDayInfoForDate(year, month, day, config)

            val views = RemoteViews(context.packageName, R.layout.widget_today)

            // 0. Ensure Card Background matches theme mode
            val bgCardRes = when (themeMode) {
                ThemeMode.DARK -> R.drawable.bg_widget_card_dark
                ThemeMode.LIGHT -> R.drawable.bg_widget_card_light
                ThemeMode.SYSTEM -> R.drawable.bg_widget_card
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", bgCardRes)

            val weekdayResId = com.sunplace.calendar.ui.getFullWeekdayResId(dayInfo.dayOfWeekNumber)

            if (TranslationHelper.isEnglish()) {
                views.setViewVisibility(R.id.widget_layout_cn, View.GONE)
                views.setViewVisibility(R.id.widget_layout_en, View.VISIBLE)

                // Line 1: Weekday centered - ALWAYS RED
                views.setTextViewText(R.id.widget_en_text_weekday, context.getString(weekdayResId))
                applyTextColor(
                    views = views,
                    viewId = R.id.widget_en_text_weekday,
                    themeMode = themeMode,
                    colorDarkInt = 0xFFF28B82.toInt(),
                    colorLightInt = 0xFFD93025.toInt(),
                    context = context
                )

                // Line 2: Month centered
                val monthText = TranslationHelper.getMonthName(month)
                views.setTextViewText(R.id.widget_en_text_month, monthText)
                applyTextColor(
                    views = views,
                    viewId = R.id.widget_en_text_month,
                    themeMode = themeMode,
                    colorDarkInt = 0xFFE8EAED.toInt(),
                    colorLightInt = 0xFF202124.toInt(),
                    context = context
                )

                // Line 3: Day centered
                val dayStr = dayInfo.day.toString()
                if (dayInfo.isRestDay) {
                    views.setViewVisibility(R.id.widget_en_text_day_number, View.GONE)
                    views.setViewVisibility(R.id.widget_en_text_day_number_red, View.VISIBLE)
                    views.setTextViewText(R.id.widget_en_text_day_number_red, dayStr)
                    applyTextColor(
                        views = views,
                        viewId = R.id.widget_en_text_day_number_red,
                        themeMode = themeMode,
                        colorDarkInt = 0xFFF28B82.toInt(),
                        colorLightInt = 0xFFD93025.toInt(),
                        context = context
                    )
                } else {
                    views.setViewVisibility(R.id.widget_en_text_day_number, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_en_text_day_number_red, View.GONE)
                    views.setTextViewText(R.id.widget_en_text_day_number, dayStr)
                    applyTextColor(
                        views = views,
                        viewId = R.id.widget_en_text_day_number,
                        themeMode = themeMode,
                        colorDarkInt = 0xFFE8EAED.toInt(),
                        colorLightInt = 0xFF202124.toInt(),
                        context = context
                    )
                }
            } else {
                views.setViewVisibility(R.id.widget_layout_cn, View.VISIBLE)
                views.setViewVisibility(R.id.widget_layout_en, View.GONE)

                // Top Left: Weekday (e.g. "周二") - ALWAYS RED
                views.setTextViewText(R.id.widget_text_weekday, context.getString(weekdayResId))
                applyTextColor(
                    views = views,
                    viewId = R.id.widget_text_weekday,
                    themeMode = themeMode,
                    colorDarkInt = 0xFFF28B82.toInt(),
                    colorLightInt = 0xFFD93025.toInt(),
                    context = context
                )

                // Top Right: Month (e.g. "9月")
                views.setTextViewText(R.id.widget_text_month, "${month}月")
                applyTextColor(
                    views = views,
                    viewId = R.id.widget_text_month,
                    themeMode = themeMode,
                    colorDarkInt = 0xFF9AA0A6.toInt(),
                    colorLightInt = 0xFF70757A.toInt(),
                    context = context
                )

                // Date Number: turns red if rest day, else primary color
                val dayStr = dayInfo.day.toString()
                if (dayInfo.isRestDay) {
                    views.setViewVisibility(R.id.widget_text_day_number, View.GONE)
                    views.setViewVisibility(R.id.widget_text_day_number_red, View.VISIBLE)
                    views.setTextViewText(R.id.widget_text_day_number_red, dayStr)
                    applyTextColor(
                        views = views,
                        viewId = R.id.widget_text_day_number_red,
                        themeMode = themeMode,
                        colorDarkInt = 0xFFF28B82.toInt(),
                        colorLightInt = 0xFFD93025.toInt(),
                        context = context
                    )
                } else {
                    views.setViewVisibility(R.id.widget_text_day_number, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_text_day_number_red, View.GONE)
                    views.setTextViewText(R.id.widget_text_day_number, dayStr)
                    applyTextColor(
                        views = views,
                        viewId = R.id.widget_text_day_number,
                        themeMode = themeMode,
                        colorDarkInt = 0xFFE8EAED.toInt(),
                        colorLightInt = 0xFF202124.toInt(),
                        context = context
                    )
                }

                // Vertical Text on Right (Lunar / Festival)
                val rawNoteText = dayInfo.widgetNote.ifBlank { " " }
                val verticalNoteText = rawNoteText.toCharArray().joinToString("\n")
                views.setTextViewText(R.id.widget_text_lunar_vertical, verticalNoteText)
                applyTextColor(
                    views = views,
                    viewId = R.id.widget_text_lunar_vertical,
                    themeMode = themeMode,
                    colorDarkInt = 0xFFE8EAED.toInt(),
                    colorLightInt = 0xFF202124.toInt(),
                    context = context
                )
            }

            // Click Intent -> Open MainActivity
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
            val thisWidget = ComponentName(context, TodayAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun scheduleMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TodayAppWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
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
