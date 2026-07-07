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
import io.github.jan.supabase.auth.auth
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
    val allTeams: List<Team> = emptyList(),
    val fixtures: List<Match> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val tournamentCreated: Boolean = false,
    val fixturesGenerated: Boolean = false
)

class TournamentViewModel : ViewModel() {

    private val tournamentRepository = TournamentRepository()
    private val teamRepository = TeamRepository()

    private val _uiState = MutableStateFlow(TournamentUiState())
    val uiState: StateFlow<TournamentUiState> = _uiState.asStateFlow()

    init {
        loadTournaments()
        loadAllTeams()
    }

    fun loadTournaments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tournaments = tournamentRepository.getAllTournaments()
                _uiState.update { it.copy(tournaments = tournaments, isLoading = false) }
            } catch (e: Exception) {
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
                val fixtures = tournamentRepository.getTournamentMatches(tournamentId)

                // Team details fetch karo
                val teamIds = tournamentTeams.map { it.teamId }
                val teamDetails = _uiState.value.allTeams.filter { it.id in teamIds }

                _uiState.update {
                    it.copy(
                        currentTournament = tournament,
                        tournamentTeams = tournamentTeams,
                        teamDetails = teamDetails,
                        fixtures = fixtures,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createTournament(name: String, selectedTeamIds: List<String>, totalOvers: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                // Tournament create karo
                val tournament = tournamentRepository.createTournament(
                    TournamentInsert(name = name, createdBy = userId)
                )

                // Teams add karo
                selectedTeamIds.forEach { teamId ->
                    tournamentRepository.addTeamToTournament(tournament.id, teamId)
                }

                // Fixtures generate karo
                val selectedTeams = _uiState.value.allTeams.filter { it.id in selectedTeamIds }
                tournamentRepository.generateFixtures(
                    tournament.id, selectedTeams, totalOvers, userId
                )

                _uiState.update {
                    it.copy(
                        currentTournament = tournament,
                        isLoading = false,
                        tournamentCreated = true
                    )
                }
                loadTournaments()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Create tournament error: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun resetTournamentCreated() {
        _uiState.update { it.copy(tournamentCreated = false) }
    }
}