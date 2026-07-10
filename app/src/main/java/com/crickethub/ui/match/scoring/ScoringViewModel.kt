package com.crickethub.ui.match.scoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.BallInsert
import com.crickethub.data.model.BatsmanStats
import com.crickethub.data.model.BowlerStats
import com.crickethub.data.model.InningsInsert
import com.crickethub.data.model.Player
import com.crickethub.data.model.ScoringUiState
import com.crickethub.data.repository.MatchRepository
import com.crickethub.data.repository.ScoringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScoringViewModel : ViewModel() {

    private val scoringRepository = ScoringRepository()
    private val matchRepository = MatchRepository()

    private val _uiState = MutableStateFlow(ScoringUiState())
    val uiState: StateFlow<ScoringUiState> = _uiState.asStateFlow()

    private var isProcessingBall = false
    private var inningsCompleteHandled = false
    private var isMatchLoaded = false
    private var currentMatchId = ""
    private var target: Int? = null

    fun loadMatch(matchId: String, forceReload: Boolean = false) {
        if (isMatchLoaded && matchId == currentMatchId && !forceReload) {
            _uiState.update {
                it.copy(inningsComplete = false, matchComplete = false, error = null)
            }
            return
        }

        isMatchLoaded = true
        currentMatchId = matchId
        inningsCompleteHandled = false

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val currentInnings = allInnings.find { it.status == "live" }
                val completedInnings = allInnings.filter { it.status == "completed" }

                if (completedInnings.size >= 2) {
                    _uiState.update { it.copy(isLoading = false, matchComplete = true) }
                    return@launch
                }

                val firstInnings = allInnings.find { it.inningsNo == 1 }
                target = if (firstInnings != null && firstInnings.status == "completed") {
                    firstInnings.totalRuns + 1
                } else null

                val battingTeamId: String
                val bowlingTeamId: String

                when {
                    allInnings.isEmpty() -> {
                        battingTeamId = match.battingFirstId ?: match.team1Id
                        bowlingTeamId = if (battingTeamId == match.team1Id) match.team2Id else match.team1Id
                    }
                    currentInnings != null -> {
                        battingTeamId = currentInnings.battingTeamId
                        bowlingTeamId = currentInnings.bowlingTeamId
                    }
                    completedInnings.size == 1 -> {
                        val first = completedInnings.first()
                        battingTeamId = first.bowlingTeamId
                        bowlingTeamId = first.battingTeamId
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                }

                val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, battingTeamId)
                val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, bowlingTeamId)

                if (currentInnings != null) {
                    val balls = scoringRepository.getBallsByInnings(currentInnings.id)
                    val batsmanStats = computeBatsmanStats(balls, battingPlayers)
                    val bowlerStats = computeBowlerStats(balls, bowlingPlayers)
                    _uiState.update {
                        it.copy(
                            match = match,
                            innings = currentInnings,
                            balls = balls,
                            battingTeamPlayers = battingPlayers,
                            bowlingTeamPlayers = bowlingPlayers,
                            batsmanStats = batsmanStats,
                            bowlerStats = bowlerStats,
                            inningsComplete = false,
                            matchComplete = false,
                            isLoading = false
                        )
                    }
                } else if (allInnings.isEmpty() || completedInnings.size == 1) {
                    val inningsNo = if (allInnings.isEmpty()) 1 else 2
                    val newInnings = scoringRepository.createInnings(
                        InningsInsert(
                            matchId = matchId,
                            inningsNo = inningsNo,
                            battingTeamId = battingTeamId,
                            bowlingTeamId = bowlingTeamId
                        )
                    )
                    _uiState.update {
                        it.copy(
                            match = match,
                            innings = newInnings,
                            balls = emptyList(),
                            striker = null,
                            nonStriker = null,
                            currentBowler = null,
                            battingTeamPlayers = battingPlayers,
                            bowlingTeamPlayers = bowlingPlayers,
                            batsmanStats = emptyMap(),
                            bowlerStats = emptyMap(),
                            inningsComplete = false,
                            matchComplete = false,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Load match error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun checkAndStartNextInnings(matchId: String, onMatchComplete: () -> Unit) {
        if (inningsCompleteHandled) return
        inningsCompleteHandled = true

        viewModelScope.launch {
            try {
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val completedInnings = allInnings.filter { it.status == "completed" }
                when {
                    completedInnings.size >= 2 -> onMatchComplete()
                    else -> {
                        target = null
                        _uiState.update { it.copy(inningsComplete = false) }
                        isMatchLoaded = false
                        loadMatch(matchId)
                    }
                }
            } catch (e: Exception) {
                inningsCompleteHandled = false
                android.util.Log.e("CricketHub", "Next innings error: ${e.message}", e)
            }
        }
    }

    fun setStriker(player: Player) { _uiState.update { it.copy(striker = player) } }
    fun setNonStriker(player: Player) { _uiState.update { it.copy(nonStriker = player) } }
    fun setBowler(player: Player) { _uiState.update { it.copy(currentBowler = player) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun getMaxOversPerBowler(totalOvers: Int): Int = when (totalOvers) {
        5 -> 1; 10 -> 2; 20 -> 4; 50 -> 10
        else -> totalOvers / 5
    }

    fun canBowlerBowl(bowlerId: String, totalOvers: Int): Boolean {
        val legalBallsBowled = _uiState.value.balls.count {
            it.bowlerId == bowlerId &&
                    it.extrasType != "wide" &&
                    it.extrasType != "no_ball"
        }
        return (legalBallsBowled / 6) < getMaxOversPerBowler(totalOvers)
    }

    fun addPenaltyRuns(team: String) {
        viewModelScope.launch {
            try {
                val innings = _uiState.value.innings ?: return@launch
                if (team == "batting") {
                    val updated = scoringRepository.updateInnings(
                        innings.id, innings.totalRuns + 5, innings.totalWickets,
                        innings.totalBalls, innings.extrasTotal + 5,
                        innings.wides, innings.noBalls
                    )
                    _uiState.update { it.copy(innings = updated) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun manualEdit(runs: Int, wickets: Int) {
        viewModelScope.launch {
            try {
                val innings = _uiState.value.innings ?: return@launch
                val updated = scoringRepository.updateInnings(
                    innings.id, runs, wickets, innings.totalBalls,
                    innings.extrasTotal, innings.wides, innings.noBalls
                )
                _uiState.update { it.copy(innings = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun undoLastBall() {
        val currentState = _uiState.value
        val lastBall = currentState.balls.lastOrNull() ?: return
        val battingPlayers = currentState.battingTeamPlayers
        val bowlingPlayers = currentState.bowlingTeamPlayers
        val currentInnings = currentState.innings ?: return

        viewModelScope.launch {
            try {
                scoringRepository.deleteLastBall(lastBall.id)
                val updatedBalls = currentState.balls.dropLast(1)

                val isLegalBall = lastBall.extrasType != "wide" && lastBall.extrasType != "no_ball"
                val wasWicket = lastBall.isWicket && lastBall.wicketType != "retired_hurt"

                val runsToRemove = when {
                    lastBall.extrasType == "wide" -> (lastBall.extrasRuns ?: 1) + lastBall.runsOffBat
                    lastBall.extrasType == "no_ball" -> 1 + lastBall.runsOffBat + (lastBall.extrasRuns ?: 0)
                    else -> lastBall.runsOffBat + (lastBall.extrasRuns ?: 0)
                }
                val extrasToRemove = when {
                    lastBall.extrasType == "wide" -> (lastBall.extrasRuns ?: 1) + lastBall.runsOffBat
                    lastBall.extrasType == "no_ball" -> (lastBall.extrasRuns ?: 0) + 1
                    lastBall.extrasType in listOf("bye", "leg_bye") -> lastBall.extrasRuns ?: 0
                    else -> 0
                }

                val newRuns = (currentInnings.totalRuns - runsToRemove).coerceAtLeast(0)
                val newWickets = if (wasWicket) (currentInnings.totalWickets - 1).coerceAtLeast(0) else currentInnings.totalWickets
                val newBalls = if (isLegalBall) (currentInnings.totalBalls - 1).coerceAtLeast(0) else currentInnings.totalBalls
                val newWides = if (lastBall.extrasType == "wide") (currentInnings.wides - 1).coerceAtLeast(0) else currentInnings.wides
                val newNoBalls = if (lastBall.extrasType == "no_ball") (currentInnings.noBalls - 1).coerceAtLeast(0) else currentInnings.noBalls
                val newExtras = (currentInnings.extrasTotal - extrasToRemove).coerceAtLeast(0)

                val updatedInnings = scoringRepository.updateInnings(
                    currentInnings.id, newRuns, newWickets,
                    newBalls, newExtras, newWides, newNoBalls
                )

                val newBatsmanStats = computeBatsmanStats(updatedBalls, battingPlayers)
                val newBowlerStats = computeBowlerStats(updatedBalls, bowlingPlayers)

                _uiState.update {
                    it.copy(
                        innings = updatedInnings,
                        balls = updatedBalls,
                        batsmanStats = newBatsmanStats,
                        bowlerStats = newBowlerStats,
                        inningsComplete = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Undo error: ${e.message}", e)
                _uiState.update { it.copy(error = "Undo failed: ${e.message}") }
            }
        }
    }

    fun recordBall(
        runsOffBat: Int,
        extrasType: String? = null,
        extrasRuns: Int? = null,
        isWicket: Boolean = false,
        wicketType: String? = null,
        fielderName: String? = null
    ) {
        if (isProcessingBall) return
        val state = _uiState.value
        val innings = state.innings ?: return
        val striker = state.striker ?: return
        val bowler = state.currentBowler ?: return
        val match = state.match ?: return
        isProcessingBall = true

        viewModelScope.launch {
            try {
                val isWide = extrasType == "wide"
                val isNoBall = extrasType == "no_ball"
                val isBye = extrasType == "bye"
                val isLegBye = extrasType == "leg_bye"
                val isRetiredHurt = wicketType == "retired_hurt"
                val isLegalBall = !isWide && !isNoBall

                val overNo = innings.totalBalls / 6
                val ballNo = innings.totalBalls % 6 + 1
                val phase = when {
                    overNo < match.powerplayOvers -> "powerplay"
                    overNo < (match.totalOvers * 0.75).toInt() -> "middle"
                    else -> "death"
                }

                val totalRunsThisBall = when {
                    isWide -> (extrasRuns ?: 1) + runsOffBat
                    isNoBall -> 1 + runsOffBat + (extrasRuns ?: 0)
                    else -> runsOffBat + (extrasRuns ?: 0)
                }
                val extrasToSave = when {
                    isWide -> (extrasRuns ?: 1) + runsOffBat
                    isNoBall -> (extrasRuns ?: 0) + 1
                    isBye || isLegBye -> extrasRuns ?: 0
                    else -> 0
                }

                val commentary = generateCommentary(
                    runsOffBat, extrasType, extrasRuns,
                    isWicket, wicketType,
                    striker.fullName, bowler.fullName, fielderName
                )

                val insertedBall = scoringRepository.insertBall(
                    BallInsert(
                        inningsId = innings.id,
                        overNo = overNo,
                        ballNo = if (isLegalBall) ballNo else 0,
                        deliveryNo = null,
                        batsmanId = striker.id,
                        nonStrikerId = state.nonStriker?.id,
                        bowlerId = bowler.id,
                        runsOffBat = runsOffBat,
                        extrasRuns = if (extrasToSave > 0) extrasToSave else null,
                        extrasType = extrasType,
                        isWicket = isWicket,
                        wicketType = wicketType,
                        fielderName = fielderName,
                        isBoundary = runsOffBat == 4,
                        isSix = runsOffBat == 6,
                        inningsPhase = phase,
                        commentary = commentary
                    )
                )

                val newTotalBalls = if (isLegalBall) innings.totalBalls + 1 else innings.totalBalls
                val newTotalRuns = innings.totalRuns + totalRunsThisBall
                val newTotalWickets = if (isWicket && !isRetiredHurt) innings.totalWickets + 1 else innings.totalWickets
                val newWides = if (isWide) innings.wides + 1 else innings.wides
                val newNoBalls = if (isNoBall) innings.noBalls + 1 else innings.noBalls
                val newExtras = innings.extrasTotal + extrasToSave

                val updatedInnings = scoringRepository.updateInnings(
                    innings.id, newTotalRuns, newTotalWickets,
                    newTotalBalls, newExtras, newWides, newNoBalls
                )

                val newBalls = state.balls + insertedBall
                var newStriker = state.striker
                var newNonStriker = state.nonStriker

                if (!isWicket) {
                    when {
                        isWide -> {
                            val t = (extrasRuns ?: 1) + runsOffBat
                            if (t % 2 == 0) { newStriker = state.nonStriker; newNonStriker = state.striker }
                        }
                        isNoBall -> {
                            if (runsOffBat % 2 == 0) { newStriker = state.nonStriker; newNonStriker = state.striker }
                        }
                        isBye || isLegBye -> {
                            val b = extrasRuns ?: 0
                            if (b % 2 == 1) { newStriker = state.nonStriker; newNonStriker = state.striker }
                        }
                        else -> {
                            if (runsOffBat % 2 == 1) { newStriker = state.nonStriker; newNonStriker = state.striker }
                        }
                    }
                } else {
                    newStriker = null
                }

                val isOverEnd = newTotalBalls % 6 == 0 && newTotalBalls > 0 && isLegalBall
                if (isOverEnd && newStriker != null) {
                    val t = newStriker; newStriker = newNonStriker; newNonStriker = t
                }

                val maxWickets = match.playersPerSide - 1
                val currentTarget = target
                val targetChased = currentTarget != null && newTotalRuns >= currentTarget
                val isInningsComplete = newTotalWickets >= maxWickets ||
                        newTotalBalls >= match.totalOvers * 6 ||
                        targetChased

                if (isInningsComplete) {
                    scoringRepository.completeInnings(innings.id)
                }

                _uiState.update {
                    it.copy(
                        innings = updatedInnings,
                        balls = newBalls,
                        striker = newStriker,
                        nonStriker = newNonStriker,
                        currentBowler = if (isOverEnd) null else state.currentBowler,
                        batsmanStats = computeBatsmanStats(newBalls, state.battingTeamPlayers),
                        bowlerStats = computeBowlerStats(newBalls, state.bowlingTeamPlayers),
                        inningsComplete = isInningsComplete,
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Record ball error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            } finally {
                isProcessingBall = false
            }
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

    private fun generateCommentary(
        runs: Int, extrasType: String?, extrasRuns: Int?,
        isWicket: Boolean, wicketType: String?,
        batsmanName: String, bowlerName: String, fielderName: String? = null
    ): String {
        if (isWicket) return when (wicketType) {
            "bowled" -> "BOWLED! $batsmanName b $bowlerName"
            "caught" -> if (fielderName != null) "CAUGHT! c $fielderName b $bowlerName" else "CAUGHT! b $bowlerName"
            "lbw" -> "LBW! lbw b $bowlerName"
            "run_out" -> if (fielderName != null) "RUN OUT! run out ($fielderName)" else "RUN OUT!"
            "stumped" -> if (fielderName != null) "STUMPED! st $fielderName b $bowlerName" else "STUMPED!"
            "hit_wicket" -> "HIT WICKET! b $bowlerName"
            "retired_out" -> "RETIRED OUT!"
            "retired_hurt" -> "RETIRED HURT! ($batsmanName can return)"
            "obstructing" -> "OBSTRUCTING THE FIELD!"
            "timed_out" -> "TIMED OUT!"
            "handled_ball" -> "HANDLED THE BALL!"
            "hit_ball_twice" -> "HIT THE BALL TWICE!"
            else -> "OUT! b $bowlerName"
        }
        if (extrasType != null) return when (extrasType) {
            "wide" -> "Wide! ${(extrasRuns ?: 1) + runs} run(s)"
            "no_ball" -> "No Ball! Free hit next! $runs off bat"
            "bye" -> "${extrasRuns ?: 1} Bye(s)"
            "leg_bye" -> "${extrasRuns ?: 1} Leg Bye(s)"
            else -> "Extras"
        }
        return when (runs) {
            0 -> "Dot. $batsmanName defends"
            1 -> "$batsmanName takes a single"
            2 -> "Two runs!"
            3 -> "Three!"
            4 -> "FOUR! $batsmanName to the boundary!"
            5 -> "Five runs!"
            6 -> "SIX! $batsmanName maximum!"
            else -> "$runs runs"
        }
    }
}