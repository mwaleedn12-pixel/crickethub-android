package com.crickethub.data.repository

import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth

class TeamRepository {

    private val client = SupabaseClient.client

    suspend fun getAllTeams(): List<Team> {
        return client.postgrest["teams"]
            .select()
            .decodeList()
    }

    suspend fun getTeamById(teamId: String): Team? {
        return client.postgrest["teams"]
            .select { filter { eq("id", teamId) } }
            .decodeSingleOrNull()
    }

    suspend fun createTeam(team: TeamInsert): Team {
        val joinCode = generateJoinCode()
        return client.postgrest["teams"]
            .insert(mapOf(
                "name" to team.name,
                "short_name" to team.shortName,
                "logo_url" to team.logoUrl,
                "jersey_color" to (team.jerseyColor ?: "#10B981"),
                "category" to (team.category ?: "Club"),
                "country" to team.country,
                "city" to team.city,
                "home_ground" to team.homeGround,
                "coach" to team.coach,
                "join_code" to joinCode,
                "is_public" to team.isPublic
            )) {
                select()
            }
            .decodeSingle()
    }

    suspend fun updateTeam(teamId: String, updates: Map<String, Any?>): Team {
        return client.postgrest["teams"]
            .update(updates) {
                filter { eq("id", teamId) }
                select()
            }
            .decodeSingle()
    }

    suspend fun deleteTeam(teamId: String) {
        client.postgrest["teams"]
            .delete { filter { eq("id", teamId) } }
    }

    suspend fun getTeamsByUser(): List<Team> {
        val userId = client.auth.currentUserOrNull()?.id ?: return emptyList()
        return client.postgrest["teams"]
            .select()
            .decodeList()
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