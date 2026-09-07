package com.example.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.OutputFile
import com.example.model.ServerStatus
import com.example.presentation.components.DiagnosticsDialog
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import com.example.presentation.components.GlassCard
import com.example.presentation.components.JobStatusBadge
import com.example.presentation.components.TerminalConsole
import com.example.ui.theme.DeckAmber
import com.example.ui.theme.DeckBackground
import com.example.ui.theme.DeckBorderGlass
import com.example.ui.theme.DeckCyan
import com.example.ui.theme.DeckCyanGradientStart
import com.example.ui.theme.DeckEmerald
import com.example.ui.theme.DeckPurple
import com.example.ui.theme.DeckPurpleGradientEnd
import com.example.ui.theme.DeckRed
import com.example.ui.theme.DeckSurface
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckTextMuted
import com.example.ui.theme.DeckTextPrimary
import com.example.ui.theme.DeckTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedToolTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.messageSnackbar) {
        uiState.messageSnackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showDiagnostics) {
        DiagnosticsDialog(
            health = uiState.serverHealth,
            onDismiss = { viewModel.setDiagnosticsVisible(false) },
            onRetry = { viewModel.checkServerHealth() },
            onEditHostPort = { _, _ -> }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeckBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header with status indicator and diagnostics
            item {
                DashboardHeader(
                    health = uiState.serverHealth,
                    onOpenDiagnostics = { viewModel.setDiagnosticsVisible(true) },
                    onRetryHealth = { viewModel.checkServerHealth() }
                )
            }

            // Offline alert banner if Termux backend is not reached
            if (uiState.serverHealth.status == ServerStatus.OFFLINE || uiState.serverHealth.status == ServerStatus.ERROR) {
                item {
                    OfflineNoticeCard(
                        onRetry = { viewModel.checkServerHealth() },
                        onOpenDiagnostics = { viewModel.setDiagnosticsVisible(true) }
                    )
                }
            }

            // Native Android System Status Cards (3-column frosted glass)
            item {
                SystemStatusSection(uiState)
            }

            // Active Jobs Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "ACTIVE JOBS",
                        color = DeckTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (uiState.serverHealth.status == ServerStatus.ONLINE) "SSE CONNECTED" else "SSE STANDBY",
                        color = DeckCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Active or Selected Job Progress & Live Terminal
            if (uiState.currentJob != null) {
                item {
                    val activeJob = uiState.currentJob!!
                    ActiveJobCard(
                        job = activeJob,
                        onCancel = { viewModel.cancelJob(activeJob.jobId) },
                        onExportFile = { file -> viewModel.exportOutputFile(file, activeJob.jobId) }
                    )
                }
            } else {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ready For Execution",
                                    color = DeckTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Select a tool below or dispatch tasks via AI Agent",
                                    color = DeckTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(DeckEmerald, CircleShape)
                            )
                        }
                    }
                }
            }

            // Native Toolset Section
            item {
                NativeToolsetSection(
                    selectedToolTab = selectedToolTab,
                    onSelectTab = { selectedToolTab = it },
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            // Recent Jobs History
            if (uiState.recentJobs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT EXECUTIONS",
                            color = DeckTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${uiState.recentJobs.size} tasks",
                            color = DeckTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                items(uiState.recentJobs.take(5)) { job ->
                    RecentJobItem(
                        job = job,
                        onClick = { viewModel.selectJob(job) },
                        onExportFile = { file -> viewModel.exportOutputFile(file, job.jobId) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
fun DashboardHeader(
    health: com.example.model.ServerHealth,
    onOpenDiagnostics: () -> Unit,
    onRetryHealth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "CommandDeck",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(DeckCyanGradientStart, DeckPurpleGradientEnd)
                    ),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenDiagnostics() }
            ) {
                val statusColor = when (health.status) {
                    ServerStatus.ONLINE -> DeckEmerald
                    ServerStatus.OFFLINE -> DeckRed
                    ServerStatus.CONNECTING -> DeckAmber
                    ServerStatus.ERROR -> DeckRed
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                        .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val label = when (health.status) {
                    ServerStatus.ONLINE -> "SERVER ONLINE • ${health.host}:${health.port}"
                    ServerStatus.OFFLINE -> "SERVER OFFLINE • ${health.host}:${health.port}"
                    ServerStatus.CONNECTING -> "CHECKING SERVER • ${health.host}:${health.port}"
                    ServerStatus.ERROR -> "SERVER ERROR • ${health.host}:${health.port}"
                }
                Text(
                    text = label,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .clickable { onOpenDiagnostics() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings / Diagnostics",
                tint = DeckTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun OfflineNoticeCard(
    onRetry: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        borderColor = DeckRed.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DeckRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = DeckRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Termux Server Offline",
                    color = DeckRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Backend not detected on 127.0.0.1:8080. Start it in Termux to execute commands.",
                    color = DeckTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeckCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, DeckCyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("retry_server_button")
            ) {
                Text("Retry", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SystemStatusSection(uiState: DashboardUiState) {
    val sys = uiState.systemStatus

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Battery Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Battery", color = DeckTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (sys != null) "${sys.batteryPercent}%" else "--",
                    color = DeckTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Thermal Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Thermal", color = DeckTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(3.dp))
                val tempText = sys?.batteryTemperatureCelsius?.let { "${it}°C" } ?: sys?.thermalStatusText ?: "Nominal"
                Text(
                    text = tempText,
                    color = DeckAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Storage Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Storage", color = DeckTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = sys?.freeStorageFormatted ?: "--",
                    color = DeckTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MediaDownloaderForm(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    Column {
        Text(
            text = "Download videos or extract audio using yt-dlp inside Termux.",
            color = DeckTextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.mediaUrl,
            onValueChange = { viewModel.setMediaUrl(it) },
            label = { Text("Media or Video URL") },
            placeholder = { Text("https://www.youtube.com/watch?v=...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("media_url_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeckCyan,
                unfocusedBorderColor = DeckBorderGlass,
                focusedTextColor = DeckTextPrimary,
                unfocusedTextColor = DeckTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedDownloadMode == "VIDEO",
                    onClick = { viewModel.setDownloadMode("VIDEO") },
                    label = { Text("Video (MP4)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeckCyan.copy(alpha = 0.2f),
                        selectedLabelColor = DeckCyan
                    )
                )
                FilterChip(
                    selected = uiState.selectedDownloadMode == "AUDIO",
                    onClick = { viewModel.setDownloadMode("AUDIO") },
                    label = { Text("Audio (MP3)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeckPurple.copy(alpha = 0.2f),
                        selectedLabelColor = DeckPurple
                    )
                )
            }

            Button(
                onClick = { viewModel.startMediaDownload() },
                enabled = !uiState.isSubmittingJob,
                colors = ButtonDefaults.buttonColors(containerColor = DeckCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("start_download_button")
            ) {
                if (uiState.isSubmittingJob) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeckBackground)
                } else {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = DeckBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download", color = DeckBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MediaCompressorForm(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    Column {
        Text(
            text = "Compress local video or audio using ffmpeg presets.",
            color = DeckTextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.compressorInputFile,
            onValueChange = { viewModel.setCompressorInputFile(it) },
            label = { Text("Source File Path (inside Termux or ~/storage/shared)") },
            placeholder = { Text("~/storage/shared/Movies/sample.mp4") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compressor_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeckCyan,
                unfocusedBorderColor = DeckBorderGlass,
                focusedTextColor = DeckTextPrimary,
                unfocusedTextColor = DeckTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("LOW", "MEDIUM", "ULTRA").forEach { preset ->
                    FilterChip(
                        selected = uiState.compressorPreset == preset,
                        onClick = { viewModel.setCompressorPreset(preset) },
                        label = { Text(preset) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeckAmber.copy(alpha = 0.2f),
                            selectedLabelColor = DeckAmber
                        )
                    )
                }
            }

            Button(
                onClick = { viewModel.startMediaCompression() },
                enabled = !uiState.isSubmittingJob,
                colors = ButtonDefaults.buttonColors(containerColor = DeckAmber),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("start_compress_button")
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = DeckBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compress", color = DeckBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TtsForm(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    Column {
        Text(
            text = "Synthesize natural spoken audio using edge-tts CLI.",
            color = DeckTextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.ttsText,
            onValueChange = { viewModel.setTtsText(it) },
            label = { Text("Text to Read") },
            placeholder = { Text("Termux CommandDeck is connected and operational.") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tts_text_input"),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeckCyan,
                unfocusedBorderColor = DeckBorderGlass,
                focusedTextColor = DeckTextPrimary,
                unfocusedTextColor = DeckTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "en-US-ChristopherNeural" to "Christopher",
                    "en-US-JennyNeural" to "Jenny",
                    "en-GB-SoniaNeural" to "Sonia"
                ).forEach { (voiceId, label) ->
                    FilterChip(
                        selected = uiState.ttsVoice == voiceId,
                        onClick = { viewModel.setTtsVoice(voiceId) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeckPurple.copy(alpha = 0.2f),
                            selectedLabelColor = DeckPurple
                        )
                    )
                }
            }

            Button(
                onClick = { viewModel.startTts() },
                enabled = !uiState.isSubmittingJob,
                colors = ButtonDefaults.buttonColors(containerColor = DeckPurple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("start_tts_button")
            ) {
                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = DeckBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Synthesize", color = DeckBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NativeToolsetSection(
    selectedToolTab: Int,
    onSelectTab: (Int) -> Unit,
    uiState: DashboardUiState,
    viewModel: DashboardViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "NATIVE TOOLSET",
            color = DeckTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 3-tile selection row matching the Frosted Glass design
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolsetTile(
                modifier = Modifier.weight(1f),
                title = "Downloader",
                subtitle = "yt-dlp binary",
                icon = Icons.Default.CloudDownload,
                gradientColor = DeckCyan,
                isSelected = selectedToolTab == 0,
                onClick = { onSelectTab(0) }
            )

            ToolsetTile(
                modifier = Modifier.weight(1f),
                title = "Compressor",
                subtitle = "ffmpeg presets",
                icon = Icons.Default.Tune,
                gradientColor = DeckPurple,
                isSelected = selectedToolTab == 1,
                onClick = { onSelectTab(1) }
            )

            ToolsetTile(
                modifier = Modifier.weight(1f),
                title = "TTS Voice",
                subtitle = "edge-tts",
                icon = Icons.Default.VolumeUp,
                gradientColor = DeckAmber,
                isSelected = selectedToolTab == 2,
                onClick = { onSelectTab(2) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tool execution parameter form in Frosted Glass Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColor = when (selectedToolTab) {
                0 -> DeckCyan
                1 -> DeckPurple
                else -> DeckAmber
            }.copy(alpha = 0.35f)
        ) {
            when (selectedToolTab) {
                0 -> MediaDownloaderForm(uiState, viewModel)
                1 -> MediaCompressorForm(uiState, viewModel)
                2 -> TtsForm(uiState, viewModel)
            }
        }
    }
}

@Composable
fun ToolsetTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = Brush.linearGradient(
        colors = if (isSelected) {
            listOf(gradientColor.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
        } else {
            listOf(gradientColor.copy(alpha = 0.12f), Color.Transparent)
        }
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) gradientColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) gradientColor else DeckTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column {
                Text(
                    text = title,
                    color = DeckTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = DeckTextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ActiveJobCard(
    job: Job,
    onCancel: () -> Unit,
    onExportFile: (OutputFile) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        borderColor = when (job.status) {
            JobStatus.RUNNING -> DeckCyan.copy(alpha = 0.40f)
            JobStatus.COMPLETED -> DeckEmerald.copy(alpha = 0.40f)
            JobStatus.FAILED -> DeckRed.copy(alpha = 0.40f)
            else -> DeckBorderGlass
        },
        topGlowColor = if (job.status == JobStatus.RUNNING) DeckCyan else if (job.status == JobStatus.COMPLETED) DeckEmerald else null,
        topGlowFraction = (job.progress / 100f).coerceIn(0.04f, 1f)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${job.type}: ${job.command.ifEmpty { "Job ${job.jobId.take(8)}" }}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ID: ${job.jobId.take(8)}${if (job.speed != null) " • ${job.speed}" else ""}${if (job.eta != null) " • ETA ${job.eta}" else ""}",
                        color = DeckTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${job.progress.toInt()}%",
                    color = DeckCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar and Cancel button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (job.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DeckCyan,
                    trackColor = Color.White.copy(alpha = 0.10f)
                )
                if (job.status == JobStatus.RUNNING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeckRed.copy(alpha = 0.20f))
                            .border(1.dp, DeckRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable { onCancel() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("cancel_job_button")
                    ) {
                        Text(
                            text = "CANCEL",
                            color = DeckRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Terminal Console
            TerminalConsole(
                stdout = job.stdout,
                stderr = job.stderr,
                title = "STREAM: ${job.jobId.take(12)}"
            )

            // Output files if available
            if (job.outputFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "GENERATED FILES:",
                    color = DeckEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                job.outputFiles.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeckSurfaceElevated, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.filename,
                            color = DeckTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { onExportFile(file) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeckEmerald),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentJobItem(
    job: Job,
    onClick: () -> Unit,
    onExportFile: (OutputFile) -> Unit
) {
    val dateStr = remember(job.updatedAt) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(job.updatedAt))
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.type,
                    color = DeckCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateStr,
                    color = DeckTextMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.command.ifEmpty { "Job ID: ${job.jobId}" },
                color = DeckTextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                JobStatusBadge(status = job.status)
                if (job.outputFiles.isNotEmpty()) {
                    Text(
                        text = "${job.outputFiles.size} output file(s)",
                        color = DeckEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
