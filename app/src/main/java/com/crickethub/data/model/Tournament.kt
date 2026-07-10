package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tournament(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val format: String? = null,
    val status: String = "upcoming",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val venue: String? = null,
    val organizer: String? = null,
    @SerialName("prize_pool") val prizePool: String? = null,
    @SerialName("max_teams") val maxTeams: Int? = null,
    @SerialName("overs_per_match") val oversPerMatch: Int? = 20,
    @SerialName("players_per_side") val playersPerSide: Int? = 11,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("match_type") val matchType: String? = "T20"
)

@Serializable
data class TournamentInsert(
    val name: String,
    val description: String? = null,
    val format: String? = null,
    val status: String = "upcoming",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val venue: String? = null,
    val organizer: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true
)

@Serializable
data class TournamentTeam(
    val id: String = "",
    @SerialName("tournament_id") val tournamentId: String = "",
    @SerialName("team_id") val teamId: String = "",
    @SerialName("matches_played") val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    @SerialName("no_results") val noResults: Int = 0,
    val points: Int = 0,
    val nrr: Double = 0.0,
    val draws: Int = 0,
    @SerialName("runs_scored_total") val runsScoreTotal: Int = 0,
    @SerialName("runs_conceded_total") val runsConcededTotal: Int = 0,
    @SerialName("overs_faced_total") val oversFacedTotal: Double = 0.0,
    @SerialName("overs_bowled_total") val oversBowledTotal: Double = 0.0
)

@Serializable
data class TournamentTeamInsert(
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("matches_played") val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    @SerialName("no_results") val noResults: Int = 0,
    val points: Int = 0,
    val nrr: Double = 0.0,
    val draws: Int = 0,
    @SerialName("runs_scored_total") val runsScoreTotal: Int = 0,
    @SerialName("runs_conceded_total") val runsConcededTotal: Int = 0,
    @SerialName("overs_faced_total") val oversFacedTotal: Double = 0.0,
    @SerialName("overs_bowled_total") val oversBowledTotal: Double = 0.0
)