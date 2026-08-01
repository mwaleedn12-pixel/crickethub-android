package com.crickethub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Dark Color Scheme — Charcoal + Platinum ──────────────────────────────────
private val CricketHubDarkColorScheme = darkColorScheme(
    primary          = EmeraldPrimary,
    onPrimary        = Color(0xFF0A0A0A),
    primaryContainer = DarkSurface2,
    onPrimaryContainer = Platinum,
    secondary        = Platinum,
    onSecondary      = Color(0xFF0A0A0A),
    secondaryContainer = DarkSurface,
    onSecondaryContainer = Platinum,
    tertiary         = WarningAmber,
    onTertiary       = Color(0xFF1A0A00),
    background       = DarkBackground,
    onBackground     = TextOnDark,
    surface          = DarkSurface,
    onSurface        = TextOnDark,
    surfaceVariant   = DarkSurface2,
    onSurfaceVariant = TextOnDarkSub,
    outline          = DarkBorder,
    outlineVariant   = Color(0xFF2A2A2A),
    error            = ErrorRed,
    onError          = Color.White,
    inverseSurface   = TextOnDark,
    inverseOnSurface = DarkBackground,
    inversePrimary   = EmeraldDark,
    scrim            = Color(0xFF000000),
)

// ── Light Color Scheme — Ivory + Slate ───────────────────────────────────────
private val CricketHubLightColorScheme = lightColorScheme(
    primary          = EmeraldDark,
    onPrimary        = Color.White,
    primaryContainer = LightSurface2,
    onPrimaryContainer = Slate,
    secondary        = Slate,
    onSecondary      = Color.White,
    secondaryContainer = LightSurface2,
    onSecondaryContainer = Slate,
    tertiary         = WarningAmber,
    onTertiary       = Color.White,
    background       = LightBackground,
    onBackground     = TextOnLight,
    surface          = LightSurface,
    onSurface        = TextOnLight,
    surfaceVariant   = LightSurface2,
    onSurfaceVariant = TextOnLightSub,
    outline          = LightBorder,
    outlineVariant   = Color(0xFFECE3CF),
    error            = ErrorRed,
    onError          = Color.White,
    inverseSurface   = TextOnLight,
    inverseOnSurface = LightBackground,
    inversePrimary   = EmeraldPrimary,
    scrim            = Color(0xFF000000),
)

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun CricketHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CricketHubDarkColorScheme else CricketHubLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}