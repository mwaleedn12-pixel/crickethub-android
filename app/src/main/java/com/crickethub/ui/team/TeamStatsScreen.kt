package com.crickethub.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.repository.TeamStats
import com.crickethub.data.repository.TeamStatsRepository
import com.crickethub.ui.components.CricketAnimatedBackground
import com.crickethub.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TeamStatsScreen(
    teamId: String,
    teamName: String,
    onBack: () -> Unit
) {
    val repo = remember { TeamStatsRepository() }
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<TeamStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(teamId) {
        scope.launch {
            try {
                stats = repo.getTeamStats(teamId)
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "TeamStats error: ${e.message}")
            }
            isLoading = false
        }
    }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                }
                Column {
                    Text(teamName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Team Statistics", color = TextSecondary, fontSize = 12.sp)
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (stats == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Could not load stats", color = TextSecondary)
                }
            } else {
                val s = stats!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Match Results Card
                    item {
                        StatsCard(title = "Match Results") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ResultCircle("Played", s.matchesPlayed.toString(), NeonGreen)
                                ResultCircle("Won", s.won.toString(), Color(0xFF22C55E))
                                ResultCircle("Lost", s.lost.toString(), ErrorRed)
                                ResultCircle("Tied", s.tied.toString(), AmberColor)
                                ResultCircle("NR", s.noResult.toString(), TextSecondary)
                            }
                        }
                    }

                    // Win Rate
                    item {
                        StatsCard(title = "Win Rate") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${"%.1f".format(s.winPercentage)}%",
                                    color = NeonGreen,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (s.winPercentage / 100).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = NeonGreen,
                                    trackColor = BorderColor
                                )
                            }
                        }
                    }

                    // Batting Stats
                    item {
                        StatsCard(title = "🏏 Batting") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatRow("Total Runs", s.totalRuns.toString(), NeonGreen)
                                StatRow("Total Wickets Lost", s.totalWickets.toString(), ErrorRed)
                                StatRow("Highest Score", s.highestScore.toString(), Color(0xFF22C55E))
                                StatRow("Lowest Score", if (s.lowestScore == 0 && s.matchesPlayed == 0) "—" else s.lowestScore.toString(), AmberColor)
                            }
                        }
                    }

                    // Streak
                    item {
                        StatsCard(title = "🔥 Win Streaks") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StreakBox("Current\nStreak", s.currentWinStreak, NeonGreen)
                                StreakBox("Longest\nStreak", s.longestWinStreak, AmberColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        HorizontalDivider(color = BorderColor)
        content()
    }
}

@Composable
fun ResultCircle(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StreakBox(label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.1f))
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text(value.toString(), color = color, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}