package com.crickethub.ui.auth

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
    val resetEmailSent: Boolean = false,
    val isUsernameAvailable: Boolean? = null,
    val isCheckingUsername: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.login(email, password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("Invalid login") == true -> "Invalid email or password"
                            e.message?.contains("Email not confirmed") == true -> "Please verify your email first"
                            else -> "Login failed. Please try again."
                        }
                    )
                }
            }
        }
    }

    fun signup(
        email: String,
        password: String,
        fullName: String,
        username: String,
        role: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.signup(email, password, fullName, username, role)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("already registered") == true -> "Email already registered"
                            e.message?.contains("username") == true -> "Username already taken"
                            e.message?.contains("Password") == true -> "Password must be at least 6 characters"
                            else -> "Signup failed. Please try again."
                        }
                    )
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.resetPassword(email)
                _uiState.update { it.copy(isLoading = false, resetEmailSent = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Could not send reset email. Check your email address.") }
            }
        }
    }

    fun checkUsername(username: String) {
        if (username.length < 3) {
            _uiState.update { it.copy(isUsernameAvailable = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUsername = true) }
            val available = repository.isUsernameAvailable(username)
            _uiState.update { it.copy(isUsernameAvailable = available, isCheckingUsername = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearResetEmailSent() {
        _uiState.update { it.copy(resetEmailSent = false) }
    }
}