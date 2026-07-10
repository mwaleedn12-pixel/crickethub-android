package com.crickethub.data.repository

import com.crickethub.data.model.Ball
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.model.PlayerUpdate
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class PlayerRepository {

    private val client = SupabaseClient.client

    suspend fun getPlayersByTeam(teamId: String): List<Player> {
        return client.postgrest["players"]
            .select { filter { eq("team_id", teamId) } }
            .decodeList()
    }

    suspend fun getPlayerById(playerId: String): Player? {
        return client.postgrest["players"]
            .select { filter { eq("id", playerId) } }
            .decodeSingleOrNull()
    }

    suspend fun createPlayer(player: PlayerInsert): Player {
        return client.postgrest["players"]
            .insert(player) { select() }
            .decodeSingle()
    }

    suspend fun updatePlayer(playerId: String, update: PlayerUpdate): Player {
        return client.postgrest["players"]
            .update(update) {
                filter { eq("id", playerId) }
                select()
            }
            .decodeSingle()
    }

    suspend fun deletePlayer(playerId: String) {
        client.postgrest["players"]
            .delete { filter { eq("id", playerId) } }
    }

    suspend fun computePlayerStats(playerId: String): PlayerStats {
        return try {
            val battingBalls = client.postgrest["balls"]
                .select { filter { eq("batsman_id", playerId) } }
                .decodeList<Ball>()

            val runs = battingBalls.sumOf { it.runsOffBat }
            val ballsFaced = battingBalls.count { it.extrasType != "wide" }
            val fours = battingBalls.count { it.isBoundary && !it.isSix }
            val sixes = battingBalls.count { it.isSix }
            val sr = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100 else 0.0

            val inningsGroups = battingBalls.groupBy { it.inningsId }
            val innings = inningsGroups.size
            val inningsRuns = inningsGroups.values.map { balls -> balls.sumOf { it.runsOffBat } }
            val notOuts = inningsGroups.values.count { balls ->
                balls.none { it.isWicket && it.wicketType != "run_out" && it.wicketType != "retired_hurt" }
            }
            val ducks = inningsRuns.count { it == 0 }
            val fifties = inningsRuns.count { it in 50..99 }
            val hundreds = inningsRuns.count { it >= 100 }
            val highestScore = inningsRuns.maxOrNull() ?: 0
            val dismissals = innings - notOuts
            val avg = if (dismissals > 0) runs.toDouble() / dismissals else runs.toDouble()

            val bowlingBalls = client.postgrest["balls"]
                .select { filter { eq("bowler_id", playerId) } }
                .decodeList<Ball>()

            val legalBalls = bowlingBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
            val oversBowled = legalBalls / 6.0
            val runsConceded = bowlingBalls.sumOf { ball ->
                when (ball.extrasType) { "bye", "leg_bye" -> 0; else -> ball.runsOffBat + (ball.extrasRuns ?: 0) }
            }
            val wickets = bowlingBalls.count {
                it.isWicket && it.wicketType !in listOf(
                    "run_out", "obstructing", "handled_ball", "timed_out", "retired_hurt", "retired_out"
                )
            }
            val economy = if (legalBalls > 0) (runsConceded.toDouble() / legalBalls) * 6 else 0.0
            val bowlingAvg = if (wickets > 0) runsConceded.toDouble() / wickets else 0.0
            val bsr = if (wickets > 0) legalBalls.toDouble() / wickets else 0.0
            val wides = bowlingBalls.count { it.extrasType == "wide" }
            val noBalls = bowlingBalls.count { it.extrasType == "no_ball" }
            val dotBalls = bowlingBalls.count {
                it.runsOffBat == 0 && it.extrasRuns == null && it.extrasType == null
            }

            val bowlingInnings = bowlingBalls.groupBy { it.inningsId }
            val bestBowling = bowlingInnings.values.maxByOrNull { balls ->
                balls.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") }
            }?.let { balls ->
                val w = balls.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") }
                val r = balls.sumOf { ball ->
                    when (ball.extrasType) { "bye", "leg_bye" -> 0; else -> ball.runsOffBat + (ball.extrasRuns ?: 0) }
                }
                "$w/$r"
            } ?: "0/0"

            val fiveWickets = bowlingInnings.values.count { balls ->
                balls.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") } >= 5
            }
            val threeWickets = bowlingInnings.values.count { balls ->
                balls.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") } >= 3
            }

            val catches = try {
                client.postgrest["balls"]
                    .select { filter { eq("fielder_id", playerId); eq("wicket_type", "caught") } }
                    .decodeList<Ball>().size
            } catch (e: Exception) { 0 }

            val runOuts = try {
                client.postgrest["balls"]
                    .select { filter { eq("fielder_id", playerId); eq("wicket_type", "run_out") } }
                    .decodeList<Ball>().size
            } catch (e: Exception) { 0 }

            val stumpings = try {
                client.postgrest["balls"]
                    .select { filter { eq("fielder_id", playerId); eq("wicket_type", "stumped") } }
                    .decodeList<Ball>().size
            } catch (e: Exception) { 0 }

            val boundaryRuns = (fours * 4) + (sixes * 6)
            val boundaryPct = if (runs > 0) (boundaryRuns.toDouble() / runs) * 100 else 0.0

            PlayerStats(
                matches = innings,
                innings = innings,
                runs = runs,
                ballsFaced = ballsFaced,
                highestScore = highestScore,
                average = avg,
                strikeRate = sr,
                fifties = fifties,
                hundreds = hundreds,
                ducks = ducks,
                fours = fours,
                sixes = sixes,
                notOuts = notOuts,
                boundaryPercent = boundaryPct,
                oversBowled = oversBowled,
                runsConceded = runsConceded,
                wickets = wickets,
                economy = economy,
                bowlingAverage = bowlingAvg,
                bowlingStrikeRate = bsr,
                bestBowling = bestBowling,
                threeWicketHauls = threeWickets,
                fiveWicketHauls = fiveWickets,
                dotBalls = dotBalls,
                wides = wides,
                noBalls = noBalls,
                catches = catches,
                runOuts = runOuts,
                stumpings = stumpings
            )
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "PlayerStats error: ${e.message}", e)
            PlayerStats()
        }
    }
}