package com.crickethub.data.repository

import com.crickethub.data.local.OfflineCache
import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class TeamRepository {

    private val client = SupabaseClient.client

    // Cache with TTL
    private var teamsCache: List<Team>? = null
    private var teamsCacheTime: Long = 0L
    private val TEAMS_TTL_MS = 3 * 60 * 1000L  // 3 minutes

    private fun context() = com.crickethub.CricketHubApp.instance

    suspend fun getAllTeams(): List<Team> {
        val cached = teamsCache
        if (cached != null && System.currentTimeMillis() - teamsCacheTime < TEAMS_TTL_MS) {
            return cached
        }
        return try {
            SupabaseClient.withRetry {
                val teams = client.postgrest["teams"]
                    .select()
                    .decodeList<Team>()
                teamsCache = teams
                teamsCacheTime = System.currentTimeMillis()
                // Save to offline cache
                OfflineCache.saveTeams(context(), teams)
                teams
            }
        } catch (e: Exception) {
            android.util.Log.w("CricketHub", "getAllTeams offline fallback: ${e.message}")
            OfflineCache.getTeams(context())
        }
    }

    suspend fun getTeamsByIds(teamIds: List<String>): List<Team> {
        if (teamIds.isEmpty()) return emptyList()
        val cached = teamsCache
        if (cached != null) {
            val result = cached.filter { it.id in teamIds }
            if (result.size == teamIds.size) return result
        }
        return try {
            client.postgrest["teams"]
                .select { filter { isIn("id", teamIds) } }
                .decodeList<Team>()
        } catch (e: Exception) {
            OfflineCache.getTeams(context()).filter { it.id in teamIds }
        }
    }

    suspend fun getTeamById(teamId: String): Team? {
        return teamsCache?.find { it.id == teamId }
            ?: try {
                client.postgrest["teams"]
                    .select { filter { eq("id", teamId) } }
                    .decodeSingleOrNull()
            } catch (e: Exception) {
                OfflineCache.getTeams(context()).find { it.id == teamId }
            }
    }

    suspend fun createTeam(team: TeamInsert): Team {
        val joinCode = generateJoinCode()
        val userId = client.auth.currentUserOrNull()?.id
        val result = SupabaseClient.withRetry {
            client.postgrest["teams"]
                .insert(
                    TeamInsert(
                        userId = userId,
                        name = team.name,
                        shortName = team.shortName,
                        logoUrl = team.logoUrl,
                        jerseyColor = team.jerseyColor ?: "#10B981",
                        category = team.category ?: "Club",
                        country = team.country,
                        city = team.city,
                        homeGround = team.homeGround,
                        coach = team.coach,
                        captainId = team.captainId,
                        viceCaptainId = team.viceCaptainId,
                        joinCode = joinCode,
                        isPublic = team.isPublic
                    )
                ) { select() }
                .decodeSingle<Team>()
        }
        teamsCache = null
        teamsCacheTime = 0L
        return result
    }

    suspend fun updateTeam(
        teamId: String, name: String, shortName: String?,
        logoUrl: String? = null,
        jerseyColor: String?, category: String?, country: String?,
        city: String?, homeGround: String?, coach: String?
    ): Team {
        val result = client.postgrest["teams"]
            .update(
                TeamInsert(
                    name = name, shortName = shortName,
                    logoUrl = logoUrl,
                    jerseyColor = jerseyColor, category = category,
                    country = country, city = city,
                    homeGround = homeGround, coach = coach
                )
            ) {
                filter { eq("id", teamId) }
                select()
            }
            .decodeSingle<Team>()
        teamsCache = null
        return result
    }

    suspend fun deleteTeam(teamId: String) {
        SupabaseClient.withRetry {
            client.postgrest["teams"]
                .delete { filter { eq("id", teamId) } }
        }
        teamsCache = null
        teamsCacheTime = 0L
    }

    suspend fun getTeamByJoinCode(code: String): Team? {
        return client.postgrest["teams"]
            .select { filter { eq("join_code", code) } }
            .decodeSingleOrNull()
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}