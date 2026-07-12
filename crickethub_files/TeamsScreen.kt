package com.crickethub.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
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
import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert


val TEAM_CATEGORIES = listOf(
    "International", "Domestic", "Club",
    "School", "College", "Corporate", "Street/Tape Ball"
)

val JERSEY_COLORS = listOf(
    "#10B981", "#3B82F6", "#EF4444", "#F59E0B",
    "#8B5CF6", "#EC4899", "#06B6D4", "#F97316",
    "#84CC16", "#FFFFFF", "#000000", "#6B7280"
)

@Composable
fun TeamsScreen(
    onTeamClick: (String) -> Unit,
    viewModel: TeamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var teamToEdit by remember { mutableStateOf<Team?>(null) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Teams", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Team", tint = NeonGreen)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (uiState.teams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                        Text("No teams yet", color = TextSecondary, fontSize = 16.sp)
                        Text("Tap + to create your first team", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.teams) { team ->
                        TeamCard(
                            team = team,
                            onClick = { onTeamClick(team.id) },
                            onEdit = { teamToEdit = team },
                            onDelete = { teamToDelete = team }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            TeamDialog(
                title = "Create Team",
                onDismiss = { showAddDialog = false },
                onConfirm = { teamInsert ->
                    viewModel.createTeam(teamInsert)
                    showAddDialog = false
                }
            )
        }

        teamToEdit?.let { team ->
            TeamDialog(
                title = "Edit Team",
                team = team,
                onDismiss = { teamToEdit = null },
                onConfirm = { teamInsert ->
                    viewModel.updateTeam(team.id, teamInsert)
                    teamToEdit = null
                }
            )
        }

        teamToDelete?.let { team ->
            AlertDialog(
                onDismissRequest = { teamToDelete = null },
                containerColor = SurfaceCard,
                title = { Text("Delete Team", color = ErrorRed, fontWeight = FontWeight.Bold) },
                text = { Text("Delete '${team.name}'? This cannot be undone.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteTeam(team.id); teamToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { teamToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        uiState.error?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = ErrorRed
            ) { Text(it, color = Color.White) }
        }
    }
}

@Composable
fun TeamCard(
    team: Team,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val jerseyColor = try {
        Color(android.graphics.Color.parseColor(team.jerseyColor ?: "#10B981"))
    } catch (e: Exception) { NeonGreen }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(jerseyColor.copy(alpha = 0.2f))
                    .border(2.dp, jerseyColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    team.shortName?.take(3) ?: team.name.take(2).uppercase(),
                    color = jerseyColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    team.category?.let { CategoryBadge(it) }
                    team.city?.let { Text("📍 $it", color = TextSecondary, fontSize = 11.sp) }
                }
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (team.coach != null || team.joinCode != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                team.coach?.let { Text("🏏 Coach: $it", color = TextSecondary, fontSize = 11.sp) }
                team.joinCode?.let { Text("🔑 $it", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val color = when (category) {
        "International" -> Color(0xFF3B82F6)
        "Domestic" -> Color(0xFF8B5CF6)
        "Club" -> NeonGreen
        "School" -> Color(0xFFF59E0B)
        "College" -> Color(0xFFEC4899)
        "Corporate" -> Color(0xFF06B6D4)
        else -> TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(category, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TeamDialog(
    title: String,
    team: Team? = null,
    onDismiss: () -> Unit,
    onConfirm: (TeamInsert) -> Unit
) {
    var name by remember { mutableStateOf(team?.name ?: "") }
    var shortName by remember { mutableStateOf(team?.shortName ?: "") }
    var selectedCategory by remember { mutableStateOf(team?.category ?: "Club") }
    var selectedColor by remember { mutableStateOf(team?.jerseyColor ?: "#10B981") }
    var country by remember { mutableStateOf(team?.country ?: "") }
    var city by remember { mutableStateOf(team?.city ?: "") }
    var homeGround by remember { mutableStateOf(team?.homeGround ?: "") }
    var coach by remember { mutableStateOf(team?.coach ?: "") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
        cursorColor = NeonGreen, focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextSecondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Team Name *") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { if (it.length <= 5) shortName = it },
                        label = { Text("Short Name (e.g. PAK)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                }
                item {
                    Text("Category", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TEAM_CATEGORIES.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    val catColor = when (cat) {
                                        "International" -> NeonBlue
                                        "Domestic" -> Color(0xFF8B5CF6)
                                        "Club" -> NeonGreen
                                        "School" -> AmberColor
                                        "College" -> Color(0xFFEC4899)
                                        "Corporate" -> Color(0xFF06B6D4)
                                        else -> TextSecondary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) catColor.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (isSelected) catColor else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedCategory = cat }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            cat,
                                            color = if (isSelected) catColor else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    Text("Jersey Color", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JERSEY_COLORS.take(6).forEach { colorHex ->
                            val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { NeonGreen }
                            Box(
                                modifier = Modifier
                                    .size(32.dp).clip(CircleShape).background(c)
                                    .border(if (selectedColor == colorHex) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JERSEY_COLORS.drop(6).forEach { colorHex ->
                            val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { NeonGreen }
                            Box(
                                modifier = Modifier
                                    .size(32.dp).clip(CircleShape).background(c)
                                    .border(if (selectedColor == colorHex) 3.dp else 0.dp, Color.White, CircleShape)
                                    .border(1.dp, BorderColor, CircleShape)
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = country, onValueChange = { country = it },
                        label = { Text("Country") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = city, onValueChange = { city = it },
                        label = { Text("City") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = homeGround, onValueChange = { homeGround = it },
                        label = { Text("Home Ground") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = coach, onValueChange = { coach = it },
                        label = { Text("Coach") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(TeamInsert(
                            name = name.trim(),
                            shortName = shortName.trim().ifBlank { null },
                            jerseyColor = selectedColor,
                            category = selectedCategory,
                            country = country.trim().ifBlank { null },
                            city = city.trim().ifBlank { null },
                            homeGround = homeGround.trim().ifBlank { null },
                            coach = coach.trim().ifBlank { null }
                        ))
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}