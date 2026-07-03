package com.crickethub.ui.match.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.BatsmanStats
import com.crickethub.data.model.BowlerStats
import com.crickethub.data.model.Innings
import com.crickethub.data.model.Player
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.MatchRepository
import com.crickethub.data.repository.ScoringRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveScorecardUiState(
    val isLoading: Boolean = true,
    val matchStatus: String = "SCHEDULED",
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
    val shareableSlug: String? = null,
    val error: String? = null
)

class LiveScorecardViewModel : ViewModel() {

    private val scoringRepository = ScoringRepository()
    private val matchRepository = MatchRepository()

    private val _uiState = MutableStateFlow(LiveScorecardUiState())
    val uiState: StateFlow<LiveScorecardUiState> = _uiState.asStateFlow()

    private var currentMatchId: String? = null
    private var currentInningsId: String? = null

    fun loadAndSubscribe(matchId: String) {
        currentMatchId = matchId
        viewModelScope.launch {
            loadData(matchId)
            subscribeToRealtime(matchId)
        }
    }

    private suspend fun loadData(matchId: String) {
        try {
            _uiState.update { it.copy(isLoading = true) }

            val match = matchRepository.getMatchById(matchId) ?: return
            val allInnings = scoringRepository.getInningsByMatch(matchId)
            val currentInnings = allInnings.find { it.status == "live" }
                ?: allInnings.lastOrNull()

            currentInningsId = currentInnings?.id

            val battingTeamId = currentInnings?.battingTeamId ?: match.battingFirstId ?: match.team1Id
            val bowlingTeamId = currentInnings?.bowlingTeamId
                ?: if (battingTeamId == match.team1Id) match.team2Id else match.team1Id

            // Team names fetch karo
            val battingTeam = SupabaseClient.client.postgrest["teams"]
                .select { filter { eq("id", battingTeamId) } }
                .decodeSingleOrNull<com.crickethub.data.model.Team>()

            val bowlingTeam = SupabaseClient.client.postgrest["teams"]
                .select { filter { eq("id", bowlingTeamId) } }
                .decodeSingleOrNull<com.crickethub.data.model.Team>()

            val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, battingTeamId)
            val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, bowlingTeamId)

            val balls = if (currentInnings != null) {
                scoringRepository.getBallsByInnings(currentInnings.id)
            } else emptyList()

            // 2nd innings ke liye target calculate karo
            val firstInnings = allInnings.firstOrNull { it.inningsNo == 1 }
            val target = if (allInnings.size >= 2) (firstInnings?.totalRuns ?: 0) + 1 else null

            updateState(
                match = match,
                innings = currentInnings,
                balls = balls,
                battingTeamName = battingTeam?.name ?: "Team 1",
                bowlingTeamName = bowlingTeam?.name ?: "Team 2",
                battingPlayers = battingPlayers,
                bowlingPlayers = bowlingPlayers,
                target = target
            )

        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "LiveScorecard error: ${e.message}", e)
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun updateState(
        match: com.crickethub.data.model.Match,
        innings: Innings?,
        balls: List<Ball>,
        battingTeamName: String,
        bowlingTeamName: String,
        battingPlayers: List<Player>,
        bowlingPlayers: List<Player>,
        target: Int?
    ) {
        val totalRuns = innings?.totalRuns ?: 0
        val totalWickets = innings?.totalWickets ?: 0
        val totalBalls = innings?.totalBalls ?: 0
        val currentOver = totalBalls / 6
        val currentBall = totalBalls % 6
        val crr = if (totalBalls > 0) (totalRuns.toDouble() / totalBalls) * 6 else 0.0

        val ballsLeft = match.totalOvers * 6 - totalBalls
        val rrr = if (target != null && ballsLeft > 0) {
            ((target - totalRuns).toDouble() / ballsLeft) * 6
        } else null

        val last6 = balls.takeLast(6).map { ball ->
            when {
                ball.isWicket -> "W"
                ball.extrasType == "wide" -> "Wd"
                ball.extrasType == "no_ball" -> "Nb"
                ball.isSix -> "6"
                ball.isBoundary -> "4"
                else -> (ball.runsOffBat + (ball.extrasRuns ?: 0)).toString()
            }
        }

        val commentary = balls
            .mapNotNull { it.commentary }
            .reversed()
            .take(20)

        val batsmanStats = computeBatsmanStats(balls, battingPlayers)
        val bowlerStats = computeBowlerStats(balls, bowlingPlayers)

        _uiState.update {
            it.copy(
                isLoading = false,
                matchStatus = match.status.uppercase(),
                battingTeamName = battingTeamName,
                bowlingTeamName = bowlingTeamName,
                totalRuns = totalRuns,
                totalWickets = totalWickets,
                currentOver = currentOver,
                currentBall = currentBall,
                currentRunRate = crr,
                requiredRunRate = rrr,
                target = target,
                ballsLeft = ballsLeft,
                last6Balls = last6,
                batsmanStats = batsmanStats,
                bowlerStats = bowlerStats,
                commentary = commentary,
                shareableSlug = match.shareableSlug
            )
        }
    }

    private fun subscribeToRealtime(matchId: String) {
        viewModelScope.launch {
            try {
                val channel = SupabaseClient.client.realtime.channel("live-$matchId")

                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "balls"
                }.onEach {
                    // Naya ball aaya — data reload karo
                    loadData(matchId)
                }.launchIn(viewModelScope)

                channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "innings"
                }.onEach {
                    loadData(matchId)
                }.launchIn(viewModelScope)

                channel.subscribe()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Realtime error: ${e.message}", e)
            }
        }
    }

    private fun computeBatsmanStats(
        balls: List<Ball>,
        players: List<Player>
    ): Map<String, BatsmanStats> {
        val statsMap = mutableMapOf<String, BatsmanStats>()
        players.forEach { player ->
            val playerBalls = balls.filter { it.batsmanId == player.id }
            if (playerBalls.isEmpty()) return@forEach
            val runs = playerBalls.sumOf { it.runsOffBat }
            val ballsFaced = playerBalls.count { it.extrasType != "wide" }
            val fours = playerBalls.count { it.isBoundary && !it.isSix }
            val sixes = playerBalls.count { it.isSix }
            val isOut = playerBalls.any { it.isWicket && it.wicketType != "run_out" }
            val dismissalType = playerBalls.firstOrNull { it.isWicket }?.wicketType
            statsMap[player.id] = BatsmanStats(
                player = player,
                runs = runs,
                balls = ballsFaced,
                fours = fours,
                sixes = sixes,
                isOut = isOut,
                dismissalType = dismissalType
            )
        }
        return statsMap
    }

    private fun computeBowlerStats(
        balls: List<Ball>,
        players: List<Player>
    ): Map<String, BowlerStats> {
        val statsMap = mutableMapOf<String, BowlerStats>()
        players.forEach { player ->
            val playerBalls = balls.filter { it.bowlerId == player.id }
            if (playerBalls.isEmpty()) return@forEach
            val legalBalls = playerBalls.count {
                it.extrasType != "wide" && it.extrasType != "no_ball"
            }
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
            statsMap[player.id] = BowlerStats(
                player = player,
                balls = legalBalls,
                runs = runs,
                wickets = wickets,
                wides = playerBalls.count { it.extrasType == "wide" },
                noBalls = playerBalls.count { it.extrasType == "no_ball" }
            )
        }
        return statsMap
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                currentMatchId?.let {
                    SupabaseClient.client.realtime.removeChannel(
                        SupabaseClient.client.realtime.channel("live-$it")
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Channel cleanup error: ${e.message}", e)
            }
        }
    }
}