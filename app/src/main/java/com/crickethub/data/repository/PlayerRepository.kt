package com.crickethub.data.repository

import com.crickethub.data.model.Ball
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.model.PlayerUpdate
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class PlayerRepository {

    private val client = SupabaseClient.client
    private val playersCache = mutableMapOf<String, List<Player>>()

    suspend fun getPlayersByTeam(teamId: String): List<Player> {
        return playersCache.getOrPut(teamId) {
            client.postgrest["players"]
                .select { filter { eq("team_id", teamId) } }
                .decodeList()
        }
    }

    suspend fun getPlayerById(playerId: String): Player? {
        return client.postgrest["players"]
            .select { filter { eq("id", playerId) } }
            .decodeSingleOrNull()
    }

    suspend fun createPlayer(player: PlayerInsert): Player {
        val userId = client.auth.currentUserOrNull()?.id
        val result = client.postgrest["players"]
            .insert(player.copy(userId = userId)) { select() }
            .decodeSingle<Player>()
        playersCache.remove(player.teamId)
        return result
    }

    suspend fun updatePlayer(playerId: String, update: PlayerUpdate): Player {
        val result = client.postgrest["players"]
            .update(update) {
                filter { eq("id", playerId) }
                select()
            }
            .decodeSingle<Player>()
        playersCache.clear()
        return result
    }

    suspend fun deletePlayer(playerId: String) {
        client.postgrest["players"]
            .delete { filter { eq("id", playerId) } }
        playersCache.clear()
    }

    suspend fun computePlayerStats(playerId: String): PlayerStats {
        return try {
            coroutineScope {
                val batBallsDeferred = async {
                    client.postgrest["balls"]
                        .select { filter { eq("batsman_id", playerId) } }
                        .decodeList<Ball>()
                }
                val bowlBallsDeferred = async {
                    client.postgrest["balls"]
                        .select { filter { eq("bowler_id", playerId) } }
                        .decodeList<Ball>()
                }
                val catchBallsDeferred = async {
                    try {
                        client.postgrest["balls"]
                            .select { filter { eq("fielder_id", playerId); eq("wicket_type", "caught") } }
                            .decodeList<Ball>()
                    } catch (e: Exception) { emptyList() }
                }
                val runOutBallsDeferred = async {
                    try {
                        client.postgrest["balls"]
                            .select { filter { eq("fielder_id", playerId); eq("wicket_type", "run_out") } }
                            .decodeList<Ball>()
                    } catch (e: Exception) { emptyList() }
                }
                val stumpingBallsDeferred = async {
                    try {
                        client.postgrest["balls"]
                            .select { filter { eq("fielder_id", playerId); eq("wicket_type", "stumped") } }
                            .decodeList<Ball>()
                    } catch (e: Exception) { emptyList() }
                }

                val battingBalls = batBallsDeferred.await()
                val bowlingBalls = bowlBallsDeferred.await()
                val catchBalls = catchBallsDeferred.await()
                val runOutBalls = runOutBallsDeferred.await()
                val stumpingBalls = stumpingBallsDeferred.await()

                // Batting stats
                val runs = battingBalls.sumOf { it.runsOffBat }
                val ballsFaced = battingBalls.count { it.extrasType != "wide" }
                val fours = battingBalls.count { it.isBoundary && !it.isSix }
                val sixes = battingBalls.count { it.isSix }
                val sr = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100 else 0.0
                val inningsGroups = battingBalls.groupBy { it.inningsId }
                val innings = inningsGroups.size
                val inningsRuns = inningsGroups.values.map { b -> b.sumOf { it.runsOffBat } }
                val notOuts = inningsGroups.values.count { b -> b.none { it.isWicket && it.wicketType != "run_out" && it.wicketType != "retired_hurt" } }
                val ducks = inningsRuns.count { it == 0 }
                val fifties = inningsRuns.count { it in 50..99 }
                val hundreds = inningsRuns.count { it >= 100 }
                val highestScore = inningsRuns.maxOrNull() ?: 0
                val dismissals = innings - notOuts
                val avg = if (dismissals > 0) runs.toDouble() / dismissals else runs.toDouble()
                val boundaryRuns = (fours * 4) + (sixes * 6)
                val totalBatRuns = battingBalls.sumOf { it.runsOffBat }
                val boundaryPct = if (totalBatRuns > 0) boundaryRuns * 100.0 / totalBatRuns else 0.0

                // Bowling stats
                val legalBalls = bowlingBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                val oversBowled = legalBalls / 6.0
                val runsConceded = bowlingBalls.sumOf { ball ->
                    when (ball.extrasType) { "bye", "leg_bye" -> 0; else -> ball.runsOffBat + (ball.extrasRuns ?: 0) }
                }
                val wickets = bowlingBalls.count {
                    it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "handled_ball", "timed_out", "retired_hurt", "retired_out")
                }
                val economy = if (legalBalls > 0) (runsConceded.toDouble() / legalBalls) * 6 else 0.0
                val bowlingAvg = if (wickets > 0) runsConceded.toDouble() / wickets else 0.0
                val bsr = if (wickets > 0) legalBalls.toDouble() / wickets else 0.0
                val wides = bowlingBalls.count { it.extrasType == "wide" }
                val noBalls = bowlingBalls.count { it.extrasType == "no_ball" }
                val dotBalls = bowlingBalls.count { it.runsOffBat == 0 && it.extrasRuns == null && it.extrasType == null }
                val bowlingInnings = bowlingBalls.groupBy { it.inningsId }
                val bestBowling = bowlingInnings.values.maxByOrNull { b ->
                    b.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") }
                }?.let { b ->
                    val w = b.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") }
                    val r = b.sumOf { ball -> when (ball.extrasType) { "bye", "leg_bye" -> 0; else -> ball.runsOffBat + (ball.extrasRuns ?: 0) } }
                    "$w/$r"
                } ?: "0/0"
                val fiveWickets = bowlingInnings.values.count { b -> b.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") } >= 5 }
                val threeWickets = bowlingInnings.values.count { b -> b.count { it.isWicket && it.wicketType !in listOf("run_out", "retired_hurt") } >= 3 }
                val maidens = bowlingBalls.groupBy { it.overNo }.values.count { ob ->
                    ob.count { it.extrasType != "wide" && it.extrasType != "no_ball" } == 6 &&
                            ob.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) } == 0
                }

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
                    maidens = maidens,
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
                    catches = catchBalls.size,
                    runOuts = runOutBalls.size,
                    stumpings = stumpingBalls.size
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "PlayerStats error: ${e.message}", e)
            PlayerStats()
        }
    }
}