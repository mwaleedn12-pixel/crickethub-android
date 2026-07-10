package com.crickethub.data.repository

import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class TeamRepository {

    private val client = SupabaseClient.client

    // Cache
    private var teamsCache: List<Team>? = null

    suspend fun getAllTeams(): List<Team> {
        return teamsCache ?: run {
            val teams = client.postgrest["teams"]
                .select()
                .decodeList<Team>()
            teamsCache = teams
            teams
        }
    }

    suspend fun getTeamsByIds(teamIds: List<String>): List<Team> {
        if (teamIds.isEmpty()) return emptyList()
        val cached = teamsCache
        if (cached != null) {
            val result = cached.filter { it.id in teamIds }
            if (result.size == teamIds.size) return result
        }
        return client.postgrest["teams"]
            .select { filter { isIn("id", teamIds) } }
            .decodeList<Team>()
    }

    suspend fun getTeamById(teamId: String): Team? {
        return teamsCache?.find { it.id == teamId }
            ?: client.postgrest["teams"]
                .select { filter { eq("id", teamId) } }
                .decodeSingleOrNull()
    }

    suspend fun createTeam(team: TeamInsert): Team {
        val joinCode = generateJoinCode()
        val result = client.postgrest["teams"]
            .insert(
                TeamInsert(
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
        teamsCache = null // invalidate
        return result
    }

    suspend fun updateTeam(
        teamId: String, name: String, shortName: String?,
        jerseyColor: String?, category: String?, country: String?,
        city: String?, homeGround: String?, coach: String?
    ): Team {
        val result = client.postgrest["teams"]
            .update(
                TeamInsert(
                    name = name, shortName = shortName,
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
        client.postgrest["teams"]
            .delete { filter { eq("id", teamId) } }
        teamsCache = null
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