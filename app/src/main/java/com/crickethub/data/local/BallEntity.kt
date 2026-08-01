package com.crickethub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balls")
data class BallEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "innings_id") val inningsId: String,
    @ColumnInfo(name = "over_no") val overNo: Int,
    @ColumnInfo(name = "ball_no") val ballNo: Int,
    @ColumnInfo(name = "delivery_no") val deliveryNo: Int? = null,
    @ColumnInfo(name = "batsman_id") val batsmanId: String,
    @ColumnInfo(name = "non_striker_id") val nonStrikerId: String? = null,
    @ColumnInfo(name = "bowler_id") val bowlerId: String,
    @ColumnInfo(name = "runs_off_bat") val runsOffBat: Int = 0,
    @ColumnInfo(name = "extras_runs") val extrasRuns: Int? = null,
    @ColumnInfo(name = "extras_type") val extrasType: String? = null,
    @ColumnInfo(name = "is_wicket") val isWicket: Boolean = false,
    @ColumnInfo(name = "wicket_type") val wicketType: String? = null,
    @ColumnInfo(name = "fielder_name") val fielderName: String? = null,
    @ColumnInfo(name = "is_boundary") val isBoundary: Boolean = false,
    @ColumnInfo(name = "is_six") val isSix: Boolean = false,
    @ColumnInfo(name = "innings_phase") val inningsPhase: String? = null,
    val commentary: String? = null,
    @ColumnInfo(name = "dismissed_batsman_id") val dismissedBatsmanId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncStatus.PENDING,
    @ColumnInfo(name = "local_created_at") val localCreatedAt: Long = System.currentTimeMillis()
)

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING = "PENDING"
    const val FAILED = "FAILED"
}