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
import com.crickethub.ui.match.Interruption
import com.crickethub.ui.match.calculateDLS
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

    companion object {
        // Cached UI state per match, survives ViewModel destruction on back-press
        private val savedStates = mutableMapOf<String, ScoringUiState>()

        fun saveState(matchId: String, state: ScoringUiState) {
            if (matchId.isNotBlank()) savedStates[matchId] = state
        }

        fun getSavedState(matchId: String): ScoringUiState? = savedStates[matchId]

        fun clearSavedState(matchId: String) {
            savedStates.remove(matchId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val id = _uiState.value.match?.id ?: currentMatchId
        if (id.isNotBlank()) saveState(id, _uiState.value)
    }
    private val ballStack = ArrayDeque<Ball>()  // Stack for undo
    private var isProcessingBall = false
    private var inningsCompleteHandled = false
    private var isMatchLoaded = false
    private var currentMatchId = ""
    private var target: Int? = null
    private var dlsTeam1Score: Int = 0
    // Stack for undo — stores complete state snapshots (max 20)
    private val undoStack = java.util.ArrayDeque<ScoringUiState>()
    private var dlsTeam1TotalOvers: Int = 0
    private var dlsTeam1Interruptions: List<Interruption> = emptyList()
    private var dlsTeam2TotalOvers: Int = 0
    private var dlsTeam2Interruptions: MutableList<Interruption> = mutableListOf()

    // ── DLS ───────────────────────────────────────────────────────────────────
    fun enableDLS(team1Score: Int, team1TotalOvers: Int, team2TotalOvers: Int,
                  oversRemainingAtStop: Double, wicketsLostAtStop: Int, oversRemainingAtRestart: Double) {
        dlsTeam1Score = team1Score; dlsTeam1TotalOvers = team1TotalOvers
        dlsTeam1Interruptions = emptyList(); dlsTeam2TotalOvers = team2TotalOvers
        dlsTeam2Interruptions = mutableListOf(Interruption(1, oversRemainingAtStop, wicketsLostAtStop, oversRemainingAtRestart))
        val result = calculateDLS(team1Score, team1TotalOvers, emptyList(), team2TotalOvers, dlsTeam2Interruptions)
        _uiState.update { it.copy(dlsEnabled = true, dlsParScore = result.parScore, dlsTarget = result.targetScore,
            dlsTeam1Resource = result.team1Resource, dlsOversRemaining = oversRemainingAtRestart,
            dlsWicketsLost = wicketsLostAtStop, showDLSBanner = true) }
    }
    fun disableDLS() {
        dlsTeam1Score = 0; dlsTeam1TotalOvers = 0; dlsTeam1Interruptions = emptyList()
        dlsTeam2TotalOvers = 0; dlsTeam2Interruptions = mutableListOf()
        _uiState.update { it.copy(dlsEnabled = false, dlsParScore = null, dlsTarget = null,
            dlsTeam1Resource = 100.0, dlsOversRemaining = 0.0, dlsWicketsLost = 0, showDLSBanner = false) }
    }
    fun updateDLSParScore() {
        val state = _uiState.value
        if (!state.dlsEnabled || dlsTeam2TotalOvers == 0) return
        val ballsBowled = state.balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
        val wicketsNow = state.innings?.totalWickets ?: 0
        val oversUsed = ballsBowled / 6.0
        val updatedInterruptions = dlsTeam2Interruptions.toMutableList()
        if (updatedInterruptions.isNotEmpty()) {
            updatedInterruptions[updatedInterruptions.lastIndex] = updatedInterruptions.last().copy(wicketsLostAtStop = wicketsNow)
        }
        val result = calculateDLS(dlsTeam1Score, dlsTeam1TotalOvers, dlsTeam1Interruptions, dlsTeam2TotalOvers, updatedInterruptions)
        val progressFraction = if (dlsTeam2TotalOvers > 0) oversUsed / dlsTeam2TotalOvers else 0.0
        _uiState.update { it.copy(dlsParScore = (result.parScore * progressFraction).toInt(),
            dlsTarget = result.targetScore, dlsWicketsLost = wicketsNow) }
    }

    // ── Restore players from balls ────────────────────────────────────────────
    private fun restorePlayersFromBalls(
        balls: List<Ball>,
        battingPlayers: List<Player>,
        bowlingPlayers: List<Player>,
        inningsLegalBalls: Int? = null
    ): Triple<Player?, Player?, Player?> {
        if (balls.isEmpty()) return Triple(null, null, null)

        val lastBall = balls.last()

        val outBatsmanIds = balls
            .filter { it.isWicket && it.wicketType != "retired_hurt" }
            .map { it.batsmanId }
            .toSet()

        var striker: Player? = battingPlayers.find { it.id == lastBall.batsmanId }
        var nonStriker: Player? = lastBall.nonStrikerId
            ?.let { nsId -> battingPlayers.find { it.id == nsId } }

        var strikerClearedByWicket = false
        var nonStrikerClearedByWicket = false

        if (lastBall.isWicket) {
            when {
                lastBall.batsmanId == nonStriker?.id -> {
                    nonStriker = null; nonStrikerClearedByWicket = true
                }
                else -> {
                    striker = null; strikerClearedByWicket = true
                }
            }
        } else {
            val strikeChanged = when (lastBall.extrasType) {
                "wide" -> (lastBall.extrasRuns ?: 1) % 2 == 1
                "no_ball" -> lastBall.runsOffBat % 2 == 1
                "bye", "leg_bye" -> (lastBall.extrasRuns ?: 0) % 2 == 1
                else -> lastBall.runsOffBat % 2 == 1
            }
            if (strikeChanged) {
                val tmp = striker; striker = nonStriker; nonStriker = tmp
            }
        }

        val legalBalls = inningsLegalBalls
            ?: balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
        val overComplete = legalBalls > 0 && legalBalls % 6 == 0
        if (overComplete && striker != null) {
            val tmp = striker; striker = nonStriker; nonStriker = tmp
        }

        if (striker?.id?.let { it in outBatsmanIds } == true) striker = null
        if (nonStriker?.id?.let { it in outBatsmanIds } == true) nonStriker = null

        val notOutBatsmen = balls
            .flatMap { listOfNotNull(it.batsmanId, it.nonStrikerId) }
            .distinct()
            .filter { id -> id !in outBatsmanIds }
            .mapNotNull { id -> battingPlayers.find { it.id == id } }

        if (striker == null && !strikerClearedByWicket) {
            striker = notOutBatsmen.firstOrNull { it.id != nonStriker?.id }
        }
        if (nonStriker == null && !nonStrikerClearedByWicket) {
            nonStriker = notOutBatsmen.firstOrNull { it.id != striker?.id }
        }

        val bowler = if (overComplete) {
            null
        } else {
            val lastLegalBall = balls
                .filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
                .lastOrNull() ?: lastBall
            bowlingPlayers.find { it.id == lastLegalBall.bowlerId }
        }

        return Triple(striker, nonStriker, bowler)
    }

    // ── Crease restore (notebook checklist) ───────────────────────────────────
    private data class Crease(
        val striker: Player?,
        val nonStriker: Player?,
        val bowler: Player?,
        val reason: String
    )

    private fun restoreCrease(
        balls: List<Ball>,
        inningsLegalBalls: Int,
        battingPlayers: List<Player>,
        bowlingPlayers: List<Player>,
        cached: ScoringUiState?
    ): Crease {
        if (balls.isEmpty()) return Crease(null, null, null, "no balls yet")

        val reason = StringBuilder()
        val lastBall = balls.last()

        val outIds = balls
            .filter { it.isWicket && it.wicketType != "retired_hurt" }
            .map { it.batsmanId }
            .toSet()

        var striker: Player? = battingPlayers.find { it.id == lastBall.batsmanId }
        var nonStriker: Player? = lastBall.nonStrikerId
            ?.let { id -> battingPlayers.find { it.id == id } }
        var strikerEmptiedByWicket = false
        var nonStrikerEmptiedByWicket = false

        if (lastBall.isWicket && lastBall.wicketType != "retired_hurt") {
            if (lastBall.batsmanId == nonStriker?.id) {
                nonStriker = null; nonStrikerEmptiedByWicket = true
            } else {
                striker = null; strikerEmptiedByWicket = true
            }
            reason.append("wicket on last ball; ")
        } else if (lastBall.wicketType == "retired_hurt") {
            striker = null; strikerEmptiedByWicket = true
            reason.append("retired hurt; ")
        } else {
            val rotated = when (lastBall.extrasType) {
                "wide" -> (lastBall.extrasRuns ?: 1) % 2 == 1
                "no_ball" -> lastBall.runsOffBat % 2 == 1
                "bye", "leg_bye" -> (lastBall.extrasRuns ?: 0) % 2 == 1
                else -> lastBall.runsOffBat % 2 == 1
            }
            if (rotated) {
                val t = striker; striker = nonStriker; nonStriker = t
                reason.append("strike rotated; ")
            }
        }

        val overComplete = inningsLegalBalls > 0 && inningsLegalBalls % 6 == 0
        if (overComplete && striker != null) {
            val t = striker; striker = nonStriker; nonStriker = t
        }

        if (striker?.id?.let { it in outIds } == true) { striker = null; strikerEmptiedByWicket = true }
        if (nonStriker?.id?.let { it in outIds } == true) { nonStriker = null; nonStrikerEmptiedByWicket = true }

        val notOut = balls
            .flatMap { listOfNotNull(it.batsmanId, it.nonStrikerId) }
            .distinct()
            .filter { id -> id !in outIds }
            .mapNotNull { id -> battingPlayers.find { it.id == id } }

        if (striker == null && !strikerEmptiedByWicket) {
            striker = cached?.striker?.takeIf { it.id !in outIds }
                ?: notOut.firstOrNull { it.id != nonStriker?.id }
            if (striker != null) reason.append("striker refilled; ")
        }
        if (nonStriker == null && !nonStrikerEmptiedByWicket) {
            nonStriker = cached?.nonStriker?.takeIf { it.id !in outIds }
                ?: notOut.firstOrNull { it.id != striker?.id }
            if (nonStriker != null) reason.append("nonStriker refilled; ")
        }
        if (striker != null && striker.id == nonStriker?.id) nonStriker = null

        var bowler: Player? = null
        if (overComplete) {
            reason.append("over complete -> ask bowler; ")
        } else {
            val lastLegal = balls
                .filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
                .lastOrNull() ?: lastBall
            bowler = bowlingPlayers.find { it.id == lastLegal.bowlerId }
                ?: cached?.currentBowler
            reason.append("over incomplete -> keep bowler; ")
        }

        return Crease(striker, nonStriker, bowler, reason.toString())
    }

    // ── Resume Match ──────────────────────────────────────────────────────────
    fun resumeMatch(matchId: String, force: Boolean = false) {
        if (!force && isMatchLoaded && currentMatchId == matchId && _uiState.value.innings != null) {
            android.util.Log.d("CricketHub", "resumeMatch: already restored, keeping current state")
            return
        }
        val cached = getSavedState(matchId)
        if (cached != null && cached.balls.isNotEmpty()) {
            _uiState.value = cached.copy(isLoading = false, error = null)
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.balls.isEmpty(), error = null) }
            try {
                val match = matchRepository.getMatchById(matchId) ?: run {
                    _uiState.update { it.copy(isLoading = false, error = "Match not found") }
                    return@launch
                }
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val liveInnings = allInnings.find { it.status == "live" }
                    ?: allInnings.filter { it.status != "completed" }.maxByOrNull { it.inningsNo }
                    ?: allInnings.maxByOrNull { it.inningsNo }

                if (liveInnings == null) {
                    val battingFirstId = match.battingFirstId ?: match.team1Id
                    val bowlingFirstId = if (battingFirstId == match.team1Id) match.team2Id else match.team1Id
                    val newInnings = scoringRepository.createInnings(
                        InningsInsert(matchId = matchId, inningsNo = 1,
                            battingTeamId = battingFirstId, bowlingTeamId = bowlingFirstId))
                    val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, battingFirstId)
                    val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, bowlingFirstId)
                    isMatchLoaded = true; currentMatchId = matchId
                    _uiState.update { it.copy(isLoading = false, match = match, innings = newInnings,
                        balls = emptyList(), striker = null, nonStriker = null, currentBowler = null,
                        battingTeamPlayers = battingPlayers, bowlingTeamPlayers = bowlingPlayers,
                        batsmanStats = emptyMap(), bowlerStats = emptyMap(),
                        inningsComplete = false, matchComplete = false, error = null) }
                    return@launch
                }

                val balls = scoringRepository.getBallsByInnings(liveInnings.id)
                val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, liveInnings.battingTeamId)
                val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, liveInnings.bowlingTeamId)
                val batsmanStats = computeBatsmanStats(balls, battingPlayers)
                val bowlerStats = computeBowlerStats(balls, bowlingPlayers)

                val crease = restoreCrease(
                    balls = balls,
                    inningsLegalBalls = liveInnings.totalBalls,
                    battingPlayers = battingPlayers,
                    bowlingPlayers = bowlingPlayers,
                    cached = if (cached?.innings?.id == liveInnings.id) cached else null
                )
                val striker = crease.striker
                val nonStriker = crease.nonStriker
                val currentBowler = crease.bowler
                android.util.Log.d("CricketHub", "RESUME: balls=${balls.size} striker=${striker?.fullName} nonStriker=${nonStriker?.fullName} bowler=${currentBowler?.fullName}")
                ballStack.clear()
                balls.forEach { ball -> ballStack.addLast(ball) }

                val firstInnings = allInnings.find { it.inningsNo == 1 }
                if (liveInnings.inningsNo == 2 && firstInnings != null) {
                    target = firstInnings.totalRuns + 1
                }

                isMatchLoaded = true; currentMatchId = matchId
                _uiState.update { it.copy(
                    isLoading = false, match = match, innings = liveInnings, balls = balls,
                    striker = striker, nonStriker = nonStriker, currentBowler = currentBowler,
                    battingTeamPlayers = battingPlayers, bowlingTeamPlayers = bowlingPlayers,
                    batsmanStats = batsmanStats, bowlerStats = bowlerStats,
                    inningsComplete = false, matchComplete = false, error = null) }

                android.util.Log.d("CricketHub", "resumeMatch: ${liveInnings.totalRuns}/${liveInnings.totalWickets} " +
                        "totalBalls=${liveInnings.totalBalls} " +
                        "striker=${striker?.fullName} nonStriker=${nonStriker?.fullName} " +
                        "bowler=${currentBowler?.fullName} | ${crease.reason}")

            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "resumeMatch error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Load Match ────────────────────────────────────────────────────────────
    fun loadMatch(matchId: String, forceReload: Boolean = false) {
        if (isMatchLoaded && matchId == currentMatchId && !forceReload) {
            _uiState.update { it.copy(inningsComplete = false, matchComplete = false, error = null) }
            return
        }
        isMatchLoaded = true; currentMatchId = matchId; inningsCompleteHandled = false
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val currentInnings = allInnings.find { it.status == "live" }
                val completedInnings = allInnings.filter { it.status == "completed" }
                if (completedInnings.size >= 2) {
                    val inn1 = allInnings.find { it.inningsNo == 1 }
                    val inn2 = allInnings.find { it.inningsNo == 2 }
                    val tied = inn1 != null && inn2 != null && inn1.totalRuns == inn2.totalRuns
                    val superOverInnings = allInnings.filter { it.inningsNo >= 3 }
                    val superOverLive = superOverInnings.find { it.status == "live" }
                    val superOverComplete = superOverInnings.filter { it.status == "completed" }
                    when {
                        superOverLive != null -> { if (superOverLive.inningsNo == 4) target = allInnings.find { it.inningsNo == 3 }?.totalRuns?.plus(1) }
                        superOverComplete.size >= 2 -> { _uiState.update { it.copy(isLoading = false, matchComplete = true) }; return@launch }
                        tied && match.superOverEnabled -> { _uiState.update { it.copy(isLoading = false, matchComplete = true) }; return@launch }
                        else -> { _uiState.update { it.copy(isLoading = false, matchComplete = true) }; return@launch }
                    }
                }
                val firstInnings = allInnings.find { it.inningsNo == 1 }
                val thirdInnings = allInnings.find { it.inningsNo == 3 }
                target = when {
                    currentInnings?.inningsNo == 4 && thirdInnings != null -> thirdInnings.totalRuns + 1
                    firstInnings != null && firstInnings.status == "completed" &&
                            (currentInnings?.inningsNo == 2 || allInnings.count { it.status == "completed" } == 1) -> firstInnings.totalRuns + 1
                    else -> null
                }
                val battingTeamId: String; val bowlingTeamId: String
                when {
                    allInnings.isEmpty() -> { battingTeamId = match.battingFirstId ?: match.team1Id; bowlingTeamId = if (battingTeamId == match.team1Id) match.team2Id else match.team1Id }
                    currentInnings != null -> { battingTeamId = currentInnings.battingTeamId; bowlingTeamId = currentInnings.bowlingTeamId }
                    completedInnings.size == 1 -> { val first = completedInnings.first(); battingTeamId = first.bowlingTeamId; bowlingTeamId = first.battingTeamId }
                    else -> { _uiState.update { it.copy(isLoading = false) }; return@launch }
                }
                if (currentInnings != null) {
                    resumeMatch(matchId)
                    return@launch
                }

                val battingPlayers = scoringRepository.getPlayingXIPlayers(matchId, battingTeamId)
                val bowlingPlayers = scoringRepository.getPlayingXIPlayers(matchId, bowlingTeamId)
                run {
                    val inningsNo = when {
                        allInnings.isEmpty() -> 1
                        completedInnings.size == 1 && allInnings.none { it.inningsNo >= 3 } -> 2
                        else -> (allInnings.maxOfOrNull { it.inningsNo } ?: 0) + 1
                    }
                    val newInnings = scoringRepository.createInnings(InningsInsert(matchId, inningsNo, battingTeamId, bowlingTeamId))
                    _uiState.update { it.copy(match = match, innings = newInnings, balls = emptyList(),
                        striker = null, nonStriker = null, currentBowler = null,
                        battingTeamPlayers = battingPlayers, bowlingTeamPlayers = bowlingPlayers,
                        batsmanStats = emptyMap(), bowlerStats = emptyMap(),
                        inningsComplete = false, matchComplete = false, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    // ── Next Innings ──────────────────────────────────────────────────────────
    fun checkAndStartNextInnings(matchId: String, onMatchComplete: () -> Unit) {
        if (inningsCompleteHandled) return
        inningsCompleteHandled = true
        viewModelScope.launch {
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val completedInnings = allInnings.filter { it.status == "completed" }
                when {
                    completedInnings.size >= 4 -> onMatchComplete()
                    completedInnings.size == 3 -> {
                        val superInn1 = allInnings.find { it.inningsNo == 3 }
                        if (superInn1 != null) { target = superInn1.totalRuns + 1; _uiState.update { it.copy(inningsComplete = false, matchComplete = false) }; isMatchLoaded = false; inningsCompleteHandled = false; loadMatch(matchId) } else onMatchComplete()
                    }
                    completedInnings.size >= 2 -> {
                        val inn1 = allInnings.find { it.inningsNo == 1 }; val inn2 = allInnings.find { it.inningsNo == 2 }
                        if (inn1 != null && inn2 != null && inn2.totalRuns == inn1.totalRuns && match.superOverEnabled) {
                            if (allInnings.find { it.inningsNo == 3 } == null) { scoringRepository.createInnings(InningsInsert(matchId, 3, inn2.battingTeamId, inn2.bowlingTeamId)); target = null; _uiState.update { it.copy(inningsComplete = false, matchComplete = false) }; isMatchLoaded = false; inningsCompleteHandled = false; loadMatch(matchId) } else onMatchComplete()
                        } else onMatchComplete()
                    }
                    completedInnings.size == 1 -> { target = completedInnings.first().totalRuns + 1; _uiState.update { it.copy(inningsComplete = false) }; isMatchLoaded = false; inningsCompleteHandled = false; loadMatch(matchId) }
                    else -> { isMatchLoaded = false; inningsCompleteHandled = false; loadMatch(matchId) }
                }
            } catch (e: Exception) { inningsCompleteHandled = false; _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── Player Selection ──────────────────────────────────────────────────────
    fun setStriker(player: Player) { _uiState.update { it.copy(striker = player) } }
    fun setNonStriker(player: Player) { _uiState.update { it.copy(nonStriker = player) } }
    fun setBowler(player: Player) { _uiState.update { it.copy(currentBowler = player) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun getMaxOversPerBowler(totalOvers: Int): Int = when (totalOvers) { 5 -> 1; 10 -> 2; 20 -> 4; 50 -> 10; else -> totalOvers / 5 }
    fun canBowlerBowl(bowlerId: String, totalOvers: Int): Boolean {
        val balls = _uiState.value.balls.count { it.bowlerId == bowlerId && it.extrasType != "wide" && it.extrasType != "no_ball" }
        return (balls / 6) < getMaxOversPerBowler(totalOvers)
    }

    // ── Penalty & Manual ──────────────────────────────────────────────────────
    fun addPenaltyRuns(team: String) {
        viewModelScope.launch {
            try {
                val innings = _uiState.value.innings ?: return@launch
                if (team == "batting") { val updated = scoringRepository.updateInnings(innings.id, innings.totalRuns + 5, innings.totalWickets, innings.totalBalls, innings.extrasTotal + 5, innings.wides, innings.noBalls); _uiState.update { it.copy(innings = updated) } }
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }
    fun manualEdit(runs: Int, wickets: Int) {
        viewModelScope.launch {
            try {
                val innings = _uiState.value.innings ?: return@launch
                val updated = scoringRepository.updateInnings(innings.id, runs, wickets, innings.totalBalls, innings.extrasTotal, innings.wides, innings.noBalls)
                _uiState.update { it.copy(innings = updated) }
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── Undo ──────────────────────────────────────────────────────────────────
    fun undoLastBall() {
        viewModelScope.launch {
            val state = _uiState.value
            val innings = state.innings ?: return@launch
            val balls = state.balls
            if (balls.isEmpty()) {
                _uiState.update { it.copy(error = "Nothing to undo") }
                return@launch
            }
            try {
                _uiState.update { it.copy(isLoading = true) }
                val lastBall = balls.last()

                scoringRepository.deleteLastBall(lastBall.id)

                if (ballStack.isNotEmpty()) ballStack.removeLast()

                val newBalls = balls.dropLast(1)
                val batsmanStats = computeBatsmanStats(newBalls, state.battingTeamPlayers)
                val bowlerStats = computeBowlerStats(newBalls, state.bowlingTeamPlayers)

                val (striker, nonStriker, bowler) = restorePlayersFromBalls(
                    newBalls, state.battingTeamPlayers, state.bowlingTeamPlayers
                )

                val totalRuns = newBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) }
                val totalWickets = newBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
                val totalLegalBalls = newBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }

                val updatedInnings = scoringRepository.updateInnings(
                    innings.id, totalRuns, totalWickets, totalLegalBalls,
                    newBalls.sumOf { if (it.extrasRuns != null) it.extrasRuns else 0 },
                    newBalls.count { it.extrasType == "wide" },
                    newBalls.count { it.extrasType == "no_ball" }
                )

                _uiState.update { it.copy(
                    isLoading = false,
                    balls = newBalls,
                    innings = updatedInnings,
                    striker = striker,
                    nonStriker = nonStriker,
                    currentBowler = bowler,
                    batsmanStats = batsmanStats,
                    bowlerStats = bowlerStats,
                    error = null
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Undo failed: ${e.message}") }
            }
        }
    }


    fun recordBall(runsOffBat: Int, extrasType: String? = null, extrasRuns: Int? = null,
                   isWicket: Boolean = false, wicketType: String? = null, fielderName: String? = null,
                   nonStrikerOut: Boolean = false) {
        if (isProcessingBall) return
        val currentState = _uiState.value
        if (undoStack.size >= 20) undoStack.removeFirst()
        undoStack.addLast(currentState)
        val state = _uiState.value
        val innings = state.innings ?: return
        val striker = state.striker ?: return
        val bowler = state.currentBowler ?: return
        val match = state.match ?: return
        isProcessingBall = true
        viewModelScope.launch {
            try {
                val isWide = extrasType == "wide"; val isNoBall = extrasType == "no_ball"
                val isBye = extrasType == "bye"; val isLegBye = extrasType == "leg_bye"
                val isRetiredHurt = wicketType == "retired_hurt"
                val isLegal = !isWide && !isNoBall
                val overNo = innings.totalBalls / 6; val ballNo = innings.totalBalls % 6 + 1
                val phase = when { overNo < match.powerplayOvers -> "powerplay"; overNo < (match.totalOvers * 0.75).toInt() -> "middle"; else -> "death" }
                val totalRunsThisBall = when { isWide -> (extrasRuns ?: 1) + runsOffBat; isNoBall -> 1 + runsOffBat + (extrasRuns ?: 0); else -> runsOffBat + (extrasRuns ?: 0) }
                val extrasToSave = when { isWide -> (extrasRuns ?: 1) + runsOffBat; isNoBall -> (extrasRuns ?: 0) + 1; isBye || isLegBye -> extrasRuns ?: 0; else -> 0 }
                // The striker always FACES the ball. On a non-striker run-out the wicket
                // belongs to the non-striker, but the delivery + any runs are the striker's.
                // dismissedBatsmanId names the victim without stealing the striker's ball.
                val runOutNonStriker = wicketType == "run_out" && nonStrikerOut
                val dismissedBatsmanId = if (runOutNonStriker) state.nonStriker?.id else null
                val insertedBall = scoringRepository.insertBall(BallInsert(
                    inningsId = innings.id, overNo = overNo, ballNo = if (isLegal) ballNo else 0, deliveryNo = null,
                    batsmanId = striker.id,
                    nonStrikerId = state.nonStriker?.id, bowlerId = bowler.id,
                    runsOffBat = runsOffBat, extrasRuns = if (extrasToSave > 0) extrasToSave else null,
                    extrasType = extrasType, isWicket = isWicket, wicketType = wicketType, fielderName = fielderName,
                    dismissedBatsmanId = dismissedBatsmanId,
                    isBoundary = runsOffBat == 4, isSix = runsOffBat == 6, inningsPhase = phase,
                    commentary = generateCommentary(runsOffBat, extrasType, extrasRuns, isWicket, wicketType,
                        if (runOutNonStriker) (state.nonStriker?.fullName ?: striker.fullName) else striker.fullName,
                        bowler.fullName, fielderName)))
                val newTotalBalls = if (isLegal) innings.totalBalls + 1 else innings.totalBalls
                val newTotalRuns = innings.totalRuns + totalRunsThisBall
                val newTotalWickets = if (isWicket && !isRetiredHurt) innings.totalWickets + 1 else innings.totalWickets
                val updatedInnings = scoringRepository.updateInnings(innings.id, newTotalRuns, newTotalWickets, newTotalBalls,
                    innings.extrasTotal + extrasToSave, if (isWide) innings.wides + 1 else innings.wides, if (isNoBall) innings.noBalls + 1 else innings.noBalls)
                val newBalls = state.balls + insertedBall
                ballStack.addLast(insertedBall)
                var newStriker: Player? = striker; var newNonStriker: Player? = state.nonStriker
                if (!isWicket) {
                    val changeStrike = when { isWide -> ((extrasRuns ?: 1) + runsOffBat) % 2 == 1; isNoBall -> runsOffBat % 2 == 1; isBye || isLegBye -> (extrasRuns ?: 0) % 2 == 1; else -> runsOffBat % 2 == 1 }
                    if (changeStrike) { val t = newStriker; newStriker = newNonStriker; newNonStriker = t }
                } else {
                    if (runOutNonStriker) {
                        newNonStriker = null
                    } else {
                        newStriker = null
                    }
                }
                val isOverEnd = newTotalBalls % 6 == 0 && newTotalBalls > 0 && isLegal
                if (isOverEnd && newStriker != null) { val t = newStriker; newStriker = newNonStriker; newNonStriker = t }
                val maxOvers = if (innings.inningsNo >= 3) 1 else match.totalOvers
                val targetChased = target != null && newTotalRuns >= target!!
                val isInningsComplete = newTotalWickets >= match.playersPerSide - 1 || newTotalBalls >= maxOvers * 6 || targetChased
                if (isInningsComplete) scoringRepository.completeInnings(innings.id)
                _uiState.update { it.copy(innings = updatedInnings, balls = newBalls,
                    striker = newStriker, nonStriker = newNonStriker,
                    currentBowler = if (isOverEnd) null else state.currentBowler,
                    batsmanStats = computeBatsmanStats(newBalls, state.battingTeamPlayers),
                    bowlerStats = computeBowlerStats(newBalls, state.bowlingTeamPlayers),
                    inningsComplete = isInningsComplete, error = null) }
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } } finally { isProcessingBall = false }
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private fun computeBatsmanStats(balls: List<Ball>, players: List<Player>): Map<String, BatsmanStats> {
        return players.associate { player ->
            val pb = balls.filter { it.batsmanId == player.id }
            // A run-out can dismiss the non-striker; that ball carries dismissedBatsmanId
            // instead of batsmanId. Count both so the victim shows out.
            val dismissalBall = balls.firstOrNull {
                it.isWicket && (it.batsmanId == player.id || it.dismissedBatsmanId == player.id)
            }
            val isOut = dismissalBall != null && dismissalBall.wicketType != "retired_hurt"
            player.id to BatsmanStats(player = player,
                runs = pb.sumOf { it.runsOffBat },
                balls = pb.count { it.extrasType != "wide" },
                fours = pb.count { it.isBoundary && !it.isSix },
                sixes = pb.count { it.isSix },
                isOut = isOut,
                dismissalType = dismissalBall?.wicketType,
                fielderName = dismissalBall?.fielderName,
                bowlerOnWicket = dismissalBall?.bowlerId)
        }
    }
    private fun computeBowlerStats(balls: List<Ball>, players: List<Player>): Map<String, BowlerStats> {
        val map = mutableMapOf<String, BowlerStats>()
        players.forEach { player ->
            val pb = balls.filter { it.bowlerId == player.id }
            if (pb.isEmpty()) return@forEach
            val legal = pb.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
            map[player.id] = BowlerStats(player = player, balls = legal,
                runs = pb.sumOf { if (it.extrasType in listOf("bye", "leg_bye")) 0 else it.runsOffBat + (it.extrasRuns ?: 0) },
                wickets = pb.count { it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "handled_ball", "timed_out", "retired_hurt", "retired_out") },
                overs = "${legal / 6}.${legal % 6}",
                wides = pb.count { it.extrasType == "wide" }, noBalls = pb.count { it.extrasType == "no_ball" })
        }
        return map
    }
    private fun generateCommentary(runs: Int, extrasType: String?, extrasRuns: Int?,
                                   isWicket: Boolean, wicketType: String?, batsmanName: String, bowlerName: String, fielderName: String? = null): String {
        if (isWicket) return when (wicketType) {
            "bowled" -> "BOWLED! $batsmanName b $bowlerName"; "caught" -> if (fielderName != null) "CAUGHT! c $fielderName b $bowlerName" else "CAUGHT! b $bowlerName"
            "lbw" -> "LBW! lbw b $bowlerName"; "run_out" -> if (fielderName != null) "RUN OUT! run out ($fielderName)" else "RUN OUT!"
            "stumped" -> if (fielderName != null) "STUMPED! st $fielderName b $bowlerName" else "STUMPED!"
            "hit_wicket" -> "HIT WICKET! b $bowlerName"; "retired_hurt" -> "RETIRED HURT! ($batsmanName can return)"; else -> "OUT! b $bowlerName" }
        if (extrasType != null) return when (extrasType) {
            "wide" -> "Wide! ${(extrasRuns ?: 1) + runs} run(s)"; "no_ball" -> "No Ball! Free hit next!"
            "bye" -> "${extrasRuns ?: 1} Bye(s)"; "leg_bye" -> "${extrasRuns ?: 1} Leg Bye(s)"; else -> "Extras" }
        return when (runs) { 0 -> "Dot. $batsmanName defends"; 1 -> "$batsmanName takes a single"; 2 -> "Two runs!"; 3 -> "Three!"; 4 -> "FOUR! $batsmanName to the boundary!"; 6 -> "SIX! $batsmanName maximum!"; else -> "$runs runs" }
    }
}