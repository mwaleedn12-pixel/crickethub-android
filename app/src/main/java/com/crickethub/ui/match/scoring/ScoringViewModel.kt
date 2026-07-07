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

    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)

                val currentInnings = allInnings.find { it.status == "live" }
                val completedInnings = allInnings.filter { it.status == "completed" }

                // Agar dono innings complete hain — match khatam, post match par jao
                if (completedInnings.size >= 2) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            matchComplete = true
                        )
                    }
                    return@launch
                }

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
                        _uiState.update { it.copy(isLoading = false, inningsComplete = true) }
                        return@launch
                    }
                }

                val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, battingTeamId)
                val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, bowlingTeamId)

                if (currentInnings != null) {
                    // Live innings exist karti hai — sirf load karo
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
                    // Innings exist nahi karti — create karo
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
        viewModelScope.launch {
            try {
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val completedInnings = allInnings.filter { it.status == "completed" }
                when {
                    completedInnings.size >= 2 -> {
                        onMatchComplete()
                    }
                    completedInnings.size == 1 -> {
                        _uiState.update { it.copy(inningsComplete = false) }
                        loadMatch(matchId)
                    }
                    else -> {
                        _uiState.update { it.copy(inningsComplete = false) }
                        loadMatch(matchId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Next innings error: ${e.message}", e)
            }
        }
    }

    fun setStriker(player: Player) { _uiState.update { it.copy(striker = player) } }
    fun setNonStriker(player: Player) { _uiState.update { it.copy(nonStriker = player) } }
    fun setBowler(player: Player) { _uiState.update { it.copy(currentBowler = player) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun getMaxOversPerBowler(totalOvers: Int): Int {
        return when (totalOvers) {
            5  -> 1
            10 -> 2
            20 -> 4
            50 -> 10
            else -> totalOvers / 5
        }
    }

    fun canBowlerBowl(bowlerId: String, totalOvers: Int): Boolean {
        val state = _uiState.value
        val legalBallsBowled = state.balls.count {
            it.bowlerId == bowlerId &&
                    it.extrasType != "wide" &&
                    it.extrasType != "no_ball"
        }
        val oversBowled = legalBallsBowled / 6
        val maxOvers = getMaxOversPerBowler(totalOvers)
        return oversBowled < maxOvers
    }

    fun recordBall(
        runsOffBat: Int,
        extrasType: String? = null,
        extrasRuns: Int? = null,
        isWicket: Boolean = false,
        wicketType: String? = null
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
                val isWide   = extrasType == "wide"
                val isNoBall = extrasType == "no_ball"
                val isBye    = extrasType == "bye"
                val isLegBye = extrasType == "leg_bye"

                val isLegalBall = !isWide && !isNoBall

                val legalBallsSoFar = innings.totalBalls
                val overNo = legalBallsSoFar / 6
                val ballNo = legalBallsSoFar % 6 + 1

                val totalOvers = match.totalOvers
                val phase = when {
                    overNo < 6 -> "powerplay"
                    overNo < (totalOvers * 0.75).toInt() -> "middle"
                    else -> "death"
                }

                val totalRunsThisBall = when {
                    isWide   -> (extrasRuns ?: 1) + runsOffBat
                    isNoBall -> 1 + runsOffBat + (extrasRuns ?: 0)
                    else     -> runsOffBat + (extrasRuns ?: 0)
                }

                val extrasToSave = when {
                    isWide            -> (extrasRuns ?: 1) + runsOffBat
                    isNoBall          -> (extrasRuns ?: 0) + 1
                    isBye || isLegBye -> extrasRuns ?: 0
                    else              -> 0
                }

                val commentary = generateCommentary(
                    runsOffBat, extrasType, extrasRuns,
                    isWicket, wicketType,
                    striker.fullName, bowler.fullName
                )

                val insertedBall = scoringRepository.insertBall(
                    BallInsert(
                        inningsId    = innings.id,
                        overNo       = overNo,
                        ballNo       = if (isLegalBall) ballNo else 0,
                        deliveryNo   = null,
                        batsmanId    = striker.id,
                        nonStrikerId = state.nonStriker?.id,
                        bowlerId     = bowler.id,
                        runsOffBat   = runsOffBat,
                        extrasRuns   = if (extrasToSave > 0) extrasToSave else null,
                        extrasType   = extrasType,
                        isWicket     = isWicket,
                        wicketType   = wicketType,
                        isBoundary   = runsOffBat == 4,
                        isSix        = runsOffBat == 6,
                        inningsPhase = phase,
                        commentary   = commentary
                    )
                )

                val newTotalBalls   = if (isLegalBall) innings.totalBalls + 1 else innings.totalBalls
                val newTotalRuns    = innings.totalRuns + totalRunsThisBall
                val newTotalWickets = if (isWicket) innings.totalWickets + 1 else innings.totalWickets
                val newWides        = if (isWide) innings.wides + 1 else innings.wides
                val newNoBalls      = if (isNoBall) innings.noBalls + 1 else innings.noBalls
                val newExtras       = innings.extrasTotal + extrasToSave

                val updatedInnings = scoringRepository.updateInnings(
                    innings.id,
                    newTotalRuns,
                    newTotalWickets,
                    newTotalBalls,
                    newExtras,
                    newWides,
                    newNoBalls
                )

                val newBalls = state.balls + insertedBall
                val batsmanStats = computeBatsmanStats(newBalls, state.battingTeamPlayers)
                val bowlerStats  = computeBowlerStats(newBalls, state.bowlingTeamPlayers)

                var newStriker    = state.striker
                var newNonStriker = state.nonStriker

                if (!isWicket) {
                    when {
                        isWide -> {
                            val wideTotal = (extrasRuns ?: 1) + runsOffBat
                            if (wideTotal % 2 == 0) {
                                newStriker    = state.nonStriker
                                newNonStriker = state.striker
                            }
                        }
                        isNoBall -> {
                            if (runsOffBat % 2 == 0) {
                                newStriker    = state.nonStriker
                                newNonStriker = state.striker
                            }
                        }
                        isBye || isLegBye -> {
                            val byeRuns = extrasRuns ?: 0
                            if (byeRuns % 2 == 1) {
                                newStriker    = state.nonStriker
                                newNonStriker = state.striker
                            }
                        }
                        else -> {
                            if (runsOffBat % 2 == 1) {
                                newStriker    = state.nonStriker
                                newNonStriker = state.striker
                            }
                        }
                    }
                } else {
                    newStriker = null
                }

                val isOverEnd = newTotalBalls % 6 == 0 &&
                        newTotalBalls > 0 && isLegalBall

                if (isOverEnd && !isWicket && newStriker != null) {
                    val temp      = newStriker
                    newStriker    = newNonStriker
                    newNonStriker = temp
                }

                val maxWickets = match.playersPerSide - 1
                val isInningsComplete = newTotalWickets >= maxWickets ||
                        newTotalBalls >= match.totalOvers * 6

                if (isInningsComplete) {
                    scoringRepository.completeInnings(innings.id)
                }

                _uiState.update {
                    it.copy(
                        innings         = updatedInnings,
                        balls           = newBalls,
                        striker         = newStriker,
                        nonStriker      = newNonStriker,
                        currentBowler   = if (isOverEnd) null else state.currentBowler,
                        batsmanStats    = batsmanStats,
                        bowlerStats     = bowlerStats,
                        inningsComplete = isInningsComplete,
                        error           = null
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

    fun undoLastBall() {
        val lastBall = _uiState.value.balls.lastOrNull() ?: return
        viewModelScope.launch {
            try {
                scoringRepository.deleteLastBall(lastBall.id)
                loadMatch(_uiState.value.match?.id ?: return@launch)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
            val runs = playerBalls.sumOf { it.runsOffBat }
            val ballsFaced = playerBalls.count { it.extrasType != "wide" }
            val fours = playerBalls.count { it.isBoundary && !it.isSix }
            val sixes = playerBalls.count { it.isSix }
            val isOut = playerBalls.any { it.isWicket && it.wicketType != "run_out" }
            val dismissalType = playerBalls.firstOrNull { it.isWicket }?.wicketType
            statsMap[player.id] = BatsmanStats(
                player        = player,
                runs          = runs,
                balls         = ballsFaced,
                fours         = fours,
                sixes         = sixes,
                isOut         = isOut,
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
                player  = player,
                balls   = legalBalls,
                runs    = runs,
                wickets = wickets,
                wides   = playerBalls.count { it.extrasType == "wide" },
                noBalls = playerBalls.count { it.extrasType == "no_ball" }
            )
        }
        return statsMap
    }

    private fun generateCommentary(
        runs: Int,
        extrasType: String?,
        extrasRuns: Int?,
        isWicket: Boolean,
        wicketType: String?,
        batsmanName: String,
        bowlerName: String
    ): String {
        if (isWicket) {
            return when (wicketType) {
                "bowled"     -> "BOWLED! $bowlerName delivers a beauty and $batsmanName is clean bowled!"
                "caught"     -> "CAUGHT! $batsmanName mistimes the shot and is caught!"
                "lbw"        -> "LBW! Plumb in front, $batsmanName has to go!"
                "run_out"    -> "RUN OUT! $batsmanName is caught short of the crease!"
                "stumped"    -> "STUMPED! $batsmanName is out of his crease!"
                "hit_wicket" -> "HIT WICKET! What a way to get out for $batsmanName!"
                else         -> "OUT! $batsmanName is dismissed!"
            }
        }
        if (extrasType != null) {
            return when (extrasType) {
                "wide"    -> "WIDE! ${(extrasRuns ?: 1) + runs} run(s) to the batting side."
                "no_ball" -> "NO BALL! $bowlerName oversteps. Free hit next ball! $runs off the bat."
                "bye"     -> "${extrasRuns ?: 1} Bye(s)! Ball beats bat and keeper."
                "leg_bye" -> "${extrasRuns ?: 1} Leg Bye(s)! Off the pad and they run."
                else      -> "Extras."
            }
        }
        return when (runs) {
            0    -> "Dot ball. Tight delivery from $bowlerName, $batsmanName defends."
            1    -> "$batsmanName pushes for a quick single."
            2    -> "Good running! $batsmanName completes a quick two."
            3    -> "Three runs! Excellent running between the wickets."
            4    -> "FOUR! $batsmanName drives it beautifully to the boundary!"
            6    -> "SIX! $batsmanName launches it over the boundary for a maximum!"
            else -> "$runs runs."
        }
    }
}