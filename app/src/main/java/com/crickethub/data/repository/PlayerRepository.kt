package com.crickethub.data.repository

import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class PlayerRepository {

    suspend fun getPlayersByTeam(teamId: String): List<Player> {
        return SupabaseClient.client.postgrest["players"]
            .select {
                filter { eq("team_id", teamId) }
                order("jersey_no", Order.ASCENDING)
            }
            .decodeList<Player>()
    }

    suspend fun getPlayerById(id: String): Player? {
        return SupabaseClient.client.postgrest["players"]
            .select {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<Player>()
    }

    suspend fun createPlayer(player: PlayerInsert): Player {
        return SupabaseClient.client.postgrest["players"]
            .insert(player) { select() }
            .decodeSingle<Player>()
    }

    suspend fun updatePlayer(
        id: String,
        fullName: String,
        jerseyNo: Int?,
        role: String?,
        battingStyle: String?,
        bowlingStyle: String?
    ): Player {
        return SupabaseClient.client.postgrest["players"]
            .update({
                set("full_name", fullName)
                set("jersey_no", jerseyNo)
                set("role", role)
                set("batting_style", battingStyle)
                set("bowling_style", bowlingStyle)
            }) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<Player>()
    }

    suspend fun deletePlayer(id: String) {
        SupabaseClient.client.postgrest["players"]
            .delete {
                filter { eq("id", id) }
            }
    }
}