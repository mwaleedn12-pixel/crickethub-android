package com.crickethub.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import com.crickethub.data.model.LEFT_HAND_BOWLING_STYLES
import com.crickethub.data.model.PLAYER_ROLES
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerInsert
import com.crickethub.data.model.RIGHT_HAND_BOWLING_STYLES


@Composable
fun PlayersScreen(
    teamId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<Player?>(null) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(teamId) { viewModel.loadPlayers(teamId) }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    "Players (${uiState.players.size})",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Player", tint = NeonGreen)
                }
            }

            if (uiState.players.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val batsmen = uiState.players.count { it.role?.lowercase() == "batsman" }
                    val bowlers = uiState.players.count { it.role?.lowercase() == "bowler" }
                    val allRounders = uiState.players.count { it.role?.lowercase() == "all-rounder" }
                    val keepers = uiState.players.count { it.role?.lowercase() == "wicket keeper" }
                    PlayerStatPill("🏏 $batsmen", "Bat")
                    PlayerStatPill("🎳 $bowlers", "Bowl")
                    PlayerStatPill("⚡ $allRounders", "AR")
                    PlayerStatPill("🧤 $keepers", "WK")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (uiState.players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                        Text("No players yet", color = TextSecondary, fontSize = 16.sp)
                        Text("Tap + to add players", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.players) { player ->
                        PlayerCard(
                            player = player,
                            onEdit = { playerToEdit = player },
                            onDelete = { playerToDelete = player }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            PlayerDialog(
                title = "Add Player",
                teamId = teamId,
                onDismiss = { showAddDialog = false },
                onConfirm = { insert ->
                    viewModel.addPlayer(insert, teamId)
                    showAddDialog = false
                }
            )
        }

        playerToEdit?.let { player ->
            PlayerDialog(
                title = "Edit Player",
                teamId = teamId,
                player = player,
                onDismiss = { playerToEdit = null },
                onConfirm = { insert ->
                    viewModel.updatePlayer(player.id, insert, teamId)
                    playerToEdit = null
                }
            )
        }

        playerToDelete?.let { player ->
            AlertDialog(
                onDismissRequest = { playerToDelete = null },
                containerColor = SurfaceCard,
                title = { Text("Remove Player", color = ErrorRed, fontWeight = FontWeight.Bold) },
                text = { Text("Remove '${player.fullName}' from team?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deletePlayer(player.id, teamId); playerToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) { Text("Remove", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { playerToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        uiState.error?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = ErrorRed
            ) { Text(it, color = Color.White) }
        }
    }
}

@Composable
fun PlayerStatPill(value: String, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun PlayerCard(
    player: Player,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val roleColor = when (player.role?.lowercase()) {
        "batsman" -> NeonBlue
        "bowler" -> ErrorRed
        "all-rounder" -> NeonGreen
        "wicket keeper" -> AmberColor
        else -> TextSecondary
    }
    val availabilityColor = when (player.availability) {
        "available" -> NeonGreen
        "unavailable" -> ErrorRed
        else -> AmberColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(roleColor.copy(alpha = 0.15f))
                .border(1.dp, roleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.jerseyNo?.toString() ?: player.fullName.first().toString(),
                color = roleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(player.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                player.nickname?.let { Text("($it)", color = TextSecondary, fontSize = 11.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(roleColor.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        player.role?.replaceFirstChar { it.uppercase() } ?: "Player",
                        color = roleColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "${player.battingHand?.take(1)?.uppercase() ?: "R"}HB",
                    color = TextSecondary, fontSize = 10.sp
                )
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(availabilityColor))
            }
            player.bowlingStyle?.let { Text(it, color = TextSecondary, fontSize = 10.sp) }
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PlayerDialog(
    title: String,
    teamId: String,
    player: Player? = null,
    onDismiss: () -> Unit,
    onConfirm: (PlayerInsert) -> Unit
) {
    var fullName by remember { mutableStateOf(player?.fullName ?: "") }
    var nickname by remember { mutableStateOf(player?.nickname ?: "") }
    var jerseyNo by remember { mutableStateOf(player?.jerseyNo?.toString() ?: "") }
    var dateOfBirth by remember { mutableStateOf(player?.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(player?.gender ?: "male") }
    var country by remember { mutableStateOf(player?.country ?: "") }
    var city by remember { mutableStateOf(player?.city ?: "") }
    var battingHand by remember { mutableStateOf(player?.battingHand ?: "right") }
    var bowlingHand by remember { mutableStateOf(player?.bowlingHand ?: "right") }
    var bowlingStyle by remember { mutableStateOf(player?.bowlingStyle ?: "") }
    var selectedRole by remember { mutableStateOf(player?.role?.replaceFirstChar { it.uppercase() } ?: "Batsman") }
    var availability by remember { mutableStateOf(player?.availability ?: "available") }

    val bowlingStyles = if (bowlingHand == "right") RIGHT_HAND_BOWLING_STYLES else LEFT_HAND_BOWLING_STYLES

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
        cursorColor = NeonGreen, focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextSecondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = fullName, onValueChange = { fullName = it },
                        label = { Text("Full Name *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nickname, onValueChange = { nickname = it },
                            label = { Text("Nickname") }, singleLine = true,
                            modifier = Modifier.weight(1f), colors = fieldColors
                        )
                        OutlinedTextField(
                            value = jerseyNo,
                            onValueChange = { if (it.all { c -> c.isDigit() }) jerseyNo = it },
                            label = { Text("Jersey #") }, singleLine = true,
                            modifier = Modifier.width(90.dp), colors = fieldColors
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = dateOfBirth, onValueChange = { dateOfBirth = it },
                        label = { Text("DOB (YYYY-MM-DD)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors
                    )
                }
                item {
                    Text("Gender", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male", "female", "other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonGreen,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = country, onValueChange = { country = it },
                            label = { Text("Country") }, singleLine = true,
                            modifier = Modifier.weight(1f), colors = fieldColors
                        )
                        OutlinedTextField(
                            value = city, onValueChange = { city = it },
                            label = { Text("City") }, singleLine = true,
                            modifier = Modifier.weight(1f), colors = fieldColors
                        )
                    }
                }
                item {
                    Text("Batting Hand", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("right", "left").forEach { hand ->
                            FilterChip(
                                selected = battingHand == hand,
                                onClick = { battingHand = hand },
                                label = { Text("${hand.replaceFirstChar { it.uppercase() }} Hand", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonBlue,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                item {
                    Text("Bowling Hand", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("right", "left").forEach { hand ->
                            FilterChip(
                                selected = bowlingHand == hand,
                                onClick = {
                                    bowlingHand = hand
                                    bowlingStyle = ""
                                },
                                label = { Text("${hand.replaceFirstChar { it.uppercase() }} Hand", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ErrorRed.copy(alpha = 0.2f),
                                    selectedLabelColor = ErrorRed,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                item {
                    Text("Bowling Style", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        bowlingStyles.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { style ->
                                    val isSelected = bowlingStyle == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ErrorRed.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (isSelected) ErrorRed else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { bowlingStyle = style }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            style,
                                            color = if (isSelected) ErrorRed else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    Text("Primary Role", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PLAYER_ROLES.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { role ->
                                    val isSelected = selectedRole == role
                                    val roleColor = when (role) {
                                        "Batsman" -> NeonBlue
                                        "Bowler" -> ErrorRed
                                        "All-rounder" -> NeonGreen
                                        "Wicket Keeper" -> AmberColor
                                        else -> TextSecondary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) roleColor.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (isSelected) roleColor else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedRole = role }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            role,
                                            color = if (isSelected) roleColor else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    Text("Availability", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("available", "unavailable", "injured").forEach { status ->
                            val color = when (status) {
                                "available" -> NeonGreen
                                "unavailable" -> ErrorRed
                                else -> AmberColor
                            }
                            FilterChip(
                                selected = availability == status,
                                onClick = { availability = status },
                                label = { Text(status.replaceFirstChar { it.uppercase() }, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
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
                        onConfirm(PlayerInsert(
                            teamId = teamId,
                            fullName = fullName.trim(),
                            nickname = nickname.trim().ifBlank { null },
                            jerseyNo = jerseyNo.toIntOrNull(),
                            dateOfBirth = dateOfBirth.trim().ifBlank { null },
                            gender = gender,
                            country = country.trim().ifBlank { null },
                            city = city.trim().ifBlank { null },
                            battingHand = battingHand,
                            bowlingHand = bowlingHand,
                            bowlingStyle = bowlingStyle.ifBlank { null },
                            role = selectedRole.lowercase(),
                            availability = availability
                        ))
                    }
                },
                enabled = fullName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}