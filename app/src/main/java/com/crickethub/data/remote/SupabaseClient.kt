package com.crickethub.data.remote

import com.crickethub.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.delay

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Auto-refresh tokens before they expire
            alwaysAutoRefresh = true
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    /**
     * Retry a Supabase call with exponential backoff.
     * On 401/auth errors, attempts a session refresh before retrying.
     * Usage:  val result = SupabaseClient.withRetry { client.postgrest["table"].select()... }
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 500,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var delayMs = initialDelayMs
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val msg = e.message?.lowercase() ?: ""
                val isAuth = msg.contains("401") || msg.contains("jwt") ||
                        msg.contains("token") || msg.contains("unauthorized")
                if (isAuth && attempt < maxAttempts - 1) {
                    try {
                        client.auth.refreshCurrentSession()
                        android.util.Log.d("CricketHub", "Session refreshed on attempt ${attempt + 1}")
                    } catch (re: Exception) {
                        android.util.Log.w("CricketHub", "Session refresh failed: ${re.message}")
                    }
                }
                if (attempt < maxAttempts - 1) {
                    delay(delayMs)
                    delayMs *= 2
                }
            }
        }
        throw lastException ?: Exception("Retry exhausted")
    }
}