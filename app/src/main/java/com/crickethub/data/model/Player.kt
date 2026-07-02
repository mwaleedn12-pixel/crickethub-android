package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("jersey_no") val jerseyNo: Int? = null,
    val role: String? = null,
    @SerialName("batting_style") val battingStyle: String? = null,
    @SerialName("bowling_style") val bowlingStyle: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PlayerInsert(
    @SerialName("team_id") val teamId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("jersey_no") val jerseyNo: Int? = null,
    val role: String? = null,
    @SerialName("batting_style") val battingStyle: String? = null,
    @SerialName("bowling_style") val bowlingStyle: String? = null
)