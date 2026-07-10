package com.crickethub.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Match
import com.crickethub.data.model.Player
import com.crickethub.data.model.PlayerStats
import com.crickethub.data.model.Team
import com.crickethub.data.model.Tournament
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.PlayerRepository
import com.crickethub.data.repository.ScoringRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
private val PurpleColor = Color(0xFF8B5CF6)

data class CareerData(
    val player: Player? = null,
    val currentTeam: Team? = null,
    val stats: PlayerStats = PlayerStats(),
    val allBalls: List<Ball> = emptyList(),
    val matchHistory: List<Match> = emptyList(),
    val teamsPlayed: List<Team> = emptyList(),
    val tournaments: List<Tournament> = emptyList(),
    val awards: List<String> = emptyList(),
    val hatTricks: Int = 0,
    val fastestFiftyBalls: Int = 0,
    val fastestHundredBalls: Int = 0
)

@Composable
fun PlayerCareerScreen(
    onBack: () -> Unit,
    onViewScorecard: (String) -> Unit = {},
    onViewAnalytics: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var allPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var allTeams by remember { mutableStateOf<List<Team>>(emptyList()) }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var careerData by remember { mutableStateOf(CareerData()) }
    var careerLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Profile", "Stats", "Records", "Achievements", "History")

    // Load all players on start
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val players: List<Player> = SupabaseClient.client.postgrest["players"]
                    .select()
                    .decodeList()
                val teams: List<Team> = SupabaseClient.client.postgrest["teams"]
                    .select()
                    .decodeList()
                allPlayers = players
                allTeams = teams
                // Default: logged in user ka player
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                selectedPlayer = players.firstOrNull()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Players load error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Load career data when player changes
    LaunchedEffect(selectedPlayer) {
        val player = selectedPlayer ?: return@LaunchedEffect
        careerLoading = true
        scope.launch {
            try {
                val repo = PlayerRepository()

                coroutineScope {
                    val statsDeferred = async { repo.computePlayerStats(player.id) }
                    val teamDeferred = async {
                        try {
                            SupabaseClient.client.postgrest["teams"]
                                .select { filter { eq("id", player.teamId) } }
                                .decodeSingleOrNull<Team>()
                        } catch (e: Exception) { null }
                    }
                    val batBallsDeferred = async {
                        try {
                            SupabaseClient.client.postgrest["balls"]
                                .select { filter { eq("batsman_id", player.id) } }
                                .decodeList<Ball>()
                        } catch (e: Exception) { emptyList() }
                    }
                    val bowlBallsDeferred = async {
                        try {
                            SupabaseClient.client.postgrest["balls"]
                                .select { filter { eq("bowler_id", player.id) } }
                                .decodeList<Ball>()
                        } catch (e: Exception) { emptyList() }
                    }
                    val matchesDeferred = async {
                        try {
                            SupabaseClient.client.postgrest["matches"]
                                .select {
                                    filter { eq("status", "completed") }
                                }
                                .decodeList<Match>()
                        } catch (e: Exception) { emptyList() }
                    }
                    val tournamentsDeferred = async {
                        try {
                            SupabaseClient.client.postgrest["tournaments"]
                                .select()
                                .decodeList<Tournament>()
                        } catch (e: Exception) { emptyList() }
                    }

                    val stats = statsDeferred.await()
                    val currentTeam = teamDeferred.await()
                    val batBalls = batBallsDeferred.await()
                    val bowlBalls = bowlBallsDeferred.await()
                    val allBalls = (batBalls + bowlBalls).distinctBy { it.id }
                    val matches = matchesDeferred.await()
                    val tournaments = tournamentsDeferred.await()

                    // Hat tricks
                    var hatTricks = 0
                    bowlBalls.groupBy { it.inningsId }.values.forEach { inningsBalls ->
                        var consecutive = 0
                        inningsBalls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
                            if (ball.isWicket && ball.wicketType !in listOf("run_out", "obstructing", "retired_hurt")) {
                                consecutive++
                                if (consecutive >= 3) hatTricks++
                            } else consecutive = 0
                        }
                    }

                    // Fastest fifty/hundred
                    var fastestFifty = Int.MAX_VALUE
                    var fastestHundred = Int.MAX_VALUE
                    batBalls.groupBy { it.inningsId }.values.forEach { innings ->
                        var runs = 0; var balls = 0; var fiftyReached = false
                        innings.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
                            runs += ball.runsOffBat
                            if (ball.extrasType != "wide") balls++
                            if (runs >= 50 && !fiftyReached) { fiftyReached = true; if (balls < fastestFifty) fastestFifty = balls }
                            if (runs >= 100 && balls < fastestHundred) fastestHundred = balls
                        }
                    }

                    // Match history — filter matches where player's team played
                    val matchHistory = matches
                        .filter { m -> m.team1Id == player.teamId || m.team2Id == player.teamId }
                        .sortedByDescending { it.createdAt }
                        .take(10)

                    careerData = CareerData(
                        player = player,
                        currentTeam = currentTeam,
                        stats = stats,
                        allBalls = allBalls,
                        matchHistory = matchHistory,
                        teamsPlayed = listOfNotNull(currentTeam),
                        tournaments = tournaments,
                        awards = player.awards ?: emptyList(),
                        hatTricks = hatTricks,
                        fastestFiftyBalls = if (fastestFifty == Int.MAX_VALUE) 0 else fastestFifty,
                        fastestHundredBalls = if (fastestHundred == Int.MAX_VALUE) 0 else fastestHundred
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Career error: ${e.message}", e)
            } finally {
                careerLoading = false
            }
        }
    }

    // Player search/picker dialog
    if (showPlayerPicker) {
        val filteredPlayers = allPlayers.filter {
            searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { showPlayerPicker = false; searchQuery = "" },
            containerColor = SurfaceCard,
            title = { Text("Search Player", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by name") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen) },
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
                        items(filteredPlayers) { player ->
                            val team = allTeams.find { it.id == player.teamId }
                            val isSelected = selectedPlayer?.id == player.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else BackgroundDark)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonGreen else BorderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedPlayer = player
                                        showPlayerPicker = false
                                        searchQuery = ""
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .border(1.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        player.jerseyNo?.toString() ?: player.fullName.take(1).uppercase(),
                                        color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${team?.name ?: "No team"} • ${player.role?.replaceFirstChar { it.uppercase() } ?: "Player"}",
                                        color = TextSecondary, fontSize = 11.sp
                                    )
                                }
                                if (isSelected) Text("✓", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (filteredPlayers.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("No players found", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlayerPicker = false; searchQuery = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
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
            IconButton(onClick = { showPlayerPicker = true }) {
                Icon(Icons.Default.Search, contentDescription = "Search Player", tint = NeonGreen)
            }
        }

        // Selected player chip
        selectedPlayer?.let { player ->
            val team = allTeams.find { it.id == player.teamId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { showPlayerPicker = true }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
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
                    Text(player.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${team?.name ?: "No team"} • ${player.role?.replaceFirstChar { it.uppercase() } ?: "Player"}",
                        color = TextSecondary, fontSize = 11.sp
                    )
                }
                Text("Change ›", color = NeonGreen, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading || careerLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = NeonGreen)
                    Text("Loading career data...", color = TextSecondary, fontSize = 13.sp)
                }
            }
            return@Column
        }

        if (careerData.player == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Text("No players found", color = TextSecondary, fontSize = 16.sp)
                    Text("Add players to teams first", color = TextSecondary, fontSize = 13.sp)
                    OutlinedButton(
                        onClick = { showPlayerPicker = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)
                    ) { Text("Search Players") }
                }
            }
            return@Column
        }

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
                            tab, fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NeonGreen else TextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> CareerProfileTab(careerData)
            1 -> CareerStatsTab(careerData)
            2 -> CareerRecordsTab(careerData)
            3 -> CareerAchievementsTab(careerData)
            4 -> CareerHistoryTab(careerData, onViewScorecard, onViewAnalytics)
        }
    }
}

// ── PROFILE TAB ──────────────────────────────────────────────

@Composable
fun CareerProfileTab(data: CareerData) {
    val player = data.player ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .border(3.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (player.jerseyNo != null) {
                        Text("#${player.jerseyNo}", color = NeonGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(player.fullName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                player.nickname?.let { Text("\"$it\"", color = TextSecondary, fontSize = 14.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                player.role?.let { role ->
                    val roleColor = when (role.lowercase()) {
                        "batsman" -> NeonBlue; "bowler" -> ErrorRed
                        "all-rounder" -> NeonGreen; "wicket keeper" -> AmberColor
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(roleColor.copy(alpha = 0.2f))
                            .border(1.dp, roleColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(role.replaceFirstChar { it.uppercase() }, color = roleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp)
            ) {
                Text("Personal Info", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                data.currentTeam?.let { CareerInfoRow("🏏 Current Team", it.name) }
                player.jerseyNo?.let { CareerInfoRow("👕 Jersey Number", "#$it") }
                player.role?.let { CareerInfoRow("⭐ Role", it.replaceFirstChar { c -> c.uppercase() }) }
                player.battingHand?.let { CareerInfoRow("🏏 Batting", "${it.replaceFirstChar { c -> c.uppercase() }}-hand") }
                player.bowlingStyle?.let { CareerInfoRow("🎳 Bowling", it) }
                if (!player.city.isNullOrBlank() || !player.country.isNullOrBlank()) {
                    CareerInfoRow("📍 Location", listOfNotNull(player.city, player.country).joinToString(", "))
                }
                player.dateOfBirth?.let { CareerInfoRow("🎂 Date of Birth", it) }
                player.gender?.let { CareerInfoRow("👤 Gender", it.replaceFirstChar { c -> c.uppercase() }) }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp)
            ) {
                Text("Career Overview", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CareerQuickStat("Matches", data.stats.matches.toString(), NeonGreen)
                    CareerQuickStat("Runs", data.stats.runs.toString(), NeonBlue)
                    CareerQuickStat("Wickets", data.stats.wickets.toString(), ErrorRed)
                    CareerQuickStat("Catches", data.stats.catches.toString(), AmberColor)
                }
            }
        }
    }
}

@Composable
fun CareerInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}

@Composable
fun CareerQuickStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

// ── STATS TAB ────────────────────────────────────────────────

@Composable
fun CareerStatsTab(data: CareerData) {
    val s = data.stats
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CareerStatsCard("🏏 Batting Stats") {
                StatRowItem("Matches", s.matches.toString())
                StatRowItem("Innings", s.innings.toString())
                StatRowItem("Runs", s.runs.toString())
                StatRowItem("Balls Faced", s.ballsFaced.toString())
                StatRowItem("Not Outs", s.notOuts.toString())
                StatRowItem("Average", "%.2f".format(s.average))
                StatRowItem("Strike Rate", "%.2f".format(s.strikeRate))
                StatRowItem("50s", s.fifties.toString())
                StatRowItem("100s", s.hundreds.toString())
                StatRowItem("Ducks", s.ducks.toString())
                StatRowItem("Fours", s.fours.toString())
                StatRowItem("Sixes", s.sixes.toString())
                StatRowItem("Boundary %", "%.1f%%".format(s.boundaryPercent))
            }
        }
        item {
            CareerStatsCard("🎳 Bowling Stats") {
                StatRowItem("Overs", "%.1f".format(s.oversBowled))
                StatRowItem("Maidens", s.maidens.toString())
                StatRowItem("Runs Conceded", s.runsConceded.toString())
                StatRowItem("Wickets", s.wickets.toString())
                StatRowItem("Economy", "%.2f".format(s.economy))
                StatRowItem("Average", "%.2f".format(s.bowlingAverage))
                StatRowItem("Strike Rate", "%.2f".format(s.bowlingStrikeRate))
                StatRowItem("Wides", s.wides.toString())
                StatRowItem("No Balls", s.noBalls.toString())
                StatRowItem("Dot Balls", s.dotBalls.toString())
                StatRowItem("3W Hauls", s.threeWicketHauls.toString())
                StatRowItem("5W Hauls", s.fiveWicketHauls.toString())
            }
        }
        item {
            CareerStatsCard("🧤 Fielding Stats") {
                StatRowItem("Catches", s.catches.toString())
                StatRowItem("Run Outs", s.runOuts.toString())
                StatRowItem("Stumpings", s.stumpings.toString())
                StatRowItem("Missed Chances", s.missedChances.toString())
            }
        }
    }
}

@Composable
fun CareerStatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(16.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
        content()
    }
}

@Composable
fun StatRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── RECORDS TAB ──────────────────────────────────────────────

@Composable
fun CareerRecordsTab(data: CareerData) {
    val s = data.stats
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CareerStatsCard("🏆 Career Records") {
                RecordRow("🏏 Highest Score", "${s.highestScore}", NeonBlue)
                RecordRow("🎳 Best Bowling", s.bestBowling, ErrorRed)
                RecordRow("⚡ Fastest Fifty", if (data.fastestFiftyBalls > 0) "${data.fastestFiftyBalls} balls" else "N/A", NeonGreen)
                RecordRow("💯 Fastest Hundred", if (data.fastestHundredBalls > 0) "${data.fastestHundredBalls} balls" else "N/A", AmberColor)
                RecordRow("🎩 Hat-tricks", "${data.hatTricks}", PurpleColor)
                RecordRow("5️⃣ Five-Wicket Hauls", "${s.fiveWicketHauls}", ErrorRed)
                RecordRow("3️⃣ Three-Wicket Hauls", "${s.threeWicketHauls}", AmberColor)
                RecordRow("💯 Centuries", "${s.hundreds}", NeonGreen)
                RecordRow("⭐ Half-Centuries", "${s.fifties}", NeonBlue)
                RecordRow("🦆 Ducks", "${s.ducks}", TextSecondary)
            }
        }
    }
}

@Composable
fun RecordRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}

// ── ACHIEVEMENTS TAB ─────────────────────────────────────────

@Composable
fun CareerAchievementsTab(data: CareerData) {
    val s = data.stats
    val achievements = mutableListOf<Pair<String, String>>()
    if (s.hundreds > 0) achievements.add("💯" to "${s.hundreds} Career ${if (s.hundreds == 1) "Century" else "Centuries"}")
    if (s.fifties > 0) achievements.add("⭐" to "${s.fifties} Half-${if (s.fifties == 1) "Century" else "Centuries"}")
    if (s.fiveWicketHauls > 0) achievements.add("🎳" to "${s.fiveWicketHauls} Five-Wicket Haul${if (s.fiveWicketHauls > 1) "s" else ""}")
    if (data.hatTricks > 0) achievements.add("🎩" to "${data.hatTricks} Hat-trick${if (data.hatTricks > 1) "s" else ""}")
    if (s.runs >= 1000) achievements.add("🏏" to "1000+ Career Runs")
    if (s.runs >= 500) achievements.add("📈" to "500+ Career Runs")
    if (s.wickets >= 100) achievements.add("💥" to "100+ Career Wickets")
    if (s.wickets >= 50) achievements.add("🎯" to "50+ Career Wickets")
    if (s.catches >= 20) achievements.add("🧤" to "20+ Career Catches")
    if (data.fastestFiftyBalls in 1..20) achievements.add("⚡" to "Fastest Fifty in ${data.fastestFiftyBalls} balls")
    if (data.fastestHundredBalls in 1..50) achievements.add("🚀" to "Fastest Century in ${data.fastestHundredBalls} balls")
    if (s.sixes >= 20) achievements.add("💥" to "20+ Career Sixes")
    if (s.economy in 0.1..5.0 && s.oversBowled >= 5) achievements.add("🎯" to "Economy under 5.00")
    data.awards.forEach { award -> achievements.add("🏆" to award) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("🏆 Achievements & Awards", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${achievements.size} achievements unlocked", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (achievements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No achievements yet", color = TextSecondary, fontSize = 14.sp)
                        Text("Play matches to earn achievements", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(achievements) { (emoji, text) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(emoji, fontSize = 24.sp)
                    Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (data.tournaments.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("🏆 Tournament Trophies", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(data.tournaments) { tournament ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(1.dp, AmberColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🏆", fontSize = 24.sp)
                    Column {
                        Text(tournament.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${tournament.format ?: "Tournament"} • ${tournament.status.replaceFirstChar { it.uppercase() }}",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ── HISTORY TAB ──────────────────────────────────────────────

@Composable
fun CareerHistoryTab(
    data: CareerData,
    onViewScorecard: (String) -> Unit,
    onViewAnalytics: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Teams
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏏 Teams Played For", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                if (data.teamsPlayed.isEmpty()) {
                    Text("No team history", color = TextSecondary, fontSize = 13.sp)
                } else {
                    data.teamsPlayed.forEach { team ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(NeonGreen.copy(alpha = 0.2f)).border(1.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(team.shortName?.take(2) ?: team.name.take(2).uppercase(), color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(team.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    team.category?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
                                }
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NeonGreen.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)
                            ) { Text("Current", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        // Tournaments
        if (data.tournaments.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏆 Tournament History", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                    data.tournaments.forEach { tournament ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tournament.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${tournament.format ?: "Tournament"} • ${tournament.status.replaceFirstChar { it.uppercase() }}", color = TextSecondary, fontSize = 11.sp)
                                tournament.startDate?.let { Text("📅 $it", color = TextSecondary, fontSize = 11.sp) }
                            }
                            val sc = when (tournament.status) { "completed" -> NeonGreen; "live" -> ErrorRed; else -> AmberColor }
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(sc.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(tournament.status.replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    }
                }
            }
        }

        // Match History — clickable
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📋 Recent Match History", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                if (data.matchHistory.isEmpty()) {
                    Text("No match history yet", color = TextSecondary, fontSize = 13.sp)
                } else {
                    data.matchHistory.forEach { match ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${match.matchType} • ${match.totalOvers} ov", color = TextSecondary, fontSize = 11.sp)
                                val sc = when (match.status) { "completed" -> TextSecondary; "live" -> NeonGreen; else -> AmberColor }
                                Text(match.status.replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            match.resultText?.let {
                                Text(it, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onViewScorecard(match.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) { Text("📊 Scorecard", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = { onViewAnalytics(match.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleColor),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) { Text("📈 Analytics", fontSize = 11.sp) }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}