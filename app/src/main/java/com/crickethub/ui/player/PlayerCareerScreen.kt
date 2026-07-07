package com.crickethub.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

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
fun PlayerCareerScreen(
    onBack: () -> Unit,
    viewModel: PlayerCareerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var showPlayerDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllPlayers()
    }

    LaunchedEffect(selectedPlayerId) {
        selectedPlayerId?.let { viewModel.loadPlayerCareer(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Career Profiles",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Player selector
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(
                        1.dp,
                        if (selectedPlayerId != null) NeonGreen else BorderColor,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { showPlayerDropdown = true }
                    .padding(16.dp)
            ) {
                Text(
                    uiState.player?.fullName ?: "Select a player",
                    color = if (selectedPlayerId != null) TextPrimary else TextSecondary,
                    fontSize = 15.sp
                )
            }

            DropdownMenu(
                expanded = showPlayerDropdown,
                onDismissRequest = { showPlayerDropdown = false },
                modifier = Modifier.background(SurfaceCard)
            ) {
                uiState.allPlayers.forEach { player ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(player.fullName, color = TextPrimary, fontSize = 14.sp)
                                player.role?.let {
                                    Text(
                                        it.replace("_", " ")
                                            .replaceFirstChar { c -> c.uppercase() },
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            selectedPlayerId = player.id
                            showPlayerDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading && selectedPlayerId != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else if (selectedPlayerId == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏏", fontSize = 48.sp)
                    Text(
                        "Select a player to view career stats",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player info card
                item {
                    uiState.player?.let { player ->
                        PlayerInfoCard(player = player)
                    }
                }

                // Batting stats
                item {
                    CareerCard(title = "🏏 Batting Career") {
                        BattingStatsGrid(stats = uiState.battingStats)
                    }
                }

                // Bowling stats — sirf tab dikhao jab bowled hai
                if (uiState.bowlingStats.wickets > 0 || uiState.bowlingStats.balls > 0) {
                    item {
                        CareerCard(title = "🎳 Bowling Career") {
                            BowlingStatsGrid(stats = uiState.bowlingStats)
                        }
                    }
                }

                // Recent form
                if (uiState.recentForm.isNotEmpty()) {
                    item {
                        CareerCard(title = "📈 Recent Form (Last ${uiState.recentForm.size} innings)") {
                            RecentFormChart(form = uiState.recentForm)
                        }
                    }
                }

                // AI Insights — Coming Soon
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🤖 AI Performance Insight", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Coming soon — AI-powered analysis of this player's strengths, weaknesses, and potential.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.1f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Coming Soon", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerInfoCard(player: com.crickethub.data.model.Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.jerseyNo?.toString() ?: player.fullName.take(1).uppercase(),
                color = NeonGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                player.fullName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            player.role?.let {
                Text(
                    it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                    color = NeonGreen,
                    fontSize = 13.sp
                )
            }
            player.battingStyle?.let {
                Text("Bat: $it", color = TextSecondary, fontSize = 12.sp)
            }
            player.bowlingStyle?.let {
                Text("Bowl: $it", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CareerCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun BattingStatsGrid(stats: CareerBattingStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox("Matches", "${stats.matches}", Modifier.weight(1f))
            StatBox("Innings", "${stats.innings}", Modifier.weight(1f))
            StatBox("Runs", "${stats.runs}", Modifier.weight(1f))
            StatBox("HS", "${stats.highScore}", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox("Avg", "${"%.2f".format(stats.average)}", Modifier.weight(1f), NeonGreen)
            StatBox("SR", "${"%.2f".format(stats.strikeRate)}", Modifier.weight(1f), NeonBlue)
            StatBox("50s", "${stats.fifties}", Modifier.weight(1f), AmberColor)
            StatBox("100s", "${stats.hundreds}", Modifier.weight(1f), PurpleColor)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox("4s", "${stats.fours}", Modifier.weight(1f), NeonBlue)
            StatBox("6s", "${stats.sixes}", Modifier.weight(1f), NeonGreen)
            StatBox("Ducks", "${stats.ducks}", Modifier.weight(1f), ErrorRed)
            StatBox("NO", "${stats.notOuts}", Modifier.weight(1f))
        }
    }
}

@Composable
fun BowlingStatsGrid(stats: CareerBowlingStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox("Wickets", "${stats.wickets}", Modifier.weight(1f), NeonGreen)
            StatBox("Best", stats.bestFigures, Modifier.weight(1f), PurpleColor)
            StatBox("Avg", "${"%.2f".format(stats.average)}", Modifier.weight(1f))
            StatBox("Eco", "${"%.2f".format(stats.economy)}", Modifier.weight(1f), AmberColor)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox("SR", "${"%.1f".format(stats.strikeRate)}", Modifier.weight(1f))
            StatBox("5W", "${stats.fiveWickets}", Modifier.weight(1f), NeonGreen)
            StatBox("Innings", "${stats.innings}", Modifier.weight(1f))
            StatBox("Balls", "${stats.balls}", Modifier.weight(1f))
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundDark)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun RecentFormChart(form: List<InningsForm>) {
    if (form.isEmpty()) return

    val maxRuns = form.maxOfOrNull { it.runs }?.coerceAtLeast(1) ?: 1

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val w = size.width
            val h = size.height
            val barWidth = (w / (form.size * 1.5f)).coerceAtMost(40f)
            val spacing = w / form.size

            form.forEachIndexed { index, innings ->
                val x = spacing * index + spacing / 2 - barWidth / 2
                val barH = (innings.runs.toFloat() / maxRuns) * (h - 20f)
                val barColor = when {
                    innings.runs >= 50 -> NeonGreen
                    innings.runs >= 30 -> AmberColor
                    innings.runs == 0  -> ErrorRed
                    else               -> NeonBlue.copy(alpha = 0.8f)
                }

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, h - 20f - barH),
                    size = Size(barWidth, barH)
                )

                if (innings.isOut) {
                    drawCircle(
                        color = ErrorRed,
                        radius = 3f,
                        center = Offset(x + barWidth / 2, h - 8f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot(color = NeonGreen, label = "50+")
            LegendDot(color = AmberColor, label = "30-49")
            LegendDot(color = NeonBlue, label = "<30")
            LegendDot(color = ErrorRed, label = "Duck/Out")
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}