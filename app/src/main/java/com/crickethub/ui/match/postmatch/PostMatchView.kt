package com.crickethub.ui.match.postmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Innings
import com.crickethub.data.model.Match
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.MatchRepository
import com.crickethub.data.repository.ScoringRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatsmanScorecard(
    val player: Player,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val strikeRate: Double,
    val isOut: Boolean,
    val dismissalType: String?
)

data class BowlerScorecard(
    val player: Player,
    val overs: String,
    val maidens: Int,
    val runs: Int,
    val wickets: Int,
    val economy: Double,
    val wides: Int,
    val noBalls: Int
)

data class MotmCandidate(
    val player: Player,
    val score: Double,
    val runs: Int,
    val wickets: Int,
    val strikeRate: Double,
    val economy: Double,
    val catches: Int
)

data class PostMatchUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val match: Match? = null,
    val innings1: Innings? = null,
    val innings2: Innings? = null,
    val team1: Team? = null,
    val team2: Team? = null,
    val innings1Batting: List<BatsmanScorecard> = emptyList(),
    val innings1Bowling: List<BowlerScorecard> = emptyList(),
    val innings2Batting: List<BatsmanScorecard> = emptyList(),
    val innings2Bowling: List<BowlerScorecard> = emptyList(),
    val motmCandidates: List<MotmCandidate> = emptyList(),
    val selectedMotm: Player? = null,
    val resultText: String = "",
    val matchSaved: Boolean = false
)

class PostMatchViewModel : ViewModel() {

    private val scoringRepository = ScoringRepository()
    private val matchRepository = MatchRepository()

    private val _uiState = MutableStateFlow(PostMatchUiState())
    val uiState: StateFlow<PostMatchUiState> = _uiState.asStateFlow()

    fun loadPostMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val innings1 = allInnings.firstOrNull { it.inningsNo == 1 }
                val innings2 = allInnings.firstOrNull { it.inningsNo == 2 }

                val balls1 = if (innings1 != null)
                    scoringRepository.getBallsByInnings(innings1.id) else emptyList()
                val balls2 = if (innings2 != null)
                    scoringRepository.getBallsByInnings(innings2.id) else emptyList()

                val team1 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team1Id) } }
                    .decodeSingleOrNull<Team>()

                val team2 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team2Id) } }
                    .decodeSingleOrNull<Team>()

                // ✅ FIX: innings ki batting/bowling team IDs se players fetch karo
                val innings1BattingTeamId = innings1?.battingTeamId ?: match.battingFirstId ?: match.team1Id
                val innings1BowlingTeamId = innings1?.bowlingTeamId ?: if (innings1BattingTeamId == match.team1Id) match.team2Id else match.team1Id

                val innings2BattingTeamId = innings2?.battingTeamId ?: innings1BowlingTeamId
                val innings2BowlingTeamId = innings2?.bowlingTeamId ?: innings1BattingTeamId

                // Playing XI players fetch karo
                val inn1BatPlayers = scoringRepository.getPlayingXIPlayers(matchId, innings1BattingTeamId)
                val inn1BowlPlayers = scoringRepository.getPlayingXIPlayers(matchId, innings1BowlingTeamId)
                val inn2BatPlayers = scoringRepository.getPlayingXIPlayers(matchId, innings2BattingTeamId)
                val inn2BowlPlayers = scoringRepository.getPlayingXIPlayers(matchId, innings2BowlingTeamId)

                android.util.Log.d("CricketHub", "Inn1 bat players: ${inn1BatPlayers.size}, Inn1 bowl players: ${inn1BowlPlayers.size}")
                android.util.Log.d("CricketHub", "Inn2 bat players: ${inn2BatPlayers.size}, Inn2 bowl players: ${inn2BowlPlayers.size}")

                val innings1Batting = computeBattingScorecard(balls1, inn1BatPlayers)
                val innings1Bowling = computeBowlingScorecard(balls1, inn1BowlPlayers)
                val innings2Batting = computeBattingScorecard(balls2, inn2BatPlayers)
                val innings2Bowling = computeBowlingScorecard(balls2, inn2BowlPlayers)

                val allPlayers = inn1BatPlayers + inn1BowlPlayers
                val motmCandidates = computeMotmCandidates(
                    allPlayers.distinctBy { it.id },
                    balls1, balls2
                )

                val resultText = computeResultText(match, innings1, innings2, team1, team2)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        match = match,
                        innings1 = innings1,
                        innings2 = innings2,
                        team1 = team1,
                        team2 = team2,
                        innings1Batting = innings1Batting,
                        innings1Bowling = innings1Bowling,
                        innings2Batting = innings2Batting,
                        innings2Bowling = innings2Bowling,
                        motmCandidates = motmCandidates,
                        selectedMotm = motmCandidates.firstOrNull()?.player,
                        resultText = resultText
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "PostMatch error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectMotm(player: Player) {
        _uiState.update { it.copy(selectedMotm = player) }
    }

    fun saveMatchResult(matchId: String) {
        viewModelScope.launch {
            try {
                val resultText = _uiState.value.resultText
                SupabaseClient.client.postgrest["matches"]
                    .update({
                        set("status", "completed")
                        set("result_text", resultText)
                    }) {
                        filter { eq("id", matchId) }
                    }
                _uiState.update { it.copy(matchSaved = true) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Save result error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun computeBattingScorecard(
        balls: List<Ball>,
        players: List<Player>
    ): List<BatsmanScorecard> {
        if (players.isEmpty()) return emptyList()

        // Sirf un players ko show karo jinhone actually bat kiya ya out hue
        val batsmanIds = balls.map { it.batsmanId }.toSet()

        return players.map { player ->
            val playerBalls = balls.filter { it.batsmanId == player.id }
            val runs = playerBalls.sumOf { it.runsOffBat }
            val ballsFaced = playerBalls.count { it.extrasType != "wide" }
            val fours = playerBalls.count { it.isBoundary && !it.isSix }
            val sixes = playerBalls.count { it.isSix }
            val isOut = playerBalls.any { it.isWicket && it.wicketType != "run_out" }
            val dismissalType = playerBalls.firstOrNull { it.isWicket }?.wicketType
            val sr = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100 else 0.0

            BatsmanScorecard(player, runs, ballsFaced, fours, sixes, sr, isOut, dismissalType)
        }.sortedByDescending { it.runs }
    }

    private fun computeBowlingScorecard(
        balls: List<Ball>,
        players: List<Player>
    ): List<BowlerScorecard> {
        if (players.isEmpty()) return emptyList()

        return players.mapNotNull { player ->
            val playerBalls = balls.filter { it.bowlerId == player.id }
            if (playerBalls.isEmpty()) return@mapNotNull null

            val legalBalls = playerBalls.count {
                it.extrasType != "wide" && it.extrasType != "no_ball"
            }
            val overs = "${legalBalls / 6}.${legalBalls % 6}"
            val runs = playerBalls.sumOf { ball ->
                when (ball.extrasType) {
                    "bye", "leg_bye" -> 0
                    else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
                }
            }
            val wickets = playerBalls.count {
                it.isWicket && it.wicketType !in listOf(
                    "run_out", "obstructing", "handled_ball", "timed_out"
                )
            }
            val economy = if (legalBalls > 0) (runs.toDouble() / legalBalls) * 6 else 0.0
            val wides = playerBalls.count { it.extrasType == "wide" }
            val noBalls = playerBalls.count { it.extrasType == "no_ball" }

            val overGroups = playerBalls.groupBy { it.overNo }
            val maidens = overGroups.values.count { overBalls ->
                overBalls.count {
                    it.extrasType != "wide" && it.extrasType != "no_ball"
                } == 6 &&
                        overBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) } == 0
            }

            BowlerScorecard(player, overs, maidens, runs, wickets, economy, wides, noBalls)
        }.sortedByDescending { it.wickets }
    }

    private fun computeMotmCandidates(
        allPlayers: List<Player>,
        balls1: List<Ball>,
        balls2: List<Ball>
    ): List<MotmCandidate> {
        val allBalls = balls1 + balls2

        return allPlayers.mapNotNull { player ->
            val battingBalls = allBalls.filter { it.batsmanId == player.id }
            val bowlingBalls = allBalls.filter { it.bowlerId == player.id }

            val runs = battingBalls.sumOf { it.runsOffBat }
            val ballsFaced = battingBalls.count { it.extrasType != "wide" }.coerceAtLeast(1)
            val strikeRate = (runs.toDouble() / ballsFaced) * 100

            val legalBallsBowled = bowlingBalls.count {
                it.extrasType != "wide" && it.extrasType != "no_ball"
            }
            val runsConceded = bowlingBalls.sumOf { ball ->
                when (ball.extrasType) {
                    "bye", "leg_bye" -> 0
                    else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
                }
            }
            val wickets = bowlingBalls.count {
                it.isWicket && it.wicketType !in listOf("run_out", "obstructing")
            }
            val economy = if (legalBallsBowled > 0)
                (runsConceded.toDouble() / legalBallsBowled) * 6 else 99.0
            val oversBowled = legalBallsBowled / 6

            if (runs == 0 && wickets == 0) return@mapNotNull null

            var score = runs.toDouble()
            score += when {
                strikeRate > 150 -> 15.0
                strikeRate > 120 -> 8.0
                else -> 0.0
            }
            score += when {
                runs >= 100 -> 25.0
                runs >= 50 -> 10.0
                else -> 0.0
            }
            score += wickets * 25.0
            if (wickets >= 5) score += 20.0
            if (oversBowled >= 2) {
                score += when {
                    economy < 6.0 -> 15.0
                    economy < 7.5 -> 8.0
                    else -> 0.0
                }
            }

            MotmCandidate(player, score, runs, wickets, strikeRate, economy, 0)
        }.sortedByDescending { it.score }
    }

    private fun computeResultText(
        match: Match,
        innings1: Innings?,
        innings2: Innings?,
        team1: Team?,
        team2: Team?
    ): String {
        if (innings1 == null) return "Match result pending"
        if (innings2 == null) return "${team1?.name ?: "Team 1"} scored ${innings1.totalRuns}/${innings1.totalWickets}"

        val battingFirstTeam = if (match.battingFirstId == match.team1Id) team1 else team2
        val battingSecondTeam = if (match.battingFirstId == match.team1Id) team2 else team1

        return when {
            innings2.totalRuns > innings1.totalRuns -> {
                val wicketsLeft = (match.playersPerSide - 1) - innings2.totalWickets
                "${battingSecondTeam?.name ?: "Team"} won by $wicketsLeft wickets"
            }
            innings2.totalRuns < innings1.totalRuns -> {
                val margin = innings1.totalRuns - innings2.totalRuns
                "${battingFirstTeam?.name ?: "Team"} won by $margin runs"
            }
            else -> "Match tied"
        }
    }
}