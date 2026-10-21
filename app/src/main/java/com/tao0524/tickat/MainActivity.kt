package com.tao0524.tickat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.tao0524.tickat.data.local.AppDatabase
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.ui.navigation.AppNavigation
import com.tao0524.tickat.ui.theme.TickAtTheme
import com.tao0524.tickat.widget.TaskAlertScheduler
import com.tao0524.tickat.widget.TaskAlertService
import com.tao0524.tickat.ui.screen.settings.SettingsViewModel
import com.tao0524.tickat.ui.component.FullScreenPermissionDialog
import com.tao0524.tickat.ui.component.FULL_SCREEN_PROMPT_TITLE
import com.tao0524.tickat.ui.component.FULL_SCREEN_PROMPT_MESSAGE
import com.tao0524.tickat.ui.component.needsFullScreenPermission
import com.tao0524.tickat.ui.component.openFullScreenPermissionSettings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "tickat_settings")
private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
// 全画面通知の許可のお願い（ポップアップ）を出したかどうか
private val FULL_SCREEN_PROMPT_SHOWN = booleanPreferencesKey("full_screen_prompt_shown")

class MainActivity : ComponentActivity() {

    private var isAppReady = false

    private val settingsViewModel: SettingsViewModel by lazy {
        SettingsViewModel(applicationContext, appWidgetId = 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        // 通知を押して開いたときは、通知音を止める（画面の作り直しのときは何もしない）
        if (savedInstanceState == null) {
            TaskAlertService.stopFromNotificationTap(this, intent)
        }

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
            val themeMode by settingsViewModel.themeMode.collectAsState()

            var onboardingState by remember { mutableStateOf<Boolean?>(null) }
            var fullScreenPromptShown by remember { mutableStateOf(true) }
            var showFullScreenPrompt by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val prefs = dataStore.data.first()
                onboardingState = prefs[ONBOARDING_COMPLETE] ?: false
                fullScreenPromptShown = prefs[FULL_SCREEN_PROMPT_SHOWN] ?: false
                // アップデートした人（案内は終わっている）には、最初に開いたときに1回出す
                if (onboardingState == true && !fullScreenPromptShown &&
                    needsFullScreenPermission(this@MainActivity)
                ) {
                    showFullScreenPrompt = true
                }
                kotlinx.coroutines.delay(150)
                isAppReady = true
            }

            TickAtTheme(settings = settings, themeMode = themeMode) {
                if (onboardingState == null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {}
                } else {
                    AppNavigation(
                        repository          = repository,
                        startWithOnboarding = !onboardingState!!,
                        onOnboardingComplete = {
                            lifecycleScope.launch {
                                dataStore.edit { it[ONBOARDING_COMPLETE] = true }
                            }
                            // 新しく入れた人には、はじめての案内が終わった直後に1回出す
                            if (!fullScreenPromptShown && needsFullScreenPermission(this@MainActivity)) {
                                showFullScreenPrompt = true
                            }
                        }
                    )

                    if (showFullScreenPrompt) {
                        val closePrompt: () -> Unit = {
                            showFullScreenPrompt = false
                            fullScreenPromptShown = true
                            lifecycleScope.launch {
                                dataStore.edit { it[FULL_SCREEN_PROMPT_SHOWN] = true }
                            }
                        }
                        FullScreenPermissionDialog(
                            title          = FULL_SCREEN_PROMPT_TITLE,
                            message        = FULL_SCREEN_PROMPT_MESSAGE,
                            onLater        = closePrompt,
                            onOpenSettings = {
                                closePrompt()
                                openFullScreenPermissionSettings(this@MainActivity)
                            }
                        )
                    }
                }
            }
        }
    }

    // すでに開いているときに通知を押した場合（singleTask のため、ここに届く）
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        TaskAlertService.stopFromNotificationTap(this, intent)
    }
}