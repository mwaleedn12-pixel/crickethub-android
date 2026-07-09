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
    val overs: String = "0.0"
) {
    val economy: Double get() = if (balls > 0) (runs.toDouble() / balls) * 6 else 0.0
}

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
    val error: String? = null
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
    val last6Balls: List<String> get() {
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
}