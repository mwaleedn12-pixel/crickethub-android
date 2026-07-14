package com.crickethub.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val otpSent: Boolean = false,
    val otpEmail: String = "",
    val otpVerified: Boolean = false,
    val usernameAvailable: Boolean? = null
)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Session check ─────────────────────────────────────────────────────────
    fun checkSession() {
        viewModelScope.launch {
            try {
                val session = repository.getSession()
                if (session != null) {
                    val expiresAt = session.expiresAt
                    val now = kotlinx.datetime.Clock.System.now()
                    // Session valid for 1 hour = 3600 seconds
                    val sessionAge = (now - session.tokenType.let {
                        kotlinx.datetime.Instant.fromEpochSeconds(0)
                    }).inWholeSeconds
                    _uiState.update { it.copy(isLoggedIn = true) }
                } else {
                    _uiState.update { it.copy(isLoggedIn = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoggedIn = false) }
            }
        }
    }

    fun isSessionValid(): Boolean {
        return try {
            val session = repository.getSession()
            session != null
        } catch (e: Exception) { false }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.login(email, password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("Invalid login") == true -> "Invalid email or password"
                    e.message?.contains("Email not confirmed") == true -> "Please verify your email first"
                    else -> e.message ?: "Login failed"
                }
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.signInWithGoogle(context)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Google sign-in failed: ${e.message}") }
            }
        }
    }

    // ── Signup ────────────────────────────────────────────────────────────────
    fun signup(email: String, password: String, fullName: String, username: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.signup(email, password, fullName, username, role)
                // Send OTP for email verification
                repository.sendOTP(email)
                _uiState.update { it.copy(isLoading = false, otpSent = true, otpEmail = email) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Signup failed") }
            }
        }
    }

    // ── OTP ───────────────────────────────────────────────────────────────────
    private var otpSentTime = 0L

    fun sendOTP(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.sendOTP(email)
                otpSentTime = System.currentTimeMillis()
                _uiState.update { it.copy(isLoading = false, otpSent = true, otpEmail = email) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Could not send OTP: ${e.message}") }
            }
        }
    }

    fun verifyOTP(email: String, token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val elapsed = System.currentTimeMillis() - otpSentTime
                if (elapsed > 120_000L) {
                    _uiState.update { it.copy(isLoading = false, error = "OTP expired. Please request a new one.") }
                    return@launch
                }
                repository.verifyOTP(email, token)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true, otpSent = false, otpVerified = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Invalid OTP code") }
            }
        }
    }

    fun resetOTP() {
        _uiState.update { it.copy(otpSent = false, otpEmail = "", error = null) }
    }

    // ── Forgot Password — OTP flow ────────────────────────────────────────────
    fun sendPasswordResetOTP(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.sendPasswordResetOTP(email)
                otpSentTime = System.currentTimeMillis()
                _uiState.update { it.copy(isLoading = false, otpSent = true, otpEmail = email) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Could not send OTP: ${e.message}") }
            }
        }
    }

    fun verifyOTPAndUpdatePassword(email: String, token: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.verifyOTP(email, token)
                repository.updatePassword(newPassword)
                _uiState.update { it.copy(isLoading = false, otpSent = false, otpVerified = true,
                    successMessage = "Password changed successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
            }
        }
    }

    // ── Other ─────────────────────────────────────────────────────────────────
    fun logout() {
        viewModelScope.launch {
            try { repository.logout() } catch (e: Exception) {}
            _uiState.update { AuthUiState() }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.resetPassword(email)
                _uiState.update { it.copy(isLoading = false, successMessage = "Reset email sent!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun checkUsername(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(usernameAvailable = null) }
            try {
                val available = repository.isUsernameAvailable(username)
                _uiState.update { it.copy(usernameAvailable = available) }
            } catch (e: Exception) {}
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}