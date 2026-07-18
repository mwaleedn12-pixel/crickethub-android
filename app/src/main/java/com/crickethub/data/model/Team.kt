package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String = "",
    val name: String = "",
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("jersey_color") val jerseyColor: String? = "#10B981",
    val category: String? = "Club",
    val country: String? = null,
    val city: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    val coach: String? = null,
    @SerialName("captain_id") val captainId: String? = null,
    @SerialName("vice_captain_id") val viceCaptainId: String? = null,
    @SerialName("wicket_keeper_id") val wicketKeeperId: String? = null,
    @SerialName("join_code") val joinCode: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TeamInsert(
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("jersey_color") val jerseyColor: String? = "#10B981",
    val category: String? = "Club",
    val country: String? = null,
    val city: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    val coach: String? = null,
    @SerialName("captain_id") val captainId: String? = null,
    @SerialName("vice_captain_id") val viceCaptainId: String? = null,
    @SerialName("join_code") val joinCode: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true
)

data class TeamStats(
    val matchesPlayed: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val tied: Int = 0,
    val noResult: Int = 0,
    val winPercentage: Double = 0.0,
    val totalRuns: Int = 0,
    val totalWickets: Int = 0,
    val highestScore: Int = 0,
    val lowestScore: Int = 0,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0
)