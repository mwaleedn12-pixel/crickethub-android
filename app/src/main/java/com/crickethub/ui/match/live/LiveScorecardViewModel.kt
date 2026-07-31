package com.crickethub.ui.match.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.BatsmanStats
import com.crickethub.data.model.BowlerStats
import com.crickethub.data.model.Innings
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

/**
 * Holds a completed innings' full data so LiveScorecard can render
 * previous innings above the live one (like PostMatchScreen does).
 */
data class CompletedInningsData(
    val innings: Innings,
    val balls: List<Ball>,
    val batsmanStats: Map<String, BatsmanStats>,
    val bowlerStats: Map<String, BowlerStats>,
    val battingTeamName: String,
    val bowlingTeamName: String,
    val commentary: List<String>,
    val wides: Int = 0,
    val noBalls: Int = 0,
    val extrasTotal: Int = 0
)

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
    val error: String? = null,
    // Current crease players — so LiveScorecard can show 0(0) batsmen before any ball is bowled
    val strikerId: String? = null,
    val nonStrikerId: String? = null,
    // Completed innings (1st innings when viewing 2nd, etc.) — sorted by inningsNo ascending
    val completedInnings: List<CompletedInningsData> = emptyList()
)

/** Dismissals with no ball bowled — excluded from balls faced and the bowler's over. */
private val NO_DELIVERY_WICKETS_LIVE = setOf("timed_out", "retired_out", "retired_hurt")

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

                // Load completed innings (e.g. 1st innings when we're in 2nd)
                val completedInningsData = loadCompletedInnings(match.id, innings.id, match.team1Id, team1Name, team2Name)

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
                        extrasTotal = innings.extrasTotal,
                        strikerId = scoringState.striker?.id,
                        nonStrikerId = scoringState.nonStriker?.id,
                        completedInnings = completedInningsData
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "UpdateFromScoring error: ${e.message}", e)
            }
        }
    }

    /**
     * Load all completed innings for a match (excluding the current live innings).
     * Returns sorted by inningsNo ascending (1st innings first).
     */
    private suspend fun loadCompletedInnings(
        matchId: String,
        currentInningsId: String,
        team1Id: String,
        team1Name: String,
        team2Name: String
    ): List<CompletedInningsData> {
        return try {
            val allInnings = scoringRepository.getInningsByMatch(matchId)
            val completed = allInnings.filter { it.status == "completed" && it.id != currentInningsId }
                .sortedBy { it.inningsNo }

            completed.map { inn ->
                val innBalls = scoringRepository.getBallsByInnings(inn.id)
                val batPlayers = scoringRepository.getPlayingXIPlayers(matchId, inn.battingTeamId)
                val bowlPlayers = scoringRepository.getPlayingXIPlayers(matchId, inn.bowlingTeamId)
                val batName = if (inn.battingTeamId == team1Id) team1Name else team2Name
                val bowlName = if (inn.bowlingTeamId == team1Id) team1Name else team2Name

                CompletedInningsData(
                    innings = inn,
                    balls = innBalls,
                    batsmanStats = computeBatsmanStats(innBalls, batPlayers),
                    bowlerStats = computeBowlerStats(innBalls, bowlPlayers),
                    battingTeamName = batName,
                    bowlingTeamName = bowlName,
                    commentary = computeCommentary(innBalls),
                    wides = inn.wides,
                    noBalls = inn.noBalls,
                    extrasTotal = inn.extrasTotal
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Load completed innings error: ${e.message}", e)
            emptyList()
        }
    }

    // DB se fresh load karo — direct access ke liye
    fun loadAndSubscribe(matchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId)
                if (match == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Match not found. The owner may need to share access.") }
                    return@launch
                }
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val currentInnings = allInnings
                    .filter { it.status == "live" }
                    .maxByOrNull { it.totalBalls * 10000 + it.totalRuns }
                    ?: allInnings.lastOrNull()
                if (currentInnings == null) {
                    _uiState.update { it.copy(isLoading = false, error = "No innings data found for this match.") }
                    return@launch
                }

                val team1 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team1Id) } }
                    .decodeSingleOrNull<Team>()
                val team2 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team2Id) } }
                    .decodeSingleOrNull<Team>()

                val team1Name = team1?.name ?: "Team 1"
                val team2Name = team2?.name ?: "Team 2"

                val battingTeamName = if (currentInnings.battingTeamId == match.team1Id) team1Name else team2Name
                val bowlingTeamName = if (currentInnings.bowlingTeamId == match.team1Id) team1Name else team2Name

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

                // Load completed innings data for both-innings display
                val completedInningsData = loadCompletedInnings(
                    matchId, currentInnings.id, match.team1Id, team1Name, team2Name
                )

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
                        shareableSlug = match.shareableSlug,
                        completedInnings = completedInningsData
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "LiveScorecard error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun computeLast6Balls(balls: List<Ball>): List<String> {
        val legalBalls = balls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" && it.wicketType !in NO_DELIVERY_WICKETS_LIVE }
        val currentOverNo = if (legalBalls.isEmpty()) 0 else legalBalls.last().overNo
        val currentOverBalls = balls.filter { it.overNo == currentOverNo }
        return currentOverBalls.map { ball ->
            val runs = ball.runsOffBat + (ball.extrasRuns ?: 0)
            when {
                ball.isWicket && ball.wicketType != "retired_hurt" -> if (runs > 0) "W+$runs" else "W"
                ball.isSix -> "6"; ball.isBoundary -> "4"
                ball.extrasType == "wide" -> { val r = (ball.extrasRuns ?: 1) - 1; if (r > 0) "Wd+$r" else "Wd" }
                ball.extrasType == "no_ball" -> if (ball.runsOffBat > 0) "Nb+${ball.runsOffBat}" else "Nb"
                ball.extrasType == "bye" -> { val b = ball.extrasRuns ?: 0; if (b <= 1) "B" else "${b}B" }
                ball.extrasType == "leg_bye" -> { val lb = ball.extrasRuns ?: 0; if (lb <= 1) "LB" else "${lb}LB" }
                ball.runsOffBat == 0 && ball.extrasRuns == null -> "0"
                else -> "$runs"
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
        // Victim = dismissedBatsmanId when set (non-striker run-out), else the striker.
        fun victimId(b: Ball) = b.dismissedBatsmanId ?: b.batsmanId
        players.forEach { player ->
            val playerBalls = balls.filter { it.batsmanId == player.id }
            val runs = playerBalls.sumOf { it.runsOffBat }
            // Balls faced excludes wides AND no-delivery dismissals (timed/retired).
            val ballsFaced = playerBalls.count {
                it.extrasType != "wide" && it.wicketType !in NO_DELIVERY_WICKETS_LIVE
            }
            val fours = playerBalls.count { it.isBoundary && !it.isSix }
            val sixes = playerBalls.count { it.isSix }

            val outIdx = balls.indexOfLast {
                it.isWicket && it.wicketType != "retired_hurt" && victimId(it) == player.id
            }
            val retiredIdx = balls.indexOfLast {
                it.isWicket && it.wicketType == "retired_hurt" && victimId(it) == player.id
            }
            // Retired batsman is "back" once he faces another ball.
            val returnedAfterRetire = retiredIdx >= 0 &&
                    balls.drop(retiredIdx + 1).any { it.batsmanId == player.id }
            val decidingBall = if (outIdx >= 0 && outIdx > retiredIdx) balls[outIdx] else null
            val isOut = decidingBall != null
            val dismissalType = when {
                isOut -> decidingBall!!.wicketType
                retiredIdx >= 0 && !returnedAfterRetire -> "retired_hurt"
                else -> null
            }
            statsMap[player.id] = BatsmanStats(
                player = player, runs = runs, balls = ballsFaced,
                fours = fours, sixes = sixes, isOut = isOut,
                dismissalType = dismissalType,
                fielderName = decidingBall?.fielderName,
                bowlerOnWicket = decidingBall?.bowlerId
            )
        }
        return statsMap
    }

    private fun computeBowlerStats(balls: List<Ball>, players: List<Player>): Map<String, BowlerStats> {
        val statsMap = mutableMapOf<String, BowlerStats>()
        players.forEach { player ->
            // Exclude no-delivery dismissals: they carry a bowlerId but no ball was bowled.
            val playerBalls = balls.filter {
                it.bowlerId == player.id && it.wicketType !in NO_DELIVERY_WICKETS_LIVE
            }
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
            // Dot ball: legal delivery conceding nothing.
            val dots = playerBalls.count {
                it.extrasType != "wide" && it.extrasType != "no_ball" &&
                        it.runsOffBat == 0 && (it.extrasRuns ?: 0) == 0
            }
            // Maiden: completed 6-legal-ball over conceding 0 runs.
            val maidens = playerBalls.groupBy { it.overNo }.count { (_, overBalls) ->
                val legalInOver = overBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                val conceded = overBalls.sumOf {
                    if (it.extrasType in listOf("bye", "leg_bye")) 0
                    else it.runsOffBat + (it.extrasRuns ?: 0)
                }
                legalInOver == 6 && conceded == 0
            }
            statsMap[player.id] = BowlerStats(
                player = player, balls = legalBalls, runs = runs, wickets = wickets,
                overs = "${legalBalls / 6}.${legalBalls % 6}",
                maidens = maidens, dotBalls = dots,
                wides = playerBalls.count { it.extrasType == "wide" },
                noBalls = playerBalls.count { it.extrasType == "no_ball" }
            )
        }
        return statsMap
    }
}