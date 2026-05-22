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
import com.tao0524.tickat.ui.screen.settings.WidgetSize
import com.tao0524.tickat.ui.screen.settings.CornerStyle
import com.tao0524.tickat.ui.screen.settings.TextWeight

object TickAtWidget {

    fun buildViews(context: Context, settings: AppSettings): RemoteViews {
        val layoutRes = if (settings.fontWeight == TextWeight.BOLD) {
            R.layout.widget_tickat_bold
        } else {
            R.layout.widget_tickat
        }
        val views = RemoteViews(context.packageName, layoutRes)

        // 背景（グラデーション/ソリッドカラー 統一Bitmap方式）
        views.setImageViewBitmap(R.id.widget_bg, buildBackgroundBitmap(context, settings))

        // テキスト色
        views.setTextColor(R.id.widget_time, settings.textColor.toInt())
        views.setTextColor(R.id.widget_date, settings.dateTextColor.toInt())

        // フォントサイズ
        val fontSize = when (settings.widgetSize) {
            WidgetSize.S -> 15f
            WidgetSize.M -> 20f
            WidgetSize.L -> 26f
        }
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, fontSize)

        // 時刻フォーマット（秒数・12h/24h の組み合わせ）
        val format = when {
            settings.use24Hour  && settings.showSeconds  -> "HH:mm:ss"
            settings.use24Hour  && !settings.showSeconds -> "HH:mm"
            !settings.use24Hour && settings.showSeconds  -> "h:mm:ss a"
            else                                         -> "h:mm a"
        }
        views.setCharSequence(R.id.widget_time, "setFormat24Hour", format)
        views.setCharSequence(R.id.widget_time, "setFormat12Hour", format)

        // 日付の表示/非表示
        views.setViewVisibility(
            R.id.widget_date,
            if (settings.showDate) View.VISIBLE else View.GONE
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

private fun buildBackgroundBitmap(context: Context, settings: AppSettings): Bitmap {
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
            val cx     = w / 2f
            val cy     = h / 2f
            val radius = maxOf(w.toFloat(), h.toFloat()) / 2f
            paint.shader = if (use3)
                RadialGradient(cx, cy, radius, intArrayOf(startColor, midColor, endColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            else
                RadialGradient(cx, cy, radius, startColor, endColor, Shader.TileMode.CLAMP)
        } else {
            paint.shader = when (settings.gradientDirection) {
                GradientDirection.HORIZONTAL -> if (use3)
                    LinearGradient(0f, h / 2f, w.toFloat(), h / 2f, intArrayOf(startColor, midColor, endColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                else
                    LinearGradient(0f, h / 2f, w.toFloat(), h / 2f, startColor, endColor, Shader.TileMode.CLAMP)
                GradientDirection.VERTICAL -> if (use3)
                    LinearGradient(w / 2f, 0f, w / 2f, h.toFloat(), intArrayOf(startColor, midColor, endColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                else
                    LinearGradient(w / 2f, 0f, w / 2f, h.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
                else -> if (use3)
                    LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(startColor, midColor, endColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                else
                    LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
            }
        }
        paint.alpha = a
    } else {
        val rgb = (settings.bgColor and 0xFFFFFFFFL).toInt() and 0x00FFFFFF
        paint.color = (a shl 24) or rgb
    }
    canvas.drawRoundRect(rect, r, r, paint)
    return bitmap
}