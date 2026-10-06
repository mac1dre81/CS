package com.keyvault.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightPrimary = Color(0xFF2563EB)   // blue-600
private val LightSecondary = Color(0xFF0F766E)  // teal-700
private val LightTertiary = Color(0xFF7C3AED)   // violet-600
private val DarkPrimary = Color(0xFF60A5FA)
private val DarkSecondary = Color(0xFF2EE5C2)
private val DarkTertiary = Color(0xFFA78BFA)

private val LightPalette: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    inverseOnSurface = Color(0xFFF1F5F9),
    inverseSurface = Color(0xFF0F172A),
    inversePrimary = DarkPrimary,
)

private val DarkPalette: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334159),
    inverseOnSurface = Color(0xFF0F172A),
    inverseSurface = Color(0xFFF1F5F9),
    inversePrimary = LightPrimary,
)

val LocalDynamicColor = staticCompositionLocalOf { false }

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun KeyVaultTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkPalette else LightPalette
    // Respect the system font scale (every TextUnit here is `sp`) and optionally
    // layer an in-app accessibility scale on top (default 1.0f = no override,
    // i.e. "system default"). This is the Material 3 way: don't fight the system,
    // just give users who need a touch more reach an easy lever.
    val typography = if (fontScale == 1f) AppTypography else scaledTypography(AppTypography, fontScale)
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                content = content,
            )
        },
    )
}

/** Material 3 [Typography] with every style's size + line height scaled by [scale]. */
private fun scaledTypography(base: Typography, scale: Float): Typography = Typography(
    displayLarge = base.displayLarge.scaledBy(scale),
    displayMedium = base.displayMedium.scaledBy(scale),
    displaySmall = base.displaySmall.scaledBy(scale),
    headlineLarge = base.headlineLarge.scaledBy(scale),
    headlineMedium = base.headlineMedium.scaledBy(scale),
    headlineSmall = base.headlineSmall.scaledBy(scale),
    titleLarge = base.titleLarge.scaledBy(scale),
    titleMedium = base.titleMedium.scaledBy(scale),
    titleSmall = base.titleSmall.scaledBy(scale),
    bodyLarge = base.bodyLarge.scaledBy(scale),
    bodyMedium = base.bodyMedium.scaledBy(scale),
    bodySmall = base.bodySmall.scaledBy(scale),
    labelLarge = base.labelLarge.scaledBy(scale),
    labelMedium = base.labelMedium.scaledBy(scale),
    labelSmall = base.labelSmall.scaledBy(scale),
)

private fun TextStyle.scaledBy(scale: Float): TextStyle =
    if (scale == 1f) this else copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
    )
