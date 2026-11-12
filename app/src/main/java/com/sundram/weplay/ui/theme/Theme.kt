package com.sundram.weplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightPalette = WePlayPalette(
    page = Color(0xFFFFFFFF),
    bgSoft = Color(0xFFF6F9FF),
    bgSection = Color(0xFFF3F5F9),
    chipBg = Color(0xFFF4F5F9),
    card = Color(0xFFFFFFFF),
    divider = Color(0xFFEEF0F5),
    stroke = Color(0xFFE7EAF2),
    textMain = Color(0xFF252A46),
    textBody = Color(0xFF4A5068),
    textMuted = Color(0xFF9AA3B7),
    overlaySoft = Color(0xB3F4F6FB),
    authSky = Color(0xFFE4F7FF),
    wallBlue = Color(0xFFEDF5FF),
    wallPurple = Color(0xFFF6EDFF),
    wallMint = Color(0xFFE8FAF2),
    accentInk = Color(0xFF007E9E),
)

private val DarkPalette = WePlayPalette(
    page = Color(0xFF0F1420),
    bgSoft = Color(0xFF151B2A),
    bgSection = Color(0xFF1A2132),
    chipBg = Color(0xFF222A3D),
    card = Color(0xFF171E2C),
    divider = Color(0xFF252D40),
    stroke = Color(0xFF2C3550),
    textMain = Color(0xFFF2F5FF),
    textBody = Color(0xFFC3CBDE),
    textMuted = Color(0xFF8D98B2),
    overlaySoft = Color(0xB30B0F19),
    authSky = Color(0xFF10283A),
    wallBlue = Color(0xFF122234),
    wallPurple = Color(0xFF1B1630),
    wallMint = Color(0xFF10241F),
    accentInk = Color(0xFF66D9FF),
)

private val LightScheme = lightColorScheme(
    primary = CyanBrand,
    onPrimary = OnBrand,
    secondary = PinkGift,
    onSecondary = OnBrand,
    background = LightPalette.page,
    onBackground = LightPalette.textMain,
    surface = LightPalette.card,
    onSurface = LightPalette.textMain,
    surfaceVariant = LightPalette.bgSection,
    onSurfaceVariant = LightPalette.textBody,
    surfaceBright = LightPalette.page,
    surfaceDim = LightPalette.bgSection,
    surfaceContainerLowest = LightPalette.page,
    surfaceContainerLow = LightPalette.bgSoft,
    surfaceContainer = LightPalette.bgSection,
    surfaceContainerHigh = LightPalette.bgSection,
    surfaceContainerHighest = LightPalette.chipBg,
    outline = LightPalette.stroke,
    outlineVariant = LightPalette.divider,
    error = WarnOrange,
)

private val DarkScheme = darkColorScheme(
    primary = CyanBrand,
    onPrimary = OnBrand,
    secondary = PinkGift,
    onSecondary = OnBrand,
    background = DarkPalette.page,
    onBackground = DarkPalette.textMain,
    surface = DarkPalette.card,
    onSurface = DarkPalette.textMain,
    surfaceVariant = DarkPalette.bgSection,
    onSurfaceVariant = DarkPalette.textBody,
    surfaceBright = DarkPalette.bgSoft,
    surfaceDim = DarkPalette.page,
    surfaceContainerLowest = DarkPalette.page,
    surfaceContainerLow = DarkPalette.bgSoft,
    surfaceContainer = DarkPalette.bgSection,
    surfaceContainerHigh = DarkPalette.bgSection,
    surfaceContainerHighest = DarkPalette.chipBg,
    outline = DarkPalette.stroke,
    outlineVariant = DarkPalette.divider,
    error = WarnOrange,
)

internal val LocalWePlayPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun WePlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWePlayPalette provides if (darkTheme) DarkPalette else LightPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content,
        )
    }
}
