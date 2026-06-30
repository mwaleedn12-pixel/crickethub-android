package com.crickethub.data.repository

import com.crickethub.data.model.Profile
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> {
        return try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                SupabaseClient.client.postgrest["profiles"]
                    .update({
                        set("full_name", fullName)
                    }) {
                        filter {
                            eq("id", userId)
                        }
                    }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            SupabaseClient.client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return SupabaseClient.client.auth.currentUserOrNull()?.id
    }

    fun hasActiveSession(): Boolean {
        return SupabaseClient.client.auth.currentUserOrNull() != null
    }

    suspend fun getCurrentProfile(): Profile? {
        val userId = getCurrentUserId() ?: return null
        return try {
            SupabaseClient.client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            null
        }
    }
}