package com.crickethub.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Match
import com.crickethub.data.model.Team
import com.crickethub.data.model.Tournament
import com.crickethub.data.model.TournamentInsert
import com.crickethub.data.model.TournamentTeam
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.TeamRepository
import com.crickethub.data.repository.TournamentRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TournamentUiState(
    val tournaments: List<Tournament> = emptyList(),
    val currentTournament: Tournament? = null,
    val tournamentTeams: List<TournamentTeam> = emptyList(),
    val teamDetails: List<Team> = emptyList(),
    val fixtures: List<Match> = emptyList(),
    val allTeams: List<Team> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TournamentViewModel : ViewModel() {

    private val tournamentRepository = TournamentRepository()
    private val teamRepository = TeamRepository()

    private val _uiState = MutableStateFlow(TournamentUiState())
    val uiState: StateFlow<TournamentUiState> = _uiState.asStateFlow()

    init {
        loadTournaments()
    }

    fun loadTournaments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tournaments = tournamentRepository.getAllTournaments()
                _uiState.update { it.copy(tournaments = tournaments, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Load tournaments error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadAllTeams() {
        viewModelScope.launch {
            try {
                val teams = teamRepository.getAllTeams()
                _uiState.update { it.copy(allTeams = teams) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadTournamentDetail(tournamentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tournament = tournamentRepository.getTournamentById(tournamentId)
                val tournamentTeams = tournamentRepository.getTournamentTeams(tournamentId)
                val fixtures = tournamentRepository.getTournamentFixtures(tournamentId)
                val teamIds = tournamentTeams.map { it.teamId }

                // Parallel load team details — much faster
                val teamDetails = coroutineScope {
                    teamIds.map { teamId ->
                        async {
                            try {
                                SupabaseClient.client.postgrest["teams"]
                                    .select { filter { eq("id", teamId) } }
                                    .decodeSingleOrNull<Team>()
                            } catch (e: Exception) { null }
                        }
                    }.awaitAll().filterNotNull()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentTournament = tournament,
                        tournamentTeams = tournamentTeams,
                        teamDetails = teamDetails,
                        fixtures = fixtures.sortedBy { m -> m.matchNumber ?: 0 }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Tournament detail error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createTournament(tournament: TournamentInsert) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val created = tournamentRepository.createTournament(tournament)
                _uiState.update { it.copy(currentTournament = created, isLoading = false) }
                loadTournaments()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Create tournament error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addTeamToTournament(tournamentId: String, teamId: String) {
        viewModelScope.launch {
            try {
                tournamentRepository.addTeamToTournament(tournamentId, teamId)
                loadTournamentDetail(tournamentId)
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Add team error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeTeamFromTournament(tournamentId: String, teamId: String) {
        viewModelScope.launch {
            try {
                tournamentRepository.removeTeamFromTournament(tournamentId, teamId)
                loadTournamentDetail(tournamentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun generateFixtures(tournamentId: String, format: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                tournamentRepository.generateFixtures(tournamentId, format)
                loadTournamentDetail(tournamentId)
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Generate fixtures error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateTournamentStatus(tournamentId: String, status: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest["tournaments"]
                    .update({ set("status", status) }) {
                        filter { eq("id", tournamentId) }
                    }
                loadTournamentDetail(tournamentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTournament(tournamentId: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest["tournaments"]
                    .delete { filter { eq("id", tournamentId) } }
                loadTournaments()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}