package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String = "",
    @SerialName("tournament_id") val tournamentId: String? = null,
    @SerialName("team1_id") val team1Id: String,
    @SerialName("team2_id") val team2Id: String,
    val venue: String? = null,
    @SerialName("match_date") val matchDate: String? = null,
    @SerialName("total_overs") val totalOvers: Int = 20,
    @SerialName("toss_winner_id") val tossWinnerId: String? = null,
    @SerialName("toss_decision") val tossDecision: String? = null,
    @SerialName("batting_first_id") val battingFirstId: String? = null,
    val status: String = "scheduled",
    @SerialName("result_text") val resultText: String? = null,
    @SerialName("shareable_slug") val shareableSlug: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MatchInsert(
    @SerialName("team1_id") val team1Id: String,
    @SerialName("team2_id") val team2Id: String,
    val venue: String? = null,
    @SerialName("total_overs") val totalOvers: Int = 20,
    @SerialName("created_by") val createdBy: String,
    val status: String = "scheduled"
)

@Serializable
data class PlayingXIInsert(
    @SerialName("match_id") val matchId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("batting_order") val battingOrder: Int? = null
)