package com.crickethub.data.repository

import com.crickethub.data.model.Ball
import com.crickethub.data.model.BallInsert
import com.crickethub.data.model.Innings
import com.crickethub.data.model.InningsInsert
import com.crickethub.data.model.Player
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class ScoringRepository {

    suspend fun createInnings(innings: InningsInsert): Innings {
        return SupabaseClient.client.postgrest["innings"]
            .insert(innings) { select() }
            .decodeSingle<Innings>()
    }

    suspend fun getInningsByMatch(matchId: String): List<Innings> {
        return SupabaseClient.client.postgrest["innings"]
            .select {
                filter { eq("match_id", matchId) }
                order("innings_no", Order.ASCENDING)
            }
            .decodeList<Innings>()
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
        return SupabaseClient.client.postgrest["innings"]
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
    }

    suspend fun completeInnings(inningsId: String): Innings {
        return SupabaseClient.client.postgrest["innings"]
            .update({ set("status", "completed") }) {
                filter { eq("id", inningsId) }
                select()
            }
            .decodeSingle<Innings>()
    }

    suspend fun insertBall(ball: BallInsert): Ball {
        return SupabaseClient.client.postgrest["balls"]
            .insert(ball) { select() }
            .decodeSingle<Ball>()
    }

    suspend fun getBallsByInnings(inningsId: String): List<Ball> {
        return SupabaseClient.client.postgrest["balls"]
            .select {
                filter { eq("innings_id", inningsId) }
                order("over_no", Order.ASCENDING)
                order("ball_no", Order.ASCENDING)
            }
            .decodeList<Ball>()
    }

    suspend fun deleteLastBall(ballId: String) {
        SupabaseClient.client.postgrest["balls"]
            .delete {
                filter { eq("id", ballId) }
            }
    }

    suspend fun getPlayingXIPlayers(matchId: String, teamId: String): List<Player> {
        val xiList = SupabaseClient.client.postgrest["playing_xi"]
            .select {
                filter {
                    eq("match_id", matchId)
                    eq("team_id", teamId)
                }
            }
            .decodeList<com.crickethub.data.model.PlayingXIInsert>()

        val playerIds = xiList.map { it.playerId }
        if (playerIds.isEmpty()) return emptyList()

        return SupabaseClient.client.postgrest["players"]
            .select {
                filter {
                    isIn("id", playerIds)
                }
            }
            .decodeList<Player>()
    }

    suspend fun updateMatchStatus(matchId: String, status: String) {
        SupabaseClient.client.postgrest["matches"]
            .update({ set("status", status) }) {
                filter { eq("id", matchId) }
            }
    }
}