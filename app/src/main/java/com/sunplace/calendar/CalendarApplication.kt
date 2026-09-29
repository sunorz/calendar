package com.sunplace.calendar

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.sunplace.calendar.data.repository.CalendarRepository
import com.sunplace.calendar.util.ServerTimeManager
import com.sunplace.calendar.widget.BaseCalendarWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val widgetDateSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctx = context ?: this@CalendarApplication
            val action = intent?.action
            Log.d("CalendarApp", "Received system broadcast in Application: $action, updating all widgets")
            BaseCalendarWidgetProvider.updateAllWidgets(ctx)
            appScope.launch(Dispatchers.IO) {
                CalendarRepository(ctx).syncServerTime()
                launch(Dispatchers.Main) {
                    BaseCalendarWidgetProvider.updateAllWidgets(ctx)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ServerTimeManager.init(this)
        BaseCalendarWidgetProvider.updateAllWidgets(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(widgetDateSyncReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(widgetDateSyncReceiver, filter)
        }

        appScope.launch(Dispatchers.IO) {
            val synced = CalendarRepository(this@CalendarApplication).syncServerTime()
            if (synced) {
                launch(Dispatchers.Main) {
                    BaseCalendarWidgetProvider.updateAllWidgets(this@CalendarApplication)
                }
            }
        }
    }
}
