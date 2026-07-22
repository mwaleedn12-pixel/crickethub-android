package com.crickethub.ui.tournament

import androidx.compose.foundation.background
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Tournament
import com.crickethub.ui.theme.*

@Composable
fun TournamentsScreen(
    onCreateTournament: () -> Unit,
    onTournamentClick: (String) -> Unit,
    viewModel: TournamentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val bg     = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
    val hdrBg  = if (isDark) Color(0xFF071610) else Color(0xFFECFDF5)
    val textP  = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS  = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val green  = Color(0xFF34D399)
    val greenDk = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    LaunchedEffect(Unit) { viewModel.loadTournaments() }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(hdrBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tournaments", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textP)
                    Text("${uiState.tournaments.size} total", fontSize = 11.sp, color = textS)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(green)
                        .clickable { onCreateTournament() }.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF031A0E), modifier = Modifier.size(16.dp))
                        Text("New", fontSize = 12.sp, color = Color(0xFF031A0E), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = green)
                }
            } else if (uiState.tournaments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🏆", fontSize = 52.sp)
                        Text("No tournaments yet", color = textP, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Tap New to create one", color = textS, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(green)
                                .clickable { onCreateTournament() }.padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text("+ Create Tournament", color = Color(0xFF031A0E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.tournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            isDark = isDark,
                            onClick = { onTournamentClick(tournament.id) },
                            onCancel = { viewModel.cancelTournament(tournament.id) },
                            onDelete = { viewModel.deleteTournament(tournament.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    isDark: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete tournament?") },
            text = { Text("This permanently removes the tournament, its fixtures, and all their scoring data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Keep") }
            }
        )
    }
    val green   = Color(0xFF34D399)
    val greenDk = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val surface = if (isDark) Color(0xFF0D2018) else Color(0xFFFFFFFF)
    val border  = if (isDark) Color(0xFF1A3828) else Color(0xFFD1FAE5)
    val textP   = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS   = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val gold    = Color(0xFFF59E0B)
    val isLive  = tournament.status == "active" || tournament.status == "live"

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(if (isLive) 1.5.dp else 1.dp, if (isLive) green.copy(alpha = 0.5f) else border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        if (isLive) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, green, Color.Transparent))))
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Trophy icon
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(gold.copy(alpha = if (isDark) 0.15f else 0.1f))
                            .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🏆", fontSize = 22.sp) }

                    Column {
                        Text(tournament.name, color = textP, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(tournament.format ?: "T20", color = textS, fontSize = 11.sp)
                    }
                }
                // Status badge
                val statusColor = when (tournament.status) {
                    "active", "live" -> green
                    "completed" -> greenDk
                    else -> gold
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = if (isDark) 0.15f else 0.1f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        (tournament.status ?: "upcoming").replaceFirstChar { it.uppercase() },
                        color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
                // ⋮ overflow menu
                Box {
                    Box(
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) { Text("\u22EE", color = textS, fontSize = 18.sp) }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF0D2018) else Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("\uD83C\uDFF3\uFE0F Cancel Tournament", color = gold, fontSize = 13.sp) },
                            onClick = { showMenu = false; onCancel() }
                        )
                        DropdownMenuItem(
                            text = { Text("\uD83D\uDDD1\uFE0F Delete Tournament", color = Color(0xFFEF4444), fontSize = 13.sp) },
                            onClick = { showMenu = false; showDeleteConfirm = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    "Teams" to (tournament.maxTeams?.toString() ?: "-"),
                    "Overs" to (tournament.oversPerMatch?.toString() ?: "20"),
                    "Format" to (tournament.matchType ?: "T20")
                ).forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = textP, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(label, color = textS, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            val progress = 0f  // updated when match data available

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status", color = textS, fontSize = 11.sp)
                    Text(
                        buildString {
                            tournament.startDate?.let { append("From $it") } ?: append("No date set")
                        },
                        color = textS, fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0xFF122A1E) else Color(0xFFD1FAE5))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(green, Color(0xFF6EE7B7)))))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View button
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(greenDk.copy(alpha = if (isDark) 0.12f else 0.1f))
                    .border(1.dp, greenDk.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable { onClick() }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("View Tournament →", color = greenDk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}