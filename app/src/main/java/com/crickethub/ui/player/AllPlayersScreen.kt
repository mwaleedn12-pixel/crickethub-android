package com.crickethub.ui.player

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.crickethub.ui.theme.*

@Composable
fun AllPlayersScreen(
    onPlayerClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var allPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var allTeams by remember { mutableStateOf<List<Team>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allPlayers = SupabaseClient.client.postgrest["players"]
                    .select().decodeList()
                allTeams = SupabaseClient.client.postgrest["teams"]
                    .select().decodeList()
            } catch (e: Exception) {
                android.util.Log.e("CricketHub", "AllPlayers error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    val filtered = allPlayers.filter {
        searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true)
    }
    // Group by team
    val grouped = filtered.groupBy { p -> allTeams.find { it.id == p.teamId } }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Players", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CH.textPrimary, modifier = Modifier.weight(1f))
                Text("${allPlayers.size} total", color = CH.textSecondary, fontSize = 12.sp)
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search players...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonGreen) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CH.textPrimary,
                    unfocusedTextColor = CH.textPrimary,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = CH.border,
                    cursorColor = NeonGreen,
                    focusedLeadingIconColor = NeonGreen,
                    unfocusedLeadingIconColor = CH.textSecondary
                )
            )

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
                return@Column
            }

            if (allPlayers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, null, tint = CH.textSecondary, modifier = Modifier.size(48.dp))
                        Text("No players yet", color = CH.textSecondary, fontSize = 14.sp)
                        Text("Add players to your teams first", color = CH.textSecondary, fontSize = 12.sp)
                    }
                }
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                grouped.forEach { (team, players) ->
                    // Team header
                    item(key = "team_${team?.id ?: "none"}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                                    .background(NeonGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    team?.shortName?.take(2) ?: team?.name?.take(2)?.uppercase() ?: "?",
                                    color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                team?.name ?: "No Team",
                                color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text("(${players.size})", color = CH.textSecondary, fontSize = 11.sp)
                        }
                    }

                    // Players
                    items(players, key = { it.id }) { player ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSystemInDarkTheme()) Color(0xFF161616) else Color(0xFFFFFFFF))
                                .border(1.dp, CH.border, RoundedCornerShape(10.dp))
                                .clickable { onPlayerClick(player.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .border(1.dp, NeonGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    player.jerseyNo?.toString() ?: player.fullName.take(1).uppercase(),
                                    color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    player.fullName, color = CH.textPrimary,
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    player.role?.let { role ->
                                        val rc = when (role.lowercase()) {
                                            "batsman" -> NeonBlue; "bowler" -> ErrorRed
                                            "allrounder", "all-rounder" -> NeonGreen
                                            "wicketkeeper", "wicket keeper" -> AmberColor
                                            else -> CH.textSecondary
                                        }
                                        Text(
                                            role.replaceFirstChar { it.uppercase() },
                                            color = rc, fontSize = 11.sp
                                        )
                                    }
                                    player.battingHand?.let {
                                        Text("• ${it.take(1).uppercase()}HB", color = CH.textSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                            Text("›", color = NeonGreen, fontSize = 18.sp)
                        }
                    }
                }

                if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No players match \"$searchQuery\"", color = CH.textSecondary, fontSize = 13.sp)
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    } // CricketAnimatedBackground
}