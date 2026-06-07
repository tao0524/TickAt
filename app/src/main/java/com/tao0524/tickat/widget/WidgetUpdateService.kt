package com.tao0524.tickat.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.IBinder
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.R
import com.tao0524.tickat.ui.screen.settings.AppSettings
import com.tao0524.tickat.ui.screen.settings.AmPmLabel
import com.tao0524.tickat.ui.screen.settings.AmPmPosition
import com.tao0524.tickat.ui.screen.settings.BackgroundType
import com.tao0524.tickat.ui.screen.settings.CornerStyle
import com.tao0524.tickat.ui.screen.settings.GradientCenter
import com.tao0524.tickat.ui.screen.settings.GradientDirection
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_TIME_OFFSET
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_LABEL
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_POSITION
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_SCALE
import com.tao0524.tickat.ui.screen.settings.KEY_BG_ALPHA
import com.tao0524.tickat.ui.screen.settings.KEY_BG_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_BG_COLOR2
import com.tao0524.tickat.ui.screen.settings.KEY_BG_COLOR2_ALPHA
import com.tao0524.tickat.ui.screen.settings.KEY_BG_GRADIENT_END
import com.tao0524.tickat.ui.screen.settings.KEY_BG_GRADIENT_END_ALPHA
import com.tao0524.tickat.ui.screen.settings.KEY_BG_IMAGE_URI
import com.tao0524.tickat.ui.screen.settings.KEY_BG_TYPE
import com.tao0524.tickat.ui.screen.settings.KEY_CLOCK_DATE_BALANCE
import com.tao0524.tickat.ui.screen.settings.KEY_COMPACT_BG
import com.tao0524.tickat.ui.screen.settings.KEY_CORNER_STYLE
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_FORMAT
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_TEXT_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_WEEKDAY_ORDER
import com.tao0524.tickat.ui.screen.settings.KEY_FONT_FAMILY
import com.tao0524.tickat.ui.screen.settings.KEY_FONT_SCALE
import com.tao0524.tickat.ui.screen.settings.KEY_FONT_WEIGHT
import com.tao0524.tickat.ui.screen.settings.KEY_GRADIENT_CENTER
import com.tao0524.tickat.ui.screen.settings.KEY_GRADIENT_COLOR_COUNT
import com.tao0524.tickat.ui.screen.settings.KEY_GRADIENT_DIRECTION
import com.tao0524.tickat.ui.screen.settings.KEY_IS_ITALIC
import com.tao0524.tickat.ui.screen.settings.KEY_LINEAR_START_POINT
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_DURATION
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_SOUND
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_SECONDS
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_TEXT_SHADOW
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_TIME
import com.tao0524.tickat.ui.screen.settings.KEY_TEXT_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_USE_24_HOUR
import com.tao0524.tickat.ui.screen.settings.KEY_WEEKDAY_FORMAT
import com.tao0524.tickat.ui.screen.settings.KEY_WIDGET_SIZE
import com.tao0524.tickat.ui.screen.settings.TextWeight
import com.tao0524.tickat.ui.screen.settings.WidgetFont
import com.tao0524.tickat.ui.screen.settings.WidgetSize
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WidgetUpdateService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var tickJob: Job? = null
    private var settingsJob: Job? = null
    private var isScreenOn = true

    @Volatile private var cachedSettings: AppSettings = AppSettings()
    @Volatile private var cachedTypeface: Typeface = Typeface.DEFAULT
    @Volatile private var cachedClockPx: Float = 0f
    @Volatile private var cachedDatePx: Float = 0f
    @Volatile private var cachedBgBitmap: android.graphics.Bitmap? = null
    @Volatile private var isFullRedrawNeeded: Boolean = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    isFullRedrawNeeded = true
                    startTicking()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopTicking()
                }
                Intent.ACTION_TIME_TICK -> {
                    if (isScreenOn && (tickJob == null || tickJob?.isActive == false)) {
                        startTicking()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_TIME_TICK)
        }
        registerReceiver(screenReceiver, filter)
        startSettingsObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTicking()
        settingsJob?.cancel()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSettingsObserver() {
        settingsJob = serviceScope.launch {
            displaySettingsDataStore.data.collect { prefs ->
                val newSettings = AppSettings(
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
                    amPmColor            = prefs[KEY_AM_PM_COLOR]           ?: 0L,
                    timeOffset           = prefs[KEY_TIME_OFFSET]           ?: 0
                )
                val typefaceStyle = when {
                    newSettings.fontWeight == TextWeight.BOLD && newSettings.isItalic -> Typeface.BOLD_ITALIC
                    newSettings.fontWeight == TextWeight.BOLD                         -> Typeface.BOLD
                    newSettings.isItalic                                              -> Typeface.ITALIC
                    else                                                              -> Typeface.NORMAL
                }
                cachedTypeface = when (newSettings.fontFamily) {
                    WidgetFont.SERIF     -> Typeface.create(Typeface.SERIF, typefaceStyle)
                    WidgetFont.CONDENSED -> Typeface.create("sans-serif-condensed", typefaceStyle)
                    WidgetFont.MONO      -> Typeface.create(Typeface.MONOSPACE, typefaceStyle)
                    else                 -> Typeface.create(Typeface.DEFAULT, typefaceStyle)
                }
                val scaledDensity = resources.displayMetrics.scaledDensity
                val manager = AppWidgetManager.getInstance(this@WidgetUpdateService)
                val ids = manager.getAppWidgetIds(ComponentName(this@WidgetUpdateService, TickAtWidgetReceiver::class.java))
                val h = if (ids.isNotEmpty()) manager.getAppWidgetOptions(ids[0]).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44) else 44
                val (clockSp, dateSp) = calcFontSizes(h, newSettings.clockDateBalance, newSettings.fontScale)
                val isFirstLoad = cachedClockPx == 0f
                cachedClockPx = clockSp * scaledDensity
                cachedDatePx  = dateSp  * scaledDensity
                cachedBgBitmap = buildBackgroundBitmap(this@WidgetUpdateService, newSettings)
                val prevShowSeconds = cachedSettings.showSeconds
                cachedSettings = newSettings
                isFullRedrawNeeded = true
                if (isFirstLoad || (newSettings.showSeconds && !prevShowSeconds)) {
                    startTicking()
                }
            }
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        isFullRedrawNeeded = true
        tickJob = serviceScope.launch {
            val initialDelay = 1000L - (System.currentTimeMillis() % 1000L)
            delay(initialDelay.coerceAtLeast(0L))
            while (isActive) {
                updateWidgets()
                val nextDelay = 1000L - (System.currentTimeMillis() % 1000L)
                delay(if (cachedSettings.showSeconds) nextDelay.coerceAtLeast(1L) else 60_000L)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun updateWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, TickAtWidgetReceiver::class.java)
        )
        if (ids.isEmpty()) {
            stopSelf()
            return
        }
        val settings = cachedSettings
        for (id in ids) {
            val opts = manager.getAppWidgetOptions(id)
            val h    = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44)
            if (isFullRedrawNeeded) {
                val views = TickAtWidget.buildViews(this, settings, h)
                manager.updateAppWidget(id, views)
            } else {
                val views = buildTimeOnlyViews(this, settings, cachedTypeface, cachedClockPx, cachedDatePx, cachedBgBitmap)
                manager.updateAppWidget(id, views)
            }
        }
        isFullRedrawNeeded = false
    }

    private fun startForegroundWithNotification() {
        val channelId = "tickat_widget_update"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "ウィジェット更新",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TickAt")
            .setContentText("ウィジェットを更新中")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
        startForeground(1001, notification)
    }
}