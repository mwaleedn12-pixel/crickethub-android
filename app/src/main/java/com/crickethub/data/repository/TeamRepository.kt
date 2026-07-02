package com.crickethub.data.repository

import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class TeamRepository {

    suspend fun getAllTeams(): List<Team> {
        return SupabaseClient.client.postgrest["teams"]
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Team>()
    }

    suspend fun getTeamById(id: String): Team? {
        return SupabaseClient.client.postgrest["teams"]
            .select {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<Team>()
    }

    suspend fun createTeam(team: TeamInsert): Team {
        return SupabaseClient.client.postgrest["teams"]
            .insert(team) { select() }
            .decodeSingle<Team>()
    }

    suspend fun updateTeam(id: String, name: String, shortName: String?): Team {
        return SupabaseClient.client.postgrest["teams"]
            .update({
                set("name", name)
                set("short_name", shortName)
            }) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<Team>()
    }

    suspend fun deleteTeam(id: String) {
        SupabaseClient.client.postgrest["teams"]
            .delete {
                filter { eq("id", id) }
            }
    }
}