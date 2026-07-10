package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayingXI(
    val id: String = "",
    @SerialName("match_id") val matchId: String = "",
    @SerialName("player_id") val playerId: String = "",
    @SerialName("team_id") val teamId: String = "",
    @SerialName("batting_order") val battingOrder: Int? = null,
    @SerialName("is_captain") val isCaptain: Boolean = false,
    @SerialName("is_vice_captain") val isViceCaptain: Boolean = false,
    @SerialName("is_wicket_keeper") val isWicketKeeper: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PlayingXIInsert(
    @SerialName("match_id") val matchId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("batting_order") val battingOrder: Int? = null,
    @SerialName("is_captain") val isCaptain: Boolean = false,
    @SerialName("is_wicket_keeper") val isWicketKeeper: Boolean = false
)