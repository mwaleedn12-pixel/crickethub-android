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

    // Cache
    private val inningsCache = mutableMapOf<String, List<Innings>>()
    private val ballsCache = mutableMapOf<String, List<Ball>>()
    private val playersCache = mutableMapOf<String, List<Player>>()

    suspend fun getInningsByMatch(matchId: String): List<Innings> {
        return inningsCache.getOrPut(matchId) {
            client.postgrest["innings"]
                .select { filter { eq("match_id", matchId) } }
                .decodeList<Innings>()
                .sortedBy { it.inningsNo }
        }
    }

    fun invalidateInningsCache(matchId: String) {
        inningsCache.remove(matchId)
    }

    suspend fun getBallsByInnings(inningsId: String): List<Ball> {
        return ballsCache.getOrPut(inningsId) {
            client.postgrest["balls"]
                .select { filter { eq("innings_id", inningsId) } }
                .decodeList<Ball>()
                .sortedWith(compareBy({ it.overNo }, { it.ballNo }))
        }
    }

    fun invalidateBallsCache(inningsId: String) {
        ballsCache.remove(inningsId)
    }

    suspend fun getPlayingXIPlayers(matchId: String, teamId: String): List<Player> {
        val key = "$matchId-$teamId"
        return playersCache.getOrPut(key) {
            val playingXI = client.postgrest["playing_xi"]
                .select {
                    filter {
                        eq("match_id", matchId)
                        eq("team_id", teamId)
                    }
                }
                .decodeList<com.crickethub.data.model.PlayingXI>()

            if (playingXI.isEmpty()) return@getOrPut emptyList()

            val playerIds = playingXI.map { it.playerId }
            client.postgrest["players"]
                .select { filter { isIn("id", playerIds) } }
                .decodeList<Player>()
                .sortedBy { p -> playingXI.indexOfFirst { it.playerId == p.id } }
        }
    }

    fun invalidatePlayersCache(matchId: String) {
        playersCache.keys.filter { it.startsWith(matchId) }.forEach { playersCache.remove(it) }
    }

    suspend fun createInnings(innings: InningsInsert): Innings {
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
        // Invalidate innings cache
        ballsCache.remove(inningsId)
        return result
    }

    suspend fun completeInnings(inningsId: String) {
        client.postgrest["innings"]
            .update({ set("status", "completed") }) {
                filter { eq("id", inningsId) }
            }
        ballsCache.remove(inningsId)
    }

    suspend fun insertBall(ball: BallInsert): Ball {
        val result = client.postgrest["balls"]
            .insert(ball) { select() }
            .decodeSingle<Ball>()
        // Invalidate balls cache
        ballsCache.remove(ball.inningsId)
        return result
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