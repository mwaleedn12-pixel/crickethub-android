package com.crickethub.ui.match.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Innings
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

data class OverData(
    val overNo: Int,
    val runs: Int,
    val wickets: Int,
    val cumulativeRuns: Int
)

data class PartnershipData(
    val batsman1: String,
    val batsman2: String,
    val runs: Int,
    val balls: Int,
    val wicketNo: Int
)

data class PhaseData(
    val phase: String,
    val runs: Int,
    val wickets: Int,
    val balls: Int,
    val boundaries: Int
)

data class WinProbabilityData(
    val overNo: Int,
    val battingTeamProbability: Float,
    val bowlingTeamProbability: Float
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val innings1Overs: List<OverData> = emptyList(),
    val innings2Overs: List<OverData> = emptyList(),
    val dismissalTypes: Map<String, Int> = emptyMap(),
    val extrasData: Map<String, Int> = emptyMap(),
    val boundaryTimeline: List<Pair<Int, String>> = emptyList(),
    val innings1Phases: List<PhaseData> = emptyList(),
    val innings2Phases: List<PhaseData> = emptyList(),
    val partnerships: List<PartnershipData> = emptyList(),
    val winProbability: List<WinProbabilityData> = emptyList(),
    val battingTeamName: String = "Batting Team",
    val bowlingTeamName: String = "Bowling Team",
    val target: Int? = null,
    val innings1Balls: List<Ball> = emptyList(),
    val innings2Balls: List<Ball> = emptyList(),
    val innings1: Innings? = null,
    val innings2: Innings? = null,
    val totalOvers: Int = 20
)

class AnalyticsViewModel : ViewModel() {

    private val scoringRepository = ScoringRepository()
    private val matchRepository = MatchRepository()

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    fun loadAnalytics(matchId: String) {
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

                val target = if (innings1 != null) innings1.totalRuns + 1 else null

                val battingTeam = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", innings1?.battingTeamId ?: match.team1Id) } }
                    .decodeSingleOrNull<Team>()

                val bowlingTeam = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", innings1?.bowlingTeamId ?: match.team2Id) } }
                    .decodeSingleOrNull<Team>()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        innings1 = innings1,
                        innings2 = innings2,
                        innings1Balls = balls1,
                        innings2Balls = balls2,
                        innings1Overs = computeOverData(balls1),
                        innings2Overs = computeOverData(balls2),
                        dismissalTypes = computeDismissalTypes(balls1 + balls2),
                        extrasData = computeExtras(balls1 + balls2),
                        boundaryTimeline = computeBoundaryTimeline(balls1),
                        innings1Phases = computePhaseData(balls1),
                        innings2Phases = computePhaseData(balls2),
                        partnerships = computePartnerships(balls1),
                        winProbability = computeWinProbability(
                            balls2, target, match.totalOvers, match.playersPerSide
                        ),
                        battingTeamName = battingTeam?.name ?: "Team 1",
                        bowlingTeamName = bowlingTeam?.name ?: "Team 2",
                        target = target,
                        totalOvers = match.totalOvers
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Analytics error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun computeOverData(balls: List<Ball>): List<OverData> {
        val overMap = balls.groupBy { it.overNo }
        var cumulativeRuns = 0
        return overMap.entries.sortedBy { it.key }.map { (overNo, overBalls) ->
            val runs = overBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) }
            val wickets = overBalls.count { it.isWicket }
            cumulativeRuns += runs
            OverData(overNo, runs, wickets, cumulativeRuns)
        }
    }

    private fun computeDismissalTypes(balls: List<Ball>): Map<String, Int> {
        return balls
            .filter { it.isWicket && it.wicketType != null }
            .groupBy { it.wicketType!! }
            .mapValues { it.value.size }
    }

    private fun computeExtras(balls: List<Ball>): Map<String, Int> {
        return mapOf(
            "Wide" to balls.count { it.extrasType == "wide" },
            "No Ball" to balls.count { it.extrasType == "no_ball" },
            "Bye" to balls.count { it.extrasType == "bye" },
            "Leg Bye" to balls.count { it.extrasType == "leg_bye" }
        ).filter { it.value > 0 }
    }

    private fun computeBoundaryTimeline(balls: List<Ball>): List<Pair<Int, String>> {
        return balls
            .filter { it.isBoundary || it.isSix }
            .mapIndexed { index, ball -> Pair(index, if (ball.isSix) "6" else "4") }
    }

    private fun computePhaseData(balls: List<Ball>): List<PhaseData> {
        return listOf("powerplay", "middle", "death").map { phase ->
            val phaseBalls = balls.filter { it.inningsPhase == phase }
            PhaseData(
                phase = phase.replaceFirstChar { it.uppercase() },
                runs = phaseBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) },
                wickets = phaseBalls.count { it.isWicket },
                balls = phaseBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" },
                boundaries = phaseBalls.count { it.isBoundary || it.isSix }
            )
        }.filter { it.balls > 0 }
    }

    private fun computePartnerships(balls: List<Ball>): List<PartnershipData> {
        if (balls.isEmpty()) return emptyList()
        val partnerships = mutableListOf<PartnershipData>()
        var partnershipBalls = mutableListOf<Ball>()
        var wicketNo = 1

        balls.forEach { ball ->
            partnershipBalls.add(ball)
            if (ball.isWicket) {
                val runs = partnershipBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) }
                partnerships.add(
                    PartnershipData(
                        batsman1 = ball.batsmanId.take(8),
                        batsman2 = ball.nonStrikerId?.take(8) ?: "",
                        runs = runs,
                        balls = partnershipBalls.size,
                        wicketNo = wicketNo++
                    )
                )
                partnershipBalls = mutableListOf()
            }
        }

        if (partnershipBalls.isNotEmpty()) {
            val runs = partnershipBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) }
            partnerships.add(
                PartnershipData("", "", runs, partnershipBalls.size, wicketNo)
            )
        }

        return partnerships
    }

    private fun computeWinProbability(
        balls2: List<Ball>,
        target: Int?,
        totalOvers: Int,
        playersPerSide: Int
    ): List<WinProbabilityData> {
        if (target == null || balls2.isEmpty()) return emptyList()

        val result = mutableListOf<WinProbabilityData>()
        val totalBalls = totalOvers * 6
        val maxWickets = playersPerSide - 1
        var cumulativeRuns = 0
        var cumulativeWickets = 0
        var cumulativeBalls = 0

        balls2.groupBy { it.overNo }.entries.sortedBy { it.key }.forEach { (overNo, overBalls) ->
            val legalBalls = overBalls.count {
                it.extrasType != "wide" && it.extrasType != "no_ball"
            }
            cumulativeRuns += overBalls.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) }
            cumulativeWickets += overBalls.count { it.isWicket }
            cumulativeBalls += legalBalls

            val ballsLeft = totalBalls - cumulativeBalls
            val wicketsLeft = maxWickets - cumulativeWickets
            val runsNeeded = target - cumulativeRuns

            val battingWinProb = calculateBattingWinProbability(
                runsNeeded, ballsLeft, wicketsLeft, maxWickets
            )

            result.add(
                WinProbabilityData(
                    overNo = overNo + 1,
                    battingTeamProbability = battingWinProb,
                    bowlingTeamProbability = 1f - battingWinProb
                )
            )
        }
        return result
    }

    private fun calculateBattingWinProbability(
        runsNeeded: Int,
        ballsLeft: Int,
        wicketsLeft: Int,
        maxWickets: Int
    ): Float {
        if (runsNeeded <= 0) return 1f
        if (ballsLeft <= 0 || wicketsLeft <= 0) return 0f

        val rrr = (runsNeeded.toFloat() / ballsLeft) * 6f
        val wicketFactor = wicketsLeft.toFloat() / maxWickets.toFloat()

        val prob = when {
            rrr < 4f  -> 0.85f + (wicketFactor * 0.1f)
            rrr < 6f  -> 0.70f + (wicketFactor * 0.15f)
            rrr < 8f  -> 0.50f + (wicketFactor * 0.2f) - ((rrr - 6f) * 0.05f)
            rrr < 10f -> 0.30f + (wicketFactor * 0.15f) - ((rrr - 8f) * 0.05f)
            rrr < 13f -> 0.15f + (wicketFactor * 0.1f)
            rrr < 16f -> 0.08f + (wicketFactor * 0.05f)
            else      -> 0.03f + (wicketFactor * 0.02f)
        }

        return prob.coerceIn(0.02f, 0.98f)
    }
}