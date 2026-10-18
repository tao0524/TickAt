package com.tao0524.tickat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tao0524.tickat.ui.screen.settings.AppSettings
import com.tao0524.tickat.ui.screen.settings.ThemeMode

private fun blendToWhite(base: Color, ratio: Float): Color = Color(
    red   = (base.red   + (1f - base.red)   * ratio).coerceIn(0f, 1f),
    green = (base.green + (1f - base.green) * ratio).coerceIn(0f, 1f),
    blue  = (base.blue  + (1f - base.blue)  * ratio).coerceIn(0f, 1f),
    alpha = 1f
)

@Composable
fun TickAtTheme(
    settings: AppSettings = AppSettings(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK  -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // 「システム」のときは、スマホ本体が壁紙などから作った色（ダイナミックカラー）を使う。
    // ダイナミックカラーは Android 12（API 31）以上だけ。それより前は Color.kt の配色を使う。
    val context = LocalContext.current
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isSystemMode = themeMode == ThemeMode.SYSTEM

    val colorScheme = if (isSystemMode && dynamicColor && isDark) {
        dynamicDarkColorScheme(context)
    } else if (isSystemMode && dynamicColor) {
        dynamicLightColorScheme(context)
    } else if (isSystemMode && isDark) {
        // Android 11 以前の「システム」でスマホがダークのとき
        darkColorScheme(
            primary          = TickAtPrimary,
            onPrimary        = TickAtOnPrimary,
            background       = TickAtBackground,
            onBackground     = TickAtOnBackground,
            surface          = TickAtSurface,
            onSurface        = TickAtOnSurface,
            surfaceVariant   = TickAtSurfaceVariant,
            onSurfaceVariant = TickAtOnSurfaceVar,
            error            = TickAtError,
            outline          = TickAtOutline
        )
    } else if (isDark) {
        val bg   = Color(settings.bgColor)
        val text = Color(settings.textColor)
        darkColorScheme(
            primary          = TickAtPrimary,
            onPrimary        = TickAtOnPrimary,
            background       = bg,
            onBackground     = text,
            surface          = blendToWhite(bg, 0.06f),
            onSurface        = text,
            surfaceVariant   = blendToWhite(bg, 0.12f),
            onSurfaceVariant = Color(0xFF9E9E9E),
            error            = Color(0xFFCF6679),
            outline          = Color(0xFF49454F)
        )
    } else {
        lightColorScheme(
            primary          = TickAtPrimary,
            onPrimary        = TickAtOnPrimary,
            background       = TickAtLightBackground,
            onBackground     = TickAtLightOnBackground,
            surface          = TickAtLightSurface,
            onSurface        = TickAtLightOnBackground,
            surfaceVariant   = TickAtLightSurfaceVariant,
            onSurfaceVariant = TickAtLightOnSurfaceVar,
            error            = TickAtLightError,
            outline          = TickAtLightOutline
        )
    }

    // ステータスバー・ナビゲーションバーのアイコン色を背景の明るさに合わせる
    // （WCAGのコントラスト比で黒と白が同じになる明るさ ≒ 0.179 を境目にする）
    val view = LocalView.current
    if (!view.isInEditMode) {
        val useDarkIcons = colorScheme.background.luminance() > 0.179f
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = useDarkIcons
            controller.isAppearanceLightNavigationBars = useDarkIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}