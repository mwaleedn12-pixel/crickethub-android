package com.crickethub.data.repository

import com.crickethub.data.model.Ball
import com.crickethub.data.model.BallInsert
import com.crickethub.data.model.Innings
import com.crickethub.data.model.InningsInsert
import com.crickethub.data.model.Player
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ScoringRepository {

    private val client = SupabaseClient.client

    // Cache with TTL (5 min for players, always-fresh for innings/balls)
    private val inningsCache = mutableMapOf<String, List<Innings>>()
    private val ballsCache = mutableMapOf<String, List<Ball>>()
    private data class CachedPlayers(val players: List<Player>, val timestamp: Long)
    private val playersCache = mutableMapOf<String, CachedPlayers>()
    private val PLAYERS_TTL_MS = 5 * 60 * 1000L  // 5 minutes

    suspend fun getInningsByMatch(matchId: String): List<Innings> {
        return SupabaseClient.withRetry {
            val fresh = client.postgrest["innings"]
                .select { filter { eq("match_id", matchId) } }
                .decodeList<Innings>().sortedBy { it.inningsNo }
            inningsCache[matchId] = fresh
            fresh
        }
    }

    fun invalidateInningsCache(matchId: String) {
        inningsCache.remove(matchId)
    }

    suspend fun getBallsByInnings(inningsId: String): List<Ball> {
        return SupabaseClient.withRetry {
            val fresh = client.postgrest["balls"]
                .select { filter { eq("innings_id", inningsId) } }
                .decodeList<Ball>().sortedBy { it.createdAt ?: "" }
            ballsCache[inningsId] = fresh
            fresh
        }
    }

    fun invalidateBallsCache(inningsId: String) {
        ballsCache.remove(inningsId)
    }

    suspend fun getPlayingXIPlayers(matchId: String, teamId: String): List<Player> {
        val key = "$matchId-$teamId"
        val cached = playersCache[key]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < PLAYERS_TTL_MS) {
            return cached.players
        }
        return SupabaseClient.withRetry {
            val playingXI = client.postgrest["playing_xi"]
                .select {
                    filter {
                        eq("match_id", matchId)
                        eq("team_id", teamId)
                    }
                }
                .decodeList<com.crickethub.data.model.PlayingXI>()

            if (playingXI.isEmpty()) return@withRetry emptyList<Player>()

            val playerIds = playingXI.map { it.playerId }
            val players = client.postgrest["players"]
                .select { filter { isIn("id", playerIds) } }
                .decodeList<Player>()
                .sortedBy { p -> playingXI.indexOfFirst { it.playerId == p.id } }
            playersCache[key] = CachedPlayers(players, System.currentTimeMillis())
            players
        }
    }

    fun invalidatePlayersCache(matchId: String) {
        playersCache.keys.filter { it.startsWith(matchId) }.forEach { playersCache.remove(it) }
    }

    suspend fun createInnings(innings: InningsInsert): Innings {
        val existing = try {
            client.postgrest["innings"].select { filter {
                eq("match_id", innings.matchId)
                eq("innings_no", innings.inningsNo)
            }}.decodeSingleOrNull<Innings>()
        } catch (e: Exception) { null }
        if (existing != null) return existing
        val result = client.postgrest["innings"]
            .insert(innings) { select() }
            .decodeSingle<Innings>()
        inningsCache.remove(innings.matchId)
        return result
    }

    suspend fun updateInnings(
        inningsId: String,
        totalRuns: Int,
        totalWickets: Int,
        totalBalls: Int,
        extrasTotal: Int,
        wides: Int,
        noBalls: Int
    ): Innings {
        return SupabaseClient.withRetry {
            val result = client.postgrest["innings"]
                .update({
                    set("total_runs", totalRuns)
                    set("total_wickets", totalWickets)
                    set("total_balls", totalBalls)
                    set("extras_total", extrasTotal)
                    set("wides", wides)
                    set("no_balls", noBalls)
                }) {
                    filter { eq("id", inningsId) }
                    select()
                }
                .decodeSingle<Innings>()
            ballsCache.remove(inningsId)
            result
        }
    }

    suspend fun completeInnings(inningsId: String) {
        SupabaseClient.withRetry {
            client.postgrest["innings"]
                .update({ set("status", "completed") }) {
                    filter { eq("id", inningsId) }
                }
        }
        ballsCache.remove(inningsId)
    }

    suspend fun insertBall(ball: BallInsert): Ball {
        return SupabaseClient.withRetry {
            val result = client.postgrest["balls"]
                .insert(ball) { select() }
                .decodeSingle<Ball>()
            ballsCache.remove(ball.inningsId)
            result
        }
    }

    suspend fun deleteLastBall(ballId: String) {
        // Find which innings this ball belongs to
        val ball = client.postgrest["balls"]
            .select { filter { eq("id", ballId) } }
            .decodeSingleOrNull<Ball>()
        client.postgrest["balls"]
            .delete { filter { eq("id", ballId) } }
        ball?.let { ballsCache.remove(it.inningsId) }
    }
}