package com.tao0524.tickat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tao0524.tickat.ui.screen.settings.AppSettings

private fun blendToWhite(base: Color, ratio: Float): Color = Color(
    red   = (base.red   + (1f - base.red)   * ratio).coerceIn(0f, 1f),
    green = (base.green + (1f - base.green) * ratio).coerceIn(0f, 1f),
    blue  = (base.blue  + (1f - base.blue)  * ratio).coerceIn(0f, 1f),
    alpha = 1f
)

@Composable
fun TickAtTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val colorScheme = if (isDark) {
        val bg   = Color(settings.bgColor)
        val text = Color(settings.textColor)
        darkColorScheme(
            primary          = Color(0xFFBB86FCL),
            onPrimary        = Color(0xFF000000),
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
            primary          = Color(0xFFBB86FCL),
            onPrimary        = Color(0xFF000000),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}