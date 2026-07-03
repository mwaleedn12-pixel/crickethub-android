package com.crickethub.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Player
import com.crickethub.data.repository.PlayerRepository
import kotlinx.coroutines.launch

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)

@Composable
fun PlayingXIScreen(
    matchId: String,
    teamId: String,
    teamName: String,
    playersPerSide: Int = 11,
    onBack: () -> Unit,
    onXISaved: () -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var players by remember { mutableStateOf<List<Player>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        try {
            val repo = PlayerRepository()
            players = repo.getPlayersByTeam(teamId)
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onXISaved()
    }

    val selectedCount = selectedIds.size
    val canSave = selectedCount == playersPerSide

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        "Playing $playersPerSide",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(teamName, fontSize = 13.sp, color = TextSecondary)
                }
            }
            Text(
                "$selectedCount/$playersPerSide",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (canSave) NeonGreen else NeonBlue
            )
        }

        if (error != null) {
            Text(
                error ?: "",
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else if (players.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No players in this team.\nAdd players first.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(players) { player ->
                    val isSelected = player.id in selectedIds
                    val canSelect = isSelected || selectedCount < playersPerSide

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NeonGreen.copy(alpha = 0.1f) else SurfaceCard
                            )
                            .border(
                                1.dp,
                                if (isSelected) NeonGreen else BorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = canSelect) {
                                selectedIds = if (isSelected) {
                                    selectedIds - player.id
                                } else {
                                    selectedIds + player.id
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) NeonGreen.copy(alpha = 0.3f)
                                        else NeonBlue.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    player.jerseyNo?.toString() ?: "-",
                                    color = if (isSelected) NeonGreen else NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    player.fullName,
                                    color = TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold
                                    else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                                player.role?.let {
                                    Text(
                                        it.replace("_", " ")
                                            .replaceFirstChar { c -> c.uppercase() },
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val repo = com.crickethub.data.repository.MatchRepository()
                            val playingXI = selectedIds.toList().mapIndexed { index, playerId ->
                                com.crickethub.data.model.PlayingXIInsert(
                                    matchId = matchId,
                                    playerId = playerId,
                                    teamId = teamId,
                                    battingOrder = index + 1
                                )
                            }
                            repo.insertPlayingXI(playingXI)
                            saveSuccess = true
                        } catch (e: Exception) {
                            error = "Save failed: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = canSave && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (canSave) "Save Playing $playersPerSide"
                        else "Select ${playersPerSide - selectedCount} more",
                        color = if (canSave) Color.Black else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}