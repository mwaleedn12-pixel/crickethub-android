package com.crickethub.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import kotlin.math.roundToInt
import com.crickethub.ui.theme.*


// ── DLS RESOURCE TABLE (Standard Edition 2002) ───────────────
// Table[oversRemaining][wicketsLost] = resource %
// wicketsLost: 0,1,2,3,4,5,6,7,8,9
private val DLS_TABLE = mapOf(
    50 to listOf(100.0, 93.4, 85.1, 74.9, 62.7, 49.0, 34.9, 22.0, 11.9, 4.7),
    49 to listOf(99.1, 92.6, 84.5, 74.4, 62.5, 48.9, 34.9, 22.0, 11.9, 4.7),
    48 to listOf(98.1, 91.7, 83.8, 74.0, 62.2, 48.8, 34.9, 22.0, 11.9, 4.7),
    47 to listOf(97.1, 90.9, 83.2, 73.5, 61.9, 48.6, 34.9, 22.0, 11.9, 4.7),
    46 to listOf(96.1, 90.0, 82.5, 73.0, 61.6, 48.5, 34.8, 22.0, 11.9, 4.7),
    45 to listOf(95.0, 89.1, 81.8, 72.5, 61.3, 48.4, 34.8, 22.0, 11.9, 4.7),
    44 to listOf(93.9, 88.2, 81.0, 72.0, 61.0, 48.3, 34.8, 22.0, 11.9, 4.7),
    43 to listOf(92.8, 87.3, 80.3, 71.4, 60.7, 48.1, 34.7, 22.0, 11.9, 4.7),
    42 to listOf(91.7, 86.3, 79.5, 70.9, 60.3, 47.9, 34.7, 22.0, 11.9, 4.7),
    41 to listOf(90.5, 85.3, 78.7, 70.3, 59.9, 47.8, 34.6, 22.0, 11.9, 4.7),
    40 to listOf(89.3, 84.2, 77.8, 69.6, 59.5, 47.6, 34.6, 22.0, 11.9, 4.7),
    39 to listOf(88.0, 83.1, 76.9, 69.0, 59.1, 47.4, 34.5, 22.0, 11.9, 4.7),
    38 to listOf(86.7, 82.0, 76.0, 68.3, 58.7, 47.1, 34.5, 21.9, 11.9, 4.7),
    37 to listOf(85.4, 80.9, 75.0, 67.6, 58.2, 46.9, 34.4, 21.9, 11.9, 4.7),
    36 to listOf(84.1, 79.7, 74.1, 66.8, 57.7, 46.6, 34.3, 21.9, 11.9, 4.7),
    35 to listOf(82.7, 78.5, 73.0, 66.0, 57.2, 46.4, 34.2, 21.9, 11.9, 4.7),
    34 to listOf(81.3, 77.2, 72.0, 65.2, 56.6, 46.1, 34.1, 21.9, 11.9, 4.7),
    33 to listOf(79.8, 75.9, 70.9, 64.4, 56.0, 45.8, 34.0, 21.9, 11.9, 4.7),
    32 to listOf(78.3, 74.6, 69.7, 63.5, 55.4, 45.4, 33.9, 21.9, 11.9, 4.7),
    31 to listOf(76.7, 73.2, 68.6, 62.5, 54.8, 45.1, 33.7, 21.9, 11.9, 4.7),
    30 to listOf(75.1, 71.8, 67.3, 61.6, 54.1, 44.7, 33.6, 21.8, 11.9, 4.7),
    29 to listOf(73.5, 70.3, 66.1, 60.5, 53.4, 44.2, 33.4, 21.8, 11.9, 4.7),
    28 to listOf(71.8, 68.8, 64.8, 59.5, 52.6, 43.8, 33.2, 21.8, 11.9, 4.7),
    27 to listOf(70.1, 67.2, 63.4, 58.4, 51.8, 43.3, 33.0, 21.7, 11.9, 4.7),
    26 to listOf(68.3, 65.6, 62.0, 57.2, 50.9, 42.8, 32.8, 21.7, 11.9, 4.7),
    25 to listOf(66.5, 63.9, 60.5, 56.0, 50.0, 42.2, 32.6, 21.6, 11.9, 4.7),
    24 to listOf(64.6, 62.2, 59.0, 54.7, 49.0, 41.6, 32.3, 21.6, 11.9, 4.7),
    23 to listOf(62.7, 60.4, 57.4, 53.4, 48.0, 40.9, 32.0, 21.5, 11.9, 4.7),
    22 to listOf(60.7, 58.6, 55.8, 52.0, 47.0, 40.2, 31.6, 21.4, 11.9, 4.7),
    21 to listOf(58.7, 56.7, 54.1, 50.6, 45.8, 39.4, 31.2, 21.3, 11.9, 4.7),
    20 to listOf(56.6, 54.8, 52.4, 49.1, 44.6, 38.6, 30.8, 21.2, 11.9, 4.7),
    19 to listOf(54.4, 52.8, 50.5, 47.5, 43.4, 37.7, 30.3, 21.1, 11.9, 4.7),
    18 to listOf(52.2, 50.7, 48.6, 45.9, 42.0, 36.8, 29.8, 20.9, 11.9, 4.7),
    17 to listOf(49.9, 48.5, 46.7, 44.1, 40.6, 35.8, 29.2, 20.7, 11.9, 4.7),
    16 to listOf(47.6, 46.3, 44.7, 42.3, 39.1, 34.7, 28.5, 20.5, 11.8, 4.7),
    15 to listOf(45.2, 44.1, 42.6, 40.5, 37.6, 33.5, 27.8, 20.2, 11.8, 4.7),
    14 to listOf(42.7, 41.7, 40.4, 38.5, 35.9, 32.2, 27.0, 19.9, 11.8, 4.7),
    13 to listOf(40.2, 39.3, 38.1, 36.5, 34.2, 30.8, 26.1, 19.5, 11.7, 4.7),
    12 to listOf(37.6, 36.8, 35.8, 34.3, 32.3, 29.4, 25.1, 19.0, 11.6, 4.7),
    11 to listOf(34.9, 34.2, 33.4, 32.1, 30.4, 27.8, 24.0, 18.5, 11.5, 4.7),
    10 to listOf(32.1, 31.5, 30.8, 29.8, 28.3, 26.1, 22.8, 17.9, 11.4, 4.7),
    9 to listOf(29.3, 28.9, 28.2, 27.4, 26.1, 24.2, 21.4, 17.1, 11.2, 4.7),
    8 to listOf(26.4, 26.0, 25.5, 24.8, 23.8, 22.3, 19.9, 16.2, 10.9, 4.7),
    7 to listOf(23.4, 23.1, 22.7, 22.2, 21.4, 20.1, 18.2, 15.2, 10.5, 4.7),
    6 to listOf(20.3, 20.1, 19.8, 19.4, 18.8, 17.8, 16.4, 13.9, 10.1, 4.6),
    5 to listOf(17.2, 17.0, 16.8, 16.5, 16.1, 15.4, 14.3, 12.5, 9.4, 4.6),
    4 to listOf(13.9, 13.8, 13.7, 13.5, 13.2, 12.7, 12.0, 10.7, 8.4, 4.5),
    3 to listOf(10.6, 10.5, 10.4, 10.3, 10.2, 9.9, 9.5, 8.7, 7.2, 4.2),
    2 to listOf(7.2, 7.1, 7.1, 7.0, 7.0, 6.8, 6.6, 6.2, 5.5, 3.7),
    1 to listOf(3.6, 3.6, 3.6, 3.6, 3.6, 3.5, 3.5, 3.4, 3.2, 2.5),
    0 to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
)

// G50 value — standard for ODI
private const val G50 = 245.0

// Get resource % for given overs remaining and wickets lost
fun getDLSResource(oversRemaining: Int, wicketsLost: Int): Double {
    val clampedOvers = oversRemaining.coerceIn(0, 50)
    val clampedWickets = wicketsLost.coerceIn(0, 9)
    return DLS_TABLE[clampedOvers]?.get(clampedWickets) ?: 0.0
}

// Interpolate for non-integer overs
fun getDLSResourceExact(oversRemaining: Double, wicketsLost: Int): Double {
    val fullOvers = oversRemaining.toInt()
    val fraction = oversRemaining - fullOvers
    val r1 = getDLSResource(fullOvers, wicketsLost)
    val r2 = getDLSResource((fullOvers + 1).coerceAtMost(50), wicketsLost)
    return r1 + (r2 - r1) * fraction
}

data class Interruption(
    val id: Int,
    val oversRemainingAtStop: Double,
    val wicketsLostAtStop: Int,
    val oversRemainingAtRestart: Double
)

data class DLSResult(
    val team1Resource: Double,
    val team2Resource: Double,
    val parScore: Int,
    val targetScore: Int,
    val method: String,
    val explanation: List<String>
)

fun calculateDLS(
    team1Score: Int,
    team1TotalOvers: Int,
    team1Interruptions: List<Interruption>,
    team2TotalOvers: Int,
    team2Interruptions: List<Interruption>
): DLSResult {
    val explanation = mutableListOf<String>()

    // ── TEAM 1 RESOURCES ─────────────────────────────────────
    var team1Resource = getDLSResource(team1TotalOvers, 0)
    explanation.add("Team 1 started with ${team1TotalOvers} overs → ${team1Resource}%")

    for (inter in team1Interruptions) {
        val resourceAtStop = getDLSResourceExact(inter.oversRemainingAtStop, inter.wicketsLostAtStop)
        val resourceAtRestart = getDLSResourceExact(inter.oversRemainingAtRestart, inter.wicketsLostAtStop)
        val lost = resourceAtStop - resourceAtRestart
        team1Resource -= lost
        explanation.add("Team 1 interruption: ${inter.oversRemainingAtStop} ov left, ${inter.wicketsLostAtStop} wkts lost → lost ${String.format("%.1f", lost)}% (${String.format("%.1f", resourceAtStop)}% - ${String.format("%.1f", resourceAtRestart)}%)")
    }
    explanation.add("Team 1 total resource (R1) = ${String.format("%.1f", team1Resource)}%")

    // ── TEAM 2 RESOURCES ─────────────────────────────────────
    var team2Resource = getDLSResource(team2TotalOvers, 0)
    explanation.add("Team 2 started with ${team2TotalOvers} overs → ${team2Resource}%")

    for (inter in team2Interruptions) {
        val resourceAtStop = getDLSResourceExact(inter.oversRemainingAtStop, inter.wicketsLostAtStop)
        val resourceAtRestart = getDLSResourceExact(inter.oversRemainingAtRestart, inter.wicketsLostAtStop)
        val lost = resourceAtStop - resourceAtRestart
        team2Resource -= lost
        explanation.add("Team 2 interruption: ${inter.oversRemainingAtStop} ov left, ${inter.wicketsLostAtStop} wkts lost → lost ${String.format("%.1f", lost)}%")
    }
    explanation.add("Team 2 total resource (R2) = ${String.format("%.1f", team2Resource)}%")

    // ── TARGET CALCULATION ────────────────────────────────────
    val parScoreRaw: Double
    val method: String

    when {
        team2Resource < team1Resource -> {
            // Reduced target
            parScoreRaw = team1Score * (team2Resource / team1Resource)
            method = "Reduced (R2 < R1): ${team1Score} × ${String.format("%.1f", team2Resource)} / ${String.format("%.1f", team1Resource)}"
            explanation.add("R2 < R1 → Reduced target: $team1Score × ${String.format("%.1f", team2Resource)}% / ${String.format("%.1f", team1Resource)}% = ${String.format("%.3f", parScoreRaw)}")
        }
        team2Resource == team1Resource -> {
            parScoreRaw = team1Score.toDouble()
            method = "No change (R2 = R1)"
            explanation.add("R2 = R1 → No adjustment needed")
        }
        else -> {
            // Increased target — Standard Edition formula
            val extraResource = team2Resource - team1Resource
            parScoreRaw = team1Score + G50 * (extraResource / 100.0)
            method = "Increased (R2 > R1): ${team1Score} + ${G50} × (${String.format("%.1f", extraResource)} / 100)"
            explanation.add("R2 > R1 → Increased target: $team1Score + $G50 × ${String.format("%.1f", extraResource)}/100 = ${String.format("%.3f", parScoreRaw)}")
        }
    }

    val parScore = parScoreRaw.toInt() // Floor = par (tie)
    val targetScore = parScore + 1      // Target to WIN

    explanation.add("Par score (tie) = ${parScore}")
    explanation.add("Target score (win) = ${targetScore}")

    return DLSResult(
        team1Resource = team1Resource,
        team2Resource = team2Resource,
        parScore = parScore,
        targetScore = targetScore,
        method = method,
        explanation = explanation
    )
}

@Composable
fun DLSCalculatorScreen(onBack: () -> Unit) {
    var team1Score by remember { mutableStateOf("") }
    var team1TotalOvers by remember { mutableStateOf("50") }
    var team2TotalOvers by remember { mutableStateOf("50") }
    var team1Interruptions by remember { mutableStateOf<List<Interruption>>(emptyList()) }
    var team2Interruptions by remember { mutableStateOf<List<Interruption>>(emptyList()) }
    var result by remember { mutableStateOf<DLSResult?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var showResourceTable by remember { mutableStateOf(false) }

    // Add interruption dialog state
    var showAddInterruption by remember { mutableStateOf(false) }
    var addingForTeam by remember { mutableStateOf(1) }
    var interruptionOversStop by remember { mutableStateOf("") }
    var interruptionWickets by remember { mutableStateOf("0") }
    var interruptionOversRestart by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
        cursorColor = NeonGreen, focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextSecondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    // Add interruption dialog
    if (showAddInterruption) {
        AlertDialog(
            onDismissRequest = { showAddInterruption = false },
            containerColor = SurfaceCard,
            title = {
                Text(
                    "Add Interruption — Team $addingForTeam",
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter details at the point rain stopped play:",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = interruptionOversStop,
                        onValueChange = { interruptionOversStop = it },
                        label = { Text("Overs remaining when stopped") },
                        placeholder = { Text("e.g. 31.0", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = interruptionWickets,
                        onValueChange = { if ((it.toIntOrNull() ?: 0) in 0..9) interruptionWickets = it },
                        label = { Text("Wickets lost at stop (0-9)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = interruptionOversRestart,
                        onValueChange = { interruptionOversRestart = it },
                        label = { Text("Overs remaining at restart") },
                        placeholder = { Text("e.g. 17.0", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    Text(
                        "⚠️ Overs at restart must be ≤ overs at stop",
                        color = AmberColor, fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stop = interruptionOversStop.toDoubleOrNull()
                        val restart = interruptionOversRestart.toDoubleOrNull()
                        val wickets = interruptionWickets.toIntOrNull() ?: 0
                        if (stop != null && restart != null && restart <= stop) {
                            val newInter = Interruption(
                                id = System.currentTimeMillis().toInt(),
                                oversRemainingAtStop = stop,
                                wicketsLostAtStop = wickets,
                                oversRemainingAtRestart = restart
                            )
                            if (addingForTeam == 1) {
                                team1Interruptions = team1Interruptions + newInter
                            } else {
                                team2Interruptions = team2Interruptions + newInter
                            }
                            interruptionOversStop = ""
                            interruptionWickets = "0"
                            interruptionOversRestart = ""
                            showAddInterruption = false
                            result = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) { Text("Add", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddInterruption = false }) {
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
                "DLS Calculator",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { showResourceTable = !showResourceTable },
                colors = ButtonDefaults.textButtonColors(contentColor = NeonBlue)
            ) { Text("Resource Table", fontSize = 12.sp) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resource table
            if (showResourceTable) {
                item {
                    DLSResourceTableView()
                }
            }

            // Match format info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonBlue.copy(alpha = 0.1f))
                        .border(1.dp, NeonBlue, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text("ℹ️ DLS Standard Edition", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Uses official 2002 resource table. For ODIs: min 20 overs each. For T20: min 5 overs each.",
                        color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }

            // Team 1 section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🏏 Team 1 (Batting First)", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = BorderColor)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = team1Score,
                            onValueChange = { if (it.all { c -> c.isDigit() }) team1Score = it },
                            label = { Text("Final Score") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = team1TotalOvers,
                            onValueChange = { if (it.all { c -> c.isDigit() }) team1TotalOvers = it },
                            label = { Text("Overs allocated") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = fieldColors
                        )
                    }

                    // Interruptions
                    Text("Rain interruptions during Team 1 innings:", color = TextSecondary, fontSize = 12.sp)

                    team1Interruptions.forEachIndexed { index, inter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Interruption ${index + 1}",
                                    color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Stopped: ${inter.oversRemainingAtStop} ov left, ${inter.wicketsLostAtStop} wkts lost",
                                    color = TextSecondary, fontSize = 11.sp
                                )
                                Text(
                                    "Restart: ${inter.oversRemainingAtRestart} ov remaining",
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    team1Interruptions = team1Interruptions.filter { it.id != inter.id }
                                    result = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            addingForTeam = 1
                            showAddInterruption = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Interruption (Team 1)", fontSize = 12.sp)
                    }
                }
            }

            // Team 2 section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🏏 Team 2 (Batting Second)", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = BorderColor)

                    OutlinedTextField(
                        value = team2TotalOvers,
                        onValueChange = { if (it.all { c -> c.isDigit() }) team2TotalOvers = it },
                        label = { Text("Overs allocated to Team 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )

                    Text("Rain interruptions during Team 2 innings:", color = TextSecondary, fontSize = 12.sp)

                    team2Interruptions.forEachIndexed { index, inter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Interruption ${index + 1}",
                                    color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Stopped: ${inter.oversRemainingAtStop} ov left, ${inter.wicketsLostAtStop} wkts lost",
                                    color = TextSecondary, fontSize = 11.sp
                                )
                                Text(
                                    "Restart: ${inter.oversRemainingAtRestart} ov remaining",
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    team2Interruptions = team2Interruptions.filter { it.id != inter.id }
                                    result = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            addingForTeam = 2
                            showAddInterruption = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Interruption (Team 2)", fontSize = 12.sp)
                    }
                }
            }

            // Calculate button
            item {
                Button(
                    onClick = {
                        val score = team1Score.toIntOrNull()
                        val t1Overs = team1TotalOvers.toIntOrNull()
                        val t2Overs = team2TotalOvers.toIntOrNull()
                        if (score != null && t1Overs != null && t2Overs != null) {
                            result = calculateDLS(
                                team1Score = score,
                                team1TotalOvers = t1Overs,
                                team1Interruptions = team1Interruptions,
                                team2TotalOvers = t2Overs,
                                team2Interruptions = team2Interruptions
                            )
                        }
                    },
                    enabled = team1Score.isNotBlank() && team1TotalOvers.isNotBlank() && team2TotalOvers.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Calculate DLS Target", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Result
            result?.let { r ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonGreen.copy(alpha = 0.1f))
                            .border(2.dp, NeonGreen, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📊 DLS Result", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = NeonGreen.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("R1 (Team 1)", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    "${String.format("%.1f", r.team1Resource)}%",
                                    color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("R2 (Team 2)", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    "${String.format("%.1f", r.team2Resource)}%",
                                    color = NeonBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = NeonGreen.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Par Score (Tie)", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    "${r.parScore}",
                                    color = AmberColor, fontSize = 28.sp, fontWeight = FontWeight.Bold
                                )
                                Text("Score to tie", color = TextSecondary, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Target (Win)", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    "${r.targetScore}",
                                    color = NeonGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold
                                )
                                Text("Score to win", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        HorizontalDivider(color = NeonGreen.copy(alpha = 0.3f))

                        Text(
                            "Method: ${r.method}",
                            color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp
                        )

                        // Show/hide explanation
                        TextButton(
                            onClick = { showExplanation = !showExplanation },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonBlue)
                        ) {
                            Text(
                                if (showExplanation) "Hide step-by-step ▲" else "Show step-by-step ▼",
                                fontSize = 12.sp
                            )
                        }

                        if (showExplanation) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Step-by-step calculation:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                r.explanation.forEachIndexed { index, step ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    ) {
                                        Text("${index + 1}.", color = NeonGreen, fontSize = 11.sp, modifier = Modifier.width(20.dp))
                                        Text(step, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick reference
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Quick Reference", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = BorderColor)

                        val t1s = team1Score.toIntOrNull() ?: 0
                        val t2o = team2TotalOvers.toIntOrNull() ?: 50

                        listOf(0, 1, 2, 3, 4, 5).forEach { wickets ->
                            val res = getDLSResource(t2o, wickets)
                            val r1 = result?.team1Resource ?: 100.0
                            val par = (t1s * res / r1).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$t2o ov, $wickets wkt${if (wickets != 1) "s" else ""} lost:", color = TextSecondary, fontSize = 12.sp)
                                Text("Par: $par | Target: ${par + 1}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Reset button
            item {
                OutlinedButton(
                    onClick = {
                        team1Score = ""
                        team1TotalOvers = "50"
                        team2TotalOvers = "50"
                        team1Interruptions = emptyList()
                        team2Interruptions = emptyList()
                        result = null
                        showExplanation = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Reset Calculator") }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ── RESOURCE TABLE VIEW ──────────────────────────────────────

@Composable
fun DLSResourceTableView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        Text(
            "DLS Resource Table (Standard Edition)",
            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Text("% resources remaining", color = TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Ov", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(28.dp))
            listOf("0W", "1W", "2W", "3W", "4W", "5W", "6W", "7W", "8W", "9W").forEach { header ->
                Text(header, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

        // Show key overs
        listOf(50, 45, 40, 35, 30, 25, 20, 15, 10, 5, 1).forEach { overs ->
            val resources = DLS_TABLE[overs] ?: return@forEach
            val isHighlighted = overs % 10 == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isHighlighted) NeonGreen.copy(alpha = 0.05f) else Color.Transparent)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    "$overs",
                    color = if (isHighlighted) NeonGreen else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(28.dp)
                )
                resources.forEach { value ->
                    Text(
                        String.format("%.1f", value),
                        color = TextPrimary, fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "W = Wickets Lost. 10W = 0 wickets lost (all in hand). G50 = $G50 (used when R2 > R1)",
            color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp
        )
    }
}