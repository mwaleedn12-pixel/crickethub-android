package com.crickethub.data.repository

import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
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

    suspend fun signup(
        email: String,
        password: String,
        fullName: String,
        username: String,
        role: String
    ) {
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

    suspend fun logout() {
        SupabaseClient.client.auth.signOut()
    }

    suspend fun resetPassword(email: String) {
        SupabaseClient.client.auth.resetPasswordForEmail(email)
    }

    fun getCurrentUser() = SupabaseClient.client.auth.currentUserOrNull()

    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val result = SupabaseClient.client.postgrest["profiles"]
                .select { filter { eq("username", username) } }
                .decodeList<com.crickethub.data.model.Profile>()
            result.isEmpty()
        } catch (e: Exception) {
            true
        }
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
            }) {
                filter { eq("id", userId) }
            }
    }
}