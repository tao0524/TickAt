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
        try {
            context.startForegroundService(
                Intent(context, WidgetUpdateService::class.java)
            )
        } catch (e: Exception) {
            // HyperOS: バックグラウンドからの起動を無視（onAppWidgetOptionsChangedで再起動される）
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            context.startForegroundService(
                Intent(context, WidgetUpdateService::class.java)
            )
        } catch (e: Exception) {
            // HyperOS: バックグラウンドからの起動を無視
        }
    }fun loadWidgetIds(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_IDS, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(
            Intent(context, WidgetUpdateService::class.java)
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        saveWidgetIds(context, listOf(appWidgetId))
        context.startForegroundService(
            Intent(context, WidgetUpdateService::class.java).apply {
                putExtra(WidgetUpdateService.EXTRA_WIDGET_IDS, intArrayOf(appWidgetId))
            }
        )
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        removeWidgetIds(context, appWidgetIds.toList())
        val host = android.appwidget.AppWidgetHost(context, 1)
        for (id in appWidgetIds) {
            host.deleteAppWidgetId(id)
        }
    }

    companion object {
        private const val PREFS_NAME = "tickat_widget_ids"
        private const val KEY_IDS = "active_widget_ids"

        fun updateAllWidgets(context: Context) {
            context.startForegroundService(
                Intent(context, WidgetUpdateService::class.java)
            )
        }

        fun saveWidgetIds(context: Context, newIds: List<Int>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = loadWidgetIds(context).toMutableSet()
            existing.addAll(newIds)
            prefs.edit().putString(KEY_IDS, existing.joinToString(",")).apply()
        }

        fun removeWidgetIds(context: Context, removedIds: List<Int>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = loadWidgetIds(context).toMutableSet()
            existing.removeAll(removedIds.toSet())
            prefs.edit().putString(KEY_IDS, existing.joinToString(",")).apply()
        }

        fun loadWidgetIds(context: Context): List<Int> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_IDS, "") ?: ""
            if (raw.isEmpty()) return emptyList()
            val savedIds = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            val manager = AppWidgetManager.getInstance(context)
            val systemIds = manager.getAppWidgetIds(
                ComponentName(context, TickAtWidgetReceiver::class.java)
            ).toSet()
            val validIds = savedIds.filter { it in systemIds }
            if (validIds.size != savedIds.size) {
                prefs.edit().putString(KEY_IDS, validIds.joinToString(",")).apply()
            }
            return validIds
        }
    }
}