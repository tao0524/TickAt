package com.tao0524.tickat.ui.screen.appsettings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tao0524.tickat.ui.screen.settings.KEY_ALERT_MODE
import com.tao0524.tickat.ui.screen.settings.KEY_END_ALERT_MODE
import com.tao0524.tickat.ui.screen.settings.KEY_END_NOTIFICATION_DURATION
import com.tao0524.tickat.ui.screen.settings.KEY_END_NOTIFICATION_SOUND
import com.tao0524.tickat.ui.screen.settings.KEY_END_NOTIFICATION_SOUND_NAME
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_DURATION
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_SOUND
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_SOUND_NAME
import com.tao0524.tickat.ui.screen.settings.KEY_THEME_MODE
import com.tao0524.tickat.ui.screen.settings.ThemeMode
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val KEY_HINT_APP_SETTINGS = booleanPreferencesKey("hint_app_settings")

// アプリ全体の設定（アプリテーマ・通知）。どれも押した瞬間に保存する。
// ウィジェットごとの表示設定とは別に、displaySettingsDataStore に置く。
class AppSettingsViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppSettingsViewModel(context.applicationContext) }
        }
    }

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _alertMode = MutableStateFlow("NOTIFICATION")
    val alertMode: StateFlow<String> = _alertMode.asStateFlow()

    private val _notificationSoundUri = MutableStateFlow("")
    val notificationSoundUri: StateFlow<String> = _notificationSoundUri.asStateFlow()

    private val _notificationSoundName = MutableStateFlow("")
    val notificationSoundName: StateFlow<String> = _notificationSoundName.asStateFlow()

    private val _notificationDuration = MutableStateFlow(5)
    val notificationDuration: StateFlow<Int> = _notificationDuration.asStateFlow()

    // 終了の通知（タイムブロックの終了時）。音と再生時間の null は「まだ選んでいない（開始と同じ）」
    private val _endAlertMode = MutableStateFlow("NOTIFICATION")
    val endAlertMode: StateFlow<String> = _endAlertMode.asStateFlow()

    private val _endSoundUri = MutableStateFlow<String?>(null)
    val endSoundUri: StateFlow<String?> = _endSoundUri.asStateFlow()

    private val _endSoundName = MutableStateFlow("")
    val endSoundName: StateFlow<String> = _endSoundName.asStateFlow()

    private val _endDuration = MutableStateFlow<Int?>(null)
    val endDuration: StateFlow<Int?> = _endDuration.asStateFlow()

    private val _hintShown = MutableStateFlow(true)
    val hintShown: StateFlow<Boolean> = _hintShown.asStateFlow()

    init {
        viewModelScope.launch {
            context.displaySettingsDataStore.data.collect { prefs ->
                _themeMode.value = prefs[KEY_THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
                _alertMode.value = prefs[KEY_ALERT_MODE] ?: "NOTIFICATION"
                _notificationSoundUri.value = prefs[KEY_NOTIFICATION_SOUND] ?: ""
                _notificationSoundName.value = prefs[KEY_NOTIFICATION_SOUND_NAME] ?: ""
                _notificationDuration.value = prefs[KEY_NOTIFICATION_DURATION] ?: 5
                _endAlertMode.value = prefs[KEY_END_ALERT_MODE] ?: "NOTIFICATION"
                _endSoundUri.value = prefs[KEY_END_NOTIFICATION_SOUND]
                _endSoundName.value = prefs[KEY_END_NOTIFICATION_SOUND_NAME] ?: ""
                _endDuration.value = prefs[KEY_END_NOTIFICATION_DURATION]
                _hintShown.value = prefs[KEY_HINT_APP_SETTINGS] ?: false
                _isLoaded.value = true
            }
        }
    }

    fun saveThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = mode.name
            }
        }
    }

    fun saveAlertMode(mode: String) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_ALERT_MODE] = mode
            }
        }
    }

    /** 通知音を保存する。name が null のときは、名前は今のままにする */
    fun saveNotificationSound(uri: String, name: String? = null) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_NOTIFICATION_SOUND] = uri
                if (name != null) prefs[KEY_NOTIFICATION_SOUND_NAME] = name
            }
        }
    }

    fun saveNotificationDuration(seconds: Int) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_NOTIFICATION_DURATION] = seconds
            }
        }
    }

    fun saveEndAlertMode(mode: String) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_END_ALERT_MODE] = mode
            }
        }
    }

    /** 終了の通知音を保存する。name が null のときは、名前は今のままにする */
    fun saveEndNotificationSound(uri: String, name: String? = null) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_END_NOTIFICATION_SOUND] = uri
                if (name != null) prefs[KEY_END_NOTIFICATION_SOUND_NAME] = name
            }
        }
    }

    /** 終了の通知音を「開始と同じ」に戻す（終了の音の値を消すと、開始の音で鳴る） */
    fun resetEndNotificationSound() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs.remove(KEY_END_NOTIFICATION_SOUND)
                prefs.remove(KEY_END_NOTIFICATION_SOUND_NAME)
            }
        }
    }

    fun saveEndNotificationDuration(seconds: Int) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_END_NOTIFICATION_DURATION] = seconds
            }
        }
    }

    fun dismissHint() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_HINT_APP_SETTINGS] = true
            }
        }
    }
}
