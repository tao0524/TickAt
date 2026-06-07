package com.tao0524.tickat.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TickAtWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        context.startForegroundService(
            Intent(context, WidgetUpdateService::class.java)
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startForegroundService(
            Intent(context, WidgetUpdateService::class.java)
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(
            Intent(context, WidgetUpdateService::class.java)
        )
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            context.startForegroundService(
                Intent(context, WidgetUpdateService::class.java)
            )
        }
    }
}