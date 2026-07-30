package com.crickethub.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.model.PlayerUpdate
import com.crickethub.data.model.Team
import com.crickethub.data.repository.PlayerRepository
import com.crickethub.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val players: List<Player> = emptyList(),
    val currentPlayer: Player? = null,
    val playerStats: PlayerStats = PlayerStats(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Import player flow
    val allTeams: List<Team> = emptyList(),
    val importTeamPlayers: List<Player> = emptyList()
)

class PlayerViewModel : ViewModel() {

    private val playerRepository = PlayerRepository()
    private val teamRepository = TeamRepository()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadPlayers(teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val players = playerRepository.getPlayersByTeam(teamId)
                _uiState.update { it.copy(players = players, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addPlayer(player: PlayerInsert, teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                playerRepository.createPlayer(player)
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updatePlayer(playerId: String, insert: PlayerInsert, teamId: String) {
        viewModelScope.launch {
            try {
                val update = PlayerUpdate(
                    fullName = insert.fullName,
                    nickname = insert.nickname,
                    jerseyNo = insert.jerseyNo,
                    dateOfBirth = insert.dateOfBirth,
                    gender = insert.gender,
                    country = insert.country,
                    city = insert.city,
                    battingHand = insert.battingHand,
                    bowlingHand = insert.bowlingHand,
                    bowlingStyle = insert.bowlingStyle,
                    role = insert.role,
                    availability = insert.availability
                )
                playerRepository.updatePlayer(playerId, update)
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePlayer(playerId: String, teamId: String) {
        viewModelScope.launch {
            try {
                playerRepository.deletePlayer(playerId)
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateAvailability(playerId: String, availability: String, teamId: String) {
        viewModelScope.launch {
            try {
                playerRepository.updatePlayer(
                    playerId,
                    PlayerUpdate(availability = availability)
                )
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    /** Load all teams (for the import-player team picker). */
    fun loadAllTeams() {
        viewModelScope.launch {
            try {
                val teams = teamRepository.getAllTeams()
                _uiState.update { it.copy(allTeams = teams) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load teams: ${e.message}") }
            }
        }
    }

    /** Load players for a specific team (for import selection). */
    fun loadImportTeamPlayers(teamId: String) {
        viewModelScope.launch {
            try {
                val players = playerRepository.getPlayersByTeam(teamId)
                _uiState.update { it.copy(importTeamPlayers = players) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load players: ${e.message}") }
            }
        }
    }

    /** Copy a player from another team into the current team. */
    fun importPlayer(sourcePlayer: Player, targetTeamId: String) {
        viewModelScope.launch {
            try {
                val insert = PlayerInsert(
                    teamId = targetTeamId,
                    fullName = sourcePlayer.fullName,
                    nickname = sourcePlayer.nickname,
                    jerseyNo = sourcePlayer.jerseyNo,
                    dateOfBirth = sourcePlayer.dateOfBirth,
                    gender = sourcePlayer.gender,
                    country = sourcePlayer.country,
                    city = sourcePlayer.city,
                    battingHand = sourcePlayer.battingHand,
                    bowlingHand = sourcePlayer.bowlingHand,
                    bowlingStyle = sourcePlayer.bowlingStyle,
                    role = sourcePlayer.role,
                    availability = sourcePlayer.availability
                )
                playerRepository.createPlayer(insert)
                loadPlayers(targetTeamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Import failed: ${e.message}") }
            }
        }
    }

    /** Clear import team players when closing import dialog. */
    fun clearImportPlayers() {
        _uiState.update { it.copy(importTeamPlayers = emptyList()) }
    }
}