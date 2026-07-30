package com.crickethub.ui.dashboard

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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.model.Match
import com.crickethub.data.model.Team
import com.crickethub.data.model.Tournament
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.crickethub.ui.theme.*

@Composable
fun DashboardScreen(
    onViewScorecard: (String) -> Unit = {},
    onViewAnalytics: (String) -> Unit = {},
    onJoinWithCode: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var userEmail by remember { mutableStateOf("") }
    var teamCount by remember { mutableStateOf(0) }
    var matchCount by remember { mutableStateOf(0) }
    var tournamentCount by remember { mutableStateOf(0) }
    var playerCount by remember { mutableStateOf(0) }
    var recentMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var activeTournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                userEmail = SupabaseClient.client.auth.currentUserOrNull()?.email ?: "Guest"
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id

                suspend fun fetchList(table: String): List<Map<String, kotlinx.serialization.json.JsonElement>> {
                    if (userId != null) {
                        val byCreated = try {
                            SupabaseClient.client.postgrest[table]
                                .select { filter { eq("created_by", userId) } }
                                .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                        } catch (_: Exception) { emptyList() }
                        if (byCreated.isNotEmpty()) return byCreated
                        val byUser = try {
                            SupabaseClient.client.postgrest[table]
                                .select { filter { eq("user_id", userId) } }
                                .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                        } catch (_: Exception) { emptyList() }
                        if (byUser.isNotEmpty()) return byUser
                    }
                    return try {
                        SupabaseClient.client.postgrest[table].select()
                            .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                    } catch (_: Exception) { emptyList() }
                }

                val teams = try {
                    if (userId != null) {
                        val t = SupabaseClient.client.postgrest["teams"]
                            .select { filter { eq("created_by", userId) } }.decodeList<Team>()
                        t.ifEmpty { SupabaseClient.client.postgrest["teams"].select().decodeList<Team>() }
                    } else SupabaseClient.client.postgrest["teams"].select().decodeList<Team>()
                } catch (_: Exception) { emptyList<Team>() }
                teamCount = teams.size

                playerCount = try {
                    if (teams.isEmpty()) 0
                    else teams.sumOf { team ->
                        try { SupabaseClient.client.postgrest["players"]
                            .select { filter { eq("team_id", team.id) } }
                            .decodeList<com.crickethub.data.model.Player>().size
                        } catch (_: Exception) { 0 }
                    }
                } catch (_: Exception) { 0 }

                val matches = try {
                    if (userId != null) {
                        val m = SupabaseClient.client.postgrest["matches"]
                            .select { filter { eq("created_by", userId) } }.decodeList<Match>()
                        m.ifEmpty { SupabaseClient.client.postgrest["matches"].select().decodeList<Match>() }
                    } else SupabaseClient.client.postgrest["matches"].select().decodeList<Match>()
                } catch (_: Exception) { emptyList<Match>() }
                matchCount = matches.size
                recentMatches = matches.sortedByDescending { it.createdAt }.take(5)

                val tournaments = try {
                    if (userId != null) {
                        val t = SupabaseClient.client.postgrest["tournaments"]
                            .select { filter { eq("created_by", userId) } }.decodeList<Tournament>()
                        t.ifEmpty { SupabaseClient.client.postgrest["tournaments"].select().decodeList<Tournament>() }
                    } else SupabaseClient.client.postgrest["tournaments"].select().decodeList<Tournament>()
                } catch (_: Exception) { emptyList<Tournament>() }
                tournamentCount = tournaments.size
                activeTournaments = tournaments.filter { it.status != "completed" }.take(3)

            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "Dashboard error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    val cardBg = if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF)

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color(0xFF030F08) else Color(0xFFF0FDF8)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User header
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .border(2.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userEmail.take(1).uppercase(),
                        color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Welcome back!", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(userEmail, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = {
                    scope.launch {
                        try { SupabaseClient.client.auth.signOut() } catch (_: Exception) {}
                        onLogout()
                    }
                }) {
                    Icon(Icons.Default.ExitToApp, "Logout", tint = ErrorRed)
                }
            }
        }

        // Quick stats
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickStatCard("Teams", "$teamCount", Icons.Default.Person, NeonGreen, Modifier.weight(1f))
                QuickStatCard("Players", "$playerCount", Icons.Default.Person, NeonBlue, Modifier.weight(1f))
                QuickStatCard("Matches", "$matchCount", Icons.Default.List, AmberColor, Modifier.weight(1f))
                QuickStatCard("Tourneys", "$tournamentCount", Icons.Default.Star, PurpleColor, Modifier.weight(1f))
            }
        }

        // Join with Code button
        item {
            Button(
                onClick = onJoinWithCode,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f))
            ) {
                Text("🔗  Join with Code", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Feature cards
        item {
            Text("Features", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp))
        }
        item {
            val features = listOf(
                Triple("🏏", "Matches", "Create and score live matches"),
                Triple("👥", "Teams", "Manage your teams and squads"),
                Triple("🏆", "Tournaments", "Run tournaments with fixtures"),
                Triple("📊", "Players", "Career batting & bowling records"),
                Triple("⚔️", "Compare", "Head-to-head player comparison"),
                Triple("🔗", "Join", "Enter a code to view shared content")
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                features.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (emoji, title, desc) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("$emoji $title", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Active tournaments
        if (activeTournaments.isNotEmpty()) {
            item {
                Text("Active Tournaments", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            items(activeTournaments) { tournament ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tournament.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${tournament.format ?: "Tournament"} • ${tournament.status.replaceFirstChar { it.uppercase() }}",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                    val sc = when (tournament.status) { "live" -> NeonGreen; "upcoming" -> AmberColor; else -> TextSecondary }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(sc.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(tournament.status.replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent matches
        item {
            Text("Recent Matches", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (recentMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(cardBg).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matches yet. Create your first match!", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(recentMatches) { match ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${match.matchType} • ${match.totalOvers} ov", color = TextSecondary, fontSize = 11.sp)
                        val sc = when (match.status) { "completed" -> NeonGreen; "live" -> AmberColor; else -> TextSecondary }
                        Text(match.status.replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    match.resultText?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (match.status == "completed") {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onViewScorecard(match.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) { Text("Scorecard", fontSize = 11.sp) }
                            OutlinedButton(
                                onClick = { onViewAnalytics(match.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleColor),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) { Text("Analytics", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun QuickStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF0D2018) else Color(0xFFFFFFFF))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}