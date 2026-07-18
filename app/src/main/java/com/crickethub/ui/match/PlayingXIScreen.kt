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
import com.crickethub.ui.theme.*


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
    var captainId by remember { mutableStateOf<String?>(null) }
    var keeperId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        try {
            val repo = PlayerRepository()
            val cached = repo.getPlayersByTeam(teamId)
            players = cached
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    val selectedCount = selectedIds.size
    val canSave = selectedCount == playersPerSide && captainId != null && keeperId != null

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

                    val isCaptain = captainId == player.id
                    val isKeeper = keeperId == player.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isCaptain -> AmberColor.copy(alpha = 0.12f)
                                    isKeeper -> NeonBlue.copy(alpha = 0.12f)
                                    isSelected -> NeonGreen.copy(alpha = 0.1f)
                                    else -> SurfaceCard
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isCaptain -> AmberColor
                                    isKeeper -> NeonBlue
                                    isSelected -> NeonGreen
                                    else -> BorderColor
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = canSelect) {
                                selectedIds = if (isSelected) {
                                    // Deselect - also remove captain/keeper
                                    if (captainId == player.id) captainId = null
                                    if (keeperId == player.id) keeperId = null
                                    selectedIds - player.id
                                } else {
                                    selectedIds + player.id
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCaptain -> AmberColor.copy(alpha = 0.3f)
                                            isKeeper -> NeonBlue.copy(alpha = 0.3f)
                                            isSelected -> NeonGreen.copy(alpha = 0.3f)
                                            else -> NeonBlue.copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    player.jerseyNo?.toString() ?: "-",
                                    color = when {
                                        isCaptain -> AmberColor
                                        isKeeper -> NeonBlue
                                        isSelected -> NeonGreen
                                        else -> NeonBlue
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        player.fullName,
                                        color = TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 15.sp
                                    )
                                    if (isCaptain) {
                                        Text("C", color = AmberColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(AmberColor.copy(0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                    if (isKeeper) {
                                        Text("WK", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(NeonBlue.copy(0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                player.role?.let {
                                    Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                                        color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        // C and WK buttons (only when selected)
                        if (isSelected) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Captain button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isCaptain) AmberColor else AmberColor.copy(0.15f))
                                        .clickable {
                                            captainId = if (isCaptain) null else player.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("C", color = if (isCaptain) Color.Black else AmberColor,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                // WK button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isKeeper) NeonBlue else NeonBlue.copy(0.15f))
                                        .clickable {
                                            keeperId = if (isKeeper) null else player.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("WK", color = if (isKeeper) Color.Black else NeonBlue,
                                        fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
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
                                    battingOrder = index + 1,
                                    isCaptain = playerId == captainId,
                                    isWicketKeeper = playerId == keeperId
                                )
                            }
                            repo.insertPlayingXI(playingXI)
                            saveSuccess = true
                            onXISaved()
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
                        else when {
                            selectedCount < playersPerSide -> "Select ${playersPerSide - selectedCount} more"
                            captainId == null -> "Select Captain (C)"
                            keeperId == null -> "Select Wicket Keeper (WK)"
                            else -> "Confirm XI"
                        },
                        color = if (canSave) Color.Black else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}