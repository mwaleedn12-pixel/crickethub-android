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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
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
import com.crickethub.data.repository.MatchRepository
import com.crickethub.ui.auth.LoginScreen
import com.crickethub.ui.auth.SignupScreen
import com.crickethub.ui.match.CreateMatchScreen
import com.crickethub.ui.match.MatchViewModel
import com.crickethub.ui.match.MatchesScreen
import com.crickethub.ui.match.PlayingXIScreen
import com.crickethub.ui.match.TossScreen
import com.crickethub.ui.match.live.LiveScorecardScreen
import com.crickethub.ui.match.scoring.ScoringScreen
import com.crickethub.ui.team.PlayersScreen
import com.crickethub.ui.team.TeamsScreen
import com.crickethub.ui.theme.CricketHubTheme

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

    val showBottomBar = currentRoute in listOf("teams", "matches")

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceCard,
                    contentColor = NeonGreen
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "teams",
                        onClick = {
                            navController.navigate("teams") {
                                popUpTo("teams") { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Teams") },
                        label = { Text("Teams") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "matches",
                        onClick = {
                            navController.navigate("matches") {
                                popUpTo("teams") { inclusive = false }
                            }
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = "Matches") },
                        label = { Text("Matches") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
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
                    onLoginSuccess = {
                        navController.navigate("teams") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToSignup = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignupSuccess = {
                        navController.navigate("teams") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable("teams") {
                TeamsScreen(
                    onTeamClick = { teamId ->
                        navController.navigate("players/$teamId")
                    }
                )
            }
            composable(
                route = "players/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                PlayersScreen(
                    teamId = teamId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("matches") {
                MatchesScreen(
                    onCreateMatch = { navController.navigate("create_match") },
                    onMatchClick = { matchId ->
                        navController.navigate("match_flow/$matchId")
                    },
                    onViewScorecard = { matchId ->
                        navController.navigate("live_scorecard/$matchId")
                    }
                )
            }
            composable("create_match") {
                CreateMatchScreen(
                    onBack = { navController.popBackStack() },
                    onMatchCreated = { matchId ->
                        navController.navigate("match_flow/$matchId") {
                            popUpTo("matches")
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
                    onGoToTeam1XI = {
                        navController.navigate("playing_xi_team1/$matchId") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    },
                    onGoToTeam2XI = {
                        navController.navigate("playing_xi_team2/$matchId") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
                        }
                    },
                    onGoToScoring = {
                        navController.navigate("scoring/$matchId") {
                            popUpTo("match_flow/$matchId") { inclusive = true }
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
                route = "playing_xi_team1/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                Team1PlayingXI(
                    matchId = matchId,
                    onBack = { navController.popBackStack() },
                    onXISaved = {
                        navController.navigate("playing_xi_team2/$matchId") {
                            popUpTo("playing_xi_team1/$matchId") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "playing_xi_team2/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                Team2PlayingXI(
                    matchId = matchId,
                    onBack = { navController.popBackStack() },
                    onXISaved = {
                        navController.navigate("scoring/$matchId") {
                            popUpTo("matches")
                        }
                    }
                )
            }
            composable(
                route = "scoring/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                ScoringScreen(
                    matchId = matchId,
                    onBack = {
                        navController.navigate("matches") {
                            popUpTo("matches") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "live_scorecard/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                LiveScorecardScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MatchFlowScreen(
    matchId: String,
    onGoToToss: () -> Unit,
    onGoToTeam1XI: () -> Unit,
    onGoToTeam2XI: () -> Unit,
    onGoToScoring: () -> Unit
) {
    val viewModel: MatchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var xiSaved by remember { mutableStateOf<Boolean?>(null) }
    var team1XISaved by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(matchId) {
        viewModel.loadMatchById(matchId)
        viewModel.loadTeams()
        try {
            val repo = MatchRepository()
            val xi = repo.getPlayingXI(matchId)
            val match = repo.getMatchById(matchId)
            val team1Id = match?.team1Id
            val team2Id = match?.team2Id
            val team1Count = xi.count { it.teamId == team1Id }
            val team2Count = xi.count { it.teamId == team2Id }
            team1XISaved = team1Count >= 11
            xiSaved = team1Count >= 11 && team2Count >= 11
        } catch (e: Exception) {
            xiSaved = false
            team1XISaved = false
        }
    }

    LaunchedEffect(uiState.currentMatch, xiSaved, team1XISaved) {
        val match = uiState.currentMatch
        if (match == null || xiSaved == null || team1XISaved == null) return@LaunchedEffect
        when {
            match.tossWinnerId == null -> onGoToToss()
            xiSaved == true -> onGoToScoring()
            team1XISaved == false -> onGoToTeam1XI()
            else -> onGoToTeam2XI()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NeonGreen)
    }
}

@Composable
fun Team1PlayingXI(
    matchId: String,
    onBack: () -> Unit,
    onXISaved: () -> Unit
) {
    val viewModel: MatchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.loadMatchById(matchId)
        viewModel.loadTeams()
    }

    val match = uiState.currentMatch
    val team1 = uiState.teams.find { it.id == match?.team1Id }

    if (uiState.isLoading || match == null || team1 == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF030712)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF10B981))
        }
    } else {
        PlayingXIScreen(
            matchId = matchId,
            teamId = team1.id,
            teamName = team1.name,
            playersPerSide = match.playersPerSide,
            onBack = onBack,
            onXISaved = onXISaved
        )
    }
}

@Composable
fun Team2PlayingXI(
    matchId: String,
    onBack: () -> Unit,
    onXISaved: () -> Unit
) {
    val viewModel: MatchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.loadMatchById(matchId)
        viewModel.loadTeams()
    }

    val match = uiState.currentMatch
    val team2 = uiState.teams.find { it.id == match?.team2Id }

    if (uiState.isLoading || match == null || team2 == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF030712)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF10B981))
        }
    } else {
        PlayingXIScreen(
            matchId = matchId,
            teamId = team2.id,
            teamName = team2.name,
            playersPerSide = match.playersPerSide,
            onBack = onBack,
            onXISaved = onXISaved
        )
    }
}