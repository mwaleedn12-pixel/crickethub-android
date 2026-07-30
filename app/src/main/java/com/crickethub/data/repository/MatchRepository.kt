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

    // TTL cache for full list
    private var matchesCache: List<Match>? = null
    private var matchesCacheTime: Long = 0L
    private val MATCHES_TTL_MS = 2 * 60 * 1000L  // 2 minutes

    private val matchCache = mutableMapOf<String, Match>()
    private val xiCache = mutableMapOf<String, List<PlayingXI>>()

    suspend fun getAllMatches(): List<Match> {
        val cached = matchesCache
        if (cached != null && System.currentTimeMillis() - matchesCacheTime < MATCHES_TTL_MS) {
            return cached
        }
        return SupabaseClient.withRetry {
            val matches = client.postgrest["matches"]
                .select()
                .decodeList<Match>()
                .sortedByDescending { it.createdAt }
            matchesCache = matches
            matchesCacheTime = System.currentTimeMillis()
            matches
        }
    }

    /** Paginated match list: returns [limit] matches starting at [offset]. */
    suspend fun getMatchesPaginated(limit: Int = 20, offset: Int = 0): List<Match> {
        return SupabaseClient.withRetry {
            client.postgrest["matches"]
                .select {
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Match>()
                .sortedByDescending { it.createdAt }
        }
    }

    fun invalidateMatchesCache() {
        matchesCache = null
        matchesCacheTime = 0L
        matchCache.clear()
    }

    suspend fun getMatchById(matchId: String): Match? {
        matchCache[matchId]?.let { return it }
        return SupabaseClient.withRetry {
            client.postgrest["matches"]
                .select { filter { eq("id", matchId) } }
                .decodeSingleOrNull<Match>()?.also { matchCache[matchId] = it }
        }
    }

    fun invalidateMatchCache(matchId: String) {
        matchCache.remove(matchId)
        matchesCache = null
        matchesCacheTime = 0L
    }

    suspend fun createMatch(match: MatchInsert): Match {
        val result = SupabaseClient.withRetry {
            client.postgrest["matches"]
                .insert(match) { select() }
                .decodeSingle<Match>()
        }
        invalidateMatchesCache()
        return result
    }

    suspend fun updateToss(
        matchId: String,
        tossWinnerId: String,
        tossDecision: String,
        battingFirstId: String
    ): Match {
        val result = SupabaseClient.withRetry {
            client.postgrest["matches"]
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
        matchCache[matchId] = result
        matchesCache = null
        matchesCacheTime = 0L
        return result
    }

    suspend fun getPlayingXI(matchId: String): List<PlayingXI> {
        return xiCache.getOrPut(matchId) {
            SupabaseClient.withRetry {
                client.postgrest["playing_xi"]
                    .select { filter { eq("match_id", matchId) } }
                    .decodeList()
            }
        }
    }

    fun invalidateXICache(matchId: String) {
        xiCache.remove(matchId)
    }

    suspend fun deleteMatch(matchId: String) {
        SupabaseClient.withRetry {
            client.postgrest["matches"].delete { filter { eq("id", matchId) } }
        }
        invalidateMatchCache(matchId)
        invalidateMatchesCache()
        invalidateXICache(matchId)
    }

    suspend fun insertPlayingXI(players: List<PlayingXIInsert>) {
        if (players.isEmpty()) return
        val matchId = players.first().matchId
        val teamId = players.first().teamId
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
        SupabaseClient.withRetry {
            client.postgrest["playing_xi"].insert(players)
        }
        xiCache.remove(matchId)
    }
}