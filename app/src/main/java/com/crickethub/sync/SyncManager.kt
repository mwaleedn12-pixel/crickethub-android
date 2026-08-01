package com.crickethub.data.sync

import android.util.Log
import com.crickethub.data.local.AppDatabase
import com.crickethub.data.local.SyncStatus
import com.crickethub.data.model.BallInsert
import com.crickethub.data.repository.ScoringRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** High-level sync state exposed to the UI. */
enum class SyncState { SYNCED, SYNCING, PENDING, OFFLINE }

/**
 * Processes the [PendingSyncEntity] queue, pushing locally-saved data to Supabase.
 *
 * - On network recovery (triggered by [NetworkMonitor]), drains the queue oldest-first.
 * - Retries up to [MAX_RETRIES] per item before skipping.
 * - Exposes [syncState] for UI indicators.
 */
class SyncManager(
    private val db: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    private val remote: ScoringRepository = ScoringRepository()
) {
    companion object {
        private const val MAX_RETRIES = 5
        private const val TAG = "SyncManager"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.SYNCED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val pendingSyncDao get() = db.pendingSyncDao()
    private val ballDao get() = db.ballDao()
    private val inningsDao get() = db.inningsDao()

    init {
        // When we come back online, start syncing.
        networkMonitor.addOnOnlineListener {
            scope.launch { processQueue() }
        }
        // Also update state when connectivity changes.
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (!online) {
                    _syncState.value = SyncState.OFFLINE
                } else {
                    refreshState()
                }
            }
        }
    }

    /** Call on app resume to pick up anything queued while backgrounded. */
    fun triggerSync() {
        if (networkMonitor.isOnline.value) {
            scope.launch { processQueue() }
        }
    }

    /** Recompute the sync indicator from DB state. */
    private suspend fun refreshState() {
        val count = pendingSyncDao.count()
        _syncState.value = when {
            !networkMonitor.isOnline.value -> SyncState.OFFLINE
            count > 0 -> SyncState.PENDING
            else -> SyncState.SYNCED
        }
    }

    /**
     * Process pending items oldest-first. Each successful sync removes the item;
     * each failure increments retryCount and skips items past [MAX_RETRIES].
     */
    private suspend fun processQueue() {
        _syncState.value = SyncState.SYNCING
        Log.d(TAG, "Processing sync queue...")

        val items = pendingSyncDao.getAll()
        if (items.isEmpty()) {
            _syncState.value = SyncState.SYNCED
            Log.d(TAG, "Queue empty — all synced")
            return
        }

        for (item in items) {
            if (item.retryCount >= MAX_RETRIES) {
                Log.w(TAG, "Skipping ${item.id} (table=${item.tableName}, op=${item.operation}) — max retries reached")
                continue
            }
            if (!networkMonitor.isOnline.value) {
                Log.d(TAG, "Went offline mid-sync, stopping")
                _syncState.value = SyncState.OFFLINE
                return
            }

            try {
                when (item.tableName) {
                    "balls" -> processBallSync(item)
                    "innings" -> processInningsSync(item)
                    else -> Log.w(TAG, "Unknown table: ${item.tableName}")
                }
                // Success — remove from queue
                pendingSyncDao.deleteById(item.id)
                Log.d(TAG, "Synced ${item.tableName}/${item.operation} recordId=${item.recordId}")
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed for ${item.id}: ${e.message}")
                pendingSyncDao.incrementRetry(item.id)
            }
        }

        refreshState()
    }

    private suspend fun processBallSync(item: com.crickethub.data.local.PendingSyncEntity) {
        when (item.operation) {
            "INSERT" -> {
                val ballInsert = json.decodeFromString<BallInsert>(item.payload)
                val remoteBall = remote.insertBall(ballInsert)
                // Update local record: swap local UUID → server id, mark SYNCED
                ballDao.replaceIdAndMarkSynced(oldId = item.recordId, newId = remoteBall.id)
            }
            "DELETE" -> {
                remote.deleteLastBall(item.recordId)
            }
            else -> Log.w(TAG, "Unknown ball operation: ${item.operation}")
        }
    }

    private suspend fun processInningsSync(item: com.crickethub.data.local.PendingSyncEntity) {
        when (item.operation) {
            "UPDATE" -> {
                val data = json.decodeFromString<Map<String, String>>(item.payload)
                // Check if this is a status-only update (e.g. "completed")
                if (data.containsKey("status") && data.size == 1) {
                    remote.completeInnings(item.recordId)
                } else {
                    remote.updateInnings(
                        inningsId = item.recordId,
                        totalRuns = data["total_runs"]?.toIntOrNull() ?: 0,
                        totalWickets = data["total_wickets"]?.toIntOrNull() ?: 0,
                        totalBalls = data["total_balls"]?.toIntOrNull() ?: 0,
                        extrasTotal = data["extras_total"]?.toIntOrNull() ?: 0,
                        wides = data["wides"]?.toIntOrNull() ?: 0,
                        noBalls = data["no_balls"]?.toIntOrNull() ?: 0
                    )
                }
                inningsDao.updateSyncStatus(item.recordId, SyncStatus.SYNCED)
            }
            else -> Log.w(TAG, "Unknown innings operation: ${item.operation}")
        }
    }
}