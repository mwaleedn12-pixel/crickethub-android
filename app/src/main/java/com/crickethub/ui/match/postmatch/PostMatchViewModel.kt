package com.crickethub.ui.match.postmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Innings
import com.crickethub.data.model.Match
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.model.TournamentTeam
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.MatchRepository
import com.crickethub.data.repository.ScoringRepository
import com.crickethub.data.repository.TournamentRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val dismissalType: String?,
    val fielderName: String? = null,
    val bowlerOnWicket: String? = null
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
    val innings1BattingTeamName: String = "",
    val innings1BowlingTeamName: String = "",
    val innings1Batting: List<BatsmanScorecard> = emptyList(),
    val innings1Bowling: List<BowlerScorecard> = emptyList(),
    val innings2BattingTeamName: String = "",
    val innings2BowlingTeamName: String = "",
    val innings2Batting: List<BatsmanScorecard> = emptyList(),
    val innings2Bowling: List<BowlerScorecard> = emptyList(),
    val inn1Balls: List<Ball> = emptyList(),
    val inn2Balls: List<Ball> = emptyList(),
    val motmCandidates: List<MotmCandidate> = emptyList(),
    val selectedMotm: Player? = null,
    val resultText: String = "",
    val matchSaved: Boolean = false
)

class PostMatchViewModel : ViewModel() {

    private val scoringRepository = ScoringRepository()
    private val matchRepository = MatchRepository()
    private val tournamentRepository = TournamentRepository()

    private val _uiState = MutableStateFlow(PostMatchUiState())
    val uiState: StateFlow<PostMatchUiState> = _uiState.asStateFlow()

    fun loadPostMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                coroutineScope {
                    // Parallel: match + innings
                    val matchDeferred = async { matchRepository.getMatchById(matchId) }
                    val allInningsDeferred = async { scoringRepository.getInningsByMatch(matchId) }

                    val match = matchDeferred.await() ?: return@coroutineScope
                    val allInnings = allInningsDeferred.await()
                    val innings1 = allInnings.firstOrNull { it.inningsNo == 1 }
                    val innings2 = allInnings.firstOrNull { it.inningsNo == 2 }

                    // Parallel: teams + balls
                    val t1Deferred = async {
                        try {
                            SupabaseClient.client.postgrest["teams"]
                                .select { filter { eq("id", match.team1Id) } }
                                .decodeSingleOrNull<Team>()
                        } catch (e: Exception) { null }
                    }
                    val t2Deferred = async {
                        try {
                            SupabaseClient.client.postgrest["teams"]
                                .select { filter { eq("id", match.team2Id) } }
                                .decodeSingleOrNull<Team>()
                        } catch (e: Exception) { null }
                    }
                    val balls1Deferred = async {
                        if (innings1 != null) scoringRepository.getBallsByInnings(innings1.id)
                        else emptyList()
                    }
                    val balls2Deferred = async {
                        if (innings2 != null) scoringRepository.getBallsByInnings(innings2.id)
                        else emptyList()
                    }

                    val team1 = t1Deferred.await()
                    val team2 = t2Deferred.await()
                    val balls1 = balls1Deferred.await()
                    val balls2 = balls2Deferred.await()

                    // Team name assignments
                    val inn1BattingTeamId = innings1?.battingTeamId
                        ?: match.battingFirstId
                        ?: match.team1Id
                    val inn1BowlingTeamId = innings1?.bowlingTeamId
                        ?: if (inn1BattingTeamId == match.team1Id) match.team2Id else match.team1Id
                    val inn1BattingTeamName = if (inn1BattingTeamId == match.team1Id) team1?.name ?: "Team 1" else team2?.name ?: "Team 2"
                    val inn1BowlingTeamName = if (inn1BowlingTeamId == match.team1Id) team1?.name ?: "Team 1" else team2?.name ?: "Team 2"
                    val inn2BattingTeamId = inn1BowlingTeamId
                    val inn2BowlingTeamId = inn1BattingTeamId
                    val inn2BattingTeamName = inn1BowlingTeamName
                    val inn2BowlingTeamName = inn1BattingTeamName

                    // Parallel: playing XI for all 4 combinations
                    val inn1BatDeferred = async {
                        scoringRepository.getPlayingXIPlayers(matchId, inn1BattingTeamId)
                    }
                    val inn1BowlDeferred = async {
                        scoringRepository.getPlayingXIPlayers(matchId, inn1BowlingTeamId)
                    }
                    val inn2BatDeferred = async {
                        scoringRepository.getPlayingXIPlayers(matchId, inn2BattingTeamId)
                    }
                    val inn2BowlDeferred = async {
                        scoringRepository.getPlayingXIPlayers(matchId, inn2BowlingTeamId)
                    }

                    val inn1BatPlayers = inn1BatDeferred.await()
                    val inn1BowlPlayers = inn1BowlDeferred.await()
                    val inn2BatPlayers = inn2BatDeferred.await()
                    val inn2BowlPlayers = inn2BowlDeferred.await()

                    // Compute scorecards
                    val innings1Batting = computeBattingScorecard(balls1, inn1BatPlayers)
                    val innings1Bowling = computeBowlingScorecard(balls1, inn1BowlPlayers)
                    val innings2Batting = computeBattingScorecard(balls2, inn2BatPlayers)
                    val innings2Bowling = computeBowlingScorecard(balls2, inn2BowlPlayers)

                    val allPlayers = (inn1BatPlayers + inn1BowlPlayers + inn2BatPlayers + inn2BowlPlayers)
                        .distinctBy { it.id }
                    val motmCandidates = computeMotmCandidates(allPlayers, balls1, balls2)

                    val resultText = computeResultText(
                        match, innings1, innings2, team1, team2,
                        inn1BattingTeamId, inn1BattingTeamName, inn1BowlingTeamName
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            match = match,
                            innings1 = innings1,
                            innings2 = innings2,
                            team1 = team1,
                            team2 = team2,
                            innings1BattingTeamName = inn1BattingTeamName,
                            innings1BowlingTeamName = inn1BowlingTeamName,
                            innings2BattingTeamName = inn2BattingTeamName,
                            innings2BowlingTeamName = inn2BowlingTeamName,
                            innings1Batting = innings1Batting,
                            innings1Bowling = innings1Bowling,
                            innings2Batting = innings2Batting,
                            innings2Bowling = innings2Bowling,
                            inn1Balls = balls1,
                            inn2Balls = balls2,
                            motmCandidates = motmCandidates,
                            selectedMotm = motmCandidates.firstOrNull()?.player,
                            resultText = resultText
                        )
                    }
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
                val state = _uiState.value
                val match = state.match ?: return@launch
                val innings1 = state.innings1
                val innings2 = state.innings2

                SupabaseClient.client.postgrest["matches"]
                    .update({
                        set("status", "completed")
                        set("result_text", state.resultText)
                    }) {
                        filter { eq("id", matchId) }
                    }

                matchRepository.invalidateMatchCache(matchId)
                matchRepository.invalidateMatchesCache()

                if (match.tournamentId != null && innings1 != null && innings2 != null) {
                    updateTournamentPoints(match, innings1, innings2)
                }

                _uiState.update { it.copy(matchSaved = true) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Save result error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun updateTournamentPoints(
        match: Match,
        innings1: Innings,
        innings2: Innings
    ) {
        try {
            val tournamentId = match.tournamentId ?: return
            val battingFirstId = match.battingFirstId ?: match.team1Id
            val battingSecondId = if (battingFirstId == match.team1Id) match.team2Id else match.team1Id

            val winnerId: String?
            val loserId: String?

            when {
                innings2.totalRuns > innings1.totalRuns -> {
                    winnerId = battingSecondId
                    loserId = battingFirstId
                }
                innings2.totalRuns < innings1.totalRuns -> {
                    winnerId = battingFirstId
                    loserId = battingSecondId
                }
                else -> {
                    winnerId = null
                    loserId = null
                }
            }

            val inn1Overs = innings1.totalBalls.toDouble() / 6
            val inn2Overs = innings2.totalBalls.toDouble() / 6

            if (winnerId != null && loserId != null) {
                tournamentRepository.updatePointsTable(
                    tournamentId = tournamentId,
                    winnerTeamId = winnerId,
                    loserTeamId = loserId,
                    winnerRuns = if (winnerId == battingFirstId) innings1.totalRuns else innings2.totalRuns,
                    loserRuns = if (loserId == battingFirstId) innings1.totalRuns else innings2.totalRuns,
                    winnerOvers = if (winnerId == battingFirstId) inn1Overs else inn2Overs,
                    loserOvers = if (loserId == battingFirstId) inn1Overs else inn2Overs
                )
            } else {
                // Tie — draw points
                listOf(battingFirstId, battingSecondId).forEach { teamId ->
                    val entry = SupabaseClient.client.postgrest["tournament_teams"]
                        .select {
                            filter {
                                eq("tournament_id", tournamentId)
                                eq("team_id", teamId)
                            }
                        }
                        .decodeSingleOrNull<TournamentTeam>()

                    if (entry != null) {
                        SupabaseClient.client.postgrest["tournament_teams"]
                            .update({
                                set("points", entry.points + 1)
                                set("matches_played", entry.matchesPlayed + 1)
                                set("draws", entry.draws + 1)
                            }) {
                                filter {
                                    eq("tournament_id", tournamentId)
                                    eq("team_id", teamId)
                                }
                            }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Tournament points error: ${e.message}", e)
        }
    }

    private fun computeBattingScorecard(
        balls: List<Ball>,
        players: List<Player>
    ): List<BatsmanScorecard> {
        if (players.isEmpty()) return emptyList()
        return players.map { player ->
            val playerBalls = balls.filter { it.batsmanId == player.id }
            val runs = playerBalls.sumOf { it.runsOffBat }
            val ballsFaced = playerBalls.count { it.extrasType != "wide" }
            val fours = playerBalls.count { it.isBoundary && !it.isSix }
            val sixes = playerBalls.count { it.isSix }
            val isOut = playerBalls.any {
                it.isWicket &&
                        it.wicketType != "run_out" &&
                        it.wicketType != "retired_hurt"
            }
            val wicketBall = playerBalls.firstOrNull { it.isWicket }
            val sr = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100 else 0.0
            BatsmanScorecard(
                player = player,
                runs = runs,
                balls = ballsFaced,
                fours = fours,
                sixes = sixes,
                strikeRate = sr,
                isOut = isOut,
                dismissalType = wicketBall?.wicketType,
                fielderName = wicketBall?.fielderName,
                bowlerOnWicket = wicketBall?.bowlerId
            )
        }.sortedWith(
            compareByDescending<BatsmanScorecard> { it.balls > 0 || it.isOut }
                .thenByDescending { it.runs }
        )
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
                    "run_out", "obstructing", "handled_ball",
                    "timed_out", "retired_hurt", "retired_out"
                )
            }
            val economy = if (legalBalls > 0) (runs.toDouble() / legalBalls) * 6 else 0.0
            val wides = playerBalls.count { it.extrasType == "wide" }
            val noBalls = playerBalls.count { it.extrasType == "no_ball" }
            val maidens = playerBalls.groupBy { it.overNo }.values.count { overBalls ->
                overBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" } == 6 &&
                        overBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) } == 0
            }
            BowlerScorecard(
                player = player,
                overs = overs,
                maidens = maidens,
                runs = runs,
                wickets = wickets,
                economy = economy,
                wides = wides,
                noBalls = noBalls
            )
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
                it.isWicket && it.wicketType !in listOf(
                    "run_out", "obstructing", "retired_hurt"
                )
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

            MotmCandidate(
                player = player,
                score = score,
                runs = runs,
                wickets = wickets,
                strikeRate = strikeRate,
                economy = economy,
                catches = 0
            )
        }.sortedByDescending { it.score }
    }

    private fun computeResultText(
        match: Match,
        innings1: Innings?,
        innings2: Innings?,
        team1: Team?,
        team2: Team?,
        inn1BattingTeamId: String,
        inn1BattingTeamName: String,
        inn1BowlingTeamName: String
    ): String {
        if (innings1 == null) return "Match result pending"
        if (innings2 == null) return "$inn1BattingTeamName scored ${innings1.totalRuns}/${innings1.totalWickets}"

        return when {
            innings2.totalRuns > innings1.totalRuns -> {
                val wicketsLeft = (match.playersPerSide - 1) - innings2.totalWickets
                "$inn1BowlingTeamName won by $wicketsLeft wickets"
            }
            innings2.totalRuns < innings1.totalRuns -> {
                val margin = innings1.totalRuns - innings2.totalRuns
                "$inn1BattingTeamName won by $margin runs"
            }
            else -> "Match tied"
        }
    }
}