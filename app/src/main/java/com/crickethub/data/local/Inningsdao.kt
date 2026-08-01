package com.crickethub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InningsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(innings: InningsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(innings: List<InningsEntity>)

    @Query("SELECT * FROM innings WHERE match_id = :matchId ORDER BY innings_no ASC")
    suspend fun getByMatchId(matchId: String): List<InningsEntity>

    @Query("SELECT * FROM innings WHERE id = :id")
    suspend fun getById(id: String): InningsEntity?

    @Query("UPDATE innings SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT * FROM innings WHERE sync_status = '${SyncStatus.PENDING}'")
    suspend fun getPendingSync(): List<InningsEntity>
}