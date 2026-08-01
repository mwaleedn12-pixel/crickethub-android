package com.crickethub.ui.match.postmatch

import androidx.compose.foundation.background
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.MatchRepository
import com.crickethub.export.*
import com.crickethub.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest


@Composable
fun PostMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onGoToMatches: () -> Unit,
    viewModel: PostMatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExport by remember { mutableStateOf(false) }

    val cards = uiState.inningsCards

    val scorecardIndex = 1
    val commentaryIndex = 2
    val oversIndex = 3
    val analyticsIndex = 4
    val impactIndex = 5

    val tabs = listOf("Result", "Scorecard", "Commentary", "Overs", "Analytics", "Impact")

    LaunchedEffect(matchId) {
        viewModel.loadPostMatch(matchId)
    }

    LaunchedEffect(uiState.matchSaved) {
        if (uiState.matchSaved) onGoToMatches()
    }

    // Export dialog
    ExportDialog(
        show = showExport,
        onDismiss = { showExport = false },
        title = "Export Scorecard",
        onExport = { format ->
            val repo = MatchRepository()
            val match = repo.getMatchById(matchId)!!
            val t1 = SupabaseClient.client.postgrest["teams"]
                .select { filter { eq("id", match.team1Id) } }.decodeSingleOrNull<Team>()!!
            val t2 = SupabaseClient.client.postgrest["teams"]
                .select { filter { eq("id", match.team2Id) } }.decodeSingleOrNull<Team>()!!
            val playerNames = mutableMapOf<String, String>()
            cards.forEach { card ->
                card.batting.forEach { playerNames[it.player.id] = it.player.fullName }
                card.bowling.forEach { playerNames[it.player.id] = it.player.fullName }
            }
            val awards = mutableListOf<String>()
            uiState.selectedMotm?.let { awards.add("POTM: ${it.fullName}") }
            uiState.bestBatter?.let { awards.add("Best Batter: ${it.player.fullName} ${it.runs}(${it.balls})") }
            uiState.bestBowler?.let { awards.add("Best Bowler: ${it.player.fullName} ${it.wickets}/${it.runs}") }
            val data = MatchReportData(match, t1, t2,
                cards.map { it.innings },
                cards.associate { it.innings.id to it.balls },
                playerNames, awards)
            when (format) {
                ExportFormat.PDF -> MatchReportGenerator.generatePdf(context, data)
                ExportFormat.CSV -> MatchReportGenerator.generateCsv(context, data)
            }
        }
    )

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CH.textPrimary)
                }
                Text(
                    "Match Summary",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = CH.textPrimary, modifier = Modifier.weight(1f)
                )
                ExportButton(onClick = { showExport = true })
                IconButton(onClick = {
                    val shareText = buildString {
                        appendLine("🏏 Match Result")
                        appendLine(uiState.resultText)
                        cards.forEach { card ->
                            appendLine("${card.battingTeamName}: ${card.scoreText} (${card.oversText} ov)" +
                                    if (card.isSuperOver) "  [${card.label}]" else "")
                        }
                        uiState.selectedMotm?.let { appendLine("POTM: ${it.fullName}") }
                    }
                    clipboardManager.setText(AnnotatedString(shareText))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonGreen)
                }
            }

            // Result banner
            if (uiState.resultText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.resultText,
                        color = NeonGreen, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CH.surface,
                contentColor = NeonGreen,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                tab, fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) NeonGreen else CH.textSecondary
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> PMResultTab(
                            uiState = uiState,
                            onOpenImpactList = { selectedTab = impactIndex }
                        )
                        scorecardIndex -> PMScorecardTab(cards)
                        commentaryIndex -> PMCommentaryTab(cards)
                        oversIndex -> PMOversTab(cards)
                        analyticsIndex -> PMAnalyticsTab(cards)
                        impactIndex -> PMPotmTab(
                            uiState = uiState,
                            onSelectMotm = { viewModel.selectMotm(it) }
                        )
                    }
                }

                // Save button
                Button(
                    onClick = { viewModel.saveMatchResult(matchId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text(
                        "Save & Complete Match",
                        color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ── RESULT TAB ───────────────────────────────────────────────

@Composable
fun PMResultTab(uiState: PostMatchUiState, onOpenImpactList: () -> Unit) {
    val cards = uiState.inningsCards

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CH.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cards.forEachIndexed { index, card ->
                    if (index > 0) HorizontalDivider(color = CH.border)
                    val accent = when {
                        card.isSuperOver -> AmberColor
                        card.innings.inningsNo == 1 -> NeonGreen
                        else -> NeonBlue
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.battingTeamName, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(card.label, color = CH.textSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(card.scoreText, color = CH.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("(${card.oversText} ov)", color = CH.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
                if (cards.isEmpty()) {
                    Text("No innings data", color = CH.textSecondary, fontSize = 12.sp)
                }
            }
        }

        // Awards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CH.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("AWARDS", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = CH.border)

                uiState.selectedMotm?.let { motm ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("⭐ Player of the Match", color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(motm.fullName, color = CH.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        val motmCandidate = uiState.motmCandidates.find { it.player.id == motm.id }
                        motmCandidate?.let {
                            Column(horizontalAlignment = Alignment.End) {
                                if (it.runs > 0) Text("${it.runs} runs", color = CH.textSecondary, fontSize = 12.sp)
                                if (it.wickets > 0) Text("${it.wickets} wkts", color = CH.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                uiState.bestBatter?.let {
                    HorizontalDivider(color = CH.border, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🏏 Best Batter", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(it.player.fullName, color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.runs}(${it.balls})", color = NeonBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                uiState.bestBowler?.let {
                    HorizontalDivider(color = CH.border, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🎳 Best Bowler", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(it.player.fullName, color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.wickets}/${it.runs}", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                uiState.bestPartnership?.let {
                    HorizontalDivider(color = CH.border, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🤝 Best Partnership", color = PurpleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${it.name1} & ${it.name2}", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.runs} (${it.balls})", color = PurpleColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.motmCandidates.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CH.surface)
                        .border(1.dp, CH.border, RoundedCornerShape(12.dp))
                        .clickable { onOpenImpactList() }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥 IMPACT LIST", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("View all ›", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = CH.border)

                    uiState.motmCandidates.take(5).forEachIndexed { index, candidate ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                color = if (index == 0) AmberColor else CH.textSecondary,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    candidate.player.fullName,
                                    color = CH.textPrimary, fontSize = 13.sp,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    impactSummaryText(candidate),
                                    color = CH.textSecondary, fontSize = 10.sp
                                )
                            }
                            Text(
                                "%.1f".format(candidate.score),
                                color = if (index == 0) AmberColor else NeonGreen,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun impactSummaryText(candidate: MotmCandidate): String = buildString {
    if (candidate.runs > 0) append("${candidate.runs} runs (${"%.1f".format(candidate.strikeRate)} SR)")
    if (candidate.runs > 0 && candidate.wickets > 0) append("  •  ")
    if (candidate.wickets > 0) append("${candidate.wickets} wkts (${"%.1f".format(candidate.economy)} eco)")
}

// ── SCORECARD TAB ────────────────────────────────────────────

@Composable
fun PMScorecardTab(cards: List<InningsCard>) {
    if (cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No innings data", color = CH.textSecondary)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        cards.sortedBy { it.innings.inningsNo }.forEach { card ->
            pmInningsSection(card)
        }
    }
}

private fun LazyListScope.pmInningsSection(card: InningsCard) {
    val innings = card.innings
    val bowlerMap = card.bowlerMap

    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (card.isSuperOver) AmberColor.copy(alpha = 0.12f)
                    else NeonGreen.copy(alpha = 0.10f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${card.battingTeamName} — ${card.label}",
                color = if (card.isSuperOver) AmberColor else NeonGreen,
                fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "${card.scoreText} (${card.oversText})",
                color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        }
    }

    item {
        Column(modifier = Modifier.fillMaxWidth().background(CH.surface)) {
            Text(
                card.battingTeamName, color = NeonGreen, fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("BATTING", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("R", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("B", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("4s", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("6s", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("SR", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border)
        }
    }

    // Include 0(0) batsmen who appeared at the crease + sort by batting order (crease arrival)
    val appearedIds = card.balls.flatMap { listOfNotNull(it.batsmanId, it.nonStrikerId) }.toSet()
    val battedList = card.batting.filter { it.balls > 0 || it.isOut || it.player.id in appearedIds }
    // Sort by batting order: first to crease = position 1
    val batOrder = run {
        val order = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        card.balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
            ball.batsmanId.let { id -> if (id !in seen) { seen.add(id); order.add(id) } }
            ball.nonStrikerId?.let { id -> if (id !in seen) { seen.add(id); order.add(id) } }
        }
        order
    }
    val sortedBattedList = battedList.sortedBy { stat ->
        val idx = batOrder.indexOf(stat.player.id)
        if (idx >= 0) idx else Int.MAX_VALUE
    }
    items(sortedBattedList) { stats ->
        Column(modifier = Modifier.fillMaxWidth().background(CH.bg)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stats.player.fullName,
                        color = if (stats.isOut) CH.textSecondary else CH.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(buildPostMatchDismissalText(stats, bowlerMap), color = CH.textSecondary, fontSize = 10.sp)
                }
                Text("${stats.runs}", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.balls}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.fours}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.sixes}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(stats.strikeRate)}", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border, thickness = 0.5.dp)
        }
    }

    val didNotBat = card.batting.filter { it.balls == 0 && !it.isOut && it.player.id !in appearedIds }
    if (didNotBat.isNotEmpty() && !card.isSuperOver) {
        item {
            Column(modifier = Modifier.fillMaxWidth().background(CH.bg).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Did Not Bat", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(didNotBat.joinToString(", ") { it.player.fullName }, color = CH.textSecondary, fontSize = 11.sp)
            }
            HorizontalDivider(color = CH.border)
        }
    }

    item {
        Column(modifier = Modifier.fillMaxWidth().background(CH.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Extras", color = CH.textSecondary, fontSize = 13.sp)
                Text("(w ${innings.wides}, nb ${innings.noBalls}, b 0, lb 0)", color = CH.textSecondary, fontSize = 11.sp)
                Text("${innings.extrasTotal}", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${card.oversText} Ov", color = CH.textSecondary, fontSize = 12.sp)
                Text(card.scoreText, color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = CH.border)
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().background(CH.surface)) {
            Text(
                card.bowlingTeamName, color = NeonBlue, fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("BOWLING", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("O", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("M", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("R", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("W", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Eco", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border)
        }
    }

    // Sort bowlers by bowling order: first bowler on top
    val bowlOrder = run {
        val order = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        card.balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
            val id = ball.bowlerId
            if (id !in seen) { seen.add(id); order.add(id) }
        }
        order
    }
    val sortedBowling = card.bowling.sortedBy { stat ->
        val idx = bowlOrder.indexOf(stat.player.id)
        if (idx >= 0) idx else Int.MAX_VALUE
    }
    items(sortedBowling) { stats ->
        Column(modifier = Modifier.fillMaxWidth().background(CH.bg)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stats.player.fullName, color = CH.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(stats.overs, color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.maidens}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.runs}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text(
                    "${stats.wickets}",
                    color = if (stats.wickets > 0) NeonGreen else CH.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (stats.wickets > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(28.dp), textAlign = TextAlign.End
                )
                Text("${"%.2f".format(stats.economy)}", color = when {
                    stats.economy < 6 -> NeonGreen; stats.economy < 9 -> AmberColor; else -> ErrorRed
                }, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border, thickness = 0.5.dp)
        }
    }
}

fun buildPostMatchDismissalText(stats: BatsmanScorecard, bowlerMap: Map<String, String>): String {
    if (!stats.isOut) return "not out"
    val bowlerName = stats.bowlerOnWicket?.let { bowlerMap[it] } ?: ""
    val fielder = stats.fielderName ?: ""
    return when (stats.dismissalType) {
        "bowled" -> "b $bowlerName"
        "caught" -> if (fielder.isNotBlank()) "c $fielder b $bowlerName" else "c & b $bowlerName"
        "lbw" -> "lbw b $bowlerName"
        "run_out" -> if (fielder.isNotBlank()) "run out ($fielder)" else "run out"
        "stumped" -> if (fielder.isNotBlank()) "st $fielder b $bowlerName" else "st Keeper b $bowlerName"
        "hit_wicket" -> "hit wicket b $bowlerName"
        "retired_out" -> "retired out"
        "retired_hurt" -> "retired hurt"
        "obstructing" -> "obstructing the field"
        "timed_out" -> "timed out"
        "handled_ball" -> "handled the ball"
        "hit_ball_twice" -> "hit the ball twice"
        else -> stats.dismissalType?.replace("_", " ") ?: "out"
    }
}

// ── SHARED INNINGS HEADER ────────────────────────────────────

@Composable
fun PMInningsBanner(card: InningsCard) {
    val accent = when {
        card.isSuperOver -> AmberColor
        card.innings.inningsNo == 1 -> NeonGreen
        else -> NeonBlue
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "${card.battingTeamName} — ${card.label}  ${card.scoreText} (${card.oversText})",
            color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
    }
}

// ── COMMENTARY TAB ───────────────────────────────────────────

@Composable
fun PMCommentaryTab(cards: List<InningsCard>) {
    val hasBalls = cards.any { it.balls.isNotEmpty() }
    if (!hasBalls) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No commentary available", color = CH.textSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        cards.sortedBy { it.innings.inningsNo }.forEach { card ->
            if (card.balls.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PMInningsBanner(card)
                }
                val ordered = card.balls
                    .sortedWith(compareBy({ it.overNo }, { it.ballNo }))
                    .reversed()
                items(ordered) { ball ->
                    PMCommentaryRow(ball)
                }
            }
        }
    }
}

@Composable
fun PMCommentaryRow(ball: Ball) {
    val isWicket = ball.isWicket && ball.wicketType != "retired_hurt"
    val isFour = ball.isBoundary && !ball.isSix
    val isSix = ball.isSix
    val isWide = ball.extrasType == "wide"
    val isNoBall = ball.extrasType == "no_ball"

    val outcomeText = when {
        isWicket -> "W"; isSix -> "6"; isFour -> "4"
        isWide -> "Wd"; isNoBall -> "Nb"
        ball.runsOffBat == 0 && ball.extrasRuns == null -> "•"
        else -> "${ball.runsOffBat + (ball.extrasRuns ?: 0)}"
    }
    val outcomeColor = when {
        isWicket -> ErrorRed; isSix -> NeonGreen; isFour -> NeonBlue
        isWide || isNoBall -> AmberColor; else -> CH.surface
    }

    Column(modifier = Modifier.fillMaxWidth().background(if (isWicket) ErrorRed.copy(alpha = 0.05f) else CH.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                if (ball.ballNo > 0) {
                    Text("${ball.overNo}.${ball.ballNo}", color = CH.textSecondary, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp)).background(outcomeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(outcomeText, color = if (outcomeColor == CH.surface) CH.textSecondary else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                ball.commentary ?: "Ball ${ball.overNo}.${ball.ballNo}",
                color = if (isWicket) ErrorRed else CH.textSecondary,
                fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = CH.border, thickness = 0.5.dp)
    }
}

// ── OVERS TAB ────────────────────────────────────────────────

@Composable
fun PMOversTab(cards: List<InningsCard>) {
    val hasBalls = cards.any { it.balls.isNotEmpty() }
    if (!hasBalls) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No over data available", color = CH.textSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        cards.forEach { card ->
            if (card.balls.isNotEmpty()) {
                item(key = "ov-header-${card.innings.id}") {
                    Spacer(modifier = Modifier.height(8.dp))
                    PMInningsBanner(card)
                }
                val overGroups = card.balls.groupBy { it.overNo }.toSortedMap()
                items(overGroups.entries.toList()) { (overNo, overBalls) ->
                    PMOverRow(overNo, overBalls, card.bowling)
                }
            }
        }
    }
}

@Composable
fun PMOverRow(overNo: Int, overBalls: List<Ball>, bowling: List<BowlerScorecard>) {
    val runsInOver = overBalls.sumOf { b ->
        when {
            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
            else -> b.runsOffBat + (b.extrasRuns ?: 0)
        }
    }
    val wicketsInOver = overBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
    val legalBalls = overBalls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
    val rr = if (legalBalls.isNotEmpty()) runsInOver.toDouble() / legalBalls.size * 6 else 0.0
    val bowlerId = overBalls.firstOrNull()?.bowlerId
    val bowlerName = bowling.find { it.player.id == bowlerId }?.player?.fullName ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CH.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Ov ${overNo + 1}", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Text("$runsInOver${if (wicketsInOver > 0) "/$wicketsInOver" else ""}", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                overBalls.sortedBy { it.ballNo }.forEach { ball ->
                    val label = when {
                        ball.isWicket -> "W"; ball.isSix -> "6"; ball.isBoundary -> "4"
                        ball.extrasType == "wide" -> "Wd"; ball.extrasType == "no_ball" -> "Nb"
                        ball.runsOffBat == 0 && ball.extrasRuns == null -> "•"
                        else -> "${ball.runsOffBat + (ball.extrasRuns ?: 0)}"
                    }
                    val bgColor = when {
                        ball.isWicket -> ErrorRed; ball.isSix -> NeonGreen; ball.isBoundary -> NeonBlue
                        ball.extrasType in listOf("wide", "no_ball") -> AmberColor; else -> CH.border
                    }
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
            Text("${"%.1f".format(rr)}", color = when { rr >= 12 -> ErrorRed; rr >= 9 -> AmberColor; rr >= 6 -> NeonGreen; else -> CH.textSecondary }, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
        }
        if (bowlerName.isNotEmpty()) {
            Text("🎳 $bowlerName", color = CH.textSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
    HorizontalDivider(color = CH.border, thickness = 0.5.dp)
}

// ── ANALYTICS TAB ────────────────────────────────────────────

@Composable
fun PMAnalyticsTab(cards: List<InningsCard>) {
    val mainCards = cards.filter { !it.isSuperOver }
    val superOverCards = cards.filter { it.isSuperOver }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PMAnalyticsCard("Innings Comparison") {
                val c1 = mainCards.getOrNull(0)
                val c2 = mainCards.getOrNull(1)
                if (c1 != null && c2 != null && c1.balls.isNotEmpty() && c2.balls.isNotEmpty()) {
                    val stats1 = computeInningsStats(c1.balls)
                    val stats2 = computeInningsStats(c2.balls)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(c1.battingTeamName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("", modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                        Text(c2.battingTeamName, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = CH.border, modifier = Modifier.padding(vertical = 6.dp))
                    PMCompareRow(stats1.runs.toString(), "Runs", stats2.runs.toString(), stats1.runs > stats2.runs)
                    PMCompareRow(stats1.fours.toString(), "Fours", stats2.fours.toString(), stats1.fours > stats2.fours)
                    PMCompareRow(stats1.sixes.toString(), "Sixes", stats2.sixes.toString(), stats1.sixes > stats2.sixes)
                    PMCompareRow("${"%.2f".format(stats1.runRate)}", "Run Rate", "${"%.2f".format(stats2.runRate)}", stats1.runRate > stats2.runRate)
                    PMCompareRow("${stats1.dotBalls}", "Dot Balls", "${stats2.dotBalls}", stats1.dotBalls < stats2.dotBalls)
                    PMCompareRow("${stats1.extras}", "Extras", "${stats2.extras}", stats1.extras < stats2.extras)
                    PMCompareRow("${"%.1f".format(stats1.boundaryPct)}%", "Boundary%", "${"%.1f".format(stats2.boundaryPct)}%", stats1.boundaryPct > stats2.boundaryPct)
                } else {
                    Text("Need both innings for comparison", color = CH.textSecondary, fontSize = 12.sp)
                }
            }
        }

        if (superOverCards.isNotEmpty()) {
            item {
                PMAnalyticsCard("Super Over") {
                    superOverCards.forEach { card ->
                        val stats = computeInningsStats(card.balls)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(card.battingTeamName, color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(card.label, color = CH.textSecondary, fontSize = 10.sp)
                            }
                            Text("${card.scoreText} (${card.oversText})", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("${stats.fours}x4  ${stats.sixes}x6", color = CH.textSecondary, fontSize = 11.sp)
                        }
                        HorizontalDivider(color = CH.border, thickness = 0.5.dp)
                    }
                }
            }
        }

        item {
            PMAnalyticsCard("Phase Analysis") {
                mainCards.forEach { card ->
                    if (card.balls.isEmpty()) return@forEach
                    Text(card.battingTeamName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val phases = listOf("powerplay" to "PP", "middle" to "Mid", "death" to "Death")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        phases.forEach { (phase, label) ->
                            val phaseBalls = card.balls.filter { it.inningsPhase == phase }
                            val runs = phaseBalls.sumOf { b ->
                                when {
                                    b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                                    b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                                    else -> b.runsOffBat + (b.extrasRuns ?: 0)
                                }
                            }
                            val legal = phaseBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                            val wkts = phaseBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
                            val rr = if (legal > 0) runs * 6.0 / legal else 0.0
                            Column(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(CH.bg).border(1.dp, CH.border, RoundedCornerShape(8.dp)).padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, color = CH.textSecondary, fontSize = 10.sp)
                                Text("$runs/$wkts", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("RR ${"%.1f".format(rr)}", color = NeonGreen, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        item {
            PMAnalyticsCard("Dismissal Types") {
                val allWickets = cards.flatMap { it.balls }.filter { it.isWicket && it.wicketType != "retired_hurt" }
                val groups = allWickets.groupBy { it.wicketType?.replace("_", " ")?.replaceFirstChar { c -> c.uppercase() } ?: "Unknown" }
                if (groups.isEmpty()) {
                    Text("No wickets yet", color = CH.textSecondary, fontSize = 12.sp)
                } else {
                    groups.forEach { (type, balls) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(type, color = CH.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.height(16.dp).width((balls.size * 30).dp.coerceAtMost(100.dp)).clip(RoundedCornerShape(4.dp)).background(ErrorRed.copy(alpha = 0.6f)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${balls.size}", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }

        item {
            PMAnalyticsCard("Extras Distribution") {
                cards.forEach { card ->
                    if (card.balls.isEmpty()) return@forEach
                    val wides = card.balls.count { it.extrasType == "wide" }
                    val noBalls = card.balls.count { it.extrasType == "no_ball" }
                    val byes = card.balls.count { it.extrasType == "bye" }
                    val legByes = card.balls.count { it.extrasType == "leg_bye" }
                    val total = wides + noBalls + byes + legByes
                    Text(
                        if (card.isSuperOver) "${card.battingTeamName} (${card.label})" else card.battingTeamName,
                        color = if (card.isSuperOver) AmberColor else NeonGreen,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PMStatBox("W", "$wides", AmberColor, Modifier.weight(1f))
                        PMStatBox("NB", "$noBalls", ErrorRed, Modifier.weight(1f))
                        PMStatBox("B", "$byes", NeonBlue, Modifier.weight(1f))
                        PMStatBox("LB", "$legByes", PurpleColor, Modifier.weight(1f))
                        PMStatBox("Tot", "$total", CH.textPrimary, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

data class InningsQuickStats(
    val runs: Int, val fours: Int, val sixes: Int,
    val runRate: Double, val dotBalls: Int,
    val extras: Int, val boundaryPct: Double
)

fun computeInningsStats(balls: List<Ball>): InningsQuickStats {
    val runs = balls.sumOf { b ->
        when {
            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
            else -> b.runsOffBat + (b.extrasRuns ?: 0)
        }
    }
    val fours = balls.count { it.isBoundary && !it.isSix }
    val sixes = balls.count { it.isSix }
    val legal = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
    val rr = if (legal > 0) runs * 6.0 / legal else 0.0
    val dots = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" && it.runsOffBat == 0 && (it.extrasRuns ?: 0) == 0 }
    val extras = balls.sumOf { it.extrasRuns ?: 0 } + balls.count { it.extrasType == "wide" } + balls.count { it.extrasType == "no_ball" }
    val batRuns = balls.sumOf { it.runsOffBat }
    val bdryRuns = (fours * 4) + (sixes * 6)
    val bdryPct = if (batRuns > 0) bdryRuns * 100.0 / batRuns else 0.0
    return InningsQuickStats(runs, fours, sixes, rr, dots, extras, bdryPct)
}

// ── IMPACT / POTM TAB ────────────────────────────────────────

@Composable
fun PMPotmTab(uiState: PostMatchUiState, onSelectMotm: (Player) -> Unit) {
    if (uiState.motmCandidates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No candidates available", color = CH.textSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Impact List", color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Ranked by impact points — tap a player to set them as Player of the Match.", color = CH.textSecondary, fontSize = 11.sp)
            }
        }
        itemsIndexed(uiState.motmCandidates) { index, candidate ->
            val isSelected = uiState.selectedMotm?.id == candidate.player.id
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) AmberColor.copy(alpha = 0.15f) else CH.surface)
                    .border(1.dp, if (isSelected) AmberColor else CH.border, RoundedCornerShape(10.dp))
                    .clickable { onSelectMotm(candidate.player) }.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", color = if (index == 0) AmberColor else CH.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(candidate.player.fullName, color = if (isSelected) AmberColor else CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(impactSummaryText(candidate), color = CH.textSecondary, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.1f".format(candidate.score), color = if (isSelected) AmberColor else NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(if (isSelected) "⭐ POTM" else "pts", color = CH.textSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── HELPER COMPOSABLES ───────────────────────────────────────

@Composable
fun PMAnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CH.surface).border(1.dp, CH.border, RoundedCornerShape(12.dp)).padding(14.dp)
    ) {
        Text(title, color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = CH.border, modifier = Modifier.padding(vertical = 8.dp))
        content()
    }
}

@Composable
fun PMStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(CH.bg).border(1.dp, CH.border, RoundedCornerShape(8.dp)).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, color = CH.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun PMCompareRow(val1: String, label: String, val2: String, val1Better: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(val1, color = if (val1Better) NeonGreen else CH.textPrimary, fontSize = 13.sp, fontWeight = if (val1Better) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        Text(label, color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(val2, color = if (!val1Better) NeonGreen else CH.textPrimary, fontSize = 13.sp, fontWeight = if (!val1Better) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}