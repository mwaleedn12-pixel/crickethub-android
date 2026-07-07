package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String = "",
    @SerialName("team_id") val teamId: String,
    @SerialName("full_name") val fullName: String,
    val nickname: String? = null,
    @SerialName("jersey_no") val jerseyNo: Int? = null,
    val role: String? = null,
    @SerialName("batting_style") val battingStyle: String? = null,
    @SerialName("bowling_style") val bowlingStyle: String? = null,
    @SerialName("batting_hand") val battingHand: String = "right",
    @SerialName("bowling_hand") val bowlingHand: String = "right",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val city: String? = null,
    @SerialName("height_cm") val heightCm: Int? = null,
    val availability: String = "available",
    @SerialName("injury_status") val injuryStatus: String? = null,
    @SerialName("debut_date") val debutDate: String? = null,
    @SerialName("previous_teams") val previousTeams: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PlayerInsert(
    @SerialName("team_id") val teamId: String,
    @SerialName("full_name") val fullName: String,
    val nickname: String? = null,
    @SerialName("jersey_no") val jerseyNo: Int? = null,
    val role: String? = null,
    @SerialName("batting_style") val battingStyle: String? = null,
    @SerialName("bowling_style") val bowlingStyle: String? = null,
    @SerialName("batting_hand") val battingHand: String = "right",
    @SerialName("bowling_hand") val bowlingHand: String = "right",
    val gender: String? = null,
    val city: String? = null,
    @SerialName("height_cm") val heightCm: Int? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val availability: String = "available"
)