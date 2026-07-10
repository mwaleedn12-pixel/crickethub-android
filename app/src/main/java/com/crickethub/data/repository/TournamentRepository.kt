package com.crickethub.data.repository

import com.crickethub.data.model.Match
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.Team
import com.crickethub.data.model.Tournament
import com.crickethub.data.model.TournamentInsert
import com.crickethub.data.model.TournamentTeam
import com.crickethub.data.model.TournamentTeamInsert
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class TournamentRepository {

    suspend fun getAllTournaments(): List<Tournament> {
        return SupabaseClient.client.postgrest["tournaments"]
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<Tournament>()
    }

    suspend fun getTournamentById(id: String): Tournament? {
        return SupabaseClient.client.postgrest["tournaments"]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Tournament>()
    }

    suspend fun createTournament(tournament: TournamentInsert): Tournament {
        return SupabaseClient.client.postgrest["tournaments"]
            .insert(tournament) { select() }
            .decodeSingle<Tournament>()
    }

    suspend fun addTeamToTournament(tournamentId: String, teamId: String): TournamentTeam {
        return SupabaseClient.client.postgrest["tournament_teams"]
            .insert(TournamentTeamInsert(tournamentId, teamId)) { select() }
            .decodeSingle<TournamentTeam>()
    }

    suspend fun getTournamentTeams(tournamentId: String): List<TournamentTeam> {
        return SupabaseClient.client.postgrest["tournament_teams"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList<TournamentTeam>()
    }

    suspend fun getTournamentMatches(tournamentId: String): List<Match> {
        return SupabaseClient.client.postgrest["matches"]
            .select {
                filter { eq("tournament_id", tournamentId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Match>()
    }

    suspend fun generateFixtures(
        tournamentId: String,
        teams: List<Team>,
        totalOvers: Int,
        createdBy: String
    ): List<Match> {
        val matches = mutableListOf<MatchInsert>()

        // Round robin — har team har doosri team se ek baar khele
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                matches.add(
                    MatchInsert(
                        team1Id = teams[i].id,
                        team2Id = teams[j].id,
                        totalOvers = totalOvers,
                        createdBy = createdBy,
                    )
                )
            }
        }

        val createdMatches = mutableListOf<Match>()
        matches.forEach { matchInsert ->
            val match = SupabaseClient.client.postgrest["matches"]
                .insert(matchInsert) { select() }
                .decodeSingle<Match>()

            // Tournament ID link karo
            SupabaseClient.client.postgrest["matches"]
                .update({ set("tournament_id", tournamentId) }) {
                    filter { eq("id", match.id) }
                }

            createdMatches.add(match)
        }

        return createdMatches
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
        // Winner update
        val winnerEntry = SupabaseClient.client.postgrest["tournament_teams"]
            .select {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("team_id", winnerTeamId)
                }
            }
            .decodeSingleOrNull<TournamentTeam>()

        if (winnerEntry != null) {
            SupabaseClient.client.postgrest["tournament_teams"]
                .update({
                    set("points", winnerEntry.points + 2)
                    set("wins", winnerEntry.wins + 1)
                    set("matches_played", winnerEntry.matchesPlayed + 1)
                    set("runs_scored", winnerEntry.runsScored + winnerRuns)
                    set("runs_conceded", winnerEntry.runsConceded + loserRuns)
                    set("overs_faced", winnerEntry.oversFaced + winnerOvers)
                    set("overs_bowled", winnerEntry.oversBowled + loserOvers)
                }) {
                    filter {
                        eq("tournament_id", tournamentId)
                        eq("team_id", winnerTeamId)
                    }
                }
        }

        // Loser update
        val loserEntry = SupabaseClient.client.postgrest["tournament_teams"]
            .select {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("team_id", loserTeamId)
                }
            }
            .decodeSingleOrNull<TournamentTeam>()

        if (loserEntry != null) {
            SupabaseClient.client.postgrest["tournament_teams"]
                .update({
                    set("losses", loserEntry.losses + 1)
                    set("matches_played", loserEntry.matchesPlayed + 1)
                    set("runs_scored", loserEntry.runsScored + loserRuns)
                    set("runs_conceded", loserEntry.runsConceded + winnerRuns)
                    set("overs_faced", loserEntry.oversFaced + loserOvers)
                    set("overs_bowled", loserEntry.oversBowled + winnerOvers)
                }) {
                    filter {
                        eq("tournament_id", tournamentId)
                        eq("team_id", loserTeamId)
                    }
                }
        }
    }
}