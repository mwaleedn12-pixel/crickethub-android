package com.crickethub.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.PlayerRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.crickethub.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── MAIN SCREEN ──────────────────────────────────────────────

@Composable
fun PlayerComparisonScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var allPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var allTeams by remember { mutableStateOf<List<Team>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var player1 by remember { mutableStateOf<Player?>(null) }
    var player2 by remember { mutableStateOf<Player?>(null) }
    var stats1 by remember { mutableStateOf<PlayerStats?>(null) }
    var stats2 by remember { mutableStateOf<PlayerStats?>(null) }
    var statsLoading by remember { mutableStateOf(false) }

    var pickingSlot by remember { mutableIntStateOf(0) } // 0=none, 1=left, 2=right
    var searchQuery by remember { mutableStateOf("") }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Batting", "Bowling", "Fielding", "Radar")

    // Load players list
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allPlayers = SupabaseClient.client.postgrest["players"]
                    .select().decodeList()
                allTeams = SupabaseClient.client.postgrest["teams"]
                    .select().decodeList()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Comparison load: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Load stats when either player changes
    LaunchedEffect(player1?.id, player2?.id) {
        if (player1 == null && player2 == null) return@LaunchedEffect
        statsLoading = true
        scope.launch {
            try {
                val repo = PlayerRepository()
                stats1 = player1?.let { repo.computePlayerStats(it.id) }
                stats2 = player2?.let { repo.computePlayerStats(it.id) }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Comparison stats: ${e.message}", e)
            } finally {
                statsLoading = false
            }
        }
    }

    // Player picker dialog
    if (pickingSlot != 0) {
        val otherPlayerId = if (pickingSlot == 1) player2?.id else player1?.id
        val filtered = allPlayers.filter {
            it.id != otherPlayerId &&
                    (searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true))
        }
        AlertDialog(
            onDismissRequest = { pickingSlot = 0; searchQuery = "" },
            containerColor = SurfaceCard,
            title = {
                Text(
                    if (pickingSlot == 1) "Select Player 1" else "Select Player 2",
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by name") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonGreen) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = NeonGreen,
                            focusedLabelColor = NeonGreen,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(filtered) { player ->
                            val team = allTeams.find { it.id == player.teamId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (pickingSlot == 1) player1 = player else player2 = player
                                        pickingSlot = 0; searchQuery = ""
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .border(1.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        player.jerseyNo?.toString() ?: player.fullName.take(1).uppercase(),
                                        color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${team?.name ?: "No team"} • ${player.role?.replaceFirstChar { it.uppercase() } ?: "Player"}",
                                        color = TextSecondary, fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("No players found", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickingSlot = 0; searchQuery = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── UI ──
    Column(
        modifier = Modifier.fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color(0xFF030F08) else Color(0xFFF0FDF8))
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text("Player Comparison", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // Player slots
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayerSlot(
                player = player1,
                team = allTeams.find { it.id == player1?.teamId },
                color = NeonGreen,
                label = "Player 1",
                modifier = Modifier.weight(1f),
                onClick = { pickingSlot = 1 }
            )
            PlayerSlot(
                player = player2,
                team = allTeams.find { it.id == player2?.teamId },
                color = NeonBlue,
                label = "Player 2",
                modifier = Modifier.weight(1f),
                onClick = { pickingSlot = 2 }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading || statsLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
            return@Column
        }

        if (stats1 == null || stats2 == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Text("Select two players to compare", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Column
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceCard,
            contentColor = NeonGreen,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { idx, tab ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            tab, fontSize = 13.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == idx) NeonGreen else TextSecondary
                        )
                    }
                )
            }
        }

        val s1 = stats1!!
        val s2 = stats2!!
        val n1 = player1!!.fullName
        val n2 = player2!!.fullName

        when (selectedTab) {
            0 -> BattingComparisonTab(s1, s2, n1, n2)
            1 -> BowlingComparisonTab(s1, s2, n1, n2)
            2 -> FieldingComparisonTab(s1, s2, n1, n2)
            3 -> RadarComparisonTab(s1, s2, n1, n2)
        }
    }
}

// ── PLAYER SLOT ──────────────────────────────────────────────

@Composable
fun PlayerSlot(
    player: Player?, team: Team?, color: Color, label: String,
    modifier: Modifier, onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
            .border(1.dp, if (player != null) color.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (player != null) {
                Text(
                    player.jerseyNo?.toString() ?: player.fullName.take(1).uppercase(),
                    color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            } else {
                Icon(Icons.Default.Person, null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            player?.fullName ?: label,
            color = if (player != null) TextPrimary else TextSecondary,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            if (player != null) (team?.name ?: "—") else "Tap to select",
            color = TextSecondary, fontSize = 10.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ── BATTING TAB ──────────────────────────────────────────────

@Composable
fun BattingComparisonTab(s1: PlayerStats, s2: PlayerStats, n1: String, n2: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ComparisonHeader(n1, n2) }
        item { CompareStatBar("Matches", s1.matches, s2.matches) }
        item { CompareStatBar("Innings", s1.innings, s2.innings) }
        item { CompareStatBar("Runs", s1.runs, s2.runs) }
        item { CompareStatBar("Balls Faced", s1.ballsFaced, s2.ballsFaced) }
        item { CompareStatBar("Highest Score", s1.highestScore, s2.highestScore) }
        item { CompareStatBarDouble("Average", s1.average, s2.average) }
        item { CompareStatBarDouble("Strike Rate", s1.strikeRate, s2.strikeRate) }
        item { CompareStatBar("50s", s1.fifties, s2.fifties) }
        item { CompareStatBar("100s", s1.hundreds, s2.hundreds) }
        item { CompareStatBar("4s", s1.fours, s2.fours) }
        item { CompareStatBar("6s", s1.sixes, s2.sixes) }
        item { CompareStatBarDouble("Boundary %", s1.boundaryPercent, s2.boundaryPercent) }
        item { CompareStatBarDouble("Dot %", s1.dotBallPercent, s2.dotBallPercent, lowerIsBetter = true) }
        item { CompareStatBar("Not Outs", s1.notOuts, s2.notOuts) }
        item { CompareStatBar("Ducks", s1.ducks, s2.ducks, lowerIsBetter = true) }
    }
}

// ── BOWLING TAB ──────────────────────────────────────────────

@Composable
fun BowlingComparisonTab(s1: PlayerStats, s2: PlayerStats, n1: String, n2: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ComparisonHeader(n1, n2) }
        item { CompareStatBarDouble("Overs", s1.oversBowled, s2.oversBowled) }
        item { CompareStatBar("Wickets", s1.wickets, s2.wickets) }
        item { CompareStatBar("Runs Conceded", s1.runsConceded, s2.runsConceded, lowerIsBetter = true) }
        item { CompareStatBarDouble("Economy", s1.economy, s2.economy, lowerIsBetter = true) }
        item { CompareStatBarDouble("Avg", s1.bowlingAverage, s2.bowlingAverage, lowerIsBetter = true) }
        item { CompareStatBarDouble("SR", s1.bowlingStrikeRate, s2.bowlingStrikeRate, lowerIsBetter = true) }
        item { CompareStatBar("Maidens", s1.maidens, s2.maidens) }
        item { CompareStatBar("3W Hauls", s1.threeWicketHauls, s2.threeWicketHauls) }
        item { CompareStatBar("5W Hauls", s1.fiveWicketHauls, s2.fiveWicketHauls) }
        item { CompareStatBar("Dot Balls", s1.dotBalls, s2.dotBalls) }
        item { CompareStatBar("Wides", s1.wides, s2.wides, lowerIsBetter = true) }
        item { CompareStatBar("No Balls", s1.noBalls, s2.noBalls, lowerIsBetter = true) }
        item {
            CompareStatText("Best Bowling", s1.bestBowling, s2.bestBowling)
        }
    }
}

// ── FIELDING TAB ─────────────────────────────────────────────

@Composable
fun FieldingComparisonTab(s1: PlayerStats, s2: PlayerStats, n1: String, n2: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ComparisonHeader(n1, n2) }
        item { CompareStatBar("Catches", s1.catches, s2.catches) }
        item { CompareStatBar("Run Outs", s1.runOuts, s2.runOuts) }
        item { CompareStatBar("Stumpings", s1.stumpings, s2.stumpings) }
        item { CompareStatBar("Missed Chances", s1.missedChances, s2.missedChances, lowerIsBetter = true) }
    }
}

// ── RADAR TAB ────────────────────────────────────────────────

@Composable
fun RadarComparisonTab(s1: PlayerStats, s2: PlayerStats, n1: String, n2: String) {
    // Normalize stats to 0–1 for radar axes
    data class RadarAxis(val label: String, val v1: Float, val v2: Float)

    fun safeNorm(a: Double, b: Double): Pair<Float, Float> {
        val mx = maxOf(a, b, 1.0)
        return Pair((a / mx).toFloat(), (b / mx).toFloat())
    }

    fun safeNormInt(a: Int, b: Int): Pair<Float, Float> = safeNorm(a.toDouble(), b.toDouble())

    val axes = listOf(
        safeNormInt(s1.runs, s2.runs).let { RadarAxis("Runs", it.first, it.second) },
        safeNorm(s1.average, s2.average).let { RadarAxis("Bat Avg", it.first, it.second) },
        safeNorm(s1.strikeRate, s2.strikeRate).let { RadarAxis("Bat SR", it.first, it.second) },
        safeNormInt(s1.wickets, s2.wickets).let { RadarAxis("Wickets", it.first, it.second) },
        // For economy/bowling avg, lower is better → invert
        run {
            val e1 = if (s1.economy > 0) s1.economy else 99.0
            val e2 = if (s2.economy > 0) s2.economy else 99.0
            val mx = maxOf(e1, e2, 1.0)
            RadarAxis("Economy", (1.0 - e1 / mx).toFloat().coerceAtLeast(0.05f), (1.0 - e2 / mx).toFloat().coerceAtLeast(0.05f))
        },
        safeNormInt(s1.catches + s1.runOuts + s1.stumpings, s2.catches + s2.runOuts + s2.stumpings)
            .let { RadarAxis("Fielding", it.first, it.second) }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ComparisonHeader(n1, n2) }

        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Overall Comparison", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Canvas(modifier = Modifier.size(260.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.width / 2f - 30.dp.toPx()
                    val n = axes.size
                    val angleStep = (2 * PI / n).toFloat()

                    // Grid rings (3 levels)
                    for (ring in 1..3) {
                        val r = radius * ring / 3f
                        val gridPath = Path()
                        for (i in 0 until n) {
                            val angle = -PI.toFloat() / 2 + i * angleStep
                            val x = cx + r * cos(angle)
                            val y = cy + r * sin(angle)
                            if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                        }
                        gridPath.close()
                        drawPath(gridPath, BorderColor, style = Stroke(width = 0.8f))
                    }

                    // Axis lines + labels
                    for (i in 0 until n) {
                        val angle = -PI.toFloat() / 2 + i * angleStep
                        val ex = cx + radius * cos(angle)
                        val ey = cy + radius * sin(angle)
                        drawLine(BorderColor, Offset(cx, cy), Offset(ex, ey), strokeWidth = 0.5f)

                        // Label
                        val lx = cx + (radius + 16.dp.toPx()) * cos(angle)
                        val ly = cy + (radius + 16.dp.toPx()) * sin(angle)
                        drawContext.canvas.nativeCanvas.drawText(
                            axes[i].label, lx, ly + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                textSize = 22f
                                color = 0xFF9E9E9E.toInt()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }

                    // Player 1 polygon
                    val path1 = Path()
                    for (i in 0 until n) {
                        val angle = -PI.toFloat() / 2 + i * angleStep
                        val r = radius * axes[i].v1.coerceIn(0.05f, 1f)
                        val x = cx + r * cos(angle)
                        val y = cy + r * sin(angle)
                        if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                    }
                    path1.close()
                    drawPath(path1, NeonGreen.copy(alpha = 0.15f))
                    drawPath(path1, NeonGreen, style = Stroke(width = 2.dp.toPx()))

                    // Player 2 polygon
                    val path2 = Path()
                    for (i in 0 until n) {
                        val angle = -PI.toFloat() / 2 + i * angleStep
                        val r = radius * axes[i].v2.coerceIn(0.05f, 1f)
                        val x = cx + r * cos(angle)
                        val y = cy + r * sin(angle)
                        if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                    }
                    path2.close()
                    drawPath(path2, NeonBlue.copy(alpha = 0.15f))
                    drawPath(path2, NeonBlue, style = Stroke(width = 2.dp.toPx()))

                    // Dots at vertices
                    for (i in 0 until n) {
                        val angle = -PI.toFloat() / 2 + i * angleStep
                        val r1 = radius * axes[i].v1.coerceIn(0.05f, 1f)
                        drawCircle(NeonGreen, 4.dp.toPx(), Offset(cx + r1 * cos(angle), cy + r1 * sin(angle)))
                        val r2 = radius * axes[i].v2.coerceIn(0.05f, 1f)
                        drawCircle(NeonBlue, 4.dp.toPx(), Offset(cx + r2 * cos(angle), cy + r2 * sin(angle)))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(12.dp, 3.dp).background(NeonGreen))
                        Text(n1, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(12.dp, 3.dp).background(NeonBlue))
                        Text(n2, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Quick head-to-head summary
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
                    .padding(16.dp)
            ) {
                Text("Head to Head Summary", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                H2HRow(n1, "Runs", "${s1.runs}", "${s2.runs}", n2, s1.runs > s2.runs)
                H2HRow(n1, "Average", "%.1f".format(s1.average), "%.1f".format(s2.average), n2, s1.average > s2.average)
                H2HRow(n1, "SR", "%.1f".format(s1.strikeRate), "%.1f".format(s2.strikeRate), n2, s1.strikeRate > s2.strikeRate)
                H2HRow(n1, "Wickets", "${s1.wickets}", "${s2.wickets}", n2, s1.wickets > s2.wickets)
                H2HRow(n1, "Economy", "%.1f".format(s1.economy), "%.1f".format(s2.economy), n2, s1.economy < s2.economy && s1.economy > 0)
                H2HRow(n1, "Catches", "${s1.catches}", "${s2.catches}", n2, s1.catches > s2.catches)
            }
        }
    }
}

// ── SHARED COMPARISON COMPOSABLES ────────────────────────────

@Composable
fun ComparisonHeader(n1: String, n2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(n1, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("vs", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Text(n2, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Side-by-side bar for integer stats. */
@Composable
fun CompareStatBar(label: String, v1: Int, v2: Int, lowerIsBetter: Boolean = false) {
    val maxVal = maxOf(v1, v2, 1)
    val better1 = if (lowerIsBetter) (v1 < v2 || (v1 == v2 && v1 > 0)) else v1 > v2
    val better2 = if (lowerIsBetter) (v2 < v1 || (v1 == v2 && v2 > 0)) else v2 > v1

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "$v1", fontSize = 14.sp,
                color = if (better1) NeonGreen else TextPrimary,
                fontWeight = if (better1) FontWeight.Bold else FontWeight.Normal
            )
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(
                "$v2", fontSize = 14.sp,
                color = if (better2) NeonBlue else TextPrimary,
                fontWeight = if (better2) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Left bar (grows right-to-left)
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(v1.toFloat() / maxVal)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonGreen.copy(alpha = if (better1) 0.8f else 0.35f))
                )
            }
            // Right bar (grows left-to-right)
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(v2.toFloat() / maxVal)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonBlue.copy(alpha = if (better2) 0.8f else 0.35f))
                )
            }
        }
    }
}

/** Side-by-side bar for Double stats. */
@Composable
fun CompareStatBarDouble(label: String, v1: Double, v2: Double, lowerIsBetter: Boolean = false) {
    val maxVal = maxOf(v1, v2, 0.01)
    val better1 = if (lowerIsBetter) (v1 < v2 && v1 > 0) else v1 > v2
    val better2 = if (lowerIsBetter) (v2 < v1 && v2 > 0) else v2 > v1

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "%.2f".format(v1), fontSize = 14.sp,
                color = if (better1) NeonGreen else TextPrimary,
                fontWeight = if (better1) FontWeight.Bold else FontWeight.Normal
            )
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(
                "%.2f".format(v2), fontSize = 14.sp,
                color = if (better2) NeonBlue else TextPrimary,
                fontWeight = if (better2) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((v1 / maxVal).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonGreen.copy(alpha = if (better1) 0.8f else 0.35f))
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((v2 / maxVal).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonBlue.copy(alpha = if (better2) 0.8f else 0.35f))
                )
            }
        }
    }
}

/** Non-numeric comparison (like best bowling figures). */
@Composable
fun CompareStatText(label: String, v1: String, v2: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(v1, fontSize = 14.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(v2, fontSize = 14.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun H2HRow(n1: String, label: String, v1: String, v2: String, n2: String, p1Better: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            v1, fontSize = 13.sp,
            color = if (p1Better) NeonGreen else TextPrimary,
            fontWeight = if (p1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(
            v2, fontSize = 13.sp,
            color = if (!p1Better) NeonBlue else TextPrimary,
            fontWeight = if (!p1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End
        )
    }
}