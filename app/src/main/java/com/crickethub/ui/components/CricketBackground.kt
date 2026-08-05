package com.crickethub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Cricket words that flow bottom → top, continuously, never empty ──────────
// Single animation driver → all positioning via graphicsLayer (GPU, zero recomposition)
// Dark: grey/ivory words · Light: charcoal words

private val CRICKET_WORDS = listOf(
    "Yorker", "SIX!", "Powerplay", "LBW", "Hat-Trick",
    "Bouncer", "Cover Drive", "Googly", "Super Over", "No Ball",
    "Reverse Swing", "FOUR!", "Maiden", "Spin", "Stumped",
    "Caught", "Century", "Duck", "Wide", "Run Out"
)

// Each word slot: fixed x%, staggered start phase, font size
private data class WordSlot(
    val text: String,
    val xFrac: Float,   // 0..1 horizontal position
    val phase: Float,   // 0..1 stagger offset so words are evenly spread
    val size: Int        // font size sp
)

// 14 word slots spread across the screen — enough that it's never empty
private val WORD_SLOTS = listOf(
    WordSlot("Yorker",        0.04f, 0.00f, 12),
    WordSlot("SIX!",          0.75f, 0.07f, 14),
    WordSlot("Powerplay",     0.35f, 0.14f, 11),
    WordSlot("LBW",           0.62f, 0.21f, 10),
    WordSlot("Hat-Trick",     0.18f, 0.28f, 13),
    WordSlot("Bouncer",       0.50f, 0.35f, 11),
    WordSlot("Cover Drive",   0.08f, 0.42f, 12),
    WordSlot("Googly",        0.82f, 0.49f, 10),
    WordSlot("Super Over",    0.28f, 0.56f, 11),
    WordSlot("No Ball",       0.68f, 0.63f, 10),
    WordSlot("Reverse Swing", 0.12f, 0.70f, 11),
    WordSlot("FOUR!",         0.55f, 0.77f, 13),
    WordSlot("Maiden",        0.40f, 0.84f, 10),
    WordSlot("Wide",          0.78f, 0.91f, 11),
)

@Composable
fun CricketAnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF7F3EA)
    Box(modifier = modifier.background(bgColor)) {
        FloatingWords(isDark)
        content()
    }
}

// Backward compat — screens that call this for just the background color
@Composable
fun CricketBackgroundDecor(isDark: Boolean = isSystemInDarkTheme()) {
    // no-op: bg color is set by the screen itself
}

@Composable
private fun FloatingWords(isDark: Boolean) {
    // Word color — visible but not distracting
    val wordColor = if (isDark)
        Color(0xFFD0D4DC).copy(alpha = 0.13f)   // grey/ivory on dark
    else
        Color(0xFF2B2620).copy(alpha = 0.10f)    // charcoal on light

    // Single animation: 0→1 over 45s, loops forever
    // Each word uses this + its phase offset to compute its Y position
    // 45s cycle = gentle drift, not distracting
    val inf = rememberInfiniteTransition(label = "words")
    val progress by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 65_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow"
    )

    // Render all word slots — pure graphicsLayer, zero recomposition
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        // Extra overflow so words smoothly exit top and enter from bottom
        val overflow = heightPx * 0.15f

        WORD_SLOTS.forEach { slot ->
            // Y position: bottom → top, wrapping around
            // (progress + phase) mod 1.0 gives current normalized position
            // Map: 0.0 = just below bottom, 1.0 = just above top
            val normalizedY = (progress + slot.phase) % 1f
            // Map to pixel: 1.0→ bottom+overflow, 0.0→ -overflow (above top)
            val yPx = (1f - normalizedY) * (heightPx + overflow * 2) - overflow
            val xPx = slot.xFrac * widthPx

            Text(
                text = slot.text,
                color = wordColor,
                fontSize = slot.size.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = xPx
                    translationY = yPx
                }
            )
        }
    }
}