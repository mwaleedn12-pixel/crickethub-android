package com.crickethub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crickethub.data.model.Innings
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import com.crickethub.data.repository.MatchRepository
import com.crickethub.ui.auth.ForgotPasswordScreen
import com.crickethub.ui.auth.LoginScreen
import com.crickethub.ui.auth.SignupScreen
import com.crickethub.ui.dashboard.DashboardScreen
import com.crickethub.ui.match.CreateMatchScreen
import com.crickethub.ui.match.MatchesScreen
import com.crickethub.ui.match.PlayingXIScreen
import com.crickethub.ui.match.TossScreen
import com.crickethub.ui.match.analytics.AnalyticsScreen
import com.crickethub.ui.match.live.LiveScorecardScreen
import com.crickethub.ui.match.live.LiveScorecardViewModel
import com.crickethub.ui.match.postmatch.PostMatchScreen
import com.crickethub.ui.match.scoring.ScoringScreen
import com.crickethub.ui.match.scoring.ScoringViewModel
import com.crickethub.ui.player.AllPlayersScreen
import com.crickethub.ui.player.PlayerCareerScreen
import com.crickethub.ui.player.PlayerComparisonScreen
import com.crickethub.ui.team.PlayersScreen
import com.crickethub.ui.team.TeamsScreen
import com.crickethub.ui.theme.CricketHubTheme
import com.crickethub.ui.tournament.CreateTournamentScreen
import com.crickethub.ui.tournament.TournamentDetailScreen
import com.crickethub.ui.tournament.TournamentsScreen
import com.crickethub.ui.join.JoinWithCodeScreen
import com.crickethub.ui.team.TeamStatsScreen
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

private val BackgroundDark = Color(0xFF0A0A0A)
private val SurfaceCard = Color(0xFF161616)
private val NeonGreen = Color(0xFF34D399)
private val TextSecondary = Color(0xFFC4C9D4)

// ── Bottom-nav tab definitions ───────────────────────────────
private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab("dashboard", "Home", Icons.Default.Home),
    BottomTab("all_players", "Players", Icons.Default.Person),
    BottomTab("matches", "Matches", Icons.Default.List),
    BottomTab("teams", "Teams", Icons.Default.Person),
    BottomTab("tournaments", "Tourneys", Icons.Default.Star),
    BottomTab("player_comparison", "Compare", Icons.Default.AccountCircle)
)

private val bottomRoutes = bottomTabs.map { it.route }.toSet()

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "crickethub_activity"
        private const val KEY_LAST_ACTIVE = "last_active_at"
        private const val INACTIVITY_LIMIT_MS = 6 * 60 * 60 * 1000L  // 6 hours
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check inactivity BEFORE rendering UI
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastActive = prefs.getLong(KEY_LAST_ACTIVE, 0L)
        val now = System.currentTimeMillis()
        val shouldAutoLogout = lastActive > 0 && (now - lastActive) > INACTIVITY_LIMIT_MS

        if (shouldAutoLogout) {
            MainScope().launch {
                try {
                    SupabaseClient.client.auth.signOut()
                    android.util.Log.d("CricketHub", "Auto-logout: ${(now - lastActive) / 3600000}h inactive")
                } catch (e: Exception) {
                    android.util.Log.w("CricketHub", "Auto-logout signOut error: ${e.message}")
                }
            }
        }

        setContent {
            CricketHubTheme {
                CricketHubContent(forceLogin = shouldAutoLogout)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save timestamp when app goes to background
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    override fun onResume() {
        super.onResume()
        // Update timestamp on resume too
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }
}

@Composable
fun CricketHubContent(forceLogin: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomRoutes
    val scope = rememberCoroutineScope()

    // ── Auth-aware start destination ──────────────────────────
    // Check session BEFORE rendering NavHost so that:
    //   • Theme change (Activity recreate) does NOT flash the login screen
    //   • An unauthenticated user never lands on dashboard
    var authChecked by remember { mutableStateOf(false) }
    var startRoute by remember { mutableStateOf("login") }

    LaunchedEffect(Unit) {
        try {
            if (forceLogin) {
                startRoute = "login"
            } else {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                startRoute = if (user != null) "dashboard" else "login"
            }
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "Session check: ${e.message}")
            startRoute = "login"
        }
        authChecked = true
    }

    // Show branded splash until auth is resolved — prevents login flash on theme change
    if (!authChecked) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏏", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("CricketHub", color = NeonGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        }
        return
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = SurfaceCard, contentColor = NeonGreen) {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        // Pop back to dashboard to keep backstack clean
                                        popUpTo("dashboard") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonGreen, selectedTextColor = NeonGreen,
                                unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                                indicatorColor = NeonGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(paddingValues)
        ) {
            // ── Auth ─────────────────────────────────────
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = { navController.navigate("signup") },
                    onNavigateToForgotPassword = { navController.navigate("forgot_password") }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignupSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }

            // ── Bottom-nav destinations ──────────────────

            composable("dashboard") {
                DashboardScreen(
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { matchId -> navController.navigate("analytics/$matchId") },
                    onJoinWithCode = { navController.navigate("join") },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("all_players") {
                AllPlayersScreen(
                    onPlayerClick = { playerId ->
                        navController.navigate("career?playerId=$playerId")
                    }
                )
            }

            composable("matches") {
                MatchesScreen(
                    onCreateMatch = { navController.navigate("create_match") },
                    onMatchClick = { matchId -> navController.navigate("match_flow/$matchId") },
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { matchId -> navController.navigate("analytics/$matchId") }
                )
            }

            composable("teams") {
                TeamsScreen(
                    onTeamClick = { teamId -> navController.navigate("players/$teamId") },
                    onTeamStats = { teamId, teamName ->
                        navController.navigate("team_stats/$teamId/${java.net.URLEncoder.encode(teamName, "UTF-8")}")
                    }
                )
            }

            composable("tournaments") {
                TournamentsScreen(
                    onCreateTournament = { navController.navigate("create_tournament") },
                    onTournamentClick = { id -> navController.navigate("tournament_detail/$id") }
                )
            }

            composable("player_comparison") {
                PlayerComparisonScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Career (opened from Players tab or standalone) ──
            composable(
                route = "career?playerId={playerId}",
                arguments = listOf(navArgument("playerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val playerId = backStackEntry.arguments?.getString("playerId")
                PlayerCareerScreen(
                    playerId = playerId,
                    onBack = { navController.popBackStack() },
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { matchId -> navController.navigate("analytics/$matchId") }
                )
            }

            // ── Team sub-screens ─────────────────────────

            composable(
                route = "team_stats/{teamId}/{teamName}",
                arguments = listOf(
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("teamName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val teamName = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("teamName") ?: "", "UTF-8")
                TeamStatsScreen(teamId = teamId, teamName = teamName, onBack = { navController.popBackStack() })
            }

            composable(
                route = "players/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                PlayersScreen(teamId = teamId, onBack = { navController.popBackStack() })
            }

            // ── Match flow ───────────────────────────────

            composable("create_match") {
                CreateMatchScreen(
                    onBack = { navController.popBackStack() },
                    onMatchCreated = { matchId ->
                        navController.navigate("toss/$matchId") {
                            popUpTo("matches") { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = "playing_xi_flow/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                PlayingXIFlowScreen(
                    matchId = matchId,
                    onGoToTeamXI = { teamId, teamName, playersPerSide ->
                        navController.navigate("playing_xi/$matchId/$teamId/$teamName/$playersPerSide")
                    },
                    onGoToScoring = {
                        navController.navigate("scoring/$matchId") {
                            popUpTo("matches") { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = "match_flow/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchFlowScreen(
                    matchId = matchId,
                    onGoToToss = {
                        navController.navigate("toss/$matchId") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    },
                    onGoToTeam1XI = { teamId, teamName, playersPerSide ->
                        navController.navigate("playing_xi/$matchId/$teamId/$teamName/$playersPerSide") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    },
                    onGoToTeam2XI = { teamId, teamName, playersPerSide ->
                        navController.navigate("playing_xi/$matchId/$teamId/$teamName/$playersPerSide") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    },
                    onGoToScoring = {
                        navController.navigate("scoring/$matchId") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    },
                    onMatchComplete = {
                        navController.navigate("post_match/$matchId") {
                            popUpTo("matches")
                        }
                    }
                )
            }

            composable(
                route = "toss/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                TossScreen(
                    matchId = matchId,
                    onTossComplete = { id ->
                        navController.navigate("playing_xi_flow/$id") {
                            popUpTo("toss/$id") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "playing_xi/{matchId}/{teamId}/{teamName}/{playersPerSide}",
                arguments = listOf(
                    navArgument("matchId") { type = NavType.StringType },
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("teamName") { type = NavType.StringType },
                    navArgument("playersPerSide") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val teamName = backStackEntry.arguments?.getString("teamName") ?: ""
                val playersPerSide = backStackEntry.arguments?.getInt("playersPerSide") ?: 11
                PlayingXIScreen(
                    matchId = matchId,
                    teamId = teamId,
                    teamName = teamName,
                    playersPerSide = playersPerSide,
                    onBack = { navController.popBackStack() },
                    onXISaved = {
                        MainScope().launch {
                            try {
                                val repo = MatchRepository()
                                val match = repo.getMatchById(matchId)
                                if (match != null) {
                                    val xi = SupabaseClient.client.postgrest["playing_xi"]
                                        .select { filter { eq("match_id", matchId) } }
                                        .decodeList<com.crickethub.data.model.PlayingXI>()
                                    val needed = match.playersPerSide
                                    val t1Count = xi.count { it.teamId == match.team1Id }
                                    val t2Count = xi.count { it.teamId == match.team2Id }
                                    android.util.Log.d("CricketHub", "After XI save: t1=$t1Count t2=$t2Count needed=$needed")
                                    when {
                                        t1Count < needed -> {
                                            val t1 = SupabaseClient.client.postgrest["teams"]
                                                .select { filter { eq("id", match.team1Id) } }
                                                .decodeSingleOrNull<Team>()
                                            navController.navigate("playing_xi/$matchId/${match.team1Id}/${t1?.name ?: "Team 1"}/$needed") {
                                                popUpTo("playing_xi_flow/$matchId") { inclusive = false }
                                            }
                                        }
                                        t2Count < needed -> {
                                            val t2 = SupabaseClient.client.postgrest["teams"]
                                                .select { filter { eq("id", match.team2Id) } }
                                                .decodeSingleOrNull<Team>()
                                            navController.navigate("playing_xi/$matchId/${match.team2Id}/${t2?.name ?: "Team 2"}/$needed") {
                                                popUpTo("playing_xi_flow/$matchId") { inclusive = false }
                                            }
                                        }
                                        else -> {
                                            navController.navigate("scoring/$matchId") {
                                                popUpTo("matches") { inclusive = false }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CricketHub", "XI save nav error: ${e.message}", e)
                            }
                        }
                    }
                )
            }

            composable("join") {
                JoinWithCodeScreen(
                    onBack = { navController.popBackStack() },
                    onJoinMatch = { matchId, _ -> navController.navigate("scoring/$matchId") },
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onJoinTeam = { teamId -> navController.navigate("players/$teamId") },
                    onJoinTournament = { tournamentId -> navController.navigate("tournament_detail/$tournamentId") },
                    onJoinPlayer = { playerId -> navController.navigate("career?playerId=$playerId") }
                )
            }

            // ── Scoring ──────────────────────────────────

            composable(
                route = "scoring/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                val scoringViewModel: ScoringViewModel = viewModel(backStackEntry)
                ScoringScreen(
                    matchId = matchId,
                    onBack = {
                        // Try tournament_detail first (tournament → match_flow → scoring path).
                        // If not on stack, try matches tab, then dashboard.
                        val poppedToTournament = navController.popBackStack("tournament_detail/{tournamentId}", false)
                        if (!poppedToTournament) {
                            val poppedToMatches = navController.popBackStack("matches", false)
                            if (!poppedToMatches) {
                                navController.popBackStack("dashboard", false)
                            }
                        }
                    },
                    onInningsComplete = {
                        // Pop to the right parent — tournament_detail if from tournament, matches otherwise
                        val hasTournament = navController.currentBackStack.value.any {
                            it.destination.route == "tournament_detail/{tournamentId}"
                        }
                        navController.navigate("post_match/$matchId") {
                            if (hasTournament) {
                                popUpTo("tournament_detail/{tournamentId}") { inclusive = false }
                            } else {
                                popUpTo("matches") { inclusive = false }
                            }
                        }
                    },
                    onViewScorecard = { navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { navController.navigate("analytics/$matchId") },
                    viewModel = scoringViewModel
                )
            }

            composable(
                route = "live_scorecard/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                val liveViewModel: LiveScorecardViewModel = viewModel(backStackEntry)

                val scoringEntry = try {
                    navController.getBackStackEntry("scoring/$matchId")
                } catch (e: Exception) { null }

                if (scoringEntry != null) {
                    val scoringViewModel: ScoringViewModel = viewModel(scoringEntry)
                    val scoringState by scoringViewModel.uiState.collectAsState()
                    var team1Name by remember { mutableStateOf("Team 1") }
                    var team2Name by remember { mutableStateOf("Team 2") }

                    LaunchedEffect(matchId) {
                        try {
                            val match = MatchRepository().getMatchById(matchId)
                            if (match != null) {
                                val t1 = SupabaseClient.client.postgrest["teams"]
                                    .select { filter { eq("id", match.team1Id) } }
                                    .decodeSingleOrNull<Team>()
                                val t2 = SupabaseClient.client.postgrest["teams"]
                                    .select { filter { eq("id", match.team2Id) } }
                                    .decodeSingleOrNull<Team>()
                                team1Name = t1?.name ?: "Team 1"
                                team2Name = t2?.name ?: "Team 2"
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CricketHub", "Team names error: ${e.message}", e)
                        }
                    }

                    LaunchedEffect(
                        scoringState.balls.size,
                        scoringState.innings?.totalRuns,
                        scoringState.innings?.totalWickets,
                        scoringState.innings?.totalBalls,
                        scoringState.innings?.wides,
                        scoringState.innings?.noBalls
                    ) {
                        if (scoringState.innings != null) {
                            liveViewModel.updateFromScoringState(
                                scoringState, team1Name, team2Name
                            )
                        }
                    }

                    LaunchedEffect(team1Name, team2Name) {
                        if (scoringState.innings != null && team1Name != "Team 1") {
                            liveViewModel.updateFromScoringState(
                                scoringState, team1Name, team2Name
                            )
                        }
                    }

                    LiveScorecardScreen(
                        matchId = matchId,
                        onBack = { navController.popBackStack() },
                        viewModel = liveViewModel
                    )
                } else {
                    LaunchedEffect(Unit) {
                        liveViewModel.loadAndSubscribe(matchId)
                    }
                    LiveScorecardScreen(
                        matchId = matchId,
                        onBack = { navController.popBackStack() },
                        viewModel = liveViewModel
                    )
                }
            }

            composable(
                route = "analytics/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                AnalyticsScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "post_match/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                PostMatchScreen(
                    matchId = matchId,
                    onBack = {
                        // Try to pop to tournament_detail first (handles Tournament → Match → PostMatch flow).
                        // If no tournament_detail on the stack, try matches tab, then dashboard.
                        val poppedToTournament = navController.popBackStack("tournament_detail/{tournamentId}", false)
                        if (!poppedToTournament) {
                            val poppedToMatches = navController.popBackStack("matches", false)
                            if (!poppedToMatches) {
                                navController.popBackStack("dashboard", false)
                            }
                        }
                    },
                    onGoToMatches = {
                        navController.navigate("matches") {
                            popUpTo("matches") { inclusive = true }
                        }
                    }
                )
            }

            // ── Tournament sub-screens ───────────────────

            composable("create_tournament") {
                CreateTournamentScreen(
                    onBack = { navController.popBackStack() },
                    onTournamentCreated = { id ->
                        navController.navigate("tournament_detail/$id") {
                            popUpTo("tournaments")
                        }
                    }
                )
            }
            composable(
                route = "tournament_detail/{tournamentId}",
                arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                TournamentDetailScreen(
                    tournamentId = tournamentId,
                    onBack = { navController.popBackStack() },
                    onMatchClick = { matchId -> navController.navigate("match_flow/$matchId") },
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { matchId -> navController.navigate("analytics/$matchId") }
                )
            }
        }
    }
}

// ── Helper screens (unchanged) ───────────────────────────────

@Composable
fun PlayingXIFlowScreen(
    matchId: String,
    onGoToTeamXI: (String, String, Int) -> Unit,
    onGoToScoring: () -> Unit
) {
    LaunchedEffect(matchId) {
        try {
            val repo = MatchRepository()
            val match = repo.getMatchById(matchId) ?: return@LaunchedEffect
            val xi = repo.getPlayingXI(matchId)
            val needed = match.playersPerSide
            val t1Count = xi.count { it.teamId == match.team1Id }
            val t2Count = xi.count { it.teamId == match.team2Id }
            if (t1Count < needed) {
                val t1 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team1Id) } }
                    .decodeSingleOrNull<Team>()
                onGoToTeamXI(match.team1Id, t1?.name ?: "Team 1", needed)
                return@LaunchedEffect
            }
            if (t2Count < needed) {
                val t2 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team2Id) } }
                    .decodeSingleOrNull<Team>()
                onGoToTeamXI(match.team2Id, t2?.name ?: "Team 2", needed)
                return@LaunchedEffect
            }
            onGoToScoring()
        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "PlayingXIFlow error: ${e.message}", e)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NeonGreen)
    }
}

@Composable
fun MatchFlowScreen(
    matchId: String,
    onGoToToss: () -> Unit,
    onGoToTeam1XI: (teamId: String, teamName: String, playersPerSide: Int) -> Unit,
    onGoToTeam2XI: (teamId: String, teamName: String, playersPerSide: Int) -> Unit,
    onGoToScoring: () -> Unit,
    onMatchComplete: () -> Unit
) {
    LaunchedEffect(matchId) {
        try {
            val repo = MatchRepository()
            val match = repo.getMatchById(matchId) ?: return@LaunchedEffect

            if (match.tossWinnerId == null) {
                onGoToToss()
                return@LaunchedEffect
            }

            val xi = repo.getPlayingXI(matchId)
            val playersNeeded = match.playersPerSide
            val team1Count = xi.count { it.teamId == match.team1Id }
            val team2Count = xi.count { it.teamId == match.team2Id }

            if (team1Count < playersNeeded) {
                val t1 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team1Id) } }
                    .decodeSingleOrNull<Team>()
                onGoToTeam1XI(match.team1Id, t1?.name ?: "Team 1", playersNeeded)
                return@LaunchedEffect
            }

            if (team2Count < playersNeeded) {
                val t2 = SupabaseClient.client.postgrest["teams"]
                    .select { filter { eq("id", match.team2Id) } }
                    .decodeSingleOrNull<Team>()
                onGoToTeam2XI(match.team2Id, t2?.name ?: "Team 2", playersNeeded)
                return@LaunchedEffect
            }

            val allInnings = SupabaseClient.client.postgrest["innings"]
                .select { filter { eq("match_id", matchId) } }
                .decodeList<Innings>()
                .sortedBy { it.inningsNo }

            val completedInnings = allInnings.filter { it.status == "completed" }

            if (completedInnings.size >= 2) {
                onMatchComplete()
                return@LaunchedEffect
            }

            onGoToScoring()

        } catch (e: Exception) {
            android.util.Log.e("CricketHub", "MatchFlow error: ${e.message}", e)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NeonGreen)
    }
}