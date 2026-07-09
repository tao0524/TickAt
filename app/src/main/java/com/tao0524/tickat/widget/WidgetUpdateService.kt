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
import android.view.View
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.R
import com.tao0524.tickat.ui.screen.settings.AppSettings
import com.tao0524.tickat.ui.screen.settings.AmPmLabel
import com.tao0524.tickat.ui.screen.settings.AmPmPosition
import com.tao0524.tickat.ui.screen.settings.BackgroundType
import com.tao0524.tickat.ui.screen.settings.GradientCenter
import com.tao0524.tickat.ui.screen.settings.GradientDirection
import com.tao0524.tickat.ui.screen.settings.KEY_AM_PM_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_TIME_OFFSET
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_TASK_NAME
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
import com.tao0524.tickat.ui.screen.settings.KEY_CORNER_RADIUS_RATIO
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_FORMAT
import com.tao0524.tickat.ui.screen.settings.KEY_DATE_PATTERN
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
import com.tao0524.tickat.ui.screen.settings.TextWeight
import com.tao0524.tickat.ui.screen.settings.WidgetFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.tao0524.tickat.data.local.AppDatabase
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_COUNTDOWN
import com.tao0524.tickat.ui.screen.settings.KEY_MESSAGE_TEXT_COLOR
import com.tao0524.tickat.ui.screen.settings.KEY_MESSAGE_SCALE
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

class WidgetUpdateService : Service() {

    companion object {
        const val EXTRA_WIDGET_IDS = "extra_widget_ids"
    }

    private val observerStartedIds = mutableSetOf<Int>()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var tickJob: Job? = null
    private var settingsJob: Job? = null
    private var isScreenOn = true

    private val cachedSettingsMap = mutableMapOf<Int, AppSettings>()
    private val cachedTypefaceMap = mutableMapOf<Int, Typeface>()
    private val cachedClockPxMap  = mutableMapOf<Int, Float>()
    private val cachedDatePxMap   = mutableMapOf<Int, Float>()
    private val cachedMessagePxMap= mutableMapOf<Int, Float>()
    private val cachedBgBitmapMap = mutableMapOf<Int, android.graphics.Bitmap?>()
    private val fullRedrawNeededMap = mutableMapOf<Int, Boolean>()
    @Volatile private var cachedTasks: List<Task> = emptyList()

    private val taskRepository by lazy {
        TaskRepository(AppDatabase.getInstance(applicationContext).taskDao())
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    fullRedrawNeededMap.keys.forEach { fullRedrawNeededMap[it] = true }
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
        startTaskObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ids = intent?.getIntArrayExtra(EXTRA_WIDGET_IDS)
        // android.util.Log.d("TickAtGhost", "onStartCommand newIds=${ids?.toList()} knownIds=${TickAtWidgetReceiver.loadWidgetIds(this)} observerStarted=$observerStartedIds")
        ids?.forEach { id ->
            startSettingsObserverForWidget(id)
        }
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
        val prefs = getSharedPreferences("tickat_ghost_cleanup", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("cleaned", false)) {
            val widgetPrefs = getSharedPreferences("tickat_widget_ids", Context.MODE_PRIVATE)
            widgetPrefs.edit().clear().apply()
            prefs.edit().putBoolean("cleaned", true).apply()
        }
        val ids = TickAtWidgetReceiver.loadWidgetIds(this)
        for (id in ids) {
            startSettingsObserverForWidget(id)
        }
    }

    private fun startSettingsObserverForWidget(appWidgetId: Int) {
        if (appWidgetId in observerStartedIds) return
        observerStartedIds.add(appWidgetId)
        serviceScope.launch {
            WidgetDataStoreManager.getStore(this@WidgetUpdateService, appWidgetId).data.collect { prefs ->
                val newSettings = AppSettings(
                    bgColor              = prefs[KEY_BG_COLOR]          ?: 0xFF1A237EL,
                    bgAlpha              = prefs[KEY_BG_ALPHA]           ?: 95,
                    bgGradientEnd        = prefs[KEY_BG_GRADIENT_END]    ?: 0L,
                    textColor            = prefs[KEY_TEXT_COLOR]         ?: 0xFFFFFFFFL,
                    use24Hour            = prefs[KEY_USE_24_HOUR]        ?: true,
                    showTime             = prefs[KEY_SHOW_TIME]          ?: true,
                    showSeconds          = prefs[KEY_SHOW_SECONDS]       ?: false,
                    dateFormat           = prefs[KEY_DATE_FORMAT]        ?: "",
                    weekdayFormat        = prefs[KEY_WEEKDAY_FORMAT]     ?: "",
                    dateWeekdayOrder     = prefs[KEY_DATE_WEEKDAY_ORDER] ?: "WEEKDAY_FIRST",
                    datePattern          = prefs[KEY_DATE_PATTERN]       ?: "",
                    fontWeight           = prefs[KEY_FONT_WEIGHT]
                        ?.let { runCatching { TextWeight.valueOf(it) }.getOrNull() }
                        ?: TextWeight.BOLD,
                    cornerRadiusRatio    = prefs[KEY_CORNER_RADIUS_RATIO] ?: 0.1f,
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
                    timeOffset           = prefs[KEY_TIME_OFFSET]           ?: 0,
                    showTaskName         = prefs[KEY_SHOW_TASK_NAME]        ?: true,
                    showCountdown        = prefs[KEY_SHOW_COUNTDOWN]        ?: true,
                    messageTextColor     = prefs[KEY_MESSAGE_TEXT_COLOR]    ?: 0x99E6E1E5L,
                    messageScale         = prefs[KEY_MESSAGE_SCALE]        ?: 1.0f
                )
                val typefaceStyle = when {
                    newSettings.fontWeight == TextWeight.BOLD && newSettings.isItalic -> Typeface.BOLD_ITALIC
                    newSettings.fontWeight == TextWeight.BOLD                         -> Typeface.BOLD
                    newSettings.isItalic                                              -> Typeface.ITALIC
                    else                                                              -> Typeface.NORMAL
                }
                val newTypeface = newSettings.fontFamily.toTypeface(this@WidgetUpdateService, typefaceStyle)
                val scaledDensity = resources.displayMetrics.scaledDensity
                val manager = AppWidgetManager.getInstance(this@WidgetUpdateService)
                val h = manager.getAppWidgetOptions(appWidgetId)
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44)
                val hasDate = newSettings.datePattern.isNotEmpty() || !newSettings.showTime
                val hasMessage = newSettings.showTaskName || newSettings.showCountdown || newSettings.showNextAlarm
                val (clockSp, dateSp, messageSp) = calcFontSizes(
                    widgetHeightDp = h,
                    balance = newSettings.clockDateBalance,
                    fontScale = newSettings.fontScale,
                    showClock = newSettings.showTime,
                    showDate = hasDate,
                    showMessage = hasMessage,
                    messageScale = newSettings.messageScale,
                    use24Hour = newSettings.use24Hour,
                    amPmScale = newSettings.amPmScale
                )
                val prevShowSeconds = cachedSettingsMap[appWidgetId]?.showSeconds ?: false
                val isFirstLoad = !cachedClockPxMap.containsKey(appWidgetId)
                cachedTypefaceMap[appWidgetId]  = newTypeface
                cachedClockPxMap[appWidgetId]   = clockSp * scaledDensity
                cachedDatePxMap[appWidgetId]    = dateSp  * scaledDensity
                cachedMessagePxMap[appWidgetId] = messageSp * scaledDensity
                cachedBgBitmapMap[appWidgetId]  = buildBackgroundBitmap(this@WidgetUpdateService, newSettings)
                cachedSettingsMap[appWidgetId]  = newSettings
                fullRedrawNeededMap[appWidgetId] = true
                if (isFirstLoad || (newSettings.showSeconds && !prevShowSeconds)) {
                    startTicking()
                }
                updateWidgets()
            }
        }
    }

    private fun startTaskObserver() {
        serviceScope.launch {
            taskRepository.allTasks.collect { tasks ->
                cachedTasks = tasks
            }
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        fullRedrawNeededMap.keys.forEach { fullRedrawNeededMap[it] = true }
        tickJob = serviceScope.launch {
            val initialDelay = 1000L - (System.currentTimeMillis() % 1000L)
            delay(initialDelay.coerceAtLeast(0L))
            while (isActive) {
                updateWidgets()
                val nextDelay = 1000L - (System.currentTimeMillis() % 1000L)
                val showSeconds = cachedSettingsMap.values.any { it.showSeconds }
                delay(if (showSeconds) nextDelay.coerceAtLeast(1L) else 60_000L)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun updateWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = TickAtWidgetReceiver.loadWidgetIds(this)
        if (ids.isEmpty()) {
            stopSelf()
            return
        }
        val now = java.time.LocalTime.now()
        val enabledTasks = cachedTasks.filter { it.isEnabled }
        val timeblocks = enabledTasks.filter { it.taskType == TaskType.TIMEBLOCK }
        val activeBlock = timeblocks.firstOrNull { now >= it.startTime && now < it.endTime }

        for (id in ids) {
            val settings = cachedSettingsMap[id] ?: AppSettings()
            val typeface  = cachedTypefaceMap[id]  ?: Typeface.DEFAULT
            val clockPx   = cachedClockPxMap[id]   ?: 0f
            val datePx    = cachedDatePxMap[id]     ?: 0f
            val messagePx = cachedMessagePxMap[id]  ?: 0f
            val bgBitmap  = cachedBgBitmapMap[id]
            val needFullRedraw = fullRedrawNeededMap[id] ?: true

            val displayText: String = if (activeBlock != null && settings.showTaskName) {
                val fmt = if (settings.use24Hour) "H:mm" else "h:mm"
                val sf = java.time.format.DateTimeFormatter.ofPattern(fmt)
                "${activeBlock.name} ${activeBlock.startTime.format(sf)}〜${activeBlock.endTime.format(sf)}"
            } else if (activeBlock == null && settings.showCountdown) {
                val nextBlock = timeblocks.filter { it.startTime > now }.minByOrNull { it.startTime }
                if (nextBlock != null) {
                    val minutes = java.time.Duration.between(now, nextBlock.startTime).toMinutes()
                    when {
                        minutes >= 60 -> "${nextBlock.name}まで あと${minutes / 60}時間${minutes % 60}分"
                        minutes >= 1  -> "${nextBlock.name}まで あと${minutes}分"
                        else          -> "${nextBlock.name}まで あと1分未満"
                    }
                } else if (settings.showNextAlarm) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val nextAlarm = alarmManager.nextAlarmClock
                    android.util.Log.d("TickAtAlarm", "CHECK1: nextAlarm=$nextAlarm showIntent=${nextAlarm?.showIntent} creatorPkg=${nextAlarm?.showIntent?.creatorPackage} triggerTime=${nextAlarm?.triggerTime}")
                    val creatorPkg = nextAlarm?.showIntent?.creatorPackage
                    val isXiaomiCalendar = creatorPkg == "com.xiaomi.calendar"
                    android.util.Log.d("TickAtAlarm", "FILTER: showIntent=${nextAlarm?.showIntent != null} isXiaomi=$isXiaomiCalendar")
                    if (nextAlarm?.showIntent != null && !isXiaomiCalendar) {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                        val isSystemAlarm = cal.get(java.util.Calendar.HOUR_OF_DAY) == 0
                                && cal.get(java.util.Calendar.MINUTE) == 0
                                && cal.get(java.util.Calendar.SECOND) == 0
                                && cal.get(java.util.Calendar.MILLISECOND) == 0
                        android.util.Log.d("TickAtAlarm", "ALARM_TIME: h=${cal.get(java.util.Calendar.HOUR_OF_DAY)} m=${cal.get(java.util.Calendar.MINUTE)} isSystemAlarm=$isSystemAlarm use24h=${settings.use24Hour}")
                        if (!isSystemAlarm) {
                            if (settings.use24Hour) {
                                val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                                val m = cal.get(java.util.Calendar.MINUTE)
                                "⏰ %d:%02d".format(h, m)
                            } else {
                                val h = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
                                val m = cal.get(java.util.Calendar.MINUTE)
                                val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "午前" else "午後"
                                "⏰ $amPm$h:%02d".format(m)
                            }
                        } else ""
                    } else ""
                } else ""
            } else if (settings.showNextAlarm) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val nextAlarm = alarmManager.nextAlarmClock
                val creatorPkg = nextAlarm?.showIntent?.creatorPackage
                val isXiaomiCalendar = creatorPkg == "com.xiaomi.calendar"
                if (nextAlarm?.showIntent != null && !isXiaomiCalendar) {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                    val isSystemAlarm = cal.get(java.util.Calendar.HOUR_OF_DAY) == 0
                            && cal.get(java.util.Calendar.MINUTE) == 0
                            && cal.get(java.util.Calendar.SECOND) == 0
                            && cal.get(java.util.Calendar.MILLISECOND) == 0
                    if (!isSystemAlarm) {
                        if (settings.use24Hour) {
                            val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val m = cal.get(java.util.Calendar.MINUTE)
                            "⏰ %d:%02d".format(h, m)
                        } else {
                            val h = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
                            val m = cal.get(java.util.Calendar.MINUTE)
                            val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "午前" else "午後"
                            "⏰ $amPm$h:%02d".format(m)
                        }
                    } else ""
                } else ""
            } else ""

            val resolvedDisplayText = if (displayText.isEmpty()) {
                val tomorrowOnce = enabledTasks.filter {
                    it.repeat == RepeatType.ONCE && it.startTime <= now
                }.minByOrNull { it.startTime }
                if (tomorrowOnce != null) {
                    val fmt = if (settings.use24Hour) "H:mm" else "h:mm"
                    val sf = java.time.format.DateTimeFormatter.ofPattern(fmt)
                    "明日 ${tomorrowOnce.startTime.format(sf)} ${tomorrowOnce.name}"
                } else ""
            } else displayText

            val opts = manager.getAppWidgetOptions(id)
            val h    = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44)
            val views = if (needFullRedraw) {
                TickAtWidget.buildViews(this, settings, h)
            } else {
                buildTimeOnlyViews(this, settings, typeface, clockPx, datePx, bgBitmap)
            }
            if (resolvedDisplayText.isNotEmpty()) {
                android.util.Log.d("TickAtAlarm", "RENDER: text=$resolvedDisplayText messagePx=$messagePx messageColor=${settings.messageTextColor}")
                val msgBitmap = buildTextBitmap(resolvedDisplayText, messagePx, settings.messageTextColor.toInt(), typeface, settings.showTextShadow)
                views.setImageViewBitmap(R.id.widget_task_name, msgBitmap)
                views.setViewVisibility(R.id.widget_task_name, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_task_name, View.GONE)
            }
            manager.updateAppWidget(id, views)
            fullRedrawNeededMap[id] = false
        }
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