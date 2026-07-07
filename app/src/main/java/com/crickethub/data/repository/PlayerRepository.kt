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
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Player>()
    }

    suspend fun getPlayerById(playerId: String): Player? {
        return SupabaseClient.client.postgrest["players"]
            .select { filter { eq("id", playerId) } }
            .decodeSingleOrNull<Player>()
    }

    suspend fun createPlayer(player: PlayerInsert): Player {
        return SupabaseClient.client.postgrest["players"]
            .insert(player) { select() }
            .decodeSingle<Player>()
    }

    suspend fun deletePlayer(playerId: String) {
        SupabaseClient.client.postgrest["players"]
            .delete { filter { eq("id", playerId) } }
    }

    suspend fun updatePlayerAvailability(playerId: String, availability: String) {
        SupabaseClient.client.postgrest["players"]
            .update({ set("availability", availability) }) {
                filter { eq("id", playerId) }
            }
    }
}