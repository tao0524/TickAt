package com.tao0524.tickat.ui.screen.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tao0524.tickat.widget.TickAtWidgetReceiver
import com.tao0524.tickat.widget.WidgetDataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType

val Context.displaySettingsDataStore by preferencesDataStore(name = "tickat_display_settings")

val KEY_BG_COLOR             = longPreferencesKey("bg_color")
val KEY_BG_ALPHA             = intPreferencesKey("bg_alpha")
val KEY_BG_GRADIENT_END      = longPreferencesKey("bg_gradient_end")
val KEY_TEXT_COLOR           = longPreferencesKey("text_color")
val KEY_USE_24_HOUR          = booleanPreferencesKey("use_24_hour")
val KEY_SHOW_TIME            = booleanPreferencesKey("show_time")
val KEY_SHOW_SECONDS         = booleanPreferencesKey("show_seconds")
val KEY_DATE_FORMAT          = stringPreferencesKey("date_format")
val KEY_WEEKDAY_FORMAT       = stringPreferencesKey("weekday_format")
val KEY_DATE_WEEKDAY_ORDER   = stringPreferencesKey("date_weekday_order")
val KEY_DATE_PATTERN         = stringPreferencesKey("date_pattern")
val KEY_FONT_WEIGHT          = stringPreferencesKey("font_weight")
val KEY_CORNER_RADIUS_RATIO  = floatPreferencesKey("corner_radius_ratio")
val KEY_DATE_TEXT_COLOR       = longPreferencesKey("date_text_color")
val KEY_NOTIFICATION_SOUND    = stringPreferencesKey("notification_sound")
val KEY_NOTIFICATION_DURATION = intPreferencesKey("notification_duration")
val KEY_BG_IMAGE_URI           = stringPreferencesKey("bg_image_uri")
val KEY_GRADIENT_COLOR_COUNT   = intPreferencesKey("gradient_color_count")
val KEY_BG_COLOR2              = longPreferencesKey("bg_color2")
val KEY_COMPACT_BG             = booleanPreferencesKey("compact_bg")
val KEY_BG_TYPE                = stringPreferencesKey("bg_type")
val KEY_GRADIENT_DIRECTION     = stringPreferencesKey("gradient_direction")
val KEY_CLOCK_DATE_BALANCE     = intPreferencesKey("clock_date_balance")
val KEY_FONT_SCALE             = floatPreferencesKey("font_scale")
val KEY_GRADIENT_CENTER        = stringPreferencesKey("gradient_center")
val KEY_LINEAR_START_POINT     = stringPreferencesKey("linear_start_point")
val KEY_IS_ITALIC              = booleanPreferencesKey("is_italic")
val KEY_FONT_FAMILY            = stringPreferencesKey("font_family")
val KEY_SHOW_TEXT_SHADOW       = booleanPreferencesKey("show_text_shadow")
val KEY_AM_PM_POSITION         = stringPreferencesKey("am_pm_position")
val KEY_AM_PM_LABEL            = stringPreferencesKey("am_pm_label")
val KEY_AM_PM_SCALE            = floatPreferencesKey("am_pm_scale")
val KEY_BG_COLOR2_ALPHA        = intPreferencesKey("bg_color2_alpha")
val KEY_BG_GRADIENT_END_ALPHA  = intPreferencesKey("bg_gradient_end_alpha")
val KEY_AM_PM_COLOR            = longPreferencesKey("am_pm_color")
val KEY_TIME_OFFSET            = intPreferencesKey("time_offset")
val KEY_SHOW_TASK_NAME         = booleanPreferencesKey("show_task_name")
val KEY_SHOW_COUNTDOWN         = booleanPreferencesKey("show_countdown")
val KEY_SHOW_NEXT_ALARM        = booleanPreferencesKey("show_next_alarm")
val KEY_ALERT_MODE             = stringPreferencesKey("alert_mode")
val KEY_MESSAGE_TEXT_COLOR     = longPreferencesKey("message_text_color")
val KEY_MESSAGE_SCALE          = floatPreferencesKey("message_scale")
private val KEY_THEME_MODE           = stringPreferencesKey("theme_mode")
private val KEY_PREVIEW_VISIBLE      = booleanPreferencesKey("preview_visible")
private val KEY_PREVIEW_SIZE_PERCENT = intPreferencesKey("preview_size_percent")
private val KEY_HINT_SETTINGS  = booleanPreferencesKey("hint_settings")
private val KEY_FONT_TIP_VISIBLE = booleanPreferencesKey("font_tip_visible")
enum class BackgroundType { TRANSPARENT, SOLID, LINEAR, RADIAL, IMAGE }
enum class TextWeight { REGULAR, BOLD }
enum class CornerStyle { PILL, ROUNDED, SOFT, SQUARE }
enum class GradientDirection { HORIZONTAL, DIAGONAL, VERTICAL, RADIAL }
enum class GradientCenter {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}
enum class WidgetFont(
    val displayName: String,
    val assetPath: String? = null
) {
    // --- 既存（システムフォント） ---
    ROBOTO("Roboto"),
    THIN("Roboto Thin"),
    LIGHT("Roboto Light"),
    MEDIUM("Roboto Medium"),
    BLACK("Roboto Black"),
    CONDENSED("Roboto Condensed"),
    SERIF("Noto Serif"),
    MONO("Droid Sans Mono"),

    // --- 時計・ディスプレイ系 ---
    ORBITRON("Orbitron", "fonts/Orbitron-Regular.ttf"),
    CHAKRA_PETCH("Chakra Petch", "fonts/ChakraPetch-Regular.ttf"),
    OXANIUM("Oxanium", "fonts/Oxanium-Regular.ttf"),
    RAJDHANI("Rajdhani", "fonts/Rajdhani-Regular.ttf"),
    EXO_2("Exo 2", "fonts/Exo2-Regular.ttf"),
    ALDRICH("Aldrich", "fonts/Aldrich-Regular.ttf"),

    // --- モダン・サンセリフ系 ---
    MONTSERRAT("Montserrat", "fonts/Montserrat-Regular.ttf"),
    POPPINS("Poppins", "fonts/Poppins-Regular.ttf"),
    INTER("Inter", "fonts/Inter-Regular.ttf"),
    LATO("Lato", "fonts/Lato-Regular.ttf"),
    DM_SANS("DM Sans", "fonts/DMSans-Regular.ttf"),
    RALEWAY("Raleway", "fonts/Raleway-Regular.ttf"),
    SPACE_GROTESK("Space Grotesk", "fonts/SpaceGrotesk-Regular.ttf"),

    // --- 個性系 ---
    PLAYFAIR("Playfair Display", "fonts/PlayfairDisplay-Regular.ttf"),
    SPACE_MONO("Space Mono", "fonts/SpaceMono-Regular.ttf"),
    CAVEAT("Caveat", "fonts/Caveat-Regular.ttf");

    fun toTypeface(context: android.content.Context, style: Int = android.graphics.Typeface.NORMAL): android.graphics.Typeface {
        assetPath?.let { path ->
            return try {
                val base = android.graphics.Typeface.createFromAsset(context.assets, path)
                if (style == android.graphics.Typeface.NORMAL) base else android.graphics.Typeface.create(base, style)
            } catch (e: Exception) {
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, style)
            }
        }
        return when (this) {
            THIN      -> android.graphics.Typeface.create("sans-serif-thin", style)
            LIGHT     -> android.graphics.Typeface.create("sans-serif-light", style)
            MEDIUM    -> android.graphics.Typeface.create("sans-serif-medium", style)
            BLACK     -> android.graphics.Typeface.create("sans-serif-black", style)
            CONDENSED -> android.graphics.Typeface.create("sans-serif-condensed", style)
            SERIF     -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, style)
            MONO      -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, style)
            else      -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, style)
        }
    }
}
enum class AmPmPosition { AFTER, BEFORE }
enum class AmPmLabel    { JAPANESE, ENGLISH }
enum class ThemeMode    { SYSTEM, DARK, LIGHT }
data class AppSettings(
    val bgColor:             Long        = 0xFF1A237EL,
    val bgAlpha:             Int         = 95,
    val bgGradientEnd:       Long        = 0L,
    val textColor:           Long        = 0xFFFFFFFFL,
    val use24Hour:           Boolean     = true,
    val showTime:            Boolean     = true,
    val showSeconds:         Boolean     = false,
    val dateFormat:          String      = "",
    val weekdayFormat:       String      = "",
    val dateWeekdayOrder:    String      = "WEEKDAY_FIRST",
    val datePattern:         String      = "M月d日",
    val fontWeight:          TextWeight  = TextWeight.BOLD,
    val cornerRadiusRatio:   Float       = 0.1f,
    val dateTextColor:            Long   = 0x99E6E1E5L,
    val notificationSoundUri:     String = "",
    val notificationDuration:     Int    = 5,
    val bgImageUri:               String  = "",
    val gradientColorCount:       Int     = 2,
    val bgColor2:                 Long    = 0L,
    val compactBg:                Boolean           = false,
    val bgType:                   BackgroundType     = BackgroundType.SOLID,
    val gradientDirection:        GradientDirection  = GradientDirection.DIAGONAL,
    val clockDateBalance:         Int                = 0,
    val fontScale:                Float              = 1.0f,
    val gradientCenter:           GradientCenter     = GradientCenter.CENTER,
    val linearStartPoint:         GradientCenter     = GradientCenter.TOP_LEFT,
    val isItalic:                 Boolean            = false,
    val fontFamily:               WidgetFont         = WidgetFont.ROBOTO,
    val showTextShadow:           Boolean            = false,
    val amPmPosition:             AmPmPosition       = AmPmPosition.AFTER,
    val amPmLabel:                AmPmLabel          = AmPmLabel.JAPANESE,
    val amPmScale:                Float              = 0.55f,
    val bgColor2Alpha:            Int                = 100,
    val bgGradientEndAlpha:       Int                = 100,
    val amPmColor:                Long               = 0L,
    val timeOffset:               Int                = 0,
    val showTaskName:             Boolean            = true,
    val showCountdown:            Boolean            = true,
    val alertMode:                String             = "NOTIFICATION",
    val messageTextColor:         Long               = 0x99E6E1E5L,
    val messageScale:             Float              = 1.0f,
    val showNextAlarm:            Boolean            = true
)

class SettingsViewModel(
    private val context: Context,
    private val taskRepository: TaskRepository? = null,
    private val appWidgetId: Int
) : ViewModel() {

    private val widgetDataStore = WidgetDataStoreManager.getStore(context, appWidgetId)

    companion object {
        val CONTEXT_KEY: CreationExtras.Key<Context> =
            object : CreationExtras.Key<Context> {}
        val REPOSITORY_KEY: CreationExtras.Key<TaskRepository?> =
            object : CreationExtras.Key<TaskRepository?> {}
        val WIDGET_ID_KEY: CreationExtras.Key<Int> =
            object : CreationExtras.Key<Int> {}

        val Factory = viewModelFactory {
            initializer {
                val ctx      = this[CONTEXT_KEY]    ?: error("Context not provided")
                val repo     = this[REPOSITORY_KEY]
                val widgetId = this[WIDGET_ID_KEY]
                    ?: createSavedStateHandle().get<Int>("widgetId")
                    ?: 0
                SettingsViewModel(ctx.applicationContext, repo, widgetId)
            }
        }
    }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _previewVisible = MutableStateFlow(true)
    val previewVisible: StateFlow<Boolean> = _previewVisible.asStateFlow()

    private val _previewSizePercent = MutableStateFlow(90)
    val previewSizePercent: StateFlow<Int> = _previewSizePercent.asStateFlow()

    private val _hintSettingsShown = MutableStateFlow(true)
    val hintSettingsShown: StateFlow<Boolean> = _hintSettingsShown.asStateFlow()

    private val _fontTipVisible = MutableStateFlow(true)
    val fontTipVisible: StateFlow<Boolean> = _fontTipVisible.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                widgetDataStore.data
                    .map { prefs ->
                        AppSettings(
                            bgColor       = prefs[KEY_BG_COLOR]         ?: 0xFF1A237EL,
                            bgAlpha       = prefs[KEY_BG_ALPHA]          ?: 95,
                            bgGradientEnd = prefs[KEY_BG_GRADIENT_END]   ?: 0L,
                            textColor     = prefs[KEY_TEXT_COLOR]        ?: 0xFFFFFFFFL,
                            use24Hour    = prefs[KEY_USE_24_HOUR]  ?: true,
                            showTime     = prefs[KEY_SHOW_TIME]    ?: true,
                            showSeconds  = prefs[KEY_SHOW_SECONDS] ?: false,
                            dateFormat       = prefs[KEY_DATE_FORMAT]        ?: "",
                            weekdayFormat    = prefs[KEY_WEEKDAY_FORMAT]     ?: "",
                            dateWeekdayOrder = prefs[KEY_DATE_WEEKDAY_ORDER] ?: "WEEKDAY_FIRST",
                            datePattern      = prefs[KEY_DATE_PATTERN]       ?: "",
                            fontWeight   = prefs[KEY_FONT_WEIGHT]
                                ?.let { runCatching { TextWeight.valueOf(it) }.getOrNull() }
                                ?: TextWeight.BOLD,
                            cornerRadiusRatio    = prefs[KEY_CORNER_RADIUS_RATIO] ?: 0.1f,
                            dateTextColor            = prefs[KEY_DATE_TEXT_COLOR]       ?: 0x99E6E1E5L,
                            notificationSoundUri     = prefs[KEY_NOTIFICATION_SOUND]    ?: "",
                            notificationDuration     = prefs[KEY_NOTIFICATION_DURATION] ?: 5,
                            bgImageUri               = prefs[KEY_BG_IMAGE_URI]          ?: "",
                            gradientColorCount       = prefs[KEY_GRADIENT_COLOR_COUNT]  ?: 2,
                            bgColor2                 = prefs[KEY_BG_COLOR2]             ?: 0L,
                            compactBg                = prefs[KEY_COMPACT_BG]            ?: false,
                            bgType                   = prefs[KEY_BG_TYPE]
                                ?.let { runCatching { BackgroundType.valueOf(it) }.getOrNull() }
                                ?: BackgroundType.SOLID,
                            gradientDirection        = prefs[KEY_GRADIENT_DIRECTION]
                                ?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() }
                                ?: GradientDirection.DIAGONAL,
                            clockDateBalance         = prefs[KEY_CLOCK_DATE_BALANCE]        ?: 0,
                            fontScale                = prefs[KEY_FONT_SCALE]               ?: 1.0f,
                            gradientCenter           = prefs[KEY_GRADIENT_CENTER]
                                ?.let { runCatching { GradientCenter.valueOf(it) }.getOrNull() }
                                ?: GradientCenter.CENTER,
                            linearStartPoint         = prefs[KEY_LINEAR_START_POINT]
                                ?.let { runCatching { GradientCenter.valueOf(it) }.getOrNull() }
                                ?: GradientCenter.TOP_LEFT,
                            isItalic                 = prefs[KEY_IS_ITALIC]            ?: false,
                            fontFamily               = prefs[KEY_FONT_FAMILY]
                                ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
                                ?: WidgetFont.ROBOTO,
                            showTextShadow           = prefs[KEY_SHOW_TEXT_SHADOW] ?: false,
                            amPmPosition             = prefs[KEY_AM_PM_POSITION]
                                ?.let { runCatching { AmPmPosition.valueOf(it) }.getOrNull() }
                                ?: AmPmPosition.AFTER,
                            amPmLabel                = prefs[KEY_AM_PM_LABEL]
                                ?.let { runCatching { AmPmLabel.valueOf(it) }.getOrNull() }
                                ?: AmPmLabel.JAPANESE,
                            amPmScale                = prefs[KEY_AM_PM_SCALE] ?: 0.55f,
                            bgColor2Alpha            = prefs[KEY_BG_COLOR2_ALPHA]       ?: 100,
                            bgGradientEndAlpha       = prefs[KEY_BG_GRADIENT_END_ALPHA] ?: 100,
                            amPmColor                = prefs[KEY_AM_PM_COLOR]           ?: 0L,
                            timeOffset               = prefs[KEY_TIME_OFFSET]           ?: 0,
                            showTaskName             = prefs[KEY_SHOW_TASK_NAME]        ?: true,
                            showCountdown            = prefs[KEY_SHOW_COUNTDOWN]        ?: true,
                            showNextAlarm            = prefs[KEY_SHOW_NEXT_ALARM]       ?: true,
                            alertMode                = prefs[KEY_ALERT_MODE]            ?: "NOTIFICATION",
                            messageTextColor         = prefs[KEY_MESSAGE_TEXT_COLOR]    ?: 0x99E6E1E5L,
                            messageScale             = prefs[KEY_MESSAGE_SCALE]        ?: 1.0f
                        ).let { raw ->
                            if (raw.datePattern.isNotEmpty()) return@let raw
                            val datePart    = raw.dateFormat
                            val weekdayPart = raw.weekdayFormat
                            val merged = when {
                                datePart.isEmpty() && weekdayPart.isEmpty() -> ""
                                datePart.isEmpty()  -> weekdayPart
                                weekdayPart.isEmpty() -> datePart
                                raw.dateWeekdayOrder == "DATE_FIRST" -> "$datePart, $weekdayPart"
                                else -> "$weekdayPart, $datePart"
                            }
                            raw.copy(datePattern = merged)
                        }
                    },
                context.displaySettingsDataStore.data
            ) { appSettings, displayPrefs ->
                appSettings to displayPrefs
            }.collect { (appSettings, displayPrefs) ->
                _settings.value = appSettings.copy(
                    alertMode = displayPrefs[KEY_ALERT_MODE] ?: "NOTIFICATION",
                    notificationSoundUri = displayPrefs[KEY_NOTIFICATION_SOUND] ?: "",
                    notificationDuration = displayPrefs[KEY_NOTIFICATION_DURATION] ?: 5
                )
                _previewVisible.value = displayPrefs[KEY_PREVIEW_VISIBLE] ?: true
                _previewSizePercent.value = (displayPrefs[KEY_PREVIEW_SIZE_PERCENT] ?: 90).coerceIn(40, 100)
                _hintSettingsShown.value = displayPrefs[KEY_HINT_SETTINGS] ?: false
                _fontTipVisible.value = displayPrefs[KEY_FONT_TIP_VISIBLE] ?: true
                _themeMode.value = displayPrefs[KEY_THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
                _isLoaded.value = true
            }
        }
    }

    val timeblockTasks: StateFlow<List<Task>> = if (taskRepository != null) {
        taskRepository.allTasks
            .map { list -> list.filter { it.taskType == TaskType.TIMEBLOCK } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }

    fun savePreviewVisible(visible: Boolean) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_PREVIEW_VISIBLE] = visible
            }
        }
    }

    fun savePreviewSizePercent(percent: Int) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_PREVIEW_SIZE_PERCENT] = percent.coerceIn(40, 100)
            }
        }
    }

    fun dismissHintSettings() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_HINT_SETTINGS] = true
            }
        }
    }

    fun saveFontTipVisible(visible: Boolean) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_FONT_TIP_VISIBLE] = visible
            }
        }
    }

    fun save(s: AppSettings) {
        viewModelScope.launch {
            widgetDataStore.edit { prefs ->
                prefs[KEY_BG_COLOR]           = s.bgColor
                prefs[KEY_BG_ALPHA]           = s.bgAlpha
                prefs[KEY_BG_GRADIENT_END]    = s.bgGradientEnd
                prefs[KEY_TEXT_COLOR]         = s.textColor
                prefs[KEY_USE_24_HOUR]        = s.use24Hour
                prefs[KEY_SHOW_TIME]          = s.showTime
                prefs[KEY_SHOW_SECONDS]       = s.showSeconds
                prefs[KEY_DATE_FORMAT]        = s.dateFormat
                prefs[KEY_WEEKDAY_FORMAT]     = s.weekdayFormat
                prefs[KEY_DATE_WEEKDAY_ORDER] = s.dateWeekdayOrder
                prefs[KEY_DATE_PATTERN]       = s.datePattern
                prefs[KEY_FONT_WEIGHT]        = s.fontWeight.name
                prefs[KEY_CORNER_RADIUS_RATIO] = s.cornerRadiusRatio
                prefs[KEY_DATE_TEXT_COLOR]       = s.dateTextColor
                prefs[KEY_BG_IMAGE_URI]          = s.bgImageUri
                prefs[KEY_GRADIENT_COLOR_COUNT]  = s.gradientColorCount
                prefs[KEY_BG_COLOR2]             = s.bgColor2
                prefs[KEY_COMPACT_BG]            = s.compactBg
                prefs[KEY_BG_TYPE]               = s.bgType.name
                prefs[KEY_GRADIENT_DIRECTION]    = s.gradientDirection.name
                prefs[KEY_CLOCK_DATE_BALANCE]    = s.clockDateBalance
                prefs[KEY_FONT_SCALE]            = s.fontScale
                prefs[KEY_GRADIENT_CENTER]       = s.gradientCenter.name
                prefs[KEY_LINEAR_START_POINT]    = s.linearStartPoint.name
                prefs[KEY_IS_ITALIC]             = s.isItalic
                prefs[KEY_FONT_FAMILY]           = s.fontFamily.name
                prefs[KEY_SHOW_TEXT_SHADOW]      = s.showTextShadow
                prefs[KEY_AM_PM_POSITION]        = s.amPmPosition.name
                prefs[KEY_AM_PM_LABEL]           = s.amPmLabel.name
                prefs[KEY_AM_PM_SCALE]           = s.amPmScale
                prefs[KEY_BG_COLOR2_ALPHA]       = s.bgColor2Alpha
                prefs[KEY_BG_GRADIENT_END_ALPHA] = s.bgGradientEndAlpha
                prefs[KEY_AM_PM_COLOR]           = s.amPmColor
                prefs[KEY_TIME_OFFSET]           = s.timeOffset
                prefs[KEY_SHOW_TASK_NAME]        = s.showTaskName
                prefs[KEY_SHOW_COUNTDOWN]        = s.showCountdown
                prefs[KEY_SHOW_NEXT_ALARM]       = s.showNextAlarm
                prefs[KEY_MESSAGE_TEXT_COLOR]    = s.messageTextColor
                prefs[KEY_MESSAGE_SCALE]         = s.messageScale
            }
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_ALERT_MODE]            = s.alertMode
                prefs[KEY_NOTIFICATION_SOUND]     = s.notificationSoundUri
                prefs[KEY_NOTIFICATION_DURATION]  = s.notificationDuration
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

    fun reset() = save(AppSettings())
}