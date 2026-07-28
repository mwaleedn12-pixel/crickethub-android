package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A partnership row. Columns match the Supabase `partnerships` table:
 * id, innings_id, batsman1_id, batsman2_id, runs, balls, wicket_no.
 * A partnership is the stand between two batsmen; wicket_no is which wicket
 * ended it (the final unbroken stand uses wickets + 1).
 */
@Serializable
data class Partnership(
    val id: String = "",
    @SerialName("innings_id") val inningsId: String,
    @SerialName("batsman1_id") val batsman1Id: String,
    @SerialName("batsman2_id") val batsman2Id: String,
    val runs: Int,
    val balls: Int,
    @SerialName("wicket_no") val wicketNo: Int
)

/** Insert payload — id is left to the DB default. */
@Serializable
data class PartnershipInsert(
    @SerialName("innings_id") val inningsId: String,
    @SerialName("batsman1_id") val batsman1Id: String,
    @SerialName("batsman2_id") val batsman2Id: String,
    val runs: Int,
    val balls: Int,
    @SerialName("wicket_no") val wicketNo: Int
)