package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val email: String = "",
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val role: String = "viewer",
    val city: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProfileUpdate(
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val city: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null
)