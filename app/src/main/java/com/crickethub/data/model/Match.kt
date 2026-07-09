package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String = "",
    val title: String? = null,
    @SerialName("match_type") val matchType: String = "T20",
    @SerialName("tournament_id") val tournamentId: String? = null,
    @SerialName("match_number") val matchNumber: Int? = null,
    @SerialName("team1_id") val team1Id: String,
    @SerialName("team2_id") val team2Id: String,
    val venue: String? = null,
    @SerialName("match_date") val matchDate: String? = null,
    @SerialName("match_time") val matchTime: String? = null,
    @SerialName("total_overs") val totalOvers: Int = 20,
    @SerialName("players_per_side") val playersPerSide: Int = 11,
    @SerialName("powerplay_overs") val powerplayOvers: Int = 6,
    @SerialName("powerplay2_overs") val powerplay2Overs: Int = 0,
    @SerialName("powerplay3_overs") val powerplay3Overs: Int = 0,
    @SerialName("innings_break_minutes") val inningsBreakMinutes: Int = 20,
    @SerialName("super_over_enabled") val superOverEnabled: Boolean = false,
    @SerialName("follow_on_enabled") val followOnEnabled: Boolean = false,
    @SerialName("free_hit_on_noball") val freeHitOnNoball: Boolean = true,
    @SerialName("toss_winner_id") val tossWinnerId: String? = null,
    @SerialName("toss_decision") val tossDecision: String? = null,
    @SerialName("batting_first_id") val battingFirstId: String? = null,
    val status: String = "draft",
    @SerialName("result_type") val resultType: String = "normal",
    @SerialName("result_text") val resultText: String? = null,
    @SerialName("umpire1") val umpire1: String? = null,
    @SerialName("umpire2") val umpire2: String? = null,
    @SerialName("third_umpire") val thirdUmpire: String? = null,
    @SerialName("match_referee") val matchReferee: String? = null,
    @SerialName("scorer_name") val scorerName: String? = null,
    @SerialName("live_sharing_enabled") val liveSharingEnabled: Boolean = true,
    @SerialName("shareable_slug") val shareableSlug: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MatchInsert(
    val title: String? = null,
    @SerialName("match_type") val matchType: String = "T20",
    @SerialName("tournament_id") val tournamentId: String? = null,
    @SerialName("match_number") val matchNumber: Int? = null,
    @SerialName("team1_id") val team1Id: String,
    @SerialName("team2_id") val team2Id: String,
    val venue: String? = null,
    @SerialName("match_date") val matchDate: String? = null,
    @SerialName("match_time") val matchTime: String? = null,
    @SerialName("total_overs") val totalOvers: Int = 20,
    @SerialName("players_per_side") val playersPerSide: Int = 11,
    @SerialName("powerplay_overs") val powerplayOvers: Int = 6,
    @SerialName("super_over_enabled") val superOverEnabled: Boolean = false,
    @SerialName("follow_on_enabled") val followOnEnabled: Boolean = false,
    @SerialName("free_hit_on_noball") val freeHitOnNoball: Boolean = true,
    @SerialName("umpire1") val umpire1: String? = null,
    @SerialName("umpire2") val umpire2: String? = null,
    @SerialName("third_umpire") val thirdUmpire: String? = null,
    @SerialName("match_referee") val matchReferee: String? = null,
    @SerialName("scorer_name") val scorerName: String? = null,
    @SerialName("live_sharing_enabled") val liveSharingEnabled: Boolean = true,
    @SerialName("created_by") val createdBy: String,
    val status: String = "draft"
)

@Serializable
data class PlayingXIInsert(
    @SerialName("match_id") val matchId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("batting_order") val battingOrder: Int? = null
)