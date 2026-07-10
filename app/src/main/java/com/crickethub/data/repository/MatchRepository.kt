package com.crickethub.data.repository

import com.crickethub.data.model.Match
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.PlayingXI
import com.crickethub.data.model.PlayingXIInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class MatchRepository {

    private val client = SupabaseClient.client

    suspend fun getAllMatches(): List<Match> {
        return client.postgrest["matches"]
            .select()
            .decodeList()
    }

    suspend fun getMatchById(matchId: String): Match? {
        return client.postgrest["matches"]
            .select { filter { eq("id", matchId) } }
            .decodeSingleOrNull()
    }

    suspend fun createMatch(match: MatchInsert): Match {
        return client.postgrest["matches"]
            .insert(match) { select() }
            .decodeSingle()
    }

    suspend fun updateToss(
        matchId: String,
        tossWinnerId: String,
        tossDecision: String,
        battingFirstId: String
    ): Match {
        return client.postgrest["matches"]
            .update({
                set("toss_winner_id", tossWinnerId)
                set("toss_decision", tossDecision)
                set("batting_first_id", battingFirstId)
                set("status", "live")
            }) {
                filter { eq("id", matchId) }
                select()
            }
            .decodeSingle()
    }

    suspend fun getPlayingXI(matchId: String): List<PlayingXI> {
        return client.postgrest["playing_xi"]
            .select { filter { eq("match_id", matchId) } }
            .decodeList()
    }

    suspend fun insertPlayingXI(players: List<PlayingXIInsert>) {
        if (players.isEmpty()) return
        client.postgrest["playing_xi"].insert(players)
    }
}