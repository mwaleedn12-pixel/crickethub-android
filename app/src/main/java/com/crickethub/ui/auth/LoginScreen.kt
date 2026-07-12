package com.crickethub.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    val inf = rememberInfiniteTransition(label = "bg")

    // Ball spin
    val ballRot by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ball"
    )
    // Bat swing
    val batRot by inf.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bat"
    )
    // Stump pulse
    val stumpScale by inf.animateFloat(
        initialValue = 1f, targetValue = 0.93f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stump"
    )
    // Drift animations — bottom to top full screen
    val drift1 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing)), label = "d1"
    )
    val drift2 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing), initialStartOffset = StartOffset(2000)), label = "d2"
    )
    val drift3 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing), initialStartOffset = StartOffset(4500)), label = "d3"
    )
    val drift4 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), initialStartOffset = StartOffset(1000)), label = "d4"
    )
    val drift5 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(50000, easing = LinearEasing), initialStartOffset = StartOffset(3500)), label = "d5"
    )
    val drift6 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(32000, easing = LinearEasing), initialStartOffset = StartOffset(5500)), label = "d6"
    )

    val isDark = isSystemInDarkTheme()
    val bgColor    = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
    val cardBg     = if (isDark) Color(0xFF0D2018).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f)
    val borderC    = if (isDark) Color(0xFF34D399).copy(alpha = 0.3f) else Color(0xFF34D399).copy(alpha = 0.4f)
    val textP      = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS      = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val glowColor  = Color(0xFF34D399)
    val wordColor  = if (isDark) Color(0xFF34D399).copy(alpha = 0.22f) else Color(0xFF059669).copy(alpha = 0.16f)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        // ── Glow blobs ────────────────────────────────────────────────────
        Box(
            modifier = Modifier.size(280.dp).offset(x = (-70).dp, y = (-50).dp)
                .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.18f), Color.Transparent)), CircleShape)
                .blur(44.dp)
        )
        Box(
            modifier = Modifier.size(220.dp).align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp)
                .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.12f), Color.Transparent)), CircleShape)
                .blur(36.dp)
        )

        // ── Animated 3D cricket elements + drifting words ─────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxW = maxWidth
            val maxH = maxHeight

            // Drifting cricket words — bottom to top
            val driftWords = listOf(
                "Yorker"        to Pair(0.06f,  drift1),
                "Six!"          to Pair(0.72f,  drift2),
                "Powerplay"     to Pair(0.44f,  drift3),
                "LBW"           to Pair(0.16f,  drift4),
                "Hat-Trick"     to Pair(0.60f,  drift5),
                "Bouncer"       to Pair(0.28f,  drift6),
                "Cover Drive"   to Pair(0.52f,  drift1),
                "Googly"        to Pair(0.80f,  drift2),
                "Super Over"    to Pair(0.04f,  drift3),
                "DLS"           to Pair(0.38f,  drift4),
                "No Ball"       to Pair(0.22f,  drift5),
                "Reverse Swing" to Pair(0.66f,  drift6),
            )
            driftWords.forEach { (word, pos) ->
                val (xFrac, driftY) = pos
                Text(
                    text = word,
                    color = wordColor,
                    fontSize = (13 + word.length % 4).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.offset(x = maxW * xFrac, y = maxH * driftY)
                )
            }

            // ── Realistic Cricket Ball (top right) ───────────────────────
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .offset(x = maxW * 0.74f, y = maxH * 0.04f)
            ) {
                // Main ball body - red/dark red gradient like real cricket ball
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFCC2200),  // bright red center
                                    Color(0xFF8B0000),  // dark red
                                    Color(0xFF5C0000)   // very dark edge
                                ),
                                radius = 120f
                            ),
                            CircleShape
                        )
                )
                // Shine highlight (top-left)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .offset(x = 10.dp, y = 8.dp)
                        .background(
                            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)),
                            CircleShape
                        )
                )
                // Secondary shine
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .offset(x = 20.dp, y = 6.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                )
                // Seam - horizontal arc (white stitching)
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.width / 2f - 4f
                    // Main seam line
                    drawArc(
                        color = Color(0xFFEEEEEE),
                        startAngle = -30f,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    drawArc(
                        color = Color(0xFFEEEEEE),
                        startAngle = 150f,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    // Stitch dots on seam
                    val stitchColor = Color(0xFFCCCCCC)
                    for (i in 0..4) {
                        val angle = Math.toRadians((-15 + i * 7).toDouble())
                        val sx = (cx + r * 0.85f * Math.cos(angle)).toFloat()
                        val sy = (cy + r * 0.85f * Math.sin(angle)).toFloat()
                        drawCircle(color = stitchColor, radius = 1.5f, center = androidx.compose.ui.geometry.Offset(sx, sy))
                    }
                    for (i in 0..4) {
                        val angle = Math.toRadians((165 + i * 7).toDouble())
                        val sx = (cx + r * 0.85f * Math.cos(angle)).toFloat()
                        val sy = (cy + r * 0.85f * Math.sin(angle)).toFloat()
                        drawCircle(color = stitchColor, radius = 1.5f, center = androidx.compose.ui.geometry.Offset(sx, sy))
                    }
                }
                // Green glow border
                Box(
                    modifier = Modifier.fillMaxSize()
                        .border(1.5.dp, glowColor.copy(alpha = 0.5f), CircleShape)
                )
            }

            // ── Second ball (mid screen, left) ────────────────────────────
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .offset(x = maxW * 0.04f, y = maxH * 0.30f)
                    .rotate(-ballRot * 0.8f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color(0xFFA7F3D0), Color(0xFF34D399))), CircleShape)
                        .border(1.5.dp, glowColor, CircleShape)
                )
                Box(modifier = Modifier.width(1.5.dp).height(38.dp).align(Alignment.Center).background(Color.White.copy(alpha = 0.4f)))
                Box(modifier = Modifier.size(10.dp).offset(x = 5.dp, y = 5.dp).background(Color.White.copy(alpha = 0.5f), CircleShape))
            }

            // ── 3D Bat (bottom left) ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(110.dp)
                    .offset(x = maxW * 0.06f, y = maxH * 0.60f)
                    .rotate(batRot - 25f)
            ) {
                // blade
                Box(
                    modifier = Modifier
                        .width(24.dp).height(76.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    if (isDark) Color(0xFFECFDF5) else Color.White,
                                    glowColor.copy(alpha = 0.45f)
                                )
                            )
                        )
                        .border(
                            1.dp, glowColor.copy(alpha = 0.6f),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                        )
                )
                // grain lines
                Column(modifier = Modifier.offset(x = 3.dp, y = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(5) {
                        Box(modifier = Modifier.width(14.dp).height(1.dp).background(glowColor.copy(alpha = 0.25f)))
                    }
                }
                // handle
                Box(
                    modifier = Modifier.width(9.dp).height(36.dp).offset(x = 7.dp, y = 74.dp)
                        .background(glowColor.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                )
                // grip wrapping
                Column(modifier = Modifier.offset(x = 7.dp, y = 74.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(6) {
                        Box(modifier = Modifier.width(9.dp).height(1.5.dp).background(Color.White.copy(alpha = 0.3f)))
                    }
                }
            }

            // ── Stumps (right side) ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .offset(x = maxW * 0.80f, y = maxH * 0.52f)
                    .graphicsLayer(scaleX = stumpScale, scaleY = stumpScale)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier.width(5.dp).height(52.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (isDark) Color(0xFFECFDF5).copy(alpha = 0.8f) else Color(0xFF064E3B).copy(alpha = 0.6f),
                                            if (isDark) Color(0xFFA7F3D0).copy(alpha = 0.5f) else Color(0xFF059669).copy(alpha = 0.3f)
                                        )
                                    ),
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                }
                // bails
                Box(
                    modifier = Modifier.width(25.dp).height(3.dp)
                        .background(
                            if (isDark) Color(0xFFECFDF5).copy(alpha = 0.7f) else Color(0xFF064E3B).copy(alpha = 0.5f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            // ── Third small ball (bottom right area) ─────────────────────
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .offset(x = maxW * 0.82f, y = maxH * 0.75f)
                    .rotate(ballRot * 1.2f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color(0xFFD1FAE5), Color(0xFF6EE7B7))), CircleShape)
                        .border(1.dp, glowColor.copy(alpha = 0.5f), CircleShape)
                )
                Box(modifier = Modifier.size(7.dp).offset(x = 4.dp, y = 4.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
            }
        }

        // ── MAIN CONTENT ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // Branding
            Text("🏏", fontSize = 54.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "CricketHub",
                fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                color = glowColor, letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Score live. Track Careers. Predict the Game.",
                fontSize = 13.sp, color = textS,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "WXYZ Production",
                fontSize = 10.sp,
                color = textS.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Login Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderC, RoundedCornerShape(22.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome back", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textP)
                    Text("Sign in to your account", fontSize = 12.sp, color = textS, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

                    // Error
                    if (uiState.error != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(ErrorRed.copy(alpha = 0.1f))
                                .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) { Text(uiState.error ?: "", color = ErrorRed, fontSize = 12.sp) }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    AuthTextField(
                        value = email, onValueChange = { email = it }, label = "Email or phone",
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthTextField(
                        value = password, onValueChange = { password = it }, label = "Password",
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = textS, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { rememberMe = !rememberMe }) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = glowColor, uncheckedColor = textS))
                            Text("Remember me", color = textS, fontSize = 12.sp)
                        }
                        Text("Forgot password?", color = glowColor, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onNavigateToForgotPassword() })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { if (email.isNotBlank() && password.isNotBlank()) viewModel.login(email.trim(), password) },
                        enabled = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = glowColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        else Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = borderC)
                        Text("  or  ", color = textS, fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = borderC)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { onLoginSuccess() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textS),
                        border = BorderStroke(1.dp, borderC),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Continue as Guest", fontSize = 14.sp) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", color = textS, fontSize = 14.sp)
                Text("Sign up", color = glowColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignup() })
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val textP  = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS  = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val borderC = if (isDark) Color(0xFF1A3828) else Color(0xFFBBF7D0)
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) }, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textP, unfocusedTextColor = textP,
            focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = borderC,
            focusedLabelColor = Color(0xFF34D399), unfocusedLabelColor = textS,
            cursorColor = Color(0xFF34D399), errorBorderColor = ErrorRed, errorLabelColor = ErrorRed
        )
    )
}