package com.crickethub.data.repository

import com.crickethub.data.model.Innings
import com.crickethub.data.model.Match
import com.crickethub.data.model.Team
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

/** Per-match result from this team's point of view. */
private enum class Outcome { WON, LOST, TIED, NO_RESULT }

class TeamStatsRepository {
    private val client = SupabaseClient.client

    suspend fun getTeamStats(teamId: String): TeamStats {
        // All completed matches this team played in.
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

        // All completed innings for those matches (super overs included; filtered per use).
        val matchIds = allMatches.map { it.id }
        val allInnings = client.postgrest["innings"]
            .select {
                filter {
                    isIn("match_id", matchIds)
                    eq("status", "completed")
                }
            }
            .decodeList<Innings>()

        val inningsByMatch = allInnings.groupBy { it.matchId }

        // result_type is only ever written for the boundary-count tiebreak, and that
        // result_text names the winner. So we only need this team's name in that case.
        val teamName: String? =
            if (allMatches.any { it.resultType == "boundary_count" }) {
                try {
                    client.postgrest["teams"]
                        .select { filter { eq("id", teamId) } }
                        .decodeSingleOrNull<Team>()?.name
                } catch (e: Exception) {
                    android.util.Log.w("CricketHub", "TeamStats team name: ${e.message}")
                    null
                }
            } else null

        // Chronological order drives the streak calculations.
        val ordered = allMatches.sortedBy { it.createdAt ?: it.matchDate ?: "" }
        val outcomes: List<Outcome> = ordered.map { match ->
            outcomeFor(match, inningsByMatch[match.id].orEmpty(), teamId, teamName)
        }

        var won = 0
        var lost = 0
        var tied = 0
        var noResult = 0
        outcomes.forEach {
            when (it) {
                Outcome.WON -> won++
                Outcome.LOST -> lost++
                Outcome.TIED -> tied++
                Outcome.NO_RESULT -> noResult++
            }
        }

        // Scores: this team's MAIN batting innings only (exclude super overs, or a
        // 6-run super over would become the "lowest score").
        val mainBatting = allInnings.filter { it.battingTeamId == teamId && it.inningsNo <= 2 }
        val scores = mainBatting.map { it.totalRuns }
        val totalRuns = scores.sum()
        val totalWicketsLost = mainBatting.sumOf { it.totalWickets }
        val highestScore = scores.maxOrNull() ?: 0
        val lowestScore = scores.minOrNull() ?: 0

        // Longest winning streak: max run of consecutive wins anywhere in the season.
        var longestStreak = 0
        var run = 0
        for (o in outcomes) {
            if (o == Outcome.WON) {
                run++
                if (run > longestStreak) longestStreak = run
            } else {
                run = 0
            }
        }

        // Current winning streak: consecutive wins from the most recent match backwards,
        // stopping at the first non-win (a loss, tie, or no-result all break it).
        var currentStreak = 0
        for (o in outcomes.asReversed()) {
            if (o == Outcome.WON) currentStreak++ else break
        }

        val played = ordered.size
        val decided = won + lost + tied
        val winPct = if (decided > 0) won.toDouble() / decided * 100 else 0.0

        return TeamStats(
            matchesPlayed = played,
            won = won,
            lost = lost,
            tied = tied,
            noResult = noResult,
            winPercentage = winPct,
            totalRuns = totalRuns,
            totalWickets = totalWicketsLost,
            highestScore = highestScore,
            lowestScore = lowestScore,
            currentWinStreak = currentStreak,
            longestWinStreak = longestStreak
        )
    }

    /**
     * Decide a single match's result for [teamId]. Deterministic from innings +
     * super overs; the only text-based branch is the boundary-count tiebreak, whose
     * result_text explicitly names the winner ("Match tied - <Team> won on boundary count").
     */
    private fun outcomeFor(
        match: Match,
        innings: List<Innings>,
        teamId: String,
        teamName: String?
    ): Outcome {
        // Explicit no-result markers (rarely written, but honor them if present).
        if (match.resultType in listOf("no_result", "abandoned", "cancelled")) {
            return Outcome.NO_RESULT
        }

        // Boundary-count decider: winner is named in result_text.
        if (match.resultType == "boundary_count") {
            val txt = match.resultText ?: ""
            val thisTeamWon = teamName != null && txt.contains("$teamName won on boundary count")
            val someoneWon = txt.contains("won on boundary count")
            val outcome = when {
                thisTeamWon -> Outcome.WON
                someoneWon -> Outcome.LOST      // the opponent is the one named
                else -> Outcome.TIED            // "boundaries level" — genuinely tied
            }
            android.util.Log.d(
                "CricketHub",
                "TeamStats boundary_count: team='$teamName' text='$txt' -> $outcome"
            )
            return outcome
        }

        val inn1 = innings.firstOrNull { it.inningsNo == 1 }
        val inn2 = innings.firstOrNull { it.inningsNo == 2 }
        // Can't determine a result without both main innings.
        if (inn1 == null || inn2 == null) return Outcome.NO_RESULT

        val winnerId: String? = when {
            inn2.totalRuns > inn1.totalRuns -> inn2.battingTeamId
            inn1.totalRuns > inn2.totalRuns -> inn1.battingTeamId
            else -> superOverWinner(innings)   // tie at main level -> super over, or null
        }

        return when {
            winnerId == null -> Outcome.TIED
            winnerId == teamId -> Outcome.WON
            else -> Outcome.LOST
        }
    }

    /**
     * Winner of the last completed super-over pair (innings 3&4 = 1st SO, 5&6 = 2nd...).
     * A later pair supersedes an earlier tied one. Returns null if there's no completed
     * pair or the deciding pair is itself tied.
     */
    private fun superOverWinner(innings: List<Innings>): String? {
        val so = innings.filter { it.inningsNo >= 3 && it.status == "completed" }
            .sortedBy { it.inningsNo }
        if (so.size < 2) return null
        val lastPairStart = if (so.size % 2 == 0) so.size - 2 else so.size - 1
        val bat = so.getOrNull(lastPairStart) ?: return null
        val chase = so.getOrNull(lastPairStart + 1) ?: return null
        return when {
            chase.totalRuns > bat.totalRuns -> chase.battingTeamId
            bat.totalRuns > chase.totalRuns -> bat.battingTeamId
            else -> null
        }
    }
}