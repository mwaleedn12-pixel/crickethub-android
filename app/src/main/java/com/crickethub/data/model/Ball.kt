package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ball(
    val id: String = "",
    @SerialName("innings_id") val inningsId: String,
    @SerialName("over_no") val overNo: Int,
    @SerialName("ball_no") val ballNo: Int,
    @SerialName("delivery_no") val deliveryNo: Int? = null,
    @SerialName("batsman_id") val batsmanId: String,
    @SerialName("non_striker_id") val nonStrikerId: String? = null,
    @SerialName("bowler_id") val bowlerId: String,
    @SerialName("runs_off_bat") val runsOffBat: Int = 0,
    @SerialName("extras_runs") val extrasRuns: Int? = null,
    @SerialName("extras_type") val extrasType: String? = null,
    @SerialName("is_wicket") val isWicket: Boolean = false,
    @SerialName("wicket_type") val wicketType: String? = null,
    @SerialName("fielder_id") val fielderId: String? = null,
    @SerialName("is_boundary") val isBoundary: Boolean = false,
    @SerialName("is_six") val isSix: Boolean = false,
    @SerialName("innings_phase") val inningsPhase: String? = null,
    val commentary: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BallInsert(
    @SerialName("innings_id") val inningsId: String,
    @SerialName("over_no") val overNo: Int,
    @SerialName("ball_no") val ballNo: Int,
    @SerialName("delivery_no") val deliveryNo: Int? = null,
    @SerialName("batsman_id") val batsmanId: String,
    @SerialName("non_striker_id") val nonStrikerId: String? = null,
    @SerialName("bowler_id") val bowlerId: String,
    @SerialName("runs_off_bat") val runsOffBat: Int = 0,
    @SerialName("extras_runs") val extrasRuns: Int? = null,
    @SerialName("extras_type") val extrasType: String? = null,
    @SerialName("is_wicket") val isWicket: Boolean = false,
    @SerialName("wicket_type") val wicketType: String? = null,
    @SerialName("is_boundary") val isBoundary: Boolean = false,
    @SerialName("is_six") val isSix: Boolean = false,
    @SerialName("innings_phase") val inningsPhase: String? = null,
    val commentary: String? = null
)