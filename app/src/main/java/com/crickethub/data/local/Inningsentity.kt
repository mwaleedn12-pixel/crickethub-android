package com.crickethub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "innings")
data class InningsEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "match_id") val matchId: String,
    @ColumnInfo(name = "innings_no") val inningsNo: Int,
    @ColumnInfo(name = "batting_team_id") val battingTeamId: String,
    @ColumnInfo(name = "bowling_team_id") val bowlingTeamId: String,
    @ColumnInfo(name = "total_runs") val totalRuns: Int = 0,
    @ColumnInfo(name = "total_wickets") val totalWickets: Int = 0,
    @ColumnInfo(name = "total_balls") val totalBalls: Int = 0,
    @ColumnInfo(name = "extras_total") val extrasTotal: Int = 0,
    val wides: Int = 0,
    @ColumnInfo(name = "no_balls") val noBalls: Int = 0,
    val byes: Int = 0,
    @ColumnInfo(name = "leg_byes") val legByes: Int = 0,
    val status: String = "pending",
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncStatus.SYNCED,
    @ColumnInfo(name = "local_updated_at") val localUpdatedAt: Long = System.currentTimeMillis()
)