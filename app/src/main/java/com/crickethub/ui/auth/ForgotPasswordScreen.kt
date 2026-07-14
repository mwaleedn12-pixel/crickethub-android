package com.crickethub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.ui.theme.*


@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            viewModel.resetOTP()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Forgot Password",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text("🔐", fontSize = 56.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Reset your password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            "Enter your email address and we'll send you a link to reset your password.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.otpSent) {
            // Success state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonGreen.copy(alpha = 0.1f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✅", fontSize = 32.sp)
                    Text(
                        "Reset link sent!",
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Check your email inbox and follow the link to reset your password.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onBack) {
                Text("Back to Login", color = NeonGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        } else {
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

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        viewModel.sendPasswordResetOTP(email.trim())
                    }
                },
                enabled = email.isNotBlank() && !uiState.isLoading,
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
                        "Send OTP Code",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OTP + New Password
            if (uiState.otpSent) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("OTP sent to ${uiState.otpEmail}", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = otpCode, onValueChange = { if (it.length <= 6) otpCode = it },
                    label = { Text("6-digit OTP Code") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("New Password") }, singleLine = true,
                    visualTransformation = if (showNewPassword) androidx.compose.ui.text.input.VisualTransformation.None
                    else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSecondary)
                    }},
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmNewPassword, onValueChange = { confirmNewPassword = it },
                    label = { Text("Confirm New Password") }, singleLine = true,
                    isError = confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { viewModel.resetOTP() }, modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("Resend") }
                    Button(
                        onClick = { if (newPassword == confirmNewPassword)
                            viewModel.verifyOTPAndUpdatePassword(uiState.otpEmail, otpCode, newPassword) },
                        enabled = otpCode.length == 6 && newPassword.length >= 6
                                && newPassword == confirmNewPassword && !uiState.isLoading,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        else Text("Change Password", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            if (uiState.otpVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Password changed! Please login.", color = NeonGreen, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remember it now? ", color = TextSecondary, fontSize = 14.sp)
                Text(
                    "Login",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        }
    }
}