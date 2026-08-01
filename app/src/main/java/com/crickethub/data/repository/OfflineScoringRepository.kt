package com.crickethub.data.repository

import android.util.Log
import com.crickethub.data.local.*
import com.crickethub.data.model.Ball
import com.crickethub.data.model.BallInsert
import com.crickethub.data.model.Innings
import com.crickethub.data.model.InningsInsert
import com.crickethub.data.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Offline-first wrapper around [ScoringRepository].
 *
 * Write path:  save to Room → return IMMEDIATELY → sync to Supabase in background.
 * Read path:   try Supabase (cache to Room) → fall back to Room on failure.
 */
class OfflineScoringRepository(
    private val db: AppDatabase,
    private val remote: ScoringRepository = ScoringRepository()
) {
    private val ballDao get() = db.ballDao()
    private val inningsDao get() = db.inningsDao()
    private val pendingSyncDao get() = db.pendingSyncDao()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Background scope for fire-and-forget Supabase syncs. */
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ──────────────────────────── BALLS ────────────────────────────

    /**
     * Insert a ball: save to Room and return IMMEDIATELY.
     * Supabase sync happens in background — no waiting.
     */
    suspend fun insertBall(ballInsert: BallInsert): Ball {
        // 1. Save to Room with a local UUID — this is instant
        val entity = ballInsert.toEntity()
        ballDao.insert(entity)

        // 2. Return immediately so the UI updates fast
        val localBall = entity.toBall()

        // 3. Fire-and-forget: sync to Supabase in background
        syncScope.launch {
            try {
                val remoteBall = remote.insertBall(ballInsert)
                ballDao.replaceIdAndMarkSynced(oldId = entity.id, newId = remoteBall.id)
                Log.d("OfflineRepo", "Ball synced: ${remoteBall.id}")
            } catch (e: Exception) {
                Log.w("OfflineRepo", "Ball sync failed, queuing: ${e.message}")
                pendingSyncDao.insert(
                    PendingSyncEntity(
                        id = UUID.randomUUID().toString(),
                        tableName = "balls",
                        recordId = entity.id,
                        operation = "INSERT",
                        payload = json.encodeToString(ballInsert)
                    )
                )
            }
        }

        return localBall
    }

    /**
     * Delete the last ball (undo). Removes locally first, syncs in background.
     */
    suspend fun deleteLastBall(ballId: String) {
        val localBall = ballDao.getById(ballId)
        val wasSynced = localBall?.syncStatus == SyncStatus.SYNCED

        // Delete locally — instant
        ballDao.deleteById(ballId)

        // Background sync
        syncScope.launch {
            if (wasSynced) {
                try {
                    remote.deleteLastBall(ballId)
                } catch (e: Exception) {
                    Log.w("OfflineRepo", "Remote delete failed, queuing: ${e.message}")
                    pendingSyncDao.insert(
                        PendingSyncEntity(
                            id = UUID.randomUUID().toString(),
                            tableName = "balls",
                            recordId = ballId,
                            operation = "DELETE",
                            payload = ""
                        )
                    )
                }
            } else {
                val pending = pendingSyncDao.getAll()
                pending.filter { it.tableName == "balls" && it.recordId == ballId }
                    .forEach { pendingSyncDao.deleteById(it.id) }
            }
        }
    }

    /**
     * Get balls for an innings: try remote (and cache), fall back to local.
     */
    suspend fun getBallsByInnings(inningsId: String): List<Ball> {
        return try {
            val remoteBalls = remote.getBallsByInnings(inningsId)
            // Cache to Room in background
            syncScope.launch {
                ballDao.deleteByInningsId(inningsId)
                ballDao.insertAll(remoteBalls.map { it.toEntity(SyncStatus.SYNCED) })
            }
            remoteBalls
        } catch (e: Exception) {
            Log.w("OfflineRepo", "Remote getBalls failed, using local: ${e.message}")
            ballDao.getByInningsId(inningsId).map { it.toBall() }
        }
    }

    // ──────────────────────────── INNINGS ────────────────────────────

    /**
     * Update innings totals: save to Room and return IMMEDIATELY.
     * Supabase sync in background.
     */
    suspend fun updateInnings(
        inningsId: String,
        totalRuns: Int,
        totalWickets: Int,
        totalBalls: Int,
        extrasTotal: Int,
        wides: Int,
        noBalls: Int
    ): Innings {
        // 1. Update Room — instant
        val existing = inningsDao.getById(inningsId)
        val updated = existing?.copy(
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            totalBalls = totalBalls,
            extrasTotal = extrasTotal,
            wides = wides,
            noBalls = noBalls,
            syncStatus = SyncStatus.PENDING,
            localUpdatedAt = System.currentTimeMillis()
        )
        if (updated != null) {
            inningsDao.insert(updated)
        }

        // 2. Build the Innings to return immediately
        val result = updated?.toInnings() ?: Innings(
            id = inningsId, matchId = existing?.matchId ?: "",
            inningsNo = existing?.inningsNo ?: 1,
            battingTeamId = existing?.battingTeamId ?: "",
            bowlingTeamId = existing?.bowlingTeamId ?: "",
            totalRuns = totalRuns, totalWickets = totalWickets,
            totalBalls = totalBalls, extrasTotal = extrasTotal,
            wides = wides, noBalls = noBalls
        )

        // 3. Fire-and-forget: sync to Supabase
        syncScope.launch {
            try {
                remote.updateInnings(
                    inningsId, totalRuns, totalWickets, totalBalls, extrasTotal, wides, noBalls
                )
                inningsDao.updateSyncStatus(inningsId, SyncStatus.SYNCED)
            } catch (e: Exception) {
                Log.w("OfflineRepo", "Innings sync failed, queuing: ${e.message}")
                pendingSyncDao.insert(
                    PendingSyncEntity(
                        id = UUID.randomUUID().toString(),
                        tableName = "innings",
                        recordId = inningsId,
                        operation = "UPDATE",
                        payload = json.encodeToString(
                            mapOf(
                                "total_runs" to totalRuns.toString(),
                                "total_wickets" to totalWickets.toString(),
                                "total_balls" to totalBalls.toString(),
                                "extras_total" to extrasTotal.toString(),
                                "wides" to wides.toString(),
                                "no_balls" to noBalls.toString()
                            )
                        )
                    )
                )
            }
        }

        return result
    }

    /**
     * Get innings by match: try remote (and cache), fall back to local.
     */
    suspend fun getInningsByMatch(matchId: String): List<Innings> {
        return try {
            val remoteInnings = remote.getInningsByMatch(matchId)
            // Cache in background
            syncScope.launch {
                inningsDao.insertAll(remoteInnings.map { it.toEntity(SyncStatus.SYNCED) })
            }
            remoteInnings
        } catch (e: Exception) {
            Log.w("OfflineRepo", "Remote getInnings failed, using local: ${e.message}")
            inningsDao.getByMatchId(matchId).map { it.toInnings() }
        }
    }

    // ──────────────────────────── PASS-THROUGH ────────────────────────────

    suspend fun createInnings(innings: InningsInsert): Innings {
        val result = remote.createInnings(innings)
        syncScope.launch {
            inningsDao.insert(result.toEntity(SyncStatus.SYNCED))
        }
        return result
    }

    suspend fun completeInnings(inningsId: String) {
        // Update local immediately
        val existing = inningsDao.getById(inningsId)
        if (existing != null) {
            inningsDao.insert(existing.copy(status = "completed", syncStatus = SyncStatus.PENDING))
        }

        // Sync in background
        syncScope.launch {
            try {
                remote.completeInnings(inningsId)
                inningsDao.updateSyncStatus(inningsId, SyncStatus.SYNCED)
            } catch (e: Exception) {
                Log.w("OfflineRepo", "completeInnings failed, queuing: ${e.message}")
                pendingSyncDao.insert(
                    PendingSyncEntity(
                        id = UUID.randomUUID().toString(),
                        tableName = "innings",
                        recordId = inningsId,
                        operation = "UPDATE",
                        payload = json.encodeToString(mapOf("status" to "completed"))
                    )
                )
            }
        }
    }

    suspend fun getPlayingXIPlayers(matchId: String, teamId: String): List<Player> {
        return remote.getPlayingXIPlayers(matchId, teamId)
    }

    fun invalidateInningsCache(matchId: String) = remote.invalidateInningsCache(matchId)
    fun invalidateBallsCache(inningsId: String) = remote.invalidateBallsCache(inningsId)
    fun invalidatePlayersCache(matchId: String) = remote.invalidatePlayersCache(matchId)
}