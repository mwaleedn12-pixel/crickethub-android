package com.crickethub.data.repository

import com.crickethub.data.model.SharedLink
import com.crickethub.data.model.SharedLinkInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class SharingRepository {

    private val client = SupabaseClient.client

    // Generate random 8-char code
    private fun generateCode(prefix: String): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = (1..6).map { chars.random() }.joinToString("")
        return "$prefix-$random"
    }

    // Create share link
    suspend fun createShareLink(
        resourceType: String,
        resourceId: String,
        canEdit: Boolean
    ): SharedLink {
        val userId = client.auth.currentUserOrNull()?.id
        val prefix = when (resourceType) {
            "match" -> "MCH"
            "team" -> "TEM"
            "tournament" -> "TRN"
            "player" -> "PLR"
            else -> "LNK"
        }
        val code = generateCode(prefix)
        return client.postgrest["shared_links"]
            .insert(SharedLinkInsert(
                code = code,
                resourceType = resourceType,
                resourceId = resourceId,
                ownerId = userId,
                canEdit = canEdit
            )) { select() }
            .decodeSingle<SharedLink>()
    }

    // Get existing share links for a resource
    suspend fun getShareLinks(resourceType: String, resourceId: String): List<SharedLink> {
        return client.postgrest["shared_links"]
            .select { filter {
                eq("resource_type", resourceType)
                eq("resource_id", resourceId)
            }}
            .decodeList<SharedLink>()
    }

    // Join by code
    suspend fun getByCode(code: String): SharedLink? {
        return try {
            client.postgrest["shared_links"]
                .select { filter { eq("code", code.uppercase().trim()) } }
                .decodeSingleOrNull<SharedLink>()
        } catch (e: Exception) { null }
    }

    // Delete share link
    suspend fun deleteShareLink(linkId: String) {
        client.postgrest["shared_links"]
            .delete { filter { eq("id", linkId) } }
    }

    // Toggle edit permission
    suspend fun updateEditPermission(linkId: String, canEdit: Boolean) {
        client.postgrest["shared_links"]
            .update({ set("can_edit", canEdit) }) {
                filter { eq("id", linkId) }
            }
    }
}
