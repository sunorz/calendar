package com.sunplace.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.data.repository.ThemePreferences
import com.sunplace.calendar.ui.getFullWeekdayResId
import com.sunplace.calendar.util.LunarCalculator
import com.sunplace.calendar.util.TranslationHelper
import java.util.Calendar

class TodayAppWidgetProvider : BaseCalendarWidgetProvider() {

    override val midnightAction: String = ACTION_MIDNIGHT_UPDATE

    override fun updateWidgetInstance(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = updateAppWidget(context, appWidgetManager, appWidgetId)

    override fun scheduleMidnight(context: Context) = scheduleMidnightUpdate(context)

    companion object {
        private const val TAG = "TodayWidget"
        const val ACTION_MIDNIGHT_UPDATE = "com.sunplace.calendar.ACTION_TODAY_MIDNIGHT_UPDATE"
        private const val MIDNIGHT_REQUEST_CODE = 1002

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val repository = CalendarRepository(context)
                val themeMode = resolveEffectiveThemeMode(context)
                val views = buildRemoteViews(context, themeMode, repository)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating today widget", e)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            themeMode: ThemeMode,
            repository: CalendarRepository
        ): RemoteViews {
            val config = repository.configState.value
            val nextConfig = repository.nextYearConfigState.value
            val showGuangxiSanyuesan = ThemePreferences(context).showGuangxiSanyuesan.value

            val cal = LunarCalculator.getChinaCalendar()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val dayInfo = LunarCalculator.getDayInfoForDate(year, month, day, config, nextConfig, showGuangxiSanyuesan)
            val views = RemoteViews(context.packageName, R.layout.widget_today)

            views.setInt(R.id.widget_root, "setBackgroundResource", getCardBackgroundRes(themeMode))

            val weekdayResId = getFullWeekdayResId(dayInfo.dayOfWeekNumber)
            val dayStr = dayInfo.day.toString()

            if (TranslationHelper.isEnglish()) {
                views.setViewVisibility(R.id.widget_layout_cn, View.GONE)
                views.setViewVisibility(R.id.widget_layout_en, View.VISIBLE)

                views.setTextViewText(R.id.widget_en_text_weekday, context.getString(weekdayResId))
                applyTextColor(views, R.id.widget_en_text_weekday, themeMode, 0xFFF28B82.toInt(), 0xFFD93025.toInt(), context)

                views.setTextViewText(R.id.widget_en_text_month, TranslationHelper.getMonthName(month))
                applyTextColor(views, R.id.widget_en_text_month, themeMode, 0xFFE8EAED.toInt(), 0xFF202124.toInt(), context)

                bindDayNumberView(
                    views = views,
                    normalViewId = R.id.widget_en_text_day_number,
                    redViewId = R.id.widget_en_text_day_number_red,
                    dayStr = dayStr,
                    isRestDay = dayInfo.isRestDay,
                    themeMode = themeMode,
                    context = context
                )
            } else {
                views.setViewVisibility(R.id.widget_layout_cn, View.VISIBLE)
                views.setViewVisibility(R.id.widget_layout_en, View.GONE)

                views.setTextViewText(R.id.widget_text_weekday, context.getString(weekdayResId))
                applyTextColor(views, R.id.widget_text_weekday, themeMode, 0xFFF28B82.toInt(), 0xFFD93025.toInt(), context)

                views.setTextViewText(R.id.widget_text_month, "${month}月")
                applyTextColor(views, R.id.widget_text_month, themeMode, 0xFF9AA0A6.toInt(), 0xFF70757A.toInt(), context)

                bindDayNumberView(
                    views = views,
                    normalViewId = R.id.widget_text_day_number,
                    redViewId = R.id.widget_text_day_number_red,
                    dayStr = dayStr,
                    isRestDay = dayInfo.isRestDay,
                    themeMode = themeMode,
                    context = context
                )

                val rawNoteText = dayInfo.widgetNote.ifBlank { " " }
                val verticalNoteText = rawNoteText.toCharArray().joinToString("\n")
                views.setTextViewText(R.id.widget_text_lunar_vertical, verticalNoteText)
                applyTextColor(views, R.id.widget_text_lunar_vertical, themeMode, 0xFFE8EAED.toInt(), 0xFF202124.toInt(), context)
            }

            bindLaunchMainActivityPendingIntent(context, views)
            return views
        }

        private fun bindDayNumberView(
            views: RemoteViews,
            normalViewId: Int,
            redViewId: Int,
            dayStr: String,
            isRestDay: Boolean,
            themeMode: ThemeMode,
            context: Context
        ) {
            if (isRestDay) {
                views.setViewVisibility(normalViewId, View.GONE)
                views.setViewVisibility(redViewId, View.VISIBLE)
                views.setTextViewText(redViewId, dayStr)
                applyTextColor(views, redViewId, themeMode, 0xFFF28B82.toInt(), 0xFFD93025.toInt(), context)
            } else {
                views.setViewVisibility(normalViewId, View.VISIBLE)
                views.setViewVisibility(redViewId, View.GONE)
                views.setTextViewText(normalViewId, dayStr)
                applyTextColor(views, normalViewId, themeMode, 0xFFE8EAED.toInt(), 0xFF202124.toInt(), context)
            }
        }

        fun updateAllWidgets(context: Context) {
            updateWidgetsForClass(context, TodayAppWidgetProvider::class.java, ::updateAppWidget)
        }

        fun scheduleMidnightUpdate(context: Context) {
            scheduleMidnightAlarm(
                context = context,
                providerClass = TodayAppWidgetProvider::class.java,
                action = ACTION_MIDNIGHT_UPDATE,
                requestCode = MIDNIGHT_REQUEST_CODE
            )
        }
    }
}
