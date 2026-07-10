package com.crickethub.data.repository

import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

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
            .decodeSingle()
    }

    suspend fun updateTeam(teamId: String, name: String, shortName: String?, jerseyColor: String?, category: String?, country: String?, city: String?, homeGround: String?, coach: String?): Team {
        return client.postgrest["teams"]
            .update(
                TeamInsert(
                    name = name,
                    shortName = shortName,
                    jerseyColor = jerseyColor,
                    category = category,
                    country = country,
                    city = city,
                    homeGround = homeGround,
                    coach = coach
                )
            ) {
                filter { eq("id", teamId) }
                select()
            }
            .decodeSingle()
    }

    suspend fun deleteTeam(teamId: String) {
        client.postgrest["teams"]
            .delete { filter { eq("id", teamId) } }
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