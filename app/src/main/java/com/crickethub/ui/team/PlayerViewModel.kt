package com.crickethub.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.model.PlayerUpdate
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
    val error: String? = null
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
}