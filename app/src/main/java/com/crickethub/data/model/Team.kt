package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TeamInsert(
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("home_ground") val homeGround: String? = null,
    @SerialName("created_by") val createdBy: String
)