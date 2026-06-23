package com.tao0524.tickat.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import com.tao0524.tickat.ui.screen.settings.BackgroundType
import com.tao0524.tickat.ui.screen.settings.GradientDirection
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.tao0524.tickat.MainActivity
import com.tao0524.tickat.R
import com.tao0524.tickat.ui.screen.settings.AppSettings
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.tao0524.tickat.ui.screen.settings.GradientCenter
import com.tao0524.tickat.ui.screen.settings.TextWeight
import com.tao0524.tickat.ui.screen.settings.WidgetFont
import android.graphics.Canvas
import android.graphics.Typeface
import com.tao0524.tickat.ui.screen.settings.AmPmPosition
import com.tao0524.tickat.ui.screen.settings.AmPmLabel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TickAtWidget {

    fun buildViews(context: Context, settings: AppSettings, widgetHeightDp: Int = 44): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_tickat_base)

        // Typeface
        val typefaceStyle = when {
            settings.fontWeight == TextWeight.BOLD && settings.isItalic -> Typeface.BOLD_ITALIC
            settings.fontWeight == TextWeight.BOLD                      -> Typeface.BOLD
            settings.isItalic                                           -> Typeface.ITALIC
            else                                                        -> Typeface.NORMAL
        }
        val typeface = when (settings.fontFamily) {
            WidgetFont.THIN      -> Typeface.create("sans-serif-thin", typefaceStyle)
            WidgetFont.LIGHT     -> Typeface.create("sans-serif-light", typefaceStyle)
            WidgetFont.MEDIUM    -> Typeface.create("sans-serif-medium", typefaceStyle)
            WidgetFont.BLACK     -> Typeface.create("sans-serif-black", typefaceStyle)
            WidgetFont.CONDENSED -> Typeface.create("sans-serif-condensed", typefaceStyle)
            WidgetFont.SERIF     -> Typeface.create(Typeface.SERIF, typefaceStyle)
            WidgetFont.MONO      -> Typeface.create(Typeface.MONOSPACE, typefaceStyle)
            else                 -> Typeface.create(Typeface.DEFAULT, typefaceStyle)
        }

        // フォントサイズ（px変換）
        val hasDate = settings.datePattern.isNotEmpty() || !settings.showTime
        val hasMessage = settings.showTaskName || settings.showCountdown || settings.showNextAlarm
        val (clockSp, dateSp, _) = calcFontSizes(
            widgetHeightDp = widgetHeightDp,
            balance = settings.clockDateBalance,
            fontScale = settings.fontScale,
            showClock = settings.showTime,
            showDate = hasDate,
            showMessage = hasMessage,
            messageScale = settings.messageScale,
            use24Hour = settings.use24Hour,
            amPmScale = settings.amPmScale
        )
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        val clockPx = clockSp * scaledDensity
        val datePx  = dateSp  * scaledDensity

        // 背景
        views.setImageViewBitmap(R.id.widget_bg, buildBackgroundBitmap(context, settings))

        // 時刻・日付文字列
        val now        = Calendar.getInstance().also {
            it.add(Calendar.MILLISECOND, settings.timeOffset)
        }
        val timeLocale = if (settings.amPmLabel == AmPmLabel.ENGLISH) Locale.ENGLISH else Locale.JAPANESE
        val timeBitmap = if (settings.use24Hour) {
            val fmt = if (settings.showSeconds) "HH:mm:ss" else "HH:mm"
            buildTextBitmap(SimpleDateFormat(fmt, Locale.getDefault()).format(now.time), clockPx, settings.textColor.toInt(), typeface, settings.showTextShadow)
        } else {
            val fmt      = if (settings.showSeconds) "h:mm:ss" else "h:mm"
            val timeOnly = SimpleDateFormat(fmt, Locale.getDefault()).format(now.time)
            val amPmStr  = SimpleDateFormat("a", timeLocale).format(now.time)
            buildTimeWithAmPmBitmap(timeOnly, amPmStr, clockPx, settings.textColor.toInt(), typeface, settings.showTextShadow, settings.amPmPosition, settings.amPmScale, settings.amPmColor.toInt())
        }
        val dateText = if (settings.datePattern.isNotEmpty())
            SimpleDateFormat(settings.datePattern, Locale.getDefault()).format(now.time)
        else ""
        val fallbackDate = if (dateText.isEmpty() && !settings.showTime)
            SimpleDateFormat("M/d", Locale.getDefault()).format(now.time) else ""
        val displayDate = dateText.ifEmpty { fallbackDate }

        // テキストBitmap
        views.setImageViewBitmap(R.id.widget_time_img, timeBitmap)
        if (displayDate.isNotEmpty()) {
            views.setImageViewBitmap(
                R.id.widget_date_img,
                buildTextBitmap(displayDate, datePx, settings.dateTextColor.toInt(), typeface, settings.showTextShadow)
            )
        }

        // 時刻の表示/非表示
        views.setViewVisibility(
            R.id.widget_time_img,
            if (settings.showTime) View.VISIBLE else View.GONE
        )
        // 日付の表示/非表示
        views.setViewVisibility(
            R.id.widget_date_img,
            if (displayDate.isNotEmpty()) View.VISIBLE else View.GONE
        )

        // タップ → ExpandedScreen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_expanded", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        return views
    }
}

internal fun calcFontSizes(
    widgetHeightDp: Int,
    balance: Int,
    fontScale: Float = 1.0f,
    showClock: Boolean = true,
    showDate: Boolean = true,
    showMessage: Boolean = false,
    messageScale: Float = 1.0f,
    use24Hour: Boolean = true,
    amPmScale: Float = 0.55f
): Triple<Int, Int, Int> {
    val h = widgetHeightDp.toFloat()
    val s = fontScale

    // 固定パディングを確保 (全高の20%)
    val totalPadding = h * 0.2f
    val availableSpace = h - totalPadding

    // バランスによるウェイト可変（balance: -10～+10）
    val shift = balance * 0.1f
    val clockWeight = if (showClock) (3.0f - shift).coerceIn(1.5f, 4.5f) else 0f
    val dateWeight = if (showDate) (1.5f + shift).coerceIn(0.5f, 3.0f) else 0f
    val messageWeight = if (showMessage) 1.5f else 0f
    val totalWeight = clockWeight + dateWeight + messageWeight

    if (totalWeight == 0f) return Triple(10, 8, 8)

    // 領域の分配
    var clockBase = (availableSpace * (clockWeight / totalWeight)) * s
    val dateBase = (availableSpace * (dateWeight / totalWeight)) * s
    val messageBase = (availableSpace * (messageWeight / totalWeight)) * s * messageScale

    // 12時間制の補正（AM/PMサイズ変更による時計領域の自動補正）
    if (!use24Hour && showClock) {
        val amPmDelta = amPmScale - 0.55f
        clockBase *= (1.0f - amPmDelta).coerceIn(0.5f, 1.5f)
    }

    return Triple(
        clockBase.roundToInt().coerceIn(if (showClock) 10 else 0, 60),
        dateBase.roundToInt().coerceIn(if (showDate) 8 else 0, 40),
        messageBase.roundToInt().coerceIn(if (showMessage) 8 else 0, 54)
    )
}

internal fun buildTextBitmap(
    text:       String,
    textSizePx: Float,
    textColor:  Int,
    typeface:   Typeface,
    hasShadow:  Boolean
): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.textSize = textSizePx
        this.color    = textColor
        if (hasShadow) setShadowLayer(10f, 8f, 8f, (textColor and 0x00FFFFFF) or 0x99000000.toInt())
    }
    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val textWidth = paint.measureText(text)
    val padX      = 8f
    val shadowPadY = if (hasShadow) 18 else 0 // 影が見切れないための余白
    val bitmapW   = (textWidth + padX * 2).toInt().coerceAtLeast(1)
    val bitmapH   = (bounds.height() + shadowPadY).coerceAtLeast(1)
    val bitmap    = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
    val canvas    = Canvas(bitmap)
    canvas.drawText(text, padX, -bounds.top.toFloat(), paint)
    return bitmap
}

internal fun buildTimeWithAmPmBitmap(
    timeText:    String,
    amPmText:    String,
    clockPx:     Float,
    textColor:   Int,
    typeface:    Typeface,
    hasShadow:   Boolean,
    amPmPosition: AmPmPosition,
    amPmScale:   Float = 0.55f,
    amPmColor:   Int = 0
): Bitmap {
    val padX        = 8f
    val amPmPx      = clockPx * amPmScale
    val gap         = 6f
    val resolvedAmPmColor = if (amPmColor != 0) amPmColor else textColor

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.textSize = clockPx
        this.color    = textColor
        if (hasShadow) setShadowLayer(10f, 8f, 8f, (textColor and 0x00FFFFFF) or 0x99000000.toInt())
    }
    val amPmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.textSize = amPmPx
        this.color    = resolvedAmPmColor
        if (hasShadow) setShadowLayer(6f, 3f, 3f, (resolvedAmPmColor and 0x00FFFFFF) or 0x99000000.toInt())
    }

    val timeBounds = android.graphics.Rect()
    timePaint.getTextBounds(timeText, 0, timeText.length, timeBounds)
    val amPmBounds = android.graphics.Rect()
    amPmPaint.getTextBounds(amPmText, 0, amPmText.length, amPmBounds)

    val timeW  = timePaint.measureText(timeText)
    val amPmW  = amPmPaint.measureText(amPmText)

    val shadowPadY = if (hasShadow) 18 else 0
    val bitmapW = (timeW + amPmW + gap + padX * 2).toInt().coerceAtLeast(1)
    val bitmapH = (maxOf(timeBounds.height(), amPmBounds.height()) + shadowPadY).coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val timeBaseline = (bitmapH - shadowPadY - timeBounds.height()) / 2f - timeBounds.top.toFloat()
    val amPmBaseline = (bitmapH - shadowPadY - amPmBounds.height()) / 2f - amPmBounds.top.toFloat()

    when (amPmPosition) {
        AmPmPosition.AFTER  -> {
            canvas.drawText(timeText, padX, timeBaseline, timePaint)
            canvas.drawText(amPmText, padX + timeW + gap, amPmBaseline, amPmPaint)
        }
        AmPmPosition.BEFORE -> {
            canvas.drawText(amPmText, padX, amPmBaseline, amPmPaint)
            canvas.drawText(timeText, padX + amPmW + gap, timeBaseline, timePaint)
        }
    }

    return bitmap
}

internal fun buildTimeOnlyViews(
    context: Context,
    settings: AppSettings,
    typeface: Typeface,
    clockPx: Float,
    datePx: Float,
    cachedBgBitmap: android.graphics.Bitmap? = null
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_tickat_base)
    val bgBitmap = cachedBgBitmap ?: buildBackgroundBitmap(context, settings)
    views.setImageViewBitmap(R.id.widget_bg, bgBitmap)
    val now = Calendar.getInstance().also {
        it.add(Calendar.MILLISECOND, settings.timeOffset)
    }
    val timeLocale = if (settings.amPmLabel == AmPmLabel.ENGLISH) Locale.ENGLISH else Locale.JAPANESE
    val timeBitmap = if (settings.use24Hour) {
        val fmt = if (settings.showSeconds) "HH:mm:ss" else "HH:mm"
        buildTextBitmap(SimpleDateFormat(fmt, Locale.getDefault()).format(now.time), clockPx, settings.textColor.toInt(), typeface, settings.showTextShadow)
    } else {
        val fmt      = if (settings.showSeconds) "h:mm:ss" else "h:mm"
        val timeOnly = SimpleDateFormat(fmt, Locale.getDefault()).format(now.time)
        val amPmStr  = SimpleDateFormat("a", timeLocale).format(now.time)
        buildTimeWithAmPmBitmap(timeOnly, amPmStr, clockPx, settings.textColor.toInt(), typeface, settings.showTextShadow, settings.amPmPosition, settings.amPmScale, settings.amPmColor.toInt())
    }
    val dateText = if (settings.datePattern.isNotEmpty())
        SimpleDateFormat(settings.datePattern, Locale.getDefault()).format(now.time)
    else ""
    val fallbackDate = if (dateText.isEmpty() && !settings.showTime) SimpleDateFormat("M/d", Locale.getDefault()).format(now.time) else ""
    val displayDate = dateText.ifEmpty { fallbackDate }
    views.setImageViewBitmap(R.id.widget_time_img, timeBitmap)
    if (displayDate.isNotEmpty()) {
        views.setImageViewBitmap(R.id.widget_date_img, buildTextBitmap(displayDate, datePx, settings.dateTextColor.toInt(), typeface, settings.showTextShadow))
    }
    views.setViewVisibility(R.id.widget_time_img, if (settings.showTime) View.VISIBLE else View.GONE)
    views.setViewVisibility(R.id.widget_date_img, if (displayDate.isNotEmpty()) View.VISIBLE else View.GONE)
    return views
}

internal fun buildBackgroundBitmap(context: Context, settings: AppSettings): Bitmap {
    val w = 512
    val h = 200
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    if (settings.bgType == BackgroundType.TRANSPARENT) return bitmap
    val canvas = android.graphics.Canvas(bitmap)
    val r = (h * settings.cornerRadiusRatio).coerceIn(0f, h / 2f)
    val topPad = if (settings.compactBg) 20f else 0f
    val botPad = if (settings.compactBg) 20f else 0f
    val rect = RectF(0f, topPad, w.toFloat(), h.toFloat() - botPad)

    if (settings.bgImageUri.isNotEmpty()) {
        runCatching {
            val uri = Uri.parse(settings.bgImageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val srcBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (srcBitmap != null) {
                val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                canvas.drawRoundRect(rect, r, r, clipPaint)
                val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                }
                val scale = maxOf(w.toFloat() / srcBitmap.width, h.toFloat() / srcBitmap.height)
                val scaledW = (srcBitmap.width * scale).toInt()
                val scaledH = (srcBitmap.height * scale).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, scaledW, scaledH, true)
                val left = (w - scaledW) / 2f
                val top = (h - scaledH) / 2f
                canvas.drawBitmap(scaledBitmap, left, top, xferPaint)
                return bitmap
            }
        }
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val aStart = (settings.bgAlpha            * 255f / 100f + 0.5f).toInt().coerceIn(0, 255)
    val aEnd   = (settings.bgGradientEndAlpha * 255f / 100f + 0.5f).toInt().coerceIn(0, 255)
    val aMid   = (settings.bgColor2Alpha      * 255f / 100f + 0.5f).toInt().coerceIn(0, 255)
    if (settings.bgType == BackgroundType.LINEAR || settings.bgType == BackgroundType.RADIAL) {
        val startColor = ((settings.bgColor       and 0x00FFFFFFL).toInt()) or (aStart shl 24)
        val endColor   = ((settings.bgGradientEnd and 0x00FFFFFFL).toInt()) or (aEnd   shl 24)
        val use3       = settings.gradientColorCount == 3 && settings.bgColor2 != 0L
        val midColor   = ((settings.bgColor2 and 0x00FFFFFFL).toInt()) or (aMid shl 24)
        if (settings.bgType == BackgroundType.RADIAL) {
            val (cx, cy) = when (settings.gradientCenter) {
                GradientCenter.TOP_LEFT      -> 0f to 0f
                GradientCenter.TOP_CENTER    -> w / 2f to 0f
                GradientCenter.TOP_RIGHT     -> w.toFloat() to 0f
                GradientCenter.CENTER_LEFT   -> 0f to h / 2f
                GradientCenter.CENTER        -> w / 2f to h / 2f
                GradientCenter.CENTER_RIGHT  -> w.toFloat() to h / 2f
                GradientCenter.BOTTOM_LEFT   -> 0f to h.toFloat()
                GradientCenter.BOTTOM_CENTER -> w / 2f to h.toFloat()
                GradientCenter.BOTTOM_RIGHT  -> w.toFloat() to h.toFloat()
            }
            val dx     = maxOf(cx, w.toFloat() - cx)
            val dy     = maxOf(cy, h.toFloat() - cy)
            val radius = sqrt(dx * dx + dy * dy)
            paint.shader = if (use3)
                RadialGradient(cx, cy, radius, intArrayOf(startColor, midColor, endColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            else
                RadialGradient(cx, cy, radius, startColor, endColor, Shader.TileMode.CLAMP)
        } else {
            val wf = w.toFloat()
            val hf = h.toFloat()
            val (x0, y0) = when (settings.linearStartPoint) {
                GradientCenter.TOP_LEFT      -> 0f to 0f
                GradientCenter.TOP_CENTER    -> wf / 2f to 0f
                GradientCenter.TOP_RIGHT     -> wf to 0f
                GradientCenter.CENTER_LEFT   -> 0f to hf / 2f
                GradientCenter.CENTER        -> 0f to hf / 2f
                GradientCenter.CENTER_RIGHT  -> wf to hf / 2f
                GradientCenter.BOTTOM_LEFT   -> 0f to hf
                GradientCenter.BOTTOM_CENTER -> wf / 2f to hf
                GradientCenter.BOTTOM_RIGHT  -> wf to hf
            }
            val (x1, y1) = when (settings.linearStartPoint) {
                GradientCenter.TOP_LEFT      -> wf to hf
                GradientCenter.TOP_CENTER    -> wf / 2f to hf
                GradientCenter.TOP_RIGHT     -> 0f to hf
                GradientCenter.CENTER_LEFT   -> wf to hf / 2f
                GradientCenter.CENTER        -> wf to hf / 2f
                GradientCenter.CENTER_RIGHT  -> 0f to hf / 2f
                GradientCenter.BOTTOM_LEFT   -> wf to 0f
                GradientCenter.BOTTOM_CENTER -> wf / 2f to 0f
                GradientCenter.BOTTOM_RIGHT  -> 0f to 0f
            }
            paint.shader = if (use3)
                LinearGradient(x0, y0, x1, y1, intArrayOf(startColor, midColor, endColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            else
                LinearGradient(x0, y0, x1, y1, startColor, endColor, Shader.TileMode.CLAMP)
        }
    } else {
        val rgb = (settings.bgColor and 0xFFFFFFFFL).toInt() and 0x00FFFFFF
        paint.color = (aStart shl 24) or rgb
    }
    canvas.drawRoundRect(rect, r, r, paint)
    return bitmap
}