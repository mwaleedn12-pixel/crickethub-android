package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tournament(
    val id: String = "",
    val name: String,
    val format: String = "round_robin",
    val status: String = "upcoming",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TournamentInsert(
    val name: String,
    val format: String = "round_robin",
    val status: String = "upcoming",
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class TournamentTeam(
    val id: String = "",
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("team_id") val teamId: String,
    val points: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    @SerialName("matches_played") val matchesPlayed: Int = 0,
    @SerialName("runs_scored") val runsScored: Int = 0,
    @SerialName("runs_conceded") val runsConceded: Int = 0,
    @SerialName("overs_faced") val oversFaced: Double = 0.0,
    @SerialName("overs_bowled") val oversBowled: Double = 0.0
) {
    val nrr: Double
        get() {
            val runRateFor = if (oversFaced > 0) runsScored / oversFaced else 0.0
            val runRateAgainst = if (oversBowled > 0) runsConceded / oversBowled else 0.0
            return runRateFor - runRateAgainst
        }
}

@Serializable
data class TournamentTeamInsert(
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("team_id") val teamId: String
)