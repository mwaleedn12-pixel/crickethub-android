package com.crickethub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync ORDER BY created_at ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync ORDER BY created_at ASC LIMIT 1")
    suspend fun getOldest(): PendingSyncEntity?

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_sync SET retry_count = retry_count + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun count(): Int
}