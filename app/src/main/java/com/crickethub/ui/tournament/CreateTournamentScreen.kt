package com.crickethub.ui.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.TournamentInsert

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun CreateTournamentScreen(
    onBack: () -> Unit,
    onTournamentCreated: (String) -> Unit,
    viewModel: TournamentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("League") }
    var venue by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var totalOvers by remember { mutableStateOf("20") }
    var maxTeams by remember { mutableStateOf("8") }
    var matchType by remember { mutableStateOf("T20") }
    var createdTournamentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.currentTournament) {
        val tournament = uiState.currentTournament
        if (tournament != null && createdTournamentId == null) {
            createdTournamentId = tournament.id
            onTournamentCreated(tournament.id)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
        cursorColor = NeonGreen, focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextSecondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Create Tournament",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tournament Name *") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
            }
            item {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = fieldColors
                )
            }

            // Format selector
            item {
                Text("Format", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TOURNAMENT_FORMATS.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { format ->
                                val isSelected = selectedFormat == format
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else BackgroundDark)
                                        .border(1.dp, if (isSelected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { selectedFormat = format }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        format,
                                        color = if (isSelected) NeonGreen else TextSecondary,
                                        fontSize = 12.sp,
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

            // Match Type
            item {
                Text("Match Type", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("T20", "ODI", "Test", "T10", "Custom").forEach { type ->
                        val isSelected = matchType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else BackgroundDark)
                                .border(1.dp, if (isSelected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    matchType = type
                                    totalOvers = when (type) {
                                        "T20" -> "20"; "ODI" -> "50"
                                        "T10" -> "10"; "Test" -> "90"
                                        else -> totalOvers
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                type,
                                color = if (isSelected) NeonGreen else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = totalOvers,
                        onValueChange = { if (it.all { c -> c.isDigit() }) totalOvers = it },
                        label = { Text("Overs/Match") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = maxTeams,
                        onValueChange = { if (it.all { c -> c.isDigit() }) maxTeams = it },
                        label = { Text("Max Teams") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = fieldColors
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = venue, onValueChange = { venue = it },
                    label = { Text("Venue") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
            }
            item {
                OutlinedTextField(
                    value = organizer, onValueChange = { organizer = it },
                    label = { Text("Organizer") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = startDate, onValueChange = { startDate = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = endDate, onValueChange = { endDate = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = fieldColors
                    )
                }
            }

            uiState.error?.let {
                item {
                    Text(it, color = ErrorRed, fontSize = 12.sp)
                }
            }
        }

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.createTournament(
                        TournamentInsert(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            format = selectedFormat,
                            venue = venue.trim().ifBlank { null },
                            organizer = organizer.trim().ifBlank { null },
                            startDate = startDate.trim().ifBlank { null },
                            endDate = endDate.trim().ifBlank { null },
                        )
                    )
                }
            },
            enabled = name.isNotBlank() && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Create Tournament", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}