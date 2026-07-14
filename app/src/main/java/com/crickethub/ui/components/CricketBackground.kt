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
    val bgColor = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
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

@Composable
private fun AnimatedBgLayer(isDark: Boolean) {
    val wordColor = if (isDark) Color(0xFF34D399).copy(alpha = 0.10f) else Color(0xFF059669).copy(alpha = 0.07f)
    val green = Color(0xFF34D399)

    val inf = rememberInfiniteTransition(label = "bg")
    val ballRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "br")
    val d1 by inf.animateFloat(1.05f, -0.1f, infiniteRepeatable(tween(35000, easing = LinearEasing)), label = "d1")
    val d2 by inf.animateFloat(0.55f, -0.1f, infiniteRepeatable(tween(45000, easing = LinearEasing)), label = "d2")
    val d3 by inf.animateFloat(0.80f, -0.1f, infiniteRepeatable(tween(55000, easing = LinearEasing)), label = "d3")
    val d4 by inf.animateFloat(0.30f, -0.1f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "d4")
    val d5 by inf.animateFloat(0.65f, -0.1f, infiniteRepeatable(tween(50000, easing = LinearEasing)), label = "d5")
    val d6 by inf.animateFloat(0.15f, -0.1f, infiniteRepeatable(tween(32000, easing = LinearEasing)), label = "d6")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = maxWidth; val H = maxHeight

        listOf(
            "Yorker" to Pair(0.06f, d1), "Six!" to Pair(0.72f, d2),
            "Powerplay" to Pair(0.44f, d3), "LBW" to Pair(0.16f, d4),
            "Hat-Trick" to Pair(0.60f, d5), "Bouncer" to Pair(0.28f, d6),
            "Cover Drive" to Pair(0.52f, d1), "Googly" to Pair(0.80f, d2),
            "Super Over" to Pair(0.04f, d3), "DLS" to Pair(0.38f, d4),
            "No Ball" to Pair(0.22f, d5), "Reverse Swing" to Pair(0.66f, d6),
        ).forEach { (word, pos) ->
            Text(text = word, color = wordColor,
                fontSize = (12 + word.length % 4).sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.offset(x = W * pos.first, y = H * pos.second))
        }

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

        Box(modifier = Modifier.size(30.dp).offset(x = W * 0.04f, y = H * 0.75f).rotate(-ballRot)) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFFA7F3D0), green)), CircleShape))
        }

        Row(modifier = Modifier.offset(x = W * 0.82f, y = H * 0.55f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                Box(modifier = Modifier.width(4.dp).height(40.dp).background(
                    Brush.verticalGradient(listOf(
                        if (isDark) Color(0xFFECFDF5).copy(0.22f) else Color(0xFF064E3B).copy(0.13f),
                        Color.Transparent)), RoundedCornerShape(topStart=2.dp, topEnd=2.dp)))
            }
        }
    }
}