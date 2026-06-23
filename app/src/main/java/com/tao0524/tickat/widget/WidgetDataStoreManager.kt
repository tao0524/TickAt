package com.tao0524.tickat.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object WidgetDataStoreManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val stores = mutableMapOf<Int, DataStore<Preferences>>()

    fun getStore(context: Context, appWidgetId: Int): DataStore<Preferences> {
        return stores.getOrPut(appWidgetId) {
            PreferenceDataStoreFactory.create(
                scope = scope
            ) {
                context.applicationContext.preferencesDataStoreFile(
                    "tickat_widget_settings_$appWidgetId"
                )
            }
        }
    }
}