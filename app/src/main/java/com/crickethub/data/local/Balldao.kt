package com.crickethub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BallDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ball: BallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balls: List<BallEntity>)

    @Query("SELECT * FROM balls WHERE innings_id = :inningsId ORDER BY local_created_at ASC")
    suspend fun getByInningsId(inningsId: String): List<BallEntity>

    @Query("SELECT * FROM balls WHERE id = :id")
    suspend fun getById(id: String): BallEntity?

    @Query("DELETE FROM balls WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM balls WHERE sync_status = '${SyncStatus.PENDING}'")
    suspend fun getPendingSync(): List<BallEntity>

    @Query("UPDATE balls SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE balls SET id = :newId, sync_status = '${SyncStatus.SYNCED}' WHERE id = :oldId")
    suspend fun replaceIdAndMarkSynced(oldId: String, newId: String)

    @Query("DELETE FROM balls WHERE innings_id = :inningsId")
    suspend fun deleteByInningsId(inningsId: String)
}