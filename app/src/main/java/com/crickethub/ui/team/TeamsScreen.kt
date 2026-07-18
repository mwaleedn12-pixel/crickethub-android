package com.crickethub.ui.team

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Team
import com.crickethub.data.model.TeamInsert
import com.crickethub.ui.theme.*

val TEAM_CATEGORIES = listOf(
    "International", "Domestic", "Club",
    "School", "College", "Corporate", "Street/Tape Ball"
)

val JERSEY_COLORS = listOf(
    "#10B981", "#3B82F6", "#EF4444", "#F59E0B",
    "#8B5CF6", "#EC4899", "#06B6D4", "#F97316",
    "#84CC16", "#FFFFFF", "#000000", "#6B7280"
)

@Composable
fun TeamsScreen(
    onTeamClick: (String) -> Unit,
    onTeamStats: (String, String) -> Unit = { _, _ -> },
    viewModel: TeamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var teamToEdit by remember { mutableStateOf<Team?>(null) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()

    val bg     = if (isDark) Color(0xFF030F08) else Color(0xFFF0FDF8)
    val hdrBg  = if (isDark) Color(0xFF071610) else Color(0xFFECFDF5)
    val surface = if (isDark) Color(0xFF0D2018) else Color(0xFFFFFFFF)
    val border = if (isDark) Color(0xFF1A3828) else Color(0xFFBBF7D0)
    val textP  = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS  = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val green  = Color(0xFF34D399)
    val greenDk = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    val filteredTeams = if (searchQuery.isBlank()) uiState.teams
    else uiState.teams.filter { it.name.contains(searchQuery, ignoreCase = true) }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(hdrBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Teams", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textP)
                    Text("${uiState.teams.size} teams", fontSize = 11.sp, color = textS)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(green)
                        .clickable { showAddDialog = true }.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF031A0E), modifier = Modifier.size(16.dp))
                        Text("New", fontSize = 12.sp, color = Color(0xFF031A0E), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search bar
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search teams...", color = textS, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textP, unfocusedTextColor = textP,
                        focusedBorderColor = green, unfocusedBorderColor = border,
                        focusedContainerColor = surface, unfocusedContainerColor = surface,
                        cursorColor = green
                    )
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = green)
                }
            } else if (filteredTeams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("👥", fontSize = 52.sp)
                        Text(if (searchQuery.isBlank()) "No teams yet" else "No teams found", color = textP, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(if (searchQuery.isBlank()) "Tap New to create your first team" else "Try a different search", color = textS, fontSize = 13.sp)
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(green)
                                    .clickable { showAddDialog = true }.padding(horizontal = 20.dp, vertical = 10.dp)
                            ) { Text("+ Create Team", color = Color(0xFF031A0E), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTeams) { team ->
                        TeamCard(
                            team = team,
                            playerCount = uiState.playerCounts[team.id] ?: 0,
                            isDark = isDark,
                            onClick = { onTeamClick(team.id) },
                            onEdit = { teamToEdit = team },
                            onDelete = { teamToDelete = team },
                            onStats = { onTeamStats(team.id, team.name) }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showAddDialog) {
            AddEditTeamDialog(
                team = null,
                isDark = isDark,
                onDismiss = { showAddDialog = false },
                onConfirm = { insert ->
                    viewModel.createTeam(insert)
                    showAddDialog = false
                }
            )
        }

        teamToEdit?.let { team ->
            AddEditTeamDialog(
                team = team,
                isDark = isDark,
                onDismiss = { teamToEdit = null },
                onConfirm = { insert ->
                    viewModel.updateTeam(team.id, insert)
                    teamToEdit = null
                }
            )
        }

        teamToDelete?.let { team ->
            AlertDialog(
                onDismissRequest = { teamToDelete = null },
                containerColor = if (isDark) Color(0xFF0D2018) else Color.White,
                title = { Text("Delete Team", color = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B), fontWeight = FontWeight.Bold) },
                text = { Text("Delete \"${team.name}\"? This cannot be undone.", color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280), fontSize = 14.sp) },
                confirmButton = {
                    Button(onClick = { viewModel.deleteTeam(team.id); teamToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { teamToDelete = null }) {
                        Text("Cancel", color = green)
                    }
                }
            )
        }
    }
}

@Composable
fun TeamCard(
    team: Team,
    playerCount: Int = 0,
    isDark: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStats: () -> Unit = {}
) {
    val green   = Color(0xFF34D399)
    val greenDk = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val surface = if (isDark) Color(0xFF0D2018) else Color(0xFFFFFFFF)
    val border  = if (isDark) Color(0xFF1A3828) else Color(0xFFD1FAE5)
    val textP   = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS   = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)

    val teamColor = try {
        Color(android.graphics.Color.parseColor(team.jerseyColor ?: "#34D399"))
    } catch (e: Exception) { green }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(surface).border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Team avatar — logo if available, else initials
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(teamColor.copy(alpha = 0.35f), teamColor.copy(alpha = 0.1f))))
                .border(1.5.dp, teamColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!team.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = team.logoUrl,
                    contentDescription = "${team.name} logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    team.name.take(2).uppercase(),
                    color = teamColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Team info
        Column(modifier = Modifier.weight(1f)) {
            Text(team.name, color = textP, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                team.category?.let {
                    Text(it, color = textS, fontSize = 11.sp)
                    Text("•", color = textS, fontSize = 11.sp)
                }
                Text("$playerCount players", color = textS, fontSize = 11.sp)
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(if (isDark) Color(0xFF122A1E) else Color(0xFFE8FDF4))
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = greenDk, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.15f else 0.1f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(Color(0xFF60A5FA).copy(alpha = if (isDark) 0.15f else 0.1f))
                    .clickable { onStats() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BarChart, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
            }
        }

        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Groups,
            contentDescription = null,
            tint = textS.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTeamDialog(
    team: Team?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TeamInsert) -> Unit,
    viewModel: TeamViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var name by remember { mutableStateOf(team?.name ?: "") }
    var shortName by remember { mutableStateOf(team?.shortName ?: "") }
    var selectedCategory by remember { mutableStateOf(team?.category ?: TEAM_CATEGORIES[0]) }
    var selectedColor by remember { mutableStateOf(team?.jerseyColor ?: JERSEY_COLORS[0]) }
    var city by remember { mutableStateOf(team?.city ?: "") }
    var country by remember { mutableStateOf(team?.country ?: "") }
    var homeGround by remember { mutableStateOf(team?.homeGround ?: "") }
    var coach by remember { mutableStateOf(team?.coach ?: "") }
    var logoUrl by remember { mutableStateOf(team?.logoUrl ?: "") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val green  = Color(0xFF34D399)
    val textP  = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS  = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val border = if (isDark) Color(0xFF1A3828) else Color(0xFFBBF7D0)
    val surfBg = if (isDark) Color(0xFF0D2018) else Color.White

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textP, unfocusedTextColor = textP,
        focusedBorderColor = green, unfocusedBorderColor = border,
        focusedLabelColor = green, unfocusedLabelColor = textS, cursorColor = green,
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfBg,
        title = {
            Text(
                if (team == null) "Create Team" else "Edit Team",
                color = textP, fontWeight = FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 480.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Team Name *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = shortName, onValueChange = { if (it.length <= 5) shortName = it },
                        label = { Text("Short Name (max 5 chars)") },
                        placeholder = { Text("e.g. EXI", color = textS) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }
                item {
                    LogoPickerField(
                        logoUrl = logoUrl,
                        isDark = isDark,
                        onLogoUrlChange = { logoUrl = it },
                        onImagePicked = { uri, url -> logoUrl = url },
                        viewModel = viewModel
                    )
                }
                item {
                    ExposedDropdownMenuBox(expanded = showCategoryDropdown, onExpandedChange = { showCategoryDropdown = it }) {
                        OutlinedTextField(
                            value = selectedCategory, onValueChange = {}, readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(10.dp), colors = fieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.background(surfBg)
                        ) {
                            TEAM_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = textP, fontSize = 13.sp) },
                                    onClick = { selectedCategory = cat; showCategoryDropdown = false }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = country, onValueChange = { country = it },
                        label = { Text("Country (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = city, onValueChange = { city = it },
                        label = { Text("City (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = homeGround, onValueChange = { homeGround = it },
                        label = { Text("Home Ground (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = coach, onValueChange = { coach = it },
                        label = { Text("Coach (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }
                item {
                    Text("Jersey Color", color = textS, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(JERSEY_COLORS.size) { i ->
                            val col = JERSEY_COLORS[i]
                            val parsedColor = try { Color(android.graphics.Color.parseColor(col)) } catch (e: Exception) { green }
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(parsedColor)
                                    .border(if (selectedColor == col) 2.5.dp else 0.dp, green, CircleShape)
                                    .clickable { selectedColor = col }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(
                        TeamInsert(
                            name = name.trim(),
                            shortName = shortName.ifBlank { null },
                            logoUrl = logoUrl.ifBlank { null },
                            category = selectedCategory,
                            jerseyColor = selectedColor,
                            country = country.ifBlank { null },
                            city = city.ifBlank { null },
                            homeGround = homeGround.ifBlank { null },
                            coach = coach.ifBlank { null }
                        )
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = green)
            ) { Text(if (team == null) "Create" else "Save", color = Color(0xFF031A0E), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = textS) }
        }
    )
}

@Composable
fun LogoPickerField(
    logoUrl: String,
    isDark: Boolean,
    onLogoUrlChange: (String) -> Unit,
    onImagePicked: (Uri, String) -> Unit,
    viewModel: TeamViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val green   = Color(0xFF34D399)
    val textP   = if (isDark) Color(0xFFECFDF5) else Color(0xFF064E3B)
    val textS   = if (isDark) Color(0xFF6EE7B7) else Color(0xFF6B7280)
    val border  = if (isDark) Color(0xFF1A3828) else Color(0xFFBBF7D0)
    val surfBg  = if (isDark) Color(0xFF122A1E) else Color(0xFFE8FDF4)

    var showUrlInput by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadTeamLogo(context, it) { url ->
                onLogoUrlChange(url)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Team Logo", color = textS, fontSize = 12.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo preview
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfBg)
                    .border(1.dp, border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "Team Logo",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else if (uiState.isUploadingLogo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = green, strokeWidth = 2.dp
                    )
                } else {
                    Text("🏏", fontSize = 24.sp)
                }
            }

            // Buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                // Gallery picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(green.copy(alpha = if (isDark) 0.15f else 0.12f))
                        .border(1.dp, green.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { launcher.launch("image/*") }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = null,
                            tint = green, modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (uiState.isUploadingLogo) "Uploading..." else "Pick from Gallery",
                            color = green, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }

                // URL input toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .border(1.dp, border, RoundedCornerShape(8.dp))
                        .clickable { showUrlInput = !showUrlInput }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Paste URL instead",
                        color = textS, fontSize = 12.sp
                    )
                }
            }
        }

        // URL input field (toggle)
        if (showUrlInput) {
            OutlinedTextField(
                value = logoUrl,
                onValueChange = { onLogoUrlChange(it) },
                label = { Text("Logo URL") },
                placeholder = { Text("https://...", color = textS) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textP, unfocusedTextColor = textP,
                    focusedBorderColor = green, unfocusedBorderColor = border,
                    focusedLabelColor = green, unfocusedLabelColor = textS, cursorColor = green,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                )
            )
        }

        if (logoUrl.isNotBlank() && !uiState.isUploadingLogo) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onLogoUrlChange("") },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Remove logo", fontSize = 11.sp)
                }
            }
        }
    }
}