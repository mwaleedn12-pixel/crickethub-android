package com.crickethub.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Match
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.PlayingXIInsert
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.MatchRepository
import com.crickethub.data.repository.TeamRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchUiState(
    val matches: List<Match> = emptyList(),
    val teams: List<Team> = emptyList(),
    val currentMatch: Match? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val matchCreated: Boolean = false
)

class MatchViewModel : ViewModel() {

    private val matchRepository = MatchRepository()
    private val teamRepository = TeamRepository()

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    init {
        loadMatches()
        loadTeams()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val matches = matchRepository.getAllMatches()
                _uiState.update { it.copy(matches = matches, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Error loading matches: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMatchById(matchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val match = matchRepository.getMatchById(matchId)
                _uiState.update { it.copy(currentMatch = match, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadTeams() {
        viewModelScope.launch {
            try {
                val teams = teamRepository.getAllTeams()
                _uiState.update { it.copy(teams = teams) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun createMatch(matchInsert: MatchInsert) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val insertWithUser = matchInsert.copy(createdBy = userId)
                val match = matchRepository.createMatch(insertWithUser)
                _uiState.update {
                    it.copy(currentMatch = match, isLoading = false, matchCreated = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Error creating match: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun recordToss(matchId: String, tossWinnerId: String, tossDecision: String) {
        viewModelScope.launch {
            try {
                val battingFirstId = if (tossDecision == "bat") {
                    tossWinnerId
                } else {
                    val match = _uiState.value.currentMatch
                    if (match?.team1Id == tossWinnerId) match.team2Id else match?.team1Id ?: tossWinnerId
                }
                val updated = matchRepository.updateToss(matchId, tossWinnerId, tossDecision, battingFirstId)
                _uiState.update { it.copy(currentMatch = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun savePlayingXI(matchId: String, teamId: String, playerIds: List<String>) {
        viewModelScope.launch {
            try {
                val playingXI = playerIds.mapIndexed { index, playerId ->
                    PlayingXIInsert(
                        matchId = matchId,
                        playerId = playerId,
                        teamId = teamId,
                        battingOrder = index + 1
                    )
                }
                matchRepository.insertPlayingXI(playingXI)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun abandonMatch(matchId: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest["matches"]
                    .update({
                        set("status", "abandoned")
                        set("result_text", "Match Abandoned")
                    }) {
                        filter { eq("id", matchId) }
                    }
                loadMatches()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun cancelMatch(matchId: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest["matches"]
                    .update({
                        set("status", "cancelled")
                        set("result_text", "Match Cancelled")
                    }) {
                        filter { eq("id", matchId) }
                    }
                loadMatches()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun resetMatchCreated() {
        _uiState.update { it.copy(matchCreated = false) }
    }
}