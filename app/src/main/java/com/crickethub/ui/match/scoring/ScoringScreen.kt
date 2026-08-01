package com.crickethub.ui.match.scoring

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Score
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Player
import com.crickethub.data.model.ScoringUiState
import com.crickethub.data.sync.SyncState
// DLS removed — future update
import io.github.jan.supabase.auth.auth
import com.crickethub.ui.theme.*


@Composable
fun ScoringScreen(
    matchId: String,
    onBack: () -> Unit,
    onInningsComplete: () -> Unit = {},
    onViewScorecard: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    viewModel: ScoringViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    var resumeKey by remember { mutableStateOf(0) }

    // Detect when we return to this screen
    // navBackStackEntry removed






    val context = LocalContext.current

    var showWicketDialog by remember { mutableStateOf(false) }
    var showExtrasDialog by remember { mutableStateOf(false) }
    var showSelectBatsman by remember { mutableStateOf(false) }
    var showSelectBowler by remember { mutableStateOf(false) }
    var showSelectNonStriker by remember { mutableStateOf(false) }
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var showMissedChanceDialog by remember { mutableStateOf(false) }
    var showManualEditDialog by remember { mutableStateOf(false) }
    var isFreeHit by remember { mutableStateOf(false) }
    var lastResumeTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(matchId, resumeKey) {
        // If returning from background after 30+ seconds, refresh session and force reload
        val elapsed = System.currentTimeMillis() - lastResumeTime
        if (elapsed > 30_000) {
            // Try to refresh session — retry once on failure
            var refreshed = false
            for (attempt in 1..2) {
                try {
                    com.crickethub.data.remote.SupabaseClient.client.auth.refreshCurrentSession()
                    refreshed = true
                    break
                } catch (e: Exception) {
                    android.util.Log.w("CricketHub", "Session refresh attempt $attempt failed: ${e.message}")
                    if (attempt == 1) kotlinx.coroutines.delay(1000)
                }
            }
            if (!refreshed) {
                android.util.Log.e("CricketHub", "Session refresh failed after 2 attempts, trying reload anyway")
            }
            viewModel.resumeMatch(matchId, force = true)
        } else {
            viewModel.resumeMatch(matchId)
        }
        lastResumeTime = System.currentTimeMillis()
    }

    // Re-run resumeMatch every time screen becomes visible again
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.inningsComplete) {
        if (uiState.inningsComplete) {
            // Small settle so the final ball's DB write lands before we read innings
            // state to decide the next one. Without it the transition can read a
            // not-yet-committed innings and appear to hang on the loading spinner.
            kotlinx.coroutines.delay(300)
            viewModel.checkAndStartNextInnings(matchId) { onInningsComplete() }
        }
    }

    LaunchedEffect(uiState.matchComplete) {
        if (uiState.matchComplete) onInningsComplete()
    }

    LaunchedEffect(uiState.balls.size) {
        val lastBall = uiState.balls.lastOrNull()
        isFreeHit = lastBall?.extrasType == "no_ball" && (uiState.match?.freeHitOnNoball == true)
    }

    val needStriker = uiState.striker == null && !uiState.isLoading && uiState.innings != null
    val needBowler = uiState.currentBowler == null && !uiState.isLoading && uiState.innings != null
    val needNonStriker = uiState.nonStriker == null && !uiState.isLoading && uiState.innings != null
    val isSecondInnings = uiState.innings?.inningsNo == 2
    val currentWickets = uiState.innings?.totalWickets ?: 0

    // ── Ball popup (+1, SIX!, WICKET, WIDE ...) ───────────────────────────────
    var popupLabel by remember { mutableStateOf<String?>(null) }
    var popupColor by remember { mutableStateOf(NeonGreen) }
    var popupKey by remember { mutableStateOf(0) }
    var popupBig by remember { mutableStateOf(false) }
    var milestoneLabel by remember { mutableStateOf<String?>(null) }
    var milestoneColor by remember { mutableStateOf(NeonGreen) }
    var milestoneKey by remember { mutableStateOf(0) }
    var lastPopupBallCount by remember { mutableStateOf(-1) }
    // Fire only when the ball count grows by exactly 1 (a freshly scored ball) — not
    // on resume, which loads many balls at once, and not on undo, which shrinks it.
    LaunchedEffect(uiState.balls.size) {
        val size = uiState.balls.size
        val prev = lastPopupBallCount
        lastPopupBallCount = size
        if (prev == -1) return@LaunchedEffect          // first load / resume: no popup
        if (size != prev + 1) return@LaunchedEffect     // undo or bulk change: no popup
        val last = uiState.balls.lastOrNull() ?: return@LaunchedEffect
        val milestone = detectMilestone(uiState, last)
        if (milestone != null) {
            milestoneLabel = milestone.first
            milestoneColor = milestone.second
            milestoneKey++
        } else {
            val (label, color) = ballPopupLabel(last)
            popupLabel = label
            popupColor = color
            popupKey++
        }
    }

    fun shareScore() {
        val text = buildString {
            appendLine("🏏 CricketHub LIVE")
            appendLine("Score: ${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)")
            appendLine("CRR: ${"%.2f".format(uiState.runRate)}")
            if (uiState.last6Balls.isNotEmpty()) appendLine("This over: ${uiState.last6Balls.joinToString(" ")}")
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

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                // ── TOP BAR ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Text(
                        "Live Scoring",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onViewScorecard) {
                        Icon(Icons.Default.Score, contentDescription = "Scorecard", tint = NeonBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onViewAnalytics) {
                        Icon(Icons.Default.BarChart, contentDescription = "Analytics", tint = PurpleColor, modifier = Modifier.size(20.dp))
                    }
                    if (isFreeHit) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonGreen)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("FREE HIT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ErrorRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // Sync status indicator
                    SyncIndicator(syncState = syncState)
                }

                // Offline banner
                if (!isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmberColor.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (syncState == SyncState.PENDING) "⚡ Offline Mode — changes will sync when online"
                            else "⚡ Offline Mode",
                            color = AmberColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (syncState == SyncState.SYNCING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Syncing...", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                } else {

                    ScoreHeader(uiState = uiState, onShare = { shareScore() },
                        popupLabel = popupLabel, popupColor = popupColor, popupKey = popupKey, popupBig = popupBig)

                    Last6BallsRow(balls = uiState.last6Balls)
                    Spacer(modifier = Modifier.height(2.dp))

                    // Partnership + last wicket + extras
                    HeaderContextRow(uiState = uiState)
                    Spacer(modifier = Modifier.height(2.dp))

                    // Ball-by-ball timeline (reverse chronological, over separators)
                    ScoringBallTimeline(uiState.balls)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Batters + bowler compact table
                    ScoringPlayersTable(
                        uiState = uiState,
                        strikerClickable = needStriker,
                        nonStrikerClickable = needNonStriker,
                        bowlerClickable = needBowler,
                        onChangeStriker = { if (needStriker) showSelectBatsman = true },
                        onChangeNonStriker = { if (needNonStriker) showSelectNonStriker = true },
                        onChangeBowler = { if (needBowler) showSelectBowler = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.error?.let {
                        Text(it, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    if (uiState.inningsComplete || uiState.matchComplete) {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(NeonGreen.copy(alpha = 0.15f)).padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (uiState.matchComplete) "Match Complete! Loading summary..."
                                    else "Innings Complete! Starting next innings...",
                                    color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!needStriker && !needBowler && !needNonStriker &&
                        !uiState.inningsComplete && !uiState.matchComplete
                    ) {
                        ScoringButtons(
                            isLoading = uiState.isLoading,
                            isFreeHit = isFreeHit,
                            onRuns = { runs -> viewModel.recordBall(runsOffBat = runs) },
                            onWicket = { if (!isFreeHit) showWicketDialog = true },
                            onExtras = { showExtrasDialog = true },
                            onUndo = { viewModel.undoLastBall() },
                            onPenalty = { showPenaltyDialog = true },
                            onManualEdit = { showManualEditDialog = true },
                            onMissedChance = { showMissedChanceDialog = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Milestone celebration overlay — floats near the top, above content, non-blocking
            MilestonePopup(
                label = milestoneLabel,
                color = milestoneColor,
                triggerKey = milestoneKey,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 130.dp)
            )

            // ── DIALOGS ───────────────────────────────────────────────────

            if (needStriker || showSelectBatsman) {
                PlayerSelectDialog(
                    title = "Select Striker",
                    players = uiState.battingTeamPlayers.filter { player ->
                        val stats = uiState.batsmanStats[player.id]
                        stats?.isOut != true && player.id != uiState.nonStriker?.id
                    },
                    onPlayerSelected = { viewModel.setStriker(it); showSelectBatsman = false },
                    onDismiss = { showSelectBatsman = false }
                )
            }

            if (needNonStriker || showSelectNonStriker) {
                PlayerSelectDialog(
                    title = "Select Non-Striker",
                    players = uiState.battingTeamPlayers.filter { player ->
                        val stats = uiState.batsmanStats[player.id]
                        stats?.isOut != true && player.id != uiState.striker?.id
                    },
                    onPlayerSelected = { viewModel.setNonStriker(it); showSelectNonStriker = false },
                    onDismiss = { showSelectNonStriker = false }
                )
            }

            if (needBowler || showSelectBowler) {
                val totalOvers = uiState.match?.totalOvers ?: 20
                val maxPerBowler = viewModel.getMaxOversPerBowler(totalOvers)
                val lastBowlerId = uiState.balls
                    .filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
                    .lastOrNull()?.bowlerId
                val eligibleBowlers = uiState.bowlingTeamPlayers.filter { player ->
                    player.id != lastBowlerId &&
                            viewModel.canBowlerBowl(player.id, totalOvers) &&
                            player.role?.lowercase() !in listOf("wicketkeeper", "wicket keeper", "wicket_keeper")
                }
                // Compute remaining overs for each bowler
                val bowlerOversMap = eligibleBowlers.associate { player ->
                    val bowled = uiState.balls.count { it.bowlerId == player.id && it.extrasType != "wide" && it.extrasType != "no_ball" } / 6
                    player.id to (maxPerBowler - bowled)
                }
                BowlerSelectDialog(
                    title = "Select Bowler (Max $maxPerBowler overs)",
                    players = eligibleBowlers,
                    remainingOvers = bowlerOversMap,
                    onPlayerSelected = { viewModel.setBowler(it); showSelectBowler = false },
                    onDismiss = { showSelectBowler = false }
                )
            }

            if (uiState.showSuperOverDecision) {
                AlertDialog(
                    onDismissRequest = { },
                    containerColor = SurfaceCard,
                    title = { Text("Super Over Tied!", color = AmberColor, fontWeight = FontWeight.Bold) },
                    text = { Text("The super over ended level. How do you want to resolve it?",
                        color = TextSecondary, fontSize = 14.sp) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.startAnotherSuperOver(matchId) }) {
                            Text("Play Another", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { viewModel.resolveSuperOverAsComplete() }) {
                                Text("Declare Tie", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.resolveByBoundaryCount(matchId) }) {
                                Text("Boundary Count", color = NeonBlue)
                            }
                        }
                    }
                )
            }

            if (showWicketDialog) {
                val keeper = uiState.bowlingTeamPlayers.firstOrNull {
                    it.role?.lowercase() in listOf("wicketkeeper", "wicket keeper", "wicket_keeper")
                }
                WicketDialog(
                    onDismiss = { showWicketDialog = false },
                    onConfirm = { wicketType, runs, fielderName, nonStrikerOut ->
                        viewModel.recordBall(runsOffBat = runs, isWicket = true, wicketType = wicketType,
                            fielderName = fielderName, nonStrikerOut = nonStrikerOut)
                        showWicketDialog = false
                    },
                    onRetiredHurt = {
                        viewModel.recordBall(runsOffBat = 0, isWicket = true, wicketType = "retired_hurt")
                        showWicketDialog = false
                    },
                    keeperName = keeper?.fullName,
                    bowlingTeamPlayers = uiState.bowlingTeamPlayers
                )
            }

            if (showExtrasDialog) {
                ExtrasDialog(
                    onDismiss = { showExtrasDialog = false },
                    onConfirm = { extrasType, runs ->
                        // No-ball runs belong to the batsman -> runsOffBat. Wide/bye/leg-bye
                        // runs are extras -> extrasRuns.
                        if (extrasType == "no_ball") {
                            viewModel.recordBall(runsOffBat = runs, extrasType = extrasType, extrasRuns = 0)
                        } else {
                            viewModel.recordBall(runsOffBat = 0, extrasType = extrasType, extrasRuns = runs)
                        }
                        showExtrasDialog = false
                    }
                )
            }

            if (showPenaltyDialog) {
                PenaltyRunsDialog(
                    onDismiss = { showPenaltyDialog = false },
                    onConfirm = { team -> viewModel.addPenaltyRuns(team); showPenaltyDialog = false }
                )
            }

            if (showMissedChanceDialog) {
                MissedChanceDialog(
                    fielders = uiState.bowlingTeamPlayers,
                    onConfirm = { player, type ->
                        viewModel.recordMissedChance(player, type)
                        showMissedChanceDialog = false
                    },
                    onDismiss = { showMissedChanceDialog = false }
                )
            }

            if (showManualEditDialog) {
                ManualEditDialog(
                    currentRuns = uiState.totalRuns,
                    currentWickets = uiState.totalWickets,
                    onDismiss = { showManualEditDialog = false },
                    onConfirm = { runs, wickets -> viewModel.manualEdit(runs, wickets); showManualEditDialog = false }
                )
            }


        } // end overlay Box
    }
}


@Composable
fun ScoreHeader(uiState: ScoringUiState, onShare: () -> Unit,
                popupLabel: String? = null, popupColor: androidx.compose.ui.graphics.Color = NeonGreen, popupKey: Int = 0, popupBig: Boolean = false) {
    val match = uiState.match
    val currentOver = uiState.currentOver
    val powerplayOvers = match?.powerplayOvers ?: 6
    val totalOvers = match?.totalOvers ?: 20
    val isSuperOver = uiState.isSuperOver

    val phaseLabel = when {
        match == null || isSuperOver -> null
        currentOver < powerplayOvers -> "PP"
        currentOver >= (totalOvers - (match.powerplay3Overs.takeIf { it > 0 } ?: 4)) -> "P3"
        else -> "P2"
    }

    Box(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {

            // ── Match info bar: tournament • Match N • format • venue ──
            uiState.tournamentName?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            val infoBits = buildList {
                match?.matchNumber?.let { add("Match $it") }
                match?.matchType?.takeIf { it.isNotBlank() }?.let { add(it) }
                match?.venue?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (infoBits.isNotEmpty()) {
                Text(infoBits.joinToString("  •  "), color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
            if (uiState.tournamentName?.isNotBlank() == true || infoBits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Team name + big score + overs ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    if (uiState.battingTeamName.isNotBlank()) {
                        Text(
                            uiState.battingTeamName.uppercase(),
                            color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${uiState.totalRuns}/${uiState.totalWickets}",
                            fontSize = 44.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text("${uiState.currentOver}.${uiState.currentBall} Ov", fontSize = 18.sp, color = NeonGreen, fontWeight = FontWeight.Medium)
                            phaseLabel?.let {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmberColor.copy(alpha = 0.2f))
                                        .border(1.dp, AmberColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(it, color = AmberColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    // Super over (innings 3+) is always 1 over - don't show the match's total.
                    if (isSuperOver) {
                        Text("SUPER OVER", color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        match?.let { Text("${it.totalOvers} ov", color = TextSecondary, fontSize = 12.sp) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onShare, colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen)) {
                        Text("📤 Share", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            // ── Stats row: CRR always; chase shows Target + RRR, 1st innings shows Projected ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderStat("CRR", "%.2f".format(uiState.runRate), NeonBlue)
                if (uiState.isSecondInnings) {
                    uiState.target?.let { HeaderStat("Target", "$it", AmberColor) }
                    uiState.requiredRunRate?.let {
                        HeaderStat("RRR", "%.2f".format(it), if (it > uiState.runRate) ErrorRed else NeonGreen)
                    }
                } else if (!isSuperOver) {
                    uiState.projectedScore?.let { HeaderStat("Projected", "$it", NeonGreen) }
                }
            }

            // ── Match status line (chase only) ──
            headerStatusLine(uiState)?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        // Celebratory ball-result overlay across the header band
        BallPopup(
            label = popupLabel,
            color = popupColor,
            triggerKey = popupKey,
            big = popupBig,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun HeaderStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$label ", color = TextSecondary, fontSize = 11.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** "Team need X off Y" while chasing; null in the 1st innings. Not shown when innings/match is over. */
fun headerStatusLine(uiState: ScoringUiState): String? {
    if (!uiState.isSecondInnings) return null
    if (uiState.inningsComplete || uiState.matchComplete) return null
    val need = uiState.runsNeeded ?: return null
    if (need <= 0) return null
    val team = uiState.battingTeamName.ifBlank { "Batting side" }
    return "$team need $need off ${uiState.ballsRemaining}"
}

/** Partnership + last wicket + extras strip, shown under the header. */
@Composable
fun HeaderContextRow(uiState: ScoringUiState) {
    val inn = uiState.innings ?: return
    val p = uiState.currentPartnership
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text("Partnership", color = TextSecondary, fontSize = 10.sp)
                Text("${p.runs} (${p.balls})", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            uiState.lastWicket?.let { lw ->
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Wkt  ${lw.score}/${lw.wicket}", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${lw.batsmanName} (${lw.dismissal})", color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Extras ${inn.extrasTotal}  (Wd ${inn.wides}  Nb ${inn.noBalls}  B ${inn.byes}  Lb ${inn.legByes})",
            color = TextSecondary, fontSize = 10.sp
        )
    }
}

// ── LAST 6 BALLS ──────────────────────────────────────────────────────────────

@Composable
fun Last6BallsRow(balls: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("This over:", color = TextSecondary, fontSize = 12.sp)
        balls.forEach { ball ->
            val (bgColor, textColor) = when {
                ball.startsWith("W") -> ErrorRed to Color.White
                ball == "4" -> NeonBlue to Color.White
                ball == "6" -> NeonGreen to Color.Black
                ball.startsWith("Wd") || ball.startsWith("Nb") -> AmberColor to Color.Black
                ball == "0" -> SurfaceCard to TextSecondary
                else -> SurfaceCard to TextPrimary
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(bgColor).border(1.dp, BorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(ball, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── CURRENT BATSMEN ───────────────────────────────────────────────────────────

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
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
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
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                )
            }
            striker?.let { s ->
                batsmanStats[s.id]?.let { stats ->
                    Text("${stats.runs}(${stats.balls}) 4s:${stats.fours} 6s:${stats.sixes} SR:${"%.1f".format(stats.strikeRate)}", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
                .border(1.dp, if (nonStrikerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                .then(if (nonStrikerClickable) Modifier.clickable { onChangeNonStriker() } else Modifier)
                .padding(10.dp)
        ) {
            Text(
                nonStriker?.fullName ?: "Select Non-Striker",
                color = if (nonStriker != null) TextPrimary else TextSecondary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
            nonStriker?.let { ns ->
                batsmanStats[ns.id]?.let { stats ->
                    Text("${stats.runs}(${stats.balls}) 4s:${stats.fours} 6s:${stats.sixes}", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── CURRENT BOWLER ────────────────────────────────────────────────────────────

@Composable
fun CurrentBowlerRow(
    bowler: Player?,
    bowlerStats: Map<String, com.crickethub.data.model.BowlerStats>,
    bowlerClickable: Boolean,
    onChangeBowler: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, if (bowlerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
            .then(if (bowlerClickable) Modifier.clickable { onChangeBowler() } else Modifier)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(bowler?.fullName ?: "Select Bowler", color = if (bowler != null) TextPrimary else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        bowler?.let { b ->
            bowlerStats[b.id]?.let { stats ->
                Text("${stats.overs} M:${stats.maidens} R:${stats.runs} W:${stats.wickets} Eco:${"%.1f".format(stats.economy)}", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ── SCORING BUTTONS ───────────────────────────────────────────────────────────

@Composable
fun ScoringButtons(
    isLoading: Boolean,
    isFreeHit: Boolean = false,
    onRuns: (Int) -> Unit,
    onWicket: () -> Unit,
    onExtras: () -> Unit,
    onUndo: () -> Unit,
    onPenalty: () -> Unit,
    onManualEdit: () -> Unit,
    onMissedChance: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2, 3, 4, 5, 6).forEach { runs ->
                val bgColor = when (runs) { 4 -> NeonBlue.copy(alpha = 0.8f); 6 -> NeonGreen.copy(alpha = 0.8f); 5 -> PurpleColor.copy(alpha = 0.8f); else -> Color(0xFF1A3828) }
                val textColor = when (runs) { 4, 5, 6 -> Color.White; 0 -> Color(0xFF6EE7B7); else -> Color(0xFFECFDF5) }
                Button(
                    onClick = { onRuns(runs) }, enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = textColor, disabledContainerColor = bgColor.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(runs.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onWicket, enabled = !isLoading && !isFreeHit,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFreeHit) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFF7F1D1D),
                    contentColor = Color(0xFFFCA5A5)
                )
            ) { Text(if (isFreeHit) "NO OUT" else "WICKET", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Button(
                onClick = onExtras, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F), contentColor = Color(0xFFFCD34D))
            ) { Text("EXTRAS", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Button(
                onClick = onUndo, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = TextSecondary)
            ) { Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPenalty, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberColor)
            ) { Text("+5 Penalty", fontSize = 11.sp) }
            OutlinedButton(
                onClick = onManualEdit, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
            ) { Text("Manual Edit", fontSize = 11.sp) }
            OutlinedButton(
                onClick = onMissedChance, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleColor)
            ) { Text("Missed", fontSize = 11.sp) }
        }
    }
}

// ── PLAYER SELECT DIALOG ──────────────────────────────────────────────────────

@Composable
fun PlayerSelectDialog(title: String, players: List<Player>, onPlayerSelected: (Player) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(players) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BackgroundDark).clickable { onPlayerSelected(player) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(NeonGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(player.jerseyNo?.toString() ?: "-", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(player.fullName, color = TextPrimary, fontSize = 14.sp)
                            player.role?.let { Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }, color = TextSecondary, fontSize = 11.sp) }
                        }
                    }
                }
                if (players.isEmpty()) {
                    item { Text("No players available", color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
fun BowlerSelectDialog(title: String, players: List<Player>, remainingOvers: Map<String, Int>, onPlayerSelected: (Player) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(players) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BackgroundDark).clickable { onPlayerSelected(player) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(NeonGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(player.jerseyNo?.toString() ?: "-", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(player.fullName, color = TextPrimary, fontSize = 14.sp)
                            player.role?.let { Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }, color = TextSecondary, fontSize = 11.sp) }
                        }
                        val rem = remainingOvers[player.id] ?: 0
                        Text("rem: $rem ov", color = if (rem <= 1) AmberColor else NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (players.isEmpty()) {
                    item { Text("No bowlers available", color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── WICKET DIALOG ─────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WicketDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?, Boolean) -> Unit,
    onRetiredHurt: () -> Unit,
    keeperName: String? = null,
    bowlingTeamPlayers: List<com.crickethub.data.model.Player> = emptyList()
) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runsBeforeWicket by remember { mutableStateOf(0) }
    var fielderName by remember { mutableStateOf("") }
    var nonStrikerOut by remember { mutableStateOf(false) }
    var dropExpanded by remember { mutableStateOf(false) }
    val showFielderInput = selectedType in listOf("caught", "run_out", "stumped")

    // Auto-fill keeper for stumped
    androidx.compose.runtime.LaunchedEffect(selectedType) {
        if (selectedType == "stumped" && keeperName != null) fielderName = keeperName
        else if (selectedType != "stumped") fielderName = ""
    }
    val wicketTypes = listOf(
        "bowled" to "Bowled", "caught" to "Caught", "lbw" to "LBW", "run_out" to "Run Out",
        "stumped" to "Stumped", "hit_wicket" to "Hit Wicket", "retired_out" to "Retired Out",
        "obstructing" to "Obstructing", "timed_out" to "Timed Out", "handled_ball" to "Handled Ball",
        "hit_ball_twice" to "Hit Ball Twice"
    )
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("Wicket!", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 480.dp)) {
                item { Text("How was the batsman dismissed?", color = TextSecondary, fontSize = 13.sp) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        wicketTypes.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (value, label) ->
                                    val selected = selectedType == value
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ErrorRed.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (selected) ErrorRed else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedType = value }.padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (selected) ErrorRed else TextSecondary, fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (selectedType == "run_out") {
                    item {
                        Text("Who got run out?", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { nonStrikerOut = false }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (!nonStrikerOut) ErrorRed else SurfaceCard)
                            ) { Text("Striker", color = if (!nonStrikerOut) Color.White else TextSecondary, fontSize = 12.sp) }
                            Button(onClick = { nonStrikerOut = true }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (nonStrikerOut) ErrorRed else SurfaceCard)
                            ) { Text("Non-Striker", color = if (nonStrikerOut) Color.White else TextSecondary, fontSize = 12.sp) }
                        }
                    }
                }
                if (showFielderInput) {
                    item {
                        if (selectedType == "stumped") {
                            OutlinedTextField(value = fielderName, onValueChange = {}, readOnly = true,
                                label = { Text("Stumped by (Keeper) ✓") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreen, unfocusedBorderColor = NeonGreen,
                                    focusedLabelColor = NeonGreen, unfocusedLabelColor = NeonGreen
                                )
                            )
                        } else if (bowlingTeamPlayers.isNotEmpty()) {
                            ExposedDropdownMenuBox(expanded = dropExpanded, onExpandedChange = { dropExpanded = it }) {
                                OutlinedTextField(
                                    value = fielderName, onValueChange = {}, readOnly = true,
                                    label = { Text(if (selectedType == "caught") "Caught by *" else "Run out by *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    isError = fielderName.isBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = ErrorRed, unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = ErrorRed, unfocusedLabelColor = TextSecondary
                                    )
                                )
                                ExposedDropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false },
                                    modifier = Modifier.background(SurfaceCard)) {
                                    bowlingTeamPlayers.forEach { player ->
                                        DropdownMenuItem(
                                            text = { Text(player.fullName, color = TextPrimary) },
                                            onClick = { fielderName = player.fullName; dropExpanded = false }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(value = fielderName, onValueChange = { fielderName = it },
                                label = { Text(if (selectedType == "caught") "Caught by *" else "Run out by *") },
                                singleLine = true, isError = fielderName.isBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = ErrorRed, unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = ErrorRed, unfocusedLabelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(AmberColor.copy(alpha = 0.1f)).border(1.dp, AmberColor, RoundedCornerShape(8.dp))
                            .clickable { onRetiredHurt() }.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("🤕 Retired Hurt (can bat again)", color = AmberColor, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
                item {
                    Text("Runs before wicket:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..4).forEach { r ->
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (runsBeforeWicket == r) NeonGreen.copy(alpha = 0.3f) else BackgroundDark)
                                    .border(1.dp, if (runsBeforeWicket == r) NeonGreen else BorderColor, CircleShape)
                                    .clickable { runsBeforeWicket = r },
                                contentAlignment = Alignment.Center
                            ) { Text(r.toString(), color = if (runsBeforeWicket == r) NeonGreen else TextSecondary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedType?.let { type ->
                    val needsFielder = type in listOf("caught", "run_out", "stumped")
                    if (!needsFielder || fielderName.isNotBlank()) {
                        onConfirm(type, runsBeforeWicket, fielderName.ifBlank { null }, nonStrikerOut)
                    }
                }},
                enabled = selectedType != null &&
                        (selectedType !in listOf("caught", "run_out", "stumped") || fielderName.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) { Text("Confirm", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── EXTRAS DIALOG ─────────────────────────────────────────────────────────────

@Composable
fun ExtrasDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runs by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("Extras", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("wide" to "Wide", "no_ball" to "No Ball", "bye" to "Bye", "leg_bye" to "Leg Bye").forEach { (value, label) ->
                        val selected = selectedType == value
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AmberColor.copy(alpha = 0.2f) else BackgroundDark)
                                .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { selectedType = value; if (value == "no_ball" && runs == 5) runs = 4 }.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) AmberColor else TextSecondary, fontSize = 12.sp,
                                textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Text("Runs:", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // No-ball: 0,1,2,3,4,6 (can't run 5, but can hit a six).
                    // Others: 0..6.
                    val runOptions = if (selectedType == "no_ball") listOf(0,1,2,3,4,6) else (0..6).toList()
                    runOptions.forEach { r ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(if (runs == r) AmberColor.copy(alpha = 0.3f) else BackgroundDark)
                                .border(1.dp, if (runs == r) AmberColor else BorderColor, CircleShape)
                                .clickable { runs = r },
                            contentAlignment = Alignment.Center
                        ) { Text(r.toString(), color = if (runs == r) AmberColor else TextSecondary, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selectedType?.let { onConfirm(it, runs) } }, enabled = selectedType != null,
                colors = ButtonDefaults.buttonColors(containerColor = AmberColor)) { Text("Confirm", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── PENALTY DIALOG ────────────────────────────────────────────────────────────

@Composable
fun PenaltyRunsDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedTeam by remember { mutableStateOf("batting") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("+5 Penalty Runs", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Award 5 penalty runs to:", color = TextSecondary, fontSize = 13.sp)
                listOf("batting" to "Batting Team", "bowling" to "Bowling Team").forEach { (value, label) ->
                    val selected = selectedTeam == value
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AmberColor.copy(alpha = 0.15f) else BackgroundDark)
                            .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { selectedTeam = value }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { Text(label, color = if (selected) AmberColor else TextSecondary, fontSize = 14.sp) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTeam) }, colors = ButtonDefaults.buttonColors(containerColor = AmberColor)) {
                Text("Add Penalty", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── MANUAL EDIT DIALOG ────────────────────────────────────────────────────────

@Composable
fun ManualEditDialog(currentRuns: Int, currentWickets: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var runs by remember { mutableStateOf(currentRuns.toString()) }
    var wickets by remember { mutableStateOf(currentWickets.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("Manual Score Edit", color = NeonBlue, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️ Use carefully — this directly edits the score", color = AmberColor, fontSize = 12.sp)
                OutlinedTextField(
                    value = runs, onValueChange = { if (it.all { c -> c.isDigit() }) runs = it },
                    label = { Text("Total Runs") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderColor,
                        cursorColor = NeonBlue, focusedLabelColor = NeonBlue, unfocusedLabelColor = TextSecondary
                    )
                )
                OutlinedTextField(
                    value = wickets, onValueChange = { if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 10) wickets = it },
                    label = { Text("Wickets") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderColor,
                        cursorColor = NeonBlue, focusedLabelColor = NeonBlue, unfocusedLabelColor = TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(runs.toIntOrNull() ?: currentRuns, wickets.toIntOrNull() ?: currentWickets) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) { Text("Update Score", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
// ── Ball popup label + colour, derived from the recorded ball ──────────────────
private fun ballPopupLabel(ball: com.crickethub.data.model.Ball): Pair<String, androidx.compose.ui.graphics.Color> {
    val green = androidx.compose.ui.graphics.Color(0xFF34D399)
    val blue = androidx.compose.ui.graphics.Color(0xFF3B82F6)
    val purple = androidx.compose.ui.graphics.Color(0xFF8B5CF6)
    val red = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val amber = androidx.compose.ui.graphics.Color(0xFFF59E0B)
    val grey = androidx.compose.ui.graphics.Color(0xFF9CA3AF)

    // Wicket takes priority
    if (ball.isWicket) {
        val label = when (ball.wicketType) {
            "bowled" -> "BOWLED!"
            "caught" -> "CAUGHT!"
            "lbw" -> "LBW!"
            "run_out" -> "RUN OUT!"
            "stumped" -> "STUMPED!"
            "hit_wicket" -> "HIT WICKET!"
            "retired_hurt" -> "RETIRED HURT"
            "retired_out" -> "RETIRED OUT"
            "obstructing" -> "OBSTRUCTING!"
            "timed_out" -> "TIMED OUT!"
            "handled_ball" -> "HANDLED BALL!"
            "hit_ball_twice" -> "HIT TWICE!"
            else -> "OUT!"
        }
        return label to (if (ball.wicketType == "retired_hurt") amber else red)
    }

    // Extras
    when (ball.extrasType) {
        "wide" -> {
            // extrasRuns = 1 penalty + runs ran. Show ran runs only when > 0.
            val ran = (ball.extrasRuns ?: 1) - 1
            return (if (ran > 0) "WIDE +$ran" else "WIDE") to amber
        }
        "no_ball" -> {
            // runsOffBat = runs the batsman scored. Show only when > 0.
            val ran = ball.runsOffBat
            return (if (ran > 0) "NO BALL +$ran" else "NO BALL") to amber
        }
        "bye" -> return ("BYE +${ball.extrasRuns ?: 1}") to grey
        "leg_bye" -> return ("LEG BYE +${ball.extrasRuns ?: 1}") to grey
        "penalty" -> return ("PENALTY +${ball.extrasRuns ?: 5}") to amber
    }

    // Runs off bat
    return when (ball.runsOffBat) {
        0 -> "DOT" to grey
        4 -> "FOUR!" to blue
        6 -> "SIX!" to green
        else -> "+${ball.runsOffBat}" to green
    }
}

@Composable
private fun BallPopup(
    label: String?,
    color: androidx.compose.ui.graphics.Color,
    triggerKey: Int,
    big: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (label == null) return
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(triggerKey) {
        if (triggerKey == 0) return@LaunchedEffect
        visible = true
        kotlinx.coroutines.delay(if (big) 1900 else 850)
        visible = false
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = if (big) 0.38f else 0.5f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "popupScale"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "popupAlpha"
    )

    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.scaleX = scale; this.scaleY = scale; this.alpha = alpha
                }
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = if (big) 0.22f else 0.18f))
                .border(if (big) 3.dp else 2.dp, color, RoundedCornerShape(20.dp))
                .padding(horizontal = if (big) 28.dp else 32.dp, vertical = 18.dp)
        ) {
            Text(
                text = label,
                color = color,
                fontSize = if (big) 34.sp else 40.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Milestone celebration toast — solid colored badge, white text, compact.
 * Floats over the top strip so it does not overlap the scorecard/buttons.
 */
@Composable
private fun MilestonePopup(
    label: String?,
    color: androidx.compose.ui.graphics.Color,
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    if (label == null) return
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(triggerKey) {
        if (triggerKey == 0) return@LaunchedEffect
        visible = true
        kotlinx.coroutines.delay(2000)
        visible = false
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.4f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "milestoneScale"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "milestoneAlpha"
    )

    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.scaleX = scale; this.scaleY = scale; this.alpha = alpha
                }
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 11.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Milestone reached on the last ball (fired once, on a freshly scored ball only).
 * Priority: hat-trick > century > 5-wkt > fifty > team milestone.
 */
private fun detectMilestone(
    uiState: ScoringUiState,
    last: Ball
): Pair<String, androidx.compose.ui.graphics.Color>? {
    val gold = androidx.compose.ui.graphics.Color(0xFFE8A200)
    val green = androidx.compose.ui.graphics.Color(0xFF34D399)
    val purple = androidx.compose.ui.graphics.Color(0xFF8B5CF6)
    val blue = androidx.compose.ui.graphics.Color(0xFF3B82F6)

    val bowlerWicketTypes = listOf("bowled", "caught", "lbw", "stumped", "hit_wicket")
    val bowlerCredited = last.isWicket && last.wicketType in bowlerWicketTypes

    // Hat-trick: last 3 of this bowler's deliveries are all bowler-credited wickets.
    if (bowlerCredited && last.bowlerId != null) {
        val deliveries = uiState.balls.filter {
            it.bowlerId == last.bowlerId && it.extrasType != "wide" &&
                    it.wicketType !in listOf("timed_out", "retired_out", "retired_hurt")
        }
        val last3 = deliveries.takeLast(3)
        if (last3.size == 3 && last3.all { it.isWicket && it.wicketType in bowlerWicketTypes }) {
            return "🎩 HAT-TRICK! 🎩" to purple
        }
    }

    // Batsman fifty / century (the batter who faced this ball)
    val stats = uiState.batsmanStats[last.batsmanId]
    if (stats != null && last.extrasType != "wide") {
        val before = stats.runs - last.runsOffBat
        val name = stats.player.fullName.uppercase()
        if (stats.runs >= 100 && before < 100) return "💯 $name — CENTURY! 💯" to gold
        if (stats.runs >= 50 && before < 50) return "🎉 $name — FIFTY! 🎉" to green
    }

    // Bowler five-wicket haul
    if (bowlerCredited) {
        val bw = uiState.bowlerStats[last.bowlerId]
        if (bw != null && bw.wickets == 5) {
            return "🔥 ${bw.player.fullName.uppercase()} — 5 WICKETS! 🔥" to purple
        }
    }

    // Team milestones
    val ballTeamRuns = when {
        last.extrasType == "wide" -> (last.extrasRuns ?: 1) + last.runsOffBat
        last.extrasType == "no_ball" -> 1 + last.runsOffBat + (last.extrasRuns ?: 0)
        else -> last.runsOffBat + (last.extrasRuns ?: 0)
    }
    val total = uiState.totalRuns
    val beforeTotal = total - ballTeamRuns
    for (m in listOf(200, 150, 100, 50)) {
        if (total >= m && beforeTotal < m) return "🎉 $m UP! 🎉" to blue
    }

    return null
}
// ── BALL-BY-BALL TIMELINE (reverse chronological) ────────────

@Composable
fun ScoringBallTimeline(balls: List<Ball>) {
    if (balls.isEmpty()) return
    val oversDesc = balls.groupBy { it.overNo }.entries.sortedByDescending { it.key }

    fun ballRuns(b: Ball) = when {
        b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
        b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
        else -> b.runsOffBat + (b.extrasRuns ?: 0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        oversDesc.forEachIndexed { idx, entry ->
            if (idx > 0) {
                val overRuns = entry.value.sumOf { ballRuns(it) }
                ScoringOverSeparator(entry.key + 1, overRuns)
            }
            entry.value.reversed().forEach { ball ->
                ScoringBallChip(ball)
            }
        }
    }
}

@Composable
private fun ScoringBallChip(ball: Ball) {
    val runs = ball.runsOffBat + (ball.extrasRuns ?: 0)
    val label: String
    val color: Color
    when {
        ball.isWicket && ball.wicketType != "retired_hurt" -> { label = if (runs > 0) "W+$runs" else "W"; color = ErrorRed }
        ball.isSix -> { label = "6"; color = PurpleColor }
        ball.isBoundary -> { label = "4"; color = NeonBlue }
        ball.extrasType == "wide" -> { val r = (ball.extrasRuns ?: 1) - 1; label = if (r > 0) "Wd+$r" else "Wd"; color = AmberColor }
        ball.extrasType == "no_ball" -> { label = if (ball.runsOffBat > 0) "Nb+${ball.runsOffBat}" else "Nb"; color = AmberColor }
        ball.extrasType == "bye" -> { val b = ball.extrasRuns ?: 0; label = if (b <= 1) "B" else "${b}B"; color = SurfaceCard }
        ball.extrasType == "leg_bye" -> { val lb = ball.extrasRuns ?: 0; label = if (lb <= 1) "LB" else "${lb}LB"; color = SurfaceCard }
        ball.runsOffBat == 0 && ball.extrasRuns == null -> { label = "•"; color = SurfaceCard }
        else -> { label = "$runs"; color = SurfaceCard }
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .heightIn(min = 32.dp)
            .widthIn(min = 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (color == SurfaceCard) TextPrimary else Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScoringOverSeparator(overNumber: Int, runs: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(1.dp)
                .height(32.dp)
                .background(BorderColor)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(scoringOrdinalOver(overNumber), color = TextSecondary, fontSize = 10.sp)
            Text("$runs RUNS", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(6.dp))
    }
}

private fun scoringOrdinalOver(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

// ── COMPACT BATTERS + BOWLER TABLE (cricinfo-style) ──────────

@Composable
fun ScoringPlayersTable(
    uiState: ScoringUiState,
    strikerClickable: Boolean,
    nonStrikerClickable: Boolean,
    bowlerClickable: Boolean,
    onChangeStriker: () -> Unit,
    onChangeNonStriker: () -> Unit,
    onChangeBowler: () -> Unit
) {
    val bowlerId = uiState.currentBowler?.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .padding(vertical = 6.dp)
    ) {
        // Batters header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BATTER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TableHead("R"); TableHead("B"); TableHead("4s"); TableHead("6s"); TableHead("SR", 42.dp); TableHead("vB", 40.dp)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

        BatterTableRow(uiState.striker, uiState.batsmanStats, true, uiState.balls, bowlerId, strikerClickable, onChangeStriker)
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        BatterTableRow(uiState.nonStriker, uiState.batsmanStats, false, uiState.balls, bowlerId, nonStrikerClickable, onChangeNonStriker)

        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(color = BorderColor)
        Spacer(modifier = Modifier.height(2.dp))

        // Bowler header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BOWLER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TableHead("O", 34.dp); TableHead("M"); TableHead("R"); TableHead("W"); TableHead("Eco", 42.dp)
            TableHead("0s"); TableHead("4s"); TableHead("6s")
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        BowlerTableRow(uiState.currentBowler, uiState.bowlerStats, uiState.balls, bowlerClickable, onChangeBowler)
    }
}

@Composable
private fun RowScope.TableHead(label: String, width: Dp = 28.dp) {
    Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End, modifier = Modifier.width(width))
}

@Composable
private fun RowScope.TableCell(text: String, width: Dp = 28.dp, color: Color = TextPrimary, bold: Boolean = false) {
    Text(text, color = color, fontSize = 12.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.End, modifier = Modifier.width(width))
}

private fun handAbbrev(battingHand: String?): String =
    if (battingHand?.lowercase()?.startsWith("l") == true) "lhb" else "rhb"

/** bowlingHand + bowlingStyle -> short code (LF, RFM, SLA, ROS, RLG, LWS...). */
private fun bowlingCode(hand: String?, style: String?): String {
    if (style.isNullOrBlank()) return ""
    val left = hand?.lowercase()?.startsWith("l") == true
    val arm = if (left) "L" else "R"
    val s = style.lowercase().replace(Regex("[-_]"), " ").trim()
    return when {
        s.contains("orthodox") || (left && s.contains("off")) -> "SLA"
        s.contains("chinaman") || s.contains("wrist") || (left && s.contains("leg")) -> "LWS"
        s.contains("off") -> "ROS"
        s.contains("leg") -> "RLG"
        s.contains("fast") && s.contains("medium") ->
            arm + if (s.indexOf("fast") < s.indexOf("medium")) "FM" else "MF"
        s.contains("fast") -> arm + "F"
        s.contains("medium") -> arm + "M"
        else -> style.take(4)
    }
}

@Composable
private fun BatterTableRow(
    player: Player?, stats: Map<String, com.crickethub.data.model.BatsmanStats>,
    isStriker: Boolean, balls: List<Ball>, bowlerId: String?,
    clickable: Boolean, onClick: () -> Unit
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 10.dp, vertical = 5.dp)
    Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (isStriker) {
                Text("* ", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                player?.fullName ?: if (isStriker) "Select Batsman" else "Select Non-Striker",
                color = if (player != null) TextPrimary else NeonGreen,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
            player?.let {
                Text("  ${handAbbrev(it.battingHand)}", color = TextSecondary, fontSize = 10.sp)
            }
        }
        val s = player?.let { stats[it.id] }
        if (s != null) {
            TableCell("${s.runs}", 28.dp, TextPrimary, true)
            TableCell("${s.balls}")
            TableCell("${s.fours}")
            TableCell("${s.sixes}")
            TableCell("%.1f".format(s.strikeRate), 42.dp, TextSecondary)
            // vs current bowler
            val vb = player.let { p ->
                val pb = balls.filter { it.batsmanId == p.id && it.bowlerId == bowlerId }
                val r = pb.sumOf { it.runsOffBat }
                val b = pb.count { it.extrasType != "wide" }
                "$r($b)"
            }
            TableCell(vb, 40.dp, TextSecondary)
        } else {
            TableCell("-"); TableCell("-"); TableCell("-"); TableCell("-"); TableCell("-", 42.dp); TableCell("-", 40.dp)
        }
    }
}

@Composable
private fun BowlerTableRow(
    bowler: Player?, stats: Map<String, com.crickethub.data.model.BowlerStats>,
    balls: List<Ball>, clickable: Boolean, onClick: () -> Unit
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 10.dp, vertical = 5.dp)
    Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(bowler?.fullName ?: "Select Bowler",
                color = if (bowler != null) TextPrimary else NeonGreen,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            bowler?.let { bw ->
                val code = bowlingCode(bw.bowlingHand, bw.bowlingStyle)
                if (code.isNotBlank()) {
                    Text("  $code", color = TextSecondary, fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
            }
        }
        val s = bowler?.let { stats[it.id] }
        if (s != null) {
            TableCell(s.overs, 34.dp, TextPrimary, true)
            TableCell("${s.maidens}")
            TableCell("${s.runs}")
            TableCell("${s.wickets}")
            TableCell("%.1f".format(s.economy), 42.dp, TextSecondary)
            val bb = bowler.let { p -> balls.filter { it.bowlerId == p.id } }
            val dots = bb.count { it.runsOffBat == 0 && it.extrasRuns == null && !it.isWicket }
            val fours = bb.count { it.isBoundary && !it.isSix }
            val sixes = bb.count { it.isSix }
            TableCell("$dots", 28.dp, TextSecondary)
            TableCell("$fours", 28.dp, TextSecondary)
            TableCell("$sixes", 28.dp, TextSecondary)
        } else {
            TableCell("-", 34.dp); TableCell("-"); TableCell("-"); TableCell("-"); TableCell("-", 42.dp)
            TableCell("-"); TableCell("-"); TableCell("-")
        }
    }
}

// ── MISSED CHANCE DIALOG ──────────────────────────────────────────────────────

@Composable
fun MissedChanceDialog(
    fielders: List<Player>,
    onConfirm: (Player, String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Player?>(null) }
    val types = listOf(
        "catch_dropped" to "Catch Dropped",
        "run_out_missed" to "Run Out Missed",
        "stumping_missed" to "Missed Stumping"
    )
    val ready = type != null && selected != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Missed Chance", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Type", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                types.forEach { (key, label) ->
                    val sel = type == key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) AmberColor.copy(alpha = 0.2f) else BackgroundDark)
                            .border(1.dp, if (sel) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { type = key }
                            .padding(10.dp)
                    ) {
                        Text(label, color = if (sel) AmberColor else TextPrimary, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Fielder (required)", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    fielders.forEach { p ->
                        val sel = selected?.id == p.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) NeonGreen.copy(alpha = 0.2f) else BackgroundDark)
                                .border(1.dp, if (sel) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { selected = p }
                                .padding(10.dp)
                        ) {
                            Text(p.fullName, color = if (sel) NeonGreen else TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { val t = type; val p = selected; if (t != null && p != null) onConfirm(p, t) },
                enabled = ready
            ) {
                Text("Record", color = if (ready) NeonGreen else TextSecondary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
@Composable
private fun SyncIndicator(syncState: SyncState) {
    val (color, label) = when (syncState) {
        SyncState.SYNCED  -> NeonGreen to "Synced"
        SyncState.SYNCING -> NeonBlue to "Syncing"
        SyncState.PENDING -> AmberColor to "Pending"
        SyncState.OFFLINE -> ErrorRed to "Offline"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}