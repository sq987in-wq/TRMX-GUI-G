package com.example.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServerHealth
import com.example.model.ServerStatus
import com.example.ui.theme.DeckAmber
import com.example.ui.theme.DeckBackground
import com.example.ui.theme.DeckBorderGlass
import com.example.ui.theme.DeckCyan
import com.example.ui.theme.DeckEmerald
import com.example.ui.theme.DeckRed
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckTextMuted
import com.example.ui.theme.DeckTextPrimary
import com.example.ui.theme.DeckTextSecondary
import com.example.ui.theme.TerminalBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsDialog(
    health: ServerHealth,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onEditHostPort: (String, Int) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val statusColor = when (health.status) {
        ServerStatus.ONLINE -> DeckEmerald
        ServerStatus.OFFLINE -> DeckRed
        ServerStatus.CONNECTING -> DeckAmber
        ServerStatus.ERROR -> DeckRed
    }

    val termuxCommand = "cd ~/termux-commanddeck && python3 -m uvicorn main:app --host 127.0.0.1 --port 8080"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeckBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Termux Backend Diagnostics",
                    color = DeckTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status summary card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeckSurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, DeckBorderGlass, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Connection Status", color = DeckTextSecondary, fontSize = 13.sp)
                            Text(
                                health.status.name,
                                color = statusColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Host", color = DeckTextSecondary, fontSize = 13.sp)
                            Text(health.host, color = DeckTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Port", color = DeckTextSecondary, fontSize = 13.sp)
                            Text("${health.port}", color = DeckTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Latency", color = DeckTextSecondary, fontSize = 13.sp)
                            Text(
                                health.latencyMs?.let { "${it} ms" } ?: "N/A",
                                color = if (health.latencyMs != null) DeckCyan else DeckTextMuted,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Backend Version", color = DeckTextSecondary, fontSize = 13.sp)
                            Text(
                                health.backendVersion ?: "Unknown",
                                color = DeckTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        if (health.lastChecked != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(health.lastChecked))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Checked", color = DeckTextSecondary, fontSize = 13.sp)
                                Text(timeStr, color = DeckTextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (health.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Error: ${health.errorMessage}",
                        color = DeckRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Launch Backend in Termux:",
                    color = DeckTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = termuxCommand,
                        color = DeckCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(termuxCommand))
                            Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy command",
                            tint = DeckCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DeckCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DeckBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Connection", color = DeckBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DeckTextSecondary)
            }
        }
    )
}
