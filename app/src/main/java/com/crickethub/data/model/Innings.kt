package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Innings(
    val id: String = "",
    @SerialName("match_id") val matchId: String,
    @SerialName("innings_no") val inningsNo: Int,
    @SerialName("batting_team_id") val battingTeamId: String,
    @SerialName("bowling_team_id") val bowlingTeamId: String,
    @SerialName("total_runs") val totalRuns: Int = 0,
    @SerialName("total_wickets") val totalWickets: Int = 0,
    @SerialName("total_balls") val totalBalls: Int = 0,
    @SerialName("extras_total") val extrasTotal: Int = 0,
    val wides: Int = 0,
    @SerialName("no_balls") val noBalls: Int = 0,
    val byes: Int = 0,
    @SerialName("leg_byes") val legByes: Int = 0,
    val status: String = "pending"
)

@Serializable
data class InningsInsert(
    @SerialName("match_id") val matchId: String,
    @SerialName("innings_no") val inningsNo: Int,
    @SerialName("batting_team_id") val battingTeamId: String,
    @SerialName("bowling_team_id") val bowlingTeamId: String,
    val status: String = "live"
)