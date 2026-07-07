package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String = "",
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("jersey_color") val jerseyColor: String = "#10B981",
    val category: String = "club",
    val country: String? = null,
    val city: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    val coach: String? = null,
    @SerialName("captain_id") val captainId: String? = null,
    @SerialName("vice_captain_id") val viceCaptainId: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TeamInsert(
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("jersey_color") val jerseyColor: String = "#10B981",
    val category: String = "club",
    val country: String? = null,
    val city: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    val coach: String? = null,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class TeamUpdate(
    val name: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("jersey_color") val jerseyColor: String? = null,
    val category: String? = null,
    val country: String? = null,
    val city: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    val coach: String? = null,
    @SerialName("captain_id") val captainId: String? = null,
    @SerialName("vice_captain_id") val viceCaptainId: String? = null
)