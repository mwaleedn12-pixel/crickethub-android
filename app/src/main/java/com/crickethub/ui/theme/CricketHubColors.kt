package com.crickethub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── Shorthand aliases — screens import these by name ─────────────────────────
// These are dark-mode defaults; screens that need theme-awareness use CH.*

val BackgroundDark  = Color(0xFF0A0A0A)
val SurfaceCard     = Color(0xFF161616)
val BorderColor     = Color(0xFF262626)
val NeonGreen       = Color(0xFF34D399)   // action accent (buttons, live badges)
val NeonBlue        = Color(0xFF60A5FA)
val TextPrimary     = Color(0xFFF2F2F0)
val TextSecondary   = Color(0xFFC4C9D4)   // platinum
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
val CHBlack         = Color(0xFF0A0A0A)

// ── Theme-aware color object ──────────────────────────────────────────────────
object CH {

    val bg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA)

    val surface: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF161616) else Color(0xFFFFFFFF)

    val surface2: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFF0ECE2)

    val border: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF262626) else Color(0xFFE6DDC8)

    val border2: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF3A3F47) else Color(0xFFD8CFB4)

    val textPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFF2F2F0) else Color(0xFF2B2620)

    val textSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFC4C9D4) else Color(0xFF566073)

    val textHint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5A5A5A) else Color(0xFF9A927E)

    val greenTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1434D399) else Color(0xFFDCFCE7)

    val redTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1AFF5470) else Color(0xFFFEF2F2)

    val amberTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1AF59E0B) else Color(0xFFFEFCE8)

    val blueTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1A60A5FA) else Color(0xFFEFF6FF)

    val navBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA)

    val headerBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF111111) else Color(0xFFF0ECE2)

    val inputBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    val iconTint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFC4C9D4) else Color(0xFF566073)

    val cardBg: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF161616) else Color(0xFFFFFFFF)

    val accent: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF059669)

    // ── Popup colors (ball result) ───────────────────────────────────────────
    val popRun: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFC4C9D4) else Color(0xFF566073)

    val popFour: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2FA57A) else Color(0xFF0F6B4C)

    val popSix: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFD4C5A0) else Color(0xFFA5622A)

    val popWicket: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF5470) else Color(0xFFC23B4F)

    val popDot: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5A5A5A) else Color(0xFF9A927E)

    val popExtra: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFF59E0B) else Color(0xFFF59E0B)

    // ── Live badge ───────────────────────────────────────────────────────────
    val liveRed: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF5470) else Color(0xFFC23B4F)

    // ── Floating words ───────────────────────────────────────────────────────
    val floatWord: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFE4E7ED) else Color(0xFF3D4759)
}