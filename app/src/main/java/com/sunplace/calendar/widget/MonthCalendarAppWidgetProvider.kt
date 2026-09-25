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
import com.sunplace.calendar.util.LunarCalculator
import com.sunplace.calendar.util.TranslationHelper
import java.util.Calendar

class MonthCalendarAppWidgetProvider : BaseCalendarWidgetProvider() {

    override val midnightAction: String = ACTION_MIDNIGHT_UPDATE

    override fun updateWidgetInstance(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = updateAppWidget(context, appWidgetManager, appWidgetId)

    override fun scheduleMidnight(context: Context) = scheduleMidnightUpdate(context)

    companion object {
        private const val TAG = "MonthWidget"
        const val ACTION_MIDNIGHT_UPDATE = "com.sunplace.calendar.ACTION_MONTH_MIDNIGHT_UPDATE"
        private const val MIDNIGHT_REQUEST_CODE = 1003

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
                val themeMode = resolveEffectiveThemeMode(context)
                val views = buildRemoteViews(context, themeMode, repository)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating month widget", e)
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

            val daysList = LunarCalculator.getMonthDays(year, month, config, nextConfig, showGuangxiSanyuesan)
            val daysInMonth = daysList.size
            val firstDayOfWeek = if (daysList.isNotEmpty()) daysList[0].dayOfWeekNumber else 1
            val startOffset = firstDayOfWeek - 1

            val views = RemoteViews(context.packageName, R.layout.widget_month)

            views.setInt(R.id.widget_root, "setBackgroundResource", getCardBackgroundRes(themeMode))

            val monthTitle = TranslationHelper.formatYearMonth(year, month)
            views.setTextViewText(R.id.widget_month_title, monthTitle)
            applyTextColor(views, R.id.widget_month_title, themeMode, 0xFFE8EAED.toInt(), 0xFF202124.toInt(), context)

            val totalCellsNeeded = startOffset + daysInMonth
            views.setViewVisibility(
                R.id.widget_week_row_5,
                if (totalCellsNeeded <= 35) View.GONE else View.VISIBLE
            )

            for (cell in 0 until 42) {
                val viewId = DAY_VIEW_IDS[cell]
                if (cell < startOffset || cell >= startOffset + daysInMonth) {
                    views.setTextViewText(viewId, "")
                    views.setInt(viewId, "setBackgroundResource", R.drawable.bg_widget_transparent)
                } else {
                    val dayNumber = cell - startOffset + 1
                    val dayInfo = daysList[dayNumber - 1]
                    views.setTextViewText(viewId, dayNumber.toString())

                    val bgRes = if (dayInfo.isToday) {
                        when {
                            dayInfo.isExplicitLocaleHoliday -> when (themeMode) {
                                ThemeMode.DARK -> R.drawable.bg_widget_today_dark_locale
                                ThemeMode.LIGHT -> R.drawable.bg_widget_today_light_locale
                                ThemeMode.SYSTEM -> R.drawable.bg_widget_today_locale
                            }
                            dayInfo.isRestDay -> when (themeMode) {
                                ThemeMode.DARK -> R.drawable.bg_widget_today_dark_restday
                                ThemeMode.LIGHT -> R.drawable.bg_widget_today_light_restday
                                ThemeMode.SYSTEM -> R.drawable.bg_widget_today_red
                            }
                            else -> when (themeMode) {
                                ThemeMode.DARK -> R.drawable.bg_widget_today_dark_workday
                                ThemeMode.LIGHT -> R.drawable.bg_widget_today_light_workday
                                ThemeMode.SYSTEM -> R.drawable.bg_widget_today
                            }
                        }
                    } else {
                        R.drawable.bg_widget_transparent
                    }
                    views.setInt(viewId, "setBackgroundResource", bgRes)

                    val (darkColor, lightColor) = when {
                        dayInfo.isExplicitLocaleHoliday -> 0xFF81C995.toInt() to 0xFF188038.toInt()
                        dayInfo.isRestDay -> 0xFFF28B82.toInt() to 0xFFD93025.toInt()
                        else -> 0xFFE8EAED.toInt() to 0xFF202124.toInt()
                    }
                    applyTextColor(views, viewId, themeMode, darkColor, lightColor, context)
                }
            }

            bindLaunchMainActivityPendingIntent(context, views)
            return views
        }

        fun updateAllWidgets(context: Context) {
            updateWidgetsForClass(context, MonthCalendarAppWidgetProvider::class.java, ::updateAppWidget)
        }

        fun scheduleMidnightUpdate(context: Context) {
            scheduleMidnightAlarm(
                context = context,
                providerClass = MonthCalendarAppWidgetProvider::class.java,
                action = ACTION_MIDNIGHT_UPDATE,
                requestCode = MIDNIGHT_REQUEST_CODE
            )
        }
    }
}
