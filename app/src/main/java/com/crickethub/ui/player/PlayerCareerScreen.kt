package com.crickethub.ui.player

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.PlayerRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)
private val AmberColor = Color(0xFFF59E0B)

@Composable
fun PlayerCareerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var playerName by remember { mutableStateOf("") }
    var playerRole by remember { mutableStateOf("") }
    var battingHand by remember { mutableStateOf("") }
    var bowlingStyle by remember { mutableStateOf("") }
    var jerseyNo by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf(PlayerStats()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Batting", "Bowling", "Fielding", "Career")

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val repo = PlayerRepository()
                val allPlayers = SupabaseClient.client.postgrest["players"]
                    .select()
                    .decodeList<Player>()

                val myPlayer = allPlayers.firstOrNull()
                if (myPlayer != null) {
                    playerName = myPlayer.fullName
                    playerRole = myPlayer.role?.replaceFirstChar { it.uppercase() } ?: "Player"
                    battingHand = "${myPlayer.battingHand?.replaceFirstChar { it.uppercase() } ?: "Right"} Hand Bat"
                    bowlingStyle = myPlayer.bowlingStyle ?: ""
                    jerseyNo = myPlayer.jerseyNo?.toString() ?: ""
                    stats = repo.computePlayerStats(myPlayer.id)
                }
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Career error: ${e.message}", e)
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Career Profile",
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = TextPrimary, modifier = Modifier.weight(1f)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Profile card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f))
                            .border(2.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (jerseyNo.isNotBlank()) {
                            Text(jerseyNo, color = NeonGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        playerName.ifBlank { "Your Profile" },
                        color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (playerRole.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .border(0.5.dp, NeonGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(playerRole, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (battingHand.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonBlue.copy(alpha = 0.15f))
                                    .border(0.5.dp, NeonBlue, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(battingHand, color = NeonBlue, fontSize = 12.sp)
                            }
                        }
                    }
                    if (bowlingStyle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(bowlingStyle, color = TextSecondary, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = BorderColor)
            }

            // Quick stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStatItem("Matches", stats.matches.toString())
                    QuickStatItem("Runs", stats.runs.toString())
                    QuickStatItem("Wickets", stats.wickets.toString())
                    QuickStatItem("HS", stats.highestScore.toString())
                }
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceCard,
                    contentColor = NeonGreen,
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    tab,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) NeonGreen else TextSecondary
                                )
                            }
                        )
                    }
                }
            }

            // Tab content
            item {
                when (selectedTab) {
                    0 -> BattingStatsTab(stats)
                    1 -> BowlingStatsTab(stats)
                    2 -> FieldingStatsTab(stats)
                    3 -> CareerInfoTab(stats)
                }
            }
        }
    }
}

@Composable
fun QuickStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun BattingStatsTab(stats: PlayerStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Batting Statistics", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        StatRow("Matches", stats.matches.toString())
        StatRow("Innings", stats.innings.toString())
        StatRow("Runs", stats.runs.toString())
        StatRow("Balls Faced", stats.ballsFaced.toString())
        StatRow("Highest Score", stats.highestScore.toString())
        StatRow("Average", "%.2f".format(stats.average))
        StatRow("Strike Rate", "%.2f".format(stats.strikeRate))
        StatRow("50s", stats.fifties.toString())
        StatRow("100s", stats.hundreds.toString())
        StatRow("Ducks", stats.ducks.toString())
        StatRow("Fours", stats.fours.toString())
        StatRow("Sixes", stats.sixes.toString())
        StatRow("Not Outs", stats.notOuts.toString())
        StatRow("Boundary %", "%.1f%%".format(stats.boundaryPercent))
    }
}

@Composable
fun BowlingStatsTab(stats: PlayerStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Bowling Statistics", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        StatRow("Overs", "%.1f".format(stats.oversBowled))
        StatRow("Maidens", stats.maidens.toString())
        StatRow("Runs Conceded", stats.runsConceded.toString())
        StatRow("Wickets", stats.wickets.toString())
        StatRow("Economy", "%.2f".format(stats.economy))
        StatRow("Average", "%.2f".format(stats.bowlingAverage))
        StatRow("Strike Rate", "%.2f".format(stats.bowlingStrikeRate))
        StatRow("Best Bowling", stats.bestBowling)
        StatRow("3W Hauls", stats.threeWicketHauls.toString())
        StatRow("5W Hauls", stats.fiveWicketHauls.toString())
        StatRow("Dot Balls", stats.dotBalls.toString())
        StatRow("Wides", stats.wides.toString())
        StatRow("No Balls", stats.noBalls.toString())
    }
}

@Composable
fun FieldingStatsTab(stats: PlayerStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Fielding Statistics", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        StatRow("Catches", stats.catches.toString())
        StatRow("Run Outs", stats.runOuts.toString())
        StatRow("Stumpings", stats.stumpings.toString())
        StatRow("Missed Chances", stats.missedChances.toString())
    }
}

@Composable
fun CareerInfoTab(stats: PlayerStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Career Records", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        StatRow("Highest Score", stats.highestScore.toString())
        StatRow("Best Bowling", stats.bestBowling)
        StatRow("5 Wicket Hauls", stats.fiveWicketHauls.toString())
        StatRow("Total Matches", stats.matches.toString())
        StatRow("Total Runs", stats.runs.toString())
        StatRow("Total Wickets", stats.wickets.toString())
        StatRow("Total Catches", stats.catches.toString())
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}