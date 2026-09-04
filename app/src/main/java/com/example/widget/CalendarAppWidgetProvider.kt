package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.CalendarRepository
import com.example.util.LunarCalculator
import java.util.Calendar

class CalendarAppWidgetProvider : AppWidgetProvider() {

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
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == ACTION_MIDNIGHT_UPDATE
        ) {
            updateAllWidgets(context)
            scheduleMidnightUpdate(context)
        }
    }

    companion object {
        private const val TAG = "CalendarWidget"
        const val ACTION_MIDNIGHT_UPDATE = "com.sunplace.calendar.ACTION_MIDNIGHT_UPDATE"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val repository = CalendarRepository(context)
                val config = repository.configState.value

                val cal = LunarCalculator.getChinaCalendar()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)

                val dayInfo = LunarCalculator.getDayInfoForDate(year, month, day, config)

                val views = RemoteViews(context.packageName, R.layout.widget_calendar)
                val weekdayResId = com.example.ui.getFullWeekdayResId(dayInfo.dayOfWeekNumber)

                if (com.example.util.TranslationHelper.isEnglish()) {
                    views.setViewVisibility(R.id.widget_layout_cn, View.GONE)
                    views.setViewVisibility(R.id.widget_layout_en, View.VISIBLE)

                    // Line 1: Weekday centered in RED
                    views.setTextViewText(R.id.widget_en_text_weekday, context.getString(weekdayResId))
                    views.setTextColor(
                        R.id.widget_en_text_weekday,
                        ContextCompat.getColor(context, R.color.widget_text_red)
                    )

                    // Line 2: Month centered (e.g. "September")
                    val monthText = com.example.util.TranslationHelper.getMonthName(month)
                    views.setTextViewText(R.id.widget_en_text_month, monthText)

                    // Line 3: Day centered (turns red if rest day)
                    views.setTextViewText(R.id.widget_en_text_day_number, dayInfo.day.toString())
                    val dayColor = if (dayInfo.isRestDay) {
                        ContextCompat.getColor(context, R.color.widget_text_red)
                    } else {
                        ContextCompat.getColor(context, R.color.widget_text_primary)
                    }
                    views.setTextColor(R.id.widget_en_text_day_number, dayColor)
                } else {
                    views.setViewVisibility(R.id.widget_layout_cn, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_layout_en, View.GONE)

                    // Top Left: Weekday in RED (e.g. "周二")
                    views.setTextViewText(R.id.widget_text_weekday, context.getString(weekdayResId))
                    views.setTextColor(
                        R.id.widget_text_weekday,
                        ContextCompat.getColor(context, R.color.widget_text_red)
                    )

                    // Top Right: Month in GRAY (e.g. "9月")
                    views.setTextViewText(R.id.widget_text_month, "${month}月")
                    views.setTextColor(
                        R.id.widget_text_month,
                        ContextCompat.getColor(context, R.color.widget_text_secondary)
                    )

                    // Date Number: turns red if rest day, else primary color
                    views.setTextViewText(R.id.widget_text_day_number, dayInfo.day.toString())
                    val cnDayColor = if (dayInfo.isRestDay) {
                        ContextCompat.getColor(context, R.color.widget_text_red)
                    } else {
                        ContextCompat.getColor(context, R.color.widget_text_primary)
                    }
                    views.setTextColor(R.id.widget_text_day_number, cnDayColor)

                    // Vertical Text on Right (Lunar / Festival)
                    val rawNoteText = dayInfo.widgetNote.ifBlank { " " }
                    val verticalNoteText = rawNoteText.toCharArray().joinToString("\n")
                    views.setTextViewText(R.id.widget_text_lunar_vertical, verticalNoteText)
                    views.setTextColor(
                        R.id.widget_text_lunar_vertical,
                        ContextCompat.getColor(context, R.color.widget_text_primary)
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

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app widget: ${e.message}", e)
            }
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, CalendarAppWidgetProvider::class.java)
                )
                if (ids != null && ids.isNotEmpty()) {
                    for (id in ids) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating all widgets: ${e.message}", e)
            }
        }

        fun scheduleMidnightUpdate(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, CalendarAppWidgetProvider::class.java).apply {
                    action = ACTION_MIDNIGHT_UPDATE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Next midnight: 00:00:02 tomorrow
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 2)
                    set(Calendar.MILLISECOND, 0)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, cal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC, cal.timeInMillis, pendingIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling midnight update: ${e.message}", e)
            }
        }
    }
}
