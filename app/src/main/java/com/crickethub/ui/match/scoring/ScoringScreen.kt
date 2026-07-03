package com.crickethub.ui.match.scoring

import android.content.Intent
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Player
import com.crickethub.data.model.ScoringUiState

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
fun ScoringScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: ScoringViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showWicketDialog by remember { mutableStateOf(false) }
    var showExtrasDialog by remember { mutableStateOf(false) }
    var showSelectBatsman by remember { mutableStateOf(false) }
    var showSelectBowler by remember { mutableStateOf(false) }
    var showSelectNonStriker by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    val needStriker = uiState.striker == null && !uiState.isLoading && uiState.innings != null
    val needBowler = uiState.currentBowler == null && !uiState.isLoading && uiState.innings != null
    val needNonStriker = uiState.nonStriker == null && !uiState.isLoading && uiState.innings != null

    fun shareScore() {
        val last6 = uiState.last6Balls.joinToString(" ")
        val text = buildString {
            appendLine("🏏 CricketHub LIVE")
            appendLine("Score: ${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} overs)")
            appendLine("CRR: ${"%.2f".format(uiState.runRate)}")
            if (uiState.last6Balls.isNotEmpty()) appendLine("Last 6: $last6")
            uiState.striker?.let {
                appendLine("🏏 ${it.fullName}*: ${uiState.batsmanStats[it.id]?.runs ?: 0}(${uiState.batsmanStats[it.id]?.balls ?: 0})")
            }
            uiState.currentBowler?.let {
                appendLine("🎳 ${it.fullName}: ${uiState.bowlerStats[it.id]?.overs ?: "0.0"}-${uiState.bowlerStats[it.id]?.runs ?: 0}-${uiState.bowlerStats[it.id]?.wickets ?: 0}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Score"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
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
                    "Live Scoring",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ErrorRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else {
                ScoreHeader(uiState = uiState, onShare = { shareScore() })
                Last6BallsRow(balls = uiState.last6Balls)
                Spacer(modifier = Modifier.height(8.dp))

                CurrentBatsmenRow(
                    striker = uiState.striker,
                    nonStriker = uiState.nonStriker,
                    batsmanStats = uiState.batsmanStats,
                    strikerClickable = needStriker,
                    nonStrikerClickable = needNonStriker,
                    onChangeStriker = { if (needStriker) showSelectBatsman = true },
                    onChangeNonStriker = { if (needNonStriker) showSelectNonStriker = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CurrentBowlerRow(
                    bowler = uiState.currentBowler,
                    bowlerStats = uiState.bowlerStats,
                    bowlerClickable = needBowler,
                    onChangeBowler = { if (needBowler) showSelectBowler = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (uiState.error != null) {
                    Text(
                        uiState.error ?: "",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (!needStriker && !needBowler && !needNonStriker) {
                    ScoringButtons(
                        isLoading = uiState.isLoading,
                        onRuns = { runs -> viewModel.recordBall(runsOffBat = runs) },
                        onWicket = { showWicketDialog = true },
                        onExtras = { showExtrasDialog = true },
                        onUndo = { viewModel.undoLastBall() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Striker select
        if (needStriker || showSelectBatsman) {
            PlayerSelectDialog(
                title = "Select Striker",
                players = uiState.battingTeamPlayers.filter { player ->
                    val stats = uiState.batsmanStats[player.id]
                    stats?.isOut != true && player.id != uiState.nonStriker?.id
                },
                onPlayerSelected = {
                    viewModel.setStriker(it)
                    showSelectBatsman = false
                },
                onDismiss = { showSelectBatsman = false }
            )
        }

        // Non-striker select
        if (needNonStriker || showSelectNonStriker) {
            PlayerSelectDialog(
                title = "Select Non-Striker",
                players = uiState.battingTeamPlayers.filter { player ->
                    val stats = uiState.batsmanStats[player.id]
                    stats?.isOut != true && player.id != uiState.striker?.id
                },
                onPlayerSelected = {
                    viewModel.setNonStriker(it)
                    showSelectNonStriker = false
                },
                onDismiss = { showSelectNonStriker = false }
            )
        }

        // Bowler select — max overs check + consecutive over check
        if (needBowler || showSelectBowler) {
            val totalOvers = uiState.match?.totalOvers ?: 20
            val lastBowlerId = uiState.balls
                .filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
                .lastOrNull()?.bowlerId

            PlayerSelectDialog(
                title = "Select Bowler (Max ${viewModel.getMaxOversPerBowler(totalOvers)} overs)",
                players = uiState.bowlingTeamPlayers.filter { player ->
                    val notConsecutive = player.id != lastBowlerId
                    val canBowl = viewModel.canBowlerBowl(player.id, totalOvers)
                    notConsecutive && canBowl
                },
                onPlayerSelected = {
                    viewModel.setBowler(it)
                    showSelectBowler = false
                },
                onDismiss = { showSelectBowler = false }
            )
        }

        // Wicket dialog
        if (showWicketDialog) {
            WicketDialog(
                onDismiss = { showWicketDialog = false },
                onConfirm = { wicketType, runs ->
                    viewModel.recordBall(runsOffBat = runs, isWicket = true, wicketType = wicketType)
                    showWicketDialog = false
                }
            )
        }

        // Extras dialog
        if (showExtrasDialog) {
            ExtrasDialog(
                onDismiss = { showExtrasDialog = false },
                onConfirm = { extrasType, runs ->
                    viewModel.recordBall(runsOffBat = 0, extrasType = extrasType, extrasRuns = runs)
                    showExtrasDialog = false
                }
            )
        }
    }
}

@Composable
fun ScoreHeader(uiState: ScoringUiState, onShare: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${uiState.totalRuns}/${uiState.totalWickets}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${uiState.currentOver}.${uiState.currentBall}",
                    fontSize = 24.sp,
                    color = NeonGreen,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CRR: ${"%.2f".format(uiState.runRate)}", color = TextSecondary, fontSize = 13.sp)
                uiState.match?.let {
                    Text("${it.totalOvers} overs", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onShare,
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen)
                ) {
                    Text("📤 Share", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun Last6BallsRow(balls: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "This over: ",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        balls.forEach { ball ->
            val (bgColor, textColor) = when (ball) {
                "W" -> ErrorRed to Color.White
                "4" -> NeonBlue to Color.White
                "6" -> NeonGreen to Color.Black
                "Wd", "Nb" -> AmberColor to Color.Black
                "0" -> SurfaceCard to TextSecondary
                else -> SurfaceCard to TextPrimary
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, BorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(ball, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CurrentBatsmenRow(
    striker: Player?,
    nonStriker: Player?,
    batsmanStats: Map<String, com.crickethub.data.model.BatsmanStats>,
    strikerClickable: Boolean,
    nonStrikerClickable: Boolean,
    onChangeStriker: () -> Unit,
    onChangeNonStriker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, if (strikerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                .then(if (strikerClickable) Modifier.clickable { onChangeStriker() } else Modifier)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("*", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    striker?.fullName ?: "Select Batsman",
                    color = if (striker != null) TextPrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            striker?.let { s ->
                val stats = batsmanStats[s.id]
                if (stats != null) {
                    Text(
                        "${stats.runs}(${stats.balls}) SR: ${"%.1f".format(stats.strikeRate)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, if (nonStrikerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                .then(if (nonStrikerClickable) Modifier.clickable { onChangeNonStriker() } else Modifier)
                .padding(10.dp)
        ) {
            Text(
                nonStriker?.fullName ?: "Select Non-Striker",
                color = if (nonStriker != null) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            nonStriker?.let { ns ->
                val stats = batsmanStats[ns.id]
                if (stats != null) {
                    Text("${stats.runs}(${stats.balls})", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun CurrentBowlerRow(
    bowler: Player?,
    bowlerStats: Map<String, com.crickethub.data.model.BowlerStats>,
    bowlerClickable: Boolean,
    onChangeBowler: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, if (bowlerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
            .then(if (bowlerClickable) Modifier.clickable { onChangeBowler() } else Modifier)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            bowler?.fullName ?: "Select Bowler",
            color = if (bowler != null) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        bowler?.let { b ->
            val stats = bowlerStats[b.id]
            if (stats != null) {
                Text(
                    "${stats.overs}-${stats.runs}-${stats.wickets} Eco: ${"%.1f".format(stats.economy)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ScoringButtons(
    isLoading: Boolean,
    onRuns: (Int) -> Unit,
    onWicket: () -> Unit,
    onExtras: () -> Unit,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1, 2, 3, 4, 6).forEach { runs ->
                val bgColor = when (runs) {
                    4 -> NeonBlue.copy(alpha = 0.8f)
                    6 -> NeonGreen.copy(alpha = 0.8f)
                    else -> SurfaceCard
                }
                val textColor = when (runs) {
                    4, 6 -> Color.White
                    0 -> TextSecondary
                    else -> TextPrimary
                }
                Button(
                    onClick = { onRuns(runs) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor,
                        contentColor = textColor,
                        disabledContainerColor = bgColor.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        if (runs == 0) "•" else runs.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onWicket,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7F1D1D),
                    contentColor = Color(0xFFFCA5A5)
                )
            ) { Text("WICKET", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

            Button(
                onClick = onExtras,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF78350F),
                    contentColor = Color(0xFFFCD34D)
                )
            ) { Text("EXTRAS", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

            Button(
                onClick = onUndo,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceCard,
                    contentColor = TextSecondary
                )
            ) { Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}

@Composable
fun PlayerSelectDialog(
    title: String,
    players: List<Player>,
    onPlayerSelected: (Player) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .clickable { onPlayerSelected(player) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                player.jerseyNo?.toString() ?: "-",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(player.fullName, color = TextPrimary, fontSize = 14.sp)
                            player.role?.let {
                                Text(
                                    it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                if (players.isEmpty()) {
                    item {
                        Text(
                            "No players available",
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun WicketDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runsBeforeWicket by remember { mutableStateOf(0) }

    val wicketTypes = listOf(
        "bowled" to "Bowled", "caught" to "Caught", "lbw" to "LBW",
        "run_out" to "Run Out", "stumped" to "Stumped", "hit_wicket" to "Hit Wicket"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Wicket!", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How was the batsman dismissed?", color = TextSecondary, fontSize = 13.sp)
                wicketTypes.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (value, label) ->
                            val selected = selectedType == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) ErrorRed.copy(alpha = 0.2f) else BackgroundDark)
                                    .border(1.dp, if (selected) ErrorRed else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { selectedType = value }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (selected) ErrorRed else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Text("Runs before wicket:", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..4).forEach { r ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (runsBeforeWicket == r) NeonGreen.copy(alpha = 0.3f) else BackgroundDark)
                                .border(1.dp, if (runsBeforeWicket == r) NeonGreen else BorderColor, CircleShape)
                                .clickable { runsBeforeWicket = r },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                r.toString(),
                                color = if (runsBeforeWicket == r) NeonGreen else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedType?.let { onConfirm(it, runsBeforeWicket) } },
                enabled = selectedType != null,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) { Text("Confirm", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ExtrasDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runs by remember { mutableStateOf(1) }

    val extrasTypes = listOf(
        "wide" to "Wide", "no_ball" to "No Ball", "bye" to "Bye", "leg_bye" to "Leg Bye"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Extras", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    extrasTypes.forEach { (value, label) ->
                        val selected = selectedType == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AmberColor.copy(alpha = 0.2f) else BackgroundDark)
                                .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { selectedType = value }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) AmberColor else TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Text("Runs:", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { r ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (runs == r) AmberColor.copy(alpha = 0.3f) else BackgroundDark)
                                .border(1.dp, if (runs == r) AmberColor else BorderColor, CircleShape)
                                .clickable { runs = r },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                r.toString(),
                                color = if (runs == r) AmberColor else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedType?.let { onConfirm(it, runs) } },
                enabled = selectedType != null,
                colors = ButtonDefaults.buttonColors(containerColor = AmberColor)
            ) { Text("Confirm", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}