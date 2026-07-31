package com.crickethub.data.repository

import com.crickethub.data.model.Match
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.Tournament
import com.crickethub.data.model.TournamentInsert
import com.crickethub.data.model.TournamentTeam
import com.crickethub.data.model.TournamentTeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlin.math.log2

class TournamentRepository {

    private val client = SupabaseClient.client

    // TTL cache
    private var tournamentsCache: List<Tournament>? = null
    private var tournamentsCacheTime: Long = 0L
    private val TTL_MS = 2 * 60 * 1000L

    suspend fun getAllTournaments(): List<Tournament> {
        val cached = tournamentsCache
        if (cached != null && System.currentTimeMillis() - tournamentsCacheTime < TTL_MS) {
            return cached
        }
        return SupabaseClient.withRetry {
            val list = client.postgrest["tournaments"]
                .select()
                .decodeList<Tournament>()
            tournamentsCache = list
            tournamentsCacheTime = System.currentTimeMillis()
            list
        }
    }

    suspend fun getTournamentById(tournamentId: String): Tournament? {
        return SupabaseClient.withRetry {
            client.postgrest["tournaments"]
                .select { filter { eq("id", tournamentId) } }
                .decodeSingleOrNull()
        }
    }

    suspend fun createTournament(tournament: TournamentInsert): Tournament {
        val userId = client.auth.currentUserOrNull()?.id
        val result = SupabaseClient.withRetry {
            client.postgrest["tournaments"]
                .insert(tournament.copy(userId = userId)) { select() }
                .decodeSingle<Tournament>()
        }
        tournamentsCache = null
        tournamentsCacheTime = 0L
        return result
    }

    suspend fun getTournamentTeams(tournamentId: String): List<TournamentTeam> {
        return SupabaseClient.withRetry {
            client.postgrest["tournament_teams"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList()
        }
    }

    suspend fun getTournamentFixtures(tournamentId: String): List<Match> {
        return SupabaseClient.withRetry {
            client.postgrest["matches"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList()
        }
    }

    suspend fun addTeamToTournament(tournamentId: String, teamId: String) {
        try {
            val existing = client.postgrest["tournament_teams"]
                .select {
                    filter {
                        eq("tournament_id", tournamentId)
                        eq("team_id", teamId)
                    }
                }
                .decodeList<TournamentTeam>()

            if (existing.isEmpty()) {
                client.postgrest["tournament_teams"]
                    .insert(
                        TournamentTeamInsert(
                            tournamentId = tournamentId,
                            teamId = teamId
                        )
                    )
            }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Add team error: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteTournament(tournamentId: String) {
        val matchRepository = MatchRepository()
        try {
            client.postgrest["player_awards"].delete { filter { eq("tournament_id", tournamentId) } }
        } catch (e: Exception) {
            android.util.Log.w("CricketHub", "deleteTournament awards: ${e.message}")
        }
        val fixtures = try {
            client.postgrest["matches"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList<Match>()
        } catch (e: Exception) {
            android.util.Log.w("CricketHub", "deleteTournament fixtures fetch: ${e.message}")
            emptyList()
        }
        for (m in fixtures) {
            try { matchRepository.deleteMatch(m.id) }
            catch (e: Exception) { android.util.Log.w("CricketHub", "deleteTournament match ${m.id}: ${e.message}") }
        }
        client.postgrest["tournaments"].delete { filter { eq("id", tournamentId) } }
    }

    suspend fun removeTeamFromTournament(tournamentId: String, teamId: String) {
        client.postgrest["tournament_teams"]
            .delete {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("team_id", teamId)
                }
            }
    }

    // ── Fixture generation ─────────────────────────────────────────

    private data class FixtureSpec(val team1Id: String, val team2Id: String, val title: String)

    suspend fun generateFixtures(tournamentId: String, format: String, seriesMatches: Int = 3): List<Match> {
        val tournament = getTournamentById(tournamentId) ?: return emptyList()
        val teams = getTournamentTeams(tournamentId)
        val teamIds = teams.map { it.teamId }
        val n = teamIds.size
        if (n < 2) return emptyList()

        val specs = mutableListOf<FixtureSpec>()

        when (format) {
            "Round Robin" -> {
                for (i in teamIds.indices)
                    for (j in i + 1 until n)
                        specs.add(FixtureSpec(teamIds[i], teamIds[j], "League"))
            }

            "Double Round Robin" -> {
                for (i in teamIds.indices)
                    for (j in i + 1 until n)
                        specs.add(FixtureSpec(teamIds[i], teamIds[j], "Leg 1"))
                for (i in teamIds.indices)
                    for (j in i + 1 until n)
                        specs.add(FixtureSpec(teamIds[j], teamIds[i], "Leg 2"))
            }

            "Single Knockout" -> {
                specs.addAll(knockoutFirstRound(teamIds))
            }

            "Group + Knockout" -> {
                if (n < 4) {
                    // Fall back to round robin if <4 teams
                    for (i in teamIds.indices)
                        for (j in i + 1 until n)
                            specs.add(FixtureSpec(teamIds[i], teamIds[j], "League"))
                } else {
                    val half = n / 2
                    val groupA = teamIds.subList(0, half)
                    val groupB = teamIds.subList(half, n)
                    for (i in groupA.indices)
                        for (j in i + 1 until groupA.size)
                            specs.add(FixtureSpec(groupA[i], groupA[j], "Group A"))
                    for (i in groupB.indices)
                        for (j in i + 1 until groupB.size)
                            specs.add(FixtureSpec(groupB[i], groupB[j], "Group B"))
                    // Knockout matches created later when teams qualify
                }
            }

            "League + Playoffs" -> {
                for (i in teamIds.indices)
                    for (j in i + 1 until n)
                        specs.add(FixtureSpec(teamIds[i], teamIds[j], "League"))
                // Playoff matches created later when standings are final
            }

            "Bilateral Series" -> {
                if (n >= 2) {
                    val count = seriesMatches.coerceIn(1, 7)
                    for (i in 1..count) {
                        val home = if (i % 2 == 1) teamIds[0] else teamIds[1]
                        val away = if (i % 2 == 1) teamIds[1] else teamIds[0]
                        specs.add(FixtureSpec(home, away, "Series"))
                    }
                }
            }

            "Tri-Series" -> {
                val triTeams = teamIds.take(3)
                if (triTeams.size == 3) {
                    for (i in 0 until 3)
                        for (j in i + 1 until 3)
                            specs.add(FixtureSpec(triTeams[i], triTeams[j], "League"))
                    for (i in 0 until 3)
                        for (j in i + 1 until 3)
                            specs.add(FixtureSpec(triTeams[j], triTeams[i], "League"))
                    // Final created when standings are final
                }
            }

            "Custom Tournament" -> {
                for (i in teamIds.indices)
                    for (j in i + 1 until n)
                        specs.add(FixtureSpec(teamIds[i], teamIds[j], "League"))
            }
        }

        // Interleave fixtures so one team's matches don't cluster together
        val interleaved = interleaveFixtures(specs)

        val matchType = tournament.matchType ?: "T20"
        val totalOvers = tournament.oversPerMatch ?: 20
        val playersPerSide = tournament.playersPerSide ?: 11
        val userId = client.auth.currentUserOrNull()?.id
        val ppOvers = when {
            totalOvers <= 10 -> 2
            totalOvers <= 20 -> 6
            else -> 10
        }

        interleaved.forEachIndexed { index, spec ->
            try {
                client.postgrest["matches"].insert(
                    MatchInsert(
                        userId = userId,
                        title = spec.title,
                        team1Id = spec.team1Id,
                        team2Id = spec.team2Id,
                        matchType = matchType,
                        totalOvers = totalOvers,
                        playersPerSide = playersPerSide,
                        tournamentId = tournamentId,
                        matchNumber = index + 1,
                        powerplayOvers = ppOvers,
                        freeHitOnNoball = true,
                        superOverEnabled = false,
                        maxOversPerBowler = if (totalOvers >= 5) totalOvers / 5 else null,
                        isPublic = true,
                        inningsBreakMinutes = 20
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Fixture #${index + 1} error: ${e.message}", e)
            }
        }

        return getTournamentFixtures(tournamentId)
    }

    /**
     * Interleave fixtures so no team plays consecutive matches.
     * Uses a greedy approach: pick the next fixture where neither
     * team played in the previous fixture.
     */
    private fun interleaveFixtures(specs: List<FixtureSpec>): List<FixtureSpec> {
        if (specs.size <= 2) return specs
        val remaining = specs.toMutableList()
        val result = mutableListOf<FixtureSpec>()
        val lastTeams = mutableSetOf<String>()

        while (remaining.isNotEmpty()) {
            // Find a fixture where neither team played last
            val idx = remaining.indexOfFirst { spec ->
                spec.team1Id !in lastTeams && spec.team2Id !in lastTeams
            }
            val pick = if (idx >= 0) {
                remaining.removeAt(idx)
            } else {
                // No ideal match — just take the first remaining
                remaining.removeAt(0)
            }
            result.add(pick)
            lastTeams.clear()
            lastTeams.add(pick.team1Id)
            lastTeams.add(pick.team2Id)
        }
        return result
    }

    /** Single Knockout: only first round (teams without byes) */
    private fun knockoutFirstRound(teamIds: List<String>): List<FixtureSpec> {
        val n = teamIds.size
        if (n == 2) return listOf(FixtureSpec(teamIds[0], teamIds[1], "Final"))

        var bracketSize = 1
        while (bracketSize < n) bracketSize *= 2
        val byes = bracketSize - n
        val totalRounds = log2(bracketSize.toDouble()).toInt()
        val roundLabel = knockoutLabel(totalRounds - 1)

        val playing = teamIds.subList(byes, n)
        val specs = mutableListOf<FixtureSpec>()
        val matchCount = playing.size / 2
        for (i in 0 until matchCount) {
            val t1 = playing[i]
            val t2 = playing[playing.size - 1 - i]
            val label = if (matchCount > 1) "$roundLabel ${i + 1}" else roundLabel
            specs.add(FixtureSpec(t1, t2, label))
        }
        return specs
    }

    /** Create a knockout match when teams qualify */
    suspend fun createKnockoutMatch(
        tournamentId: String,
        team1Id: String,
        team2Id: String,
        title: String,
        matchNumber: Int
    ) {
        val tournament = getTournamentById(tournamentId) ?: return
        val matchType = tournament.matchType ?: "T20"
        val totalOvers = tournament.oversPerMatch ?: 20
        val userId = client.auth.currentUserOrNull()?.id
        try {
            client.postgrest["matches"].insert(
                MatchInsert(
                    userId = userId,
                    title = title,
                    team1Id = team1Id,
                    team2Id = team2Id,
                    matchType = matchType,
                    totalOvers = totalOvers,
                    playersPerSide = tournament.playersPerSide ?: 11,
                    tournamentId = tournamentId,
                    matchNumber = matchNumber,
                    powerplayOvers = if (totalOvers <= 10) 2 else if (totalOvers <= 20) 6 else 10,
                    freeHitOnNoball = true,
                    superOverEnabled = false,
                    maxOversPerBowler = if (totalOvers >= 5) totalOvers / 5 else null,
                    isPublic = true,
                    inningsBreakMinutes = 20
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Knockout match create error: ${e.message}", e)
            throw e
        }
    }

    /** Reschedule a fixture */
    suspend fun rescheduleMatch(matchId: String, newDate: String?, newTime: String?) {
        try {
            client.postgrest["matches"].update({
                if (newDate != null) set("match_date", newDate)
                if (newTime != null) set("match_time", newTime)
            }) { filter { eq("id", matchId) } }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Reschedule error: ${e.message}", e)
            throw e
        }
    }

    // ── Points table ───────────────────────────────────────────────

    suspend fun updatePointsTable(
        tournamentId: String,
        winnerTeamId: String,
        loserTeamId: String,
        winnerRuns: Int,
        loserRuns: Int,
        winnerOvers: Double,
        loserOvers: Double
    ) {
        android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable CALLED: tournament=$tournamentId winner=$winnerTeamId loser=$loserTeamId " +
                "winnerRuns=$winnerRuns loserRuns=$loserRuns winnerOvers=$winnerOvers loserOvers=$loserOvers")
        try {
            val winnerEntry = client.postgrest["tournament_teams"]
                .select { filter { eq("tournament_id", tournamentId); eq("team_id", winnerTeamId) } }
                .decodeSingleOrNull<TournamentTeam>()

            android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: winnerEntry=${winnerEntry != null} id=${winnerEntry?.id}")

            if (winnerEntry != null) {
                val rs = winnerEntry.runsScoreTotal + winnerRuns
                val rc = winnerEntry.runsConcededTotal + loserRuns
                val of = winnerEntry.oversFacedTotal + winnerOvers
                val ob = winnerEntry.oversBowledTotal + loserOvers
                val nrr = if (of > 0 && ob > 0) (rs / of) - (rc / ob) else 0.0

                client.postgrest["tournament_teams"]
                    .update({
                        set("wins", winnerEntry.wins + 1)
                        set("matches_played", winnerEntry.matchesPlayed + 1)
                        set("points", winnerEntry.points + 2)
                        set("runs_scored_total", rs)
                        set("runs_conceded_total", rc)
                        set("overs_faced_total", of)
                        set("overs_bowled_total", ob)
                        set("nrr", nrr)
                    }) { filter { eq("tournament_id", tournamentId); eq("team_id", winnerTeamId) } }
                android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: WINNER updated wins=${winnerEntry.wins + 1} pts=${winnerEntry.points + 2}")
            } else {
                android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: NO winner entry found for team=$winnerTeamId in tournament=$tournamentId")
            }

            val loserEntry = client.postgrest["tournament_teams"]
                .select { filter { eq("tournament_id", tournamentId); eq("team_id", loserTeamId) } }
                .decodeSingleOrNull<TournamentTeam>()

            android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: loserEntry=${loserEntry != null} id=${loserEntry?.id}")

            if (loserEntry != null) {
                val rs = loserEntry.runsScoreTotal + loserRuns
                val rc = loserEntry.runsConcededTotal + winnerRuns
                val of = loserEntry.oversFacedTotal + loserOvers
                val ob = loserEntry.oversBowledTotal + winnerOvers
                val nrr = if (of > 0 && ob > 0) (rs / of) - (rc / ob) else 0.0

                client.postgrest["tournament_teams"]
                    .update({
                        set("losses", loserEntry.losses + 1)
                        set("matches_played", loserEntry.matchesPlayed + 1)
                        set("runs_scored_total", rs)
                        set("runs_conceded_total", rc)
                        set("overs_faced_total", of)
                        set("overs_bowled_total", ob)
                        set("nrr", nrr)
                    }) { filter { eq("tournament_id", tournamentId); eq("team_id", loserTeamId) } }
                android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: LOSER updated losses=${loserEntry.losses + 1}")
            } else {
                android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: NO loser entry found for team=$loserTeamId in tournament=$tournamentId")
            }
            android.util.Log.e("CricketHub", "POINTS-DEBUG updatePointsTable: DONE successfully")
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Points table error: ${e.message}", e)
        }
    }

    companion object {
        fun knockoutLabel(roundsFromFinal: Int): String = when (roundsFromFinal) {
            0 -> "Final"
            1 -> "Semi Final"
            2 -> "Quarter Final"
            else -> "Round of ${1 shl (roundsFromFinal + 1)}"
        }
    }
}