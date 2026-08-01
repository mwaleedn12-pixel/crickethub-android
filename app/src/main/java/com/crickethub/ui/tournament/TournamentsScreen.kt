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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Tournament
import com.crickethub.ui.components.ShareDialog
import com.crickethub.ui.theme.*

@Composable
fun TournamentsScreen(
    onCreateTournament: () -> Unit,
    onTournamentClick: (String) -> Unit,
    viewModel: TournamentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val hdrBg  = if (isDark) Color(0xFF111111) else Color(0xFFF0ECE2)
    val textP  = if (isDark) Color(0xFFF2F2F0) else Color(0xFF2B2620)
    val textS  = if (isDark) Color(0xFFC4C9D4) else Color(0xFF566073)
    val green  = Color(0xFF34D399)

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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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

// ── 1-line professional tournament card ──────────────────────────────────────
// Single row: [🏆 Name · Format] ······ [Status] [⋮]
// Clean, compact, no multi-section bloat.
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
    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        ShareDialog(
            resourceType = "tournament",
            resourceId = tournament.id,
            resourceName = tournament.name,
            onDismiss = { showShareDialog = false }
        )
    }
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
    val surface = if (isDark) Color(0xFF161616) else Color(0xFFFFFFFF)
    val border  = if (isDark) Color(0xFF262626) else Color(0xFFE6DDC8)
    val textP   = if (isDark) Color(0xFFF2F2F0) else Color(0xFF2B2620)
    val textS   = if (isDark) Color(0xFFC4C9D4) else Color(0xFF566073)
    val gold    = Color(0xFFF59E0B)
    val isLive  = tournament.status == "active" || tournament.status == "live"

    val statusColor = when (tournament.status) {
        "active", "live" -> green
        "completed" -> greenDk
        "cancelled" -> Color(0xFFEF4444)
        else -> gold
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surface)
            .border(
                if (isLive) 1.5.dp else 1.dp,
                if (isLive) green.copy(alpha = 0.4f) else border,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Trophy icon — small circle
        Box(
            modifier = Modifier.size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gold.copy(alpha = if (isDark) 0.12f else 0.08f)),
            contentAlignment = Alignment.Center
        ) { Text("🏆", fontSize = 16.sp) }

        // Name + format on one line
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tournament.name,
                color = textP, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val details = buildList {
                    tournament.format?.let { add(it) }
                    tournament.matchType?.let { add(it) }
                    tournament.venue?.let { add(it) }
                }
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString("  ·  "),
                        color = textS, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Status badge — compact
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                .border(0.5.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                (tournament.status ?: "upcoming").replaceFirstChar { it.uppercase() },
                color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }

        // ⋮ overflow menu
        Box {
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) { Text("\u22EE", color = textS, fontSize = 16.sp) }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(if (isDark) Color(0xFF161616) else Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("📤 Share", color = green, fontSize = 13.sp) },
                    onClick = { showMenu = false; showShareDialog = true }
                )
                DropdownMenuItem(
                    text = { Text("\uD83C\uDFF3\uFE0F Cancel", color = gold, fontSize = 13.sp) },
                    onClick = { showMenu = false; onCancel() }
                )
                DropdownMenuItem(
                    text = { Text("\uD83D\uDDD1\uFE0F Delete", color = Color(0xFFEF4444), fontSize = 13.sp) },
                    onClick = { showMenu = false; showDeleteConfirm = true }
                )
            }
        }
    }
}