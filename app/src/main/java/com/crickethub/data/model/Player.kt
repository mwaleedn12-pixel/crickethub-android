package com.crickethub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String = "",
    @SerialName("team_id") val teamId: String = "",
    @SerialName("full_name") val fullName: String = "",
    val nickname: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("jersey_no") val jerseyNo: Int? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = "male",
    val country: String? = null,
    val city: String? = null,
    @SerialName("batting_hand") val battingHand: String? = "right",
    @SerialName("bowling_hand") val bowlingHand: String? = "right",
    @SerialName("bowling_style") val bowlingStyle: String? = null,
    val role: String? = "batsman",
    val availability: String? = "available",
    @SerialName("injury_status") val injuryStatus: String? = null,
    @SerialName("current_team_id") val currentTeamId: String? = null,
    @SerialName("debut_date") val debutDate: String? = null,
    @SerialName("career_span") val careerSpan: String? = null,
    @SerialName("batting_rating") val battingRating: Int? = 50,
    @SerialName("bowling_rating") val bowlingRating: Int? = 50,
    @SerialName("fielding_rating") val fieldingRating: Int? = 50,
    val awards: List<String>? = null,
    val achievements: List<String>? = null,
    @SerialName("previous_teams") val previousTeams: List<String>? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PlayerInsert(
    @SerialName("team_id") val teamId: String,
    @SerialName("full_name") val fullName: String,
    val nickname: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("jersey_no") val jerseyNo: Int? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = "male",
    val country: String? = null,
    val city: String? = null,
    @SerialName("batting_hand") val battingHand: String? = "right",
    @SerialName("bowling_hand") val bowlingHand: String? = "right",
    @SerialName("bowling_style") val bowlingStyle: String? = null,
    val role: String? = "batsman",
    val availability: String? = "available",
    @SerialName("injury_status") val injuryStatus: String? = null,
    @SerialName("debut_date") val debutDate: String? = null
)

data class PlayerStats(
    // Batting
    val matches: Int = 0,
    val innings: Int = 0,
    val runs: Int = 0,
    val ballsFaced: Int = 0,
    val highestScore: Int = 0,
    val average: Double = 0.0,
    val strikeRate: Double = 0.0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val ducks: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val notOuts: Int = 0,
    val boundaryPercent: Double = 0.0,
    val dotBallPercent: Double = 0.0,
    // Bowling
    val oversBowled: Double = 0.0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val economy: Double = 0.0,
    val bowlingAverage: Double = 0.0,
    val bowlingStrikeRate: Double = 0.0,
    val bestBowling: String = "0/0",
    val threeWicketHauls: Int = 0,
    val fiveWicketHauls: Int = 0,
    val dotBalls: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0,
    // Fielding
    val catches: Int = 0,
    val runOuts: Int = 0,
    val stumpings: Int = 0,
    val missedChances: Int = 0
)

// Bowling style options based on hand
val RIGHT_HAND_BOWLING_STYLES = listOf(
    "Fast", "Fast Medium", "Medium", "Off Spin", "Leg Spin"
)
val LEFT_HAND_BOWLING_STYLES = listOf(
    "Fast", "Fast Medium", "Medium", "Chinaman", "Orthodox"
)

val PLAYER_ROLES = listOf(
    "Batsman", "Bowler", "All-rounder", "Wicket Keeper"
)