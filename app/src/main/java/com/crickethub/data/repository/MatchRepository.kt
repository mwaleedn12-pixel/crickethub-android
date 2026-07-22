package com.crickethub.data.repository

import com.crickethub.data.model.Match
import com.crickethub.data.model.Innings
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.PlayingXI
import com.crickethub.data.model.PlayingXIInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class MatchRepository {

    private val client = SupabaseClient.client
    private var matchesCache: List<Match>? = null
    private val matchCache = mutableMapOf<String, Match>()
    private val xiCache = mutableMapOf<String, List<PlayingXI>>()

    suspend fun getAllMatches(): List<Match> {
        return matchesCache ?: run {
            val matches = client.postgrest["matches"]
                .select()
                .decodeList<Match>()
                .sortedByDescending { it.createdAt }
            matchesCache = matches
            matches
        }
    }

    fun invalidateMatchesCache() {
        matchesCache = null
        matchCache.clear()
    }

    suspend fun getMatchById(matchId: String): Match? {
        return matchCache.getOrPut(matchId) {
            client.postgrest["matches"]
                .select { filter { eq("id", matchId) } }
                .decodeSingleOrNull<Match>() ?: return null
        }
    }

    fun invalidateMatchCache(matchId: String) {
        matchCache.remove(matchId)
        matchesCache = null
    }

    suspend fun createMatch(match: MatchInsert): Match {
        val result = client.postgrest["matches"]
            .insert(match) { select() }
            .decodeSingle<Match>()
        matchesCache = null
        return result
    }

    suspend fun updateToss(
        matchId: String,
        tossWinnerId: String,
        tossDecision: String,
        battingFirstId: String
    ): Match {
        val result = client.postgrest["matches"]
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
        matchCache[matchId] = result
        matchesCache = null
        return result
    }

    suspend fun getPlayingXI(matchId: String): List<PlayingXI> {
        return xiCache.getOrPut(matchId) {
            client.postgrest["playing_xi"]
                .select { filter { eq("match_id", matchId) } }
                .decodeList()
        }
    }

    fun invalidateXICache(matchId: String) {
        xiCache.remove(matchId)
    }

    // Deletes a match. All children (innings, balls, playing_xi, player_awards,
    // match_notifications) are ON DELETE CASCADE in the database, so removing the
    // match row removes them automatically.
    suspend fun deleteMatch(matchId: String) {
        client.postgrest["matches"].delete { filter { eq("id", matchId) } }
        invalidateMatchCache(matchId)
        invalidateMatchesCache()
        invalidateXICache(matchId)
    }

    suspend fun insertPlayingXI(players: List<PlayingXIInsert>) {
        if (players.isEmpty()) return
        val matchId = players.first().matchId
        val teamId = players.first().teamId
        // Delete existing XI for this team in this match first
        try {
            client.postgrest["playing_xi"].delete {
                filter {
                    eq("match_id", matchId)
                    eq("team_id", teamId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CricketHub", "Delete XI warning: ${e.message}")
        }
        client.postgrest["playing_xi"].insert(players)
        xiCache.remove(matchId)
    }
}