package com.tao0524.tickat.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tao0524.tickat.data.local.AppDatabase
import com.tao0524.tickat.data.repository.TaskRepository
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.tao0524.tickat.ui.screen.settings.SettingsViewModel
import com.tao0524.tickat.ui.screen.settings.SettingsScreen
import com.tao0524.tickat.ui.theme.TickAtTheme

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = TaskRepository(
            AppDatabase.getInstance(applicationContext).taskDao()
        )
        val extras = MutableCreationExtras().apply {
            set(SettingsViewModel.CONTEXT_KEY,    applicationContext)
            set(SettingsViewModel.REPOSITORY_KEY, repository)
            set(SettingsViewModel.WIDGET_ID_KEY,  appWidgetId)
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        setContent {
            TickAtTheme {
                SettingsScreen(
                    viewModel = viewModel(
                        factory = SettingsViewModel.Factory,
                        extras  = extras
                    ),
                    onBack    = {
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}