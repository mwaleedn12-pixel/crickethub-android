package com.crickethub.ui.match

import androidx.compose.foundation.background
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Score
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
import com.crickethub.data.model.Match
import com.crickethub.ui.theme.*
import com.crickethub.ui.components.ShareDialog

@Composable
fun MatchesScreen(
    onCreateMatch: () -> Unit,
    onMatchClick: (String) -> Unit,
    onViewScorecard: (String) -> Unit,
    onViewAnalytics: (String) -> Unit,
    onDLSCalculator: () -> Unit = {},
    viewModel: MatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val bg      = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
    val surface = if (isDark) Color(0xFF0D2018) else Color(0xFFFFFFFF)
    val border  = if (isDark) Color(0xFF1A3828) else Color(0xFFBBF7D0)
    val textP   = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS   = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val green   = Color(0xFF34D399)
    val greenDk = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Live", "Upcoming", "Completed")

    val filteredMatches = when (selectedFilter) {
        "Live"      -> uiState.matches.filter { it.status == "live" }
        "Upcoming"  -> uiState.matches.filter { it.status == "upcoming" || it.status == "scheduled" }
        "Completed" -> uiState.matches.filter { it.status == "completed" }
        else        -> uiState.matches
    }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color(0xFF071610) else Color(0xFFECFDF5)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Matches",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textP
                    )
                    Text(
                        "${uiState.matches.size} total",
                        fontSize = 11.sp, color = textS
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // DLS Calculator button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDark) Color(0xFF1A3828) else Color(0xFFD1FAE5)
                            )
                            .clickable { onDLSCalculator() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("🌧️ DLS", fontSize = 12.sp, color = greenDk, fontWeight = FontWeight.SemiBold)
                    }
                    // New match button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(green)
                            .clickable { onCreateMatch() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF031A0E), modifier = Modifier.size(16.dp))
                            Text("New", fontSize = 12.sp, color = Color(0xFF031A0E), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Filter chips ─────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val selected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) green else if (isDark) Color(0xFF0D2018) else Color.White)
                            .border(1.dp, if (selected) green else border, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            filter,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color(0xFF031A0E) else textS
                        )
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = green)
                }
            } else if (filteredMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🏏", fontSize = 52.sp)
                        Text("No ${if (selectedFilter == "All") "" else selectedFilter.lowercase()} matches", color = textP, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Tap New to create one", color = textS, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(green)
                                .clickable { onCreateMatch() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text("+ Create Match", color = Color(0xFF031A0E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMatches) { match ->
                        MatchCard(
                            match = match,
                            team1Name = uiState.teams.find { it.id == match.team1Id }?.name ?: "Team 1",
                            team2Name = uiState.teams.find { it.id == match.team2Id }?.name ?: "Team 2",
                            isDark = isDark,
                            onClick = { onMatchClick(match.id) },
                            onViewScorecard = { onViewScorecard(match.id) },
                            onViewAnalytics = { onViewAnalytics(match.id) },
                            onAbandon = { viewModel.abandonMatch(match.id) },
                            onCancel = { viewModel.cancelMatch(match.id) },
                            onDelete = { viewModel.deleteMatch(match.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    team1Name: String,
    team2Name: String,
    isDark: Boolean,
    onClick: () -> Unit,
    onViewScorecard: () -> Unit,
    onViewAnalytics: () -> Unit,
    onAbandon: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val green   = Color(0xFF34D399)
    val greenDk = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val surface = if (isDark) Color(0xFF0D2018) else Color(0xFFFFFFFF)
    val border  = if (isDark) Color(0xFF1A3828) else Color(0xFFD1FAE5)
    val textP   = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS   = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)

    val isLive      = match.status == "live"
    val isCompleted = match.status == "completed"
    val isAbandoned = match.status == "abandoned"

    val statusColor = when (match.status) {
        "live"      -> Color(0xFFEF4444)
        "completed" -> greenDk
        "abandoned" -> Color(0xFFF59E0B)
        "cancelled" -> Color(0xFFEF4444)
        else        -> Color(0xFFF59E0B)
    }

    val cardBorder = if (isLive) green.copy(alpha = 0.5f) else border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(
                width = if (isLive) 1.5.dp else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        // Live glow bar at top
        if (isLive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, green, Color.Transparent))
                    )
            )
        }

        Column(modifier = Modifier.padding(14.dp)) {

            // ── Status row ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = if (isDark) 0.15f else 0.1f))
                            .border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isLive) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                            }
                            Text(
                                match.status.uppercase(),
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    match.title?.let {
                        Text("• $it", color = textS, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${match.matchType.uppercase()} • ${match.totalOvers} ov",
                        color = textS, fontSize = 11.sp
                    )
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF122A1E) else Color(0xFFE8FDF4))
                                .clickable { showMoreMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⋮", color = textS, fontSize = 16.sp)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF0D2018) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("🏳️ Abandon Match", color = Color(0xFFF59E0B), fontSize = 13.sp) },
                                onClick = { onAbandon(); showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("❌ Cancel Match", color = Color(0xFFEF4444), fontSize = 13.sp) },
                                onClick = { onCancel(); showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("🗑️ Delete Match", color = Color(0xFFEF4444), fontSize = 13.sp) },
                                onClick = { showMoreMenu = false; showDeleteConfirm = true }
                            )
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete match?") },
                                text = { Text("This permanently removes the match and all its scoring data. This cannot be undone.") },
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Teams row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team 1
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(green.copy(alpha = 0.3f), green.copy(alpha = 0.1f))
                                )
                            )
                            .border(1.dp, green.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            team1Name.take(2).uppercase(),
                            color = greenDk, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(team1Name, color = textP, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                // VS divider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(border))
                    Text("vs", color = textS, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(border))
                }

                // Team 2
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF60A5FA).copy(alpha = 0.3f), Color(0xFF60A5FA).copy(alpha = 0.1f))
                                )
                            )
                            .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            team2Name.take(2).uppercase(),
                            color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(team2Name, color = textP, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.End)
                }
            }

            // ── Venue & Result ────────────────────────────────────────
            match.venue?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("📍 $it", color = textS, fontSize = 11.sp)
            }

            match.resultText?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(greenDk.copy(alpha = if (isDark) 0.1f else 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🏆 $it", color = greenDk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Progress bar (for live) ───────────────────────────────
            if (isLive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color(0xFF122A1E) else Color(0xFFD1FAE5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(green, Color(0xFF6EE7B7))))
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Action buttons ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Score / Continue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(green)
                        .clickable { onClick() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLive) "▶ Score" else if (isCompleted) "View" else "Start",
                        color = Color(0xFF031A0E), fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }

                // Scorecard
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF60A5FA).copy(alpha = if (isDark) 0.15f else 0.12f))
                        .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onViewScorecard() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊 Live", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Analytics
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.15f else 0.12f))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onViewAnalytics() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📈 Stats", color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}