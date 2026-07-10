package com.crickethub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.crickethub.data.repository.MatchRepository
import com.crickethub.ui.auth.ForgotPasswordScreen
import com.crickethub.ui.auth.LoginScreen
import com.crickethub.ui.auth.SignupScreen
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
import com.crickethub.ui.player.PlayerCareerScreen
import com.crickethub.ui.team.PlayersScreen
import com.crickethub.ui.team.TeamsScreen
import com.crickethub.ui.theme.CricketHubTheme
import com.crickethub.ui.tournament.CreateTournamentScreen
import com.crickethub.ui.tournament.TournamentDetailScreen
import com.crickethub.ui.tournament.TournamentsScreen
import io.github.jan.supabase.postgrest.postgrest

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val NeonGreen = Color(0xFF10B981)
private val TextSecondary = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CricketHubTheme {
                CricketHubApp()
            }
        }
    }
}

@Composable
fun CricketHubApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("teams", "matches", "tournaments", "career")

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = SurfaceCard, contentColor = NeonGreen) {
                    NavigationBarItem(
                        selected = currentRoute == "teams",
                        onClick = { navController.navigate("teams") { popUpTo("teams") { inclusive = true } } },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Teams") },
                        label = { Text("Teams") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen, selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "matches",
                        onClick = { navController.navigate("matches") { popUpTo("teams") { inclusive = false } } },
                        icon = { Icon(Icons.Default.List, contentDescription = "Matches") },
                        label = { Text("Matches") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen, selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "tournaments",
                        onClick = { navController.navigate("tournaments") { popUpTo("teams") { inclusive = false } } },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Tournaments") },
                        label = { Text("Tournaments") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen, selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "career",
                        onClick = { navController.navigate("career") { popUpTo("teams") { inclusive = false } } },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Career") },
                        label = { Text("Career") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen, selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { navController.navigate("teams") { popUpTo("login") { inclusive = true } } },
                    onNavigateToSignup = { navController.navigate("signup") },
                    onNavigateToForgotPassword = { navController.navigate("forgot_password") }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignupSuccess = { navController.navigate("teams") { popUpTo("login") { inclusive = true } } },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable("teams") {
                TeamsScreen(onTeamClick = { teamId -> navController.navigate("players/$teamId") })
            }
            composable(
                route = "players/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                PlayersScreen(teamId = teamId, onBack = { navController.popBackStack() })
            }
            composable("matches") {
                MatchesScreen(
                    onCreateMatch = { navController.navigate("create_match") },
                    onMatchClick = { matchId -> navController.navigate("match_flow/$matchId") },
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { matchId -> navController.navigate("analytics/$matchId") }
                )
            }
            composable("create_match") {
                CreateMatchScreen(
                    onBack = { navController.popBackStack() },
                    onMatchCreated = { matchId ->
                        navController.navigate("match_flow/$matchId") { popUpTo("matches") }
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
                        navController.navigate("match_flow/$id") {
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
                        navController.navigate("match_flow/$matchId") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "scoring/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                val scoringViewModel: ScoringViewModel = viewModel(backStackEntry)
                ScoringScreen(
                    matchId = matchId,
                    onBack = {
                        navController.navigate("matches") {
                            popUpTo("matches") { inclusive = true }
                        }
                    },
                    onInningsComplete = {
                        navController.navigate("post_match/$matchId") {
                            popUpTo("matches")
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
                            liveViewModel.updateFromScoringState(scoringState, team1Name, team2Name)
                        }
                    }

                    LaunchedEffect(team1Name, team2Name) {
                        if (scoringState.innings != null && team1Name != "Team 1") {
                            liveViewModel.updateFromScoringState(scoringState, team1Name, team2Name)
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
                AnalyticsScreen(matchId = matchId, onBack = { navController.popBackStack() })
            }
            composable(
                route = "post_match/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                PostMatchScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStack() },
                    onGoToMatches = {
                        navController.navigate("matches") {
                            popUpTo("matches") { inclusive = true }
                        }
                    }
                )
            }
            composable("tournaments") {
                TournamentsScreen(
                    onCreateTournament = { navController.navigate("create_tournament") },
                    onTournamentClick = { id -> navController.navigate("tournament_detail/$id") }
                )
            }
            composable("create_tournament") {
                CreateTournamentScreen(
                    onBack = { navController.popBackStack() },
                    onTournamentCreated = { id ->
                        navController.navigate("tournament_detail/$id") { popUpTo("tournaments") }
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
            composable("career") {
                PlayerCareerScreen(
                    onBack = { navController.popBackStack() },
                    onViewScorecard = { matchId -> navController.navigate("live_scorecard/$matchId") },
                    onViewAnalytics = { matchId -> navController.navigate("analytics/$matchId") }
                )
            }
        }
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
            val team1Count = xi.count { player -> player.teamId == match.team1Id }
            val team2Count = xi.count { player -> player.teamId == match.team2Id }

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