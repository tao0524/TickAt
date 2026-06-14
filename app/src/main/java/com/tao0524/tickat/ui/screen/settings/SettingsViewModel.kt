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
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.widget.TickAtWidgetReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
val KEY_FONT_WEIGHT          = stringPreferencesKey("font_weight")
val KEY_CORNER_STYLE         = stringPreferencesKey("corner_style")
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
val KEY_SHOW_CHECKBOXES        = booleanPreferencesKey("show_checkboxes")
val KEY_ALERT_MODE             = stringPreferencesKey("alert_mode")
private val KEY_HINT_SETTINGS  = booleanPreferencesKey("hint_settings")
enum class BackgroundType { TRANSPARENT, SOLID, LINEAR, RADIAL, IMAGE }
enum class TextWeight { REGULAR, BOLD }
enum class CornerStyle { PILL, ROUNDED, SOFT, SQUARE }
enum class GradientDirection { HORIZONTAL, DIAGONAL, VERTICAL, RADIAL }
enum class GradientCenter {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}
enum class WidgetFont { ROBOTO, SERIF, CONDENSED, MONO }
enum class AmPmPosition { AFTER, BEFORE }
enum class AmPmLabel    { JAPANESE, ENGLISH }

data class AppSettings(
    val bgColor:             Long        = 0xFF1C1B1FL,
    val bgAlpha:             Int         = 100,
    val bgGradientEnd:       Long        = 0L,
    val textColor:           Long        = 0xFFE6E1E5L,
    val use24Hour:           Boolean     = true,
    val showTime:            Boolean     = true,
    val showSeconds:         Boolean     = false,
    val dateFormat:          String      = "",
    val weekdayFormat:       String      = "",
    val dateWeekdayOrder:    String      = "WEEKDAY_FIRST",
    val fontWeight:          TextWeight  = TextWeight.BOLD,
    val cornerStyle:         CornerStyle = CornerStyle.PILL,
    val dateTextColor:            Long   = 0x99E6E1E5L,
    val notificationSoundUri:     String = "",
    val notificationDuration:     Int    = 5,  // 秒
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
    val showCheckboxes:           Boolean            = true,
    val alertMode:                String             = "NOTIFICATION"
)

class SettingsViewModel(private val context: Context) : ViewModel() {

    val settings: StateFlow<AppSettings> = context.displaySettingsDataStore.data
        .map { prefs ->
            AppSettings(
                bgColor       = prefs[KEY_BG_COLOR]         ?: 0xFF1C1B1FL,
                bgAlpha       = prefs[KEY_BG_ALPHA]          ?: 100,
                bgGradientEnd = prefs[KEY_BG_GRADIENT_END]   ?: 0L,
                textColor     = prefs[KEY_TEXT_COLOR]        ?: 0xFFE6E1E5L,
                use24Hour    = prefs[KEY_USE_24_HOUR]  ?: true,
                showTime     = prefs[KEY_SHOW_TIME]    ?: true,
                showSeconds  = prefs[KEY_SHOW_SECONDS] ?: false,
                dateFormat       = prefs[KEY_DATE_FORMAT]        ?: "",
                weekdayFormat    = prefs[KEY_WEEKDAY_FORMAT]     ?: "",
                dateWeekdayOrder = prefs[KEY_DATE_WEEKDAY_ORDER] ?: "WEEKDAY_FIRST",
                fontWeight   = prefs[KEY_FONT_WEIGHT]
                    ?.let { runCatching { TextWeight.valueOf(it) }.getOrNull() }
                    ?: TextWeight.BOLD,
                cornerStyle  = prefs[KEY_CORNER_STYLE]
                    ?.let { runCatching { CornerStyle.valueOf(it) }.getOrNull() }
                    ?: CornerStyle.PILL,
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
                showCheckboxes           = prefs[KEY_SHOW_CHECKBOXES]       ?: true,
                alertMode                = prefs[KEY_ALERT_MODE]            ?: "NOTIFICATION"
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    val hintSettingsShown = context.displaySettingsDataStore.data
        .map { prefs -> prefs[KEY_HINT_SETTINGS] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissHintSettings() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_HINT_SETTINGS] = true
            }
        }
    }

    fun save(s: AppSettings) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
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
                prefs[KEY_FONT_WEIGHT]        = s.fontWeight.name
                prefs[KEY_CORNER_STYLE]       = s.cornerStyle.name
                prefs[KEY_DATE_TEXT_COLOR]       = s.dateTextColor
                prefs[KEY_NOTIFICATION_SOUND]    = s.notificationSoundUri
                prefs[KEY_NOTIFICATION_DURATION] = s.notificationDuration
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
                prefs[KEY_SHOW_CHECKBOXES]       = s.showCheckboxes
                prefs[KEY_ALERT_MODE]            = s.alertMode
            }
        }
    }

    fun reset() = save(AppSettings())
}