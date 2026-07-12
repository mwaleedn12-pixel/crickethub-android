package com.crickethub.ui.auth

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("viewer") }
    var city by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var showGenderDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onSignupSuccess()
    }

    val inf = rememberInfiniteTransition(label = "bg")

    val ballRot by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "ball"
    )
    // Drift — bottom to top
    val drift1 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing)), label = "d1"
    )
    val drift2 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing), initialStartOffset = StartOffset(2200)), label = "d2"
    )
    val drift3 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing), initialStartOffset = StartOffset(4800)), label = "d3"
    )
    val drift4 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), initialStartOffset = StartOffset(1200)), label = "d4"
    )
    val drift5 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(50000, easing = LinearEasing), initialStartOffset = StartOffset(3800)), label = "d5"
    )
    val drift6 by inf.animateFloat(
        initialValue = 1.1f, targetValue = -0.15f,
        animationSpec = infiniteRepeatable(tween(32000, easing = LinearEasing), initialStartOffset = StartOffset(5800)), label = "d6"
    )

    val isDark   = isSystemInDarkTheme()
    val bgColor  = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
    val cardBg   = if (isDark) Color(0xFF0D2018).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f)
    val borderC  = if (isDark) Color(0xFF34D399).copy(alpha = 0.3f) else Color(0xFF34D399).copy(alpha = 0.4f)
    val textP    = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS    = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val glowColor = Color(0xFF34D399)
    val wordColor = if (isDark) Color(0xFF34D399).copy(alpha = 0.22f) else Color(0xFF059669).copy(alpha = 0.16f)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        // Glow blobs
        Box(modifier = Modifier.size(260.dp).offset(x = (-60).dp, y = (-40).dp)
            .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.15f), Color.Transparent)), CircleShape)
            .blur(40.dp))
        Box(modifier = Modifier.size(200.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp)
            .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.10f), Color.Transparent)), CircleShape)
            .blur(32.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxW = maxWidth
            val maxH = maxHeight

            // Drifting words — bottom to top
            listOf(
                "Yorker"        to Pair(0.06f, drift1),
                "Six!"          to Pair(0.74f, drift2),
                "LBW"           to Pair(0.16f, drift3),
                "Hat-Trick"     to Pair(0.62f, drift4),
                "Bouncer"       to Pair(0.28f, drift5),
                "Cover Drive"   to Pair(0.54f, drift6),
                "Googly"        to Pair(0.80f, drift1),
                "Super Over"    to Pair(0.38f, drift2),
                "No Ball"       to Pair(0.10f, drift3),
                "Powerplay"     to Pair(0.48f, drift4),
                "DLS"           to Pair(0.85f, drift5),
                "Reverse Swing" to Pair(0.22f, drift6),
            ).forEach { (word, pos) ->
                val (xFrac, driftY) = pos
                Text(
                    text = word, color = wordColor,
                    fontSize = (13 + word.length % 4).sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    modifier = Modifier.offset(x = maxW * xFrac, y = maxH * driftY)
                )
            }

            // Realistic Cricket Ball top right
            Box(modifier = Modifier.size(58.dp).offset(x = maxW * 0.75f, y = maxH * 0.04f)) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFCC2200), Color(0xFF8B0000), Color(0xFF5C0000)),
                            radius = 110f
                        ), CircleShape
                    ))
                Box(modifier = Modifier.size(18.dp).offset(x = 8.dp, y = 7.dp)
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)), CircleShape))
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Color(0xFFEEEEEE), startAngle = -30f, sweepAngle = 60f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(size.width-8f, size.height-8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                    drawArc(color = Color(0xFFEEEEEE), startAngle = 150f, sweepAngle = 60f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(size.width-8f, size.height-8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                    val cx = size.width/2f; val cy = size.height/2f; val r = size.width/2f - 4f
                    for (i in 0..4) {
                        val angle = Math.toRadians((-15 + i * 7).toDouble())
                        drawCircle(color = Color(0xFFCCCCCC), radius = 1.5f,
                            center = androidx.compose.ui.geometry.Offset((cx + r*0.85f*Math.cos(angle)).toFloat(), (cy + r*0.85f*Math.sin(angle)).toFloat()))
                    }
                    for (i in 0..4) {
                        val angle = Math.toRadians((165 + i * 7).toDouble())
                        drawCircle(color = Color(0xFFCCCCCC), radius = 1.5f,
                            center = androidx.compose.ui.geometry.Offset((cx + r*0.85f*Math.cos(angle)).toFloat(), (cy + r*0.85f*Math.sin(angle)).toFloat()))
                    }
                }
                Box(modifier = Modifier.fillMaxSize().border(1.5.dp, glowColor.copy(alpha = 0.5f), CircleShape))
            }

            // Small ball bottom right
            Box(modifier = Modifier.size(32.dp).offset(x = maxW * 0.82f, y = maxH * 0.72f).rotate(-ballRot)) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFFA7F3D0), Color(0xFF34D399))), CircleShape)
                    .border(1.5.dp, glowColor.copy(alpha = 0.6f), CircleShape))
                Box(modifier = Modifier.size(9.dp).offset(x = 5.dp, y = 5.dp).background(Color.White.copy(alpha = 0.45f), CircleShape))
            }

            // Stumps right side
            Box(modifier = Modifier.offset(x = maxW * 0.04f, y = maxH * 0.55f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.width(5.dp).height(48.dp)
                            .background(
                                Brush.verticalGradient(listOf(
                                    if (isDark) Color(0xFFECFDF5).copy(alpha = 0.7f) else Color(0xFF064E3B).copy(alpha = 0.5f),
                                    if (isDark) Color(0xFFA7F3D0).copy(alpha = 0.4f) else Color(0xFF059669).copy(alpha = 0.25f)
                                )), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            ))
                    }
                }
                Box(modifier = Modifier.width(25.dp).height(3.dp)
                    .background(if (isDark) Color(0xFFECFDF5).copy(alpha = 0.65f) else Color(0xFF064E3B).copy(alpha = 0.45f), RoundedCornerShape(2.dp)))
            }
        }

        // Main content
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text("🏏", fontSize = 46.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text("CricketHub", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = glowColor, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "Score live. Track Careers. Predict the Game.",
                fontSize = 12.sp, color = textS, textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic, letterSpacing = 0.3.sp,
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

            Spacer(modifier = Modifier.height(22.dp))

            // Signup Card
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(cardBg).border(1.dp, borderC, RoundedCornerShape(22.dp)).padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Create Account", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textP)
                    Text("Join the CricketHub community", fontSize = 12.sp, color = textS,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    if (uiState.error != null) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(ErrorRed.copy(alpha = 0.1f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(10.dp)
                        ) { Text(uiState.error ?: "", color = ErrorRed, fontSize = 12.sp) }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    AuthTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name",
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = glowColor, modifier = Modifier.size(18.dp)) })
                    Spacer(modifier = Modifier.height(9.dp))

                    AuthTextField(
                        value = username,
                        onValueChange = { username = it; if (it.length >= 3) viewModel.checkUsername(it) },
                        label = "Username",
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                        isError = uiState.isUsernameAvailable == false,
                        supportingText = if (uiState.isUsernameAvailable == false) "Username already taken" else null
                    )
                    Spacer(modifier = Modifier.height(9.dp))

                    AuthTextField(value = email, onValueChange = { email = it }, label = "Email",
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                        keyboardType = KeyboardType.Email)
                    Spacer(modifier = Modifier.height(9.dp))

                    AuthTextField(value = city, onValueChange = { city = it }, label = "City (optional)",
                        leadingIcon = { Icon(Icons.Default.LocationCity, null, tint = glowColor, modifier = Modifier.size(18.dp)) })
                    Spacer(modifier = Modifier.height(9.dp))

                    // Gender dropdown
                    ExposedDropdownMenuBox(expanded = showGenderDropdown, onExpandedChange = { showGenderDropdown = it }) {
                        OutlinedTextField(
                            value = gender.ifBlank { "" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender (optional)") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGenderDropdown) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textP, unfocusedTextColor = textP,
                                focusedBorderColor = glowColor, unfocusedBorderColor = if (isDark) Color(0xFF1A3828) else Color(0xFFBBF7D0),
                                focusedLabelColor = glowColor, unfocusedLabelColor = textS, cursorColor = glowColor
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = showGenderDropdown,
                            onDismissRequest = { showGenderDropdown = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF0D2018) else Color.White)
                        ) {
                            listOf("Male", "Female", "Other", "Prefer not to say").forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g, color = textP, fontSize = 13.sp) },
                                    onClick = { gender = g; showGenderDropdown = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(9.dp))

                    AuthTextField(
                        value = password, onValueChange = { password = it }, label = "Password",
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = textS, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password
                    )
                    Spacer(modifier = Modifier.height(9.dp))

                    AuthTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password",
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = glowColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = textS, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        supportingText = if (confirmPassword.isNotBlank() && password != confirmPassword) "Passwords don't match" else null
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Role
                    Text("I am a...", color = textS, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("player" to "🏏 Player", "scorer" to "📊 Scorer", "viewer" to "👁️ Viewer").forEach { (role, label) ->
                            val selected = selectedRole == role
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) glowColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.dp, if (selected) glowColor else borderC, RoundedCornerShape(10.dp))
                                    .clickable { selectedRole = role }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (selected) glowColor else textS, fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && username.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && password == confirmPassword) {
                                viewModel.signup(
                                    email = email.trim(), password = password,
                                    fullName = fullName.trim(), username = username.trim(),
                                    role = selectedRole
                                )
                            }
                        },
                        enabled = fullName.isNotBlank() && username.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && password == confirmPassword && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = glowColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        else Text("Create Account", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", color = textS, fontSize = 14.sp)
                Text("Sign in", color = glowColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() })
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}