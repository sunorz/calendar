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
import android.widget.RemoteViews
import com.sunplace.calendar.MainActivity
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.repository.ThemePreferences
import com.sunplace.calendar.util.LunarCalculator
import java.util.Calendar

abstract class BaseCalendarWidgetProvider : AppWidgetProvider() {

    protected abstract val midnightAction: String

    protected abstract fun updateWidgetInstance(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

    protected abstract fun scheduleMidnight(context: Context)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetInstance(context, appWidgetManager, appWidgetId)
        }
        scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgetInstance(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateWidgetsForClass(context, this::class.java, ::updateWidgetInstance)
        scheduleMidnight(context)
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
            action == Intent.ACTION_SCREEN_ON ||
            action == Intent.ACTION_CONFIGURATION_CHANGED ||
            action == midnightAction
        ) {
            updateWidgetsForClass(context, this::class.java, ::updateWidgetInstance)
            scheduleMidnight(context)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            TodayAppWidgetProvider.updateAllWidgets(context)
            MonthCalendarAppWidgetProvider.updateAllWidgets(context)
            TodayAppWidgetProvider.scheduleMidnightUpdate(context)
            MonthCalendarAppWidgetProvider.scheduleMidnightUpdate(context)
        }

        fun resolveEffectiveThemeMode(context: Context): ThemeMode {
            val themePrefs = ThemePreferences(context)
            return if (themePrefs.isElderlyMode.value) ThemeMode.LIGHT else themePrefs.themeMode.value
        }

        fun getCardBackgroundRes(themeMode: ThemeMode): Int {
            return when (themeMode) {
                ThemeMode.DARK -> R.drawable.bg_widget_card_dark
                ThemeMode.LIGHT -> R.drawable.bg_widget_card_light
                ThemeMode.SYSTEM -> R.drawable.bg_widget_card
            }
        }

        fun applyTextColor(
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
                ThemeMode.DARK -> views.setTextColor(viewId, colorDarkInt)
                ThemeMode.LIGHT -> views.setTextColor(viewId, colorLightInt)
            }
        }

        fun bindLaunchMainActivityPendingIntent(context: Context, views: RemoteViews) {
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
        }

        fun <T : AppWidgetProvider> updateWidgetsForClass(
            context: Context,
            providerClass: Class<T>,
            updater: (Context, AppWidgetManager, Int) -> Unit
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, providerClass)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updater(context, appWidgetManager, appWidgetId)
            }
        }

        fun <T : AppWidgetProvider> scheduleMidnightAlarm(
            context: Context,
            providerClass: Class<T>,
            action: String,
            requestCode: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, providerClass).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = System.currentTimeMillis()
            val localMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }
            val chinaMidnight = LunarCalculator.getChinaCalendar().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }

            val triggerTime = listOf(localMidnight.timeInMillis, chinaMidnight.timeInMillis)
                .filter { it > now }
                .minOrNull() ?: (now + 60 * 60 * 1000L)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        }
    }
}
