package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String = "viewer",
    @SerialName("created_at") val createdAt: String? = null
)