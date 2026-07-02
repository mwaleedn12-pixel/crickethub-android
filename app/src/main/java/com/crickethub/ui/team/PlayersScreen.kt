package com.crickethub.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Player

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun PlayersScreen(
    teamId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) { viewModel.loadPlayers(teamId) }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Text("Players", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add player", tint = NeonGreen)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (uiState.players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No players yet. Tap + to add one.", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.players) { player ->
                        PlayerCard(
                            player = player,
                            onDelete = { viewModel.deletePlayer(teamId, player.id) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPlayerDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { fullName, jerseyNo, role, battingStyle ->
                    viewModel.createPlayer(teamId, fullName, jerseyNo, role, battingStyle, null)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun PlayerCard(player: Player, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(NeonBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.jerseyNo?.toString() ?: "-",
                    color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(player.fullName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                player.role?.let {
                    Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                        color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, String?, String?) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var jerseyNo by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<String?>(null) }

    val roles = listOf("batsman" to "Batsman", "bowler" to "Bowler",
        "allrounder" to "All-rounder", "wicketkeeper" to "Wicketkeeper")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Add player", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary
                    )
                )
                OutlinedTextField(
                    value = jerseyNo,
                    onValueChange = { if (it.all { c -> c.isDigit() }) jerseyNo = it },
                    label = { Text("Jersey number") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary
                    )
                )
                Text("Role", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.chunked(2).forEach { }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { (value, label) ->
                                val selected = selectedRole == value
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) NeonGreen.copy(alpha = 0.25f) else BackgroundDark)
                                        .border(1.dp, if (selected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { selectedRole = value }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(label, fontSize = 12.sp,
                                        color = if (selected) NeonGreen else TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onConfirm(fullName, jerseyNo.toIntOrNull(), selectedRole, null)
                    }
                },
                enabled = fullName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) { Text("Add", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}