package com.crickethub.ui.tournament

import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import com.crickethub.ui.components.CricketAnimatedBackground
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.TournamentInsert
import com.crickethub.ui.theme.*
import com.crickethub.ui.components.DatePickerField


@Composable
fun CreateTournamentScreen(
    onBack: () -> Unit,
    onTournamentCreated: (String) -> Unit,
    viewModel: TournamentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("Round Robin") }
    var knockoutFormat by remember { mutableStateOf("Semi Finals + Final") }

    // Auto-link knockout format when certain formats are selected
    LaunchedEffect(selectedFormat) {
        when (selectedFormat) {
            "Group + Knockout" -> knockoutFormat = "Semi Finals + Final"
            "League + Playoffs" -> knockoutFormat = "Playoffs"
        }
    }
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
        focusedTextColor = CH.textPrimary, unfocusedTextColor = CH.textPrimary,
        focusedBorderColor = NeonGreen, unfocusedBorderColor = CH.border,
        cursorColor = NeonGreen, focusedLabelColor = NeonGreen,
        unfocusedLabelColor = CH.textSecondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CH.textPrimary)
                }
                Text(
                    "Create Tournament",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CH.textPrimary
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

                // Format selector
                item {
                    Text("Format", color = CH.textSecondary, fontSize = 13.sp)
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
                                            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else CH.bg)
                                            .border(1.dp, if (isSelected) NeonGreen else CH.border, RoundedCornerShape(8.dp))
                                            .clickable { selectedFormat = format }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            format,
                                            color = if (isSelected) NeonGreen else CH.textSecondary,
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

                // Knockout Stage — auto-linked to format
                // Group+Knockout → locked to Semi Finals + Final
                // League+Playoffs → locked to Playoffs
                // Round Robin / Double RR / Tri-Series → user picks
                // Single Knockout / Bilateral / Custom → no knockout picker
                val autoKnockout = when (selectedFormat) {
                    "Group + Knockout" -> "Semi Finals + Final"
                    "League + Playoffs" -> "Playoffs"
                    else -> null
                }
                val showKnockoutPicker = selectedFormat in listOf(
                    "Round Robin", "Double Round Robin", "Tri-Series"
                )
                if (showKnockoutPicker || autoKnockout != null) {
                    item {
                        Text("Knockout Stage", color = CH.textSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (autoKnockout != null) {
                            Text("Auto-set for $selectedFormat", color = CH.textHint, fontSize = 11.sp)
                        } else {
                            Text("How the tournament ends after the league phase", color = CH.textHint, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val knockoutOptions = listOf(
                            "Top Team Champion" to "Top team on points wins — no knockout match",
                            "Top 2 → Final" to "1st vs 2nd play the final",
                            "Semi Finals + Final" to "Top 4: SF1 (1v4) · SF2 (2v3) · Final",
                            "Playoffs" to "Q1 (1v2) · Eliminator (3v4) · Q2 · Final",
                            "Super 4" to "Top 4 play round-robin, then final"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            knockoutOptions.forEach { (label, desc) ->
                                val isSelected = knockoutFormat == label
                                val isLocked = autoKnockout != null && label != autoKnockout
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) NeonGreen.copy(alpha = 0.15f)
                                            else if (isLocked) CH.surface.copy(alpha = 0.4f)
                                            else CH.surface
                                        )
                                        .border(1.dp, if (isSelected) NeonGreen else CH.border, RoundedCornerShape(10.dp))
                                        .then(if (!isLocked) Modifier.clickable { knockoutFormat = label } else Modifier)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(18.dp).clip(CircleShape)
                                            .border(1.5.dp, if (isSelected) NeonGreen else if (isLocked) CH.textHint else CH.textSecondary, CircleShape)
                                            .background(if (isSelected) NeonGreen else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Black))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label,
                                            color = if (isSelected) NeonGreen else if (isLocked) CH.textHint else CH.textPrimary,
                                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(desc,
                                            color = if (isLocked) CH.textHint else CH.textSecondary,
                                            fontSize = 10.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }

                // Match Type
                item {
                    Text("Match Type", color = CH.textSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("T20", "ODI", "Test", "T10", "Custom").forEach { type ->
                            val isSelected = matchType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else CH.bg)
                                    .border(1.dp, if (isSelected) NeonGreen else CH.border, RoundedCornerShape(8.dp))
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
                                    color = if (isSelected) NeonGreen else CH.textSecondary,
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
                        DatePickerField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = "Start Date",
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = "End Date",
                            minDate = if (startDate.isNotBlank()) {
                                try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                        .parse(startDate)?.time
                                } catch (e: Exception) { null }
                            } else null,
                            modifier = Modifier.weight(1f)
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
                        val fullFormat = if (selectedFormat !in listOf("Single Knockout", "Bilateral Series", "Custom Tournament")) {
                            "$selectedFormat | $knockoutFormat"
                        } else {
                            selectedFormat
                        }
                        viewModel.createTournament(
                            TournamentInsert(
                                name = name.trim(),
                                format = fullFormat,
                                venue = venue.trim().ifBlank { null },
                                organizer = organizer.trim().ifBlank { null },
                                startDate = startDate.trim().ifBlank { null },
                                endDate = endDate.trim().ifBlank { null },
                                oversPerMatch = totalOvers.toIntOrNull() ?: 20,
                                matchType = matchType,
                                playersPerSide = 11,
                                maxTeams = maxTeams.toIntOrNull()
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
    } // CricketAnimatedBackground
}