package com.crickethub.ui.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.repository.SharingRepository
import com.crickethub.ui.components.CricketAnimatedBackground
import com.crickethub.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun JoinWithCodeScreen(
    onBack: () -> Unit,
    onJoinMatch: (matchId: String, canEdit: Boolean) -> Unit,
    onJoinTeam: (teamId: String) -> Unit,
    onJoinTournament: (tournamentId: String) -> Unit
) {
    val repo = remember { SharingRepository() }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                }
                Text("Join with Code", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🏏", fontSize = 64.sp)
                Text("Enter Share Code", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Ask the owner for their share code", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(10) },
                    label = { Text("e.g. MCH-A3X9B2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary,
                        cursorColor = NeonGreen
                    )
                )

                error?.let {
                    Text(it, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        if (code.isBlank()) { error = "Enter a code first"; return@Button }
                        scope.launch {
                            isLoading = true
                            error = null
                            try {
                                val link = repo.getByCode(code)
                                if (link == null) {
                                    error = "Invalid code. Check and try again."
                                } else {
                                    when (link.resourceType) {
                                        "match" -> onJoinMatch(link.resourceId, link.canEdit)
                                        "team" -> onJoinTeam(link.resourceId)
                                        "tournament" -> onJoinTournament(link.resourceId)
                                        else -> error = "Unknown resource type"
                                    }
                                }
                            } catch (e: Exception) {
                                error = "Error: ${e.message}"
                            }
                            isLoading = false
                        }
                    },
                    enabled = code.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Join", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
