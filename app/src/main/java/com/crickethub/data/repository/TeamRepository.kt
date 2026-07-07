package com.crickethub.data.repository

import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class TeamRepository {

    suspend fun getAllTeams(): List<Team> {
        return SupabaseClient.client.postgrest["teams"]
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<Team>()
    }

    suspend fun getTeamById(id: String): Team? {
        return SupabaseClient.client.postgrest["teams"]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Team>()
    }

    suspend fun createTeam(team: TeamInsert): Team {
        return SupabaseClient.client.postgrest["teams"]
            .insert(team) { select() }
            .decodeSingle<Team>()
    }

    suspend fun deleteTeam(teamId: String) {
        SupabaseClient.client.postgrest["teams"]
            .delete { filter { eq("id", teamId) } }
    }

    suspend fun updateTeam(teamId: String, updates: com.crickethub.data.model.TeamUpdate) {
        SupabaseClient.client.postgrest["teams"]
            .update({
                updates.name?.let { set("name", it) }
                updates.shortName?.let { set("short_name", it) }
                updates.jerseyColor?.let { set("jersey_color", it) }
                updates.category?.let { set("category", it) }
                updates.country?.let { set("country", it) }
                updates.city?.let { set("city", it) }
                updates.homeGround?.let { set("home_ground", it) }
                updates.coach?.let { set("coach", it) }
                updates.captainId?.let { set("captain_id", it) }
                updates.viceCaptainId?.let { set("vice_captain_id", it) }
            }) {
                filter { eq("id", teamId) }
            }
    }
}