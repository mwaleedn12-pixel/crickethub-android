package com.crickethub.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Match

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val AmberColor = Color(0xFFF59E0B)
private val ErrorRed = Color(0xFFEF4444)
private val PurpleColor = Color(0xFF8B5CF6)

@Composable
fun MatchesScreen(
    onCreateMatch: () -> Unit,
    onMatchClick: (String) -> Unit,
    onViewScorecard: (String) -> Unit,
    onViewAnalytics: (String) -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Matches",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onCreateMatch) {
                    Icon(Icons.Default.Add, contentDescription = "Create match", tint = NeonGreen)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (uiState.matches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏏", fontSize = 48.sp)
                        Text("No matches yet", color = TextSecondary, fontSize = 16.sp)
                        Text("Tap + to create one", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.matches) { match ->
                        MatchCard(
                            match = match,
                            team1Name = uiState.teams.find { it.id == match.team1Id }?.name ?: "Team 1",
                            team2Name = uiState.teams.find { it.id == match.team2Id }?.name ?: "Team 2",
                            onClick = { onMatchClick(match.id) },
                            onViewScorecard = { onViewScorecard(match.id) },
                            onViewAnalytics = { onViewAnalytics(match.id) },
                            onAbandon = { viewModel.abandonMatch(match.id) },
                            onCancel = { viewModel.cancelMatch(match.id) }
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
    onClick: () -> Unit,
    onViewScorecard: () -> Unit,
    onViewAnalytics: () -> Unit,
    onAbandon: () -> Unit,
    onCancel: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    val statusColor = when (match.status) {
        "live" -> NeonGreen
        "completed" -> TextSecondary
        "abandoned" -> AmberColor
        "cancelled" -> ErrorRed
        else -> AmberColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.status.uppercase(),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                match.title?.let {
                    Text("• $it", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${match.matchType} • ${match.totalOvers} ov",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("⋮", color = TextSecondary, fontSize = 18.sp)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("🏳️ Abandon Match", color = AmberColor) },
                            onClick = {
                                onAbandon()
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("❌ Cancel Match", color = ErrorRed) },
                            onClick = {
                                onCancel()
                                showMoreMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                team1Name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "vs",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                team2Name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }

        match.venue?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text("📍 $it", color = TextSecondary, fontSize = 12.sp)
        }

        match.resultText?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Score", fontSize = 11.sp) }

            OutlinedButton(
                onClick = onViewScorecard,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Live", fontSize = 11.sp) }

            OutlinedButton(
                onClick = onViewAnalytics,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleColor),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Analytics", fontSize = 11.sp) }
        }
    }
}