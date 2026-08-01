package com.crickethub.ui.theme

import androidx.compose.ui.graphics.Color

// ── CricketHub Brand Colors — Charcoal+Platinum / Ivory+Slate ────────────────

// Dark Theme Palette — Charcoal + Platinum
val DarkBackground      = Color(0xFF0A0A0A)   // deep charcoal black
val DarkSurface         = Color(0xFF161616)   // charcoal card
val DarkSurface2        = Color(0xFF1E1E1E)   // slightly lighter surface
val DarkBorder          = Color(0xFF262626)   // subtle grey border

// Light Theme Palette — Ivory + Slate
val LightBackground     = Color(0xFFF7F3EA)   // warm ivory
val LightSurface        = Color(0xFFFFFFFF)   // pure white card
val LightSurface2       = Color(0xFFF0ECE2)   // very light ivory tint
val LightBorder         = Color(0xFFE6DDC8)   // warm tan border

// Accent — Green (action / interactive elements)
val EmeraldPrimary      = Color(0xFF34D399)   // main action accent
val EmeraldDark         = Color(0xFF059669)   // darker variant (light mode actions)
val EmeraldLight        = Color(0xFF6EE7B7)   // lighter variant
val EmeraldMint         = Color(0xFFA7F3D0)   // very light mint

// Platinum / Slate — secondary accent (scores, stats, labels)
val Platinum            = Color(0xFFC4C9D4)   // dark mode secondary accent
val Slate               = Color(0xFF566073)   // light mode secondary accent
val SlateLight          = Color(0xFF8A7C60)   // light mode muted label

// Text Colors
val TextOnDark          = Color(0xFFF2F2F0)   // warm white on dark
val TextOnDarkSub       = Color(0xFFC4C9D4)   // platinum on dark
val TextOnDarkHint      = Color(0xFF5A5A5A)   // very muted on dark
val TextOnLight         = Color(0xFF2B2620)   // warm dark on light
val TextOnLightSub      = Color(0xFF566073)   // slate on light
val TextOnLightHint     = Color(0xFF9A927E)   // hint olive on light

// Semantic Colors
val SuccessGreen        = Color(0xFF2FA57A)
val ErrorRed            = Color(0xFFEF4444)
val WarningAmber        = Color(0xFFF59E0B)
val InfoBlue            = Color(0xFF60A5FA)
val PurpleAccent        = Color(0xFF8B5CF6)

// Live badge / scoring red
val LiveRed             = Color(0xFFFF5470)   // dark mode live badge
val LiveRedLight        = Color(0xFFC23B4F)   // light mode live badge

// Ball Popup Colors — dark
val PopRun              = Color(0xFFC4C9D4)   // platinum — runs
val PopFour             = Color(0xFF2FA57A)   // emerald — four
val PopSix              = Color(0xFFD4C5A0)   // champagne — six
val PopWicket           = Color(0xFFFF5470)   // red — wicket

// Ball Popup Colors — light
val PopRunLight         = Color(0xFF566073)   // slate — runs
val PopFourLight        = Color(0xFF0F6B4C)   // deep emerald — four
val PopSixLight         = Color(0xFFA5622A)   // rose-copper — six
val PopWicketLight      = Color(0xFFC23B4F)   // red — wicket

// Floating words
val FloatWordDark       = Color(0xFFE4E7ED)   // bright platinum on dark
val FloatWordLight      = Color(0xFF3D4759)   // dark slate on light

// Ball chip colors (ball timeline)
val BallFour            = Color(0xFF2FA57A)
val BallSix             = Color(0xFFD4C5A0)
val BallWicket          = Color(0xFFFF5470)
val BallExtra           = Color(0xFFF59E0B)
val BallDot             = Color(0xFF2A2A2A)

// Legacy aliases (for backward compat)
val Purple80            = Color(0xFFD0BCFF)
val PurpleGrey80        = Color(0xFFCCC2DC)
val Pink80              = Color(0xFFEFB8C8)
val Purple40            = Color(0xFF6650a4)
val PurpleGrey40        = Color(0xFF625b71)
val Pink40              = Color(0xFF7D5260)