package com.crickethub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Used by Matches, Teams, Tournaments, Career screens
@Composable
fun CricketAnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF7F3EA)
    Box(modifier = modifier.background(bgColor)) {
        AnimatedBgLayer(isDark)
        content()
    }
}

// Used by Scoring, LiveScorecard, PostMatch, Analytics, CreateMatch, Toss, PlayingXI
// Does NOT block touches — just shows background color
@Composable
fun CricketBackgroundDecor(isDark: Boolean = isSystemInDarkTheme()) {
    // intentionally empty — just background color is set in the screen itself
}

// ── Floating cricket words — drift animation matching the HTML mockup ────────
// Words float gently: translateY oscillation, slight rotation, scale pulse.
// High opacity for strong presence on both themes.
@Composable
private fun AnimatedBgLayer(isDark: Boolean) {
    val wordColor = if (isDark)
        Color(0xFFE4E7ED).copy(alpha = 0.08f)   // bright platinum, subtle
    else
        Color(0xFF3D4759).copy(alpha = 0.06f)   // dark slate, subtle

    val inf = rememberInfiniteTransition(label = "bg")

    // Drift offsets — each word gets a unique vertical oscillation
    val d1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "d1")
    val d2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "d2")
    val d3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "d3")
    val d4 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "d4")
    val d5 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "d5")
    val d6 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "d6")

    // Cricket ball rotation
    val ballRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "br")

    data class FloatWord(val text: String, val xFrac: Float, val yFrac: Float, val size: Int, val drift: Float)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = maxWidth; val H = maxHeight

        val words = listOf(
            FloatWord("Yorker",        0.06f, 0.05f, 12, d1),
            FloatWord("SIX!",          0.72f, 0.08f, 14, d2),
            FloatWord("Powerplay",     0.44f, 0.15f, 11, d3),
            FloatWord("LBW",           0.16f, 0.22f, 10, d4),
            FloatWord("Hat-Trick",     0.60f, 0.28f, 13, d5),
            FloatWord("Bouncer",       0.28f, 0.35f, 11, d6),
            FloatWord("Cover Drive",   0.52f, 0.42f, 12, d1),
            FloatWord("Googly",        0.80f, 0.48f, 10, d2),
            FloatWord("Super Over",    0.04f, 0.55f, 11, d3),
            FloatWord("No Ball",       0.38f, 0.62f, 10, d4),
            FloatWord("Reverse Swing", 0.22f, 0.72f, 11, d5),
            FloatWord("FOUR!",         0.66f, 0.78f, 13, d6),
        )

        words.forEach { w ->
            // Drift: translateY oscillates ±14dp, slight rotation ±4°, scale 1.0–1.05
            val translateY = -14f + 28f * w.drift
            val rotation = -4f + 8f * w.drift
            val scale = 1f + 0.05f * w.drift

            Text(
                text = w.text,
                color = wordColor,
                fontSize = w.size.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .offset(x = W * w.xFrac, y = H * w.yFrac)
                    .graphicsLayer {
                        this.translationY = translateY
                        this.rotationZ = rotation
                        this.scaleX = scale
                        this.scaleY = scale
                    }
            )
        }

        // Cricket ball — top right
        Box(modifier = Modifier.size(52.dp).offset(x = W * 0.80f, y = H * 0.04f)) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color(0xFFCC2200), Color(0xFF8B0000), Color(0xFF5C0000))), CircleShape))
            Box(modifier = Modifier.size(14.dp).offset(8.dp, 7.dp)
                .background(Brush.radialGradient(listOf(Color.White.copy(0.4f), Color.Transparent)), CircleShape))
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().rotate(ballRot)) {
                val s = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)
                drawArc(Color(0xFFEEEEEE), -30f, 60f, false,
                    androidx.compose.ui.geometry.Offset(4f,4f),
                    androidx.compose.ui.geometry.Size(size.width-8f,size.height-8f), style = s)
                drawArc(Color(0xFFEEEEEE), 150f, 60f, false,
                    androidx.compose.ui.geometry.Offset(4f,4f),
                    androidx.compose.ui.geometry.Size(size.width-8f,size.height-8f), style = s)
            }
        }

        // Small green ball — bottom left
        val green = Color(0xFF34D399)
        Box(modifier = Modifier.size(30.dp).offset(x = W * 0.04f, y = H * 0.75f).rotate(-ballRot)) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFFA7F3D0), green)), CircleShape))
        }

        // Stumps — right side
        Row(modifier = Modifier.offset(x = W * 0.82f, y = H * 0.55f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                Box(modifier = Modifier.width(4.dp).height(40.dp).background(
                    Brush.verticalGradient(listOf(
                        if (isDark) Color(0xFFF2F2F0).copy(0.15f) else Color(0xFF2B2620).copy(0.10f),
                        Color.Transparent)), RoundedCornerShape(topStart=2.dp, topEnd=2.dp)))
            }
        }
    }
}