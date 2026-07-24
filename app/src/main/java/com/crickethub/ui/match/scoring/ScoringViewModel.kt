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
import io.github.jan.supabase.postgrest.postgrest
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
    // Set synchronously while a restore is running. force=true bypasses the
    // "already restored" guard, so without this two resumeMatch calls can overlap
    // and BOTH try to create innings 1 -> duplicate key error.
    private var isRestoring = false
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
        if (isRestoring) {
            android.util.Log.d("CricketHub", "resumeMatch: restore already in flight, ignored")
            return
        }
        isRestoring = true
        // On a FORCED reload (innings transition) never restore the cache - it holds the
        // PREVIOUS innings and would flash its score/players onto the new innings.
        val cached = if (force) null else getSavedState(matchId)
        if (cached != null && cached.balls.isNotEmpty()) {
            _uiState.value = cached.copy(isLoading = false, error = null)
        }
        if (force) {
            // clear the finished innings so the new one loads onto a clean slate
            _uiState.update { it.copy(innings = null, balls = emptyList(),
                striker = null, nonStriker = null, currentBowler = null,
                batsmanStats = emptyMap(), bowlerStats = emptyMap(),
                inningsComplete = false, matchComplete = false) }
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
                    // Another concurrent restore may have just created innings 1.
                    // If the insert collides, re-read and use the existing row.
                    val newInnings = try {
                        scoringRepository.createInnings(
                            InningsInsert(matchId = matchId, inningsNo = 1,
                                battingTeamId = battingFirstId, bowlingTeamId = bowlingFirstId))
                    } catch (dup: Exception) {
                        android.util.Log.w("CricketHub", "innings 1 already exists, reusing: ${dup.message}")
                        scoringRepository.invalidateInningsCache(matchId)
                        scoringRepository.getInningsByMatch(matchId).firstOrNull { it.inningsNo == 1 }
                            ?: throw dup
                    }
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
            } finally {
                isRestoring = false
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
                // Same fallback as resumeMatch: a newly created innings may carry the DB
                // default status ("pending") rather than "live". Treat any non-completed
                // innings as the current one, otherwise a new super over is invisible here
                // and "Play Again" appears to do nothing.
                val currentInnings = allInnings.find { it.status == "live" }
                    ?: allInnings.filter { it.status != "completed" }.maxByOrNull { it.inningsNo }
                val completedInnings = allInnings.filter { it.status == "completed" }
                // FAST PATH: a live innings exists -> resumeMatch handles it. Skip the
                // redundant target/team analysis below AND its duplicate fetches. This
                // removes one full match+innings+balls+players round-trip per transition.
                if (currentInnings != null) {
                    // set chase target for 2nd innings / super-over chase before delegating
                    val firstInn = allInnings.find { it.inningsNo == 1 }
                    val prevOdd = allInnings.find { it.inningsNo == currentInnings.inningsNo - 1 }
                    target = when {
                        currentInnings.inningsNo == 2 && firstInn != null -> firstInn.totalRuns + 1
                        currentInnings.inningsNo >= 4 && currentInnings.inningsNo % 2 == 0 && prevOdd != null -> prevOdd.totalRuns + 1
                        else -> null
                    }
                    // force = true: loadMatch has just decided this innings must load.
                    // Without force, resumeMatch's "already restored" guard sees
                    // isMatchLoaded (set moments ago) and returns, leaving the screen
                    // stuck on the finished innings.
                    resumeMatch(matchId, force = true)
                    return@launch
                }
                // If there's a live innings (main or super over), fall through and load it.
                // Only decide match-complete when NOTHING is live. Super-over creation and
                // repeat/tie decisions are owned by checkAndStartNextInnings, not here.
                if (completedInnings.size >= 2 && currentInnings == null) {
                    val superOverInnings = allInnings.filter { it.inningsNo >= 3 }
                    // A super over is live? set its chase target and fall through to load it.
                    val superOverLive = superOverInnings.find { it.status == "live" }
                    if (superOverLive != null) {
                        if (superOverLive.inningsNo % 2 == 0) {
                            // even = chase; target = previous (odd) super-over innings + 1
                            target = allInnings.find { it.inningsNo == superOverLive.inningsNo - 1 }?.totalRuns?.plus(1)
                        }
                        // fall through - currentInnings is null here, so let the normal
                        // live-innings path below pick it up via resumeMatch.
                    } else {
                        // Nothing live and 2+ complete. Before declaring the match over,
                        // check whether a super over is still owed. This must happen HERE
                        // too, not only on the inningsComplete event - otherwise backing
                        // out and reopening at the end of innings 2 shows "match complete"
                        // and the super over can never start.
                        val soInnings = allInnings.filter { it.inningsNo >= 3 }
                        val lastNo = allInnings.maxOfOrNull { it.inningsNo } ?: 0
                        val needsSuperOver = when {
                            // main match tied, super over enabled, none created yet
                            soInnings.isEmpty() -> {
                                val i1 = allInnings.find { it.inningsNo == 1 }
                                val i2 = allInnings.find { it.inningsNo == 2 }
                                i1 != null && i2 != null && i1.totalRuns == i2.totalRuns && match.superOverEnabled
                            }
                            // an odd super-over innings finished but its chase never started
                            lastNo % 2 == 1 -> true
                            else -> false
                        }
                        if (needsSuperOver) {
                            android.util.Log.d("CricketHub", "loadMatch: super over owed, starting it")
                            inningsCompleteHandled = false
                            checkAndStartNextInnings(matchId) {
                                _uiState.update { it.copy(isLoading = false, matchComplete = true) }
                            }
                            return@launch
                        }
                        _uiState.update { it.copy(isLoading = false, matchComplete = true) }
                        return@launch
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
                    resumeMatch(matchId, force = true)
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

    // ── Next Innings (incl. super over) ───────────────────────────────────────
    fun checkAndStartNextInnings(matchId: String, onMatchComplete: () -> Unit) {
        if (inningsCompleteHandled) return
        inningsCompleteHandled = true
        viewModelScope.launch {
            try {
                val match = matchRepository.getMatchById(matchId) ?: return@launch
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val completed = allInnings.filter { it.status == "completed" }

                val inn1 = allInnings.find { it.inningsNo == 1 }
                val inn2 = allInnings.find { it.inningsNo == 2 }
                val mainTied = inn1 != null && inn2 != null && inn1.totalRuns == inn2.totalRuns
                android.util.Log.d("CricketHub", "NEXT_INNINGS: completed=${completed.size} " +
                        "inn1=${inn1?.totalRuns} inn2=${inn2?.totalRuns} tied=$mainTied " +
                        "superEnabled=${match.superOverEnabled} allInnings=${allInnings.map { "${it.inningsNo}:${it.status}" }}")

                // Helper: start an innings (create if missing) and reload.
                suspend fun startInnings(no: Int, battingTeamId: String, bowlingTeamId: String, chaseTarget: Int?) {
                    if (allInnings.find { it.inningsNo == no } == null) {
                        scoringRepository.createInnings(InningsInsert(matchId, no, battingTeamId, bowlingTeamId))
                    }
                    target = chaseTarget
                    _uiState.update { it.copy(inningsComplete = false, matchComplete = false) }
                    isMatchLoaded = false; inningsCompleteHandled = false
                    loadMatch(matchId)
                }

                // ---- MAIN MATCH (innings 1 & 2) ----
                if (completed.size == 1) {
                    // start 2nd innings, chasing 1st
                    val first = completed.first()
                    startInnings(2, first.bowlingTeamId, first.battingTeamId, first.totalRuns + 1)
                    return@launch
                }
                if (completed.size == 2) {
                    if (mainTied && match.superOverEnabled) {
                        // Super over 1: team that batted SECOND bats first (innings 3).
                        startInnings(3, inn2!!.battingTeamId, inn2.bowlingTeamId, null)
                    } else {
                        inningsCompleteHandled = false
                        onMatchComplete()  // decided on runs
                    }
                    return@launch
                }

                // ---- SUPER OVERS (innings >= 3, in pairs: odd = bat, even = chase) ----
                if (completed.size >= 3) {
                    val lastNo = completed.maxOf { it.inningsNo }
                    if (lastNo % 2 == 1) {
                        // an ODD super-over innings just completed -> start its chase
                        val batInn = allInnings.find { it.inningsNo == lastNo }!!
                        startInnings(lastNo + 1, batInn.bowlingTeamId, batInn.battingTeamId, batInn.totalRuns + 1)
                        return@launch
                    } else {
                        // an EVEN (chase) innings completed -> compare the pair
                        val bat = allInnings.find { it.inningsNo == lastNo - 1 }
                        val chase = allInnings.find { it.inningsNo == lastNo }
                        val soTied = bat != null && chase != null && bat.totalRuns == chase.totalRuns
                        if (soTied) {
                            // Tie -> ask the user: repeat / declare tie / boundary count.
                            inningsCompleteHandled = false
                            _uiState.update { it.copy(showSuperOverDecision = true, inningsComplete = false) }
                        } else {
                            inningsCompleteHandled = false
                            onMatchComplete()  // higher score wins
                        }
                        return@launch
                    }
                }

                // fallback: nothing completed yet
                isMatchLoaded = false; inningsCompleteHandled = false; loadMatch(matchId)
            } catch (e: Exception) {
                inningsCompleteHandled = false
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // User chose to play another super over after a tie.
    fun startAnotherSuperOver(matchId: String) {
        viewModelScope.launch {
            try {
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val lastNo = allInnings.maxOfOrNull { it.inningsNo } ?: 2
                val prevChase = allInnings.find { it.inningsNo == lastNo }
                // Next super over: same batting order rule - team that batted second in
                // the PREVIOUS super over (the chaser) bats first now.
                val nextNo = lastNo + 1
                if (prevChase == null) {
                    android.util.Log.e("CricketHub", "startAnotherSuperOver: no previous innings found")
                    _uiState.update { it.copy(showSuperOverDecision = false, error = "Could not start super over") }
                    return@launch
                }
                val created = try {
                    scoringRepository.createInnings(
                        InningsInsert(matchId, nextNo, prevChase.battingTeamId, prevChase.bowlingTeamId))
                } catch (dup: Exception) {
                    android.util.Log.w("CricketHub", "super over $nextNo exists, reusing: ${dup.message}")
                    scoringRepository.invalidateInningsCache(matchId)
                    scoringRepository.getInningsByMatch(matchId).firstOrNull { it.inningsNo == nextNo }
                        ?: throw dup
                }
                android.util.Log.d("CricketHub", "startAnotherSuperOver: created innings $nextNo id=${created.id}")
                scoringRepository.invalidateInningsCache(matchId)
                clearSavedState(matchId)
                target = null
                _uiState.update { it.copy(showSuperOverDecision = false, inningsComplete = false,
                    matchComplete = false, innings = null, balls = emptyList(),
                    striker = null, nonStriker = null, currentBowler = null,
                    batsmanStats = emptyMap(), bowlerStats = emptyMap()) }
                isMatchLoaded = false; inningsCompleteHandled = false
                loadMatch(matchId, forceReload = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // User chose to declare it a tie - no further play.
    fun resolveSuperOverAsComplete() {
        _uiState.update { it.copy(showSuperOverDecision = false, matchComplete = true) }
    }

    // Decide the match on total boundaries (4s + 6s) across the WHOLE match.
    // Stores the winner on the match so post-match can report it.
    fun resolveByBoundaryCount(matchId: String) {
        viewModelScope.launch {
            try {
                val allInnings = scoringRepository.getInningsByMatch(matchId)
                val match = matchRepository.getMatchById(matchId)
                var team1Boundaries = 0
                var team2Boundaries = 0
                for (inn in allInnings) {
                    val balls = scoringRepository.getBallsByInnings(inn.id)
                    val b = balls.count { it.isBoundary || it.isSix }
                    if (inn.battingTeamId == match?.team1Id) team1Boundaries += b else team2Boundaries += b
                }
                android.util.Log.d("CricketHub", "BOUNDARY COUNT: t1=$team1Boundaries t2=$team2Boundaries")
                // Fetch team names so the result reads "Team A won on boundary count".
                suspend fun teamName(id: String?): String = try {
                    com.crickethub.data.remote.SupabaseClient.client.postgrest["teams"]
                        .select { filter { eq("id", id ?: "") } }
                        .decodeSingleOrNull<com.crickethub.data.model.Team>()?.name ?: "Team"
                } catch (e: Exception) { "Team" }
                val t1Name = teamName(match?.team1Id)
                val t2Name = teamName(match?.team2Id)
                // How many super overs were played (pairs of innings from 3 upward)
                val soCount = allInnings.count { it.inningsNo >= 3 } / 2
                val soLabel = when {
                    soCount >= 2 -> "after $soCount Super Overs"
                    soCount == 1 -> "after the Super Over"
                    else -> ""
                }
                val resultText = when {
                    team1Boundaries > team2Boundaries ->
                        "Match tied - $t1Name won on boundary count ($team1Boundaries vs $team2Boundaries) $soLabel".trim()
                    team2Boundaries > team1Boundaries ->
                        "Match tied - $t2Name won on boundary count ($team2Boundaries vs $team1Boundaries) $soLabel".trim()
                    else -> "Match tied (boundaries level $team1Boundaries-$team2Boundaries)"
                }
                try {
                    com.crickethub.data.remote.SupabaseClient.client
                        .postgrest["matches"].update({
                        set("result_text", resultText)
                        set("result_type", "boundary_count")
                    }) { filter { eq("id", matchId) } }
                    matchRepository.invalidateMatchCache(matchId)
                } catch (e: Exception) {
                    android.util.Log.w("CricketHub", "boundary result save: ${e.message}")
                }
                _uiState.update { it.copy(showSuperOverDecision = false, matchComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(showSuperOverDecision = false, error = e.message, matchComplete = true) }
            }
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
        // Guard AND claim in one step - set the flag before reading any state so a
        // second rapid tap can't slip through and double-count the ball (this was the
        // cause of total_balls drift: two taps both read the same totalBalls, both +1).
        if (isProcessingBall) return
        isProcessingBall = true
        val currentState = _uiState.value
        if (undoStack.size >= 20) undoStack.removeFirst()
        undoStack.addLast(currentState)
        val state = _uiState.value
        val innings = state.innings ?: run { isProcessingBall = false; return }
        val striker = state.striker ?: run { isProcessingBall = false; return }
        val bowler = state.currentBowler ?: run { isProcessingBall = false; return }
        val match = state.match ?: run { isProcessingBall = false; return }
        viewModelScope.launch {
            try {
                val isWide = extrasType == "wide"; val isNoBall = extrasType == "no_ball"
                val isBye = extrasType == "bye"; val isLegBye = extrasType == "leg_bye"
                val isRetiredHurt = wicketType == "retired_hurt"
                val isLegal = !isWide && !isNoBall
                // Derive position from the ACTUAL legal balls already in state, not the
                // stored total_balls counter. The counter can desync (a failed resume,
                // an interrupted write) and since over/ball are computed from it, a
                // desync would misplace every future ball. The ball list is truth.
                val legalBallsSoFar = state.balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                val overNo = legalBallsSoFar / 6; val ballNo = legalBallsSoFar % 6 + 1
                val phase = when { overNo < match.powerplayOvers -> "powerplay"; overNo < (match.totalOvers * 0.75).toInt() -> "middle"; else -> "death" }
                // Normalise no-ball input: runs off a no-ball belong to the BATSMAN.
                // The dialog may historically pass them as extrasRuns; treat either source
                // as bat-runs so nb+4 credits the striker 4 and a boundary.
                val noBallBatRuns = if (isNoBall) (if (runsOffBat > 0) runsOffBat else (extrasRuns ?: 0)) else runsOffBat
                // Wide: extrasRuns = runs the batsmen RAN (0 for a plain wide). Penalty +1.
                val wideRan = if (isWide) (extrasRuns ?: 0) else 0

                val totalRunsThisBall = when {
                    isWide   -> 1 + wideRan                 // 1 penalty + runs ran
                    isNoBall -> 1 + noBallBatRuns           // 1 penalty + runs off bat
                    else     -> runsOffBat + (extrasRuns ?: 0)
                }
                // extras_total counts only the PENALTY+ran portion, not bat-runs on a no-ball.
                val extrasToSave = when {
                    isWide   -> 1 + wideRan                 // whole wide is extras
                    isNoBall -> 1                            // only the penalty is an extra
                    isBye || isLegBye -> extrasRuns ?: 0
                    else     -> 0
                }
                // What the batsman actually faced/scored on this delivery
                val batRunsForStriker = when { isNoBall -> noBallBatRuns; isWide -> 0; isBye || isLegBye -> 0; else -> runsOffBat }
                // The striker always FACES the ball. On a non-striker run-out the wicket
                // belongs to the non-striker, but the delivery + any runs are the striker's.
                // dismissedBatsmanId names the victim without stealing the striker's ball.
                val runOutNonStriker = wicketType == "run_out" && nonStrikerOut
                val dismissedBatsmanId = if (runOutNonStriker) state.nonStriker?.id else null
                val insertedBall = scoringRepository.insertBall(BallInsert(
                    inningsId = innings.id, overNo = overNo, ballNo = if (isLegal) ballNo else 0, deliveryNo = null,
                    batsmanId = striker.id,
                    nonStrikerId = state.nonStriker?.id, bowlerId = bowler.id,
                    runsOffBat = batRunsForStriker, extrasRuns = if (extrasToSave > 0) extrasToSave else null,
                    extrasType = extrasType, isWicket = isWicket, wicketType = wicketType, fielderName = fielderName,
                    dismissedBatsmanId = dismissedBatsmanId,
                    isBoundary = batRunsForStriker == 4, isSix = batRunsForStriker == 6, inningsPhase = phase,
                    commentary = generateCommentary(runsOffBat, extrasType, extrasRuns, isWicket, wicketType,
                        if (runOutNonStriker) (state.nonStriker?.fullName ?: striker.fullName) else striker.fullName,
                        bowler.fullName, fielderName)))
                // Stored counter is always exactly (legal balls so far + this one if legal).
                // Never "previous stored + 1", so a desynced counter self-heals on the next ball.
                val newTotalBalls = if (isLegal) legalBallsSoFar + 1 else legalBallsSoFar
                val newTotalRuns = innings.totalRuns + totalRunsThisBall
                val newTotalWickets = if (isWicket && !isRetiredHurt) innings.totalWickets + 1 else innings.totalWickets
                val updatedInnings = scoringRepository.updateInnings(innings.id, newTotalRuns, newTotalWickets, newTotalBalls,
                    innings.extrasTotal + extrasToSave, if (isWide) innings.wides + 1 else innings.wides, if (isNoBall) innings.noBalls + 1 else innings.noBalls)
                val newBalls = state.balls + insertedBall
                ballStack.addLast(insertedBall)
                var newStriker: Player? = striker; var newNonStriker: Player? = state.nonStriker
                if (!isWicket) {
                    // Strike flips on ODD runs: wide -> runs ran; no-ball -> bat runs;
                    // bye/leg-bye -> runs ran; normal -> runs off bat.
                    val changeStrike = when {
                        isWide   -> wideRan % 2 == 1
                        isNoBall -> noBallBatRuns % 2 == 1
                        isBye || isLegBye -> (extrasRuns ?: 0) % 2 == 1
                        else     -> runsOffBat % 2 == 1
                    }
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
                val isSuperOver = innings.inningsNo >= 3
                val maxOvers = if (isSuperOver) 1 else match.totalOvers
                // Super over: 2 wickets ends the innings. Main match: all out = players-1.
                val maxWickets = if (isSuperOver) 2 else match.playersPerSide - 1
                val targetChased = target != null && newTotalRuns >= target!!
                val isInningsComplete = newTotalWickets >= maxWickets || newTotalBalls >= maxOvers * 6 || targetChased
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
            // Dot ball: a legal delivery that conceded nothing (no bat runs, no extras).
            val dots = pb.count {
                it.extrasType != "wide" && it.extrasType != "no_ball" &&
                        it.runsOffBat == 0 && (it.extrasRuns ?: 0) == 0
            }
            // Maiden: a COMPLETED over (6 legal balls) by this bowler conceding 0 runs.
            // Wides/no-balls in the over mean runs were conceded, so it can't be a maiden.
            val maidens = pb.groupBy { it.overNo }.count { (_, overBalls) ->
                val legalInOver = overBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                val conceded = overBalls.sumOf {
                    if (it.extrasType in listOf("bye", "leg_bye")) 0
                    else it.runsOffBat + (it.extrasRuns ?: 0)
                }
                legalInOver == 6 && conceded == 0
            }
            map[player.id] = BowlerStats(player = player, balls = legal,
                runs = pb.sumOf { if (it.extrasType in listOf("bye", "leg_bye")) 0 else it.runsOffBat + (it.extrasRuns ?: 0) },
                wickets = pb.count { it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "handled_ball", "timed_out", "retired_hurt", "retired_out") },
                overs = "${legal / 6}.${legal % 6}",
                maidens = maidens, dotBalls = dots,
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