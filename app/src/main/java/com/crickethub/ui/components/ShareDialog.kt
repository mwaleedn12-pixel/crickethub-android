package com.crickethub.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
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
import com.crickethub.data.model.SharedLink
import com.crickethub.data.repository.SharingRepository
import com.crickethub.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ShareDialog(
    resourceType: String,
    resourceId: String,
    resourceName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repo = remember { SharingRepository() }

    var viewLink by remember { mutableStateOf<SharedLink?>(null) }
    var editLink by remember { mutableStateOf<SharedLink?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load existing links
    LaunchedEffect(resourceId) {
        try {
            val links = repo.getShareLinks(resourceType, resourceId)
            viewLink = links.find { !it.canEdit }
            editLink = links.find { it.canEdit }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    fun shareViaWhatsApp(code: String, canEdit: Boolean) {
        val permission = if (canEdit) "view & edit" else "view only"
        val emoji = when (resourceType) {
            "match" -> "🏏"
            "team" -> "👕"
            "tournament" -> "🏆"
            else -> "📋"
        }
        val text = """
$emoji CricketHub — $resourceName

Join with code: *$code*

Permission: $permission

Open CricketHub app → Menu → Join with Code → Enter: $code
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed - use general share
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(generalIntent, "Share via"))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D2018),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Share, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                Text("Share $resourceName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    error?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }

                    // View Only Link
                    ShareLinkCard(
                        title = "View Only",
                        icon = Icons.Default.Visibility,
                        iconColor = NeonBlue,
                        link = viewLink,
                        onGenerate = {
                            scope.launch {
                                isLoading = true
                                try {
                                    viewLink = repo.createShareLink(resourceType, resourceId, false)
                                } catch (e: Exception) { error = e.message }
                                isLoading = false
                            }
                        },
                        onCopy = { code ->
                            clipboard.setText(AnnotatedString(code))
                        },
                        onWhatsApp = { code -> shareViaWhatsApp(code, false) },
                        onDelete = {
                            scope.launch {
                                viewLink?.let { repo.deleteShareLink(it.id) }
                                viewLink = null
                            }
                        }
                    )

                    HorizontalDivider(color = BorderColor)

                    // Edit Link
                    ShareLinkCard(
                        title = "Can Edit",
                        icon = Icons.Default.Edit,
                        iconColor = AmberColor,
                        link = editLink,
                        onGenerate = {
                            scope.launch {
                                isLoading = true
                                try {
                                    editLink = repo.createShareLink(resourceType, resourceId, true)
                                } catch (e: Exception) { error = e.message }
                                isLoading = false
                            }
                        },
                        onCopy = { code ->
                            clipboard.setText(AnnotatedString(code))
                        },
                        onWhatsApp = { code -> shareViaWhatsApp(code, true) },
                        onDelete = {
                            scope.launch {
                                editLink?.let { repo.deleteShareLink(it.id) }
                                editLink = null
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ShareLinkCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    link: SharedLink?,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Text(title, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (link != null) {
            // Show code
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF030F08))
                    .border(1.dp, iconColor.copy(0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    link.code,
                    color = iconColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Copy button
                OutlinedButton(
                    onClick = { onCopy(link.code) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp)
                }
                // WhatsApp button
                Button(
                    onClick = { onWhatsApp(link.code) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                // Delete
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("✕", fontSize = 14.sp)
                }
            }
        } else {
            // Generate button
            OutlinedButton(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = iconColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(0.5f))
            ) {
                Text("+ Generate ${title} Link", fontSize = 13.sp)
            }
        }
    }
}
