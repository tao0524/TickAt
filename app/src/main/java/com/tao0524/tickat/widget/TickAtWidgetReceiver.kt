package com.tao0524.tickat.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.tao0524.tickat.ui.screen.settings.AppSettings
import com.tao0524.tickat.ui.screen.settings.BackgroundType
import com.tao0524.tickat.ui.screen.settings.CornerStyle
import com.tao0524.tickat.ui.screen.settings.GradientDirection
import com.tao0524.tickat.ui.screen.settings.KEY_BG_ALPHA
import com.tao0524.tickat.ui.screen.settings.KEY_BG_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_BG_COLOR2
import com.tao0524.tickat.ui.screen.settings.KEY_BG_GRADIENT_END
import com.tao0524.tickat.ui.screen.settings.KEY_BG_IMAGE_URI
import com.tao0524.tickat.ui.screen.settings.KEY_BG_TYPE
import com.tao0524.tickat.ui.screen.settings.KEY_COMPACT_BG
import com.tao0524.tickat.ui.screen.settings.KEY_CORNER_STYLE
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_TEXT_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_FONT_WEIGHT
import com.tao0524.tickat.ui.screen.settings.KEY_GRADIENT_COLOR_COUNT
import com.tao0524.tickat.ui.screen.settings.KEY_CLOCK_DATE_BALANCE
import com.tao0524.tickat.ui.screen.settings.KEY_FONT_SCALE
import com.tao0524.tickat.ui.screen.settings.GradientCenter
import com.tao0524.tickat.ui.screen.settings.KEY_GRADIENT_CENTER
import com.tao0524.tickat.ui.screen.settings.KEY_LINEAR_START_POINT
import com.tao0524.tickat.ui.screen.settings.KEY_IS_ITALIC
import com.tao0524.tickat.ui.screen.settings.KEY_FONT_FAMILY
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_TEXT_SHADOW
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_POSITION
import com.tao0524.tickat.ui.screen.settings.AmPmPosition
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_LABEL
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_SCALE
import com.tao0524.tickat.ui.screen.settings.AmPmLabel
import com.tao0524.tickat.ui.screen.settings.KEY_BG_COLOR2_ALPHA
import com.tao0524.tickat.ui.screen.settings.KEY_BG_GRADIENT_END_ALPHA
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_COLOR
import com.tao0524.tickat.ui.screen.settings.WidgetFont
import com.tao0524.tickat.ui.screen.settings.KEY_GRADIENT_DIRECTION
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_DURATION
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_SOUND
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_TIME
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_FORMAT
import com.tao0524.tickat.ui.screen.settings.KEY_WEEKDAY_FORMAT
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_WEEKDAY_ORDER
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_SECONDS
import com.tao0524.tickat.ui.screen.settings.KEY_TEXT_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_USE_24_HOUR
import com.tao0524.tickat.ui.screen.settings.KEY_WIDGET_SIZE
import com.tao0524.tickat.ui.screen.settings.TextWeight
import com.tao0524.tickat.ui.screen.settings.WidgetSize
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
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.displaySettingsDataStore.data.first()
            val settings = AppSettings(
                bgColor              = prefs[KEY_BG_COLOR]          ?: 0xFF1C1B1FL,
                bgAlpha              = prefs[KEY_BG_ALPHA]           ?: 100,
                bgGradientEnd        = prefs[KEY_BG_GRADIENT_END]    ?: 0L,
                textColor            = prefs[KEY_TEXT_COLOR]         ?: 0xFFE6E1E5L,
                use24Hour            = prefs[KEY_USE_24_HOUR]        ?: true,
                widgetSize           = prefs[KEY_WIDGET_SIZE]
                    ?.let { runCatching { WidgetSize.valueOf(it) }.getOrNull() }
                    ?: WidgetSize.M,
                showTime             = prefs[KEY_SHOW_TIME]          ?: true,
                showSeconds          = prefs[KEY_SHOW_SECONDS]       ?: false,
                dateFormat           = prefs[KEY_DATE_FORMAT]        ?: "",
                weekdayFormat        = prefs[KEY_WEEKDAY_FORMAT]     ?: "",
                dateWeekdayOrder     = prefs[KEY_DATE_WEEKDAY_ORDER] ?: "WEEKDAY_FIRST",
                fontWeight           = prefs[KEY_FONT_WEIGHT]
                    ?.let { runCatching { TextWeight.valueOf(it) }.getOrNull() }
                    ?: TextWeight.BOLD,
                cornerStyle          = prefs[KEY_CORNER_STYLE]
                    ?.let { runCatching { CornerStyle.valueOf(it) }.getOrNull() }
                    ?: CornerStyle.PILL,
                dateTextColor        = prefs[KEY_DATE_TEXT_COLOR]       ?: 0x99E6E1E5L,
                notificationSoundUri = prefs[KEY_NOTIFICATION_SOUND]    ?: "",
                notificationDuration = prefs[KEY_NOTIFICATION_DURATION] ?: 5,
                bgImageUri           = prefs[KEY_BG_IMAGE_URI]          ?: "",
                gradientColorCount   = prefs[KEY_GRADIENT_COLOR_COUNT]  ?: 2,
                bgColor2             = prefs[KEY_BG_COLOR2]             ?: 0L,
                compactBg            = prefs[KEY_COMPACT_BG]            ?: false,
                bgType               = prefs[KEY_BG_TYPE]
                    ?.let { runCatching { BackgroundType.valueOf(it) }.getOrNull() }
                    ?: BackgroundType.SOLID,
                gradientDirection    = prefs[KEY_GRADIENT_DIRECTION]
                    ?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() }
                    ?: GradientDirection.DIAGONAL,
                clockDateBalance     = prefs[KEY_CLOCK_DATE_BALANCE]    ?: 0,
                fontScale            = prefs[KEY_FONT_SCALE]            ?: 1.0f,
                gradientCenter       = prefs[KEY_GRADIENT_CENTER]
                    ?.let { runCatching { GradientCenter.valueOf(it) }.getOrNull() }
                    ?: GradientCenter.CENTER,
                linearStartPoint     = prefs[KEY_LINEAR_START_POINT]
                    ?.let { runCatching { GradientCenter.valueOf(it) }.getOrNull() }
                    ?: GradientCenter.TOP_LEFT,
                isItalic             = prefs[KEY_IS_ITALIC]            ?: false,
                fontFamily           = prefs[KEY_FONT_FAMILY]
                    ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
                    ?: WidgetFont.ROBOTO,
                showTextShadow       = prefs[KEY_SHOW_TEXT_SHADOW] ?: false,
                amPmPosition         = prefs[KEY_AM_PM_POSITION]
                    ?.let { runCatching { AmPmPosition.valueOf(it) }.getOrNull() }
                    ?: AmPmPosition.AFTER,
                amPmLabel            = prefs[KEY_AM_PM_LABEL]
                    ?.let { runCatching { AmPmLabel.valueOf(it) }.getOrNull() }
                    ?: AmPmLabel.JAPANESE,
                amPmScale            = prefs[KEY_AM_PM_SCALE] ?: 0.55f,
                bgColor2Alpha        = prefs[KEY_BG_COLOR2_ALPHA]       ?: 100,
                bgGradientEndAlpha   = prefs[KEY_BG_GRADIENT_END_ALPHA] ?: 100,
                amPmColor            = prefs[KEY_AM_PM_COLOR]           ?: 0L
            )
            for (id in appWidgetIds) {
                val opts = appWidgetManager.getAppWidgetOptions(id)
                val h    = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44)
                val views = TickAtWidget.buildViews(context, settings, h)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    companion object {
        fun updateAll(context: Context, settings: AppSettings) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, TickAtWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return
            for (id in ids) {
                val opts = manager.getAppWidgetOptions(id)
                val h    = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44)
                val views = TickAtWidget.buildViews(context, settings, h)
                manager.updateAppWidget(id, views)
            }
        }
    }
}