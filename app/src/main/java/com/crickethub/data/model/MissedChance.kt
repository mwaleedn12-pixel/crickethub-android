package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A fielding miss (dropped catch / missed run-out / missed stumping).
 * type: "catch_dropped" | "run_out_missed" | "stumping_missed"
 */
@Serializable
data class MissedChanceInsert(
    @SerialName("match_id") val matchId: String? = null,
    @SerialName("innings_id") val inningsId: String,
    @SerialName("ball_id") val ballId: String? = null,
    @SerialName("player_id") val playerId: String,
    @SerialName("player_name") val playerName: String,
    val type: String,
    @SerialName("over_no") val overNo: Int
)