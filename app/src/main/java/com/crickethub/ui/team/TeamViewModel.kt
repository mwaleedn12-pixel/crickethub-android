package com.crickethub.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.model.TeamStats
import com.crickethub.data.repository.PlayerRepository
import com.crickethub.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamUiState(
    val teams: List<Team> = emptyList(),
    val currentTeam: Team? = null,
    val teamPlayers: List<Player> = emptyList(),
    val teamStats: TeamStats = TeamStats(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false
)

class TeamViewModel : ViewModel() {

    private val teamRepository = TeamRepository()
    private val playerRepository = PlayerRepository()

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init { loadTeams() }

    fun loadTeams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val teams = teamRepository.getAllTeams()
                _uiState.update { it.copy(teams = teams, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadTeamDetails(teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val team = teamRepository.getTeamById(teamId)
                val players = playerRepository.getPlayersByTeam(teamId)
                _uiState.update {
                    it.copy(
                        currentTeam = team,
                        teamPlayers = players,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createTeam(team: TeamInsert) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                teamRepository.createTeam(team)
                loadTeams()
                _uiState.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateTeam(teamId: String, teamInsert: TeamInsert) {
        viewModelScope.launch {
            try {
                teamRepository.updateTeam(
                    teamId = teamId,
                    name = teamInsert.name,
                    shortName = teamInsert.shortName,
                    jerseyColor = teamInsert.jerseyColor,
                    category = teamInsert.category,
                    country = teamInsert.country,
                    city = teamInsert.city,
                    homeGround = teamInsert.homeGround,
                    coach = teamInsert.coach
                )
                loadTeams()
                _uiState.update { it.copy(showEditDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            try {
                teamRepository.deleteTeam(teamId)
                loadTeams()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setCaptain(teamId: String, playerId: String) {
        viewModelScope.launch {
            try {
                teamRepository.updateTeam(
                    teamId = teamId,
                    name = _uiState.value.currentTeam?.name ?: return@launch,
                    shortName = null,
                    jerseyColor = null,
                    category = null,
                    country = null,
                    city = null,
                    homeGround = null,
                    coach = null
                )
                loadTeamDetails(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }
    fun showEditDialog() { _uiState.update { it.copy(showEditDialog = true) } }
    fun hideEditDialog() { _uiState.update { it.copy(showEditDialog = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}