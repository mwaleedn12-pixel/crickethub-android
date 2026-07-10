package com.crickethub.data.repository

import com.crickethub.data.model.Match
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.Tournament
import com.crickethub.data.model.TournamentInsert
import com.crickethub.data.model.TournamentTeam
import com.crickethub.data.model.TournamentTeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class TournamentRepository {

    private val client = SupabaseClient.client

    suspend fun getAllTournaments(): List<Tournament> {
        return client.postgrest["tournaments"]
            .select()
            .decodeList()
    }

    suspend fun getTournamentById(tournamentId: String): Tournament? {
        return client.postgrest["tournaments"]
            .select { filter { eq("id", tournamentId) } }
            .decodeSingleOrNull()
    }

    suspend fun createTournament(tournament: TournamentInsert): Tournament {
        return client.postgrest["tournaments"]
            .insert(tournament) { select() }
            .decodeSingle()
    }

    suspend fun getTournamentTeams(tournamentId: String): List<TournamentTeam> {
        return client.postgrest["tournament_teams"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList()
    }

    suspend fun getTournamentFixtures(tournamentId: String): List<Match> {
        return client.postgrest["matches"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList()
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

    suspend fun removeTeamFromTournament(tournamentId: String, teamId: String) {
        client.postgrest["tournament_teams"]
            .delete {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("team_id", teamId)
                }
            }
    }

    suspend fun generateFixtures(tournamentId: String, format: String): List<Match> {
        val teams = getTournamentTeams(tournamentId)
        val teamIds = teams.map { it.teamId }
        if (teamIds.size < 2) return emptyList()

        val pairs = mutableListOf<Pair<String, String>>()

        when (format) {
            "League", "Round Robin" -> {
                for (i in teamIds.indices) {
                    for (j in i + 1 until teamIds.size) {
                        pairs.add(Pair(teamIds[i], teamIds[j]))
                    }
                }
            }
            "Double Round Robin" -> {
                for (round in 1..2) {
                    for (i in teamIds.indices) {
                        for (j in i + 1 until teamIds.size) {
                            pairs.add(Pair(teamIds[i], teamIds[j]))
                        }
                    }
                }
            }
            "Knockout" -> {
                for (i in teamIds.indices step 2) {
                    if (i + 1 < teamIds.size) {
                        pairs.add(Pair(teamIds[i], teamIds[i + 1]))
                    }
                }
            }
            "Group + Knockout", "Hybrid" -> {
                for (i in teamIds.indices) {
                    for (j in i + 1 until teamIds.size) {
                        pairs.add(Pair(teamIds[i], teamIds[j]))
                    }
                }
            }
        }

        pairs.forEachIndexed { index, (team1Id, team2Id) ->
            try {
                client.postgrest["matches"]
                    .insert(
                        MatchInsert(
                            team1Id = team1Id,
                            team2Id = team2Id,
                            matchType = "T20",
                            totalOvers = 20,
                            playersPerSide = 11,
                            tournamentId = tournamentId,
                            matchNumber = index + 1,
                            powerplayOvers = 6,
                            freeHitOnNoball = true,
                            superOverEnabled = false,
                            maxOversPerBowler = 4,
                            isPublic = true,
                            inningsBreakMinutes = 20
                        )
                    )
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Fixture create error: ${e.message}", e)
            }
        }

        return getTournamentFixtures(tournamentId)
    }

    suspend fun updatePointsTable(
        tournamentId: String,
        winnerTeamId: String,
        loserTeamId: String,
        winnerRuns: Int,
        loserRuns: Int,
        winnerOvers: Double,
        loserOvers: Double
    ) {
        try {
            val winnerEntry = client.postgrest["tournament_teams"]
                .select {
                    filter {
                        eq("tournament_id", tournamentId)
                        eq("team_id", winnerTeamId)
                    }
                }
                .decodeSingleOrNull<TournamentTeam>()

            if (winnerEntry != null) {
                val newRunsScored = winnerEntry.runsScoreTotal + winnerRuns
                val newRunsConceded = winnerEntry.runsConcededTotal + loserRuns
                val newOversFaced = winnerEntry.oversFacedTotal + winnerOvers
                val newOversBowled = winnerEntry.oversBowledTotal + loserOvers
                val nrr = if (newOversFaced > 0 && newOversBowled > 0)
                    (newRunsScored / newOversFaced) - (newRunsConceded / newOversBowled)
                else 0.0

                client.postgrest["tournament_teams"]
                    .update({
                        set("wins", winnerEntry.wins + 1)
                        set("matches_played", winnerEntry.matchesPlayed + 1)
                        set("points", winnerEntry.points + 2)
                        set("runs_scored_total", newRunsScored)
                        set("runs_conceded_total", newRunsConceded)
                        set("overs_faced_total", newOversFaced)
                        set("overs_bowled_total", newOversBowled)
                        set("nrr", nrr)
                    }) {
                        filter {
                            eq("tournament_id", tournamentId)
                            eq("team_id", winnerTeamId)
                        }
                    }
            }

            val loserEntry = client.postgrest["tournament_teams"]
                .select {
                    filter {
                        eq("tournament_id", tournamentId)
                        eq("team_id", loserTeamId)
                    }
                }
                .decodeSingleOrNull<TournamentTeam>()

            if (loserEntry != null) {
                val newRunsScored = loserEntry.runsScoreTotal + loserRuns
                val newRunsConceded = loserEntry.runsConcededTotal + winnerRuns
                val newOversFaced = loserEntry.oversFacedTotal + loserOvers
                val newOversBowled = loserEntry.oversBowledTotal + winnerOvers
                val nrr = if (newOversFaced > 0 && newOversBowled > 0)
                    (newRunsScored / newOversFaced) - (newRunsConceded / newOversBowled)
                else 0.0

                client.postgrest["tournament_teams"]
                    .update({
                        set("losses", loserEntry.losses + 1)
                        set("matches_played", loserEntry.matchesPlayed + 1)
                        set("runs_scored_total", newRunsScored)
                        set("runs_conceded_total", newRunsConceded)
                        set("overs_faced_total", newOversFaced)
                        set("overs_bowled_total", newOversBowled)
                        set("nrr", nrr)
                    }) {
                        filter {
                            eq("tournament_id", tournamentId)
                            eq("team_id", loserTeamId)
                        }
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Points table error: ${e.message}", e)
        }
    }
}