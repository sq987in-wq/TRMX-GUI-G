package com.example.presentation.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CustomTool
import com.example.presentation.components.GlassCard
import com.example.ui.theme.DeckBackground
import com.example.ui.theme.DeckBorderGlass
import com.example.ui.theme.DeckCyan
import com.example.ui.theme.DeckEmerald
import com.example.ui.theme.DeckPurple
import com.example.ui.theme.DeckRed
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckTextMuted
import com.example.ui.theme.DeckTextPrimary
import com.example.ui.theme.DeckTextSecondary

@Composable
fun CustomToolsScreen(
    viewModel: CustomToolsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.messageSnackbar) {
        uiState.messageSnackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showEditorDialog) {
        ToolEditorDialog(
            initialTool = uiState.toolToEdit,
            onDismiss = { viewModel.closeEditorDialog() },
            onSave = { tool -> viewModel.saveTool(tool) }
        )
    }

    uiState.toolToExecute?.let { tool ->
        ToolExecutionDialog(
            tool = tool,
            onDismiss = { viewModel.closeExecutionDialog() },
            onExecute = { inputs -> viewModel.executeTool(tool, inputs) }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeckBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Custom Actions",
                        color = DeckTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Structural CLI actions & parameter builders",
                        color = DeckTextMuted,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${uiState.tools.size} ACTIONS",
                        color = DeckCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Tools List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
            ) {
                if (uiState.tools.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom actions created yet.\nClick + to create your first action.",
                                color = DeckTextMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(uiState.tools, key = { it.id }) { tool ->
                    CustomToolCard(
                        tool = tool,
                        onExecute = { viewModel.openExecutionDialog(tool) },
                        onEdit = { viewModel.openEditDialog(tool) },
                        onDelete = { viewModel.deleteTool(tool.id) },
                        onToggle = { viewModel.toggleToolEnabled(tool) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // FAB to add action
        FloatingActionButton(
            onClick = { viewModel.openAddDialog() },
            containerColor = DeckCyan,
            contentColor = DeckBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("add_custom_action_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom action")
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
fun CustomToolCard(
    tool: CustomTool,
    onExecute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val toolAccent = try {
        Color(android.graphics.Color.parseColor(tool.accentColor))
    } catch (_: Exception) {
        DeckCyan
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_tool_card_${tool.id}"),
        shape = RoundedCornerShape(22.dp),
        borderColor = if (tool.enabled) toolAccent.copy(alpha = 0.40f) else DeckBorderGlass,
        topGlowColor = if (tool.enabled) toolAccent else null,
        topGlowFraction = 1f
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(toolAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, toolAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = toolAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tool.name,
                            color = DeckTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tool.executable,
                            color = toolAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Switch(
                    checked = tool.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = toolAccent,
                        uncheckedThumbColor = DeckTextMuted,
                        uncheckedTrackColor = DeckSurfaceElevated
                    )
                )
            }

            if (tool.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tool.description,
                    color = DeckTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arguments template preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeckSurfaceElevated, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${tool.executable} ${tool.arguments.joinToString(" ")}",
                    color = DeckTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = DeckTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DeckRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            if (tool.enabled) toolAccent else DeckSurfaceElevated,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    IconButton(
                        onClick = onExecute,
                        enabled = tool.enabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Execute action",
                            tint = if (tool.enabled) DeckBackground else DeckTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
