package com.crickethub.ui.match.analytics

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import com.crickethub.data.model.Ball

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)
private val AmberColor = Color(0xFFF59E0B)
private val PurpleColor = Color(0xFF8B5CF6)

@Composable
fun AnalyticsScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Match", "Batting", "Bowling", "Win%")

    LaunchedEffect(matchId) {
        viewModel.loadAnalytics(matchId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
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
                "Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { index ->
                val selected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) NeonGreen else SurfaceCard)
                        .border(1.dp, if (selected) NeonGreen else BorderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedTab = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        tabs[index],
                        color = if (selected) Color.Black else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            when (selectedTab) {
                0 -> MatchAnalyticsTab(uiState)
                1 -> BattingAnalyticsTab(uiState)
                2 -> BowlingAnalyticsTab(uiState)
                3 -> WinProbabilityTab(uiState)
            }
        }
    }
}

@Composable
fun MatchAnalyticsTab(uiState: AnalyticsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsCard(title = "Worm Graph — Cumulative Runs") {
                WormGraph(
                    innings1Overs = uiState.innings1Overs,
                    innings2Overs = uiState.innings2Overs,
                    team1Name = uiState.battingTeamName,
                    team2Name = uiState.bowlingTeamName
                )
            }
        }
        item {
            AnalyticsCard(title = "Manhattan Chart — Runs per Over") {
                ManhattanChart(
                    innings1Overs = uiState.innings1Overs,
                    innings2Overs = uiState.innings2Overs
                )
            }
        }
        item {
            AnalyticsCard(title = "Phase Performance") {
                PhasePerformanceChart(
                    innings1Phases = uiState.innings1Phases,
                    innings2Phases = uiState.innings2Phases
                )
            }
        }
        item {
            AnalyticsCard(title = "Extras Distribution") {
                ExtrasChart(extrasData = uiState.extrasData)
            }
        }
    }
}

@Composable
fun BattingAnalyticsTab(uiState: AnalyticsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsCard(title = "Dismissal Types") {
                DismissalPieChart(dismissalTypes = uiState.dismissalTypes)
            }
        }
        item {
            AnalyticsCard(title = "Boundary Timeline") {
                BoundaryTimeline(boundaries = uiState.boundaryTimeline)
            }
        }
        item {
            AnalyticsCard(title = "Partnership Graph") {
                PartnershipChart(partnerships = uiState.partnerships)
            }
        }
        item {
            AnalyticsCard(title = "Innings Summary") {
                InningsSummaryCard(uiState = uiState)
            }
        }
    }
}

@Composable
fun BowlingAnalyticsTab(uiState: AnalyticsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsCard(title = "Economy Rate by Over") {
                EconomyRateChart(overs = uiState.innings1Overs)
            }
        }
        item {
            AnalyticsCard(title = "Wickets Timeline") {
                WicketsTimeline(overs = uiState.innings1Overs)
            }
        }
        item {
            AnalyticsCard(title = "Dot Ball %") {
                DotBallStats(balls = uiState.innings1Balls)
            }
        }
    }
}

@Composable
fun WinProbabilityTab(uiState: AnalyticsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.winProbability.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Win probability available after 2nd innings starts",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            item {
                AnalyticsCard(
                    title = "Win Probability — ${uiState.battingTeamName} chasing ${uiState.target}"
                ) {
                    WinProbabilityChart(
                        data = uiState.winProbability,
                        battingTeamName = uiState.battingTeamName,
                        bowlingTeamName = uiState.bowlingTeamName
                    )
                }
            }
            item {
                uiState.winProbability.lastOrNull()?.let { latest ->
                    AnalyticsCard(title = "Current Win Probability") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(latest.battingTeamProbability * 100).toInt()}%",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                                Text(uiState.battingTeamName, color = TextSecondary, fontSize = 13.sp)
                                Text("Batting", color = TextSecondary, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(latest.bowlingTeamProbability * 100).toInt()}%",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonBlue
                                )
                                Text(uiState.bowlingTeamName, color = TextSecondary, fontSize = 13.sp)
                                Text("Bowling", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun WormGraph(
    innings1Overs: List<OverData>,
    innings2Overs: List<OverData>,
    team1Name: String,
    team2Name: String
) {
    if (innings1Overs.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val maxRuns = maxOf(
        innings1Overs.lastOrNull()?.cumulativeRuns ?: 0,
        innings2Overs.lastOrNull()?.cumulativeRuns ?: 0
    ).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val w = size.width
        val h = size.height
        val padL = 40f
        val padB = 30f
        val chartW = w - padL - 16f
        val chartH = h - padB - 16f

        for (i in 0..4) {
            val y = 16f + chartH - (chartH * i / 4f)
            drawLine(
                color = BorderColor,
                start = Offset(padL, y),
                end = Offset(w - 16f, y),
                strokeWidth = 0.5f
            )
        }

        if (innings1Overs.size > 1) {
            val path = Path()
            innings1Overs.forEachIndexed { index, over ->
                val x = padL + (index.toFloat() / (innings1Overs.size - 1)) * chartW
                val y = 16f + chartH - (over.cumulativeRuns.toFloat() / maxRuns * chartH)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        if (innings2Overs.size > 1) {
            val path = Path()
            innings2Overs.forEachIndexed { index, over ->
                val x = padL + (index.toFloat() / (innings2Overs.size - 1)) * chartW
                val y = 16f + chartH - (over.cumulativeRuns.toFloat() / maxRuns * chartH)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonBlue, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }
    }

    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = NeonGreen, label = team1Name)
        if (innings2Overs.isNotEmpty()) LegendItem(color = NeonBlue, label = team2Name)
    }
}

@Composable
fun ManhattanChart(
    innings1Overs: List<OverData>,
    innings2Overs: List<OverData>
) {
    if (innings1Overs.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val maxRuns = innings1Overs.maxOfOrNull { it.runs }?.coerceAtLeast(1) ?: 1

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val w = size.width
        val h = size.height
        val padB = 24f
        val chartH = h - padB
        val barWidth = (w / (innings1Overs.size * 2.5f)).coerceAtMost(30f)

        innings1Overs.forEachIndexed { index, over ->
            val x = (index.toFloat() / innings1Overs.size) * w + barWidth * 0.5f
            val barH = (over.runs.toFloat() / maxRuns) * chartH
            val barColor = when {
                over.runs >= 15 -> NeonGreen
                over.runs >= 10 -> AmberColor
                else -> NeonBlue.copy(alpha = 0.8f)
            }
            drawRect(
                color = barColor,
                topLeft = Offset(x, chartH - barH),
                size = Size(barWidth, barH)
            )
            repeat(over.wickets) { wIdx ->
                drawCircle(
                    color = ErrorRed,
                    radius = 4f,
                    center = Offset(x + barWidth / 2, chartH - barH - 8f - (wIdx * 12f))
                )
            }
        }
    }
}

@Composable
fun WinProbabilityChart(
    data: List<WinProbabilityData>,
    battingTeamName: String,
    bowlingTeamName: String
) {
    if (data.isEmpty()) return

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val w = size.width
        val h = size.height
        val padL = 40f
        val padB = 24f
        val chartW = w - padL - 16f
        val chartH = h - padB - 16f

        val midY = 16f + chartH / 2
        drawLine(
            color = BorderColor,
            start = Offset(padL, midY),
            end = Offset(w - 16f, midY),
            strokeWidth = 1f
        )

        if (data.size > 1) {
            val greenPath = Path()
            data.forEachIndexed { index, d ->
                val x = padL + (index.toFloat() / (data.size - 1)) * chartW
                val y = 16f + chartH - (d.battingTeamProbability * chartH)
                if (index == 0) greenPath.moveTo(x, y) else greenPath.lineTo(x, y)
            }
            drawPath(greenPath, NeonGreen, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            val bluePath = Path()
            data.forEachIndexed { index, d ->
                val x = padL + (index.toFloat() / (data.size - 1)) * chartW
                val y = 16f + chartH - (d.bowlingTeamProbability * chartH)
                if (index == 0) bluePath.moveTo(x, y) else bluePath.lineTo(x, y)
            }
            drawPath(bluePath, NeonBlue, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        drawContext.canvas.nativeCanvas.drawText(
            "50%",
            padL - 36f,
            midY + 4f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
            }
        )
    }

    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = NeonGreen, label = "$battingTeamName (Batting)")
        LegendItem(color = NeonBlue, label = "$bowlingTeamName (Bowling)")
    }
}

@Composable
fun DismissalPieChart(dismissalTypes: Map<String, Int>) {
    if (dismissalTypes.isEmpty()) {
        Text("No dismissals yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val total = dismissalTypes.values.sum().toFloat()
    val colors = listOf(ErrorRed, NeonGreen, NeonBlue, AmberColor, PurpleColor, Color(0xFFEC4899))
    val entries = dismissalTypes.entries.toList()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            entries.forEachIndexed { index, (_, count) ->
                val sweep = (count / total) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweep
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEachIndexed { index, (type, count) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], RoundedCornerShape(2.dp))
                    )
                    Text(
                        "${type.replace("_", " ").replaceFirstChar { it.uppercase() }}: $count",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PhasePerformanceChart(
    innings1Phases: List<PhaseData>,
    innings2Phases: List<PhaseData>
) {
    if (innings1Phases.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        innings1Phases.forEach { phase ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    phase.phase,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.width(90.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${phase.runs} runs | ${phase.wickets}W | ${phase.boundaries} boundaries",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    val rpo = if (phase.balls > 0) (phase.runs.toFloat() / phase.balls) * 6 else 0f
                    LinearProgressIndicator(
                        progress = { (rpo / 18f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when (phase.phase) {
                            "Powerplay" -> NeonGreen
                            "Middle" -> AmberColor
                            else -> ErrorRed
                        },
                        trackColor = BorderColor
                    )
                }
            }
        }
    }
}

@Composable
fun ExtrasChart(extrasData: Map<String, Int>) {
    if (extrasData.isEmpty()) {
        Text("No extras", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val maxVal = extrasData.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        extrasData.forEach { (type, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(type, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                LinearProgressIndicator(
                    progress = { count.toFloat() / maxVal },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AmberColor,
                    trackColor = BorderColor
                )
                Text("$count", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
            }
        }
    }
}

@Composable
fun PartnershipChart(partnerships: List<PartnershipData>) {
    if (partnerships.isEmpty()) {
        Text("No partnerships data", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val maxRuns = partnerships.maxOfOrNull { it.runs }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        partnerships.take(10).forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("W${p.wicketNo}", color = ErrorRed, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                LinearProgressIndicator(
                    progress = { p.runs.toFloat() / maxRuns },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = NeonGreen,
                    trackColor = BorderColor
                )
                Text(
                    "${p.runs}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

@Composable
fun BoundaryTimeline(boundaries: List<Pair<Int, String>>) {
    if (boundaries.isEmpty()) {
        Text("No boundaries yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        boundaries.forEach { (_, type) ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (type == "6") NeonGreen.copy(alpha = 0.2f)
                        else NeonBlue.copy(alpha = 0.2f)
                    )
                    .border(
                        1.dp,
                        if (type == "6") NeonGreen else NeonBlue,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    type,
                    color = if (type == "6") NeonGreen else NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EconomyRateChart(overs: List<OverData>) {
    if (overs.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val maxEco = 24f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val w = size.width
        val h = size.height

        if (overs.size > 1) {
            val path = Path()
            overs.forEachIndexed { index, over ->
                val eco = over.runs.toFloat()
                val x = (index.toFloat() / (overs.size - 1)) * w
                val y = h - (eco / maxEco * h)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, AmberColor, style = Stroke(width = 2f, cap = StrokeCap.Round))
        }

        val sixY = h - (6f / maxEco * h)
        drawLine(
            color = NeonGreen.copy(alpha = 0.5f),
            start = Offset(0f, sixY),
            end = Offset(w, sixY),
            strokeWidth = 1f
        )
    }
}

@Composable
fun WicketsTimeline(overs: List<OverData>) {
    if (overs.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        overs.forEach { over ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp)
            ) {
                if (over.wickets > 0) {
                    repeat(over.wickets) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(ErrorRed, RoundedCornerShape(6.dp))
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(8.dp)
                        .background(
                            if (over.wickets > 0) ErrorRed.copy(alpha = 0.3f) else SurfaceCard,
                            RoundedCornerShape(2.dp)
                        )
                )
                Text("${over.overNo + 1}", color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun DotBallStats(balls: List<Ball>) {
    if (balls.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 13.sp)
        return
    }

    val totalLegal = balls.count { it.extrasType != "wide" }.coerceAtLeast(1)
    val dotBalls = balls.count { it.runsOffBat == 0 && it.extrasType == null && !it.isWicket }
    val dotPct = dotBalls.toFloat() / totalLegal * 100
    val boundaryBalls = balls.count { it.isBoundary || it.isSix }
    val boundaryPct = boundaryBalls.toFloat() / totalLegal * 100
    val fours = balls.count { it.isBoundary && !it.isSix }
    val sixes = balls.count { it.isSix }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatRow("Dot Ball %", "${dotPct.toInt()}%", dotPct / 100f, NeonGreen)
        StatRow("Boundary %", "${boundaryPct.toInt()}%", boundaryPct / 100f, AmberColor)
        StatRow("4s", "$fours", fours.toFloat() / totalLegal, NeonBlue)
        StatRow("6s", "$sixes", sixes.toFloat() / totalLegal, PurpleColor)
    }
}

@Composable
fun StatRow(label: String, value: String, progress: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = BorderColor
        )
        Text(value, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(40.dp))
    }
}

@Composable
fun InningsSummaryCard(uiState: AnalyticsUiState) {
    val innings1 = uiState.innings1
    val innings2 = uiState.innings2

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        innings1?.let { i ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(uiState.battingTeamName, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${i.totalRuns}/${i.totalWickets} (${i.totalBalls / 6}.${i.totalBalls % 6})",
                        color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Extras: ${i.extrasTotal}", color = TextSecondary, fontSize = 12.sp)
                    Text("Wides: ${i.wides}", color = TextSecondary, fontSize = 12.sp)
                    Text("No Balls: ${i.noBalls}", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        innings2?.let { i ->
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(uiState.bowlingTeamName, color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${i.totalRuns}/${i.totalWickets} (${i.totalBalls / 6}.${i.totalBalls % 6})",
                        color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
                uiState.target?.let { t ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target: $t", color = AmberColor, fontSize = 13.sp)
                        val needed = t - i.totalRuns
                        Text(
                            if (needed > 0) "Need: $needed" else "Won!",
                            color = if (needed > 0) ErrorRed else NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}