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

    private val repository = PlayerRepository()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadPlayers(teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val players = repository.getPlayersByTeam(teamId)
                _uiState.update { it.copy(players = players, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createPlayer(
        teamId: String,
        fullName: String,
        jerseyNo: Int?,
        role: String?,
        battingStyle: String?,
        bowlingStyle: String?
    ) {
        viewModelScope.launch {
            try {
                repository.createPlayer(
                    PlayerInsert(
                        teamId = teamId,
                        fullName = fullName,
                        jerseyNo = jerseyNo,
                        role = role,
                        battingStyle = battingStyle,
                        bowlingStyle = bowlingStyle
                    )
                )
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePlayer(teamId: String, playerId: String) {
        viewModelScope.launch {
            try {
                repository.deletePlayer(playerId)
                loadPlayers(teamId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}