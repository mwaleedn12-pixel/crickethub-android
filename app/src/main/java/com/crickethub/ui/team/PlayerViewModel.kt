package com.crickethub.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.repository.PlayerRepository
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
    val showAddDialog: Boolean = false
)

class PlayerViewModel : ViewModel() {

    private val playerRepository = PlayerRepository()

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

    fun loadPlayerDetails(playerId: String) {
        viewModelScope.launch {
            try {
                val player = playerRepository.getPlayerById(playerId)
                val stats = playerRepository.computePlayerStats(playerId)
                _uiState.update { it.copy(currentPlayer = player, playerStats = stats) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addPlayer(player: PlayerInsert, teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                playerRepository.createPlayer(player)
                loadPlayers(teamId)
                _uiState.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updatePlayer(playerId: String, updates: Map<String, Any?>, teamId: String) {
        viewModelScope.launch {
            try {
                playerRepository.updatePlayer(playerId, updates)
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
                playerRepository.updatePlayer(playerId, mapOf("availability" to availability))
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}