package com.crickethub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── Shorthand aliases — screens import these by name ─────────────────────────
// These replace the old private vals inside each screen file.

val BackgroundDark  = Color(0xFF030F08)
val SurfaceCard     = Color(0xFF0D2018)
val BorderColor     = Color(0xFF1A3828)
val NeonGreen       = Color(0xFF34D399)
val NeonBlue        = Color(0xFF60A5FA)
val TextPrimary     = Color(0xFFECFDF5)
val TextSecondary   = Color(0xFF6EE7B7)
val AmberColor      = Color(0xFFF59E0B)
val PurpleColor     = Color(0xFF8B5CF6)
// Note: ErrorRed is already defined in Color.kt — do NOT redefine here

// Static brand aliases
val CHGreen         = Color(0xFF34D399)
val CHGreenDark     = Color(0xFF059669)
val CHGreenLight    = Color(0xFF6EE7B7)
val CHGreenMint     = Color(0xFFA7F3D0)
val CHRed           = Color(0xFFEF4444)
val CHAmber         = Color(0xFFF59E0B)
val CHBlue          = Color(0xFF60A5FA)
val CHPurple        = Color(0xFF8B5CF6)
val CHBlack         = Color(0xFF031A0E)

// ── Theme-aware color object ──────────────────────────────────────────────────
object CH {

    val bg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF030F08) else Color(0xFFF0FDF8)

    val surface: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF)

    val surface2: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF122A1E) else Color(0xFFE8FDF4)

    val border: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1A3828) else Color(0xFFBBF7D0)

    val border2: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF234D36) else Color(0xFF86EFAC)

    val textPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFECFDF5) else Color(0xFF064E3B)

    val textSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF6B7280)

    val textHint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2D5A3D) else Color(0xFF9CA3AF)

    val greenTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1434D399) else Color(0xFFDCFCE7)

    val redTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1AEF4444) else Color(0xFFFEF2F2)

    val amberTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1AF59E0B) else Color(0xFFFEFCE8)

    val blueTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1A60A5FA) else Color(0xFFEFF6FF)

    val navBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0A1F10) else Color(0xFFFFFFFF)

    val headerBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF071610) else Color(0xFFECFDF5)

    val inputBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1434D399) else Color(0xFFFFFFFF)

    val iconTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF059669)

    val cardBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF)

    val accent: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF059669)
}
