package com.crickethub.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val players: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PlayerViewModel : ViewModel() {

    private val playerRepository = PlayerRepository()
    private var currentTeamId: String = ""

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadPlayers(teamId: String) {
        currentTeamId = teamId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val players = playerRepository.getPlayersByTeam(teamId)
                _uiState.update { it.copy(players = players, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Error loading players: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addPlayer(player: PlayerInsert) {
        currentTeamId = player.teamId
        viewModelScope.launch {
            try {
                playerRepository.createPlayer(player)
                // Player add hone ke baad reload karo
                val players = playerRepository.getPlayersByTeam(player.teamId)
                _uiState.update { it.copy(players = players, isLoading = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Dialog close hone par cancel — data save hua hoga, reload karo
                    android.util.Log.d("CricketHub", "Cancelled — reloading players")
                    try {
                        val players = playerRepository.getPlayersByTeam(player.teamId)
                        _uiState.update { it.copy(players = players, isLoading = false) }
                    } catch (ex: Exception) {
                        android.util.Log.e("CricketHub", "Reload error: ${ex.message}", ex)
                    }
                } else {
                    android.util.Log.e("CricketHub", "Error adding player: ${e.message}", e)
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            }
        }
    }

    fun deletePlayer(playerId: String) {
        viewModelScope.launch {
            try {
                playerRepository.deletePlayer(playerId)
                val players = playerRepository.getPlayersByTeam(currentTeamId)
                _uiState.update { it.copy(players = players) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Error deleting player: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}