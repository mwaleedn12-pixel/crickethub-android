package com.crickethub.data.repository

import com.crickethub.data.model.Match
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.PlayingXIInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class MatchRepository {

    suspend fun getAllMatches(): List<Match> {
        return SupabaseClient.client.postgrest["matches"]
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Match>()
    }

    suspend fun getMatchById(id: String): Match? {
        return SupabaseClient.client.postgrest["matches"]
            .select {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<Match>()
    }

    suspend fun createMatch(match: MatchInsert): Match {
        return SupabaseClient.client.postgrest["matches"]
            .insert(match) { select() }
            .decodeSingle<Match>()
    }

    suspend fun updateToss(
        matchId: String,
        tossWinnerId: String,
        tossDecision: String,
        battingFirstId: String
    ): Match {
        return SupabaseClient.client.postgrest["matches"]
            .update({
                set("toss_winner_id", tossWinnerId)
                set("toss_decision", tossDecision)
                set("batting_first_id", battingFirstId)
                set("status", "live")
            }) {
                filter { eq("id", matchId) }
                select()
            }
            .decodeSingle<Match>()
    }

    suspend fun insertPlayingXI(players: List<PlayingXIInsert>) {
        SupabaseClient.client.postgrest["playing_xi"]
            .insert(players)
    }

    suspend fun getPlayingXI(matchId: String): List<PlayingXIInsert> {
        return SupabaseClient.client.postgrest["playing_xi"]
            .select {
                filter { eq("match_id", matchId) }
            }
            .decodeList<PlayingXIInsert>()
    }

    suspend fun isPlayingXISaved(matchId: String): Boolean {
        val result = SupabaseClient.client.postgrest["playing_xi"]
            .select {
                filter { eq("match_id", matchId) }
            }
            .decodeList<PlayingXIInsert>()
        return result.size >= 22
    }
}