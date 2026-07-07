package com.crickethub.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.crickethub.data.model.PlayerInsert

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)
private val AmberColor = Color(0xFFF59E0B)
private val PurpleColor = Color(0xFF8B5CF6)

@Composable
fun PlayersScreen(
    teamId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        viewModel.loadPlayers(teamId)
    }

    LaunchedEffect(showAddDialog) {
        if (!showAddDialog) {
            viewModel.loadPlayers(teamId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            "Players",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "${uiState.players.size} players",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Player",
                        tint = NeonGreen
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (uiState.players.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👤", fontSize = 48.sp)
                        Text("No players yet", color = TextSecondary, fontSize = 16.sp)
                        Text("Tap + to add players", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.players) { player ->
                        PlayerCard(
                            player = player,
                            onDelete = { viewModel.deletePlayer(player.id) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPlayerDialog(
                teamId = teamId,
                onDismiss = { showAddDialog = false },
                onAdd = { playerInsert ->
                    viewModel.addPlayer(playerInsert)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun PlayerCard(player: Player, onDelete: () -> Unit) {
    val roleColor = when (player.role) {
        "batsman" -> NeonBlue
        "bowler" -> ErrorRed
        "all_rounder" -> NeonGreen
        "wicket_keeper" -> AmberColor
        else -> TextSecondary
    }

    val availabilityColor = when (player.availability) {
        "available" -> NeonGreen
        "unavailable" -> ErrorRed
        "injured" -> AmberColor
        else -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(roleColor.copy(alpha = 0.2f))
                .border(1.dp, roleColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.jerseyNo?.toString() ?: player.fullName.take(1).uppercase(),
                color = roleColor,
                fontSize = if (player.jerseyNo != null) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    player.fullName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                player.nickname?.let {
                    Text("($it)", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                player.role?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(roleColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                            color = roleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(availabilityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        player.availability.replaceFirstChar { it.uppercase() },
                        color = availabilityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                player.battingStyle?.let {
                    Text("🏏 $it", color = TextSecondary, fontSize = 11.sp)
                }
                player.bowlingStyle?.let {
                    Text("🎳 $it", color = TextSecondary, fontSize = 11.sp)
                }
            }

            player.city?.let {
                Text("📍 $it", color = TextSecondary, fontSize = 11.sp)
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = ErrorRed.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AddPlayerDialog(
    teamId: String,
    onDismiss: () -> Unit,
    onAdd: (PlayerInsert) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var jerseyNo by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("batsman") }
    var battingHand by remember { mutableStateOf("right") }
    var bowlingHand by remember { mutableStateOf("right") }
    var selectedBowlingStyle by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf("available") }
    var showBowlingStyleDropdown by remember { mutableStateOf(false) }

    val roles = listOf(
        "batsman" to "🏏 Batsman",
        "bowler" to "🎳 Bowler",
        "all_rounder" to "⭐ All-rounder",
        "wicket_keeper" to "🧤 Wicket Keeper"
    )

    val bowlingStyles = listOf(
        "Fast", "Fast Medium", "Medium",
        "Off Spin", "Leg Spin",
        "Left Arm Orthodox", "Chinaman"
    )

    val availabilities = listOf(
        "available" to "✅ Available",
        "unavailable" to "❌ Unavailable",
        "injured" to "🤕 Injured"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(
                "Add Player",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = playerDialogFieldColors()
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = playerDialogFieldColors()
                )

                OutlinedTextField(
                    value = jerseyNo,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 3) jerseyNo = it
                    },
                    label = { Text("Jersey Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = playerDialogFieldColors()
                )

                Text("Primary Role", color = TextSecondary, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.chunked(2).forEach { rowRoles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowRoles.forEach { (value, label) ->
                                val selected = selectedRole == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) NeonGreen.copy(alpha = 0.2f)
                                            else BackgroundDark
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) NeonGreen else BorderColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedRole = value }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) NeonGreen else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold
                                        else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (rowRoles.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text("Batting Hand", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("right" to "Right", "left" to "Left").forEach { (value, label) ->
                        val selected = battingHand == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) NeonBlue.copy(alpha = 0.2f) else BackgroundDark
                                )
                                .border(
                                    1.dp,
                                    if (selected) NeonBlue else BorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { battingHand = value }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) NeonBlue else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Text("Bowling Hand", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("right" to "Right", "left" to "Left").forEach { (value, label) ->
                        val selected = bowlingHand == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) PurpleColor.copy(alpha = 0.2f)
                                    else BackgroundDark
                                )
                                .border(
                                    1.dp,
                                    if (selected) PurpleColor else BorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { bowlingHand = value }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) PurpleColor else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Box {
                    OutlinedTextField(
                        value = selectedBowlingStyle.ifEmpty { "Select bowling style" },
                        onValueChange = {},
                        label = { Text("Bowling Style") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = playerDialogFieldColors()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showBowlingStyleDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showBowlingStyleDropdown,
                        onDismissRequest = { showBowlingStyleDropdown = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        bowlingStyles.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style, color = TextPrimary) },
                                onClick = {
                                    selectedBowlingStyle = style
                                    showBowlingStyleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = playerDialogFieldColors()
                )

                Text("Availability", color = TextSecondary, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availabilities.forEach { (value, label) ->
                        val selected = availability == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) NeonGreen.copy(alpha = 0.1f) else BackgroundDark
                                )
                                .border(
                                    1.dp,
                                    if (selected) NeonGreen else BorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { availability = value }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = if (selected) NeonGreen else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onAdd(
                            PlayerInsert(
                                teamId = teamId,
                                fullName = fullName.trim(),
                                nickname = nickname.ifBlank { null },
                                jerseyNo = jerseyNo.toIntOrNull(),
                                role = selectedRole,
                                battingHand = battingHand,
                                bowlingHand = bowlingHand,
                                bowlingStyle = selectedBowlingStyle.ifBlank { null },
                                city = city.ifBlank { null },
                                availability = availability
                            )
                        )
                    }
                },
                enabled = fullName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Add Player", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun playerDialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = NeonGreen,
    unfocusedBorderColor = BorderColor,
    focusedLabelColor = NeonGreen,
    unfocusedLabelColor = TextSecondary,
    cursorColor = NeonGreen
)