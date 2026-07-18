package com.crickethub.data.repository

import com.crickethub.data.model.Innings
import com.crickethub.data.model.Match
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

data class TeamStats(
    val matchesPlayed: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val tied: Int = 0,
    val noResult: Int = 0,
    val winPercentage: Double = 0.0,
    val totalRuns: Int = 0,
    val totalWickets: Int = 0,
    val highestScore: Int = 0,
    val lowestScore: Int = 0,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0
)

class TeamStatsRepository {
    private val client = SupabaseClient.client

    suspend fun getTeamStats(teamId: String): TeamStats {
        // Get all completed matches for this team
        val allMatches = client.postgrest["matches"]
            .select {
                filter {
                    or {
                        eq("team1_id", teamId)
                        eq("team2_id", teamId)
                    }
                    eq("status", "completed")
                }
            }
            .decodeList<Match>()

        if (allMatches.isEmpty()) return TeamStats()

        // Get all innings for these matches
        val matchIds = allMatches.map { it.id }
        val allInnings = client.postgrest["innings"]
            .select {
                filter {
                    isIn("match_id", matchIds)
                    eq("status", "completed")
                }
            }
            .decodeList<Innings>()

        // Team's batting innings
        val battingInnings = allInnings.filter { it.battingTeamId == teamId }

        var won = 0
        var lost = 0
        var tied = 0
        var noResult = 0

        // Sort matches chronologically for streak calculation
        val sortedMatches = allMatches.sortedBy { it.createdAt ?: "" }

        for (match in sortedMatches) {
            val resultType = match.resultType ?: ""
            when {
                resultType == "no_result" || resultType == "abandoned" -> noResult++
                resultType == "tied" -> tied++
                else -> {
                    // Determine winner from innings scores
                    val inn1 = allInnings.find { it.matchId == match.id && it.inningsNo == 1 }
                    val inn2 = allInnings.find { it.matchId == match.id && it.inningsNo == 2 }

                    if (inn1 != null && inn2 != null) {
                        val team1Won = inn1.battingTeamId == match.team1Id &&
                                inn1.totalRuns > inn2.totalRuns ||
                                inn2.battingTeamId == match.team1Id &&
                                inn2.totalRuns > inn1.totalRuns

                        val winnerTeamId = if (team1Won) {
                            if (inn1.battingTeamId == match.team1Id && inn1.totalRuns > inn2.totalRuns)
                                match.team1Id
                            else match.team2Id
                        } else {
                            if (inn1.battingTeamId == match.team1Id) match.team2Id else match.team1Id
                        }

                        if (winnerTeamId == teamId) won++ else lost++
                    }
                }
            }
        }

        // Scores
        val scores = battingInnings.map { it.totalRuns }
        val totalRuns = scores.sum()
        val totalWickets = battingInnings.sumOf { it.totalWickets }
        val highestScore = scores.maxOrNull() ?: 0
        val lowestScore = scores.minOrNull() ?: 0

        // Win streaks
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        for (match in sortedMatches.reversed()) {
            val inn1 = allInnings.find { it.matchId == match.id && it.inningsNo == 1 }
            val inn2 = allInnings.find { it.matchId == match.id && it.inningsNo == 2 }

            if (inn1 != null && inn2 != null && match.resultType != "no_result" && match.resultType != "tied") {
                val scores1 = if (inn1.battingTeamId == teamId) inn1.totalRuns else inn2.totalRuns
                val scores2 = if (inn1.battingTeamId == teamId) inn2.totalRuns else inn1.totalRuns
                if (scores1 > scores2) {
                    if (tempStreak == 0 || currentStreak == tempStreak) currentStreak++
                    tempStreak++
                } else {
                    tempStreak = 0
                }
                longestStreak = maxOf(longestStreak, tempStreak)
            }
        }

        val matchesPlayed = won + lost + tied
        val winPct = if (matchesPlayed > 0) (won.toDouble() / matchesPlayed * 100) else 0.0

        return TeamStats(
            matchesPlayed = matchesPlayed + noResult,
            won = won,
            lost = lost,
            tied = tied,
            noResult = noResult,
            winPercentage = winPct,
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            highestScore = highestScore,
            lowestScore = if (scores.isEmpty()) 0 else scores.minOrNull() ?: 0,
            currentWinStreak = currentStreak,
            longestWinStreak = longestStreak
        )
    }
}