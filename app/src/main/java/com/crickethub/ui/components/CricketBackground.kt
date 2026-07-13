package com.crickethub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

@Composable
fun CricketAnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor  = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
    val glowColor = Color(0xFF34D399)
    val wordColor = if (isDark) Color(0xFF34D399).copy(alpha = 0.10f) else Color(0xFF059669).copy(alpha = 0.07f)

    val inf = rememberInfiniteTransition(label = "cricket_bg")

    val ballRot by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "br"
    )
    val drift1 by inf.animateFloat(
        initialValue = 1.05f, targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing)), label = "d1"
    )
    val drift2 by inf.animateFloat(
        initialValue = 0.55f, targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing)), label = "d2"
    )
    val drift3 by inf.animateFloat(
        initialValue = 0.80f, targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing)), label = "d3"
    )
    val drift4 by inf.animateFloat(
        initialValue = 0.30f, targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "d4"
    )
    val drift5 by inf.animateFloat(
        initialValue = 0.65f, targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(50000, easing = LinearEasing)), label = "d5"
    )
    val drift6 by inf.animateFloat(
        initialValue = 0.15f, targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(32000, easing = LinearEasing)), label = "d6"
    )

    Box(modifier = modifier.background(bgColor)) {

        // Glow blobs
        Box(
            modifier = Modifier.size(260.dp).offset(x = (-60).dp, y = (-40).dp)
                .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.10f), Color.Transparent)), CircleShape)
                .blur(44.dp)
        )
        Box(
            modifier = Modifier.size(200.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp)
                .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.07f), Color.Transparent)), CircleShape)
                .blur(36.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxW = maxWidth
            val maxH = maxHeight

            // Drifting cricket words
            listOf(
                "Yorker"        to Pair(0.06f, drift1),
                "Six!"          to Pair(0.72f, drift2),
                "Powerplay"     to Pair(0.44f, drift3),
                "LBW"           to Pair(0.16f, drift4),
                "Hat-Trick"     to Pair(0.60f, drift5),
                "Bouncer"       to Pair(0.28f, drift6),
                "Cover Drive"   to Pair(0.52f, drift1),
                "Googly"        to Pair(0.80f, drift2),
                "Super Over"    to Pair(0.04f, drift3),
                "DLS"           to Pair(0.38f, drift4),
                "No Ball"       to Pair(0.22f, drift5),
                "Reverse Swing" to Pair(0.66f, drift6),
            ).forEach { (word, pos) ->
                val (xFrac, driftY) = pos
                Text(
                    text = word,
                    color = wordColor,
                    fontSize = (12 + word.length % 4).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.offset(x = maxW * xFrac, y = maxH * driftY)
                )
            }

            // Cricket ball top-right
            Box(
                modifier = Modifier.size(52.dp)
                    .offset(x = maxW * 0.80f, y = maxH * 0.04f)
            ) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Brush.radialGradient(
                        colors = listOf(Color(0xFFCC2200), Color(0xFF8B0000), Color(0xFF5C0000))
                    ), CircleShape)
                    .rotate(ballRot))
                Box(modifier = Modifier.size(14.dp).offset(x = 8.dp, y = 7.dp)
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)), CircleShape))
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Color(0xFFEEEEEE), startAngle = -30f, sweepAngle = 60f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
                    drawArc(color = Color(0xFFEEEEEE), startAngle = 150f, sweepAngle = 60f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
                    val cx = size.width / 2f; val cy = size.height / 2f; val r = size.width / 2f - 4f
                    for (i in 0..4) {
                        val angle = Math.toRadians((-15 + i * 7).toDouble())
                        drawCircle(color = Color(0xFFCCCCCC), radius = 1.2f,
                            center = androidx.compose.ui.geometry.Offset((cx + r * 0.85f * Math.cos(angle)).toFloat(), (cy + r * 0.85f * Math.sin(angle)).toFloat()))
                    }
                    for (i in 0..4) {
                        val angle = Math.toRadians((165 + i * 7).toDouble())
                        drawCircle(color = Color(0xFFCCCCCC), radius = 1.2f,
                            center = androidx.compose.ui.geometry.Offset((cx + r * 0.85f * Math.cos(angle)).toFloat(), (cy + r * 0.85f * Math.sin(angle)).toFloat()))
                    }
                }
                Box(modifier = Modifier.fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .run { this })
                // glow border
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color.Transparent, CircleShape)
                    .then(Modifier))
            }

            // Small ball bottom-left
            Box(
                modifier = Modifier.size(32.dp)
                    .offset(x = maxW * 0.04f, y = maxH * 0.72f)
            ) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFFA7F3D0), Color(0xFF34D399))), CircleShape)
                    .rotate(-ballRot * 0.7f))
                Box(modifier = Modifier.size(9.dp).offset(x = 5.dp, y = 4.dp)
                    .background(Color.White.copy(alpha = 0.4f), CircleShape))
            }

            // Stumps right side
            Box(modifier = Modifier.offset(x = maxW * 0.82f, y = maxH * 0.55f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.width(4.dp).height(40.dp)
                            .background(
                                Brush.verticalGradient(listOf(
                                    if (isDark) Color(0xFFECFDF5).copy(alpha = 0.30f) else Color(0xFF064E3B).copy(alpha = 0.20f),
                                    Color.Transparent
                                )), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            ))
                    }
                }
                Box(modifier = Modifier.width(20.dp).height(2.dp)
                    .background(if (isDark) Color(0xFFECFDF5).copy(alpha = 0.25f) else Color(0xFF064E3B).copy(alpha = 0.18f), RoundedCornerShape(1.dp)))
            }
        }

        // Actual screen content on top
        content()
    }
}