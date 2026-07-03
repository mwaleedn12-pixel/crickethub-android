package com.crickethub.data.model

data class BatsmanStats(
    val player: Player,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissalType: String? = null
) {
    val strikeRate: Double
        get() = if (balls == 0) 0.0 else (runs.toDouble() / balls) * 100
}

data class BowlerStats(
    val player: Player,
    val balls: Int = 0,
    val runs: Int = 0,
    val wickets: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0
) {
    val overs: String
        get() = "${balls / 6}.${balls % 6}"
    val economy: Double
        get() = if (balls == 0) 0.0 else (runs.toDouble() / balls) * 6
}

data class ScoringUiState(
    val match: Match? = null,
    val innings: Innings? = null,
    val balls: List<Ball> = emptyList(),
    val striker: Player? = null,
    val nonStriker: Player? = null,
    val currentBowler: Player? = null,
    val playingXI: List<Player> = emptyList(),
    val battingTeamPlayers: List<Player> = emptyList(),
    val bowlingTeamPlayers: List<Player> = emptyList(),
    val batsmanStats: Map<String, BatsmanStats> = emptyMap(),
    val bowlerStats: Map<String, BowlerStats> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inningsComplete: Boolean = false,
    val matchComplete: Boolean = false
) {
    val currentOver: Int get() = innings?.totalBalls?.div(6) ?: 0
    val currentBall: Int get() = innings?.totalBalls?.rem(6) ?: 0
    val totalRuns: Int get() = innings?.totalRuns ?: 0
    val totalWickets: Int get() = innings?.totalWickets ?: 0

    val runRate: Double
        get() {
            val balls = innings?.totalBalls ?: 0
            return if (balls == 0) 0.0
            else (totalRuns.toDouble() / balls) * 6
        }

    val last6Balls: List<String>
        get() = balls.takeLast(6).map { ball ->
            when {
                ball.isWicket -> "W"
                ball.extrasType == "wide" -> "Wd"
                ball.extrasType == "no_ball" -> "Nb"
                ball.isSix -> "6"
                ball.isBoundary -> "4"
                else -> (ball.runsOffBat + (ball.extrasRuns ?: 0)).toString()
            }
        }
}