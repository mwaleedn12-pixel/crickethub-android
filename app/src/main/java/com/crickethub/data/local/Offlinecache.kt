package com.crickethub.data.local

import android.content.Context
import android.content.SharedPreferences
import com.crickethub.data.model.Match
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.model.Tournament
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SharedPreferences-backed cache for full entity lists.
 * Populated when online, read when offline.
 *
 * Each table has save/get. Data is JSON-serialized.
 */
object OfflineCache {

    private const val PREFS = "crickethub_offline_cache"
    private const val KEY_TEAMS = "teams"
    private const val KEY_PLAYERS = "players"
    private const val KEY_MATCHES = "matches"
    private const val KEY_TOURNAMENTS = "tournaments"
    private const val KEY_USER_EMAIL = "user_email"
    // per-team players: "players_<teamId>"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Teams ──

    fun saveTeams(context: Context, teams: List<Team>) {
        prefs(context).edit().putString(KEY_TEAMS, json.encodeToString(teams)).apply()
    }

    fun getTeams(context: Context): List<Team> = try {
        val s = prefs(context).getString(KEY_TEAMS, null)
        if (s != null) json.decodeFromString<List<Team>>(s) else emptyList()
    } catch (_: Exception) { emptyList() }

    // ── Players (per team) ──

    fun savePlayersForTeam(context: Context, teamId: String, players: List<Player>) {
        prefs(context).edit().putString("players_$teamId", json.encodeToString(players)).apply()
    }

    fun getPlayersForTeam(context: Context, teamId: String): List<Player> = try {
        val s = prefs(context).getString("players_$teamId", null)
        if (s != null) json.decodeFromString<List<Player>>(s) else emptyList()
    } catch (_: Exception) { emptyList() }

    /** Save a flat list of all players (for dashboard count). */
    fun saveAllPlayers(context: Context, players: List<Player>) {
        prefs(context).edit().putString(KEY_PLAYERS, json.encodeToString(players)).apply()
    }

    fun getAllPlayers(context: Context): List<Player> = try {
        val s = prefs(context).getString(KEY_PLAYERS, null)
        if (s != null) json.decodeFromString<List<Player>>(s) else emptyList()
    } catch (_: Exception) { emptyList() }

    // ── Matches ──

    fun saveMatches(context: Context, matches: List<Match>) {
        prefs(context).edit().putString(KEY_MATCHES, json.encodeToString(matches)).apply()
    }

    fun getMatches(context: Context): List<Match> = try {
        val s = prefs(context).getString(KEY_MATCHES, null)
        if (s != null) json.decodeFromString<List<Match>>(s) else emptyList()
    } catch (_: Exception) { emptyList() }

    // ── Tournaments ──

    fun saveTournaments(context: Context, tournaments: List<Tournament>) {
        prefs(context).edit().putString(KEY_TOURNAMENTS, json.encodeToString(tournaments)).apply()
    }

    fun getTournaments(context: Context): List<Tournament> = try {
        val s = prefs(context).getString(KEY_TOURNAMENTS, null)
        if (s != null) json.decodeFromString<List<Tournament>>(s) else emptyList()
    } catch (_: Exception) { emptyList() }

    // ── User ──

    fun saveUserEmail(context: Context, email: String) {
        prefs(context).edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(context: Context): String =
        prefs(context).getString(KEY_USER_EMAIL, "Guest") ?: "Guest"

    // ── Utility ──

    fun hasData(context: Context): Boolean =
        prefs(context).contains(KEY_TEAMS)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}