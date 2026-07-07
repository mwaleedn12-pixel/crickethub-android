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
import com.crickethub.data.model.Team

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
fun TeamsScreen(
    onTeamClick: (String) -> Unit,
    viewModel: TeamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Teams",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = { showCreateDialog = true }) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏏", fontSize = 48.sp)
                        Text("No teams yet", color = TextSecondary, fontSize = 16.sp)
                        Text("Tap + to create your first team", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.teams) { team ->
                        TeamCard(
                            team = team,
                            onClick = { onTeamClick(team.id) },
                            onDelete = { viewModel.deleteTeam(team.id) }
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateTeamDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, shortName, category, country, city, coach ->
                    viewModel.createTeam(name, shortName, category, country, city, coach)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun TeamCard(
    team: Team,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val jerseyColor = try {
        Color(android.graphics.Color.parseColor(team.jerseyColor))
    } catch (e: Exception) {
        NeonGreen
    }

    val categoryColor = when (team.category) {
        "international" -> AmberColor
        "domestic" -> NeonBlue
        "club" -> NeonGreen
        "school", "college" -> Color(0xFF8B5CF6)
        "corporate" -> Color(0xFFEC4899)
        "street", "tape_ball" -> Color(0xFFF97316)
        else -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Team color circle / logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(jerseyColor.copy(alpha = 0.2f))
                .border(2.dp, jerseyColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                team.shortName?.uppercase() ?: team.name.take(2).uppercase(),
                color = jerseyColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    team.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        team.category.replaceFirstChar { it.uppercase() },
                        color = categoryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                team.city?.let {
                    Text("📍 $it", color = TextSecondary, fontSize = 11.sp)
                }
                team.coach?.let {
                    Text("👨‍💼 $it", color = TextSecondary, fontSize = 11.sp)
                }
            }

            team.homeGround?.let {
                Text("🏟 $it", color = TextSecondary, fontSize = 11.sp)
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = ErrorRed.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?, String, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("club") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var coach by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val categories = listOf(
        "international" to "🌍 International",
        "domestic" to "🏠 Domestic",
        "club" to "🏏 Club",
        "school" to "🏫 School",
        "college" to "🎓 College",
        "corporate" to "💼 Corporate",
        "tape_ball" to "🎾 Street/Tape Ball"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        title = {
            Text(
                "Create Team",
                color = Color(0xFFF9FAFB),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Team name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Team Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // Short name
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { if (it.length <= 4) shortName = it },
                    label = { Text("Short Name (max 4 chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // Category dropdown
                Box {
                    OutlinedTextField(
                        value = categories.find { it.first == selectedCategory }?.second ?: "Club",
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogFieldColors(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.background(Color(0xFF111827))
                    ) {
                        categories.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color(0xFFF9FAFB)) },
                                onClick = {
                                    selectedCategory = value
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Country
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // City
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // Coach
                OutlinedTextField(
                    value = coach,
                    onValueChange = { coach = it },
                    label = { Text("Coach") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(
                            name.trim(),
                            shortName.ifBlank { null },
                            selectedCategory,
                            country.ifBlank { null },
                            city.ifBlank { null },
                            coach.ifBlank { null }
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9CA3AF))
            }
        }
    )
}

@Composable
fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFF9FAFB),
    unfocusedTextColor = Color(0xFFF9FAFB),
    focusedBorderColor = Color(0xFF10B981),
    unfocusedBorderColor = Color(0xFF1F2937),
    focusedLabelColor = Color(0xFF10B981),
    unfocusedLabelColor = Color(0xFF9CA3AF),
    cursorColor = Color(0xFF10B981)
)