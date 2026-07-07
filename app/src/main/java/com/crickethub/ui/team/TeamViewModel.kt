package com.crickethub.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.TeamRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamUiState(
    val teams: List<Team> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TeamViewModel : ViewModel() {

    private val teamRepository = TeamRepository()

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        loadTeams()
    }

    fun loadTeams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val teams = teamRepository.getAllTeams()
                _uiState.update { it.copy(teams = teams, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Error loading teams: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createTeam(
        name: String,
        shortName: String?,
        category: String,
        country: String?,
        city: String?,
        coach: String?
    ) {
        viewModelScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                teamRepository.createTeam(
                    TeamInsert(
                        name = name,
                        shortName = shortName,
                        category = category,
                        country = country,
                        city = city,
                        coach = coach,
                        createdBy = userId
                    )
                )
                loadTeams()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Error creating team: ${e.message}", e)
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
                android.util.Log.e("CricketHub", "Error deleting team: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}