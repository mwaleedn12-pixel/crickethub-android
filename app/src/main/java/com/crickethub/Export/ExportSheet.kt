package com.crickethub.export

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.crickethub.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Export format options.
 */
enum class ExportFormat(val label: String, val emoji: String, val mime: String, val ext: String) {
    PDF("PDF Report", "📄", "application/pdf", "pdf"),
    CSV("CSV Data", "📊", "text/csv", "csv")
}

/**
 * Reusable export bottom sheet / dialog.
 *
 * Usage:
 *   ExportDialog(
 *       show = showExport,
 *       onDismiss = { showExport = false },
 *       title = "Export Scorecard",
 *       onExport = { format ->
 *           when (format) {
 *               ExportFormat.PDF -> MatchReportGenerator.generatePdf(context, data)
 *               ExportFormat.CSV -> MatchReportGenerator.generateCsv(context, data)
 *           }
 *       }
 *   )
 */
@Composable
fun ExportDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String = "Export Report",
    formats: List<ExportFormat> = ExportFormat.entries,
    onExport: suspend (ExportFormat) -> android.net.Uri
) {
    if (!show) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportingFormat by remember { mutableStateOf<ExportFormat?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        containerColor = CH.surface,
        title = {
            Text(title, color = CH.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose export format",
                    color = CH.textSecondary, fontSize = 12.sp
                )

                formats.forEach { format ->
                    val isActive = exportingFormat == format
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) NeonGreen.copy(alpha = 0.1f)
                                else if (isSystemInDarkTheme()) Color(0xFF0A0A0A)
                                else Color(0xFFF7F3EA)
                            )
                            .border(
                                1.dp,
                                if (isActive) NeonGreen else CH.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !isExporting) {
                                scope.launch {
                                    isExporting = true
                                    exportingFormat = format
                                    try {
                                        val uri = withContext(Dispatchers.IO) {
                                            onExport(format)
                                        }
                                        shareFile(context, uri, format.mime)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        android.util.Log.e("CricketHub", "Export error: ${e.message}", e)
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isExporting = false
                                        exportingFormat = null
                                    }
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(format.emoji, fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                format.label, color = CH.textPrimary,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when (format) {
                                    ExportFormat.PDF -> "Professional report with tables & branding"
                                    ExportFormat.CSV -> "Raw data, opens in Excel/Sheets"
                                },
                                color = CH.textSecondary, fontSize = 11.sp
                            )
                        }
                        if (isActive) {
                            CircularProgressIndicator(
                                color = NeonGreen,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isExporting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = CH.textSecondary)
                }
            }
        }
    )
}

/**
 * Simple export button composable to place in any screen's toolbar/header.
 */
@Composable
fun ExportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("📤 Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}