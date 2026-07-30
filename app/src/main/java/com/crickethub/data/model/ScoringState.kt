package com.crickethub.data.model

data class BatsmanStats(
    val player: Player,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissalType: String? = null,
    val fielderName: String? = null,
    val bowlerOnWicket: String? = null
) {
    val strikeRate: Double get() = if (balls > 0) (runs.toDouble() / balls) * 100 else 0.0
}

data class BowlerStats(
    val player: Player,
    val balls: Int = 0,
    val runs: Int = 0,
    val wickets: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0,
    val maidens: Int = 0,
    val dotBalls: Int = 0,
    val overs: String = "0.0"
) {
    val economy: Double get() = if (balls > 0) (runs.toDouble() / balls) * 6 else 0.0
}

/** Current unbroken stand: runs added and legal balls faced since the last wicket. */
data class PartnershipInfo(val runs: Int, val balls: Int)

/** The most recent wicket: score when it fell + who was out and how. */
data class LastWicketInfo(
    val score: Int,
    val wicket: Int,
    val batsmanName: String,
    val dismissal: String
)

/** Runs a single delivery added to the team total (bat + its own extras + penalty). */
private fun Ball.teamRuns(): Int = when {
    extrasType == "wide" -> (extrasRuns ?: 1) + runsOffBat
    extrasType == "no_ball" -> 1 + runsOffBat + (extrasRuns ?: 0)
    else -> runsOffBat + (extrasRuns ?: 0)
}

private fun Ball.isLegal(): Boolean = extrasType != "wide" && extrasType != "no_ball"

/** Dismissals with no ball bowled — not shown as a delivery chip. */
private val NO_DELIVERY_WICKET_TYPES = setOf("timed_out", "retired_out", "retired_hurt")

data class ScoringUiState(
    val isLoading: Boolean = false,
    val match: Match? = null,
    val innings: Innings? = null,
    val balls: List<Ball> = emptyList(),
    val striker: Player? = null,
    val nonStriker: Player? = null,
    val currentBowler: Player? = null,
    val battingTeamPlayers: List<Player> = emptyList(),
    val bowlingTeamPlayers: List<Player> = emptyList(),
    val batsmanStats: Map<String, BatsmanStats> = emptyMap(),
    val bowlerStats: Map<String, BowlerStats> = emptyMap(),
    val inningsComplete: Boolean = false,
    val matchComplete: Boolean = false,
    // Super over: true when a super over just tied and the user must choose what next
    val showSuperOverDecision: Boolean = false,
    val error: String? = null,
    // Match-info header (populated on load; empty until then)
    val battingTeamName: String = "",
    val bowlingTeamName: String = "",
    val tournamentName: String? = null,
    // Chase target (1st-innings total + 1) for the 2nd innings / super-over chase; null in the 1st innings
    val target: Int? = null,
    val dlsEnabled: Boolean = false,
    val dlsParScore: Int? = null,
    val dlsTarget: Int? = null,
    val dlsTeam1Resource: Double = 100.0,
    val dlsOversRemaining: Double = 0.0,
    val dlsWicketsLost: Int = 0,
    val showDLSBanner: Boolean = false
) {
    val totalRuns: Int get() = innings?.totalRuns ?: 0
    val totalWickets: Int get() = innings?.totalWickets ?: 0
    val currentOver: Int get() = (innings?.totalBalls ?: 0) / 6
    val currentBall: Int get() = (innings?.totalBalls ?: 0) % 6
    val runRate: Double get() {
        val b = innings?.totalBalls ?: 0
        val r = innings?.totalRuns ?: 0
        return if (b > 0) (r.toDouble() / b) * 6 else 0.0
    }

    val isSuperOver: Boolean get() = (innings?.inningsNo ?: 1) >= 3
    val isSecondInnings: Boolean get() = (innings?.inningsNo ?: 1) == 2

    /** Overs limit for the current innings (a super over is always 1 over). */
    val oversLimit: Int get() = if (isSuperOver) 1 else (match?.totalOvers ?: 20)

    val ballsRemaining: Int get() =
        (oversLimit * 6 - (innings?.totalBalls ?: 0)).coerceAtLeast(0)

    /** Runs still needed to win, when chasing. Null in the 1st innings. */
    val runsNeeded: Int? get() = target?.let { (it - totalRuns).coerceAtLeast(0) }

    /** Required run rate for the chase. Null in the 1st innings or when no balls remain. */
    val requiredRunRate: Double? get() {
        val need = runsNeeded ?: return null
        val br = ballsRemaining
        return if (br > 0) need.toDouble() * 6 / br else null
    }

    /**
     * Projected total if the current run rate holds — 1st innings only (a chase has a
     * fixed target, so a projection is meaningless there).
     */
    val projectedScore: Int? get() {
        if (isSuperOver || isSecondInnings) return null
        val b = innings?.totalBalls ?: 0
        if (b == 0) return null
        return (runRate * oversLimit).toInt().coerceAtLeast(totalRuns)
    }

    /** Current partnership (since the last wicket, or since the start of the innings). */
    val currentPartnership: PartnershipInfo get() {
        val afterWicket = balls.indexOfLast { it.isWicket && it.wicketType != "retired_hurt" } + 1
        val since = balls.drop(afterWicket)
        val runs = since.sumOf { it.teamRuns() }
        val faced = since.count { it.isLegal() }
        return PartnershipInfo(runs, faced)
    }

    /** The most recent wicket in this innings, or null if none have fallen. */
    val lastWicket: LastWicketInfo? get() {
        val idx = balls.indexOfLast { it.isWicket && it.wicketType != "retired_hurt" }
        if (idx < 0) return null
        val upTo = balls.subList(0, idx + 1)
        val runsAt = upTo.sumOf { it.teamRuns() }
        val wicketsAt = upTo.count { it.isWicket && it.wicketType != "retired_hurt" }
        val ball = balls[idx]
        val victimId = ball.dismissedBatsmanId ?: ball.batsmanId
        val name = battingTeamPlayers.firstOrNull { it.id == victimId }?.fullName ?: "Batsman"
        val dismissal = ball.wicketType?.replace("_", " ") ?: "out"
        return LastWicketInfo(runsAt, wicketsAt, name, dismissal)
    }

    val last6Balls: List<String> get() {
        val legalBalls = balls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" && it.wicketType !in NO_DELIVERY_WICKET_TYPES }
        val currentOverNo = if (legalBalls.isEmpty()) 0 else legalBalls.last().overNo
        val currentOverBalls = balls.filter { it.overNo == currentOverNo && it.wicketType !in NO_DELIVERY_WICKET_TYPES }
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
}