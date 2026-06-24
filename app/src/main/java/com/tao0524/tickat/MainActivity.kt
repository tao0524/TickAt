package com.tao0524.tickat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.tao0524.tickat.data.local.AppDatabase
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.ui.navigation.AppNavigation
import com.tao0524.tickat.ui.theme.TickAtTheme
import com.tao0524.tickat.widget.TaskAlertScheduler
import com.tao0524.tickat.ui.screen.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "tickat_settings")
private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

class MainActivity : ComponentActivity() {

    private val _showExpanded      = MutableStateFlow(false)
    private val _expandedTaskId    = MutableStateFlow<String?>(null)
    private val _onboardingChecked = MutableStateFlow<Boolean?>(null)

    private val settingsViewModel: SettingsViewModel by lazy {
        SettingsViewModel(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 33+ の通知パーミッション要求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }

        if (intent?.getBooleanExtra("show_expanded", false) == true) {
            _showExpanded.value = true
            _expandedTaskId.value = intent?.getStringExtra("expanded_task_id")
        }

        lifecycleScope.launch {
            val complete = dataStore.data.first()[ONBOARDING_COMPLETE] ?: false
            _onboardingChecked.value = complete
        }

        startForegroundService(
            Intent(this, com.tao0524.tickat.widget.WidgetUpdateService::class.java)
        )

        val db         = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepository(db.taskDao())

        // アプリ起動時に全タスクのアラームを再スケジュール
        lifecycleScope.launch {
            val tasks = repository.allTasks.first()
            TaskAlertScheduler.scheduleAll(applicationContext, tasks)
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            TickAtTheme(settings = settings) {
                val onboardingChecked by _onboardingChecked.collectAsState()
                val showExpanded      by _showExpanded.collectAsState()
                val expandedTaskId    by _expandedTaskId.collectAsState()

                when (val complete = onboardingChecked) {
                    null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1C1B1F))
                    )
                    else -> AppNavigation(
                        repository          = repository,
                        startWithOnboarding = !complete,
                        onOnboardingComplete = {
                            lifecycleScope.launch {
                                dataStore.edit { it[ONBOARDING_COMPLETE] = true }
                            }
                        },
                        openExpandedOnStart = showExpanded,
                        expandedTaskId      = expandedTaskId,
                        onExpandedConsumed  = {
                            _showExpanded.value = false
                            _expandedTaskId.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("show_expanded", false)) {
            _showExpanded.value = true
            _expandedTaskId.value = intent.getStringExtra("expanded_task_id")
        }
    }
}