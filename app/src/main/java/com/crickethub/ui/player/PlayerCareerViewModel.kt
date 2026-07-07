package com.crickethub.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.PlayerRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CareerBattingStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val runs: Int = 0,
    val balls: Int = 0,
    val highScore: Int = 0,
    val average: Double = 0.0,
    val strikeRate: Double = 0.0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val ducks: Int = 0,
    val notOuts: Int = 0
)

data class CareerBowlingStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val balls: Int = 0,
    val runs: Int = 0,
    val wickets: Int = 0,
    val bestFigures: String = "0/0",
    val average: Double = 0.0,
    val economy: Double = 0.0,
    val strikeRate: Double = 0.0,
    val fiveWickets: Int = 0,
    val maidens: Int = 0
)

data class InningsForm(
    val matchId: String,
    val runs: Int,
    val balls: Int,
    val isOut: Boolean,
    val strikeRate: Double
)

data class PlayerCareerUiState(
    val isLoading: Boolean = true,
    val player: Player? = null,
    val battingStats: CareerBattingStats = CareerBattingStats(),
    val bowlingStats: CareerBowlingStats = CareerBowlingStats(),
    val recentForm: List<InningsForm> = emptyList(),
    val error: String? = null,
    val allPlayers: List<Player> = emptyList()
)

class PlayerCareerViewModel : ViewModel() {

    private val playerRepository = PlayerRepository()

    private val _uiState = MutableStateFlow(PlayerCareerUiState())
    val uiState: StateFlow<PlayerCareerUiState> = _uiState.asStateFlow()

    fun loadAllPlayers() {
        viewModelScope.launch {
            try {
                val teams = SupabaseClient.client.postgrest["teams"]
                    .select()
                    .decodeList<Team>()

                val allPlayers = mutableListOf<Player>()
                teams.forEach { team ->
                    val players = playerRepository.getPlayersByTeam(team.id)
                    allPlayers.addAll(players)
                }
                _uiState.update { it.copy(allPlayers = allPlayers, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Load players error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadPlayerCareer(playerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val player = playerRepository.getPlayerById(playerId) ?: return@launch

                val battingBalls = SupabaseClient.client.postgrest["balls"]
                    .select { filter { eq("batsman_id", playerId) } }
                    .decodeList<Ball>()

                val bowlingBalls = SupabaseClient.client.postgrest["balls"]
                    .select { filter { eq("bowler_id", playerId) } }
                    .decodeList<Ball>()

                val battingStats = computeBattingStats(battingBalls)
                val bowlingStats = computeBowlingStats(bowlingBalls)
                val recentForm = computeRecentForm(battingBalls)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        player = player,
                        battingStats = battingStats,
                        bowlingStats = bowlingStats,
                        recentForm = recentForm
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Career error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun computeBattingStats(balls: List<Ball>): CareerBattingStats {
        if (balls.isEmpty()) return CareerBattingStats()

        val inningsGroups = balls.groupBy { it.inningsId }
        val innings = inningsGroups.size

        val totalRuns = balls.sumOf { it.runsOffBat }
        val totalBalls = balls.count { it.extrasType != "wide" }
        val fours = balls.count { it.isBoundary && !it.isSix }
        val sixes = balls.count { it.isSix }

        val inningsRuns = inningsGroups.map { (_, inningsBalls) ->
            val runs = inningsBalls.sumOf { it.runsOffBat }
            val isOut = inningsBalls.any { it.isWicket && it.wicketType != "run_out" }
            Pair(runs, isOut)
        }

        val dismissals = inningsRuns.count { it.second }
        val notOuts = innings - dismissals
        val highScore = inningsRuns.maxOfOrNull { it.first } ?: 0
        val average = if (dismissals > 0) totalRuns.toDouble() / dismissals else totalRuns.toDouble()
        val strikeRate = if (totalBalls > 0) (totalRuns.toDouble() / totalBalls) * 100 else 0.0
        val fifties = inningsRuns.count { it.first in 50..99 }
        val hundreds = inningsRuns.count { it.first >= 100 }
        val ducks = inningsRuns.count { it.first == 0 && it.second }

        return CareerBattingStats(
            matches    = innings,
            innings    = innings,
            runs       = totalRuns,
            balls      = totalBalls,
            highScore  = highScore,
            average    = average,
            strikeRate = strikeRate,
            fours      = fours,
            sixes      = sixes,
            fifties    = fifties,
            hundreds   = hundreds,
            ducks      = ducks,
            notOuts    = notOuts
        )
    }

    private fun computeBowlingStats(balls: List<Ball>): CareerBowlingStats {
        if (balls.isEmpty()) return CareerBowlingStats()

        val inningsGroups = balls.groupBy { it.inningsId }
        val innings = inningsGroups.size

        val legalBalls = balls.count {
            it.extrasType != "wide" && it.extrasType != "no_ball"
        }
        val totalRuns = balls.sumOf { ball ->
            when (ball.extrasType) {
                "bye", "leg_bye" -> 0
                else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
            }
        }
        val wickets = balls.count {
            it.isWicket && it.wicketType !in listOf(
                "run_out", "obstructing", "handled_ball", "timed_out"
            )
        }

        val economy   = if (legalBalls > 0) (totalRuns.toDouble() / legalBalls) * 6 else 0.0
        val average   = if (wickets > 0) totalRuns.toDouble() / wickets else 0.0
        val bowlingSR = if (wickets > 0) legalBalls.toDouble() / wickets else 0.0

        val inningsWickets = inningsGroups.map { (_, inningsBalls) ->
            val w = inningsBalls.count {
                it.isWicket && it.wicketType !in listOf("run_out", "obstructing")
            }
            val r = inningsBalls.sumOf { ball ->
                when (ball.extrasType) {
                    "bye", "leg_bye" -> 0
                    else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
                }
            }
            Pair(w, r)
        }

        val bestFigures  = inningsWickets.maxByOrNull { it.first }
            ?.let { "${it.first}/${it.second}" } ?: "0/0"
        val fiveWickets  = inningsWickets.count { it.first >= 5 }

        return CareerBowlingStats(
            matches     = innings,
            innings     = innings,
            balls       = legalBalls,
            runs        = totalRuns,
            wickets     = wickets,
            bestFigures = bestFigures,
            average     = average,
            economy     = economy,
            strikeRate  = bowlingSR,
            fiveWickets = fiveWickets,
            maidens     = 0
        )
    }

    private fun computeRecentForm(balls: List<Ball>): List<InningsForm> {
        val inningsGroups = balls.groupBy { it.inningsId }
        return inningsGroups.entries
            .toList()
            .takeLast(10)
            .map { (inningsId, inningsBalls) ->
                val runs      = inningsBalls.sumOf { it.runsOffBat }
                val ballsFaced = inningsBalls.count { it.extrasType != "wide" }
                val isOut     = inningsBalls.any { it.isWicket && it.wicketType != "run_out" }
                val sr        = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100 else 0.0
                InningsForm(inningsId, runs, ballsFaced, isOut, sr)
            }
            .reversed()
    }
}