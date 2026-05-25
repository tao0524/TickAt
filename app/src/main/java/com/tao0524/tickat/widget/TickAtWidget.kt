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
import com.tao0524.tickat.ui.screen.settings.CornerStyle
import com.tao0524.tickat.ui.screen.settings.GradientCenter
import com.tao0524.tickat.ui.screen.settings.TextWeight
import com.tao0524.tickat.ui.screen.settings.WidgetFont
object TickAtWidget {

    fun buildViews(context: Context, settings: AppSettings, widgetHeightDp: Int = 44): RemoteViews {
        val layoutRes = when (settings.fontFamily) {
            WidgetFont.SERIF -> when {
                settings.fontWeight == TextWeight.BOLD && settings.isItalic -> R.layout.widget_tickat_serif_bold_italic
                settings.fontWeight == TextWeight.BOLD                      -> R.layout.widget_tickat_serif_bold
                settings.isItalic                                           -> R.layout.widget_tickat_serif_italic
                else                                                        -> R.layout.widget_tickat_serif
            }
            WidgetFont.CONDENSED -> when {
                settings.fontWeight == TextWeight.BOLD && settings.isItalic -> R.layout.widget_tickat_condensed_bold_italic
                settings.fontWeight == TextWeight.BOLD                      -> R.layout.widget_tickat_condensed_bold
                settings.isItalic                                           -> R.layout.widget_tickat_condensed_italic
                else                                                        -> R.layout.widget_tickat_condensed
            }
            WidgetFont.MONO -> when {
                settings.fontWeight == TextWeight.BOLD && settings.isItalic -> R.layout.widget_tickat_mono_bold_italic
                settings.fontWeight == TextWeight.BOLD                      -> R.layout.widget_tickat_mono_bold
                settings.isItalic                                           -> R.layout.widget_tickat_mono_italic
                else                                                        -> R.layout.widget_tickat_mono
            }
            else -> when {
                settings.fontWeight == TextWeight.BOLD && settings.isItalic -> R.layout.widget_tickat_bold_italic
                settings.fontWeight == TextWeight.BOLD                      -> R.layout.widget_tickat_bold
                settings.isItalic                                           -> R.layout.widget_tickat_italic
                else                                                        -> R.layout.widget_tickat
            }
        }
        val views = RemoteViews(context.packageName, layoutRes)
        // 背景（グラデーション/ソリッドカラー 統一Bitmap方式）
        views.setImageViewBitmap(R.id.widget_bg, buildBackgroundBitmap(context, settings))

        // テキスト色
        views.setTextColor(R.id.widget_time, settings.textColor.toInt())
        views.setTextColor(R.id.widget_date, settings.dateTextColor.toInt())

        // フォントサイズ
        val (clockSp, dateSp) = calcFontSizes(widgetHeightDp, settings.clockDateBalance, settings.fontScale)
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, clockSp.toFloat())
        views.setTextViewTextSize(R.id.widget_date, TypedValue.COMPLEX_UNIT_SP, dateSp.toFloat())

        // 時刻フォーマット（秒数・12h/24h の組み合わせ）
        val format = when {
            settings.use24Hour  && settings.showSeconds  -> "HH:mm:ss"
            settings.use24Hour  && !settings.showSeconds -> "HH:mm"
            !settings.use24Hour && settings.showSeconds  -> "h:mm:ss a"
            else                                         -> "h:mm a"
        }
        views.setCharSequence(R.id.widget_time, "setFormat24Hour", format)
        views.setCharSequence(R.id.widget_time, "setFormat12Hour", format)

        // 時刻の表示/非表示
        views.setViewVisibility(
            R.id.widget_time,
            if (settings.showTime) View.VISIBLE else View.GONE
        )
        // 日付の表示/非表示（showTime=OFF のときは強制表示）
        views.setViewVisibility(
            R.id.widget_date,
            if (settings.showDate || !settings.showTime) View.VISIBLE else View.GONE
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

internal fun calcFontSizes(widgetHeightDp: Int, balance: Int, fontScale: Float = 1.0f): Pair<Int, Int> {
    val t       = (balance + 10f) / 20f
    val clockSp = (widgetHeightDp * (0.80f - t * 0.40f) * fontScale).roundToInt().coerceIn(10, 60)
    val dateSp  = (widgetHeightDp * (0.12f + t * 0.38f) * fontScale).roundToInt().coerceIn(8,  40)
    return clockSp to dateSp
}

internal fun buildBackgroundBitmap(context: Context, settings: AppSettings): Bitmap {
    val w = 512
    val h = 200
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    if (settings.bgType == BackgroundType.TRANSPARENT) return bitmap
    val canvas = android.graphics.Canvas(bitmap)
    val r = when (settings.cornerStyle) {
        CornerStyle.PILL    -> h / 2f
        CornerStyle.ROUNDED -> h * 0.27f
        CornerStyle.SOFT    -> h * 0.09f
        CornerStyle.SQUARE  -> 4f
    }
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
    val a = (settings.bgAlpha * 255f / 100f + 0.5f).toInt().coerceIn(0, 255)
    if (settings.bgType == BackgroundType.LINEAR || settings.bgType == BackgroundType.RADIAL) {
        val startColor = (settings.bgColor       and 0xFFFFFFFFL).toInt()
        val endColor   = (settings.bgGradientEnd and 0xFFFFFFFFL).toInt()
        val use3       = settings.gradientColorCount == 3 && settings.bgColor2 != 0L
        val midColor   = (settings.bgColor2 and 0xFFFFFFFFL).toInt()
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
        paint.alpha = a
    } else {
        val rgb = (settings.bgColor and 0xFFFFFFFFL).toInt() and 0x00FFFFFF
        paint.color = (a shl 24) or rgb
    }
    canvas.drawRoundRect(rect, r, r, paint)
    return bitmap
}