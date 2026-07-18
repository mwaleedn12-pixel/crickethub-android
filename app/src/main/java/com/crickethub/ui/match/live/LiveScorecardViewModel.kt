package com.crickethub.ui.match.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.BatsmanStats
import com.crickethub.data.model.BowlerStats
import com.crickethub.data.model.Player
import com.crickethub.data.model.ScoringUiState
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

data class LiveScorecardUiState(
    val isLoading: Boolean = true,
    val matchStatus: String = "LIVE",
    val battingTeamName: String = "",
    val bowlingTeamName: String = "",
    val totalRuns: Int = 0,
    val totalWickets: Int = 0,
    val currentOver: Int = 0,
    val currentBall: Int = 0,
    val currentRunRate: Double = 0.0,
    val requiredRunRate: Double? = null,
    val target: Int? = null,
    val ballsLeft: Int = 0,
    val last6Balls: List<String> = emptyList(),
    val batsmanStats: Map<String, BatsmanStats> = emptyMap(),
    val bowlerStats: Map<String, BowlerStats> = emptyMap(),
    val commentary: List<String> = emptyList(),
    val balls: List<Ball> = emptyList(),
    val wides: Int = 0,
    val noBalls: Int = 0,
    val extrasTotal: Int = 0,
    val resultText: String = "",
    val shareableSlug: String? = null,
    val error: String? = null
)

class LiveScorecardViewModel : ViewModel() {

    private val matchRepository = MatchRepository()
    private val scoringRepository = ScoringRepository()

    private val _uiState = MutableStateFlow(LiveScorecardUiState())
    val uiState: StateFlow<LiveScorecardUiState> = _uiState.asStateFlow()

    // Scoring screen se directly state receive karo
    fun updateFromScoringState(
        scoringState: ScoringUiState,
        team1Name: String,
        team2Name: String
    ) {
        viewModelScope.launch {
            try {
                val balls = scoringState.balls
                val battingPlayers = scoringState.battingTeamPlayers
                val bowlingPlayers = scoringState.bowlingTeamPlayers
                val innings = scoringState.innings ?: return@launch
                val match = scoringState.match ?: return@launch

                val batsmanStats = computeBatsmanStats(balls, battingPlayers)
                val bowlerStats = computeBowlerStats(balls, bowlingPlayers)
                val last6 = computeLast6Balls(balls)
                val commentary = computeCommentary(balls)

                val legalBalls = innings.totalBalls
                val overNo = legalBalls / 6
                val ballNo = legalBalls % 6
                val crr = if (legalBalls > 0) innings.totalRuns.toDouble() / legalBalls * 6 else 0.0

                val battingTeamName = if (innings.battingTeamId == match.team1Id) team1Name else team2Name
                val bowlingTeamName = if (innings.bowlingTeamId == match.team1Id) team1Name else team2Name

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        matchStatus = "LIVE",
                        battingTeamName = battingTeamName,
                        bowlingTeamName = bowlingTeamName,
                        totalRuns = innings.totalRuns,
                        totalWickets = innings.totalWickets,
                        currentOver = overNo,
                        currentBall = ballNo,
                        currentRunRate = crr,
                        last6Balls = last6,
                        batsmanStats = batsmanStats,
                        bowlerStats = bowlerStats,
                        commentary = commentary,
                        balls = balls,
                        wides = innings.wides,
                        noBalls = innings.noBalls,
                        extrasTotal = innings.extrasTotal
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "UpdateFromScoring error: ${e.message}", e)
            }
        }
    }

    // DB se fresh load karo — direct access ke liye
    fun loadAndSubscribe(matchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val currentInnings = allInnings
                    .filter { it.status == "live" }
                    .maxByOrNull { it.totalBalls * 10000 + it.totalRuns }
                    ?: allInnings.lastOrNull()
                    ?: return@launch

                val team1 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team1Id) } }
                    .decodeSingleOrNull<Team>()
                val team2 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team2Id) } }
                    .decodeSingleOrNull<Team>()

                val battingTeamName = if (currentInnings.battingTeamId == match.team1Id)
                    team1?.name ?: "Team 1" else team2?.name ?: "Team 2"
                val bowlingTeamName = if (currentInnings.bowlingTeamId == match.team1Id)
                    team1?.name ?: "Team 1" else team2?.name ?: "Team 2"

                val balls = scoringRepository.getBallsByInnings(currentInnings.id)
                val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, currentInnings.battingTeamId)
                val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, currentInnings.bowlingTeamId)

                val batsmanStats = computeBatsmanStats(balls, battingPlayers)
                val bowlerStats = computeBowlerStats(balls, bowlingPlayers)
                val last6 = computeLast6Balls(balls)
                val commentary = computeCommentary(balls)

                val legalBalls = currentInnings.totalBalls
                val overNo = legalBalls / 6
                val ballNo = legalBalls % 6
                val crr = if (legalBalls > 0) currentInnings.totalRuns.toDouble() / legalBalls * 6 else 0.0

                val completedInnings = allInnings.filter { it.status == "completed" }
                val target: Int?
                val rrr: Double?
                val ballsLeft: Int

                if (allInnings.size >= 2 && completedInnings.isNotEmpty()) {
                    val firstInnings = completedInnings.first()
                    target = firstInnings.totalRuns + 1
                    ballsLeft = (match.totalOvers * 6) - legalBalls
                    val runsNeeded = target - currentInnings.totalRuns
                    rrr = if (ballsLeft > 0) runsNeeded.toDouble() / ballsLeft * 6 else null
                } else {
                    target = null
                    rrr = null
                    ballsLeft = (match.totalOvers * 6) - legalBalls
                }

                val matchStatus = when (match.status) {
                    "completed" -> "COMPLETED"
                    "live" -> "LIVE"
                    "abandoned" -> "ABANDONED"
                    else -> "UPCOMING"
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        matchStatus = matchStatus,
                        battingTeamName = battingTeamName,
                        bowlingTeamName = bowlingTeamName,
                        totalRuns = currentInnings.totalRuns,
                        totalWickets = currentInnings.totalWickets,
                        currentOver = overNo,
                        currentBall = ballNo,
                        currentRunRate = crr,
                        requiredRunRate = rrr,
                        target = target,
                        ballsLeft = ballsLeft,
                        last6Balls = last6,
                        batsmanStats = batsmanStats,
                        bowlerStats = bowlerStats,
                        commentary = commentary,
                        balls = balls,
                        wides = currentInnings.wides,
                        noBalls = currentInnings.noBalls,
                        extrasTotal = currentInnings.extrasTotal,
                        resultText = match.resultText ?: "",
                        shareableSlug = match.shareableSlug
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "LiveScorecard error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun computeLast6Balls(balls: List<Ball>): List<String> {
        val legalBalls = balls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
        val currentOverNo = if (legalBalls.isEmpty()) 0 else legalBalls.last().overNo
        val currentOverBalls = balls.filter { it.overNo == currentOverNo }
        return currentOverBalls.map { ball ->
            when {
                ball.isWicket -> "W"
                ball.isSix -> "6"
                ball.isBoundary -> "4"
                ball.extrasType == "wide" -> "Wd"
                ball.extrasType == "no_ball" -> "Nb"
                ball.runsOffBat == 0 && ball.extrasRuns == null -> "0"
                else -> "${ball.runsOffBat + (ball.extrasRuns ?: 0)}"
            }
        }
    }

    private fun computeCommentary(balls: List<Ball>): List<String> {
        return balls.mapNotNull { ball ->
            val overBall = "${ball.overNo}.${ball.ballNo}"
            val outcome = when {
                ball.isWicket -> "W"
                ball.isSix -> "6"
                ball.isBoundary -> "4"
                ball.extrasType == "wide" -> "Wd"
                ball.extrasType == "no_ball" -> "Nb"
                ball.runsOffBat == 0 -> "0"
                else -> "${ball.runsOffBat}"
            }
            val description = ball.commentary ?: return@mapNotNull null
            "$overBall | $outcome | $description"
        }
    }

    private fun computeBatsmanStats(balls: List<Ball>, players: List<Player>): Map<String, BatsmanStats> {
        val statsMap = mutableMapOf<String, BatsmanStats>()
        players.forEach { player ->
            val playerBalls = balls.filter { it.batsmanId == player.id }
            val runs = playerBalls.sumOf { it.runsOffBat }
            val ballsFaced = playerBalls.count { it.extrasType != "wide" }
            val fours = playerBalls.count { it.isBoundary && !it.isSix }
            val sixes = playerBalls.count { it.isSix }
            val isOut = playerBalls.any {
                it.isWicket && it.wicketType != "run_out" && it.wicketType != "retired_hurt"
            }
            val wicketBall = playerBalls.firstOrNull { it.isWicket }
            statsMap[player.id] = BatsmanStats(
                player = player, runs = runs, balls = ballsFaced,
                fours = fours, sixes = sixes, isOut = isOut,
                dismissalType = wicketBall?.wicketType,
                fielderName = wicketBall?.fielderName,
                bowlerOnWicket = wicketBall?.bowlerId
            )
        }
        return statsMap
    }

    private fun computeBowlerStats(balls: List<Ball>, players: List<Player>): Map<String, BowlerStats> {
        val statsMap = mutableMapOf<String, BowlerStats>()
        players.forEach { player ->
            val playerBalls = balls.filter { it.bowlerId == player.id }
            if (playerBalls.isEmpty()) return@forEach
            val legalBalls = playerBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
            val runs = playerBalls.sumOf { ball ->
                when (ball.extrasType) { "bye", "leg_bye" -> 0; else -> ball.runsOffBat + (ball.extrasRuns ?: 0) }
            }
            val wickets = playerBalls.count {
                it.isWicket && it.wicketType !in listOf(
                    "run_out", "obstructing", "handled_ball",
                    "timed_out", "retired_hurt", "retired_out"
                )
            }
            statsMap[player.id] = BowlerStats(
                player = player, balls = legalBalls, runs = runs, wickets = wickets,
                overs = "${legalBalls / 6}.${legalBalls % 6}",
                wides = playerBalls.count { it.extrasType == "wide" },
                noBalls = playerBalls.count { it.extrasType == "no_ball" }
            )
        }
        return statsMap
    }
}