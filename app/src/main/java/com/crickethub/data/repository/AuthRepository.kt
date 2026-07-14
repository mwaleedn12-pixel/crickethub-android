package com.crickethub.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    suspend fun login(email: String, password: String) {
        SupabaseClient.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signup(email: String, password: String, fullName: String, username: String, role: String) {
        SupabaseClient.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject {
                put("full_name", fullName)
                put("username", username)
                put("role", role)
            }
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────
    suspend fun signInWithGoogle(context: Context) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("118745721856-j92hkjci6dpbuag7jspjs4o5khpvk3hu.apps.googleusercontent.com")
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        SupabaseClient.client.auth.signInWith(IDToken) {
            idToken = googleCredential.idToken
            provider = Google
        }
    }

    // ── OTP ───────────────────────────────────────────────────────────────────
    suspend fun sendOTP(email: String) {
        SupabaseClient.client.auth.signInWith(OTP) {
            this.email = email
            this.createUser = true
        }
    }

    suspend fun verifyOTP(email: String, token: String) {
        SupabaseClient.client.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
    }

    // ── Forgot Password ───────────────────────────────────────────────────────
    suspend fun sendPasswordResetOTP(email: String) {
        SupabaseClient.client.auth.signInWith(OTP) {
            this.email = email
            this.createUser = false
        }
    }

    suspend fun updatePassword(newPassword: String) {
        SupabaseClient.client.auth.updateUser {
            password = newPassword
        }
    }

    // ── Other ─────────────────────────────────────────────────────────────────
    suspend fun logout() { SupabaseClient.client.auth.signOut() }
    suspend fun resetPassword(email: String) { SupabaseClient.client.auth.resetPasswordForEmail(email) }
    fun getCurrentUser() = SupabaseClient.client.auth.currentUserOrNull()
    fun getSession() = SupabaseClient.client.auth.currentSessionOrNull()

    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val result = SupabaseClient.client.postgrest["profiles"]
                .select { filter { eq("username", username) } }
                .decodeList<com.crickethub.data.model.Profile>()
            result.isEmpty()
        } catch (e: Exception) { true }
    }

    suspend fun updateProfile(userId: String, update: com.crickethub.data.model.ProfileUpdate) {
        SupabaseClient.client.postgrest["profiles"]
            .update({
                update.username?.let { set("username", it) }
                update.fullName?.let { set("full_name", it) }
                update.city?.let { set("city", it) }
                update.dateOfBirth?.let { set("date_of_birth", it) }
                update.gender?.let { set("gender", it) }
                update.photoUrl?.let { set("photo_url", it) }
            }) { filter { eq("id", userId) } }
    }
}