package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedLink(
    val id: String = "",
    val code: String = "",
    @SerialName("resource_type") val resourceType: String = "",
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class SharedLinkInsert(
    val code: String,
    @SerialName("resource_type") val resourceType: String,
    @SerialName("resource_id") val resourceId: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("can_edit") val canEdit: Boolean = false
)
