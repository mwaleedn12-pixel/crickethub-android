package com.crickethub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
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

    val roles = listOf(
        "player" to "🏏 Player",
        "scorer" to "📋 Scorer",
        "coach" to "🎯 Coach",
        "organizer" to "🏆 Tournament Organizer",
        "viewer" to "👁 Viewer",
        "team_manager" to "👔 Team Manager"
    )

    val genders = listOf("Male", "Female", "Other", "Prefer not to say")

    val passwordsMatch = password == confirmPassword
    val isFormValid = fullName.isNotBlank() &&
            username.length >= 3 &&
            email.isNotBlank() &&
            password.length >= 6 &&
            passwordsMatch &&
            uiState.isUsernameAvailable != false

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onSignupSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text("🏏", fontSize = 40.sp)
        Text(
            "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "Join the cricket community",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Error
        if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorRed.copy(alpha = 0.1f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(uiState.error ?: "", color = ErrorRed, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Full name
        AuthTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full name",
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username
        AuthTextField(
            value = username,
            onValueChange = {
                username = it.lowercase().replace(" ", "_")
                viewModel.checkUsername(it)
            },
            label = "Username (unique)",
            leadingIcon = {
                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                when {
                    uiState.isCheckingUsername -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonGreen, strokeWidth = 2.dp)
                    }
                    uiState.isUsernameAvailable == true && username.length >= 3 -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = NeonGreen, modifier = Modifier.size(18.dp))
                    }
                    uiState.isUsernameAvailable == false -> {
                        Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = ErrorRed, modifier = Modifier.size(18.dp))
                    }
                }
            },
            isError = uiState.isUsernameAvailable == false,
            supportingText = when {
                username.length in 1..2 -> "Username must be at least 3 characters"
                uiState.isUsernameAvailable == false -> "Username already taken"
                uiState.isUsernameAvailable == true && username.length >= 3 -> "✓ Username available"
                else -> null
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password,
            isError = password.isNotEmpty() && password.length < 6,
            supportingText = if (password.isNotEmpty() && password.length < 6) "Minimum 6 characters" else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm password
        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm password",
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password,
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) "Passwords do not match" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // City (optional)
        AuthTextField(
            value = city,
            onValueChange = { city = it },
            label = "City (optional)",
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gender dropdown
        Box {
            AuthTextField(
                value = gender,
                onValueChange = {},
                label = "Gender (optional)",
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showGenderDropdown = true }
            )
            DropdownMenu(
                expanded = showGenderDropdown,
                onDismissRequest = { showGenderDropdown = false },
                modifier = Modifier.background(SurfaceCard)
            ) {
                genders.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g, color = TextPrimary) },
                        onClick = {
                            gender = g
                            showGenderDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Role selection
        Text(
            "Select your role",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            roles.chunked(2).forEach { rowRoles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowRoles.forEach { (value, label) ->
                        val selected = selectedRole == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) NeonGreen.copy(alpha = 0.15f) else SurfaceCard
                                )
                                .border(
                                    1.dp,
                                    if (selected) NeonGreen else BorderColor,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedRole = value }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) NeonGreen else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    // Fill empty space if odd number
                    if (rowRoles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Create account button
        Button(
            onClick = {
                if (isFormValid) {
                    viewModel.signup(
                        email = email.trim(),
                        password = password,
                        fullName = fullName.trim(),
                        username = username.trim(),
                        role = selectedRole
                    )
                }
            },
            enabled = isFormValid && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Create Account",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Login",
                color = NeonGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}